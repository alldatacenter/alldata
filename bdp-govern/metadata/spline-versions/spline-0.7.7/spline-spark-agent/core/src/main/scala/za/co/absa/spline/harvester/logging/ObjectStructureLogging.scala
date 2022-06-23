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

package za.co.absa.spline.harvester.logging

import org.apache.spark.internal.Logging

import scala.util.control.NonFatal

trait ObjectStructureLogging {
  self: Logging =>

  protected def logObjectStructureAsTrace(obj: => AnyRef): Unit =
    if (log.isTraceEnabled) logObjectStructure(obj, log.trace, log.trace)

  protected def logObjectStructureAsWarn(obj: => AnyRef): Unit =
    if (log.isWarnEnabled) logObjectStructure(obj, log.warn, log.warn)

  protected def logObjectStructureAsError(obj: => AnyRef): Unit =
    if (log.isErrorEnabled) logObjectStructure(obj, log.error, log.error)

  private def logObjectStructure(
    obj: AnyRef,
    logFunction: String => Unit,
    logFunctionThrowable: (String, Throwable) => Unit
  ): Unit = {
    try {
      val structureString = ObjectStructureDumper.dump(obj)
      logFunction(structureString)
    } catch {
      case e @ (_: LinkageError | NonFatal(_)) =>
        logFunctionThrowable(s"Attempt to dump structure of ${obj.getClass} failed", e)
    }
  }
}
