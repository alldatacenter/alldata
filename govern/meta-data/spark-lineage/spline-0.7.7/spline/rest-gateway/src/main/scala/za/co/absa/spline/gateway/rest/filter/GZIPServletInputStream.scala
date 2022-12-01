/*
 * Copyright 2020 ABSA Group Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package za.co.absa.spline.gateway.rest.filter

import java.util.zip.GZIPInputStream

import javax.servlet.{ReadListener, ServletInputStream}

final class GZIPServletInputStream(val inputStream: ServletInputStream) extends ServletInputStream {

  val gzipStream = new GZIPInputStream(inputStream)


  override def read: Int = gzipStream.read

  override def read(b: Array[Byte]): Int = gzipStream.read(b)

  override def read(b: Array[Byte], off: Int, len: Int): Int = gzipStream.read(b, off, len)

  override def available: Int = gzipStream.available

  override def close(): Unit = gzipStream.close()

  override def isFinished: Boolean = gzipStream.available() == 0

  override def isReady: Boolean = true

  override def setReadListener(readListener: ReadListener): Unit = throw new UnsupportedOperationException

}
