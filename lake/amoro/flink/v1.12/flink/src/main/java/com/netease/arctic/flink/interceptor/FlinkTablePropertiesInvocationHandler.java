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

package com.netease.arctic.flink.interceptor;

import com.netease.arctic.flink.util.ReflectionUtil;
import com.netease.arctic.table.ArcticTable;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/**
 * Integrate flinkTable properties
 */
public class FlinkTablePropertiesInvocationHandler implements InvocationHandler, Serializable {

  private ArcticTable arcticTable;
  private final Map<String, String> flinkTableProperties = new HashMap<>();
  protected Map<String, String> tablePropertiesCombined = new HashMap<>();

  public FlinkTablePropertiesInvocationHandler(Map<String, String> flinkTableProperties,
                                               ArcticTable arcticTable) {
    this.tablePropertiesCombined.putAll(arcticTable.properties());
    this.arcticTable = arcticTable;
    if (flinkTableProperties == null) {
      return;
    }
    this.flinkTableProperties.putAll(flinkTableProperties);
    this.tablePropertiesCombined.putAll(flinkTableProperties);
  }

  public Object getProxy() {
    return Proxy.newProxyInstance(arcticTable.getClass().getClassLoader(),
        ReflectionUtil.getAllInterface(arcticTable.getClass()), this);
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    if ("properties".equals(method.getName())) {
      return tablePropertiesCombined;
    } else if ("asKeyedTable".equals(method.getName())) {
      return proxy;
    }
    Object result = method.invoke(arcticTable, args);
    // rewrite the properties as of the arctic table properties may be updated.
    if ("refresh".equals(method.getName())) {
      rewriteProperties();
    }
    return result;
  }

  private void rewriteProperties() {
    Map<String, String> refreshedProperties = arcticTable.properties();
    // iterate through the properties of the arctic table and update the properties of the tablePropertiesCombined.
    for (Map.Entry<String, String> entry : refreshedProperties.entrySet()) {
      if (flinkTableProperties.containsKey(entry.getKey())) {
        // Don't update the properties of the tablePropertiesCombined
        continue;
      }
      if (!tablePropertiesCombined.containsKey(entry.getKey()) ||
          !tablePropertiesCombined.get(entry.getKey()).equals(entry.getValue())) {
        tablePropertiesCombined.put(entry.getKey(), entry.getValue());
      }
    }
  }

}
