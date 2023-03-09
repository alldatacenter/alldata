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

package org.apache.griffin.measure.context.streaming.checkpoint.offset

import org.apache.griffin.measure.configuration.dqdefinition.CheckpointParam
import org.apache.griffin.measure.context.streaming.checkpoint.lock.{
  CheckpointLock,
  CheckpointLockSeq
}

object OffsetCheckpointClient extends OffsetCheckpoint with OffsetOps {
  var offsetCheckpoints: Seq[OffsetCheckpoint] = Nil

  def initClient(checkpointParams: Iterable[CheckpointParam], metricName: String): Unit = {
    val fac = OffsetCheckpointFactory(checkpointParams, metricName)
    offsetCheckpoints = checkpointParams.flatMap(param => fac.getOffsetCheckpoint(param)).toList
  }

  def init(): Unit = offsetCheckpoints.foreach(_.init())
  def available(): Boolean = offsetCheckpoints.foldLeft(false)(_ || _.available)
  def close(): Unit = offsetCheckpoints.foreach(_.close())

  def cache(kvs: Map[String, String]): Unit = {
    offsetCheckpoints.foreach(_.cache(kvs))
  }
  def read(keys: Iterable[String]): Map[String, String] = {
    val maps = offsetCheckpoints.map(_.read(keys)).reverse
    maps.fold(Map[String, String]())(_ ++ _)
  }
  def delete(keys: Iterable[String]): Unit = offsetCheckpoints.foreach(_.delete(keys))
  def clear(): Unit = offsetCheckpoints.foreach(_.clear())

  def listKeys(path: String): List[String] = {
    offsetCheckpoints.foldLeft(Nil: List[String]) { (res, offsetCheckpoint) =>
      if (res.nonEmpty) res else offsetCheckpoint.listKeys(path)
    }
  }

  def genLock(s: String): CheckpointLock = CheckpointLockSeq(offsetCheckpoints.map(_.genLock(s)))

}
