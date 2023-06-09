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
package org.apache.drill.exec.physical.impl;

import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.common.scanner.persistence.ScanResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OperatorCreatorRegistry {
  private static final Logger logger = LoggerFactory.getLogger(OperatorCreatorRegistry.class);

  private volatile Map<Class<?>, Constructor<?>> constructorRegistry = new HashMap<Class<?>, Constructor<?>>();
  private volatile Map<Class<?>, Object> instanceRegistry = new HashMap<Class<?>, Object>();

  public OperatorCreatorRegistry(ScanResult scanResult) {
    addImplementorsToMap(scanResult, BatchCreator.class);
    addImplementorsToMap(scanResult, RootCreator.class);
    logger.debug("Adding Operator Creator map: {}", constructorRegistry);
  }

  public synchronized Object getOperatorCreator(Class<?> operator) throws ExecutionSetupException {
    Object opCreator = instanceRegistry.get(operator);
    if (opCreator != null) {
      return opCreator;
    }

    Constructor<?> c = constructorRegistry.get(operator);
    if(c == null) {
      throw new ExecutionSetupException(
          String.format("Failure finding OperatorCreator constructor for config %s", operator.getCanonicalName()));
    }
    try {
      opCreator = c.newInstance();
      instanceRegistry.put(operator, opCreator);
      return opCreator;
    } catch (Throwable t) {
      throw ExecutionSetupException.fromThrowable(
          String.format("Failure creating OperatorCreator for Operator %s", operator.getCanonicalName()), t);
    }
  }

  private <T> void addImplementorsToMap(ScanResult scanResult, Class<T> baseInterface) {
    Set<Class<? extends T>> providerClasses = scanResult.getImplementations(baseInterface);
    for (Class<?> c : providerClasses) {
      Class<?> operatorClass = c;
      boolean interfaceFound = false;
      while (!interfaceFound && !(c.equals(java.lang.Object.class))) {
        Type[] ifaces = c.getGenericInterfaces(); // never returns null
        for (Type iface : ifaces) {
          if (!(iface instanceof ParameterizedType
              && ((ParameterizedType)iface).getRawType().equals(baseInterface))) {
            continue;
          }
          Type[] args = ((ParameterizedType)iface).getActualTypeArguments();
          interfaceFound = true;
          boolean constructorFound = false;
          for (Constructor<?> constructor : operatorClass.getConstructors()) {
            Class<?>[] params = constructor.getParameterTypes();
            if (params.length == 0) {
              Constructor<?> old = constructorRegistry.put((Class<?>) args[0], constructor);
              if (old != null) {
                throw new RuntimeException(
                    String.format("Duplicate OperatorCreator [%s, %s] found for PhysicalOperator %s",
                    old.getDeclaringClass().getCanonicalName(), operatorClass.getCanonicalName(),
                    ((Class<?>) args[0]).getCanonicalName()));
              }
              constructorFound = true;
            }
          }
          if (!constructorFound) {
            logger.debug("Skipping registration of OperatorCreator {} as it doesn't have a default constructor",
                operatorClass.getCanonicalName());
          }
        }
        c = c.getSuperclass();
      }
    }
  }
}
