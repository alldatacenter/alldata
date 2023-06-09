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

import org.apache.drill.shaded.guava.com.google.common.base.Strings;
import org.apache.commons.io.FileUtils;
import org.apache.drill.common.util.DrillFileUtils;
import org.apache.drill.exec.ExecTest;
import org.apache.drill.test.BaseTest;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.junit.BeforeClass;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * Base test class for file system util classes that will during test initialization
 * setup file system connection and create directories and files needed for unit tests.
 */
public class FileSystemUtilTestBase extends BaseTest {

  /*
    Directory and file structure created during test initialization:
    ../a
    ../a/f.txt
    ../a/.f.txt
    ../a/_f.txt

    ../a/aa
    ../a/aa/f.txt
    ../a/aa/.f.txt
    ../a/aa/_f.txt

    ../b
    ../b/f.txt
    ../b/.f.txt
    ../b/_f.txt

    ../.a
    ../.a/f.txt

    ../_a
    ../_a/f.txt
  */
  protected static FileSystem fs;
  protected static Path base;

  @BeforeClass
  public static void setup() throws IOException {
    // initialize file system
    fs = ExecTest.getLocalFileSystem();

    // create temporary directory with sub-folders and files
    final File tempDir = DrillFileUtils.createTempDir();
    Runtime.getRuntime().addShutdownHook(new Thread(() -> FileUtils.deleteQuietly(tempDir)));
    base = new Path(tempDir.toURI().getPath());

    createDefaultStructure(fs, base, "a", 2);
    createDefaultStructure(fs, base, "b", 1);

    // create hidden directory with file
    Path hiddenDirectory = new Path(base, ".a");
    fs.mkdirs(hiddenDirectory);
    fs.createNewFile(new Path(hiddenDirectory, "f.txt"));

    // create underscore directory with file
    Path underscoreDirectory = new Path(base, "_a");
    fs.mkdirs(underscoreDirectory);
    fs.createNewFile(new Path(underscoreDirectory, "f.txt"));
  }

  private static void createDefaultStructure(FileSystem fs, Path base, String name, int nesting) throws IOException {
    Path newBase = base;
    for (int i = 1; i <= nesting; i++) {
      Path path = new Path(newBase, Strings.repeat(name, i));
      fs.mkdirs(path);
      for (String fileName : Arrays.asList("f.txt", ".f.txt", "_f.txt")) {
        fs.createNewFile(new Path(path, fileName));
      }
      newBase = path;
    }
  }

}
