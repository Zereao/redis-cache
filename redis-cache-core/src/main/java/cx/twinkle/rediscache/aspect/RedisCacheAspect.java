package cx.twinkle.rediscache.aspect;

import cx.twinkle.rediscache.annotation.CacheEvict;
import cx.twinkle.rediscache.cache.RedisCacheService;
import cx.twinkle.rediscache.cache.RedisCacheServiceImpl;
import cx.twinkle.rediscache.dto.MethodCacheInfo;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Method;
import java.util.List;

/**
 * AOP核心类，提供方法拦截
 *
 * @author twinkle
 * @version 2019/12/27 14:50
 */
@Aspect
public class RedisCacheAspect {
    private static final Logger log = LoggerFactory.getLogger(RedisCacheAspect.class);

    private CacheInfoOperator operator;
    private RedisCacheService cacheService;

    public RedisCacheAspect(Integer maxParamNum, BeanFactory beanFactory, StringRedisTemplate stringRedisTemplate) {
        this.operator = new CacheInfoOperator(beanFactory);
        this.operator.setMaxParamNum(maxParamNum);
        this.cacheService = new RedisCacheServiceImpl(stringRedisTemplate);
    }

    /**
     * 用于处理 缓存 过期
     */
    @Before("@annotation(cx.twinkle.rediscache.annotation.CacheEvict)")
    public void cacheEvict(JoinPoint joinPoint) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        MethodCacheInfo cacheInfo = operator.getCacheInfoWhenEvict(method);
        String cacheName = cacheInfo.getCacheName();
        Boolean allEntries = operator.getValue(method, CacheEvict.class, CacheEvict::allEntries, null);
        if (allEntries) {
            cacheService.deleteByCacheName(cacheName);
            log.info("缓存 {} 下所有的 key 都删除完毕！", cacheName);
            return;
        }
        List<String> keyList = operator.parseEvictCacheKey(cacheInfo, method, joinPoint.getArgs());
        if (CollectionUtils.isEmpty(keyList)) {
            log.info("没有任何缓存被清理，请关注 @CacheEvict 注解是否正确设置了参数！");
            return;
        }
        String[] keys = keyList.toArray(new String[0]);
        cacheService.deleteByKey(keys);
        log.info("KEY = {} 对应的缓存删除成功！", keyList);
    }

    /**
     * 用于处理 缓存写入、读取
     */
    @Around("@annotation(cx.twinkle.rediscache.annotation.Cacheable)")
    public Object cacheable(ProceedingJoinPoint pjp) throws Throwable {
        Object[] params = pjp.getArgs();
        Method targetMethod = ((MethodSignature) pjp.getSignature()).getMethod();
        MethodCacheInfo cacheInfo = operator.getCacheInfoWhenRead(targetMethod, params);
        String cacheKey = cacheInfo.getCacheKey(),
                methodName = cacheInfo.getMethodName(),
                cacheName = cacheInfo.getCacheName();
        Object result = cacheService.getFromRedis(cacheKey);
        if (result != null) {
            log.info("方法 {} 通过Redis缓存获取到结果：\n{}", methodName,
                    operator.buildLogInfo(methodName, cacheName, cacheKey, params));
            return result;
        }
        // 如果没有读取到缓存，则执行方法，并且将结果集写入Redis
        Object resultAfterRun = pjp.proceed();
        if (operator.isNullResult(resultAfterRun)) {
            return resultAfterRun;
        }
        // 异步写入Redis
        operator.execute(() -> {
            cacheService.insert2Redis(cacheKey, resultAfterRun, cacheInfo.getExpireTime(), cacheName);
            log.info("方法 {} 结果集已写入Redis：\n{}", methodName,
                    operator.buildLogInfo(methodName, cacheName, cacheKey, params));
        });
        return resultAfterRun;
    }

    @Around("@annotation(cx.twinkle.rediscache.annotation.CachePut)")
    public Object cachePut(ProceedingJoinPoint pjp) throws Throwable {
        Object result = pjp.proceed();
        if (operator.isNullResult(result)) {
            return null;
        }
        Object[] params = pjp.getArgs();
        Method targetMethod = ((MethodSignature) pjp.getSignature()).getMethod();
        MethodCacheInfo cacheInfo = operator.getCacheInfoWhenPut(targetMethod, params);
        String cacheKey = cacheInfo.getCacheKey(),
                methodName = cacheInfo.getMethodName(),
                cacheName = cacheInfo.getCacheName();
        operator.execute(() -> {
            cacheService.insert2Redis(cacheKey, result, cacheInfo.getExpireTime(), cacheName);
            log.info("方法 {} 结果集已写入Redis：\n{}", methodName,
                    operator.buildLogInfo(methodName, cacheName, cacheKey, params));
        });
        return result;
    }
}
