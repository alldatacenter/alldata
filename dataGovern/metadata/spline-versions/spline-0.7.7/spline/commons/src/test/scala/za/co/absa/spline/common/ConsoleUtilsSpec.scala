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

import org.backuity.ansi.AnsiFormatter.FormattedHelper
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ConsoleUtilsSpec extends AnyFlatSpec with Matchers {

  behavior of "ConsoleUtils"

  behavior of "stripAnsiEscapeSequences()"

  import za.co.absa.spline.common.ConsoleUtils.stripAnsiEscapeSequences

  it should "de-ANSI-ify the given string" in {
    stripAnsiEscapeSequences(ansi"%green{green %bold{bold}} plain %red{red}") should equal("green bold plain red")
  }

  it should "remove any ANSI ESC sequences" in {
    stripAnsiEscapeSequences("\u001b[s\u001b[0;38;5;232;48;5;255mHello\u001b[u \u001b[2;37;41mWorld") should equal("Hello World")
  }
}
