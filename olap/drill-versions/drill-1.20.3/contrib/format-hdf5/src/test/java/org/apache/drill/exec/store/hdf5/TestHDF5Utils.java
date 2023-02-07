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

package org.apache.drill.exec.store.hdf5;

import org.apache.drill.test.BaseTest;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TestHDF5Utils extends BaseTest {

  @Test
  public void testGetNameFromPath() {
    String path1 = "/group1";
    assertEquals(HDF5Utils.getNameFromPath(path1), "group1");
  }

  @Test
  public void testMultiplePath() {
    String path2 = "/group1/group2/group3";
    assertEquals(HDF5Utils.getNameFromPath(path2), "group3");
  }

  @Test
  public void testEmptyPath() {
    String emptyPath = "";
    assertEquals(HDF5Utils.getNameFromPath(emptyPath), "");
  }

  @Test
  public void testNullPath() {
    String nullPath = null;
    assertNull(HDF5Utils.getNameFromPath(nullPath));
  }

  @Test
  public void testRootPath() {
    String rootPath = "/";
    assertEquals(HDF5Utils.getNameFromPath(rootPath), "");
  }
}
