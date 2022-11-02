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

package za.co.absa.spline.common.validation

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class UniqueConstraintSpec extends AnyFlatSpec with Matchers {

  behavior of "isValid"

  it should "return `true` on empty collections" in {
    Constraint.unique(Seq.empty).isValid shouldBe true
  }

  it should "return `true` when no duplicates found" in {
    Constraint.unique(Seq(1, 2, 3)).isValid shouldBe true
    Constraint.unique(Seq("a", "b", "c")).isValid shouldBe true
  }

  it should "return `false` when there are duplicates" in {
    Constraint.unique(Seq(1, 2, 1)).isValid shouldBe false
    Constraint.unique(Seq("a", "b", "a")).isValid shouldBe false
  }

  behavior of "message"

  it should "print N first duplicates" in {
    Constraint.unique(Seq(1, 2, 3, 4, 1)).message shouldEqual "Duplicated items: 1"
    Constraint.unique(Seq(1, 2, 3, 4, 1, 2)).message shouldEqual "Duplicated items: 1, 2"
    Constraint.unique(Seq(1, 2, 3, 4, 1, 2, 3)).message shouldEqual "Duplicated items: 1, 2, 3"
    Constraint.unique(Seq(1, 2, 3, 4, 1, 2, 3, 4)).message shouldEqual "Duplicated items: 1, 2, 3 and 1 others"
  }

  behavior of "by()"

  it should "create a new constraint instance with the given projection" in {
    Constraint.unique(Seq("a1", "b1")).by(_ (0)).isValid shouldBe true
    Constraint.unique(Seq("a1", "b1")).by(_ (1)).isValid shouldBe false
    Constraint.unique(Seq("a1", "b1")).by(_ (1)).message shouldBe "Duplicated items: 1"
  }

  behavior of "as()"

  it should "create a new constraint instance with the given item name" in {
    Constraint.unique(Seq(1, 1)).as("number").message shouldEqual "Duplicated numbers: 1"
  }
}
