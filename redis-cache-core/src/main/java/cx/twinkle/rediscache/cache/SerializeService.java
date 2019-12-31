package cx.twinkle.rediscache.cache;

/**
 * @author twinkle
 * @version 2019/12/30 18:58
 */
public interface SerializeService {
    /**
     * 将对象序列化为字符串;
     * Map 使用 JDK 序列化；否则使用 Jackson 序列化
     *
     * @param obj 需要序列化的对象
     * @return 得到的字符串
     */
    String serialize(Object obj);

    /**
     * 反序列化
     *
     * @param str 需要反序列化的字符串
     * @return 反序列化得到的对象
     */
    Object deserialize(String str);
}
