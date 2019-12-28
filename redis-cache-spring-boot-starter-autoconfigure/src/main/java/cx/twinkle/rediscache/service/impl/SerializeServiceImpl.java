package cx.twinkle.rediscache.service.impl;

import cx.twinkle.rediscache.service.SerializeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author twinkle
 * @version 2019/12/27 15:05
 */
public class SerializeServiceImpl implements SerializeService {
    private static final Logger log = LoggerFactory.getLogger(SerializeServiceImpl.class);

    @Resource(type = GenericJackson2JsonRedisSerializer.class)
    private GenericJackson2JsonRedisSerializer jacksonSerializer;
    @Resource(type = JdkSerializationRedisSerializer.class)
    private JdkSerializationRedisSerializer jdkSerializer;

    /**
     * java.util.HashMap 的内部内 KetSet 类的 class对象，该类访问权限为 default
     */
    @SuppressWarnings("rawtypes")
    private static final Class KEY_SET_CLASS = new HashMap(1).keySet().getClass();

    /**
     * Map 类型的数据使用JDK序列化，Redis中存储的是 Arrays.toString(bytes)。
     */
    private static final Pattern MAP_BYTE_ARRAY_PATTERN = Pattern.compile("\\[(.*)]");

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public String serialize(Object obj) {
        if (obj instanceof Map) {
            Map objMap = (Map) obj;
            return CollectionUtils.isEmpty(objMap) ? "" : this.serializeMap(objMap);
        }
        if (KEY_SET_CLASS.isInstance(obj)) {
            obj = new HashSet((Set) obj);
        }
        byte[] serializedBytes = jacksonSerializer.serialize(obj);
        return (serializedBytes == null || serializedBytes.length == 0) ? "" : new String(serializedBytes);
    }

    @Override
    public String serializeMap(Map<?, ?> objMap) {
        byte[] serializedBytes = jdkSerializer.serialize(objMap);
        if (serializedBytes == null || serializedBytes.length <= 0) {
            log.warn("存在一个非空的Map = {}使用JDK序列化后的结果byte数组为空！", objMap);
            return "";
        }
        return Arrays.toString(serializedBytes);
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
