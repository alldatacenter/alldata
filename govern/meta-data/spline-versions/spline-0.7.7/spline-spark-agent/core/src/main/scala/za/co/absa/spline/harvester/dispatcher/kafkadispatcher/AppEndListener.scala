/*
 * Copyright 2022 ABSA Group Limited
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

package za.co.absa.spline.harvester.dispatcher.kafkadispatcher

import org.apache.spark.internal.Logging
import org.apache.spark.scheduler.{SparkListener, SparkListenerApplicationEnd}

import scala.util.control.NonFatal

class AppEndListener(body: () => Unit) extends SparkListener with Logging {

  override def onApplicationEnd(applicationEnd: SparkListenerApplicationEnd): Unit = {
    try body()
    catch {
      case e if NonFatal(e) => logWarning("Exception when executing app end hook: ", e)
    }
  }
}
