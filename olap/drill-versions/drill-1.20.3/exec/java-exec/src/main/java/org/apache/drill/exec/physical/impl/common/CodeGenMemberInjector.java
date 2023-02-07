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
package org.apache.drill.exec.physical.impl.common;

import com.sun.codemodel.JVar;
import io.netty.buffer.DrillBuf;
import org.apache.calcite.util.Pair;
import org.apache.drill.exec.expr.ClassGenerator;
import org.apache.drill.exec.expr.holders.ValueHolder;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.shaded.guava.com.google.common.base.Function;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class CodeGenMemberInjector {

  /**
   * Generated code for a class may have several class members, they
   * are initialized by invoking this method when the instance created.
   *
   * @param cg       the class generator
   * @param instance the class instance created by the compiler
   * @param context  the fragment context
   */
  public static void injectMembers(ClassGenerator<?> cg, Object instance, FragmentContext context) {
    Map<Integer, Object> cachedInstances = new HashMap<>();
    for (Map.Entry<Pair<Integer, JVar>, Function<DrillBuf, ? extends ValueHolder>> setter : cg.getConstantVars().entrySet()) {
      try {
        JVar var = setter.getKey().getValue();
        Integer depth = setter.getKey().getKey();
        Object varInstance = getFieldInstance(instance, depth, cachedInstances);
        Field field = varInstance.getClass().getDeclaredField(var.name());
        field.setAccessible(true);
        field.set(varInstance, setter.getValue().apply(context.getManagedBuffer()));
      } catch (ReflectiveOperationException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private static Object getFieldInstance(Object instance, Integer depth, Map<Integer, Object> cache) throws ReflectiveOperationException {
    if (depth <= 1) {
      return instance;
    }
    Object methodInstance = cache.get(depth);
    if (methodInstance != null) {
      return methodInstance;
    }
    methodInstance = getFieldInstance(instance, depth);
    cache.put(depth, methodInstance);
    return methodInstance;
  }

  private static Object getFieldInstance(Object instance, Integer depth) throws ReflectiveOperationException {
    if (depth <= 1) {
      return instance;
    }
    Field field = instance.getClass().getDeclaredField(ClassGenerator.INNER_CLASS_FIELD_NAME);
    field.setAccessible(true);
    return getFieldInstance(field.get(instance), depth - 1);
  }
}
