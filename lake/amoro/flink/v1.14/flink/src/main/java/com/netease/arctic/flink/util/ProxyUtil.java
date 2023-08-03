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

package com.netease.arctic.flink.util;

import com.netease.arctic.flink.interceptor.KerberosInterceptor;
import com.netease.arctic.flink.interceptor.KerberosInvocationHandler;
import com.netease.arctic.flink.interceptor.ProxyFactory;
import com.netease.arctic.io.ArcticFileIO;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;

/**
 * A proxy util wraps an object with the kerberos authenticate ability by {@link KerberosInvocationHandler}.
 */
public class ProxyUtil {

  public static <T> Object getProxy(T obj, KerberosInvocationHandler<T> handler) {
    return handler.getProxy(obj);
  }

  public static <T> Object getProxy(T obj, ArcticFileIO arcticFileIO) {
    KerberosInvocationHandler<T> handler = new KerberosInvocationHandler<>(arcticFileIO);
    return getProxy(obj, handler);
  }

  public static <T> T getProxy(Class<T> clazz, MethodInterceptor interceptor,
                               Class[] argumentTypes, Object[] arguments) {
    Enhancer enhancer = new Enhancer();
    enhancer.setSuperclass(clazz);
    enhancer.setCallback(interceptor);
    return (T) enhancer.create(argumentTypes, arguments);
  }

  public static <T> T getProxy(Class<T> clazz, ArcticFileIO arcticFileIO,
                               Class[] argumentTypes, Object[] arguments) {
    return getProxy(clazz, new KerberosInterceptor(arcticFileIO), argumentTypes, arguments);
  }

  public static <T> ProxyFactory<T> getProxyFactory(Class<T> clazz, ArcticFileIO arcticFileIO,
                                                    Class[] argumentTypes, Object[] arguments) {
    return new ProxyFactory<T>(clazz, new KerberosInterceptor(arcticFileIO), argumentTypes, arguments);
  }

}
