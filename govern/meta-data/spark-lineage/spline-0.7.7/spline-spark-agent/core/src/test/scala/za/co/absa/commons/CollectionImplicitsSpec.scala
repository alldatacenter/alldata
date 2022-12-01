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
import za.co.absa.commons.CollectionImplicits._

class CollectionImplicitsSpec
  extends AnyFlatSpec
    with Matchers {

  behavior of "MapOps.|+|"

  it should "merge two maps with append" in {
    val m1 = Map("a" -> 1, "b" -> 1)
    val m2 = Map("b" -> 1, "c" -> 1)
    (m1 |+| m2) should equal(Map("a" -> 1, "b" -> 2, "c" -> 1))
  }

  it should "ignore empty maps" in {
    (Map.empty[String, Int] |+| Map.empty[String, Int]) should be(empty)
    (Map.empty[String, Int] |+| Map("a" -> 1)) should equal(Map("a" -> 1))
    (Map("a" -> 1) |+| Map.empty[String, Int]) should equal(Map("a" -> 1))
  }

}
