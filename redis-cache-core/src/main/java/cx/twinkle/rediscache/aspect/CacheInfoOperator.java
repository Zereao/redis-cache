package cx.twinkle.rediscache.aspect;

import cx.twinkle.rediscache.annotation.CacheEvict;
import cx.twinkle.rediscache.annotation.CachePut;
import cx.twinkle.rediscache.annotation.Cacheable;
import cx.twinkle.rediscache.annotation.RedisCache;
import cx.twinkle.rediscache.cache.SpelParser;
import cx.twinkle.rediscache.dto.MethodCacheInfo;
import cx.twinkle.rediscache.utils.DurationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * @author twinkle
 * @version 2019/12/27 21:12
 */
public class CacheInfoOperator {
    private static final Logger log = LoggerFactory.getLogger(CacheInfoOperator.class);

    private SpelParser spelParser;
    private Integer maxParamNum = 5;

    public CacheInfoOperator(BeanFactory beanFactory) {
        this.spelParser = new SpelParser(beanFactory);
    }

    /**
     * 根据 缓存名、方法名，参数，生成 缓存的 key
     * <p>
     * Key 的格式为：缓存名::方法名-参数toString()
     * 如果参数个数大于5，或者参数包含集合对象，则使用MD5算法对 参数的toString() 进行处理
     *
     * @param cacheName  缓存名
     * @param methodName 方法名
     * @param params     方法的参数，本身是一个 可变参数
     * @return 缓存的key
     * @apiNote 为什么不适用 Hash 结构存储同一个缓存名下面的 key-value对？
     * ------>  因为 Hash 结构的数据不支持 自动过期。所以就采用了 :: 分级
     */
    public String generateCacheKey(String cacheName, String methodName, Object... params) {
        StringBuilder keyBuilder = new StringBuilder(cacheName).append("::").append(methodName).append("-v1_0");
        if (params == null || params.length <= 0) {
            log.debug("获取缓存key，方法 {} 的参数体为空！", methodName);
            return keyBuilder.toString();
        }
        keyBuilder.append("-");
        if (this.paramsTooLong(params, this.maxParamNum)) {
            log.info("获取缓存key，方法 {} 的参数个数大于【{}】个 或存在集合，采用MD5摘要~", methodName, this.maxParamNum);
            String paramMd5 = DigestUtils.md5DigestAsHex(Arrays.toString(params).getBytes());
            keyBuilder.append(paramMd5);
            return keyBuilder.toString();
        }
        for (Object param : params) {
            String paramStr = String.valueOf(param);
            keyBuilder.append(paramStr.replace(":", "-")).append("_");
        }
        return keyBuilder.toString();
    }

    /**
     * 如果方法参数个数大于 指定个数，或者 参数类型是 集合、数组，则对参数进行MD5摘要
     *
     * @param params      参数数组
     * @param maxParamNum 最大参数个数，默认为 5
     * @return 是否对参数MD5摘要
     */
    private boolean paramsTooLong(Object[] params, Integer maxParamNum) {
        if (params.length > maxParamNum) {
            return true;
        }
        for (Object param : params) {
            if (param instanceof Iterable || param instanceof Map || param.getClass().isArray()) {
                return true;
            }
        }
        return false;
    }

    MethodCacheInfo getCacheInfoWhenRead(Method method, Object... params) {
        return this.getCacheInfo(method, Cacheable.class, Cacheable::cache, Cacheable::key, Cacheable::expire, params);
    }

    MethodCacheInfo getCacheInfoWhenPut(Method method, Object... params) {
        return this.getCacheInfo(method, CachePut.class, CachePut::cache, CachePut::key, CachePut::expire, params);
    }

    private <T extends Annotation> MethodCacheInfo getCacheInfo(Method method, Class<T> cls, Function<T, String> f1,
                                                                Function<T, String> f2, Function<T, String> f3, Object... params) {
        String methodName = method.getName();
        String cacheName = this.getValue(method, cls, f1, RedisCache::cache);
        if (!StringUtils.isEmpty(cacheName)) {
            log.debug("从方法 {} 上解析到需要使用缓存名称：{}", methodName, cacheName);
        } else {
            log.debug("从方法 {} 中解析未解析到 缓存名！使用默认缓存名生成策略 - MD5摘要！", methodName);
            cacheName = DigestUtils.md5DigestAsHex(method.toGenericString().getBytes());
        }
        String cacheKey = this.getValue(method, cls, f2, null);
        if (!StringUtils.isEmpty(cacheKey)) {
            log.debug("从方法 {} 上解析到需要使用缓存Key：{}", methodName, cacheKey);
        } else {
            log.debug("从方法 {} 中解析未解析到 缓存Key！使用默认缓存Key生成策略！", methodName);
            cacheKey = this.generateCacheKey(cacheName, methodName, params);
        }
        Duration expire = Duration.ZERO;
        String expireTime = this.getValue(method, cls, f3, RedisCache::expire);
        if (!StringUtils.isEmpty(expireTime)) {
            log.debug("从方法 {} 上解析到该缓存的过期时间：{}", methodName, expireTime);
            expire = DurationUtils.parseDuration(expireTime);
        }
        return new MethodCacheInfo(methodName, cacheName, cacheKey, expire);
    }

