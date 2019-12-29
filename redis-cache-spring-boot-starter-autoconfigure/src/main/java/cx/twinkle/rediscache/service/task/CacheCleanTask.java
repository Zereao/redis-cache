package cx.twinkle.rediscache.service.task;

import cx.twinkle.rediscache.config.CustomCacheConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Set;

import static cx.twinkle.rediscache.service.impl.CacheServiceImpl.CACHE_NAME_OF_KEY_HASH_KEY;

/**
 * @author twinkle
 * @version 2019/12/28 16:58
 */
public class CacheCleanTask {
    private static final Logger log = LoggerFactory.getLogger(CacheCleanTask.class);

    @Resource
    private TaskScheduler taskScheduler;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private CustomCacheConfig customCacheConfig;

    @PostConstruct
    public void run() {
        String cron = customCacheConfig.getCleanTaskCron();
        taskScheduler.schedule(this::cleanCache, new CronTrigger(cron));
        log.info("Redis缓存 KeySet、CacheNameHash清理 定时任务注册成功！cron = {}", cron);
    }

    public void cleanCache() {
        log.info("******* 准备开始清理 KeySet、CacheNameHash~ *******");
        Set<String> cacheNameSet = CacheHolder.getAndClear();
        if (!CollectionUtils.isEmpty(cacheNameSet)) {
            for (String cacheNameKey : cacheNameSet) {
                stringRedisTemplate.delete(cacheNameKey);
            }
        }
        stringRedisTemplate.delete(CACHE_NAME_OF_KEY_HASH_KEY);
        log.info("Redis缓存 KeySet、CacheNameHash清理 定时任务执行完毕！相关缓存清理成功！");
    }
}
