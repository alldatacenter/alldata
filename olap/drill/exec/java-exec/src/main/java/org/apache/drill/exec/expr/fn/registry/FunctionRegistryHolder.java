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
package org.apache.drill.exec.expr.fn.registry;

import org.apache.drill.shaded.guava.com.google.common.collect.ArrayListMultimap;
import org.apache.drill.shaded.guava.com.google.common.collect.ListMultimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.drill.common.AutoCloseables.Closeable;
import org.apache.drill.common.concurrent.AutoCloseableLock;
import org.apache.drill.exec.expr.fn.DrillFuncHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Function registry holder stores function implementations by jar name, function name.
 * Contains two maps that hold data by jars and functions respectively.
 * Jars map contains each jar as a key and map of all its functions with collection of function signatures as value.
 * Functions map contains function name as key and map of its signatures and function holder as value.
 * All maps and collections used are concurrent to guarantee memory consistency effects.
 * Such structure is chosen to achieve maximum speed while retrieving data by jar or by function name,
 * since we expect infrequent registry changes.
 * Holder is designed to allow concurrent reads and single writes to keep data consistent.
 * This is achieved by {@link ReadWriteLock} implementation usage.
 * Holder has number version which indicates remote function registry version number it is in sync with.
 * <p/>
 * Structure example:
 *
 * <pre>
 * JARS
 * built-in   -> upper          -> upper(VARCHAR-REQUIRED)
 *            -> lower          -> lower(VARCHAR-REQUIRED)
 *
 * First.jar  -> upper          -> upper(VARCHAR-OPTIONAL)
 *            -> custom_upper   -> custom_upper(VARCHAR-REQUIRED)
 *                              -> custom_upper(VARCHAR-OPTIONAL)
 *
 * Second.jar -> lower          -> lower(VARCHAR-OPTIONAL)
 *            -> custom_upper   -> custom_upper(VARCHAR-REQUIRED)
 *                              -> custom_upper(VARCHAR-OPTIONAL)
 *
 * FUNCTIONS
 * upper        -> upper(VARCHAR-REQUIRED)        -> function holder for upper(VARCHAR-REQUIRED)
 *              -> upper(VARCHAR-OPTIONAL)        -> function holder for upper(VARCHAR-OPTIONAL)
 *
 * lower        -> lower(VARCHAR-REQUIRED)        -> function holder for lower(VARCHAR-REQUIRED)
 *              -> lower(VARCHAR-OPTIONAL)        -> function holder for lower(VARCHAR-OPTIONAL)
 *
 * custom_upper -> custom_upper(VARCHAR-REQUIRED) -> function holder for custom_upper(VARCHAR-REQUIRED)
 *              -> custom_upper(VARCHAR-OPTIONAL) -> function holder for custom_upper(VARCHAR-OPTIONAL)
 *
 * custom_lower -> custom_lower(VARCHAR-REQUIRED) -> function holder for custom_lower(VARCHAR-REQUIRED)
 *              -> custom_lower(VARCHAR-OPTIONAL) -> function holder for custom_lower(VARCHAR-OPTIONAL)
 * </pre>
 * where
 * <li><b>First.jar</b> is jar name represented by {@link String}.</li>
 * <li><b>upper</b> is function name represented by {@link String}.</li>
 * <li><b>upper(VARCHAR-REQUIRED)</b> is signature name represented by String which consist of function name, list of input parameters.</li>
 * <li><b>function holder for upper(VARCHAR-REQUIRED)</b> is {@link DrillFuncHolder} initiated for each function.</li>
 *
 */
public class FunctionRegistryHolder implements AutoCloseable {
  private static final Logger logger = LoggerFactory.getLogger(FunctionRegistryHolder.class);

  private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
  private final AutoCloseableLock readLock = new AutoCloseableLock(readWriteLock.readLock());
  private final AutoCloseableLock writeLock = new AutoCloseableLock(readWriteLock.writeLock());
  // remote function registry number, it is in sync with
  private int version;

  // jar name, Map<function name, Queue<function signature>
  private final Map<String, Map<String, Queue<String>>> jars;

  // function name, Map<function signature, function holder>
  private final Map<String, Map<String, DrillFuncHolder>> functions;

