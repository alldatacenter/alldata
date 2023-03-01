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

package org.apache.uniffle.common.config;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.collect.Sets;

import org.apache.uniffle.common.util.UnitConverter;

public class RssConf implements Cloneable {

  /**
   * Stores the concrete key/value pairs of this configuration object.
   */
  private ConcurrentHashMap<String, Object> settings;

  /**
   * Creates a new empty configuration.
   */
  public RssConf() {
    this.settings = new ConcurrentHashMap<>();
  }

  /**
   * Creates a new configuration with the copy of the given configuration.
   *
   * @param other The configuration to copy the entries from.
   */
  public RssConf(RssConf other) {
    this.settings = new ConcurrentHashMap<>(other.settings);
  }

  public Set<String> getKeySet() {
    if (settings != null) {
      return settings.keySet();
    }
    return Sets.newHashSet();
  }

  /**
   * Returns the value associated with the given key as a string.
   *
   * @param key the key pointing to the associated value
   * @param defaultValue the default value which is returned when there is no
   *     value associated with the given key
   * @return the (default) value associated with the given key
   */
  public String getString(String key, String defaultValue) {
    return getRawValue(key)
        .map(ConfigUtils::convertToString)
        .orElse(defaultValue);
  }

  /**
   * Returns the value associated with the given config option as a string.
   *
   * @param configOption The configuration option
   * @return the (default) value associated with the given config option
   */
  public String getString(ConfigOption<String> configOption) {
    return getOptional(configOption)
        .orElseGet(configOption::defaultValue);
  }

  /**
   * Returns the value associated with the given config option as a string.
   * If no value is mapped under any key of the option, it returns the specified
   * default instead of the option's default value.
   *
   * @param configOption The configuration option
   * @return the (default) value associated with the given config option
   */
  public String getString(ConfigOption<String> configOption, String overrideDefault) {
    return getOptional(configOption)
        .orElse(overrideDefault);
  }

  /**
   * Adds the given key/value pair to the configuration object.
   *
   * @param key the key of the key/value pair to be added
   * @param value the value of the key/value pair to be added
   */
  public void setString(String key, String value) {
    setValueInternal(key, value);
  }

  /**
   * Adds the given value to the configuration object.
   * The main key of the config option will be used to map the value.
   *
   * @param key the option specifying the key to be added
   * @param value the value of the key/value pair to be added
   */
  public void setString(ConfigOption<String> key, String value) {
    setValueInternal(key.key(), value);
  }

  /**
   * Returns the value associated with the given key as an integer.
   *
   * @param key the key pointing to the associated value
   * @param defaultValue the default value which is returned when there is no
   *     value associated with the given key
   * @return the (default) value associated with the given key
   */
  public int getInteger(String key, int defaultValue) {
    return getRawValue(key)
        .map(ConfigUtils::convertToInt)
        .orElse(defaultValue);
  }

  /**
   * Returns the value associated with the given config option as an integer.
   *
   * @param configOption The configuration option
   * @return the (default) value associated with the given config option
   */
  public int getInteger(ConfigOption<Integer> configOption) {
    return getOptional(configOption)
        .orElseGet(configOption::defaultValue);
  }

  /**
   * Returns the value associated with the given config option as an integer.
   * If no value is mapped under any key of the option, it returns the specified
   * default instead of the option's default value.
   *
   * @param configOption The configuration option
   * @param overrideDefault The value to return if no value was mapper for any key of the option
   * @return the configured value associated with the given config option, or the overrideDefault
   */
  public int getInteger(ConfigOption<Integer> configOption, int overrideDefault) {
    return getOptional(configOption)
        .orElse(overrideDefault);
  }

  /**
   * Adds the given key/value pair to the configuration object.
   *
   * @param key the key of the key/value pair to be added
   * @param value the value of the key/value pair to be added
   */
  public void setInteger(String key, int value) {
    setValueInternal(key, value);
  }

  /**
   * Adds the given value to the configuration object.
   * The main key of the config option will be used to map the value.
   *
   * @param key the option specifying the key to be added
   * @param value the value of the key/value pair to be added
   */
  public void setInteger(ConfigOption<Integer> key, int value) {
    setValueInternal(key.key(), value);
  }

  /**
   * Returns the value associated with the given key as a long.
   *
   * @param key the key pointing to the associated value
   * @param defaultValue the default value which is returned in case there is no value
   *     associated with the given key
   * @return the (default) value associated with the given key
   */
  public long getLong(String key, long defaultValue) {
    return getRawValue(key)
        .map(ConfigUtils::convertToLong)
        .orElse(defaultValue);
  }

