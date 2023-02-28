/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.datavines.common.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommonPropertyUtils {

    public static final String EXEC_THREADS = "exec.threads";
    public static final Integer EXEC_THREADS_DEFAULT = 100;

    public static final String MAX_CPU_LOAD_AVG = "max.cpu.load.avg";
    public static final Double MAX_CPU_LOAD_AVG_DEFAULT = 0.5;

    public static final String RESERVED_MEMORY = "reserved.memory";
    public static final Double RESERVED_MEMORY_DEFAULT = 0.3;

    public static final String SERVER_PORT = "server.port";
    public static final Integer SERVER_PORT_DEFAULT = 5600;

    public static final String FAILOVER_KEY = "registry.failover.key";
    public static final String FAILOVER_KEY_DEFAULT = "/datavines/failover";

    public static final String TASK_LOCK_KEY = "registry.task.lock.key";
    public static final String TASK_LOCK_KEY_DEFAULT = "/datavines/taskLock";

    public static final String CATALOG_TASK_LOCK_KEY = "registry.catalog.task.lock.key";
    public static final String CATALOG_TASK_LOCK_KEY_DEFAULT = "/datavines/catalog/taskLock";

    public static final String SERVERS_KEY = "registry.servers.key";
    public static final String SERVERS_KEY_DEFAULT = "/datavines/servers";

    public static final String REGISTRY_TYPE = "registry.type";
    public static final String REGISTRY_TYPE_DEFAULT = "default";

    public static final String REGISTRY_ZOOKEEPER_SERVER_LIST = "registry.zookeeper.server.list";
    public static final String REGISTRY_ZOOKEEPER_SERVER_LIST_DEFAULT = "localhost:2181";

    public static final String LOCAL_TMP_WORKDIR = "local.tmp.workdir";
    public static final String LOCAL_TMP_WORKDIR_DEFAULT = "/tmp/datavines";

    public static final String LOCAL_DATA_DIR = "local.data.dir";
    public static final String LOCAL_DATA_DIR_DEFAULT = "/tmp/datavines/data";

    public static final String ERROR_DATA_DIR = "error.data.dir";
    public static final String ERROR_DATA_DIR_DEFAULT = "/tmp/datavines/error-data";

    public static final String VALIDATE_RESULT_DATA_DIR = "validate.result.data.dir";
    public static final String VALIDATE_RESULT_DATA_DIR_DEFAULT = "/tmp/datavines/validate-result-data";

    public static final String LINE_SEPARATOR = "line.separator";
    public static final String LINE_SEPARATOR_DEFAULT = "\r\n";

    public static final String COLUMN_SEPARATOR = "column.separator";
    public static final String COLUMN_SEPARATOR_DEFAULT = ",";

    public static final String AES_KEY = "aes.key";
    public static final String AES_KEY_DEFAULT = "1234567890123456";

    public static final String FILE_MAX_LENGTH = "file.max.length";
    public static final Long FILE_MAX_LENGTH_DEFAULT = 10 * 1024 * 1024L;

    private static final Logger logger = LoggerFactory.getLogger(CommonPropertyUtils.class);

    public static final String COMMON_PROPERTIES_PATH = "/common.properties";

    private static final Properties PROPERTIES = new Properties();

    private CommonPropertyUtils() {
        throw new UnsupportedOperationException("Construct PropertyUtils");
    }

    static {
        loadPropertyFile(COMMON_PROPERTIES_PATH);
    }

    public static synchronized void loadPropertyFile(String... propertyFiles) {
        for (String fileName : propertyFiles) {
            try (InputStream fis = CommonPropertyUtils.class.getResourceAsStream(fileName);) {
                PROPERTIES.load(fis);

            } catch (IOException e) {
                logger.error(e.getMessage(), e);
                System.exit(1);
            }
        }

        // Override from system properties
        System.getProperties().forEach((k, v) -> {
            final String key = String.valueOf(k);
            logger.info("Overriding property from system property: {}", key);
            CommonPropertyUtils.setValue(key, String.valueOf(v));
        });
    }

    public static Properties getProperties() {
        return PROPERTIES;
    }
    /**
     * get property value
     *
     * @param key property name
     * @return property value
     */
    public static String getString(String key) {
        return PROPERTIES.getProperty(key.trim());
    }

    /**
     * get property value with upper case
     *
     * @param key property name
     * @return property value  with upper case
     */
    public static String getUpperCaseString(String key) {
        String val = getString(key);
        return StringUtils.isEmpty(val) ? val : val.toUpperCase();
    }

    /**
     * get property value
     *
     * @param key property name
     * @param defaultVal default value
     * @return property value
     */
    public static String getString(String key, String defaultVal) {
        String val = getString(key);
        return StringUtils.isEmpty(val) ? defaultVal : val;
    }

    /**
     * get property value
     *
     * @param key property name
     * @return get property int value , if key == null, then return -1
     */
    public static int getInt(String key) {
        return getInt(key, -1);
    }

    /**
     * @param key key
     * @param defaultValue default value
     * @return property value
     */
    public static int getInt(String key, int defaultValue) {
        String value = getString(key);
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            logger.info(e.getMessage(), e);
        }
        return defaultValue;
    }

    /**
     * get property value
     *
     * @param key property name
     * @return property value
     */
    public static boolean getBoolean(String key) {
        return getBoolean(key, false);
    }

    /**
     * get property value
     *
     * @param key property name
     * @param defaultValue default value
     * @return property value
     */
    public static Boolean getBoolean(String key, boolean defaultValue) {
        String value = getString(key);
        return StringUtils.isEmpty(value) ? defaultValue : Boolean.parseBoolean(value);
    }

    /**
     * get property long value
     *
     * @param key key
     * @param defaultValue default value
     * @return property value
     */
    public static long getLong(String key, long defaultValue) {
        String value = getString(key);
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            logger.info(e.getMessage(), e);
        }
        return defaultValue;
    }

    /**
     * @param key key
     * @return property value
     */
    public static long getLong(String key) {
        return getLong(key, -1);
    }

    /**
     * @param key key
     * @param defaultValue default value
     * @return property value
     */
    public static double getDouble(String key, double defaultValue) {
        String value = getString(key);
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            logger.info(e.getMessage(), e);
        }
        return defaultValue;
    }

    /**
     * get array
     *
     * @param key property name
     * @param splitStr separator
     * @return property value through array
     */
    public static String[] getArray(String key, String splitStr) {
        String value = getString(key);
        if (StringUtils.isEmpty(value)) {
            return new String[0];
        }
        return value.split(splitStr);
    }

    /**
     * @param key key
     * @param type type
     * @param defaultValue default value
     * @param <T> T
     * @return get enum value
     */
    public static <T extends Enum<T>> T getEnum(String key, Class<T> type,
                                                T defaultValue) {
        String value = getString(key);
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }

        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException e) {
            logger.info(e.getMessage(), e);
        }
        return defaultValue;
    }

    /**
     * get all properties with specified prefix, like: fs.
     *
     * @param prefix prefix to search
     * @return all properties with specified prefix
     */
    public static Map<String, String> getPrefixedProperties(String prefix) {
        Map<String, String> matchedProperties = new HashMap<>();
        for (String propName : PROPERTIES.stringPropertyNames()) {
            if (propName.startsWith(prefix)) {
                matchedProperties.put(propName, PROPERTIES.getProperty(propName));
            }
        }
        return matchedProperties;
    }

    /**
     * set value
     * @param key key
     * @param value value
     */
    public static void setValue(String key, String value) {
        PROPERTIES.setProperty(key, value);
    }

    public static Map<String, String> getPropertiesByPrefix(String prefix) {
        if (StringUtils.isEmpty(prefix)) {
            return null;
        }
        Set<Object> keys = PROPERTIES.keySet();
        if (keys.isEmpty()) {
            return null;
        }
        Map<String, String> propertiesMap = new HashMap<>();
        keys.forEach(k -> {
            if (k.toString().contains(prefix)) {
                propertiesMap.put(k.toString().replaceFirst(prefix + ".", ""), PROPERTIES.getProperty((String) k));
            }
        });
        return propertiesMap;
    }

}
