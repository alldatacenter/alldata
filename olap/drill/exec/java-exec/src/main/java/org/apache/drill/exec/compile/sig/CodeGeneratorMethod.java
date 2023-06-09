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
package org.apache.drill.exec.compile.sig;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Iterator;

import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.apache.drill.shaded.guava.com.google.common.collect.Iterators;
import com.thoughtworks.paranamer.AnnotationParanamer;
import com.thoughtworks.paranamer.Paranamer;

public class CodeGeneratorMethod implements Iterable<CodeGeneratorArgument> {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CodeGeneratorMethod.class);

  private final String methodName;
  private final Class<?> returnType;
  private final CodeGeneratorArgument[] arguments;
  private final Class<?>[] exs;
  private final Method underlyingMethod;

  public CodeGeneratorMethod(String name, Class<?> returnType) {
    this.methodName = name;
    this.returnType = returnType;
    this.exs = new Class<?>[0];
    this.underlyingMethod = null;
    this.arguments = new CodeGeneratorArgument[0];
  }

  public CodeGeneratorMethod(Method m) {
    this.underlyingMethod = m;
    this.methodName = m.getName();
    this.returnType = m.getReturnType();
//    Paranamer para = new BytecodeReadingParanamer();
    Paranamer para = new AnnotationParanamer();
    String[] parameterNames = para.lookupParameterNames(m, true);
    if (parameterNames == null) {
      throw new RuntimeException(String.format("Unable to read the parameter names for method %s.  This is likely due to the class files not including the appropriate debugging information.  Look up java -g for more information.", m));
    }
    Class<?>[] types = m.getParameterTypes();
    if (parameterNames.length != types.length) {
      throw new RuntimeException(String.format("Unexpected number of parameter names %s.  Expected %s on method %s.", Arrays.toString(parameterNames), Arrays.toString(types), m.toGenericString()));
    }
    arguments = new CodeGeneratorArgument[parameterNames.length];
    for (int i = 0; i < parameterNames.length; i++) {
      arguments[i] = new CodeGeneratorArgument(parameterNames[i], types[i]);
    }
    exs = m.getExceptionTypes();
  }

  public String getMethodName() {
    return methodName;
  }
  public Class<?> getReturnType() {
    return returnType;
  }

  public Iterable<Class<?>> getThrowsIterable() {
    return ImmutableList.copyOf(exs);
  }

  @Override
  public Iterator<CodeGeneratorArgument> iterator() {
    return Iterators.forArray(arguments);
  }

  @Override
  public String toString() {
    return "CodeGeneratorMethod [" + ((underlyingMethod == null) ? "null" : underlyingMethod.toGenericString()) + "]";
  }

}
