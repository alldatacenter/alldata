/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.griffin.measure.utils

import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should._

import org.apache.griffin.measure.utils.ParamUtil._

class ParamUtilTest extends AnyFlatSpec with Matchers with BeforeAndAfter {

  val fruits: Map[String, Any] =
    Map[String, Any]("A" -> "apple", "B" -> "banana", "O" -> "orange")
  val numbers: Map[String, Any] = Map[String, Any]("a" -> 1, "b" -> 5, "c" -> 3)
  val ids: Seq[Any] = Seq[Any](2, 3, 5, 7)
  val cities: Seq[Any] = Seq[Any]("LA", "NY", "SLC")
  val percentiles: Seq[Any] = Seq[Any](.95, "0.4", ".3", 1, "static", "0.2")
  var params: Map[String, Any] = _

  before {
    params = Map[String, Any](
      "name" -> "alex",
      "age" -> 34,
      "fruits" -> fruits,
      "numbers" -> numbers,
      "ids" -> ids,
      "cities" -> cities,
      "percentiles" -> percentiles)
  }

  "TransUtil" should "transform all basic data types" in {
    import ParamUtil.TransUtil._
    toAny("test") should be(Some("test"))
    toAnyRef[Seq[_]]("test") should be(None)
    toAnyRef[Seq[_]](Seq(1, 2)) should be(Some(Seq(1, 2)))
    toStringOpt("test") should be(Some("test"))
    toStringOpt(123) should be(Some("123"))
    toByte(12) should be(Some(12))
    toByte(123456) should not be Some(123456)
    toShort(12) should be(Some(12))
    toShort(123456) should not be Some(123456)
    toInt(12) should be(Some(12))
    toInt(1.8) should be(Some(1))
    toInt("123456") should be(Some(123456))
    toLong(123456) should be(Some(123456L))
    toFloat(1.2) should be(Some(1.2f))
    toDouble("1.21") should be(Some(1.21))
    toBoolean(true) should be(Some(true))
    toBoolean("false") should be(Some(false))
    toBoolean("test") should be(None)
  }

  "params" should "extract string any map field" in {
    params.getParamAnyMap("fruits") should be(fruits)
    params.getParamAnyMap("numbers") should be(numbers)
    params.getParamAnyMap("name") should be(Map.empty[String, Any])
  }

  "params" should "extract string string map field" in {
    params.getParamStringMap("fruits") should be(fruits)
    params.getParamStringMap("numbers") should be(
      Map[String, String]("a" -> "1", "b" -> "5", "c" -> "3"))
    params.getParamStringMap("name") should be(Map.empty[String, String])
  }

  "params" should "extract array field" in {
    params.getStringArr("ids") should be(Seq("2", "3", "5", "7"))
    params.getStringArr("cities") should be(cities)
    params.getStringArr("name") should be(Nil)
  }

  "params" should "get double array" in {
    params.getDoubleArr("percentiles") should be(Seq(0.95, 0.4, 0.3, 1, 0.2))
  }

  "map" should "add if not exist" in {
    val map = Map[String, Any]("a" -> 1, "b" -> 2)
    map.addIfNotExist("a", 11) should be(Map[String, Any]("a" -> 1, "b" -> 2))
    map.addIfNotExist("c", 11) should be(Map[String, Any]("a" -> 1, "b" -> 2, "c" -> 11))
  }

  "map" should "remove keys" in {
    val map = Map[String, Any]("a" -> 1, "b" -> 2)
    map.removeKeys(Seq("c", "d")) should be(Map[String, Any]("a" -> 1, "b" -> 2))
    map.removeKeys(Seq("a")) should be(Map[String, Any]("b" -> 2))
  }

}
