package datart.core.common;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class CacheFactory {

    private static final String CACHE_IMPL_CLASS_NAME = "cacheImpl";

    private static final String DEFAULT_CACHE = "datart.server.service.impl.RedisCacheImpl";

    private static Cache cache;

    public static Cache getCache() {
        if (cache != null) {
            return cache;
        }
        try {
            String className = Application.getProperty(CACHE_IMPL_CLASS_NAME);
            if (StringUtils.isBlank(className)) {
                className = DEFAULT_CACHE;
            }
            cache = (Cache) Application.getBean(Class.forName(className));
            return cache;
        } catch (Exception e) {
            log.error("get cache instance error", e);
        }
        return null;
    }

}