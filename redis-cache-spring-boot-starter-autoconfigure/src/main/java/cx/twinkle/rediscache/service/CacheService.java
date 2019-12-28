package cx.twinkle.rediscache.service;

import java.time.Duration;

/**
 * @author twinkle
 * @version 2019/12/27 15:03
 */
public interface CacheService {
    /**
     * 根据 缓存名、方法名，参数，生成 缓存的 key
     * <p>
     * Key 的格式为：缓存名::方法名-参数toString()
     * 如果参数个数大于5，则使用MD5算法对 参数toString() 进行处理
     *
     * @param cacheName  缓存名
     * @param methodName 方法名
     * @param params     方法的参数，本身是一个 可变参数
     * @return 缓存的key
     * @apiNote 为什么不适用 Hash 结构存储同一个缓存名下面的 key-value对？
     * ------>  因为 Hash 结构的数据不支持 自动过期。所以就采用了 :: 分级
     */
    String generateCacheKey(String cacheName, String methodName, Object... params);

    /**
     * 根据Key，从Redis中读取数据，并且反序列化为对应对象
     *
     * @param key 缓存key
     * @return 得到的反序列化后的对象
     */
    Object getFromRedis(String key);

    /**
     * 将数据写入Redis，并且额外记录下key值
     *
     * @param key       需要写入的key
     * @param value     需要序列化后的写入Redis的数据
     * @param duration  过期时间
     * @param cacheName 缓存名称
     */
    void insert2Redis(String key, Object value, Duration duration, String cacheName);

    /**
     * 根据Key,删除Key对应的缓存
     *
     * @param keys 需要删除的key，可变参数
     */
    void deleteByKey(String... keys);

    /**
     * 根据缓存名称，删除该缓存下所有的key
     *
     * @param cacheName 缓存名称
     */
    void deleteByCacheName(String cacheName);
}
