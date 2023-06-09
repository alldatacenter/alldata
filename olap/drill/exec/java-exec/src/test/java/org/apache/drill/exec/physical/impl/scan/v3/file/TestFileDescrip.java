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
package org.apache.drill.exec.physical.impl.scan.v3.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.apache.drill.categories.EvfTest;
import org.apache.drill.exec.store.dfs.DrillFileSystem;
import org.apache.drill.exec.store.schedule.CompleteFileWork.FileWorkImpl;
import org.apache.drill.test.BaseTest;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(EvfTest.class)
public class TestFileDescrip extends BaseTest {

  private static final DrillFileSystem dfs;

  static {
    Configuration conf = new Configuration();
    conf.set(FileSystem.FS_DEFAULT_NAME_KEY, FileSystem.DEFAULT_FS);
    try {
      dfs = new DrillFileSystem(conf);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private FileDescrip fileDescrip(Path input, Path root) {
    return new FileDescrip(dfs,
        new FileWorkImpl(0, 1000, input),
        root);
  }

  /**
   * Simple file, no selection root.
   * Should never really occur, but let's test it anyway.
   */
  @Test
  public void testNoRoot() {
    Path input = new Path("file:///foo.csv");
    FileDescrip fd = fileDescrip(input, null);
    assertSame(input, fd.filePath());
    assertEquals(0, fd.dirPathLength());
    assertNull(fd.partition(0));
  }

  /**
   * Normal file, no selection root.
   */
  @Test
  public void testSingleFile() {
    Path input = new Path("file:///a/b/c/foo.csv");
    FileDescrip fd = fileDescrip(input, null);
    assertSame(input, fd.filePath());
    assertEquals(0, fd.dirPathLength());
    assertNull(fd.partition(0));
  }

  /**
   * Normal file, resides in selection root.
   */
  @Test
  public void testRootFile() {
    Path root = new Path("file:///a/b");
    Path input = new Path("file:///a/b/foo.csv");
    FileDescrip fd = fileDescrip(input, root);
    assertSame(input, fd.filePath());
    assertEquals(0, fd.dirPathLength());
    assertNull(fd.partition(0));
  }

  /**
   * Normal file, below selection root.
   */
  @Test
  public void testBelowRoot() {
    Path root = new Path("file:///a/b");
    Path input = new Path("file:///a/b/c/foo.csv");
    FileDescrip fd = fileDescrip(input, root);
    assertSame(input, fd.filePath());
    assertEquals(1, fd.dirPathLength());
    assertEquals("c", fd.partition(0));
    assertNull(fd.partition(1));
  }

  /**
   * Normal file, above selection root.
   * This is an error condition.
   */
  @Test
  public void testAboveRoot() {
    Path root = new Path("file:///a/b");
    Path input = new Path("file:///a/foo.csv");
    try {
      fileDescrip(input, root);
      fail();
    } catch (IllegalArgumentException e) {
      // Expected
    }
  }

  /**
   * Normal file, disjoint with selection root.
   * This is an error condition.
   */
  @Test
  public void testDisjointPath() {
    Path root = new Path("file:///a/b");
    Path input = new Path("file:///d/foo.csv");
    try {
      fileDescrip(input, root);
      fail();
    } catch (IllegalArgumentException e) {
      // Expected
    }
  }
}
