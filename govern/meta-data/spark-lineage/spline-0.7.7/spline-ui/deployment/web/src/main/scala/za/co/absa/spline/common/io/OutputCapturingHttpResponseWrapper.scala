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

package za.co.absa.spline.common.io

import java.io.{ByteArrayOutputStream, OutputStreamWriter, PrintWriter}
import java.nio.charset.StandardCharsets.UTF_8

import javax.servlet.ServletOutputStream
import javax.servlet.http.{HttpServletResponse, HttpServletResponseWrapper}


class OutputCapturingHttpResponseWrapper(val response: HttpServletResponse) extends HttpServletResponseWrapper(response) {

  private val baos = new ByteArrayOutputStream

  override def getWriter: PrintWriter = new PrintWriter(new OutputStreamWriter(baos, UTF_8))

  override def getOutputStream: ServletOutputStream = new ChainingServletOutputStream(baos)

  override def setContentLength(len: Int): Unit = {}

  override def setContentLengthLong(len: Long): Unit = {}

  def getContentAsString: String = baos.toString("UTF-8")
}