    MethodCacheInfo getCacheInfoWhenEvict(Method method) {
        String methodName = method.getName();
        String cacheName = this.getValue(method, CacheEvict.class, CacheEvict::cache, RedisCache::cache);
        if (StringUtils.isEmpty(cacheName)) {
            log.debug("从方法 {} 中解析未解析到 缓存名！", methodName);
            cacheName = null;
        } else {
            log.debug("从方法 {} 上解析到需要删除的缓存名称：{}", methodName, cacheName);
        }
        String cacheKey = this.getValue(method, CacheEvict.class, CacheEvict::key, null);
        if (StringUtils.isEmpty(cacheKey)) {
            log.debug("从方法 {} 中解析未解析到 缓存Key！", methodName);
            cacheKey = null;
        } else {
            log.debug("从方法 {} 上解析到需要使用缓存Key：{}", methodName, cacheKey);
        }
        return new MethodCacheInfo(methodName, cacheName, cacheKey);
    }

    List<String> parseEvictCacheKey(MethodCacheInfo cacheInfo, Method method, Object[] args) {
        String spel = cacheInfo.getCacheKey();
        String methodName = cacheInfo.getMethodName();
        if (StringUtils.isEmpty(spel)) {
            log.warn("方法 {} 对应的缓存的过期时间为空！", methodName);
        } else {
            @SuppressWarnings("unchecked")
            List<String> keyList = spelParser.parse(spel, method, List.class, args);
            if (CollectionUtils.isEmpty(keyList)) {
                log.warn("方法 {} 对应的缓存注解解析SPEL表达式得到的数据为空！SPEL = {}", methodName, spel);
            } else {
                log.info("从SPEL表达式【{}】中解析到KeyList = {}", spel, keyList);
                return keyList;
            }
        }
        return Collections.emptyList();
    }

    /**
     * 封装的方法，用户处理一些公共逻辑
     *
     * @param method 方法对象
     * @param cls    需要处理的注解的Class对象
     * @param f      对注解需要执行的函数操作
     * @param <T>    泛型，约束了 T 的类型必须为 注解类型
     * @return 处理结果
     */
    <T extends Annotation, R> R getValue(Method method, Class<T> cls, Function<T, R> f, Function<RedisCache, R> f2) {
        R f1Result;
        if (method.isAnnotationPresent(cls)) {
            f1Result = f.apply(method.getAnnotation(cls));
            if (!StringUtils.isEmpty(f1Result)) {
                return f1Result;
            }
        }
        Class<?> declaringClass;
        if (f2 != null && (declaringClass = method.getDeclaringClass()).isAnnotationPresent(RedisCache.class)) {
            RedisCache redisCache = declaringClass.getAnnotation(RedisCache.class);
            R redisCacheResult = f2.apply(redisCache);
            if (!StringUtils.isEmpty(redisCacheResult)) {
                return redisCacheResult;
            }
        }
        return null;
    }

    /**
     * 判断结果是否是Null
     *
     * @param result 结果
     * @return true OR false
     */
    boolean isNullResult(Object result) {
        return StringUtils.isEmpty(result)
                || (result instanceof Collection && CollectionUtils.isEmpty((Collection<?>) result))
                || (result instanceof Map && CollectionUtils.isEmpty((Map<?, ?>) result));
    }

    /**
     * 仅用于这里的线程池
     */
    private static ExecutorService executor;

    static {
        executor = new ThreadPoolExecutor(
                8,
                15,
                5L,
                TimeUnit.MINUTES,
                new LinkedBlockingDeque<>(),
                new ThreadPoolExecutor.DiscardPolicy());
    }

    void execute(Runnable task) {
        executor.execute(task);
    }

    /**
     * 剥离出的公共方法，主要用于日志打印
     *
     * @param methodName 方法名称
     * @param cacheName  缓存名称
     * @param redisKey   Redis中缓存的KEY
     * @param params     方法的参数
     * @return 日志内容，直接打印即可
     */
    String buildLogInfo(String methodName, String cacheName, String redisKey, Object[] params) {
        return "{\n"
                + "\t方法名称：" + methodName + ",\n"
                + "\t缓存名称：" + cacheName + ",\n"
                + "\tredisKey：" + redisKey + ",\n"
                + "\t请求参数：" + Arrays.toString(params) + ",\n"
                + "}";
    }

    public Integer getMaxParamNum() {
        return maxParamNum;
    }

    public void setMaxParamNum(Integer maxParamNum) {
        this.maxParamNum = maxParamNum;
    }
}
