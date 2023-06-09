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
import org.apache.drill.categories.SqlFunctionTest;
import org.apache.drill.exec.expr.fn.DrillFuncHolder;
import org.apache.drill.test.BaseTest;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

@Category(SqlFunctionTest.class)
public class FunctionRegistryHolderTest extends BaseTest {

  private static final String built_in = "built-in";
  private static final String udf_jar = "DrillUDF-1.0.jar";
  private static final String LOWER_FUNC_NAME = "lower";
  private static final String SHUFFLE_FUNC_NAME = "shuffle";

  private static Map<String, List<FunctionHolder>> newJars;
  private FunctionRegistryHolder registryHolder;

  @BeforeClass
  public static void init() {
    newJars = new HashMap<>();
    FunctionHolder lower = new FunctionHolder(LOWER_FUNC_NAME, "lower(VARCHAR-REQUIRED)", mock(DrillFuncHolder.class));
    FunctionHolder upper = new FunctionHolder("upper", "upper(VARCHAR-REQUIRED)", mock(DrillFuncHolder.class));
    FunctionHolder shuffle = new FunctionHolder(SHUFFLE_FUNC_NAME, "shuffle()", mock(DrillFuncHolder.class));
    newJars.put(built_in, new ArrayList<>(Arrays.asList(lower, upper, shuffle)));
    FunctionHolder custom_lower = new FunctionHolder("custom_lower", "lower(VARCHAR-REQUIRED)", mock(DrillFuncHolder.class));
    FunctionHolder custom_upper = new FunctionHolder("custom_upper", "custom_upper(VARCHAR-REQUIRED)", mock(DrillFuncHolder.class));
    FunctionHolder overloaded_shuffle = new FunctionHolder(SHUFFLE_FUNC_NAME, "shuffle(FLOAT8-REQUIRED,FLOAT8-OPTIONAL)", mock(DrillFuncHolder.class));
    newJars.put(udf_jar, new ArrayList<>(Arrays.asList(custom_lower, custom_upper, overloaded_shuffle)));
  }

  @Before
  public void setup() {
    resetRegistry();
    fillInRegistry(1);
  }

  @Test
  public void testVersion() {
    resetRegistry();
    int expectedVersion = 0;
    assertEquals("Initial version should be 0", expectedVersion, registryHolder.getVersion());
    registryHolder.addJars(new HashMap<>(), ++expectedVersion);
    assertEquals("Version can change if no jars were added.", expectedVersion, registryHolder.getVersion());
    fillInRegistry(++expectedVersion);
    assertEquals("Version should have incremented by 1", expectedVersion, registryHolder.getVersion());
    registryHolder.removeJar(built_in);
    assertEquals("Version should have incremented by 1", expectedVersion, registryHolder.getVersion());
    fillInRegistry(++expectedVersion);
    assertEquals("Version should have incremented by 1", expectedVersion, registryHolder.getVersion());
    fillInRegistry(++expectedVersion);
    assertEquals("Version should have incremented by 1", expectedVersion, registryHolder.getVersion());
  }

  @Test
  public void testAddJars() {
    resetRegistry();
    List<String> jars = new ArrayList<>();
    ListMultimap<String, DrillFuncHolder> functionsWithHolders = ArrayListMultimap.create();
    ListMultimap<String, String> functionsWithSignatures = ArrayListMultimap.create();
    Set<String> functionsSet = new HashSet<>();
    for (Map.Entry<String, List<FunctionHolder>> jar : newJars.entrySet()) {
      jars.add(jar.getKey());
      for (FunctionHolder functionHolder : jar.getValue()) {
        functionsWithHolders.put(functionHolder.getName(), functionHolder.getHolder());
        functionsWithSignatures.put(functionHolder.getName(), functionHolder.getSignature());
        functionsSet.add(functionHolder.getName()); //Track unique function names
      }
    }

    int expectedVersion = 0;
    registryHolder.addJars(newJars, ++expectedVersion);
    assertEquals("Version number should match", expectedVersion, registryHolder.getVersion());
    compareTwoLists(jars, registryHolder.getAllJarNames());
    assertEquals(functionsSet.size(), registryHolder.functionsSize());
    compareListMultimaps(functionsWithHolders, registryHolder.getAllFunctionsWithHolders());
    compareListMultimaps(functionsWithSignatures, registryHolder.getAllFunctionsWithSignatures());
  }

