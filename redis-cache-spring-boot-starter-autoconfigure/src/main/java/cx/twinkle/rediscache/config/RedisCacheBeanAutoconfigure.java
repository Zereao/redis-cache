package cx.twinkle.rediscache.config;

import cx.twinkle.rediscache.aspects.CacheAspectHelper;
import cx.twinkle.rediscache.aspects.RedisCacheAspect;
import cx.twinkle.rediscache.service.CacheService;
import cx.twinkle.rediscache.service.SerializeService;
import cx.twinkle.rediscache.service.SpelParseService;
import cx.twinkle.rediscache.service.impl.CacheServiceImpl;
import cx.twinkle.rediscache.service.impl.SerializeServiceImpl;
import cx.twinkle.rediscache.service.impl.SpelParseServiceImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * @author twinkle
 * @version 2019/12/27 16:09
 */
@Configuration
@EnableConfigurationProperties({CustomRedisCacheConfig.class})
@Import({CacheSerializerConfig.class, TaskSchedulerConfig.class})
public class RedisCacheBeanAutoconfigure {
    @Bean
    public RedisCacheAspect redisCacheAspect() {
        return new RedisCacheAspect();
    }

    @Bean
    public CacheAspectHelper cacheAspectHelper() {
        return new CacheAspectHelper();
    }

    @Bean
    @ConditionalOnMissingBean(CacheServiceImpl.class)
    public CacheService cacheService() {
        return new CacheServiceImpl();
    }

    @Bean
    @ConditionalOnMissingBean(SerializeServiceImpl.class)
    public SerializeService serializeService() {
        return new SerializeServiceImpl();
    }

    @Bean
    @ConditionalOnMissingBean(SpelExpressionParser.class)
    public SpelExpressionParser spelExpressionParser() {
        return new SpelExpressionParser();
    }

    @Bean
    @ConditionalOnMissingBean(SpelParseServiceImpl.class)
    public SpelParseService spelParseService() {
        return new SpelParseServiceImpl();
    }
}
