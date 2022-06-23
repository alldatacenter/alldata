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

package za.co.absa.commons

import scala.util.matching.Regex

class CaptureGroupReplacer(replacement: String) {

  final def replace(str: String, regexes: Seq[Regex]): String = {
    regexes.foldLeft(str) {
      case (prevStr, regex) =>
        val matches = regex.findAllMatchIn(prevStr).toSeq
        if (matches.isEmpty) prevStr
        else replaceMatchedGroups(matches, prevStr)
    }
  }

  private def replaceMatchedGroups(regMatches: Seq[Regex.Match], str: String): String = {
    val groupsStartsAndEnds =
      for {
        regMatch <- regMatches.toArray
        i <- 1 to regMatch.groupCount
      } yield
        (regMatch.start(i), regMatch.`end`(i))

    val start = (0, 0)
    val end = (str.length, str.length)

    val intervals = (start +: groupsStartsAndEnds :+ end)
      .sliding(2, 1)
      .map { case Array((_, firstEnd), (secondStart, _)) => (firstEnd, secondStart) }

    intervals
      .map { case (from, to) => str.substring(from, to) }
      .mkString(replacement)
  }
}
