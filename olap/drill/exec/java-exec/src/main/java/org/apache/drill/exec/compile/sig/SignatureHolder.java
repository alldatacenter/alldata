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
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableMap;
import org.apache.drill.shaded.guava.com.google.common.collect.Iterators;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.shaded.guava.com.google.common.collect.Maps;

public class SignatureHolder implements Iterable<CodeGeneratorMethod> {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SignatureHolder.class);

  private final Class<?> signature;
  private final CodeGeneratorMethod[] methods;
  private final Map<String, Integer> methodMap;
  private final SignatureHolder[] childHolders;

  public static final String DRILL_INIT_METHOD = "__DRILL_INIT__";
  public static final CodeGeneratorMethod DRILL_INIT = new CodeGeneratorMethod(DRILL_INIT_METHOD, void.class);

  public static SignatureHolder getHolder(Class<?> signature) {
    List<SignatureHolder> innerClasses = Lists.newArrayList();
    for (Class<?> inner : signature.getClasses()) {

      // Do not generate classes for nested enums.
      // (Occurs in HashAggTemplate.)

      if (inner.isEnum()) {
        continue;
      }
      SignatureHolder h = getHolder(inner);
      if (h.childHolders.length > 0 || h.methods.length > 0) {
        innerClasses.add(h);
      }
    }
    return new SignatureHolder(signature, innerClasses.toArray(new SignatureHolder[innerClasses.size()]));
  }

  private SignatureHolder(Class<?> signature, SignatureHolder[] childHolders) {
    this.childHolders = childHolders;
    this.signature = signature;
    Map<String, Integer> newMap = Maps.newHashMap();

    List<CodeGeneratorMethod> methodHolders = Lists.newArrayList();
    Method[] reflectMethods = signature.getDeclaredMethods();

    for (Method m : reflectMethods) {
      if ( (m.getModifiers() & Modifier.ABSTRACT) == 0 && m.getAnnotation(RuntimeOverridden.class) == null) {
        continue;
      }
      methodHolders.add(new CodeGeneratorMethod(m));
    }

    // Alphabetize methods to ensure generated code is comparable.
    // Also eases debugging as the generated code contain different method
    // order from run to run.

    Collections.sort( methodHolders, new Comparator<CodeGeneratorMethod>( ) {
      @Override
      public int compare(CodeGeneratorMethod o1, CodeGeneratorMethod o2) {
        return o1.getMethodName().compareTo( o2.getMethodName() );
      } } );

    methods = new CodeGeneratorMethod[methodHolders.size()+1];
    for (int i =0; i < methodHolders.size(); i++) {
      methods[i] = methodHolders.get(i);
      Integer old = newMap.put(methods[i].getMethodName(), i);
      if (old != null) {
        throw new IllegalStateException(String.format("Attempting to add a method with name %s when there is already one method of that name in this class that is set to be runtime generated.", methods[i].getMethodName()));
      }

    }
    methods[methodHolders.size()] = DRILL_INIT;
    newMap.put(DRILL_INIT.getMethodName(), methodHolders.size());

    methodMap = ImmutableMap.copyOf(newMap);
  }

  public Class<?> getSignatureClass() {
    return signature;
  }

  public CodeGeneratorMethod get(int i) {
    return methods[i];
  }

  @Override
  public Iterator<CodeGeneratorMethod> iterator() {
    return Iterators.forArray(methods);
  }

  public int size() {
    return methods.length;
  }

  public SignatureHolder[] getChildHolders() {
    return childHolders;
  }

  public int get(String method) {
    Integer meth =  methodMap.get(method);
    if (meth == null) {
      throw new IllegalStateException(String.format("Unknown method requested of name %s.", method));
    }
    return meth;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder( );
    buf.append( "SignatureHolder [methods=" );
    final int maxLen = 10;
    for ( int i = 0;  i < maxLen  &&  i < methods.length; i++ ) {
      if ( i > 0 ) {
        buf.append( ", \n" );
      }
      buf.append( methods[i] );
    }
    buf.append( "]" );
    return buf.toString();
  }
}
