package cx.twinkle.rediscache.aspects;

import java.time.Duration;

/**
 * @author twinkle
 * @version 2019/12/28 09:43
 */
class MethodCacheInfo {
    /**
     * 缓存对应方法的名称
     */
    private String methodName;
    /**
     * 缓存名称
     */
    private String cacheName;
    /**
     * 缓存Key
     */
    private String cacheKey;
    /**
     * 过期时间
     */
    private Duration expireTime;

    public MethodCacheInfo(String methodName, String cacheName, String cacheKey) {
        this.methodName = methodName;
        this.cacheName = cacheName;
        this.cacheKey = cacheKey;
    }

    public MethodCacheInfo(String methodName, String cacheName, String cacheKey, Duration expireTime) {
        this.methodName = methodName;
        this.cacheName = cacheName;
        this.cacheKey = cacheKey;
        this.expireTime = expireTime;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getCacheName() {
        return cacheName;
    }

    public String getCacheKey() {
        return cacheKey;
    }

    public Duration getExpireTime() {
        return expireTime;
    }
}
