package cx.twinkle.rediscache.task;

import cx.twinkle.rediscache.cache.CacheHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.Set;

import static cx.twinkle.rediscache.cache.RedisCacheServiceImpl.CACHE_NAME_OF_KEY_HASH_KEY;

/**
 * @author twinkle
 * @version 2019/12/30 19:08
 */
public class CacheCleanTask {
    private static final Logger log = LoggerFactory.getLogger(CacheCleanTask.class);

    private TaskScheduler taskScheduler;
    private StringRedisTemplate stringRedisTemplate;
    private String cron = "0 10 0 * * ?";

    public CacheCleanTask(TaskScheduler taskScheduler, StringRedisTemplate stringRedisTemplate) {
        this.taskScheduler = taskScheduler;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @PostConstruct
    public void run() {
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

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }
}
