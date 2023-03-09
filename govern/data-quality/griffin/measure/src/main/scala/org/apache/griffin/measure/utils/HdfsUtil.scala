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

import org.apache.hadoop.fs.{FSDataInputStream, FSDataOutputStream, Path}

import org.apache.griffin.measure.Loggable

object HdfsUtil extends Loggable {

  private val seprator = "/"

  private def getFS(implicit path: Path) = FSUtil.getFileSystem(path.toString)

  def existPath(filePath: String): Boolean = {
    try {
      implicit val path: Path = new Path(filePath)
      getFS.exists(path)
    } catch {
      case _: Throwable => false
    }
  }

  def existFileInDir(dirPath: String, fileName: String): Boolean = {
    val filePath = getHdfsFilePath(dirPath, fileName)
    existPath(filePath)
  }

  def createFile(filePath: String): FSDataOutputStream = {
    implicit val path: Path = new Path(filePath)
    if (getFS.exists(path)) getFS.delete(path, true)
    getFS.create(path)
  }

  def appendOrCreateFile(filePath: String): FSDataOutputStream = {
    implicit val path: Path = new Path(filePath)
    if (getFS.getConf.getBoolean("dfs.support.append", false) && getFS.exists(path)) {
      getFS.append(path)
    } else createFile(filePath)
  }

  def openFile(filePath: String): FSDataInputStream = {
    implicit val path: Path = new Path(filePath)
    getFS.open(path)
  }

  def writeContent(filePath: String, message: String): Unit = {
    val out = createFile(filePath)
    out.write(message.getBytes("utf-8"))
    out.close()
  }

  def withHdfsFile(filePath: String, appendIfExists: Boolean = true)(
      f: FSDataOutputStream => Unit): Unit = {
    val out =
      if (appendIfExists) {
        appendOrCreateFile(filePath)
      } else {
        createFile(filePath)
      }

    f(out)
    out.close()
  }

  def createEmptyFile(filePath: String): Unit = {
    val out = createFile(filePath)
    out.close()
  }

  def getHdfsFilePath(parentPath: String, fileName: String): String = {
    if (parentPath.endsWith(seprator)) parentPath + fileName else parentPath + seprator + fileName
  }

  def deleteHdfsPath(dirPath: String): Unit = {
    try {
      implicit val path: Path = new Path(dirPath)
      if (getFS.exists(path)) getFS.delete(path, true)
    } catch {
      case e: Throwable => error(s"delete path [$dirPath] error: ${e.getMessage}", e)
    }
  }

  def listSubPathsByType(
      dirPath: String,
      subType: String,
      fullPath: Boolean = false): Iterable[String] = {
    if (existPath(dirPath)) {
      try {
        implicit val path: Path = new Path(dirPath)
        val fileStatusArray = getFS.listStatus(path)
        fileStatusArray
          .filter { fileStatus =>
            subType match {
              case "dir" => fileStatus.isDirectory
              case "file" => fileStatus.isFile
              case _ => true
            }
          }
          .map { fileStatus =>
            val fname = fileStatus.getPath.getName
            if (fullPath) getHdfsFilePath(dirPath, fname) else fname
          }
      } catch {
        case e: Throwable =>
          warn(s"list path [$dirPath] warn: ${e.getMessage}", e)
          Nil
      }
    } else Nil
  }

  def listSubPathsByTypes(
      dirPath: String,
      subTypes: Iterable[String],
      fullPath: Boolean = false): Iterable[String] = {
    subTypes.flatMap { subType =>
      listSubPathsByType(dirPath, subType, fullPath)
    }
  }

  def fileNameFromPath(filePath: String): String = {
    val path = new Path(filePath)
    path.getName
  }
}
