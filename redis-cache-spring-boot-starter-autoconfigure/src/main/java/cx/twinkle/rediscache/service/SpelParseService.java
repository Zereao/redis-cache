package cx.twinkle.rediscache.service;

import java.lang.reflect.Method;

/**
 * @author twinkle
 * @version 2019/12/28 11:25
 */
public interface SpelParseService {
    /**
     * 解析SPEL 表达式，将结果解析为泛型类型
     *
     * @param expression SPEL表达式
     * @param method     切入点方法
     * @param cls        返回类型Class对象
     * @param args       方法参数
     * @param <T>        泛型
     * @return 解析结果
     */
    <T> T parse(String expression, Method method, Class<T> cls, Object... args);
}
