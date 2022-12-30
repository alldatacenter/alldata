package com.alibaba.tesla.appmanager.spring.config;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

/**
 * fastjson 全局配置覆盖
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Configuration
@ConditionalOnClass(JSON.class)
public class FastjsonConfig {

    static {
        JSON.DEFAULT_GENERATE_FEATURE |= SerializerFeature.IgnoreErrorGetter.getMask();
    }
}