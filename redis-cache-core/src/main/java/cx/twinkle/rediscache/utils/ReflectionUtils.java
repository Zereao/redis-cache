package cx.twinkle.rediscache.utils;

/**
 * @author twinkle
 * @version 2019/12/31 14:31
 */
public class ReflectionUtils {

    public static boolean hasDefaultConstructor(Object obj) {
        try {
            return null != obj.getClass().getDeclaredConstructor();
        } catch (NoSuchMethodException ignored) {
            return false;
        }
    }
}
