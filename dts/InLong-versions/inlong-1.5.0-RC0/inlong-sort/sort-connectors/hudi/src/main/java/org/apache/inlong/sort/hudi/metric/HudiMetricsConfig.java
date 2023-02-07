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

package org.apache.inlong.sort.hudi.metric;

import org.apache.flink.configuration.ConfigOption;
import org.apache.hudi.common.config.ConfigClassProperty;
import org.apache.hudi.common.config.ConfigGroups;
import org.apache.hudi.common.config.ConfigProperty;
import org.apache.hudi.common.config.HoodieConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.Immutable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Properties;

import static org.apache.hudi.config.metrics.HoodieMetricsConfig.METRIC_PREFIX;

/**
 * Configs for InLongHudi reporter type.
 */
@Immutable
@ConfigClassProperty(name = "Metrics Configurations for InLongHudi reporter", groupName = ConfigGroups.Names.METRICS, description = "Enables reporting on Hudi metrics using the InLongHudi reporter type. "
        + "Hudi publishes metrics on every commit, clean, rollback etc.")
public class HudiMetricsConfig extends HoodieConfig {

    public static final Logger LOG = LoggerFactory.getLogger(HudiMetricsConfig.class);
    public static final String METRIC_TYPE = "inlonghudi";

    public static final String INLONG_HUDI_PREFIX = METRIC_PREFIX + "." + METRIC_TYPE;

    public static final ConfigProperty<Integer> REPORT_PERIOD_IN_SECONDS = ConfigProperty
            .key(INLONG_HUDI_PREFIX + ".report.period.seconds")
            .defaultValue(30)
            .sinceVersion("0.6.0")
            .withDocumentation("InLongHudi reporting period in seconds. Default to 30.");

    /**
     * Get config from props
     */
    public static <T> T getConfig(
            Properties props,
            ConfigProperty<T> configProperty) {
        return new GeneralConfig<T>(configProperty).getProperty(props);
    }

    /**
     * Get config from props
     */
    public static <T> T getConfig(
            Properties props,
            ConfigOption<T> configProperty) {
        return new GeneralConfig<T>(configProperty).getProperty(props);
    }

    /**
     * The until for parse config.
     */
    public static class GeneralConfig<T> {

        private ConfigOption<T> option;
        private ConfigProperty<T> config;
        private Type type;

        public GeneralConfig(ConfigOption<T> configOption) {
            this.option = configOption;
        }

        public GeneralConfig(ConfigProperty<T> configProperty) {
            this.config = configProperty;
        }

        public String key() {
            return option != null ? option.key() : config.key();
        }

        public T defaultValue() {
            return option != null ? option.defaultValue() : config.defaultValue();
        }

        public T getProperty(Properties prop) {
            String key = key();
            T defaultValue = defaultValue();
            Object o = prop.get(key);
            if (o == null) {
                return defaultValue;
            }
            Class<?> defaultValueClass = defaultValue.getClass();
            if (o.getClass().isAssignableFrom(defaultValueClass)) {
                return (T) o;
            } else {
                String property = String.valueOf(o);
                try {
                    return (T) parseValue(defaultValueClass, property);
                } catch (InvocationTargetException | IllegalAccessException e) {
                    throw new RuntimeException("Can not properly parse value:'" + property +
                            "' to '" + defaultValueClass + "'", e);
                }
            }
        }

        private static <T> T parseValue(
                Class<T> clazz,
                String property) throws InvocationTargetException, IllegalAccessException {
            Method valueOfMethod = null;
            try {
                valueOfMethod = clazz.getMethod("valueOf", String.class);
            } catch (NoSuchMethodException e) {
                LOG.error("Can not properly find 'valueOf' method of " + clazz, e);
                throw new UnsupportedOperationException();
            }
            return (T) valueOfMethod.invoke(null, property);
        }
    }

}
