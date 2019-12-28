package cx.twinkle.rediscache.aspects;

import cx.twinkle.rediscache.annotation.CacheEvict;
import cx.twinkle.rediscache.service.CacheService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.util.List;

/**
 * @author twinkle
 * @version 2019/12/27 14:50
 */
@Aspect
public class RedisCacheAspect {
    private static final Logger log = LoggerFactory.getLogger(RedisCacheAspect.class);

    @Resource
    private CacheService cacheService;
    @Resource(type = CacheAspectHelper.class)
    private CacheAspectHelper helper;

    /**
     * 切点，所有方法
     */
    @Pointcut("execution(public * *(..))")
    public void allPublicMethods() {}

    /**
     * 切点，被 @Cacheable 标注的所有方法
     */
    @Pointcut("@annotation(cx.twinkle.rediscache.annotation.Cacheable)")
    public void cacheableAnnotated() {}

    /**
     * 切点，被 @CacheEvict 标注的所有方法
     */
    @Pointcut("@annotation(cx.twinkle.rediscache.annotation.CacheEvict)")
    public void cacheEvictAnnotated() {}

    /**
     * 用于处理 缓存写入、读取
     */
    @Around("allPublicMethods() && cacheableAnnotated()")
    public Object cacheable(ProceedingJoinPoint pjp) throws Throwable {
        Object[] params = pjp.getArgs();
        Method targetMethod = ((MethodSignature) pjp.getSignature()).getMethod();
        MethodCacheInfo cacheInfo = helper.getCacheInfoWhenRead(targetMethod, params);
        String cacheKey = cacheInfo.getCacheKey(),
                methodName = cacheInfo.getMethodName(),
                cacheName = cacheInfo.getCacheName();
        Object result = cacheService.getFromRedis(cacheKey);
        if (result != null) {
            log.info("方法 {} 通过Redis缓存获取到结果：\n{}", methodName,
                    helper.buildLogInfo(methodName, cacheName, cacheKey, params));
            return result;
        }
        // 如果没有读取到缓存，则执行方法，并且将结果集写入Redis
        Object resultAfterRun = pjp.proceed();
        // 异步写入Redis
        helper.execute(() -> {
            if (helper.isNullResult(resultAfterRun)) {
                return;
            }
            cacheService.insert2Redis(cacheKey, resultAfterRun, cacheInfo.getExpireTime(), cacheInfo.getCacheName());
            log.info("方法 {} 结果集已写入Redis：\n{}", methodName,
                    helper.buildLogInfo(methodName, cacheName, cacheKey, params));
        });
        return resultAfterRun;
    }

    /**
     * 用于处理 缓存 过期
     */
    @Before("allPublicMethods() && cacheEvictAnnotated()")
    public void cacheEvict(JoinPoint joinPoint) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        MethodCacheInfo cacheInfo = helper.getCacheInfoWhenEvict(method);
        String cacheName = cacheInfo.getCacheName();
        Boolean allEntries = helper.getValue(method, CacheEvict.class, CacheEvict::allEntries, null);
        if (allEntries) {
            cacheService.deleteByCacheName(cacheName);
            log.info("缓存 {} 下所有的 key 都删除完毕！", cacheName);
            return;
        }
        List<String> keyList = helper.parseEvictCacheKey(cacheInfo, method, joinPoint.getArgs());
        if (CollectionUtils.isEmpty(keyList)) {
            log.info("没有任何缓存被清理，请关注 @CacheEvict 注解是否正确设置了参数！");
            return;
        }
        String[] keys = keyList.toArray(new String[0]);
        cacheService.deleteByKey(keys);
        log.info("KEY = {} 对应的缓存删除成功！", keyList);
    }
}
