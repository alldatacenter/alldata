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
 *
 * Original Files: alibaba/DataX (https://github.com/alibaba/DataX)
 * Copyright: Copyright 1999-2022 Alibaba Group Holding Ltd.
 * SPDX-License-Identifier: Apache License 2.0
 *
 * This file may have been modified by ByteDance Ltd. and/or its affiliates.
 */

package com.bytedance.bitsail.common.configuration;

import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.exception.CommonErrorCode;
import com.bytedance.bitsail.common.exception.ErrorCode;
import com.bytedance.bitsail.common.option.ConfigOption;
import com.bytedance.bitsail.common.util.FastJsonUtil;
import com.bytedance.bitsail.common.util.StrUtil;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class BitSailConfiguration implements Serializable {
  private static final Logger LOG = LoggerFactory.getLogger(BitSailConfiguration.class);
  private static final long serialVersionUID = 1L;

  private Set<String> secretKeyPathSet =
      new HashSet<String>();

  private Object root = null;

  private BitSailConfiguration(final String json) {
    try {
      this.root = FastJsonUtil.parse(json);
    } catch (Exception e) {
      throw BitSailException.asBitSailException(CommonErrorCode.CONFIG_ERROR,
          String.format("Illegal json format, value = [%s]", json), e);
    }
  }

  /**
   * construct default configuration with empty.
   */
  public static BitSailConfiguration newDefault() {
    return BitSailConfiguration.from("{}");
  }

  /**
   * construct configuration from json file.
   */
  public static BitSailConfiguration from(String json) {
    checkJSON(json);

    try {
      return new BitSailConfiguration(json);
    } catch (Exception e) {
      throw BitSailException.asBitSailException(CommonErrorCode.CONFIG_ERROR, e);
    }

  }

  /**
   * construct configuration from file
   */
  public static BitSailConfiguration from(File file) {
    try {
      return BitSailConfiguration.from(IOUtils
          .toString(new FileInputStream(file)));
    } catch (FileNotFoundException e) {
      throw BitSailException.asBitSailException(CommonErrorCode.CONFIG_ERROR,
          String.format("Configuration file = %s is not exists.", file));
    } catch (IOException e) {
      throw BitSailException.asBitSailException(
          CommonErrorCode.CONFIG_ERROR, "Failed to read configuration file, plz check the fil content", e);
    }
  }

  /**
   * construct configuration file from map object.
   */
  public static BitSailConfiguration from(final Map<String, Object> object) {
    return BitSailConfiguration.from(BitSailConfiguration.toJSONString(object));
  }

  private static void checkJSON(final String json) {
    if (StringUtils.isBlank(json)) {
      throw BitSailException.asBitSailException(CommonErrorCode.CONFIG_ERROR,
          "Json should not be empty.");
    }
  }

  private static String toJSONString(final Object object) {
    return JSON.toJSONString(object, SerializerFeature.DisableCircularReferenceDetect);
  }

  /**
   * check field exists or not.
   */
  public boolean fieldExists(String field) {
    return this.get(field) != null;
  }

  public <T> boolean fieldExists(ConfigOption<T> option) {
    String field = option.key();
    return this.get(field) != null;
  }

  public String getUnnecessaryValue(String key, String defaultValue) {
    String value = this.getString(key, defaultValue);
    if (StringUtils.isBlank(value)) {
      value = defaultValue;
    }
    return value;
  }

  /**
   * get field value according to the path.
   *
   * @return result value will be when catch the exception.
   */
  public Object get(final String path) {
    this.checkPath(path);
    try {
      return this.findObject(path);
    } catch (Exception e) {
      return null;
    }
  }

  public <T> T get(final String path, Class<T> clazz) {
    this.checkPath(path);
    return (T) this.get(path);
  }

  public <T> T get(final ConfigOption<T> option) {
    String path = option.key();
    this.checkPath(path);
    try {
      return this.findObjectByConfig(path, option);
    } catch (Exception e) {
      if (!option.hasDefaultValue()) {
        return null;
      }
      return option.defaultValue();
    }
  }

  public <T> T getNecessaryOption(final ConfigOption<T> option, ErrorCode errorCode) {
    String path = option.key();
    this.checkPath(path);
    try {
      return (T) this.findObjectByConfig(path, option);
    } catch (Exception e) {
      throw BitSailException.asBitSailException(errorCode,
          String.format("Invalid configuration, [%s] must be set.", path));
    }
  }

  public <T> T getUnNecessaryOption(final ConfigOption<T> option, T defaultValue) {
    String path = option.key();
    this.checkPath(path);
    try {
      return this.findObjectByConfig(path, option);
    } catch (Exception e) {
      return defaultValue;
    }
  }

  public BitSailConfiguration getConfiguration(final String path) {
    BitSailConfiguration subConf = BitSailConfiguration.newDefault();
    checkPath(path);

    Object result = this.get(path);

    subConf.setObject(path, extractConfiguration(result));

    return subConf;
  }

  public String getString(final String path) {
    Object string = this.get(path);
    if (null == string) {
      return null;
    }
    return String.valueOf(string);
  }

  public String getString(final String path, final String defaultValue) {
    String result = this.getString(path);

    if (null == result) {
      return defaultValue;
    }

    return result;
  }

  public Character getChar(final String path) {
    String result = this.getString(path);
    if (null == result) {
      return null;
    }

    try {
      return CharUtils.toChar(result);
    } catch (Exception e) {
      throw BitSailException.asBitSailException(
          CommonErrorCode.CONFIG_ERROR,
          String.format("Failed to get char configuration value from path = [%s].", path), e);
    }
  }

  public Boolean getBool(final String path) {
    String result = this.getString(path);

    if (null == result) {
      return null;
    } else if ("true".equalsIgnoreCase(result)) {
      return Boolean.TRUE;
    } else if ("false".equalsIgnoreCase(result)) {
      return Boolean.FALSE;
    } else {
      throw BitSailException.asBitSailException(CommonErrorCode.CONFIG_ERROR,
          String.format("Failed to get boolean configuration value from path = [%s]", path));
    }

  }

  public Integer getInt(final String path) {
    String result = this.getString(path);
    if (null == result) {
      return null;
    }

    try {
      return (int) Double.parseDouble(result);
    } catch (Exception e) {
      throw BitSailException.asBitSailException(
          CommonErrorCode.CONFIG_ERROR,
          String.format(String.format("Failed to get int configuration value from path = [%s]", path)));
    }
  }

  public Long getLong(final String path) {
    String result = this.getString(path);
    if (StringUtils.isBlank(result)) {
      return null;
    }

    try {
      return (long) Double.parseDouble(result);
    } catch (Exception e) {
      throw BitSailException.asBitSailException(
          CommonErrorCode.CONFIG_ERROR,
          String.format(String.format("Failed to get long configuration value from path = [%s]", path)));
    }
  }

  public Float getFloat(final String path) {
    String result = this.getString(path);
    if (StringUtils.isBlank(result)) {
      return null;
    }

    try {
      return Float.valueOf(result);
    } catch (Exception e) {
      throw BitSailException.asBitSailException(
          CommonErrorCode.CONFIG_ERROR,
          String.format(String.format(String.format("Failed to get float configuration value from path = [%s]", path))));
    }
  }

  public Double getDouble(final String path) {
    String result = this.getString(path);
    if (StringUtils.isBlank(result)) {
      return null;
    }

    try {
      return Double.valueOf(result);
    } catch (Exception e) {
      throw BitSailException.asBitSailException(
          CommonErrorCode.CONFIG_ERROR,
          String.format(String.format(String.format("Failed to get double configuration value from path = [%s]", path))));
    }
  }

  @SuppressWarnings("unchecked")
  public <T> List<T> getList(final String path, Class<T> t) {
    Object object = this.get(path, List.class);
    if (null == object) {
      return null;
    }

    List<T> result = new ArrayList<T>();

    List<Object> origin = (List<Object>) object;
    for (final Object each : origin) {
      result.add((T) each);
    }

    return result;
  }

  public List<?> getList(final String path) {
    return this.get(path, List.class);
  }

  public <T> T getObject(final String path, TypeReference<T> reference) {
    Object result = this.get(path, Object.class);
    if (null == result) {
      return null;
    }
    if (result instanceof JSON) {
      return ((JSON) result).toJavaObject(reference);
    } else {
      return (T) result;
    }
  }

  public Map<?, ?> getMap(final String path) {
    if (this.get(path) instanceof String) {
      String str = (String) this.get(path);
      try {
        Map<?, ?> result = JSON.parseObject(str);
        this.set(path, result);
        return result;
      } catch (Exception e) {
        BitSailException bitSailException = BitSailException.asBitSailException(CommonErrorCode.CONFIG_ERROR, String.format("can't convert string %s to map", str));
        LOG.info(bitSailException.getMessage());
        throw bitSailException;
      }
    }

    return this.get(path, Map.class);
  }

  public <T> Map<String, T> getMap(final String path, Class<T> t) {
    Map<String, Object> map = this.get(path, Map.class);
    if (null == map) {
      return null;
    }

    Map<String, T> result = new HashMap<String, T>();
    for (final String key : map.keySet()) {
      result.put(key, (T) map.get(key));
    }

    return result;
  }

  /**
   * print configuration as format json.
   */
  private String beautify() {
    return JSON.toJSONString(this.getInternal(),
        SerializerFeature.PrettyFormat, SerializerFeature.DisableCircularReferenceDetect);
  }

  /**
   * print configuration as hide the value which sensitive.
   */
  public String desensitizedBeautify() {
    BitSailConfiguration copy = this.clone();
    Set<String> keys = getKeys();
    for (final String key : keys) {
      boolean isSensitive = StringUtils.endsWithIgnoreCase(key, "password")
          || StringUtils.endsWithIgnoreCase(key, "accessKey")
          || StringUtils.endsWithIgnoreCase(key, "secret")
          || StringUtils.endsWithIgnoreCase(key, "key")
          || StringUtils.endsWithIgnoreCase(key, "id")
          || StringUtils.endsWithIgnoreCase(key, "token");
      if (isSensitive && get(key) instanceof String) {
        copy.set(key, StrUtil.mask(getString(key)));
      }
    }
    return copy.beautify();
  }

  public Object set(final String path, final Object object) {
    checkPath(path);

    Object result = this.get(path);

    setObject(path, extractConfiguration(object));

    return result;
  }

  public <T> BitSailConfiguration set(final ConfigOption<T> key) {
    checkPath(key.key());

    setObject(key.key(), extractConfiguration(key.defaultValue()));

    return this;
  }

  public <T> BitSailConfiguration set(final ConfigOption<T> key, final T value) {
    checkPath(key.key());

    setObject(key.key(), extractConfiguration(value));

    return this;
  }

  public <T> BitSailConfiguration setIfAbsent(final ConfigOption<T> key, final T value) {
    checkPath(key.key());

    if (!this.fieldExists(key)) {
      setObject(key.key(), extractConfiguration(value));
    }

    return this;
  }

  public <T> BitSailConfiguration setOption(final String key, final T value) {
    checkPath(key);

    setObject(key, extractConfiguration(value));

    return this;
  }

  public Set<String> getKeys() {
    Set<String> collect = new HashSet<String>();
    this.getKeysRecursive(this.getInternal(), "", collect);
    return collect;
  }

  /**
   * Merge configuration.
   */
  @SafeVarargs
  public final BitSailConfiguration merge(final BitSailConfiguration another,
                                          boolean updateWhenConflict,
                                          Function<String, ? extends RuntimeException>... exceptionSupplier) {
    Set<String> keys = another.getKeys();

    for (final String key : keys) {
      if (updateWhenConflict) {
        this.set(key, another.get(key));
        continue;
      }

      boolean isCurrentExists = this.get(key) != null;

      if (isCurrentExists && exceptionSupplier.length > 0) {
        throw exceptionSupplier[0].apply(key);
      }

      if (isCurrentExists) {
        continue;
      }

      this.set(key, another.get(key));
    }
    return this;
  }

  @Override
  public String toString() {
    return this.toJSON();
  }

  public String toJSON() {
    return BitSailConfiguration.toJSONString(this.getInternal());
  }

  /**
   * Deep copy for the configuration.
   */
  @Override
  public BitSailConfiguration clone() {
    BitSailConfiguration config = BitSailConfiguration
        .from(BitSailConfiguration.toJSONString(this.getInternal()));
    config.addSecretKeyPath(this.secretKeyPathSet);
    return config;
  }

  public void addSecretKeyPath(Set<String> pathSet) {
    if (pathSet != null) {
      this.secretKeyPathSet.addAll(pathSet);
    }
  }

  public void setSecretKeyPathSet(Set<String> keyPathSet) {
    if (keyPathSet != null) {
      this.secretKeyPathSet = keyPathSet;
    }
  }

  public boolean isSecretPath(String path) {
    return this.secretKeyPathSet.contains(path);
  }

  @SuppressWarnings("unchecked")
  void getKeysRecursive(final Object current, String path, Set<String> collect) {
    boolean isRegularElement = !(current instanceof Map || current instanceof List);
    if (isRegularElement) {
      collect.add(path);
      return;
    }

    boolean isMap = current instanceof Map;
    if (isMap) {
      Map<String, Object> mapping = ((Map<String, Object>) current);
      for (final String key : mapping.keySet()) {
        if (StringUtils.isBlank(path)) {
          getKeysRecursive(mapping.get(key), key.trim(), collect);
        } else {
          getKeysRecursive(mapping.get(key), path + "." + key.trim(),
              collect);
        }
      }
      return;
    }

    boolean isList = current instanceof List;
    if (isList) {
      List<Object> lists = (List<Object>) current;
      for (int i = 0; i < lists.size(); i++) {
        getKeysRecursive(lists.get(i), path + String.format("[%d]", i),
            collect);
      }
      return;
    }

    return;
  }

  public Object getInternal() {
    return this.root;
  }

  private void setObject(final String path, final Object object) {
    Object newRoot = setObjectRecursive(this.root, split2List(path), 0,
        object);

    if (isSuitForRoot(newRoot)) {
      this.root = newRoot;
      return;
    }

    throw BitSailException.asBitSailException(CommonErrorCode.RUNTIME_ERROR,
        String.format("Can't set value = %s to the path = %s.",
            ToStringBuilder.reflectionToString(object), path));
  }

  @SuppressWarnings("unchecked")
  private Object extractConfiguration(final Object object) {
    if (object instanceof BitSailConfiguration) {
      return extractFromConfiguration(object);
    }

    if (object instanceof List) {
      List<Object> result = new ArrayList<Object>();
      for (final Object each : (List<Object>) object) {
        result.add(extractFromConfiguration(each));
      }
      return result;
    }

    if (object instanceof Map) {
      Map<String, Object> result = new HashMap<String, Object>();
      for (final String key : ((Map<String, Object>) object).keySet()) {
        result.put(key,
            extractFromConfiguration(((Map<String, Object>) object)
                .get(key)));
      }
      return result;
    }

    return object;
  }

  private Object extractFromConfiguration(final Object object) {
    if (object instanceof BitSailConfiguration) {
      return ((BitSailConfiguration) object).getInternal();
    }

    return object;
  }

  Object buildObject(final List<String> paths, final Object object) {
    if (null == paths) {
      throw BitSailException.asBitSailException(
          CommonErrorCode.RUNTIME_ERROR,
          "Field path can't be null.");
    }

    if (1 == paths.size() && StringUtils.isBlank(paths.get(0))) {
      return object;
    }

    Object child = object;
    for (int i = paths.size() - 1; i >= 0; i--) {
      String path = paths.get(i);

      if (isPathMap(path)) {
        Map<String, Object> mapping = new HashMap<String, Object>();
        mapping.put(path, child);
        child = mapping;
        continue;
      }

      if (isPathList(path)) {
        List<Object> lists = new ArrayList<Object>(
            this.getIndex(path) + 1);
        expand(lists, this.getIndex(path) + 1);
        lists.set(this.getIndex(path), child);
        child = lists;
        continue;
      }

      throw BitSailException.asBitSailException(
          CommonErrorCode.RUNTIME_ERROR, String.format(
              "Illegal value = %s or paths = %s",
              StringUtils.join(paths, "."), path));
    }

    return child;
  }

  @SuppressWarnings("unchecked")
  Object setObjectRecursive(Object current, final List<String> paths,
                            int index, final Object value) {

    boolean isLastIndex = index == paths.size();
    if (isLastIndex) {
      return value;
    }

    String path = paths.get(index).trim();
    boolean isNeedMap = isPathMap(path);
    if (isNeedMap) {
      Map<String, Object> mapping;

      boolean isCurrentMap = current instanceof Map;
      if (!isCurrentMap) {
        mapping = new HashMap<String, Object>();
        mapping.put(
            path,
            buildObject(paths.subList(index + 1, paths.size()),
                value));
        return mapping;
      }

      mapping = ((Map<String, Object>) current);
      boolean hasSameKey = mapping.containsKey(path);
      if (!hasSameKey) {
        mapping.put(
            path,
            buildObject(paths.subList(index + 1, paths.size()),
                value));
        return mapping;
      }

      current = mapping.get(path);
      mapping.put(path,
          setObjectRecursive(current, paths, index + 1, value));
      return mapping;
    }

    boolean isNeedList = isPathList(path);
    if (isNeedList) {
      List<Object> lists;
      int listIndexer = getIndex(path);

      boolean isCurrentList = current instanceof List;
      if (!isCurrentList) {
        lists = expand(new ArrayList<Object>(), listIndexer + 1);
        lists.set(
            listIndexer,
            buildObject(paths.subList(index + 1, paths.size()),
                value));
        return lists;
      }

      lists = (List<Object>) current;
      lists = expand(lists, listIndexer + 1);

      boolean hasSameIndex = lists.get(listIndexer) != null;
      if (!hasSameIndex) {
        lists.set(
            listIndexer,
            buildObject(paths.subList(index + 1, paths.size()),
                value));
        return lists;
      }

      current = lists.get(listIndexer);
      lists.set(listIndexer,
          setObjectRecursive(current, paths, index + 1, value));
      return lists;
    }

    throw BitSailException.asBitSailException(CommonErrorCode.RUNTIME_ERROR, "Unexpected exception");
  }

  private <T> T findObjectByConfig(final String path, final ConfigOption<T> configOption) throws Exception {
    Object result = null;
    Class clazz = configOption.typeClass();
    if (Objects.nonNull(clazz)) {
      switch (ConfigType.valueOf(clazz.getSimpleName())) {
        case String:
          result = getString(path);
          break;
        case Boolean:
          result = getBool(path);
          break;
        case Character:
          result = getChar(path);
          break;
        case Float:
          result = getFloat(path);
          break;
        case Double:
          result = getDouble(path);
          break;
        case Integer:
          result = getInt(path);
          break;
        case List:
          result = getList(path);
          break;
        case Long:
          result = getLong(path);
          break;
        case Map:
          result = getMap(path);
          break;
        default:
          throw new Exception();
      }
    }
    if (result == null && Objects.nonNull(configOption.getTypeReference())) {
      result = getObject(path, configOption.getTypeReference());
    }

    if (result == null) {
      throw new Exception("wait for deep check by ConfigOption");
    }

    return (T) result;
  }

  private Object findObject(final String path) {
    boolean isRootQuery = StringUtils.isBlank(path);
    if (isRootQuery) {
      return this.root;
    }

    Object target = this.root;

    for (final String each : split2List(path)) {
      if (isPathMap(each)) {
        target = findObjectInMap(target, each);
        continue;
      } else {
        target = findObjectInList(target, each);
        continue;
      }
    }

    return target;
  }

  @SuppressWarnings("unchecked")
  private Object findObjectInMap(final Object target, final String index) {
    boolean isMap = (target instanceof Map);
    if (!isMap) {
      throw new IllegalArgumentException(String.format(
          "Path %s should be map type, but now is %s",
          index, target.getClass().toString()));
    }

    Object result = ((Map<String, Object>) target).get(index);
    if (null == result) {
      throw new IllegalArgumentException(String.format(
          "The configuration is illegal, can't find key field %s in the map value.", index));
    }

    return result;
  }

  @SuppressWarnings({"unchecked"})
  private Object findObjectInList(final Object target, final String each) {
    boolean isList = (target instanceof List);
    if (!isList) {
      throw new IllegalArgumentException(String.format(
          "Path %s should be list type, but now is %s",
          each, target.getClass().toString()));
    }

    String index = each.replace("[", "").replace("]", "");
    if (!StringUtils.isNumeric(index)) {
      throw new IllegalArgumentException(
          String.format(
              "The configuration is illegal, can't find element %s in the list value.", index));
    }

    return ((List<Object>) target).get(Integer.valueOf(index));
  }

  private List<Object> expand(List<Object> list, int size) {
    int expand = size - list.size();
    while (expand-- > 0) {
      list.add(null);
    }
    return list;
  }

  private boolean isPathList(final String path) {
    return path.contains("[") && path.contains("]");
  }

  private boolean isPathMap(final String path) {
    return StringUtils.isNotBlank(path) && !isPathList(path);
  }

  private int getIndex(final String index) {
    return Integer.valueOf(index.replace("[", "").replace("]", ""));
  }

  private boolean isSuitForRoot(final Object object) {
    return null != object && (object instanceof List || object instanceof Map);

  }

  private String split(final String path) {
    return StringUtils.replace(path, "[", ".[");
  }

  private List<String> split2List(final String path) {
    return Arrays.asList(StringUtils.split(split(path), "."));
  }

  private void checkPath(final String path) {
    if (null == path) {
      throw new IllegalArgumentException(
          "Path is empty.");
    }

    for (final String each : StringUtils.split(".")) {
      if (StringUtils.isBlank(each)) {
        throw new IllegalArgumentException("Path can't blank between the dot.");
      }
    }
  }

  @SuppressWarnings("unused")
  private String toJSONPath(final String path) {
    return (StringUtils.isBlank(path) ? "$" : "$." + path).replace("$.[",
        "$[");
  }

  public Map<String, String> getUnNecessaryMap(final ConfigOption option) {
    if (!this.fieldExists(option)) {
      return new HashMap<>();
    }
    return getFlattenMap(option.key());
  }

  public Map<String, String> getFlattenMap(String path) {
    Map<String, Object> map = (Map<String, Object>) this.getMap(path);
    if (Objects.isNull(map)) {
      return new HashMap<>();
    }
    return flatten(map);
  }

  private Map<String, String> flatten(Map<String, Object> in) {
    return in.entrySet().stream()
        .flatMap(entry -> flatten(entry).entrySet().stream())
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            Map.Entry::getValue));
  }

  private Map<String, String> flatten(Map.Entry<String, Object> in) {
    if (!Map.class.isInstance(in.getValue())) {
      return Collections.singletonMap(in.getKey(), in.getValue().toString());
    }

    String prefix = in.getKey();
    Map<String, Object> values = (Map<String, Object>) in.getValue();
    Map<String, Object> flattenMap = new HashMap<>();
    values.keySet().forEach(key -> {
      flattenMap.put(prefix + "." + key, values.get(key));
    });
    return flatten(flattenMap);
  }

  private enum ConfigType {
    Boolean, Character, Double, Float, Integer, List, Long, Map, String
  }
}
