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
package org.apache.drill.common.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.drill.exec.proto.UserBitShared;

import java.lang.reflect.Constructor;
import java.util.Arrays;

/**
 * Utility class which contain methods for conversion Drill ProtoBuf Error and Throwable
 */
public class DrillExceptionUtil {

  /**
   * Recreate throwable from exception protoBuf which received from remote or local node.
   * if no match constructor found, the base Throwable (Exception or Error) is created as a substitution
   *
   * @param exceptionWrapper the exception protoBuf
   * @return Throwable deserialized from protoBuf
   */
  public static Throwable getThrowable(UserBitShared.ExceptionWrapper exceptionWrapper) {
    if (exceptionWrapper == null) {
      return null;
    }
    String className = exceptionWrapper.getExceptionClass();
    if (StringUtils.isBlank(className) || exceptionWrapper.getStackTraceCount() < 1) {
      return null;
    }
    Throwable inner = getThrowable(exceptionWrapper.getCause());
    try {
      Throwable throwable = getInstance(className, exceptionWrapper.getMessage(), inner);
      int size = exceptionWrapper.getStackTraceCount();
      StackTraceElement[] stackTrace = new StackTraceElement[size];
      for (int i = 0; i < size; ++i) {
        UserBitShared.StackTraceElementWrapper w = exceptionWrapper.getStackTrace(i);
        stackTrace[i] = new StackTraceElement(w.getClassName(), w.getMethodName(), w.getFileName(), w.getLineNumber());
      }
      throwable.setStackTrace(stackTrace);
      return throwable;
    } catch (Throwable t) {
      return null;
    }
  }

  /**
   * Get throwable from class name and its constructors, the candidate constructor of exception are:
   * 1) ExceptionClass(String message, Throwable t),
   * 2) ExceptionClass(Throwable t, String message),
   * 3) ExceptionClass(String message).
   * if no match constructor found, the base Throwable (Exception or Error) is created as a substitution
   *
   * @param className the exception class name
   * @param message the exception message
   * @param inner the parent cause of the exception
   * @return Throwable
   */
  private static Throwable getInstance(String className, String message, Throwable inner) throws ReflectiveOperationException {
    Class clazz;
    try {
      clazz = Class.forName(className);
    } catch (ClassNotFoundException e) {
      return new Exception(message, inner);
    }
    Constructor<Throwable>[] constructors = clazz.getConstructors();
    Class<?>[] defaultParameterTypes = new Class<?>[]{String.class, Throwable.class};
    Class<?>[] revertParameterTypes = new Class<?>[]{Throwable.class, String.class};
    Class<?>[] singleParameterType = new Class<?>[]{String.class};
    for (Constructor<Throwable> constructor : constructors) {
      if (Arrays.equals(defaultParameterTypes, constructor.getParameterTypes())) {
        return constructor.newInstance(message, inner);
      }
      if (Arrays.equals(revertParameterTypes, constructor.getParameterTypes())) {
        return constructor.newInstance(inner, message);
      }
      if (inner == null) {
        if (Arrays.equals(singleParameterType, constructor.getParameterTypes())) {
          return constructor.newInstance(message);
        }
      }
    }
    return getBaseInstance(clazz, message, inner);
  }

  private static Throwable getBaseInstance(Class clazz, String message, Throwable inner) {
    if (Error.class.isAssignableFrom(clazz)) {
      return new Error(message, inner);
    }
    return new Exception(message, inner);
  }
}