  /**
   * Returns the value associated with the given config option as a long integer.
   *
   * @param configOption The configuration option
   * @return the (default) value associated with the given config option
   */
  public long getLong(ConfigOption<Long> configOption) {
    return getOptional(configOption)
        .orElseGet(configOption::defaultValue);
  }

  /**
   * Returns the value associated with the given config option as a long integer.
   * If no value is mapped under any key of the option, it returns the specified
   * default instead of the option's default value.
   *
   * @param configOption The configuration option
   * @param overrideDefault The value to return if no value was mapper for any key of the option
   * @return the configured value associated with the given config option, or the overrideDefault
   */
  public long getLong(ConfigOption<Long> configOption, long overrideDefault) {
    return getOptional(configOption)
        .orElse(overrideDefault);
  }

  /**
   * Adds the given key/value pair to the configuration object.
   *
   * @param key the key of the key/value pair to be added
   * @param value the value of the key/value pair to be added
   */
  public void setLong(String key, long value) {
    setValueInternal(key, value);
  }

  /**
   * Adds the given value to the configuration object.
   * The main key of the config option will be used to map the value.
   *
   * @param key the option specifying the key to be added
   * @param value the value of the key/value pair to be added
   */
  public void setLong(ConfigOption<Long> key, long value) {
    setValueInternal(key.key(), value);
  }

  /**
   * Returns the value associated with the given key as a boolean.
   *
   * @param key the key pointing to the associated value
   * @param defaultValue the default value which is returned when there is no value
   *     associated with the given key
   * @return the (default) value associated with the given key
   */
  public boolean getBoolean(String key, boolean defaultValue) {
    return getRawValue(key)
        .map(ConfigUtils::convertToBoolean)
        .orElse(defaultValue);
  }

  /**
   * Returns the value associated with the given config option as a boolean.
   *
   * @param configOption The configuration option
   * @return the (default) value associated with the given config option
   */
  public boolean getBoolean(ConfigOption<Boolean> configOption) {
    return getOptional(configOption)
        .orElseGet(configOption::defaultValue);
  }

  /**
   * Returns the value associated with the given config option as a boolean.
   * If no value is mapped under any key of the option, it returns the specified
   * default instead of the option's default value.
   *
   * @param configOption The configuration option
   * @param overrideDefault The value to return if no value was mapper for any key of the option
   * @return the configured value associated with the given config option, or the overrideDefault
   */
  public boolean getBoolean(ConfigOption<Boolean> configOption, boolean overrideDefault) {
    return getOptional(configOption)
        .orElse(overrideDefault);
  }

  /**
   * Adds the given key/value pair to the configuration object.
   *
   * @param key the key of the key/value pair to be added
   * @param value the value of the key/value pair to be added
   */
  public void setBoolean(String key, boolean value) {
    setValueInternal(key, value);
  }

  /**
   * Adds the given value to the configuration object.
   * The main key of the config option will be used to map the value.
   *
   * @param key the option specifying the key to be added
   * @param value the value of the key/value pair to be added
   */
  public void setBoolean(ConfigOption<Boolean> key, boolean value) {
    setValueInternal(key.key(), value);
  }

  /**
   * Returns the value associated with the given key as a float.
   *
   * @param key the key pointing to the associated value
   * @param defaultValue the default value which is returned in case there is no value associated with the
   *     given key
   * @return the (default) value associated with the given key
   */
  public float getFloat(String key, float defaultValue) {
    return getRawValue(key)
        .map(ConfigUtils::convertToFloat)
        .orElse(defaultValue);
  }

  /**
   * Returns the value associated with the given config option as a float.
   *
   * @param configOption The configuration option
   * @return the (default) value associated with the given config option
   */
  public float getFloat(ConfigOption<Float> configOption) {
    return getOptional(configOption)
        .orElseGet(configOption::defaultValue);
  }

  /**
   * Returns the value associated with the given config option as a float.
   * If no value is mapped under any key of the option, it returns the specified
   * default instead of the option's default value.
   *
   * @param configOption The configuration option
   * @param overrideDefault The value to return if no value was mapper for any key of the option
   * @return the configured value associated with the given config option, or the overrideDefault
   */
  public float getFloat(ConfigOption<Float> configOption, float overrideDefault) {
    return getOptional(configOption)
        .orElse(overrideDefault);
  }

