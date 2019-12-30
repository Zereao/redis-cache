package cx.twinkle.rediscache.config;

import cx.twinkle.rediscache.task.CacheCleanTask;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * @author twinkle
 * @version 2019/12/28 17:32
 */
@Configuration
public class TaskSchedulerConfig {
    @Bean
    @ConditionalOnMissingBean(ThreadPoolTaskScheduler.class)
    public TaskScheduler taskScheduler() {
        return new ThreadPoolTaskScheduler();
    }

    @Bean
    public CacheCleanTask cacheCleanTask(CustomCacheConfig customCacheConfig, TaskScheduler taskScheduler, StringRedisTemplate stringRedisTemplate) {
        CacheCleanTask task = new CacheCleanTask(taskScheduler, stringRedisTemplate);
        String cron = customCacheConfig.getCleanTaskCron();
        task.setCron(cron);
        return task;
    }
}