  @Test
  public void testAddTheSameJars() {
    resetRegistry();
    Set<String> functionsSet = new HashSet<>();
    List<String> jars = new ArrayList<>();
    ListMultimap<String, DrillFuncHolder> functionsWithHolders = ArrayListMultimap.create();
    ListMultimap<String, String> functionsWithSignatures = ArrayListMultimap.create();
    for (Map.Entry<String, List<FunctionHolder>> jar : newJars.entrySet()) {
      jars.add(jar.getKey());
      for (FunctionHolder functionHolder : jar.getValue()) {
        functionsWithHolders.put(functionHolder.getName(), functionHolder.getHolder());
        functionsWithSignatures.put(functionHolder.getName(), functionHolder.getSignature());
        functionsSet.add(functionHolder.getName()); //Track unique function names
      }
    }
    int expectedVersion = 0;
    registryHolder.addJars(newJars, ++expectedVersion);
    assertEquals("Version number should match", expectedVersion, registryHolder.getVersion());
    compareTwoLists(jars, registryHolder.getAllJarNames());
    assertEquals(functionsSet.size(), registryHolder.functionsSize());
    compareListMultimaps(functionsWithHolders, registryHolder.getAllFunctionsWithHolders());
    compareListMultimaps(functionsWithSignatures, registryHolder.getAllFunctionsWithSignatures());

    // adding the same jars should not cause adding duplicates, should override existing jars only
    registryHolder.addJars(newJars, ++expectedVersion);
    assertEquals("Version number should match", expectedVersion, registryHolder.getVersion());
    compareTwoLists(jars, registryHolder.getAllJarNames());
    assertEquals(functionsSet.size(), registryHolder.functionsSize());
    compareListMultimaps(functionsWithHolders, registryHolder.getAllFunctionsWithHolders());
    compareListMultimaps(functionsWithSignatures, registryHolder.getAllFunctionsWithSignatures());
  }

  @Test
  public void testRemoveJar() {
    registryHolder.removeJar(built_in);
    assertFalse("Jar should be absent", registryHolder.containsJar(built_in));
    assertTrue("Jar should be present", registryHolder.containsJar(udf_jar));
    assertEquals("Functions size should match", newJars.get(udf_jar).size(), registryHolder.functionsSize());
  }

  @Test
  public void testGetAllJarNames() {
    List<String> expectedResult = new ArrayList<>(newJars.keySet());
    compareTwoLists(expectedResult, registryHolder.getAllJarNames());
  }

  @Test
  public void testGetAllJarsWithFunctionHolders() {
    Map<String, List<FunctionHolder>> fnHoldersInRegistry = registryHolder.getAllJarsWithFunctionHolders();
    //Iterate and confirm lists are same
    for (String jarName : newJars.keySet()) {
      List<DrillFuncHolder> expectedHolderList = newJars.get(jarName).stream()
          .map(FunctionHolder::getHolder) //Extract DrillFuncHolder
          .collect(Collectors.toList());
      List<DrillFuncHolder> testHolderList = fnHoldersInRegistry.get(jarName).stream()
          .map(FunctionHolder::getHolder) //Extract DrillFuncHolder
          .collect(Collectors.toList());

      compareTwoLists(expectedHolderList, testHolderList);
    }

    Map<String, String> shuffleFunctionMap = new HashMap<>();
    // Confirm that same function spans multiple jars with different signatures
    //Init: Expected Map of items
    for (String jarName : newJars.keySet()) {
      for (FunctionHolder funcHolder : newJars.get(jarName)) {
        if (SHUFFLE_FUNC_NAME.equals(funcHolder.getName())) {
          shuffleFunctionMap.put(funcHolder.getSignature(), jarName);
        }
      }
    }

    //Test: Remove items from ExpectedMap based on match from testJar's functionHolder items
    for (String testJar : registryHolder.getAllJarNames()) {
      for (FunctionHolder funcHolder : fnHoldersInRegistry.get(testJar)) {
        if (SHUFFLE_FUNC_NAME.equals(funcHolder.getName())) {
          String testSignature = funcHolder.getSignature();
          String expectedJar = shuffleFunctionMap.get(testSignature);
          if (testJar.equals(expectedJar)) {
            shuffleFunctionMap.remove(testSignature);
          }
        }
      }
    }
    assertTrue(shuffleFunctionMap.isEmpty());
  }

  @Test
  public void testGetFunctionNamesByJar() {
    List<String> expectedResult = newJars.get(built_in).stream()
        .map(FunctionHolder::getName)
        .collect(Collectors.toList());
    compareTwoLists(expectedResult, registryHolder.getFunctionNamesByJar(built_in));
  }

  @Test
  public void testGetAllFunctionsWithHoldersWithVersion() {
    ListMultimap<String, DrillFuncHolder> expectedResult = ArrayListMultimap.create();
    for (List<FunctionHolder> functionHolders : newJars.values()) {
      for(FunctionHolder functionHolder : functionHolders) {
        expectedResult.put(functionHolder.getName(), functionHolder.getHolder());
      }
    }
    AtomicInteger version = new AtomicInteger();
    compareListMultimaps(expectedResult, registryHolder.getAllFunctionsWithHolders(version));
    assertEquals("Version number should match", version.get(), registryHolder.getVersion());
  }

  @Test
  public void testGetAllFunctionsWithHolders() {
    ListMultimap<String, DrillFuncHolder> expectedResult = ArrayListMultimap.create();
    for (List<FunctionHolder> functionHolders : newJars.values()) {
      for(FunctionHolder functionHolder : functionHolders) {
        expectedResult.put(functionHolder.getName(), functionHolder.getHolder());
      }
    }
    compareListMultimaps(expectedResult, registryHolder.getAllFunctionsWithHolders());
  }

