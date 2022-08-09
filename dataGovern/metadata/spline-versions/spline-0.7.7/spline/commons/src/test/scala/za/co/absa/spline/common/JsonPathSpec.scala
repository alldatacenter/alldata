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

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.{util => ju}

class JsonPathSpec extends AnyFlatSpec with Matchers {

  behavior of "JsonPath"

  private val testObj = Map(
    "a" -> Map(
      "b" -> Seq(
        1,
        Map(
          "c" -> "TO BE REPLACED",
          "c2" -> "vc2"),
        3),
      "b2" -> "vb2"),
    "a2" -> "va2")

  it should "get the value" in {
    JsonPath.parse("$['a']['b'][0]").get[Int](testObj) should equal(1)
    JsonPath.parse("$['a']['b'][1]['c2']").get[String](testObj) should equal("vc2")
  }

  it should "set the value" in {
    (JsonPath.parse("$['a']['b'][1]['c']").set(testObj, 42)
      should equal(
      Map(
        "a" -> Map(
          "b" -> Seq(
            1,
            Map(
              "c" -> 42,
              "c2" -> "vc2"),
            3),
          "b2" -> "vb2"),
        "a2" -> "va2")))

    (JsonPath.parse("$['a']['b'][1]").set(testObj, 42)
      should equal(
      Map(
        "a" -> Map(
          "b" -> Seq(
            1,
            42,
            3),
          "b2" -> "vb2"),
        "a2" -> "va2")))
  }

  it should "support Array" in {
    val arr = Array(1, 2)
    JsonPath.parse("[0]").get[Int](arr) should equal(1)
    JsonPath.parse("[0]").set(arr, 42) should equal(Array(42, 2))
  }

  it should "support java.util.List" in {
    val juList = ju.Arrays.asList(1, 2)
    JsonPath.parse("[0]").get[Int](juList) should equal(1)
    JsonPath.parse("[0]").set(juList, 42) should equal(ju.Arrays.asList(42, 2))
  }

  it should "support java.util.Map" in {
    val juMap = new ju.HashMap[String, Int] {
      put("a", 1)
      put("b", 2)
    }
    JsonPath.parse("['a']").get[Int](juMap) should equal(1)
    JsonPath.parse("['a']").set(juMap, 42) should equal(new ju.HashMap[String, Int] {
      put("a", 42)
      put("b", 2)
    })
  }
}
