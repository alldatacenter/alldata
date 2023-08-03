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

import com.netease.arctic.flink.util.ProxyUtil;

import java.io.Serializable;

/**
 * Create proxy in runtime to avoid 'ClassNotFoundException: XXX$$EnhancerByCglib'
 *
 * @param <T>
 */
public class ProxyFactory<T> implements Serializable {
  private static final long serialVersionUID = 1L;
  private final Class<T> clazz;
  private final KerberosInterceptor interceptor;
  private final Class[] argumentTypes;
  private final Object[] arguments;

  public ProxyFactory(Class<T> clazz, KerberosInterceptor interceptor,
                      Class[] argumentTypes, Object[] arguments) {
    this.clazz = clazz;
    this.interceptor = interceptor;
    this.argumentTypes = argumentTypes;
    this.arguments = arguments;
  }

  public T getInstance() {
    return ProxyUtil.getProxy(clazz, interceptor, argumentTypes, arguments);
  }
}
