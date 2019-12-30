package cx.twinkle.rediscache.annotation;

import java.lang.annotation.*;

/**
 * @author twinkle
 * @version 2019/12/27 14:43
 */
@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheEvict {
    /**
     * 需要过期的Key，支持 SPEL表达式
     */
    String key() default "";

    /**
     * 缓存名称
     */
    String cache() default "";

    /**
     * 是否使当前缓存下所有key失效
     */
    boolean allEntries() default false;
}
