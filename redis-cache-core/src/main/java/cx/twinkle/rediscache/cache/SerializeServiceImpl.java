package cx.twinkle.rediscache.cache;

import cx.twinkle.rediscache.utils.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author twinkle
 * @version 2019/12/30 18:58
 */
public class SerializeServiceImpl implements SerializeService {
    private static final Logger log = LoggerFactory.getLogger(SerializeServiceImpl.class);

    private GenericJackson2JsonRedisSerializer jacksonSerializer = new GenericJackson2JsonRedisSerializer();
    private JdkSerializationRedisSerializer jdkSerializer = new JdkSerializationRedisSerializer();

    /**
     * java.util.HashMap 的内部内 KetSet 类的 class对象，该类访问权限为 default
     */
    private static final Class<?> KEY_SET_CLASS = new HashMap<>(1).keySet().getClass();

    /**
     * Map 类型的数据使用JDK序列化，Redis中存储的是 Arrays.toString(bytes)。
     */
    private static final Pattern MAP_BYTE_ARRAY_PATTERN = Pattern.compile("\\[(.*)]");

    @Override
    public String serialize(Object obj) {
        if (obj instanceof Serializable) {
            byte[] serializedBytes = jdkSerializer.serialize(obj);
            return Arrays.toString(serializedBytes);
        }
        if (!ReflectionUtils.hasDefaultConstructor(obj)) {
            if (KEY_SET_CLASS.isInstance(obj)) {
                obj = new HashSet<>((Set<?>) obj);
            } else {
                log.error("存在一个结果无法序列化！resultObj = {}", obj);
                return "";
            }
        }
        byte[] serializedBytes = jacksonSerializer.serialize(obj);
        return (serializedBytes == null || serializedBytes.length == 0) ? "" : new String(serializedBytes);
    }


    @Override
    public Object deserialize(String str) {
        if (StringUtils.isEmpty(str)) {
            return null;
        }
        Matcher mapMatcher = MAP_BYTE_ARRAY_PATTERN.matcher(str);
        boolean isMapObj = mapMatcher.find();
        if (!isMapObj) {
            return jacksonSerializer.deserialize(str.getBytes());
        }
        // Map对象，使用JDK反序列化
        String byteStr = mapMatcher.group(1);
        String[] byteStrArray = byteStr.split(", ");
        int len;
        if ((len = byteStrArray.length) <= 0) {
            return null;
        }
        byte[] bytes = new byte[len];
        for (int i = 0; i < len; i++) {
            bytes[i] = Byte.parseByte(byteStrArray[i]);
        }
        return jdkSerializer.deserialize(bytes);
    }
}
