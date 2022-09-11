/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sort.standalone.config.holder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.flume.Context;
import org.apache.inlong.sort.standalone.config.loader.ClassResourceCommonPropertiesLoader;
import org.apache.inlong.sort.standalone.config.loader.CommonPropertiesLoader;
import org.apache.inlong.sort.standalone.utils.InlongLoggerFactory;
import org.slf4j.Logger;

/**
 * 
 * CommonPropertiesHolder
 */
public class CommonPropertiesHolder {

    public static final Logger LOG = InlongLoggerFactory.getLogger(CommonPropertiesHolder.class);
    public static final String DEFAULT_LOADER = ClassResourceCommonPropertiesLoader.class.getName();
    public static final String KEY_COMMON_PROPERTIES = "common_properties_loader";
    public static final String KEY_CLUSTER_ID = "clusterId";

    private static Map<String, String> props;
    private static Context context;

    private static long auditFormatInterval = 60000L;

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
                    }
                } catch (Throwable t) {
                    LOG.error("Fail to init CommonPropertiesLoader,loaderClass:{},error:{}",
                            loaderClassName, t.getMessage());
                    LOG.error(t.getMessage(), t);
                }
                context = new Context(props);
            }
        }
    }

    /**
     * get props
     * 
     * @return the props
     */
    public static Map<String, String> get() {
        if (props != null) {
            return props;
        }
        init();
        return props;
    }

    /**
     * get context
     *
     * @return the context
     */
    public static Context getContext() {
        if (context != null) {
            return context;
        }
        init();
        return context;
    }

    /**
     * Gets value mapped to key, returning defaultValue if unmapped.
     * 
     * @param  key          to be found
     * @param  defaultValue returned if key is unmapped
     * @return              value associated with key
     */
    public static String getString(String key, String defaultValue) {
        return get().getOrDefault(key, defaultValue);
    }

    /**
     * Gets value mapped to key, returning null if unmapped.
     * 
     * @param  key to be found
     * @return     value associated with key or null if unmapped
     */
    public static String getString(String key) {
        return get().get(key);
    }

    /**
     * Gets value mapped to key, returning defaultValue if unmapped.
     * 
     * @param  key          to be found
     * @param  defaultValue returned if key is unmapped
     * @return              value associated with key
     */
    public static Long getLong(String key, Long defaultValue) {
        return NumberUtils.toLong(get().get(key), defaultValue);
    }

    /**
     * Gets value mapped to key, returning null if unmapped.
     * 
     * @param  key to be found
     * @return     value associated with key or null if unmapped
     */
    public static Long getLong(String key) {
        String strValue = get().get(key);
        Long value = (strValue == null) ? null : NumberUtils.toLong(get().get(key));
        return value;
    }

    /**
     * getStringFromContext
     * 
     * @param  context
     * @param  key
     * @param  defaultValue
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
     * @param  key          to be found
     * @param  defaultValue returned if key is unmapped
     * @return              value associated with key
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
     * @param  key to be found
     * @return     value associated with key or null if unmapped
     */
    public static Integer getInteger(String key) {
        return getInteger(key, null);
    }

    /**
     * getClusterId
     * 
     * @return
     */
    public static String getClusterId() {
        return getString(KEY_CLUSTER_ID);
    }

    /**
     * getAuditFormatInterval
     *
     * @return
     */
    public static long getAuditFormatInterval() {
        return auditFormatInterval;
    }

}
