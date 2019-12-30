package cx.twinkle.rediscache.cache;

import java.time.Duration;

/**
 * @author twinkle
 * @version 2019/12/30 18:53
 */
public interface RedisCacheService {
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
