package cx.twinkle.rediscache.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author twinkle
 * @version 2019/12/30 18:54
 */
public class RedisCacheServiceImpl implements RedisCacheService {
    private static final Logger log = LoggerFactory.getLogger(RedisCacheServiceImpl.class);

    private StringRedisTemplate redisTemplate;
    private SerializeService serializeService;

    public RedisCacheServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.serializeService = new SerializeServiceImpl();
    }

    /**
     * 用于存储每个Cache的key
     */
    private static final String CACHE_KEY_SET_PREFIX = "REDIS_CACHE_KEYS_OF_";
    /**
     * 用于存储每个CacheKey 对应的 CacheName，使用一个Hash存储，下面就是Hash的key
     */
    public static final String CACHE_NAME_OF_KEY_HASH_KEY = "C7264226X_CACHE_NAME_OF_KEY_HASH";

    @Override
    public Object getFromRedis(String key) {
        String resultStr = redisTemplate.opsForValue().get(key);
        return serializeService.deserialize(resultStr);
    }

    @Override
    public void insert2Redis(String key, Object value, Duration duration, String cacheName) {
        String serializedStr = serializeService.serialize(value);
        // 如果序列化结果是空字符串，则不写入Redis
        if (StringUtils.isEmpty(serializedStr)) {
            log.info("缓存key = {} 序列化的结果为空！不写入Redis。value = {}", key, value);
            return;
        }
        if (Duration.ZERO == duration) {
            redisTemplate.opsForValue().set(key, serializedStr);
        } else {
            redisTemplate.opsForValue().set(key, serializedStr, duration.getSeconds(), TimeUnit.SECONDS);
        }
        /* 记录 key */
        String keySetKey = CACHE_KEY_SET_PREFIX + cacheName;
        redisTemplate.opsForSet().add(keySetKey, key);
        redisTemplate.opsForHash().put(CACHE_NAME_OF_KEY_HASH_KEY, key, cacheName);
        // 记录一下这个Key
        CacheHolder.add(keySetKey);
    }

    @Override
    @SuppressWarnings("ConfusingArgumentToVarargsMethod")
    public void deleteByKey(String... keys) {
        for (String key : keys) {
            redisTemplate.delete(key);
            Object cacheName = redisTemplate.opsForHash().get(CACHE_NAME_OF_KEY_HASH_KEY, key);
            String keySetKey = CACHE_KEY_SET_PREFIX + cacheName;
            redisTemplate.opsForSet().remove(keySetKey, key);
        }
        redisTemplate.opsForHash().delete(CACHE_NAME_OF_KEY_HASH_KEY, keys);
    }

    @Override
    public void deleteByCacheName(String cacheName) {
        String keySetKey = CACHE_KEY_SET_PREFIX + cacheName;
        Set<String> keySet = redisTemplate.opsForSet().members(keySetKey);
        if (CollectionUtils.isEmpty(keySet)) {
            log.info("缓存名 {} 下没有任何缓存Key！没有任何缓存被删除！", cacheName);
            return;
        }
        keySet.forEach(k -> redisTemplate.delete(k));
        redisTemplate.delete(keySetKey);
        redisTemplate.opsForHash().delete(CACHE_NAME_OF_KEY_HASH_KEY, keySet.toArray(new Object[0]));
    }
}
