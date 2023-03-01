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

package org.apache.celeborn.common.quota

import org.apache.celeborn.common.identity.UserIdentifier
import org.apache.celeborn.common.internal.Logging
import org.apache.celeborn.common.util.Utils

case class Quota(
    var diskBytesWritten: Long = -1,
    var diskFileCount: Long = -1,
    var hdfsBytesWritten: Long = -1,
    var hdfsFileCount: Long = -1) extends Logging {

  def update(userIdentifier: UserIdentifier, name: String, value: Long): Unit = {
    name match {
      case "diskBytesWritten" => diskBytesWritten = value
      case "diskFileCount" => diskFileCount = value
      case "hdfsBytesWritten" => hdfsBytesWritten = value
      case "hdfsFileCount" => hdfsFileCount = value
      case _ => logWarning(s"Unsupported quota name: $name for user: $userIdentifier.")
    }
  }

  def checkQuotaSpaceAvailable(
      userIdentifier: UserIdentifier,
      resourceResumption: ResourceConsumption): (Boolean, String) = {
    val checkResults = Seq(
      checkDiskBytesWritten(userIdentifier, resourceResumption.diskBytesWritten),
      checkDiskFileCount(userIdentifier, resourceResumption.diskFileCount),
      checkHdfsBytesWritten(userIdentifier, resourceResumption.hdfsBytesWritten),
      checkHdfsFileCount(userIdentifier, resourceResumption.hdfsFileCount))
    val exceed = checkResults.foldLeft(false)(_ || _._1)
    val reason = checkResults.foldLeft("")(_ + _._2)
    (!exceed, reason)
  }

  private def checkDiskBytesWritten(
      userIdentifier: UserIdentifier,
      value: Long): (Boolean, String) = {
    val exceed = (diskBytesWritten > 0 && value >= diskBytesWritten)
    var reason = ""
    if (exceed) {
      reason = s"User $userIdentifier used diskBytesWritten (${Utils.bytesToString(value)}) " +
        s"exceeds quota (${Utils.bytesToString(diskBytesWritten)}). "
      logWarning(reason)
    }
    (exceed, reason)
  }

  private def checkDiskFileCount(userIdentifier: UserIdentifier, value: Long): (Boolean, String) = {
    val exceed = (diskFileCount > 0 && value >= diskFileCount)
    var reason = ""
    if (exceed) {
      reason = s"User $userIdentifier used diskFileCount($value) exceeds quota($diskFileCount). "
      logWarning(reason)
    }
    (exceed, reason)
  }

  private def checkHdfsBytesWritten(
      userIdentifier: UserIdentifier,
      value: Long): (Boolean, String) = {
    val exceed = (hdfsBytesWritten > 0 && value >= hdfsBytesWritten)
    var reason = ""
    if (exceed) {
      reason = s"User $userIdentifier used hdfsBytesWritten(${Utils.bytesToString(value)}) " +
        s"exceeds quota(${Utils.bytesToString(hdfsBytesWritten)}). "
      logWarning(reason)
    }
    (exceed, reason)
  }

  private def checkHdfsFileCount(userIdentifier: UserIdentifier, value: Long): (Boolean, String) = {
    val exceed = (hdfsFileCount > 0 && value >= hdfsFileCount)
    var reason = ""
    if (exceed) {
      reason = s"User $userIdentifier used hdfsFileCount($value) exceeds quota($hdfsFileCount). "
      logWarning(reason)
    }
    (exceed, reason)
  }

  override def toString: String = {
    s"Quota[" +
      s"diskBytesWritten=${Utils.bytesToString(diskBytesWritten)}, " +
      s"diskFileCount=$diskFileCount, " +
      s"hdfsBytesWritten=${Utils.bytesToString(hdfsBytesWritten)}, " +
      s"hdfsFileCount=$hdfsFileCount" +
      s"]"
  }
}
