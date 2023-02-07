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
package org.apache.drill.exec.dotdrill;

import java.io.File;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.List;

import static org.junit.Assert.assertTrue;

import org.apache.drill.exec.store.dfs.DrillFileSystem;
import org.apache.drill.test.BaseDirTestWatcher;
import org.apache.drill.test.BaseTest;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

public class TestDotDrillUtil extends BaseTest {

  private static File tempDir;
  private static Path tempPath;
  private static DrillFileSystem dfs;

  @ClassRule
  public static final BaseDirTestWatcher dirTestWatcher = new BaseDirTestWatcher();

  @BeforeClass
  public static void setup() throws Exception {
    Configuration conf = new Configuration();
    conf.set(FileSystem.FS_DEFAULT_NAME_KEY, FileSystem.DEFAULT_FS);
    dfs = new DrillFileSystem(conf);
    tempDir = dirTestWatcher.getTmpDir();
    tempPath = new Path(tempDir.getAbsolutePath());
  }


  @Test //DRILL-6640
  public void testViewFileStatus() throws Exception {
    List<DotDrillFile> dotDrillFiles;

    Files.createFile(Paths.get(tempDir + "/test1.view.drill"));
    Files.createFile(Paths.get(tempDir + "/test2.view.drill"));
    Files.createFile(Paths.get(tempDir + "/test1.txt"));


    // Check for view file by passing file name without extension
    dotDrillFiles = DotDrillUtil.getDotDrills(dfs, tempPath, "test1", DotDrillType.VIEW);
    assertTrue(dotDrillFiles.size() == 1);

    // Check for dot drill file by passing full name
    dotDrillFiles = DotDrillUtil.getDotDrills(dfs, tempPath, "test1.view.drill");
    assertTrue(dotDrillFiles.size() == 1);

    // Check for dot drill files by passing pattern *.drill
    dotDrillFiles = DotDrillUtil.getDotDrills(dfs, tempPath, "*.drill");
    assertTrue(dotDrillFiles.size() >= 2);

    // Check for non existent file
    dotDrillFiles = DotDrillUtil.getDotDrills(dfs, tempPath, "junkfile", DotDrillType.VIEW);
    assertTrue(dotDrillFiles.size() == 0);

    // Check for existing file which is not a drill view file
    dotDrillFiles = DotDrillUtil.getDotDrills(dfs, tempPath, "test1.txt", DotDrillType.VIEW);
    assertTrue(dotDrillFiles.size() == 0);

    // Check for views files by passing file name having glob without any extension
    dotDrillFiles = DotDrillUtil.getDotDrills(dfs, tempPath, "test*", DotDrillType.VIEW);
    assertTrue(dotDrillFiles.size() >= 2);
  }

  @Test //DRILL-6640
  public void testDotFilesStatus() throws Exception {
    String filePrefix = "sample";
    //Creating different Dot Drill files supported for base file name "sample"
    for (DotDrillType dotType : DotDrillType.values()) {
      Files.createFile(Paths.get(tempDir + "/" + filePrefix + dotType.getEnding()));
    }
    // Check Dot File count for "sample" file created for available Drill dot types
    List<DotDrillFile> dotDrillFiles = DotDrillUtil.getDotDrills(dfs, tempPath, "sample");
    assertTrue(dotDrillFiles.size() == DotDrillType.values().length);
  }

}
