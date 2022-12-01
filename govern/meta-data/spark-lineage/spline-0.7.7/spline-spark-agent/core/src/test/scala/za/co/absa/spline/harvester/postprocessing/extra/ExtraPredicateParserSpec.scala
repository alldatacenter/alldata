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

package za.co.absa.spline.harvester.postprocessing.extra

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import za.co.absa.spline.harvester.postprocessing.metadata._

private class ExtraPredicateParserSpec  extends AnyFlatSpec with Matchers {

  behavior of "ExtraSelectorParser"

  it should "parse selector without predicate" in {
    val (name, predicate) = PredicateParser.parse("executionPlan")

    name shouldBe "executionPlan"
    predicate shouldBe Predicate(Bool(true))
  }

  it should "parse boolean predicate" in {
    val (name, predicate) = PredicateParser.parse("executionPlan[true]")

    name shouldBe "executionPlan"
    predicate shouldBe Predicate(Bool(true))
  }

  it should "parse path predicate" in {
    val (name, predicate) = PredicateParser.parse("executionPlan[@.extraInfo.isAllRight]")

    name shouldBe "executionPlan"
    predicate shouldBe Predicate(Path(PropertyPS("@"), PropertyPS("extraInfo"), PropertyPS("isAllRight")))
  }

  it should "parse path predicate with eq" in {
    val (name, predicate) = PredicateParser.parse("executionPlan[@.extraInfo.isAllRight == false]")

    name shouldBe "executionPlan"
    predicate shouldBe Predicate(
      Eq(
        Path(PropertyPS("@"), PropertyPS("extraInfo"), PropertyPS("isAllRight")),
        Bool(false)
      ))
  }

  it should "parse long" in {
    val (name, predicate) = PredicateParser.parse("executionPlan[x == 66L]")

    predicate shouldBe Predicate(
      Eq(
        Path(PropertyPS("x")),
        LongNum(66L)
      )
    )
  }

  it should "parse text" in {
    val (name, predicate) = PredicateParser.parse("executionPlan[x == 'abc']")

    predicate shouldBe Predicate(
      Eq(
        Path(PropertyPS("x")),
        Text("abc")
      )
    )
  }

  it should "parse nested boolean exprs" in {
    val (name, predicate) = PredicateParser.parse("executionPlan[!(true && (false || !false))]")

    predicate shouldBe Predicate(
      Not(
        And(
          Bool(true),
          Or(
            Bool(false),
            Not(Bool(false))
          )
        )
      )
    )
  }

  it should "parse path to sequence" in {
    val (name, predicate) = PredicateParser.parse("executionPlan[foo.bar[32] < 42]")

    predicate shouldBe Predicate(
      Comparison(Path(PropertyPS("foo") , PropertyPS("bar"), ArrayPS(32)), IntNum(42), "<")
    )
  }

  it should "parse path to map" in {
    val (name, predicate) = PredicateParser.parse("executionPlan[foo.map['abc'] != 42]")

    predicate shouldBe Predicate(
      Neq(Path(PropertyPS("foo") , PropertyPS("map"), MapPS("abc")), IntNum(42))
    )
  }

  it should "parse multiple paths" in {
    val (name, predicate) = PredicateParser.parse("executionPlan[(foo.bar.x < 42) && (x.y.z > 5)]")

    predicate shouldBe Predicate(
      And(
        Comparison(Path(PropertyPS("foo") , PropertyPS("bar"), PropertyPS("x")), IntNum(42), "<"),
        Comparison(Path(PropertyPS("x"), PropertyPS("y"), PropertyPS("z")), IntNum(5), ">")
      )
    )
  }

  it should "parse using proper precedence" in {
    val (name, predicate) = PredicateParser.parse("executionPlan[x <= 42 && y >= 5]")

    predicate shouldBe Predicate(
      And(
        Comparison(Path(PropertyPS("x")), IntNum(42), "<="),
        Comparison(Path(PropertyPS("y")), IntNum(5), ">=")
      )
    )
  }

}
