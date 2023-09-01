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

package org.apache.uniffle.common;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ShuffleDataResultTest {

  @Test
  public void testEmpty() {
    List<BufferSegment> segments = Collections.singletonList(new BufferSegment(1, 2, 3, 4, 5, 6));
    assertTrue(new ShuffleDataResult().isEmpty());
    assertTrue(new ShuffleDataResult(new byte[1]).isEmpty());
    assertTrue(new ShuffleDataResult(null, segments).isEmpty());
    assertTrue(new ShuffleDataResult(new byte[0], segments).isEmpty());
    assertTrue(new ShuffleDataResult(new byte[1], null).isEmpty());
    assertTrue(new ShuffleDataResult(new byte[1], Collections.emptyList()).isEmpty());
    assertFalse(new ShuffleDataResult(new byte[1], segments).isEmpty());
  }

}
