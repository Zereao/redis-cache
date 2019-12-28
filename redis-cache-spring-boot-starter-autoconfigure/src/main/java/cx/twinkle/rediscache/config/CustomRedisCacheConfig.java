package cx.twinkle.rediscache.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author twinkle
 * @version 2019/12/28 17:55
 */
@ConfigurationProperties(prefix = "ct.cache")
public class CustomRedisCacheConfig {
    /**
     * 自定义生成Key时，当犯法的参数个数大于 maxParamNum (默认为5)个时，
     * 就对参数进行MD5摘要，防止缓存Key过长
     */
    private Integer maxParamNum = 5;
    /**
     * 自定义 清理CacheKeySet CacheNameHash 的任务执行时间，默认：每天00:10
     */
    private String cleanTaskCron = "0 10 0 * * ?";

    public Integer getMaxParamNum() {
        return maxParamNum;
    }

    public void setMaxParamNum(Integer maxParamNum) {
        this.maxParamNum = maxParamNum;
    }

    public String getCleanTaskCron() {
        return cleanTaskCron;
    }

    public void setCleanTaskCron(String cleanTaskCron) {
        this.cleanTaskCron = cleanTaskCron;
    }
}
