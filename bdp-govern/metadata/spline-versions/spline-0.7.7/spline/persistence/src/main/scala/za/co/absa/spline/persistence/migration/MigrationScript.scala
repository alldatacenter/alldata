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

package za.co.absa.spline.persistence.migration

import za.co.absa.commons.version.impl.SemVer20Impl.SemanticVersion

import scala.util.matching.Regex

case class MigrationScript(verFrom: SemanticVersion, verTo: SemanticVersion, script: String) {
  override def toString: String = MigrationScript.asString(this)
}

object MigrationScript {
  private val SemVerRegexp: Regex = ("" +
    "(?:0|[1-9]\\d*)\\." +
    "(?:0|[1-9]\\d*)\\." +
    "(?:0|[1-9]\\d*)" +
    "(?:-(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*)?" +
    "(?:\\+[0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*)?").r

  private def asString(script: MigrationScript): String =
    FileNamePattern
      .replaceFirst("\\*", script.verFrom.asString)
      .replaceFirst("\\*", script.verTo.asString)

  val FileNamePattern = "*-*.js"

  val NameRegexp: Regex = s"($SemVerRegexp)-($SemVerRegexp).js".r
}
