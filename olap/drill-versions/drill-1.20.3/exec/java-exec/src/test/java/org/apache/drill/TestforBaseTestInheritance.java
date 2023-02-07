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
package org.apache.drill;

import org.apache.drill.categories.UnlikelyTest;
import org.apache.drill.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public class TestforBaseTestInheritance extends BaseTest {

  @Test
  @Category(UnlikelyTest.class)
  public void verifyInheritance() {
    // Get all BaseTest inheritors
    Reflections reflections = new Reflections("org.apache.drill", new SubTypesScanner(false));
    Set<Class<? extends BaseTest>> baseTestInheritors = reflections.getSubTypesOf(BaseTest.class);

    // Get all tests that are not inherited from BaseTest
    Set<String> testClasses = reflections.getSubTypesOf(Object.class).stream()
        .filter(c -> !c.isInterface())
        .filter(c -> c.getSimpleName().toLowerCase().contains("test"))
        .filter(c -> Arrays.stream(c.getDeclaredMethods())
                .anyMatch(m -> m.getAnnotation(Test.class) != null))
        .filter(c -> !baseTestInheritors.contains(c))
        .map(Class::getName)
        .collect(Collectors.toSet());

    Assert.assertEquals("Found test classes that are not inherited from BaseTest:", Collections.emptySet(), testClasses);
  }
}