  public FunctionRegistryHolder() {
    this.functions = new ConcurrentHashMap<>();
    this.jars = new ConcurrentHashMap<>();
  }

  /**
   * This is read operation, so several users at a time can get this data.
   * @return local function registry version number
   */
  public int getVersion() {
    try (@SuppressWarnings("unused") Closeable lock = readLock.open()) {
      return version;
    }
  }

  /**
   * Adds jars to the function registry.
   * If jar with the same name already exists, it and its functions will be removed.
   * Then jar will be added to {@link #jars}
   * and each function will be added using {@link #addFunctions(Map, List)}.
   * Registry version is updated with passed version if all jars were added successfully.
   * This is write operation, so one user at a time can call perform such action,
   * others will wait till first user completes his action.
   *
   * @param newJars jars and list of their function holders, each contains function name, signature and holder
   */
  public void addJars(Map<String, List<FunctionHolder>> newJars, int version) {
    try (@SuppressWarnings("unused") Closeable lock = writeLock.open()) {
      for (Map.Entry<String, List<FunctionHolder>> newJar : newJars.entrySet()) {
        String jarName = newJar.getKey();
        removeAllByJar(jarName);
        Map<String, Queue<String>> jar = new ConcurrentHashMap<>();
        jars.put(jarName, jar);
        addFunctions(jar, newJar.getValue());
      }
      this.version = version;
    }
  }

  /**
   * Removes jar from {@link #jars} and all associated with jar functions from {@link #functions}
   * This is write operation, so one user at a time can call perform such action,
   * others will wait till first user completes his action.
   *
   * @param jarName jar name to be removed
   */
  public void removeJar(String jarName) {
    try (@SuppressWarnings("unused") Closeable lock = writeLock.open()) {
      removeAllByJar(jarName);
    }
  }

  /**
   * Retrieves list of all jars name present in {@link #jars}
   * This is read operation, so several users can get this data.
   *
   * @return list of all jar names
   */
  public List<String> getAllJarNames() {
    try (@SuppressWarnings("unused") Closeable lock = readLock.open()) {
      return new ArrayList<>(jars.keySet());
    }
  }

  /**
   * Retrieves all functions (holders) associated with all the jars
   * This is read operation, so several users can perform this operation at the same time.
   * @return list of all functions, mapped by their sources
   */
  public Map<String, List<FunctionHolder>> getAllJarsWithFunctionHolders() {
    Map<String, List<FunctionHolder>> allFunctionHoldersByJar = new HashMap<>();

    try (@SuppressWarnings("unused") Closeable lock = readLock.open()) {
      for (String jarName : jars.keySet()) {
        //Capture functionHolders here
        List<FunctionHolder> drillFuncHolderList = new LinkedList<>();

        Map<String, Queue<String>> functionsInJar = jars.get(jarName);
        for (Map.Entry<String, Queue<String>> functionEntry : functionsInJar.entrySet()) {
          String fnName = functionEntry.getKey();
          Queue<String> fnSignatureList = functionEntry.getValue();
          //Get all FunctionHolders (irrespective of source)
          Map<String, DrillFuncHolder> functionHolders = functions.get(fnName);
          //Iterate for matching entries and populate new Map
          for (Map.Entry<String, DrillFuncHolder> entry : functionHolders.entrySet()) {
            if (fnSignatureList.contains(entry.getKey())) {
              drillFuncHolderList.add(new FunctionHolder(fnName, entry.getKey(), entry.getValue()));
            }
          }
        }
        allFunctionHoldersByJar.put(jarName, drillFuncHolderList);
      }
    }
    return allFunctionHoldersByJar;
  }

  /**
   * Retrieves all function names associated with the jar from {@link #jars}.
   * Returns empty list if jar is not registered.
   * This is a read operation, so several users can perform this operation at the same time.
   *
   * @param jarName jar name
   * @return list of functions names associated from the jar
   */
  public List<String> getFunctionNamesByJar(String jarName) {
    try (@SuppressWarnings("unused") Closeable lock = readLock.open()){
      Map<String, Queue<String>> functions = jars.get(jarName);
      return functions == null ? new ArrayList<>() : new ArrayList<>(functions.keySet());
    }
  }