  /**
   * Adds the given key/value pair to the configuration object.
   *
   * @param key the key of the key/value pair to be added
   * @param value the value of the key/value pair to be added
   */
  public void setFloat(String key, float value) {
    setValueInternal(key, value);
  }

  /**
   * Adds the given value to the configuration object.
   * The main key of the config option will be used to map the value.
   *
   * @param key the option specifying the key to be added
   * @param value the value of the key/value pair to be added
   */
  public void setFloat(ConfigOption<Float> key, float value) {
    setValueInternal(key.key(), value);
  }

  /**
   * Returns the value associated with the given key as a double.
   *
   * @param key the key pointing to the associated value
   * @param defaultValue the default value which is returned when there is no value associated with the given
   *     key
   * @return the (default) value associated with the given key
   */
  public double getDouble(String key, double defaultValue) {
    return getRawValue(key)
        .map(ConfigUtils::convertToDouble)
        .orElse(defaultValue);
  }

  /**
   * Returns the value associated with the given config option as a {@code double}.
   *
   * @param configOption The configuration option
   * @return the (default) value associated with the given config option
   */
  public double getDouble(ConfigOption<Double> configOption) {
    return getOptional(configOption)
        .orElseGet(configOption::defaultValue);
  }

  /**
   * Returns the value associated with the given config option as a {@code double}.
   * If no value is mapped under any key of the option, it returns the specified
   * default instead of the option's default value.
   *
   * @param configOption The configuration option
   * @param overrideDefault The value to return if no value was mapper for any key of the option
   * @return the configured value associated with the given config option, or the overrideDefault
   */
  public double getDouble(ConfigOption<Double> configOption, double overrideDefault) {
    return getOptional(configOption)
        .orElse(overrideDefault);
  }

  /**
   * Adds the given key/value pair to the configuration object.
   *
   * @param key the key of the key/value pair to be added
   * @param value the value of the key/value pair to be added
   */
  public void setDouble(String key, double value) {
    setValueInternal(key, value);
  }

  /**
   * Adds the given value to the configuration object.
   * The main key of the config option will be used to map the value.
   *
   * @param key the option specifying the key to be added
   * @param value the value of the key/value pair to be added
   */
  public void setDouble(ConfigOption<Double> key, double value) {
    setValueInternal(key.key(), value);
  }

  /**
   * Returns the value associated with the given key as size in bytes.
   *
   * @param key the key pointing to the associated value
   * @param defaultValue the default value which is returned in case there is no value
   *     associated with the given key
   * @return the (default) value associated with the given key
   */
  public long getSizeInBytes(String key, long defaultValue) {
    return getRawValue(key)
      .map(ConfigUtils::convertToSizeInBytes)
      .orElse(defaultValue);
  }

  /**
   * Returns the value associated with the given key as size in bytes.
   *
   * @param key the key pointing to the associated value
   * @param defaultValue the default value which is returned in case there is no value
   *     associated with the given key
   * @return the (default) value associated with the given key
   */
  public long getSizeAsBytes(String key, String defaultValue) {
    return getSizeInBytes(key, UnitConverter.byteStringAsBytes(defaultValue));
  }

  /**
   * Returns the value associated with the given config option as size in bytes.
   *
   * @param configOption The configuration option
   * @return the (default) value associated with the given config option
   */
  public long getSizeAsBytes(ConfigOption<Long> configOption) {
    return getOptional(configOption).orElseGet(configOption::defaultValue);
  }

  /**
   * Adds the given key/value pair to the configuration object.
   *
   * @param key the key of the key/value pair to be added
   * @param value the value of the key/value pair to be added
   */
  public void setSizeAsBytes(String key, long value) {
    setValueInternal(key, value);
  }

  /**
   * Adds the given value to the configuration object.
   * The main key of the config option will be used to map the value.
   *
   * @param key the option specifying the key to be added
   * @param value the value of the key/value pair to be added
   */
  public void setSizeAsBytes(ConfigOption<Long> key, long value) {
    setValueInternal(key.key(), value);
  }

  /**
   * Adds the given value to the configuration object.
   * The main key of the config option will be used to map the value.
   *
   * @param key the option specifying the key to be added
   * @param value the value of the key/value pair to be added
   */
  public void setSizeAsBytes(ConfigOption<Long> key, String value) {
    setValueInternal(key.key(), UnitConverter.byteStringAsBytes(value));
  }


