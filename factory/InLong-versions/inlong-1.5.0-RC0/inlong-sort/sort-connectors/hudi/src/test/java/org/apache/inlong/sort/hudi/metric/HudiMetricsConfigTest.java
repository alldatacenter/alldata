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
import org.apache.flink.configuration.ConfigOptions;
import org.apache.hudi.common.config.ConfigProperty;
import org.junit.Assert;
import org.junit.Test;

import java.util.Properties;

/**
 * The UnitTest for {@link HudiMetricsConfig#getConfig(Properties, ConfigOption)}
 *
 */
public class HudiMetricsConfigTest {

    @Test
    public void testGetConfigByConfigProperty() {
        Properties props = new Properties();

        String stringPropKey = "string_prop";
        String stringPropDefaultValue = "default_string_value";
        String stringPropExpectedValue = "sample_value";

        ConfigProperty<String> stringProp =
                ConfigProperty.key(stringPropKey)
                        .defaultValue(stringPropDefaultValue);

        testGetValueByConfig(
                props,
                stringProp,
                stringPropKey,
                stringPropDefaultValue,
                stringPropExpectedValue);

        String intPropKey = "int_prop";
        int intPropDefaultValue = 1;
        int intPropExpectedValue = 3;
        ConfigProperty<Integer> intProp =
                ConfigProperty.key(intPropKey)
                        .defaultValue(intPropDefaultValue);
        testGetValueByConfig(props, intProp, intPropKey, intPropDefaultValue, intPropExpectedValue);

        String longPropKey = "long_prop";
        long longPropDefaultValue = 1L;
        long longPropExpectedValue = 1000L;
        ConfigProperty<Long> longProp =
                ConfigProperty.key(longPropKey)
                        .defaultValue(longPropDefaultValue);
        testGetValueByConfig(props, longProp, longPropKey, longPropDefaultValue, longPropExpectedValue);

        String floatPropKey = "float_prop";
        float floatPropDefaultValue = 3.14f;
        ConfigProperty<Float> floatProp =
                ConfigProperty.key(floatPropKey)
                        .defaultValue(floatPropDefaultValue);
        float floatExpectedValue = 1000.0f;
        testGetValueByConfig(props, floatProp, floatPropKey, floatPropDefaultValue, floatExpectedValue);

        String doublePropKey = "double_prop";
        double doublePropDefaultValue = 3.14159265358979;
        double doublePropExpectedValue = 1000.1000;
        ConfigProperty<Double> doubleProp =
                ConfigProperty.key(doublePropKey)
                        .defaultValue(doublePropDefaultValue);
        testGetValueByConfig(props, doubleProp, doublePropKey, doublePropDefaultValue, doublePropExpectedValue);
    }

    private <T> void testGetValueByConfig(
            Properties props,
            ConfigProperty<T> property,
            String key,
            T defaultValue,
            T expectedValue) {
        T value = HudiMetricsConfig.getConfig(props, property);
        Assert.assertEquals(value, defaultValue);

        props.put(key, expectedValue);
        value = HudiMetricsConfig.getConfig(props, property);
        Assert.assertEquals(expectedValue, value);
    }

    private <T> void testGetValueByConfig(
            Properties props,
            ConfigOption<T> option,
            String key,
            T defaultValue,
            T expectedValue) {
        T value = HudiMetricsConfig.getConfig(props, option);
        Assert.assertEquals(value, defaultValue);

        props.put(key, expectedValue);
        value = HudiMetricsConfig.getConfig(props, option);
        Assert.assertEquals(expectedValue, value);
    }

    @Test
    public void testGetConfigByConfigOption() {
        Properties props = new Properties();

        String stringPropKey = "string_prop";
        String stringPropDefaultValue = "default_string_value";
        String stringPropExpectedValue = "sample_value";

        ConfigOption<String> stringProp =
                ConfigOptions.key(stringPropKey)
                        .defaultValue(stringPropDefaultValue);

        testGetValueByConfig(
                props,
                stringProp,
                stringPropKey,
                stringPropDefaultValue,
                stringPropExpectedValue);

        String intPropKey = "int_prop";
        int intPropDefaultValue = 1;
        int intPropExpectedValue = 3;
        ConfigOption<Integer> intProp =
                ConfigOptions.key(intPropKey)
                        .defaultValue(intPropDefaultValue);
        testGetValueByConfig(props, intProp, intPropKey, intPropDefaultValue, intPropExpectedValue);

        String longPropKey = "long_prop";
        long longPropDefaultValue = 1L;
        long longPropExpectedValue = 1000L;
        ConfigOption<Long> longProp =
                ConfigOptions.key(longPropKey)
                        .defaultValue(longPropDefaultValue);
        testGetValueByConfig(props, longProp, longPropKey, longPropDefaultValue, longPropExpectedValue);

        String floatPropKey = "float_prop";
        float floatPropDefaultValue = 3.14f;
        ConfigOption<Float> floatProp =
                ConfigOptions.key(floatPropKey)
                        .defaultValue(floatPropDefaultValue);
        float floatExpectedValue = 1000.0f;
        testGetValueByConfig(props, floatProp, floatPropKey, floatPropDefaultValue, floatExpectedValue);

        String doublePropKey = "double_prop";
        double doublePropDefaultValue = 3.14159265358979;
        double doublePropExpectedValue = 1000.1000;
        ConfigOption<Double> doubleProp =
                ConfigOptions.key(doublePropKey)
                        .defaultValue(doublePropDefaultValue);
        testGetValueByConfig(props, doubleProp, doublePropKey, doublePropDefaultValue, doublePropExpectedValue);
    }
}