  /**
   * Returns list of functions with list of function holders for each functions.
   * Uses guava {@link ListMultimap} structure to return data.
   * If no functions present, will return empty {@link ListMultimap}.
   * If version holder is not null, updates it with current registry version number.
   * This is a read operation, so several users can perform this operation at the same time.
   *
   * @param version version holder
   * @return all functions which their holders
   */
  public ListMultimap<String, DrillFuncHolder> getAllFunctionsWithHolders(AtomicInteger version) {
    try (@SuppressWarnings("unused") Closeable lock = readLock.open()) {
      if (version != null) {
        version.set(this.version);
      }
      ListMultimap<String, DrillFuncHolder> functionsWithHolders = ArrayListMultimap.create();
      for (Map.Entry<String, Map<String, DrillFuncHolder>> function : functions.entrySet()) {
        functionsWithHolders.putAll(function.getKey(), new ArrayList<>(function.getValue().values()));
      }
      return functionsWithHolders;
    }
  }

  /**
   * Returns list of functions with list of function holders for each functions without version number.
   * This is a read operation, so several users can perform this operation at the same time.
   *
   * @return all functions which their holders
   */
  public ListMultimap<String, DrillFuncHolder> getAllFunctionsWithHolders() {
    return getAllFunctionsWithHolders(null);
  }

  /**
   * Returns list of functions with list of function signatures for each functions.
   * Uses guava {@link ListMultimap} structure to return data.
   * If no functions present, will return empty {@link ListMultimap}.
   * This is a read operation, so several users can perform this operation at the same time.
   *
   * @return all functions which their signatures
   */
  public ListMultimap<String, String> getAllFunctionsWithSignatures() {
    try (@SuppressWarnings("unused") Closeable lock = readLock.open()) {
      ListMultimap<String, String> functionsWithSignatures = ArrayListMultimap.create();
      for (Map.Entry<String, Map<String, DrillFuncHolder>> function : functions.entrySet()) {
        functionsWithSignatures.putAll(function.getKey(), new ArrayList<>(function.getValue().keySet()));
      }
      return functionsWithSignatures;
    }
  }

  /**
   * Returns all function holders associated with function name.
   * If function is not present, will return empty list.
   * If version holder is not null, updates it with current registry version number.
   * This is a read operation, so several users can perform this operation at the same time.
   *
   * @param functionName function name
   * @param version version holder
   * @return list of function holders
   */
  public List<DrillFuncHolder> getHoldersByFunctionName(String functionName, AtomicInteger version) {
    try (@SuppressWarnings("unused") Closeable lock = readLock.open()) {
      if (version != null) {
        version.set(this.version);
      }
      Map<String, DrillFuncHolder> holders = functions.get(functionName);
      return holders == null ? new ArrayList<>() : new ArrayList<>(holders.values());
    }
  }

  /**
   * Returns all function holders associated with function name without version number.
   * This is a read operation, so several users can perform this operation at the same time.
   *
   * @param functionName function name
   * @return list of function holders
   */
  public List<DrillFuncHolder> getHoldersByFunctionName(String functionName) {
    return getHoldersByFunctionName(functionName, null);
  }

  /**
   * Checks is jar is present in {@link #jars}.
   * This is a read operation, so several users can perform this operation at the same time.
   *
   * @param jarName jar name
   * @return true if jar exists, else false
   */
  public boolean containsJar(String jarName) {
    try (@SuppressWarnings("unused") Closeable lock = readLock.open()) {
      return jars.containsKey(jarName);
    }
  }

  /**
   * Returns quantity of functions stored in {@link #functions}.
   * This is a read operation, so several users can perform this operation at the same time.
   *
   * @return quantity of functions
   */
  public int functionsSize() {
    try (@SuppressWarnings("unused") Closeable lock = readLock.open()) {
      return functions.size();
    }
  }

  /**
   * Looks which jar in {@link #jars} contains passed function signature.
   * First looks by function name and if found checks if such function has passed function signature.
   * Returns jar name if found matching function signature, else null.
   * This is a read operation, so several users can perform this operation at the same time.
   *
   * @param functionName function name
   * @param functionSignature function signature
   * @return jar name
   */
  public String getJarNameByFunctionSignature(String functionName, String functionSignature) {
    try (@SuppressWarnings("unused") Closeable lock = readLock.open()) {
      for (Map.Entry<String, Map<String, Queue<String>>> jar : jars.entrySet()) {
        Queue<String> functionSignatures = jar.getValue().get(functionName);
        if (functionSignatures != null && functionSignatures.contains(functionSignature)) {
          return jar.getKey();
        }
      }
    }
    return null;
  }

