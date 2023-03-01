/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.uniffle.common;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import org.apache.uniffle.common.util.Constants;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class RemoteStorageInfoTest {
  private static final String TEST_PATH = "hdfs://test";
  private static final String CONF_STRING = "k1=v1,k2=v2";
  private static final Map<String, String> confMap = ImmutableMap.of("k1", "v1", "k2", "v2");

  private static Stream<Arguments> confItemsParams() {
    return Stream.of(
        arguments(null, null, String.join(Constants.COMMA_SPLIT_CHAR, TEST_PATH, "empty conf")),
        arguments("", Collections.emptyMap(), String.join(Constants.COMMA_SPLIT_CHAR, TEST_PATH, "empty conf")),
        arguments(CONF_STRING, confMap, String.join(Constants.COMMA_SPLIT_CHAR, TEST_PATH, CONF_STRING))
    );
  }

  @ParameterizedTest
  @NullAndEmptySource
  public void testEmptyStoragePath(String path) {
    RemoteStorageInfo info = new RemoteStorageInfo(path);
    assertTrue(info.isEmpty());
    assertEquals(path, info.getPath());
    assertEquals("Empty Remote Storage", info.toString());
  }

  @ParameterizedTest
  @MethodSource("confItemsParams")
  public void testRemoteStorageInfo(String confString, Map<String, String> confItems, String expectedToString) {
    RemoteStorageInfo info = new RemoteStorageInfo(TEST_PATH, confItems);
    assertFalse(info.isEmpty());
    assertEquals(TEST_PATH, info.getPath());
    assertEquals(Optional.ofNullable(confItems).orElse(Collections.emptyMap()), info.getConfItems());
    assertEquals(Optional.ofNullable(confString).orElse(""), info.getConfString());
    assertEquals(expectedToString, info.toString());

    info = new RemoteStorageInfo(TEST_PATH, confString);
    assertFalse(info.isEmpty());
    assertEquals(TEST_PATH, info.getPath());
    assertEquals(Optional.ofNullable(confItems).orElse(Collections.emptyMap()), info.getConfItems());
    assertEquals(Optional.ofNullable(confString).orElse(""), info.getConfString());
    assertEquals(expectedToString, info.toString());
  }

  @ParameterizedTest
  @ValueSource(strings = {",", "=,", ",="})
  public void testUncommonConfString(String confString) {
    RemoteStorageInfo info = new RemoteStorageInfo(TEST_PATH, confString);
    assertEquals(TEST_PATH, info.getPath());
    assertEquals(Collections.emptyMap(), info.getConfItems());
    assertEquals("", info.getConfString());
  }

  @Test
  public void testEquals() {
    RemoteStorageInfo info = new RemoteStorageInfo(TEST_PATH, confMap);
    assertEquals(info, info);
    assertNotEquals(info, null);
    assertNotEquals(info, new Object());
    RemoteStorageInfo info1 = new RemoteStorageInfo(TEST_PATH, CONF_STRING);
    assertEquals(info, info1);
    RemoteStorageInfo info2 = new RemoteStorageInfo(TEST_PATH + "2", confMap);
    assertNotEquals(info, info2);
  }

  @ParameterizedTest
  @ValueSource(strings = {"k1=v1", "k1=v1,k2=v3", "k1=v1,k3=v2"})
  public void testNotEquals(String confString) {
    RemoteStorageInfo info = new RemoteStorageInfo(TEST_PATH, CONF_STRING);
    RemoteStorageInfo info2 = new RemoteStorageInfo(TEST_PATH, confString);
    assertNotEquals(info, info2);
  }

  @Test
  public void testHashCode() {
    RemoteStorageInfo info = new RemoteStorageInfo(TEST_PATH, confMap);
    RemoteStorageInfo info1 = new RemoteStorageInfo(TEST_PATH, CONF_STRING);
    assertEquals(info.hashCode(), info1.hashCode());
  }
}
