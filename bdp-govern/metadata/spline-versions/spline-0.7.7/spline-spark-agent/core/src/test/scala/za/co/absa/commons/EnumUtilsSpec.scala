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

package za.co.absa.commons

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import za.co.absa.commons.EnumUtils.EnumOps
import za.co.absa.commons.EnumUtilsSpec.Fruit
import za.co.absa.commons.EnumUtilsSpec.Fruit.{Apple, Kiwi, Orange}

class EnumUtilsSpec
  extends AnyFlatSpec
    with Matchers {

  behavior of "EnumOps"

  behavior of "valueOf"

  it should "return proper fruit" in {
    Fruit.valueOf("apple") should be theSameInstanceAs Apple
    Fruit.valueOf("kiwi") should be theSameInstanceAs Kiwi
    Fruit.valueOf("orange") should be theSameInstanceAs Orange
  }

  it should "return Kiwi regardless of the case of the letters" in {
    Fruit.valueOf("kiwi") should be theSameInstanceAs Kiwi
    Fruit.valueOf("KIWI") should be theSameInstanceAs Kiwi
    Fruit.valueOf("KiWi") should be theSameInstanceAs Kiwi
  }
  it should "throw exceptions on non-matching fruit names" in {
    the[IllegalArgumentException] thrownBy Fruit.valueOf("boo") should have message
      "String 'boo' does not match any of the Fruit enum values (Apple, Kiwi, Orange)"
  }

}

object EnumUtilsSpec {

  sealed trait Fruit

  object Fruit extends EnumOps[Fruit] {

    object Apple extends Fruit

    object Kiwi extends Fruit

    object Orange extends Fruit

    def values: Seq[Fruit] = Array(Apple, Kiwi, Orange)
  }

}
