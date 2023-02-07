/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.dataproxy.config.holder;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.flume.Context;
import org.apache.inlong.dataproxy.config.loader.ClassResourceCommonPropertiesLoader;
import org.apache.inlong.dataproxy.config.loader.CommonPropertiesLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CommonPropertiesHolder
 */
public class CommonPropertiesHolder {

    public static final Logger LOG = LoggerFactory.getLogger(CommonPropertiesHolder.class);
    public static final String KEY_COMMON_PROPERTIES = "common-properties-loader";
    public static final String DEFAULT_LOADER = ClassResourceCommonPropertiesLoader.class.getName();
    public static final String KEY_PROXY_CLUSTER_NAME = "proxy.cluster.name";
    public static final String KEY_RESPONSE_AFTER_SAVE = "isResponseAfterSave";
    public static final boolean DEFAULT_RESPONSE_AFTER_SAVE = false;
    public static final String KEY_MAX_RESPONSE_TIMEOUT_MS = "maxResponseTimeoutMs";
    public static final long DEFAULT_MAX_RESPONSE_TIMEOUT_MS = 10000L;

    private static Map<String, String> props;

    private static long auditFormatInterval = 60000L;
    private static boolean isResponseAfterSave = DEFAULT_RESPONSE_AFTER_SAVE;
    private static long maxResponseTimeout = DEFAULT_MAX_RESPONSE_TIMEOUT_MS;

    /**
     * init
     */
    private static void init() {
        synchronized (KEY_COMMON_PROPERTIES) {
            if (props == null) {
                props = new ConcurrentHashMap<>();
                String loaderClassName = System.getenv(KEY_COMMON_PROPERTIES);
                loaderClassName = (loaderClassName == null) ? DEFAULT_LOADER : loaderClassName;
                try {
                    Class<?> loaderClass = ClassUtils.getClass(loaderClassName);
                    Object loaderObject = loaderClass.getDeclaredConstructor().newInstance();
                    if (loaderObject instanceof CommonPropertiesLoader) {
                        CommonPropertiesLoader loader = (CommonPropertiesLoader) loaderObject;
                        props.putAll(loader.load());
                        LOG.info("loaderClass:{},properties:{}", loaderClassName, props);
                        auditFormatInterval = NumberUtils
                                .toLong(CommonPropertiesHolder.getString("auditFormatInterval"), 60000L);
                        isResponseAfterSave = BooleanUtils
                                .toBoolean(CommonPropertiesHolder.getString(KEY_RESPONSE_AFTER_SAVE));
                        maxResponseTimeout = CommonPropertiesHolder.getLong(KEY_MAX_RESPONSE_TIMEOUT_MS,
                                DEFAULT_MAX_RESPONSE_TIMEOUT_MS);
                    }
                } catch (Throwable t) {
                    LOG.error("Fail to init CommonPropertiesLoader,loaderClass:{},error:{}",
                            loaderClassName, t.getMessage());
                    LOG.error(t.getMessage(), t);
                }

            }
        }
    }

    /**
     * get props
     *
     * @return the props
     */
    public static Map<String, String> get() {
        synchronized (KEY_COMMON_PROPERTIES) {
            if (props != null) {
                return props;
            }
        }
        init();
        return props;
    }

    /**
     * Gets value mapped to key, returning defaultValue if unmapped.
     *
     * @param key to be found
     * @param defaultValue returned if key is unmapped
     * @return value associated with key
     */
    public static String getString(String key, String defaultValue) {
        return get().getOrDefault(key, defaultValue);
    }

    /**
     * Gets value mapped to key, returning null if unmapped.
     *
     * @param key to be found
     * @return value associated with key or null if unmapped
     */
    public static String getString(String key) {
        return get().get(key);
    }

    /**
     * getStringFromContext
     *
     * @param context
     * @param key
     * @param defaultValue
     * @return
     */
    public static String getStringFromContext(Context context, String key, String defaultValue) {
        String value = context.getString(key);
        value = (value != null) ? value : props.getOrDefault(key, defaultValue);
        return value;
    }

    /**
     * Gets value mapped to key, returning defaultValue if unmapped.
     *
     * @param key to be found
     * @param defaultValue returned if key is unmapped
     * @return value associated with key
     */
    public static Integer getInteger(String key, Integer defaultValue) {
        String value = get().get(key);
        if (value != null) {
            return Integer.valueOf(Integer.parseInt(value.trim()));
        }
        return defaultValue;
    }

    /**
     * Gets value mapped to key, returning null if unmapped.
     * <p>
     * Note that this method returns an object as opposed to a primitive. The configuration key requested may not be
     * mapped to a value and by returning the primitive object wrapper we can return null. If the key does not exist the
     * return value of this method is assigned directly to a primitive, a {@link NullPointerException} will be thrown.
     * </p>
     *
     * @param key to be found
     * @return value associated with key or null if unmapped
     */
    public static Integer getInteger(String key) {
        return getInteger(key, null);
    }

    /**
     * Gets value mapped to key, returning defaultValue if unmapped.
     *
     * @param key to be found
     * @param defaultValue returned if key is unmapped
     * @return value associated with key
     */
    public static Long getLong(String key, Long defaultValue) {
        String value = get().get(key);
        if (value != null) {
            return Long.valueOf(Long.parseLong(value.trim()));
        }
        return defaultValue;
    }

    /**
     * Gets value mapped to key, returning null if unmapped.
     * <p>
     * Note that this method returns an object as opposed to a primitive. The configuration key requested may not be
     * mapped to a value and by returning the primitive object wrapper we can return null. If the key does not exist the
     * return value of this method is assigned directly to a primitive, a {@link NullPointerException} will be thrown.
     * </p>
     *
     * @param key to be found
     * @return value associated with key or null if unmapped
     */
    public static Long getLong(String key) {
        return getLong(key, null);
    }

    /**
     * getAuditFormatInterval
     *
     * @return
     */
    public static long getAuditFormatInterval() {
        return auditFormatInterval;
    }

    /**
     * isResponseAfterSave
     *
     * @return
     */
    public static boolean isResponseAfterSave() {
        return isResponseAfterSave;
    }

    /**
     * get maxResponseTimeout
     *
     * @return the maxResponseTimeout
     */
    public static long getMaxResponseTimeout() {
        return maxResponseTimeout;
    }

}
