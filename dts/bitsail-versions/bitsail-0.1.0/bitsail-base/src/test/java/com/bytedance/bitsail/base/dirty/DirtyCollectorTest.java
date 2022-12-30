/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytedance.bitsail.base.dirty;

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.option.CommonOptions;

import org.apache.commons.lang3.tuple.Triple;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DirtyCollectorTest {

  private List<String> allDirtyRecords;

  @Test
  public void testDirtyCollector() {
    String[] expectedDirtyRecords = {
        "dirty_record_0", "dirty_record_2", "dirty_record_3"
    };

    BitSailConfiguration jobConf = BitSailConfiguration.newDefault();
    jobConf.set(CommonOptions.DirtyRecordOptions.DIRTY_COLLECTOR_SIZE, 3);
    jobConf.set(CommonOptions.DirtyRecordOptions.DIRTY_SAMPLE_RATIO, 0.5);

    AbstractDirtyCollector dirtyCollector = new TestDirtyCollector(jobConf, 0);
    for (int i = 0; i < 6; ++i) {
      dirtyCollector.collectDirty("dirty_record_" + i, new Throwable("" + i), 10000L + i);
    }
    dirtyCollector.storeDirtyRecords();

    Assert.assertEquals(3, allDirtyRecords.size());
    Assert.assertArrayEquals(expectedDirtyRecords, allDirtyRecords.toArray());
  }

  class TestDirtyCollector extends AbstractDirtyCollector {

    final List<Triple<Object, Throwable, Long>> dirtyRecords;
    int count = 0;

    public TestDirtyCollector(BitSailConfiguration jobConf, int taskId) {
      super(jobConf, taskId);
      this.dirtyRecords = new ArrayList<>();
    }

    @Override
    protected void collect(Object obj, Throwable e, long processingTime) throws IOException {
      dirtyRecords.add(Triple.of(obj, e, processingTime));
    }

    @Override
    public void close() throws IOException {

    }

    @Override
    public void storeDirtyRecords() {
      allDirtyRecords = new ArrayList<>();
      dirtyRecords.forEach(record -> allDirtyRecords.add(record.getLeft().toString()));
    }

    @Override
    protected boolean shouldSample() {
      Set<Integer> sampleIndices = new HashSet<>(Arrays.asList(0, 2, 3, 5));
      return sampleIndices.contains(count++);
    }
  }
}