  /**
   * Returns the value associated with the given key as a byte array.
   *
   * @param key The key pointing to the associated value.
   * @param defaultValue The default value which is returned in case there is no value associated with the
   *     given key.
   * @return the (default) value associated with the given key.
   */
  public byte[] getBytes(String key, byte[] defaultValue) {
    return getRawValue(key).map(o -> {
          if (o.getClass().equals(byte[].class)) {
            return (byte[]) o;
          } else {
            throw new IllegalArgumentException(String.format(
                "Configuration cannot evaluate value %s as a byte[] value",
                o));
          }
        }
    ).orElse(defaultValue);
  }

  /**
   * Adds the given byte array to the configuration object. If key is <code>null</code> then nothing is added.
   *
   * @param key The key under which the bytes are added.
   * @param bytes The bytes to be added.
   */
  public void setBytes(String key, byte[] bytes) {
    setValueInternal(key, bytes);
  }

  /**
   * Returns the value associated with the given config option as a string.
   *
   * @param configOption The configuration option
   * @return the (default) value associated with the given config option
   */
  public String getValue(ConfigOption<?> configOption) {
    return Optional.ofNullable(getRawValueFromOption(configOption).orElseGet(configOption::defaultValue))
        .map(String::valueOf)
        .orElse(null);
  }

  // --------------------------------------------------------------------------------------------

  @Override
  public RssConf clone() throws CloneNotSupportedException {
    RssConf config = (RssConf) super.clone();
    config.settings = new ConcurrentHashMap<>(settings);
    return config;
  }

  public void addAll(RssConf other) {
    this.settings.putAll(other.settings);
  }

  /**
   * Checks whether there is an entry with the specified key.
   *
   * @param key key of entry
   * @return true if the key is stored, false otherwise
   */
  public boolean containsKey(String key) {
    return this.settings.containsKey(key);
  }

  /**
   * Checks whether there is an entry for the given config option.
   *
   * @param configOption The configuration option
   * @return <tt>true</tt> if a valid (current or deprecated) key of the config option is stored,
   *     <tt>false</tt> otherwise
   */
  public boolean contains(ConfigOption<?> configOption) {
    return this.settings.containsKey(configOption.key());
  }

  public <T> T get(ConfigOption<T> option) {
    return getOptional(option)
        .orElseGet(option::defaultValue);
  }

  public <T> Optional<T> getOptional(ConfigOption<T> option) {
    Optional<Object> rawValue = getRawValueFromOption(option);
    Class<?> clazz = option.getClazz();
    Optional<T> value = rawValue.map(v -> option.convertValue(v, clazz));
    return value;
  }

  public <T> RssConf set(ConfigOption<T> option, T value) {
    setValueInternal(option.key(), value);
    return this;
  }

  <T> void setValueInternal(String key, T value) {
    if (key == null) {
      throw new NullPointerException("Key must not be null.");
    }
    if (value == null) {
      throw new NullPointerException("Value must not be null.");
    }
    this.settings.put(key, value);
  }

  private Optional<Object> getRawValue(String key) {
    if (key == null) {
      throw new NullPointerException("Key must not be null.");
    }
    return Optional.ofNullable(this.settings.get(key));
  }

  private Optional<Object> getRawValueFromOption(ConfigOption<?> configOption) {
    return getRawValue(configOption.key());
  }

  @Override
  public int hashCode() {
    int hash = 0;
    for (String s : this.settings.keySet()) {
      hash ^= s.hashCode();
    }
    return hash;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    } else if (obj instanceof RssConf) {
      Map<String, Object> otherConf = ((RssConf) obj).settings;
      for (Map.Entry<String, Object> e : this.settings.entrySet()) {
        Object thisVal = e.getValue();
        Object otherVal = otherConf.get(e.getKey());
        if (!thisVal.getClass().equals(byte[].class)) {
          if (!thisVal.equals(otherVal)) {
            return false;
          }
        } else if (otherVal.getClass().equals(byte[].class)) {
          if (!Arrays.equals((byte[]) thisVal, (byte[]) otherVal)) {
            return false;
          }
        } else {
          return false;
        }
      }
      return true;
    } else {
      return false;
    }
  }

  @Override
  public String toString() {
    return this.settings.toString();
  }

  public String getEnv(String key) {
    return System.getenv(key);
  }

}
