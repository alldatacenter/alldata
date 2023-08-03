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

package com.netease.arctic.trace;

import org.apache.iceberg.FileFormat;
import org.apache.iceberg.UpdateProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Wrap {@link UpdateProperties} with {@link TableTracer}.
 */
public class TracedUpdateProperties implements UpdateProperties {

  private final UpdateProperties updateProperties;
  private final TableTracer tracer;
  private Map<String, String> properties;

  public TracedUpdateProperties(UpdateProperties updateProperties, TableTracer tracer) {
    this.updateProperties = updateProperties;
    this.tracer = tracer;
  }

  @Override
  public Map<String, String> apply() {
    Map<String, String> props = updateProperties.apply();
    this.properties = new HashMap<>(props);
    tracer.replaceProperties(this.properties);
    return props;
  }

  @Override
  public void commit() {
    if (this.properties == null) {
      this.apply();
    }
    this.updateProperties.commit();
    this.tracer.commit();
  }

  @Override
  public UpdateProperties set(String key, String value) {
    updateProperties.set(key, value);
    this.properties = null;
    return this;
  }

  @Override
  public UpdateProperties remove(String key) {
    updateProperties.remove(key);
    this.properties = null;
    return this;
  }

  @Override
  public UpdateProperties defaultFormat(FileFormat format) {
    updateProperties.defaultFormat(format);
    this.properties = null;
    return this;
  }
}
