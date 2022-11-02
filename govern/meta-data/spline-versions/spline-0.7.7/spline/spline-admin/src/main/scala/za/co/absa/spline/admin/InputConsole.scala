/*
 * Copyright 2021 ABSA Group Limited
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

package za.co.absa.spline.admin

import java.io.Console

trait InputConsole {
  def readLine(msg: String): String
  def readPassword(msg: String): String
}

object InputConsole {

  def systemConsoleIfAvailable(): Option[InputConsole] = Option(System.console()).map(new SystemConsole(_))

  class SystemConsole(console: Console) extends InputConsole {

    override def readLine(msg: String): String = console.readLine(msg)

    override def readPassword(msg: String): String = new String(console.readPassword(msg))
  }
}
