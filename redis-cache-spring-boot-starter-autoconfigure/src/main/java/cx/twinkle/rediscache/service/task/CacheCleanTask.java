package cx.twinkle.rediscache.service.task;

import cx.twinkle.rediscache.config.CustomRedisCacheConfig;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Set;

/**
 * @author twinkle
 * @version 2019/12/28 16:58
 */
public class CacheCleanTask {
    @Resource
    private TaskScheduler taskScheduler;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private CustomRedisCacheConfig customRedisCacheConfig;
    /**
     * 用于存储每个CacheKey 对应的 CacheName，使用一个Hash存储，下面就是Hash的key
     *
     * @apiNote 同 {@link cx.twinkle.rediscache.service.impl.CacheServiceImpl#CACHE_NAME_OF_KEY_HASH_KEY}
     */
    @SuppressWarnings("JavadocReference")
    private static final String CACHE_NAME_OF_KEY_HASH_KEY = "C7264226X_CACHE_NAME_OF_KEY_HASH";

    @PostConstruct
    public void run() {
        String cron = customRedisCacheConfig.getCleanTaskCron();
        taskScheduler.schedule(this::cleanCache, new CronTrigger(cron));
    }

    public void cleanCache() {
        Set<String> cacheNameSet = CacheHolder.getAndClear();
        if (CollectionUtils.isEmpty(cacheNameSet)) {
            return;
        }
        for (String cacheNameKey : cacheNameSet) {
            stringRedisTemplate.delete(cacheNameKey);
        }
        stringRedisTemplate.delete(CACHE_NAME_OF_KEY_HASH_KEY);
    }
}
