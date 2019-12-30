package cx.twinkle.rediscache.annotation;

import java.lang.annotation.*;

/**
 * @author twinkle
 * @version 2019/12/27 14:45
 */
@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RedisCache {
    /**
     * 缓存名称
     * <p>
     * 当 标注 @Cacheable 注解的方法所属的类上标注了 @RedisCache，并且 @Cacheable 也指定了 cache 属性时，优先解析 cache 属性
     */
    String cache() default "";

    /**
     * 过期时间，默认不过期；
     * <p>
     * 使用 Duration 解析：
     * <p>
     * D：天；1D 表示 1天过期
     * H：小时；23H 表示 23小时过期
     * M：分钟；5M 表示 5分钟过期
     * S：秒；30S 表示 30秒过期
     *
     * @apiNote 上面的 D/H/M/S 不区分大小写：23H 与 23h 等效。
     * @apiNote 匹配实现参考：{@link cx.twinkle.rediscache.utils.DurationUtils#parseDuration(String str)}
     */
    String expire() default "";
}
