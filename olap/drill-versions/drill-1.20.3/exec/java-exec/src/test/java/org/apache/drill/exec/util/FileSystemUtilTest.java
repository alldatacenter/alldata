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

import org.apache.drill.common.exceptions.UserException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class FileSystemUtilTest extends FileSystemUtilTestBase {

  @Test
  public void testListDirectoriesWithoutFilter() throws IOException {
    List<FileStatus> statuses = FileSystemUtil.listDirectories(fs, base, false);
    assertEquals("Directory count should match", 4, statuses.size());
  }

  @Test
  public void testListDirectoriesWithFilter() throws IOException {
    List<FileStatus> statuses = FileSystemUtil.listDirectories(fs, base, false,
      (PathFilter) path -> path.getName().endsWith("a"));
    assertEquals("Directory count should match", 3, statuses.size());

    Collections.sort(statuses);
    assertEquals("Directory name should match", ".a", statuses.get(0).getPath().getName());
    assertEquals("Directory name should match", "_a", statuses.get(1).getPath().getName());
    assertEquals("Directory name should match", "a", statuses.get(2).getPath().getName());
  }

  @Test
  public void testListDirectoriesRecursiveWithoutFilter() throws IOException {
    List<FileStatus> statuses = FileSystemUtil.listDirectories(fs, base, true);
    assertEquals("Directory count should match", 5, statuses.size());
  }

  @Test
  public void testListDirectoriesRecursiveWithFilter() throws IOException {
    List<FileStatus> statuses = FileSystemUtil.listDirectories(fs, base, true,
      (PathFilter) path -> path.getName().endsWith("a"));
    assertEquals("Directory count should match", 4, statuses.size());

    Collections.sort(statuses);
    assertEquals("Directory name should match", ".a", statuses.get(0).getPath().getName());
    assertEquals("Directory name should match", "_a", statuses.get(1).getPath().getName());
    assertEquals("Directory name should match", "a", statuses.get(2).getPath().getName());
    assertEquals("Directory name should match", "aa", statuses.get(3).getPath().getName());
  }

  @Test
  public void testListDirectoriesEmptyResult() throws IOException {
    List<FileStatus> statuses = FileSystemUtil.listDirectories(fs, base, false,
      (PathFilter) path -> path.getName().startsWith("abc"));
    assertEquals("Directory count should match", 0, statuses.size());
  }

  @Test
  public void testListFilesWithoutFilter() throws IOException {
    List<FileStatus> statuses = FileSystemUtil.listFiles(fs, new Path(base, "a"), false);
    assertEquals("File count should match", 3, statuses.size());
  }

  @Test
  public void testListFilesWithFilter() throws IOException {
    List<FileStatus> statuses = FileSystemUtil.listFiles(fs, new Path(base, "a"), false,
      (PathFilter) path -> path.getName().endsWith(".txt"));
    assertEquals("File count should match", 3, statuses.size());

    Collections.sort(statuses);
    assertEquals("File name should match", ".f.txt", statuses.get(0).getPath().getName());
    assertEquals("File name should match", "_f.txt", statuses.get(1).getPath().getName());
    assertEquals("File name should match", "f.txt", statuses.get(2).getPath().getName());
  }

  @Test
  public void testListFilesRecursiveWithoutFilter() throws IOException {
    List<FileStatus> statuses = FileSystemUtil.listFiles(fs, base, true);
    assertEquals("File count should match", 11, statuses.size());
  }

  @Test
  public void testListFilesRecursiveWithFilter() throws IOException {
    List<FileStatus> statuses = FileSystemUtil.listFiles(fs, base, true,
      (PathFilter) path -> path.getName().endsWith("a") || path.getName().endsWith(".txt"));

    assertEquals("File count should match", 8, statuses.size());
  }

  @Test
  public void testListFilesEmptyResult() throws IOException {
    List<FileStatus> statuses = FileSystemUtil.listFiles(fs, base, false);
    assertEquals("File count should match", 0, statuses.size());
  }

  @Test
  public void testListAllWithoutFilter() throws IOException {
    List<FileStatus> statuses = FileSystemUtil.listAll(fs, new Path(base, "a"), false);
    assertEquals("Directory and file count should match", 4, statuses.size());
  }

  @Test
  public void testListAllWithFilter() throws IOException {
    List<FileStatus> statuses = FileSystemUtil.listAll(fs, new Path(base, "a"), false,
      (PathFilter) path -> path.getName().endsWith("a") || path.getName().endsWith(".txt"));
    assertEquals("Directory and file count should match", 4, statuses.size());
  }

  @Test
  public void testListAllRecursiveWithoutFilter() throws IOException {
    List<FileStatus> statuses = FileSystemUtil.listAll(fs, new Path(base, "a"), true);
    assertEquals("Directory and file count should match", 7, statuses.size());
  }

  @Test
  public void testListAllRecursiveWithFilter() throws IOException {
    List<FileStatus> statuses = FileSystemUtil.listAll(fs, new Path(base, "a"), true,
      (PathFilter) path -> path.getName().endsWith("a") || path.getName().endsWith(".txt"));
    assertEquals("Directory and file count should match", 7, statuses.size());
  }

  @Test
  public void testListAllEmptyResult() throws IOException {
    List<FileStatus> statuses = FileSystemUtil.listAll(fs, base, false,
      (PathFilter) path -> path.getName().startsWith("xyz"));
    assertEquals("Directory and file count should match", 0, statuses.size());
  }

  @Test
  public void testMergeFiltersWithMissingParameters() {
    PathFilter filter = path -> path.getName().startsWith("a");

    assertEquals("Should have returned initial filter", filter, FileSystemUtil.mergeFilters(filter, null));
    assertEquals("Should have returned initial filter", filter, FileSystemUtil.mergeFilters(filter, new PathFilter[]{}));
    assertEquals("Should have returned dummy filter", FileSystemUtil.DUMMY_FILTER, FileSystemUtil.mergeFilters());
  }

  @Test
  public void mergeFiltersTrue() {
    Path file = new Path("abc.txt");

    PathFilter firstFilter = path -> path.getName().startsWith("a");
    PathFilter secondFilter = path -> path.getName().endsWith(".txt");

    assertTrue("Path should have been included in the path list", FileSystemUtil.mergeFilters(firstFilter, secondFilter).accept(file));
    assertTrue("Path should have been included in the path list", FileSystemUtil.mergeFilters(firstFilter, new PathFilter[] {secondFilter}).accept(file));
  }

  @Test
  public void mergeFiltersFalse() {
    Path file = new Path("abc.txt");

    PathFilter firstFilter = path -> path.getName().startsWith("a");
    PathFilter secondFilter = path -> path.getName().endsWith(".csv");

    assertFalse("Path should have been excluded from the path list", FileSystemUtil.mergeFilters(firstFilter, secondFilter).accept(file));
    assertFalse("Path should have been excluded from the path list", FileSystemUtil.mergeFilters(firstFilter, new PathFilter[] {secondFilter}).accept(file));
  }

  @Test
  public void testListDirectoriesSafe() {
    Path file = new Path(base, "missing");
    List<FileStatus> fileStatuses = FileSystemUtil.listDirectoriesSafe(fs, file, true);
    assertTrue("Should return empty result", fileStatuses.isEmpty());
  }

  @Test
  public void testListFilesSafe() {
    Path file = new Path(base, "missing.txt");
    List<FileStatus> fileStatuses = FileSystemUtil.listFilesSafe(fs, file, true);
    assertTrue("Should return empty result", fileStatuses.isEmpty());
  }

  @Test
  public void testListAllSafe() {
    Path file = new Path(base, "missing");
    List<FileStatus> fileStatuses = FileSystemUtil.listAllSafe(fs, file, true);
    assertTrue("Should return empty result", fileStatuses.isEmpty());
  }

  @Test // DRILL-8283
  public void testRecursiveListingMaxSize() throws IOException {
    Configuration conf = fs.getConf();
    int oldSize = conf.getInt(FileSystemUtil.RECURSIVE_FILE_LISTING_MAX_SIZE, 0);
    conf.setInt(FileSystemUtil.RECURSIVE_FILE_LISTING_MAX_SIZE, 5);

    try {
      FileSystemUtil.listAll(fs, new Path(base, "a"), true);
    } catch (UserException ex) {
      assertThat(ex.getMessage(), containsString("RESOURCE ERROR: File listing size limit"));
    } finally {
      conf.setInt(FileSystemUtil.RECURSIVE_FILE_LISTING_MAX_SIZE, oldSize);
    }
  }
}
