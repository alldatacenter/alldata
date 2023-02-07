/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.common.config;

import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigList;
import com.typesafe.config.ConfigMergeable;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigOrigin;
import com.typesafe.config.ConfigResolveOptions;
import com.typesafe.config.ConfigValue;

abstract class NestedConfig implements Config {
  private final Config c;

  NestedConfig(Config c) {
    this.c = c;
  }

  @Override
  public ConfigObject root() {
    return c.root();
  }

  @Override
  public ConfigOrigin origin() {
    return c.origin();
  }

  @Override
  public Config withFallback(ConfigMergeable other) {
    return c.withFallback(other);
  }

  @Override
  public Config resolve() {
    return c.resolve();
  }

  @Override
  public Config resolve(ConfigResolveOptions options) {
    return c.resolve(options);
  }

  @Override
  public void checkValid(Config reference, String... restrictToPaths) {
    c.checkValid(reference, restrictToPaths);
  }

  @Override
  public boolean hasPath(String path) {
    return c.hasPath(path);
  }

  @Override
  public boolean isEmpty() {
    return c.isEmpty();
  }

  @Override
  public Set<Entry<String, ConfigValue>> entrySet() {
    return c.entrySet();
  }

  @Override
  public boolean getBoolean(String path) {
    return c.getBoolean(path);
  }

  @Override
  public Number getNumber(String path) {
    return c.getNumber(path);
  }

  @Override
  public int getInt(String path) {
    return c.getInt(path);
  }

  @Override
  public long getLong(String path) {
    return c.getLong(path);
  }

  @Override
  public double getDouble(String path) {
    return c.getDouble(path);
  }

  @Override
  public String getString(String path) {
    return c.getString(path);
  }

  @Override
  public ConfigObject getObject(String path) {
    return c.getObject(path);
  }

  @Override
  public Config getConfig(String path) {
    return c.getConfig(path);
  }

  @Override
  public Object getAnyRef(String path) {
    return c.getAnyRef(path);
  }

  @Override
  public ConfigValue getValue(String path) {
    return c.getValue(path);
  }

  @Override
  public Long getBytes(String path) {
    return c.getBytes(path);
  }

  @Override
  public Long getMilliseconds(String path) {
    return c.getMilliseconds(path);
  }

  @Override
  public Long getNanoseconds(String path) {
    return c.getNanoseconds(path);
  }

  @Override
  public ConfigList getList(String path) {
    return c.getList(path);
  }

  @Override
  public List<Boolean> getBooleanList(String path) {
    return c.getBooleanList(path);
  }

  @Override
  public List<Number> getNumberList(String path) {
    return c.getNumberList(path);
  }

  @Override
  public List<Integer> getIntList(String path) {
    return c.getIntList(path);
  }

  @Override
  public List<Long> getLongList(String path) {
    return c.getLongList(path);
  }

  @Override
  public List<Double> getDoubleList(String path) {
    return c.getDoubleList(path);
  }

  @Override
  public List<String> getStringList(String path) {
    return c.getStringList(path);
  }

  @Override
  public List<? extends ConfigObject> getObjectList(String path) {
    return c.getObjectList(path);
  }

  @Override
  public List<? extends Config> getConfigList(String path) {
    return c.getConfigList(path);
  }

  @Override
  public List<? extends Object> getAnyRefList(String path) {
    return c.getAnyRefList(path);
  }

  @Override
  public List<Long> getBytesList(String path) {
    return c.getBytesList(path);
  }

  @Override
  public List<Long> getMillisecondsList(String path) {
    return c.getMillisecondsList(path);
  }

  @Override
  public List<Long> getNanosecondsList(String path) {
    return c.getNanosecondsList(path);
  }

  @Override
  public Config withOnlyPath(String path) {
    return c.withOnlyPath(path);
  }

  @Override
  public Config withoutPath(String path) {
    return c.withoutPath(path);
  }

  @Override
  public Config atPath(String path) {
    return c.atPath(path);
  }

  @Override
  public Config atKey(String key) {
    return c.atKey(key);
  }

  @Override
  public Config withValue(String path, ConfigValue value) {
    return c.withValue(path, value);
  }
}
