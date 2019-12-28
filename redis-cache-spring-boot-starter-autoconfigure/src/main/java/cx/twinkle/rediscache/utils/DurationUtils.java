package cx.twinkle.rediscache.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author twinkle
 * @version 2019/12/27 14:26
 */
public class DurationUtils {
    private static final Logger log = LoggerFactory.getLogger(DurationUtils.class);
    /**
     * 缓存过期时间，简化 Duration 格式的匹配正则
     */
    private static final Pattern DURATION_FORMAT_PATTERN = Pattern.compile("(?:([0-9]+)D)?(?:([0-9]+)H)?(?:([0-9]+)M)?(?:([0-9]+)S)?",
            Pattern.CASE_INSENSITIVE);

    /**
     * 从指定正则的 Matcher 中解析出 Duration
     *
     * @param str 待解析的字符串
     * @return 解析出的Duration
     */
    public static Duration parseDuration(String str) {
        if (StringUtils.isEmpty(str)) {
            return Duration.ZERO;
        }
        Matcher matcher = DURATION_FORMAT_PATTERN.matcher(str);
        if (!matcher.find()) {
            log.warn("Duration格式不正确！待解析的字符串 duration = {}，使用默认过期时间 2H ！", str);
            return Duration.ofHours(2L);
        }
        String dayStr = matcher.group(1);
        String hourStr = matcher.group(2);
        String minuteStr = matcher.group(3);
        String secondStr = matcher.group(4);
        String duration = "P{DAY}DT{HOUR}H{MINUTE}M{SECONDS}S"
                .replace("{DAY}", StringUtils.isEmpty(dayStr) ? "0" : dayStr)
                .replace("{HOUR}", StringUtils.isEmpty(hourStr) ? "0" : hourStr)
                .replace("{MINUTE}", StringUtils.isEmpty(minuteStr) ? "0" : minuteStr)
                .replace("{SECONDS}", StringUtils.isEmpty(secondStr) ? "0.0" : secondStr);
        try {
            return Duration.parse(duration);
        } catch (Exception e) {
            log.warn("解析 Duration 失败！使用默认 2H！");
        }
        return Duration.ofHours(2L);
    }
}
