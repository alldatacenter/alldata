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

package com.bytedance.bitsail.base.execution;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class ProcessResultTest {

  List<List<String>> inputDirtyRecords = Arrays.asList(
      Arrays.asList("input_dirty_0", "input_dirty_1"),
      Arrays.asList("input_dirty_2", "input_dirty_3")
  );

  List<List<String>> outputDirtyRecords = Arrays.asList(
      Arrays.asList("output_dirty_0", "output_dirty_1"),
      Arrays.asList("output_dirty_2", "output_dirty_3")
  );

  @Test
  public void testAddDirtyRecords() {
    ProcessResult<?> result = ProcessResult.builder().build();
    inputDirtyRecords.forEach(result::addInputDirtyRecords);
    outputDirtyRecords.forEach(result::addOutputDirtyRecords);

    for (int i = 0; i < 4; ++i) {
      Assert.assertEquals("input_dirty_" + i, result.getInputDirtyRecords().get(i));
      Assert.assertEquals("output_dirty_" + i, result.getOutputDirtyRecords().get(i));
    }
  }
}
