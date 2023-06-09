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
package org.apache.drill.jdbc;

import java.util.Map;
import java.util.Properties;

import org.apache.drill.shaded.guava.com.google.common.base.Objects;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableMap;

/**
 * An immutable bag of parameters that describes a {@link java.sql.Connection}.
 */
public class ConnectionInfo {
  private final String url;
  private final Map<Object, Object> params;

  public ConnectionInfo(String url, Properties params) {
    this(url, ImmutableMap.copyOf(params));
  }

  public ConnectionInfo(String url, ImmutableMap<Object, Object> params) {
    this.url = Preconditions.checkNotNull(url, "URL cannot be null");
    this.params = params;
  }

  /**
   * Returns connection url.
   */
  public String getUrl() {
    return url;
  }

  /**
   * Returns connection parameters.
   */
  public Map<Object, Object> getParameters() {
    return params;
  }

  /**
   * Creates a new {@link java.util.Properties} instance from underlying parameters.
   */
  public Properties getParamsAsProperties() {
    final Properties props = new Properties();
    for (Object key:params.keySet()) {
      props.put(key, params.get(key));
    }
    return props;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(url, params);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ConnectionInfo) {
      final ConnectionInfo info = ConnectionInfo.class.cast(obj);
      return Objects.equal(url, info.getUrl()) && Objects.equal(params, info.getParameters());
    }
    return false;
  }
}