  /**
   * Adds all function names and signatures to passed jar,
   * adds all function names, their signatures and holders to {@link #functions}.
   *
   * @param jar jar where function to be added
   * @param newFunctions collection of function holders, each contains function name, signature and holder.
   */
  private void addFunctions(Map<String, Queue<String>> jar, List<FunctionHolder> newFunctions) {
    for (FunctionHolder function : newFunctions) {
      final String functionName = function.getName();
      Queue<String> jarFunctions = jar.get(functionName);
      if (jarFunctions == null) {
        jarFunctions = new ConcurrentLinkedQueue<>();
        jar.put(functionName, jarFunctions);
      }
      final String functionSignature = function.getSignature();
      jarFunctions.add(functionSignature);

      Map<String, DrillFuncHolder> signatures = functions.computeIfAbsent(functionName, k -> new ConcurrentHashMap<>());
      signatures.put(functionSignature, function.getHolder());
    }
  }

  /**
   * Removes jar from {@link #jars} and all associated with jars functions from {@link #functions}
   * Since each jar is loaded with separate class loader before
   * removing we need to close class loader to release opened connection to jar.
   * All jar functions have the same class loader, so we need to close only one time.
   *
   * @param jarName jar name to be removed
   */
  private void removeAllByJar(String jarName) {
    Map<String, Queue<String>> jar = jars.remove(jarName);
    if (jar == null) {
      return;
    }

    boolean isClosed  = false;
    for (Map.Entry<String, Queue<String>> functionEntry : jar.entrySet()) {
      final String function = functionEntry.getKey();
      Map<String, DrillFuncHolder> functionHolders = functions.get(function);
      Queue<String> functionSignatures = functionEntry.getValue();
      // closes class loader only one time
      isClosed = !isClosed && closeClassLoader(function, functionSignatures);
      functionHolders.keySet().removeAll(functionSignatures);

      if (functionHolders.isEmpty()) {
        functions.remove(function);
      }
    }
  }

  @Override
  public void close() {
    try (@SuppressWarnings("unused") Closeable lock = writeLock.open()) {
      jars.forEach((jarName, jar) -> {
        if (!LocalFunctionRegistry.BUILT_IN.equals(jarName)) {
          for (Map.Entry<String, Queue<String>> functionEntry : jar.entrySet()) {
            if (closeClassLoader(functionEntry.getKey(), functionEntry.getValue())) {
              // class loader is closed, iterates to another jar
              break;
            }
          }
        }
      });
    }
  }

  /**
   * Produces search of {@link DrillFuncHolder} which corresponds to specified {@code String functionName}
   * with signature from {@code Queue<String> functionSignatures},
   * closes its class loader if {@link DrillFuncHolder} is found and returns true. Otherwise false is returned.
   *
   * @param functionName       name of the function
   * @param functionSignatures function signatures
   * @return {@code true} if {@link DrillFuncHolder} was found and attempted to close class loader disregarding the result
   */
  private boolean closeClassLoader(String functionName, Queue<String> functionSignatures) {
    return getDrillFuncHolder(functionName, functionSignatures)
        .map(drillFuncHolder -> {
          ClassLoader classLoader = drillFuncHolder.getClassLoader();
          try {
            ((AutoCloseable) classLoader).close();
          } catch (Exception e) {
            logger.warn("Problem during closing class loader", e);
          }
          return true;
        })
        .orElse(false);
  }

  /**
   * Produces search of {@link DrillFuncHolder} which corresponds to specified {@code String functionName}
   * with signature from {@code Queue<String> functionSignatures} and returns first found instance.
   *
   * @param functionName       name of the function
   * @param functionSignatures function signatures
   * @return {@link Optional} with first found {@link DrillFuncHolder} instance
   */
  private Optional<DrillFuncHolder> getDrillFuncHolder(String functionName, Queue<String> functionSignatures) {
    Map<String, DrillFuncHolder> functionHolders = functions.get(functionName);
    return functionSignatures.stream()
        .map(functionHolders::get)
        .filter(Objects::nonNull)
        .findAny();
  }
}
