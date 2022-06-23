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

package za.co.absa.spline.harvester.dispatcher

import org.apache.commons.configuration.Configuration
import za.co.absa.commons.EnumUtils.EnumOps
import za.co.absa.commons.config.ConfigurationImplicits._
import za.co.absa.commons.reflect.EnumerationMacros
import za.co.absa.spline.harvester.dispatcher.ConsoleLineageDispatcher._

import java.io.PrintStream

class ConsoleLineageDispatcher(stream: => PrintStream)
  extends AbstractJsonLineageDispatcher {

  def this(conf: Configuration) = this(
    stream = StdStream.valueOf(conf.getRequiredString(StreamKey)).stream()
  )

  override def name = "Console"

  override protected def send(json: String): Unit = stream.println(json)
}

object ConsoleLineageDispatcher {
  private val StreamKey = "stream"

  sealed abstract class StdStream(val stream: () => PrintStream)

  object StdStream extends EnumOps[StdStream] {

    object Out extends StdStream(() => Console.out)

    object Err extends StdStream(() => Console.err)

    protected val values: Seq[StdStream] = EnumerationMacros.sealedInstancesOf[StdStream].toSeq
  }

}
