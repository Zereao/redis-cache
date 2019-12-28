package cx.twinkle.rediscache.service.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author twinkle
 * @version 2019/12/28 16:53
 */
public class CacheHolder {
    private static final Logger log = LoggerFactory.getLogger(CacheHolder.class);
    /**
     * 运行时 记录缓存Key的 集合
     */
    private static final Set<String> RUNTIME_CACHE_NAME_SET = Collections.synchronizedSet(new HashSet<>());

    public static void add(String cacheName) {
        RUNTIME_CACHE_NAME_SET.add(cacheName);
    }

    public static Set<String> getAndClear() {
        if (CollectionUtils.isEmpty(RUNTIME_CACHE_NAME_SET)) {
            return Collections.emptySet();
        }
        Set<String> resultSet = new HashSet<>(RUNTIME_CACHE_NAME_SET);
        RUNTIME_CACHE_NAME_SET.clear();
        log.info("CacheHolder清理完毕！");
        return resultSet;
    }
}
