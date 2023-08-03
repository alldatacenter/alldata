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

package com.netease.arctic.server.utils;

import com.netease.arctic.server.TableManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.function.Function;

public class ThriftServiceProxy<S> implements InvocationHandler {
  private static final Logger LOG = LoggerFactory.getLogger(TableManagementService.class);

  private final S service;
  private final Function<Throwable, Throwable> exceptionTransfer;

  private ThriftServiceProxy(S service, Function<Throwable, Throwable> exceptionTransfer) {
    this.service = service;
    this.exceptionTransfer = exceptionTransfer;
  }

  @SuppressWarnings("unchecked")
  public static <S> S createProxy(Class<S> serviceClazz, S service,
                                  Function<Throwable, Throwable> exceptionTransfer) {
    return (S) Proxy.newProxyInstance(ThriftServiceProxy.class.getClassLoader(),
        new Class<?>[]{serviceClazz}, new ThriftServiceProxy<>(service, exceptionTransfer));
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    Object result;
    try {
      result = method.invoke(service, args);
    } catch (InvocationTargetException e) {
      Throwable exception = e.getTargetException();
      String errorMessage = String.format("Thrift service:%s.%s execute failed",
          service.getClass().getSimpleName(), method.getName());
      LOG.error(errorMessage, exception);
      if (exceptionTransfer != null) {
        throw exceptionTransfer.apply(exception);
      } else {
        throw exception;
      }
    }
    return result;
  }
}
