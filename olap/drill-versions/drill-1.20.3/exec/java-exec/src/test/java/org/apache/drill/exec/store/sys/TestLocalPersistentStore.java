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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.drill.categories.UnlikelyTest;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.exec.store.dfs.DrillFileSystem;
import org.apache.drill.exec.store.sys.store.LocalPersistentStore;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.test.BaseTest;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@Category({UnlikelyTest.class})
public class TestLocalPersistentStore extends BaseTest {

  @Rule
  public TemporaryFolder root = new TemporaryFolder();

  private static final PersistentStoreConfig<String> DEFAULT_STORE_CONFIG = PersistentStoreConfig
    .newJacksonBuilder(new ObjectMapper(), String.class)
    .name("local-test-store")
    .build();

  private static final List<String> ILLEGAL_KEYS = Arrays.asList(
    null, "", "/abc", "a/b/c", "abc/", "C:\\abc", "../abc", "..");

  private static DrillFileSystem fs;

  @BeforeClass
  public static void init() throws Exception {
    Configuration configuration = new Configuration();
    configuration.set(FileSystem.FS_DEFAULT_NAME_KEY, FileSystem.DEFAULT_FS);
    fs = new DrillFileSystem(configuration);
  }

  @Test
  public void testAbsentGet() throws Exception {
    Path path = new Path(root.newFolder("absent-get").toURI().getPath());
    try (LocalPersistentStore<String> store = new LocalPersistentStore<>(fs, path, DEFAULT_STORE_CONFIG);) {

      assertNull(store.get("abc"));

      ILLEGAL_KEYS.stream()
        .map(store::get)
        .forEach(Assert::assertNull);
    }
  }

  @Test
  public void testContains() throws Exception {
    Path path = new Path(root.newFolder("contains").toURI().getPath());
    try (LocalPersistentStore<String> store = new LocalPersistentStore<>(fs, path, DEFAULT_STORE_CONFIG);) {
      store.put("abc", "desc");

      ILLEGAL_KEYS.stream()
        .map(store::contains)
        .forEach(Assert::assertFalse);

      assertFalse(store.contains("a"));
      assertTrue(store.contains("abc"));
    }
  }

  @Test
  public void testPutAndGet() throws Exception {
    Path path = new Path(root.newFolder("put-and-get").toURI().getPath());
    try (LocalPersistentStore<String> store = new LocalPersistentStore<>(fs, path, DEFAULT_STORE_CONFIG);) {

      store.put("abc", "desc");
      assertEquals("desc", store.get("abc"));

      store.put("abc", "new-desc");
      assertEquals("new-desc", store.get("abc"));
    }
  }

  @Test
  public void testIllegalPut() throws Exception {
    Path path = new Path(root.newFolder("illegal-put").toURI().getPath());
    try (LocalPersistentStore<String> store = new LocalPersistentStore<>(fs, path, DEFAULT_STORE_CONFIG);) {

      ILLEGAL_KEYS.forEach(key -> {
        try {
          store.put(key, "desc");
          fail(String.format("Key [%s] should be illegal, put in the store should have failed", key));
        } catch (DrillRuntimeException e) {
          assertTrue(e.getMessage().startsWith("Illegal storage key name"));
        }
      });
    }
  }

  @Test
  public void testPutIfAbsent() throws Exception {
    Path path = new Path(root.newFolder("put-if-absent").toURI().getPath());
    try (LocalPersistentStore<String> store = new LocalPersistentStore<>(fs, path, DEFAULT_STORE_CONFIG);) {

      assertTrue(store.putIfAbsent("abc", "desc"));
      assertFalse(store.putIfAbsent("abc", "new-desc"));
      assertEquals("desc", store.get("abc"));
    }
  }

  @Test
  public void testIllegalPutIfAbsent() throws Exception {
    Path path = new Path(root.newFolder("illegal-put-if-absent").toURI().getPath());
    try (LocalPersistentStore<String> store = new LocalPersistentStore<>(fs, path, DEFAULT_STORE_CONFIG);) {

      ILLEGAL_KEYS.forEach(key -> {
        try {
          store.putIfAbsent(key, "desc");
          fail(String.format("Key [%s] should be illegal, putIfAbsent in the store should have failed", key));
        } catch (DrillRuntimeException e) {
          assertTrue(e.getMessage().startsWith("Illegal storage key name"));
        }
      });
    }
  }

  @Test
  public void testRange() throws Exception {
    Path path = new Path(root.newFolder("range").toURI().getPath());
    try (LocalPersistentStore<String> store = new LocalPersistentStore<>(fs, path, DEFAULT_STORE_CONFIG);) {

      assertEquals(0, Lists.newArrayList(store.getRange(0, 10)).size());

      IntStream.range(0, 10)
        .forEach(i -> store.put("key_" + i, "value_" + i));

      assertEquals(10, Lists.newArrayList(store.getRange(0, 20)).size());
      assertEquals(10, Lists.newArrayList(store.getRange(0, 10)).size());
      assertEquals(9, Lists.newArrayList(store.getRange(0, 9)).size());
      assertEquals(0, Lists.newArrayList(store.getRange(10, 2)).size());
      assertEquals(5, Lists.newArrayList(store.getRange(2, 5)).size());
      assertEquals(0, Lists.newArrayList(store.getRange(0, 0)).size());
      assertEquals(0, Lists.newArrayList(store.getRange(4, 0)).size());
    }
  }
}
