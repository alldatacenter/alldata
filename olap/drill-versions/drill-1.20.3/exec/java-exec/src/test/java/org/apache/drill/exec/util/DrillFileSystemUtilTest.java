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
package org.apache.drill.exec.util;

import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DrillFileSystemUtilTest extends FileSystemUtilTestBase {

  @Test
  public void testListDirectoriesWithoutFilter() throws IOException {
    List<FileStatus> statuses = DrillFileSystemUtil.listDirectories(fs, base, false);
    assertEquals("Directory count should match", 2, statuses.size());
  }

  @Test
  public void testListDirectoriesWithFilter() throws IOException {
    List<FileStatus> statuses = DrillFileSystemUtil.listDirectories(fs, base, false,
      (PathFilter) path -> path.getName().endsWith("a"));
    assertEquals("Directory count should match", 1, statuses.size());
    assertEquals("Directory name should match", "a", statuses.get(0).getPath().getName());
  }

  @Test
  public void testListDirectoriesRecursiveWithoutFilter() throws IOException {
    List<FileStatus> statuses = DrillFileSystemUtil.listDirectories(fs, base, true);
    assertEquals("Directory count should match", 3, statuses.size());
  }

  @Test
  public void testListDirectoriesRecursiveWithFilter() throws IOException {
    List<FileStatus> statuses = DrillFileSystemUtil.listDirectories(fs, base, true,
      (PathFilter) path -> path.getName().endsWith("a"));
    assertEquals("Directory count should match", 2, statuses.size());

    Collections.sort(statuses);
    assertEquals("Directory name should match", "a", statuses.get(0).getPath().getName());
    assertEquals("Directory name should match", "aa", statuses.get(1).getPath().getName());
  }

  @Test
  public void testListFilesWithoutFilter() throws IOException {
    List<FileStatus> statuses = DrillFileSystemUtil.listFiles(fs, new Path(base, "a"), false);
    assertEquals("File count should match", 1, statuses.size());
    assertEquals("File name should match", "f.txt", statuses.get(0).getPath().getName());
  }

  @Test
  public void testListFilesWithFilter() throws IOException {
    List<FileStatus> statuses = DrillFileSystemUtil.listFiles(fs, new Path(base, "a"), false,
      (PathFilter) path -> path.getName().endsWith(".txt"));
    assertEquals("File count should match", 1, statuses.size());
    assertEquals("File name should match", "f.txt", statuses.get(0).getPath().getName());
  }

  @Test
  public void testListFilesRecursiveWithoutFilter() throws IOException {
    List<FileStatus> statuses = DrillFileSystemUtil.listFiles(fs, base, true);
    assertEquals("File count should match", 3, statuses.size());
  }

  @Test
  public void testListFilesRecursiveWithFilter() throws IOException {
    List<FileStatus> statuses = DrillFileSystemUtil.listFiles(fs, base, true,
      (PathFilter) path -> path.getName().endsWith("a") || path.getName().endsWith(".txt"));
    assertEquals("File count should match", 2, statuses.size());

    Collections.sort(statuses);
    assertEquals("File name should match", "f.txt", statuses.get(0).getPath().getName());
    assertEquals("File name should match", "f.txt", statuses.get(1).getPath().getName());
  }

  @Test
  public void testListAllWithoutFilter() throws IOException {
    List<FileStatus> statuses = DrillFileSystemUtil.listAll(fs, new Path(base, "a"), false);
    assertEquals("File count should match", 2, statuses.size());

    Collections.sort(statuses);
    assertEquals("File name should match", "aa", statuses.get(0).getPath().getName());
    assertEquals("File name should match", "f.txt", statuses.get(1).getPath().getName());
  }

  @Test
  public void testListAllWithFilter() throws IOException {
    List<FileStatus> statuses = DrillFileSystemUtil.listAll(fs, new Path(base, "a"), false,
      (PathFilter) path -> path.getName().endsWith("a") || path.getName().endsWith(".txt"));
    assertEquals("Directory and file count should match", 2, statuses.size());

    Collections.sort(statuses);
    assertEquals("Directory name should match", "aa", statuses.get(0).getPath().getName());
    assertEquals("File name should match", "f.txt", statuses.get(1).getPath().getName());
  }

  @Test
  public void testListAllRecursiveWithoutFilter() throws IOException {
    List<FileStatus> statuses = DrillFileSystemUtil.listAll(fs, base, true);
    assertEquals("Directory and file count should match", 6, statuses.size());
  }

  @Test
  public void testListAllRecursiveWithFilter() throws IOException {
    List<FileStatus> statuses = DrillFileSystemUtil.listAll(fs, new Path(base, "a"), true,
      (PathFilter) path -> path.getName().startsWith("a") || path.getName().endsWith(".txt"));
    assertEquals("Directory and file count should match", 3, statuses.size());

    Collections.sort(statuses);
    assertEquals("Directory name should match", "aa", statuses.get(0).getPath().getName());
    assertEquals("File name should match", "f.txt", statuses.get(1).getPath().getName());
    assertEquals("File name should match", "f.txt", statuses.get(2).getPath().getName());
  }

  @Test
  public void testListDirectoriesSafe() {
    Path file = new Path(base, "missing");
    List<FileStatus> fileStatuses = DrillFileSystemUtil.listDirectoriesSafe(fs, file, true);
    assertTrue("Should return empty result", fileStatuses.isEmpty());
  }

  @Test
  public void testListFilesSafe() {
    Path file = new Path(base, "missing.txt");
    List<FileStatus> fileStatuses = DrillFileSystemUtil.listFilesSafe(fs, file, true);
    assertTrue("Should return empty result", fileStatuses.isEmpty());
  }

  @Test
  public void testListAllSafe() {
    Path file = new Path(base, "missing");
    List<FileStatus> fileStatuses = DrillFileSystemUtil.listAllSafe(fs, file, true);
    assertTrue("Should return empty result", fileStatuses.isEmpty());
  }

}