  @Test
  public void testGetAllFunctionsWithSignatures() {
    ListMultimap<String, String> expectedResult = ArrayListMultimap.create();
    for (List<FunctionHolder> functionHolders : newJars.values()) {
      for(FunctionHolder functionHolder : functionHolders) {
        expectedResult.put(functionHolder.getName(), functionHolder.getSignature());
      }
    }
    compareListMultimaps(expectedResult, registryHolder.getAllFunctionsWithSignatures());
  }

  @Test
  public void testGetHoldersByFunctionNameWithVersion() {
    List<DrillFuncHolder> expectedResult = newJars.values().stream()
        .flatMap(Collection::stream)
        .filter(f -> LOWER_FUNC_NAME.equals(f.getName()))
        .map(FunctionHolder::getHolder)
        .collect(Collectors.toList());

    assertFalse(expectedResult.isEmpty());
    AtomicInteger version = new AtomicInteger();
    compareTwoLists(expectedResult, registryHolder.getHoldersByFunctionName(LOWER_FUNC_NAME, version));
    assertEquals("Version number should match", version.get(), registryHolder.getVersion());
  }

  @Test
  public void testGetHoldersByFunctionName() {
    List<DrillFuncHolder> expectedUniqueResult = new ArrayList<>();
    List<DrillFuncHolder> expectedMultipleResult = new ArrayList<>();
    for (List<FunctionHolder> functionHolders : newJars.values()) {
      for (FunctionHolder functionHolder : functionHolders) {
        if (LOWER_FUNC_NAME.equals(functionHolder.getName())) {
          expectedUniqueResult.add(functionHolder.getHolder());
        } else
          if (SHUFFLE_FUNC_NAME.equals(functionHolder.getName())) {
            expectedMultipleResult.add(functionHolder.getHolder());
          }
      }
    }

    //Test for function with one signature
    assertFalse(expectedUniqueResult.isEmpty());
    compareTwoLists(expectedUniqueResult, registryHolder.getHoldersByFunctionName(LOWER_FUNC_NAME));

    //Test for function with multiple signatures
    assertFalse(expectedMultipleResult.isEmpty());
    compareTwoLists(expectedMultipleResult, registryHolder.getHoldersByFunctionName(SHUFFLE_FUNC_NAME));
  }

  @Test
  public void testContainsJar() {
    assertTrue("Jar should be present in registry holder", registryHolder.containsJar(built_in));
    assertFalse("Jar should be absent in registry holder", registryHolder.containsJar("unknown.jar"));
  }

  @Test
  public void testFunctionsSize() {
    int fnCountInRegistryHolder = 0;
    int fnCountInNewJars = 0;

    Set<String> functionNameSet = new HashSet<>();
    for (List<FunctionHolder> functionHolders : newJars.values()) {
      for (FunctionHolder functionHolder : functionHolders) {
        functionNameSet.add(functionHolder.getName()); //Track unique function names
        fnCountInNewJars++; //Track all functions
      }
    }
    assertEquals("Unique function name count should match", functionNameSet.size(), registryHolder.functionsSize());

    for (String jarName : registryHolder.getAllJarNames()) {
      fnCountInRegistryHolder += registryHolder.getFunctionNamesByJar(jarName).size();
    }

    assertEquals("Function count should match", fnCountInNewJars, fnCountInRegistryHolder);
  }

  @Test
  public void testJarNameByFunctionSignature() {
    FunctionHolder functionHolder = newJars.get(built_in).get(0);
    assertEquals("Jar name should match",
        built_in, registryHolder.getJarNameByFunctionSignature(functionHolder.getName(), functionHolder.getSignature()));
    assertNull("Jar name should be null",
        registryHolder.getJarNameByFunctionSignature("unknown_function", "unknown_function(unknown-input)"));
  }

  private void resetRegistry() {
    registryHolder = new FunctionRegistryHolder();
  }

  private void fillInRegistry(int version) {
    registryHolder.addJars(newJars, version);
  }

  private <T> void compareListMultimaps(ListMultimap<String, T> lm1, ListMultimap<String, T> lm2) {
    Map<String, Collection<T>> m1 = lm1.asMap();
    Map<String, Collection<T>> m2 = lm2.asMap();
    assertEquals("Multimaps size should match", m1.size(), m2.size());
    for (Map.Entry<String, Collection<T>> entry : m1.entrySet()) {
      try {
        compareTwoLists(new ArrayList<>(entry.getValue()), new ArrayList<>(m2.get(entry.getKey())));
      } catch (AssertionError e) {
        throw new AssertionError("Multimaps values should match", e);
      }
    }
  }

  private <T> void compareTwoLists(List<T> l1, List<T> l2) {
    assertEquals("Lists size should match", l1.size(), l2.size());
    l1.forEach(i -> assertTrue("Two lists should have the same values", l2.contains(i)));
  }

}
