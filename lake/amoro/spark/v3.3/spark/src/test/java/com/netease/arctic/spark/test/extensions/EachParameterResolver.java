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

package com.netease.arctic.spark.test.extensions;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;

public class EachParameterResolver implements ParameterResolver {
  @Override
  public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    return isBeforeOrAfterEachMethod(parameterContext.getDeclaringExecutable()) &&
        isParameterTypeSupported(parameterContext.getDeclaringExecutable());
  }

  @Override
  public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    Class<?> type = parameterContext.getParameter().getType();
    if (type.isAssignableFrom(ExtensionContext.class)) {
      return extensionContext;
    }
    return null;
  }

  private boolean isBeforeOrAfterEachMethod(Executable executable) {
    return executable.getAnnotation(BeforeEach.class) != null ||
        executable.getAnnotation(AfterEach.class) != null;
  }

  private boolean isParameterTypeSupported(Executable executable) {
    Parameter[] parameters = executable.getParameters();
    for (Parameter parameter : parameters) {
      Class<?> type = parameter.getType();
      if (!type.isAssignableFrom(ExtensionContext.class)) {
        return false;
      }
    }
    return true;
  }
}
