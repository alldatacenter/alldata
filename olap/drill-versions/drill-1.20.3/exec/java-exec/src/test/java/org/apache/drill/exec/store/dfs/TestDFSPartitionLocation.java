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

import org.apache.drill.exec.planner.DFSFilePartitionLocation;
import org.apache.drill.test.DrillTest;
import org.apache.hadoop.fs.Path;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class TestDFSPartitionLocation extends DrillTest {

  private static final Path SELECTION_ROOT = new Path("/tmp/drill");
  private static final Path PARTITION = new Path("/tmp/drill/test_table/first_dir/second_dir/");

  @Test
  public void testDFSFilePartitionLocation() {
    Path file = new Path(PARTITION, "0_0_0.parquet");
    DFSFilePartitionLocation dfsPartition = new DFSFilePartitionLocation(4, SELECTION_ROOT, file, false);
    checkSubdirectories(dfsPartition, file);
  }

  @Test
  public void testDFSDirectoryPartitionLocation() {
    DFSFilePartitionLocation dfsPartition = new DFSFilePartitionLocation(4, SELECTION_ROOT, PARTITION, true);
    checkSubdirectories(dfsPartition, PARTITION);
  }

  private void checkSubdirectories(DFSFilePartitionLocation dfsPartition, Path partition) {
    assertArrayEquals("Wrong partition dirs",  new String[]{"test_table", "first_dir", "second_dir", null}, dfsPartition.getDirs());
    assertEquals("Wrong partition value","test_table", dfsPartition.getPartitionValue(0));
    assertEquals("Wrong partition value", "first_dir", dfsPartition.getPartitionValue(1));
    assertEquals("Wrong partition value", "second_dir", dfsPartition.getPartitionValue(2));
    assertEquals("Wrong partition location", partition, dfsPartition.getEntirePartitionLocation());
  }

}
