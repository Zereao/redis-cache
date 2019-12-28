package cx.twinkle.rediscache.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;

/**
 * @author twinkle
 * @version 2019/12/28 22:37
 */
@Configuration
public class CacheSerializerConfig {
    @Bean
    @ConditionalOnMissingBean(GenericJackson2JsonRedisSerializer.class)
    public GenericJackson2JsonRedisSerializer genericJackson2JsonRedisSerializer() {
        return new GenericJackson2JsonRedisSerializer();
    }

    @Bean
    @ConditionalOnMissingBean(JdkSerializationRedisSerializer.class)
    public JdkSerializationRedisSerializer jdkSerializationRedisSerializer() {
        return new JdkSerializationRedisSerializer();
    }
}
