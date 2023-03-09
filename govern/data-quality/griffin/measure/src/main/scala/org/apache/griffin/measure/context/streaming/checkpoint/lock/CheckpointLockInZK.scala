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

package org.apache.griffin.measure.context.streaming.checkpoint.lock

import java.util.concurrent.TimeUnit

import org.apache.curator.framework.recipes.locks.InterProcessMutex

case class CheckpointLockInZK(@transient mutex: InterProcessMutex) extends CheckpointLock {

  def lock(outtime: Long, unit: TimeUnit): Boolean = {
    try {
      if (outtime >= 0) {
        mutex.acquire(outtime, unit)
      } else {
        mutex.acquire(-1, null)
      }
    } catch {
      case e: Throwable =>
        error(s"lock error: ${e.getMessage}")
        false
    }

  }

  def unlock(): Unit = {
    try {
      if (mutex.isAcquiredInThisProcess) mutex.release()
    } catch {
      case e: Throwable =>
        error(s"unlock error: ${e.getMessage}")
    }
  }

}
