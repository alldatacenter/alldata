/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.controller.jmx;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

/**
 *
 */
public final class JMXMetricHolder {
  private static final String NAME_KEY = "name";

  private List<Map<String, Object>> beans;

  public List<Map<String, Object>> getBeans() {
    return beans;
  }

  public void setBeans(List<Map<String, Object>> beans) {
    this.beans = beans;
  }

  @Override
  public String toString() {
    StringBuilder stringBuilder = new StringBuilder();

    for (Map<String, Object> map : beans) {
      for (Map.Entry<String, Object> entry : map.entrySet()) {
        stringBuilder.append("    ").append(entry).append("\n");
      }
    }
    return stringBuilder.toString();
  }

  public List<Object> findAll(List<String> properties) {
    return properties.stream()
      .map(this::find)
      .filter(Optional::isPresent)
      .map(Optional::get)
      .collect(toList());
  }

  public Optional<Object> find(String pattern) {
    JmxPattern jmxPattern = JmxPattern.parse(pattern);
    return beans.stream()
      .map(jmxPattern::extract)
      .filter(Optional::isPresent)
      .map(Optional::get)
      .findFirst();
  }

  private static class JmxPattern {
    private static final Pattern PATTERN = Pattern.compile("(.*?)\\[(\\S+?)\\]");
    private final String beanName;
    private final String propertyName;
    private final @Nullable String key;

    public static JmxPattern parse(String property) {
      String beanName = property.split("/")[0];
      String propertyName = property.split("/")[1];
      String key = null;
      Matcher matcher = PATTERN.matcher(propertyName);
      if (matcher.matches()) {
        propertyName = matcher.group(1);
        key = matcher.group(2);
      }
      return new JmxPattern(beanName, propertyName, key);
    }

    private JmxPattern(String beanName, String propertyName, String key) {
      this.beanName = beanName;
      this.propertyName = propertyName;
      this.key = key;
    }

    public Optional<Object> extract(Map<String, Object> bean) {
      return beanName.equals(name(bean))
        ? Optional.ofNullable(lookupByKey(bean.get(propertyName)))
        : Optional.empty();
    }

    public Object lookupByKey(Object bean) {
      return key != null && bean instanceof Map ? ((Map) bean).get(key) : bean;
    }

    private String name(Map<String, Object> bean) {
      return bean.containsKey(NAME_KEY) ? (String) bean.get(NAME_KEY) : null;
    }
  }
}
