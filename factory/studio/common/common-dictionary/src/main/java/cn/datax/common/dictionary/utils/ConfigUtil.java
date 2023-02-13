package cn.datax.common.dictionary.utils;

import cn.datax.common.core.RedisConstant;
import cn.datax.common.redis.service.RedisService;
import cn.datax.common.utils.SpringContextHolder;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class ConfigUtil {

    private ConfigUtil() {}

    private static volatile ConfigUtil instance;

    public static ConfigUtil getInstance() {
        if(instance == null) {
            synchronized (ConfigUtil.class) {
                if(instance == null) {
                    instance = new ConfigUtil();
                }
            }
        }
        return instance;
    }

    private RedisService redisService = SpringContextHolder.getBean(RedisService.class);

    /**
     * 获取参数
     * @param code
     */
    public Object getConfig(String code) {
        String key = RedisConstant.SYSTEM_CONFIG_KEY;
        Object o = redisService.hget(key, code);
        return Optional.ofNullable(o).orElse(null);
    }
}
