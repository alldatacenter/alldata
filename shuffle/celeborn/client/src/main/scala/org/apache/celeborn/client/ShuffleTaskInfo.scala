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

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.function
import javax.annotation.concurrent.ThreadSafe

@ThreadSafe
class ShuffleTaskInfo {

  private var currentShuffleIndex: Int = 0
  // task shuffle id -> mapId_taskAttemptId -> attemptIdx
  private val taskShuffleAttemptIdToAttemptId =
    new ConcurrentHashMap[String, ConcurrentHashMap[String, Int]]
  // map attemptId index
  private val taskShuffleAttemptIdIndex =
    new ConcurrentHashMap[String, ConcurrentHashMap[Int, Int]]
  // task shuffle id -> celeborn shuffle id
  private val taskShuffleIdToShuffleId = new ConcurrentHashMap[String, Int]
  // celeborn shuffle id -> task shuffle id
  private val shuffleIdToTaskShuffleId = new ConcurrentHashMap[Int, String]

  val newMapFunc: function.Function[String, ConcurrentHashMap[Int, Int]] =
    new function.Function[String, ConcurrentHashMap[Int, Int]]() {
      override def apply(s: String): ConcurrentHashMap[Int, Int] = {
        new ConcurrentHashMap[Int, Int]()
      }
    }

  val newMapFunc2: function.Function[String, ConcurrentHashMap[String, Int]] =
    new function.Function[String, ConcurrentHashMap[String, Int]]() {
      override def apply(s: String): ConcurrentHashMap[String, Int] = {
        new ConcurrentHashMap[String, Int]()
      }
    }

  def getShuffleId(taskShuffleId: String): Int = {
    taskShuffleIdToShuffleId.synchronized {
      if (taskShuffleIdToShuffleId.containsKey(taskShuffleId)) {
        taskShuffleIdToShuffleId.get(taskShuffleId)
      } else {
        taskShuffleIdToShuffleId.put(taskShuffleId, currentShuffleIndex)
        shuffleIdToTaskShuffleId.put(currentShuffleIndex, taskShuffleId)
        val tempShuffleIndex = currentShuffleIndex
        currentShuffleIndex = currentShuffleIndex + 1
        tempShuffleIndex
      }
    }
  }

  def getAttemptId(taskShuffleId: String, mapId: Int, attemptId: String): Int = {
    val attemptIndex = taskShuffleAttemptIdIndex.computeIfAbsent(taskShuffleId, newMapFunc)
    val attemptIdMap =
      taskShuffleAttemptIdToAttemptId.computeIfAbsent(taskShuffleId, newMapFunc2)
    val mapAttemptId = mapId + "_" + attemptId
    attemptIndex.synchronized {
      if (!attemptIdMap.containsKey(mapAttemptId)) {
        if (attemptIndex.containsKey(mapId)) {
          val index = attemptIndex.get(mapId)
          attemptIdMap.put(mapAttemptId, index + 1)
          attemptIndex.put(mapId, index + 1)
        } else {
          attemptIdMap.put(mapAttemptId, 0)
          attemptIndex.put(mapId, 0)
        }
      }
    }

    attemptIdMap.get(mapAttemptId)
  }

  def removeExpiredShuffle(shuffleId: Int): Unit = {
    if (shuffleIdToTaskShuffleId.containsKey(shuffleId)) {
      val taskShuffleId = shuffleIdToTaskShuffleId.remove(shuffleId)
      taskShuffleIdToShuffleId.remove(taskShuffleId)
      taskShuffleAttemptIdIndex.remove(shuffleId)
      taskShuffleAttemptIdToAttemptId.remove(taskShuffleId)
    }
  }
}
