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

package org.apache.griffin.measure.utils

import java.io.File
import java.net.URI

import scala.collection.mutable.{Map => MutableMap}

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileSystem

import org.apache.griffin.measure.Loggable

object FSUtil extends Loggable {

  private val fsMap: MutableMap[String, FileSystem] = MutableMap()
  private val defaultFS: FileSystem = FileSystem.get(getConfiguration)

  def getFileSystem(path: String): FileSystem = {
    getUriOpt(path) match {
      case Some(uri) =>
        fsMap.get(uri.getScheme) match {
          case Some(fs) => fs
          case _ =>
            val fs =
              try {
                FileSystem.get(uri, getConfiguration)
              } catch {
                case e: Throwable =>
                  error(s"get file system error: ${e.getMessage}", e)
                  throw e
              }
            fsMap += (uri.getScheme -> fs)
            fs
        }
      case _ => defaultFS
    }
  }

  private def getConfiguration: Configuration = {
    val conf = new Configuration()
    conf.setBoolean("dfs.support.append", true)
//    conf.set("fs.defaultFS", "hdfs://localhost")    // debug in hdfs localhost env
    conf
  }

  private def getUriOpt(path: String): Option[URI] = {
    val uriOpt =
      try {
        Some(new URI(path))
      } catch {
        case _: Throwable => None
      }
    uriOpt.flatMap { uri =>
      if (uri.getScheme == null) {
        try {
          Some(new File(path).toURI)
        } catch {
          case _: Throwable => None
        }
      } else Some(uri)
    }
  }

}
