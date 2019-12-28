package cx.twinkle.rediscache.service.impl;

import cx.twinkle.rediscache.config.CustomRedisCacheConfig;
import cx.twinkle.rediscache.service.CacheService;
import cx.twinkle.rediscache.service.SerializeService;
import cx.twinkle.rediscache.service.task.CacheHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author twinkle
 * @version 2019/12/27 15:04
 */
public class CacheServiceImpl implements CacheService {
    private static final Logger log = LoggerFactory.getLogger(CacheServiceImpl.class);

    @Resource(name = "stringRedisTemplate")
    private StringRedisTemplate redisTemplate;
    @Resource
    private SerializeService serializeService;
    @Resource
    private CustomRedisCacheConfig customRedisCacheConfig;

    /**
     * 用于存储每个Cache的key
     */
    private static final String CACHE_KEY_SET_PREFIX = "REDIS_CACHE_KEYS_OF_";
    /**
     * 用于存储每个CacheKey 对应的 CacheName，使用一个Hash存储，下面就是Hash的key
     */
    public static final String CACHE_NAME_OF_KEY_HASH_KEY = "C7264226X_CACHE_NAME_OF_KEY_HASH";

    @Override
    public String generateCacheKey(String cacheName, String methodName, Object... params) {
        StringBuilder keyBuilder = new StringBuilder(cacheName).append("::").append(methodName).append("-v1_0");
        if (params == null || params.length <= 0) {
            log.debug("获取缓存key，方法 {} 的参数体为空！", methodName);
            return keyBuilder.toString();
        }
        keyBuilder.append("-");
        Integer maxParamNum = customRedisCacheConfig.getMaxParamNum();
        if (params.length > maxParamNum) {
            log.info("获取缓存key，方法 {} 的参数个数大于【{}】个，采用MD5摘要~", methodName, maxParamNum);
            String paramMd5 = DigestUtils.md5DigestAsHex(Arrays.toString(params).getBytes());
            keyBuilder.append(paramMd5);
            return keyBuilder.toString();
        }
        for (Object param : params) {
            String paramStr = String.valueOf(param);
            keyBuilder.append(paramStr.replace(":", "-")).append("_");
        }
        return keyBuilder.toString();
    }

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
        // FIXME 公司的Redis支持这个批量操作么？待测
        redisTemplate.opsForHash().delete(CACHE_NAME_OF_KEY_HASH_KEY, keys);
    }

    @Override
    @SuppressWarnings("ConfusingArgumentToVarargsMethod")
    public void deleteByCacheName(String cacheName) {
        String keySetKey = CACHE_KEY_SET_PREFIX + cacheName;
        Set<String> keySet = redisTemplate.opsForSet().members(keySetKey);
        if (CollectionUtils.isEmpty(keySet)) {
            log.warn("缓存名 {} 下没有任何缓存Key！", cacheName);
            return;
        }
        keySet.forEach(k -> redisTemplate.delete(k));
        redisTemplate.delete(keySetKey);
        // FIXME 公司的Redis支持这个批量操作么？待测
        redisTemplate.opsForHash().delete(CACHE_NAME_OF_KEY_HASH_KEY, keySet.toArray(new String[0]));
    }
}
