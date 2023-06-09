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
package org.apache.drill.exec.coord.zk;

import org.apache.drill.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;

public class TestPathUtils extends BaseTest {

  @Test(expected = NullPointerException.class)
  public void testNullSegmentThrowsNPE() {
    PathUtils.join("", null, "");
  }

  @Test
  public void testJoinPreservesAbsoluteOrRelativePaths() {
    final String actual = PathUtils.join("/a", "/b", "/c");
    final String expected = "/a/b/c";
    Assert.assertEquals("invalid path", expected, actual);

    final String actual2 = PathUtils.join("/a", "b", "c");
    final String expected2 = "/a/b/c";
    Assert.assertEquals("invalid path", expected2, actual2);

    final String actual3 = PathUtils.join("a", "b", "c");
    final String expected3 = "a/b/c";
    Assert.assertEquals("invalid path", expected3, actual3);

    final String actual4 = PathUtils.join("a", "", "c");
    final String expected4 = "a/c";
    Assert.assertEquals("invalid path", expected4, actual4);

    final String actual5 = PathUtils.join("", "", "c");
    final String expected5 = "c";
    Assert.assertEquals("invalid path", expected5, actual5);

    final String actual6 = PathUtils.join("", "", "");
    final String expected6 = "";
    Assert.assertEquals("invalid path", expected6, actual6);

    final String actual7 = PathUtils.join("", "", "/");
    final String expected7 = "/";
    Assert.assertEquals("invalid path", expected7, actual7);

    final String actual8 = PathUtils.join("", "", "c/");
    final String expected8 = "c/";
    Assert.assertEquals("invalid path", expected8, actual8);
  }


  @Test
  public void testNormalizeRemovesRedundantForwardSlashes() {
    final String actual = PathUtils.normalize("/a/b/c");
    final String expected = "/a/b/c";
    Assert.assertEquals("invalid path", expected, actual);

    final String actual2 = PathUtils.normalize("//a//b//c");
    final String expected2 = "/a/b/c";
    Assert.assertEquals("invalid path", expected2, actual2);

    final String actual3 = PathUtils.normalize("///");
    final String expected3 = "/";
    Assert.assertEquals("invalid path", expected3, actual3);

    final String actual4 = PathUtils.normalize("/a");
    final String expected4 = "/a";
    Assert.assertEquals("invalid path", expected4, actual4);

    final String actual5 = PathUtils.normalize("//////");
    final String expected5 = "/";
    Assert.assertEquals("invalid path", expected5, actual5);

    final String actual6 = PathUtils.normalize("");
    final String expected6 = "";
    Assert.assertEquals("invalid path", expected6, actual6);
  }
}
