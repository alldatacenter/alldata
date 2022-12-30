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

package com.bytedance.bitsail.base.connector.writer.v1.committable;

import com.bytedance.bitsail.base.connector.writer.v1.comittable.CommittableState;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.IntStream;

public class CommittableStateTest {

  private final List<Long> checkpointList = Arrays.asList(1L, 2L, 3L, 4L);
  private final List<List<String>> committables = Arrays.asList(
      Collections.singletonList("committable_1"),
      Collections.singletonList("committable_2"),
      Collections.singletonList("committable_3"),
      Collections.singletonList("committable_4")
  );
  private final List<CommittableState<String>> expectedCommittableStateList = Arrays.asList(
      new CommittableState<>(1, Collections.singletonList("committable_1")),
      new CommittableState<>(2, Collections.singletonList("committable_2")),
      new CommittableState<>(3, Collections.singletonList("committable_3")),
      new CommittableState<>(4, Collections.singletonList("committable_4"))
  );

  @Test
  public void testFromNavigableMap() {
    TreeMap<Long, List<String>> map = new TreeMap<>();
    IntStream.range(0, 4).boxed().forEach(i -> map.put(checkpointList.get(i), committables.get(i)));

    List<CommittableState<String>> actualCommittableStateList = CommittableState.fromNavigableMap(map);
    IntStream.range(0, 4).boxed().forEach(i -> checkCommittableStateEqual(expectedCommittableStateList.get(i),
        actualCommittableStateList.get(i)));
  }

  private void checkCommittableStateEqual(CommittableState<String> expectedState,
                                          CommittableState<String> actualState) {
    Assert.assertEquals(expectedState.getCheckpointId(), actualState.getCheckpointId());
    Assert.assertArrayEquals(expectedState.getCommittables().toArray(), actualState.getCommittables().toArray());
  }
}
