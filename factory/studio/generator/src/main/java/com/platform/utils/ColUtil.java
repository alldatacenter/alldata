
package com.platform.utils;

import org.apache.commons.configuration.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * sql字段转java
 *
 * @author AllDataDC
 * @date 2023-01-27
 */
public class ColUtil {
    private static final Logger log = LoggerFactory.getLogger(ColUtil.class);

    /**
     * 转换mysql数据类型为java数据类型
     *
     * @param type 数据库字段类型
     * @return String
     */
    static String cloToJava(String type) {
        Configuration config = getConfig();
        assert config != null;
        return config.getString(type, "unknowType");
    }

    /**
     * 获取配置信息
     */
    public static PropertiesConfiguration getConfig() {
        try {
            return new PropertiesConfiguration("generator.properties");
        } catch (ConfigurationException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }
}
