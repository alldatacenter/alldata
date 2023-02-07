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
package org.apache.drill.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.apache.drill.test.BaseTest;
import org.junit.Test;

/**
 * Test class for {@code Version}
 *
 */
public class TestVersion extends BaseTest {

  @Test
  public void testSnapshotVersion() {
    Version version = new Version("1.2.3-SNAPSHOT", 1, 2, 3, 0, "SNAPSHOT");

    assertEquals("1.2.3-SNAPSHOT", version.getVersion());
    assertEquals(1, version.getMajorVersion());
    assertEquals(2, version.getMinorVersion());
    assertEquals(3, version.getPatchVersion());
    assertEquals(0, version.getBuildNumber());
    assertEquals("SNAPSHOT", version.getQualifier());
  }

  @Test
  public void testReleaseVersion() {
    Version version = new Version("2.1.4", 2, 1, 4, 0, "");

    assertEquals("2.1.4", version.getVersion());
    assertEquals(2, version.getMajorVersion());
    assertEquals(1, version.getMinorVersion());
    assertEquals(4, version.getPatchVersion());
    assertEquals(0, version.getBuildNumber());
    assertEquals("", version.getQualifier());
  }

  @Test
  public void testBuildNumberVersion() {
    Version version = new Version("3.1.5-2-BUGFIX", 3, 1, 5, 2, "BUGFIX");

    assertEquals("3.1.5-2-BUGFIX", version.getVersion());
    assertEquals(3, version.getMajorVersion());
    assertEquals(1, version.getMinorVersion());
    assertEquals(5, version.getPatchVersion());
    assertEquals(2, version.getBuildNumber());
    assertEquals("BUGFIX", version.getQualifier());
  }

  private final Version v1 = new Version("1.2.3-SNAPSHOT", 1, 2, 3, 0, "SNAPSHOT");
  private final Version v2 = new Version("2.1.4", 2, 1, 4, 0, "");
  private final Version v3 = new Version("3.1.5-2-BUGFIX", 3, 1, 5, 2, "BUGFIX");
  private final Version v4 = new Version("1.2.3-snapshot", 1, 2, 3, 0, "snapshot");
  private final Version v5 = new Version("1.2.3", 1, 2, 3, 0, "");

  @Test
  public void testEquals() {
    assertEquals(v1, v1);
    assertNotEquals(v1, v2);
    assertNotEquals(v1, v3);
    assertEquals(v1, v4);
    assertNotEquals(v1, v5);
    assertNotEquals(v1, null);
    assertNotEquals(v1, new Object());
  }

  @Test
  public void testHashcode() {
    assertEquals(v1.hashCode(), v1.hashCode());
    assertNotEquals(v1.hashCode(), v2.hashCode());
    assertNotEquals(v1.hashCode(), v3.hashCode());
    assertEquals(v1.hashCode(), v4.hashCode());
    assertNotEquals(v1.hashCode(), v5.hashCode());
  }

  @Test
  public void testCompareTo() {
    assertTrue(v1.compareTo(v1) == 0);
    assertTrue(v1.compareTo(v2) < 0);
    assertTrue(v1.compareTo(v3) < 0);
    assertTrue(v1.compareTo(v4) == 0);
    assertTrue(v1.compareTo(v5) < 0);
    assertTrue(v1.compareTo(new Version("1.2", 1, 2, 0, 0, "")) > 0);
  }
}
