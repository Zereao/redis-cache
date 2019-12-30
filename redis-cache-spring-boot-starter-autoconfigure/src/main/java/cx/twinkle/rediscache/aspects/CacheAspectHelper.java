package cx.twinkle.rediscache.aspects;

import cx.twinkle.rediscache.annotation.CacheEvict;
import cx.twinkle.rediscache.annotation.Cacheable;
import cx.twinkle.rediscache.annotation.RedisCache;
import cx.twinkle.rediscache.service.CacheService;
import cx.twinkle.rediscache.service.SpelParseService;
import cx.twinkle.rediscache.utils.DurationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
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
public class CacheAspectHelper {
    private static final Logger log = LoggerFactory.getLogger(CacheAspectHelper.class);

    @Resource
    private CacheService cacheService;
    @Resource
    private SpelParseService spelParseService;

    MethodCacheInfo getCacheInfoWhenRead(Method method, Object... params) {
        String methodName = method.getName();
        String cacheName = this.getValue(method, Cacheable.class, Cacheable::cache, RedisCache::cache);
        if (!StringUtils.isEmpty(cacheName)) {
            log.debug("从方法 {} 上解析到需要使用缓存名称：{}", methodName, cacheName);
        } else {
            log.debug("从方法 {} 中解析未解析到 缓存名！使用默认缓存名生成策略 - MD5摘要！", methodName);
            cacheName = DigestUtils.md5DigestAsHex(method.toGenericString().getBytes());
        }
        String cacheKey = this.getValue(method, Cacheable.class, Cacheable::key, null);
        if (!StringUtils.isEmpty(cacheKey)) {
            log.debug("从方法 {} 上解析到需要使用缓存Key：{}", methodName, cacheKey);
        } else {
            log.debug("从方法 {} 中解析未解析到 缓存Key！使用默认缓存Key生成策略！", methodName);
            cacheKey = cacheService.generateCacheKey(cacheName, methodName, params);
        }
        Duration expire = Duration.ZERO;
        String expireTime = this.getValue(method, Cacheable.class, Cacheable::expire, RedisCache::expire);
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
            List<String> keyList = spelParseService.parse(spel, method, List.class, args);
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
    @SuppressWarnings("rawtypes")
    boolean isNullResult(Object result) {
        return StringUtils.isEmpty(result)
                || (result instanceof Collection && CollectionUtils.isEmpty((Collection) result))
                || (result instanceof Map && CollectionUtils.isEmpty((Map) result));
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
}
