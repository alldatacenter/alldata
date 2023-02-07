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
/**
 * <p>
 * This package stores the JUnit test categories. The theory behind categories is that there are three broad categories of tests:
 *
 * <ul>
 *   <li><b>FastTest:</b> These are fast tests, which are effective smoke tests and are run by default. Tests that fall into this category are simply the tests
 *   that are not explicitly labeled as a {@link org.apache.drill.categories.SlowTest} or {@link org.apache.drill.categories.UnlikelyTest}.</li>
 *   <li>{@link org.apache.drill.categories.SlowTest}: These tests run slowly, and are disabled by default.</li>
 *   <li>{@link org.apache.drill.categories.UnlikelyTest}: These tests represent corner cases, specific bug fixes, or tests for pieces of code that are unlikely to be changed.
 *   These are disabled by default</li>
 *   <li>{@link org.apache.drill.categories.FlakyTest}: A category for tests that intermittently fail.</li>
 *   <li>{@link org.apache.drill.categories.EasyOutOfMemory}: Inherited class FlakyTest and allow the CI tool uses a new JVM to test the unit.</li>
 * </ul>
 * </p>
 * <p>
 * In additon to each broad category there are functional categories including but not limited to:
 *
 * <ul>
 *   <li>{@link org.apache.drill.categories.SecurityTest}</li>
 *   <li>{@link org.apache.drill.categories.HiveStorageTest}</li>
 *   <li>And more...</li>
 * </ul>
 *
 * Functional categories represent tests for specific piece of drill.
 * </p>
 * <p>
 * Now here are some examples of how to run different sets of tests.
 * </p>
 * <p>
 * In order to run only the <b>FastTests</b> you can do:<br/>
 * <code>mvn -T 1C clean install</code>
 * </p>
 * <p>
 * In order to run only the <b>FastTests</b> and {@link org.apache.drill.categories.UnlikelyTest}s you can do:
 * <code>mvn -T 1C clean install -DexcludedGroups="org.apache.drill.categories.SlowTest"</code>
 * </p>
 * <p>
 * In order to run all the tests you can do:
 * <code>mvn -T 1C clean install -DexcludedGroups=""</code>
 * </p>
 * <p>
 * In order to run all the fast security tests:
 * <code>mvn -T 1C clean install -Dgroups="org.apache.drill.categories.SecurityTest"</code>
 * </p>
 * <p>
 * In order to run all the security tests:
 * <code>mvn -T 1C clean install -Dgroups="org.apache.drill.categories.SecurityTest" -DexcludedGroups=""</code>
 * </p>
 */
package org.apache.drill.categories;

