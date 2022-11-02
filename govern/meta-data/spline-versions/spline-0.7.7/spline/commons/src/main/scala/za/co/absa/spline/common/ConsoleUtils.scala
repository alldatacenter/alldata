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

package za.co.absa.spline.common

import org.backuity.ansi.AnsiFormatter

import scala.language.experimental.macros
import scala.reflect.macros.blackbox

/**
 * See: https://github.com/backuity/ansi-interpolator/pull/2
 * TODO: This class can be removed when the above ansi-interpolator issue is resolved
 */
object ConsoleUtils {

  implicit class AnsiInterpolator(val sc: StringContext) extends AnyVal {
    //noinspection ScalaUnusedSymbol
    def ansi(args: Any*): String = macro ansiImpl
  }

  def ansiImpl(c: blackbox.Context)(args: c.Tree*): c.universe.Tree = {
    import c.universe._

    val ansiInterpolation = AnsiFormatter.ansiImpl(c)(args: _*)

    q"""
      val ansiAllowed = System.console != null && System.getenv.get("TERM") != null
      val ansiString  = $ansiInterpolation

      if (ansiAllowed) ansiString
      else stripAnsiEscapeSequences(ansiString)
    """
  }

  def stripAnsiEscapeSequences(s: String): String = {
    val csiSequencePattern = "\u001b\\[.*?[\\x40-\\x7E]"
    s.replaceAll(csiSequencePattern, "")
  }

}
