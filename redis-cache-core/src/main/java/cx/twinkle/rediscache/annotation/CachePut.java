package cx.twinkle.rediscache.annotation;

import java.lang.annotation.*;

/**
 * @author twinkle
 * @version 2019/12/30 18:37
 */
@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CachePut {
    /**
     * 缓存key
     * <p>
     * 当未指定缓存 key 时，使用统一规则生成key
     */
    String key() default "";

    /**
     * 过期时间，默认不过期；
     */
    String expire() default "";

    /**
     * 缓存名称
     */
    String cache() default "";
}
