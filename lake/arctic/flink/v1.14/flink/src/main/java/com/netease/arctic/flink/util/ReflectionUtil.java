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

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * An util for reflection.
 */
public class ReflectionUtil {

  /**
   * get interfaces of class and its parent
   */
  public static Class<?>[] getAllInterface(Class clazz) {
    if (clazz.equals(Object.class)) {
      return new Class[]{};
    }
    Class<?>[] current = clazz.getInterfaces();
    Class superClass = clazz.getSuperclass();
    Class<?>[] superInterfaces = getAllInterface(superClass);

    Set<Class<?>> all = new HashSet<>();
    all.addAll(Arrays.asList(current));
    all.addAll(Arrays.asList(superInterfaces));

    Class<?>[] deduplicated = new Class[all.size()];
    return all.toArray(deduplicated);
  }

  public static <O, V> V getField(Class<O> clazz, O obj, String fieldName) {
    try {
      Field field = clazz.getDeclaredField(fieldName);
      field.setAccessible(true);
      Object v = field.get(obj);
      return v == null ? null : (V) v;
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }
}
