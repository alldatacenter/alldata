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

package org.apache.celeborn.service.deploy.worker.storage

import java.nio.channels.FileChannel

import io.netty.buffer.{ByteBufUtil, CompositeByteBuf}
import org.apache.hadoop.fs.Path

abstract private[worker] class FlushTask(
    val buffer: CompositeByteBuf,
    val notifier: FlushNotifier) {
  def flush(): Unit
}

private[worker] class LocalFlushTask(
    buffer: CompositeByteBuf,
    fileChannel: FileChannel,
    notifier: FlushNotifier) extends FlushTask(buffer, notifier) {
  override def flush(): Unit = {
    val buffers = buffer.nioBuffers()
    for (buffer <- buffers) {
      while (buffer.hasRemaining) {
        fileChannel.write(buffer)
      }
    }
  }
}

private[worker] class HdfsFlushTask(
    buffer: CompositeByteBuf,
    val path: Path,
    notifier: FlushNotifier) extends FlushTask(buffer, notifier) {
  override def flush(): Unit = {
    val hdfsStream = StorageManager.hadoopFs.append(path, 256 * 1024)
    hdfsStream.write(ByteBufUtil.getBytes(buffer))
    hdfsStream.close()
  }
}
