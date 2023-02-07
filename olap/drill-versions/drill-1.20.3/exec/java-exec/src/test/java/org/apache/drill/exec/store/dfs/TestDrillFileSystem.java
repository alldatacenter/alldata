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
package org.apache.drill.exec.store.dfs;

import org.apache.drill.exec.ops.OpProfileDef;
import org.apache.drill.exec.ops.OperatorStats;
import org.apache.drill.exec.proto.UserBitShared.OperatorProfile;
import org.apache.drill.test.BaseTest;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;
import java.io.PrintWriter;

import static org.junit.Assert.assertTrue;

public class TestDrillFileSystem extends BaseTest {

  private static String tempFilePath;

  @BeforeClass
  public static void createTempFile() throws Exception {

    File tempFile;
    while (true) {
      tempFile = File.createTempFile("drillFSTest", ".txt");
      if (tempFile.exists()) {
        boolean success = tempFile.delete();
        if (success) {
          break;
        }
      }
    }

    // Write some data
    PrintWriter printWriter = new PrintWriter(tempFile);
    for (int i=1; i<=200000; i++) {
      printWriter.println (String.format("%d, key_%d", i, i));
    }
    printWriter.close();

    tempFilePath = tempFile.getPath();
  }

  @Test
  public void testIOStats() throws Exception {
    DrillFileSystem dfs = null;
    InputStream is = null;
    Configuration conf = new Configuration();
    conf.set(FileSystem.FS_DEFAULT_NAME_KEY, FileSystem.DEFAULT_FS);
    OpProfileDef profileDef = new OpProfileDef(0 /*operatorId*/, "" /*operatorType*/, 0 /*inputCount*/);
    OperatorStats stats = new OperatorStats(profileDef, null /*allocator*/);

    // start wait time method in OperatorStats expects the OperatorStats state to be in "processing"
    stats.startProcessing();

    try {
      dfs = new DrillFileSystem(conf, stats);
      is = dfs.open(new Path(tempFilePath));

      byte[] buf = new byte[8000];
      while (is.read(buf, 0, buf.length) != -1) {
      }
    } finally {
      stats.stopProcessing();

      if (is != null) {
        is.close();
      }

      if (dfs != null) {
        dfs.close();
      }
    }

    OperatorProfile operatorProfile = stats.getProfile();
    assertTrue("Expected wait time is non-zero, but got zero wait time", operatorProfile.getWaitNanos() > 0);
  }

  @AfterClass
  public static void deleteTempFile() throws Exception {
    new File(tempFilePath).delete();
  }

}
