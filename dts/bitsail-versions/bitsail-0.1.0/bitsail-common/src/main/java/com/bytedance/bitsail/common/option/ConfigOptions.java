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

package com.bytedance.bitsail.common.option;

import com.bytedance.bitsail.common.util.Preconditions;

import com.alibaba.fastjson.TypeReference;

/**
 * @desc: {@code ConfigOptions} are used to build a {@link ConfigOption}.
 * The option is typically built in one of the following pattern:
 *
 * <pre>{@code
 * // simple string-valued option with a default value
 * ConfigOption<String> tempDirs = ConfigOptions
 *     .key("tmp.dir")
 *     .defaultValue("/tmp");
 *
 * // simple integer-valued option with a default value
 * ConfigOption<Integer> parallelism = ConfigOptions
 *     .key("application.parallelism")
 *     .defaultValue(100);
 *
 * // option with no default value
 * ConfigOption<String> userName = ConfigOptions
 *     .key("user.name")
 *     .noDefaultValue();
 *
 * // option with deprecated keys to check
 * ConfigOption<Double> threshold = ConfigOptions
 *     .key("cpu.utilization.threshold")
 *     .defaultValue(0.9).
 *     .withDeprecatedKeys("cpu.threshold");
 * }</pre>
 */
public class ConfigOptions {
  /**
   * Not intended to be instantiated
   */
  private ConfigOptions() {
  }

  // ------------------------------------------------------------------------

  /**
   * @param key The key for the config option.
   * @return The builder for the config option with the given key.
   */
  public static OptionBuilder key(String key) {
    Preconditions.checkNotNull(key);
    return new OptionBuilder(key);
  }

  // ------------------------------------------------------------------------

  public static final class OptionBuilder {

    /**
     * The key for the config option
     */
    private final String key;

    /**
     * Creates a new OptionBuilder.
     *
     * @param key The key for the config option
     */
    OptionBuilder(String key) {
      this.key = key;
    }

    /**
     * Creates a ConfigOption with the given default value.
     *
     * <p>This method does not accept "null". For options with no default value, choose
     * one of the {@code noDefaultValue} methods.
     *
     * @param value The default value for the config option
     * @param <T>   The type of the default value.
     * @return The config option with the default value.
     */
    public <T> ConfigOption<T> defaultValue(T value) {
      Preconditions.checkNotNull(value);
      return new ConfigOption<>(key, value, value.getClass());
    }

    /**
     * Creates a dynamic-class-valued option with no default value.
     *
     * @return The created ConfigOption.
     */
    public <T> ConfigOption<T> noDefaultValue(Class<T> clazz) {
      return new ConfigOption<>(key, null, clazz);
    }

    public <T> ConfigOption<T> onlyReference(TypeReference<T> reference) {
      return new ConfigOption<T>(key, null, reference);
    }
  }
}
