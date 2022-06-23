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

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class CaptureGroupReplacerSpec extends AnyFlatSpec with Matchers {

  val replacer = new CaptureGroupReplacer("<replaced>")

  it should "not modify the input when there is no match" in {
    val input = "some string"
    replacer.replace(input, Seq("""(\d+)""".r)) should be theSameInstanceAs input
  }

  it should "replace the capturing groups" in {
    replacer
      .replace("foo@bar.cz", Seq("""[^@]+@(\w+).\w+""".r))
      .shouldBe("foo@<replaced>.cz")

    replacer
      .replace("ftp://username:pumba@hostname/", Seq("""ftp://[^:]+:([^@]+)@""".r)) //NOSONAR
      .shouldBe("ftp://username:<replaced>@hostname/")
  }

  it should "replace empty groups" in {
    replacer
      .replace("foo:@bar.cz", Seq(""":(\d*)@""".r))
      .shouldBe("foo:<replaced>@bar.cz")
  }

  it should "replace all occurrences" in {
    replacer
      .replace("some 42 string 66!", Seq("""[^s](\d+)""".r))
      .shouldBe("some <replaced> string <replaced>!")
  }

  it should "replace multiple capturing groups" in {
    replacer
      .replace("something@pumba:34:timon/", Seq("""@([^:]+):[\d]+:([^\s/]+)""".r))
      .shouldBe("something@<replaced>:34:<replaced>/")
  }

  it should "replace groups from multiple regexes" in {
    replacer
      .replace("something&foo=42&pumba=lala&bar=66&timon=33", Seq("""pumba=([^&\s]+)""".r, """timon=([^&\s]+)""".r))
      .shouldBe("something&foo=42&pumba=<replaced>&bar=66&timon=<replaced>")
  }
}

