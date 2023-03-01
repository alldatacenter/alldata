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

package org.apache.celeborn.client

import org.scalatest.funsuite.AnyFunSuite

class ShuffleTaskInfoSuite extends AnyFunSuite {

  test("encode shuffle id & map attemptId") {
    val shuffleTaskInfo = new ShuffleTaskInfo
    val encodeShuffleId = shuffleTaskInfo.getShuffleId("shuffleId")
    assert(encodeShuffleId == 0)

    // another shuffle
    val encodeShuffleId1 = shuffleTaskInfo.getShuffleId("shuffleId1")
    assert(encodeShuffleId1 == 1)

    val encodeShuffleId0 = shuffleTaskInfo.getShuffleId("shuffleId")
    assert(encodeShuffleId0 == 0)

    val encodeAttemptId011 = shuffleTaskInfo.getAttemptId("shuffleId", 1, "attempt1")
    val encodeAttemptId112 = shuffleTaskInfo.getAttemptId("shuffleId1", 1, "attempt2")
    val encodeAttemptId021 = shuffleTaskInfo.getAttemptId("shuffleId", 2, "attempt1")
    val encodeAttemptId012 = shuffleTaskInfo.getAttemptId("shuffleId", 1, "attempt2")
    assert(encodeAttemptId011 == 0)
    assert(encodeAttemptId112 == 0)
    assert(encodeAttemptId021 == 0)
    assert(encodeAttemptId012 == 1)

    // remove shuffleId and reEncode
    shuffleTaskInfo.removeExpiredShuffle(encodeShuffleId)
    val encodeShuffleIdNew = shuffleTaskInfo.getShuffleId("shuffleId")
    assert(encodeShuffleIdNew == 2)
  }

  test("remove none exist shuffle") {
    val shuffleTaskInfo = new ShuffleTaskInfo
    // remove none exist shuffle
    shuffleTaskInfo.removeExpiredShuffle(0)
  }
}
