package cx.twinkle.rediscache.config;

import cx.twinkle.rediscache.aspect.RedisCacheAspect;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * @author twinkle
 * @version 2019/12/27 16:09
 */
@Configuration
@Import({TaskSchedulerConfig.class})
@EnableConfigurationProperties({CustomCacheConfig.class})
public class RedisCacheBeanAutoconfigure {
    @Bean
    public RedisCacheAspect redisCacheAspect(BeanFactory beanFactory, CustomCacheConfig customCacheConfig, StringRedisTemplate stringRedisTemplate) {
        Integer maxParamNum = customCacheConfig.getMaxParamNum();
        return new RedisCacheAspect(maxParamNum, beanFactory, stringRedisTemplate);
    }
}
