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
package org.apache.drill.exec.store.sys;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.drill.exec.expr.fn.DrillFuncHolder;
import org.apache.drill.exec.expr.fn.FunctionImplementationRegistry;
import org.apache.drill.exec.expr.fn.FunctionLookupContext;
import org.apache.drill.exec.expr.fn.registry.FunctionHolder;
import org.apache.drill.exec.ops.ExecutorFragmentContext;
import org.apache.drill.exec.store.pojo.NonNullable;

/**
 * List functions as a System Table
 */
public class FunctionsIterator implements Iterator<Object> {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(FunctionsIterator.class);

  private final Iterator<FunctionInfo> sortedIterator;

  public FunctionsIterator(ExecutorFragmentContext context) {
    Map<String, FunctionInfo> functionMap = new HashMap<String, FunctionInfo>();
    //Access Registry for function list
    FunctionLookupContext functionLookupContext = context.getFunctionRegistry();
    //Check true instance type
    if (functionLookupContext instanceof FunctionImplementationRegistry) {
      FunctionImplementationRegistry functionImplRegistry = (FunctionImplementationRegistry) functionLookupContext;
      Map<String, List<FunctionHolder>> jarFunctionListMap = functionImplRegistry.getAllJarsWithFunctionsHolders();

      for (String jarName : jarFunctionListMap.keySet()) {
        for (FunctionHolder dfhEntry : jarFunctionListMap.get(jarName)) {
          populateFunctionMap(functionMap, jarName, dfhEntry.getHolder());
        }
      }

      List<FunctionInfo> functionList = new ArrayList<FunctionsIterator.FunctionInfo>(functionMap.values());
      functionList.sort((FunctionInfo o1, FunctionInfo o2) -> {
        int result = o1.name.compareTo(o2.name);
        if (result == 0) {
          result = o1.signature.compareTo(o2.signature);
        }
        if (result == 0) {
          return o1.returnType.compareTo(o2.returnType);
        }
        return result;
      });

      sortedIterator = functionList.iterator();
    } else {
      //Logging error
      logger.error("Function Registry was found to be of {} instead of {}. No functions could be loaded ",
          FunctionImplementationRegistry.class, functionLookupContext.getClass());
      sortedIterator = Collections.emptyIterator();
    }

  }

  /**
   * Populate the map of functionInfo based on the functionSignatureKey for a given Jar-DrillFunctionHolder
   * @param functionMap map to populate
   * @param jarName name of the source jar
   * @param dfh functionHolder that carries all the registered names and the signature
   */
  private void populateFunctionMap(Map<String, FunctionInfo> functionMap, String jarName, DrillFuncHolder dfh) {
    String registeredNames[] = dfh.getRegisteredNames();
    String signature = dfh.getInputParameters();
    String returnType = dfh.getReturnType().getMinorType().toString();
    boolean isInternal = dfh.isInternal();
    for (String name : registeredNames) {
      //Generate a unique key for a function holder as 'functionName#functionSignature'
      //Bumping capacity from default 16 to 64 (since key is expected to be bigger, and reduce probability of resizing)
      String funcSignatureKey = new StringBuilder(64).append(name).append('#').append(signature).toString();
      functionMap.put(funcSignatureKey, new FunctionInfo(name, signature, returnType, jarName, isInternal));
    }
  }

  @Override
  public boolean hasNext() {
    return sortedIterator.hasNext();
  }

  @Override
  public FunctionInfo next() {
    return sortedIterator.next();
  }

  /**
   * Representation of an entry in the System table - Functions
   */
  public static class FunctionInfo {
    @NonNullable
    public final String name;
    @NonNullable
    public final String signature;
    @NonNullable
    public final String returnType;
    @NonNullable
    public final String source;
    @NonNullable
    public final boolean internal;

    public FunctionInfo(String funcName, String funcSignature, String funcReturnType, String jarName) {
      this(funcName, funcSignature, funcReturnType, jarName, false);
    }

    public FunctionInfo(String funcName, String funcSignature, String funcReturnType, String jarName, boolean isInternal) {
      this.name = funcName;
      this.signature = funcSignature;
      this.returnType = funcReturnType;
      this.source = jarName;
      this.internal = isInternal;
    }
  }
}
