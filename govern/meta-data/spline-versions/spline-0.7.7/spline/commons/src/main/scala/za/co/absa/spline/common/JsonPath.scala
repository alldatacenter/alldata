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

import za.co.absa.spline.common.JsonPath._

import java.{util => ju}

/**
 * A very simple implementation of a subset of JSON Path
 * supporting read/write accessors for Map[String. Any]
 *
 * Only explicit property and index tokens are supported at the moment, no predicates.
 */
class JsonPath(tokens: Seq[Token]) {

  def get[V <: Value](o: Target): V = {
    tokens.foldLeft(o) {
      case (xs: Seq[_], i: Int) => xs(i)
      case (xs: Array[_], i: Int) => xs(i)
      case (xs: ju.List[_], i: Int) => xs.get(i)

      case (m: Map[String, _], k: String) => m(k)
      case (m: ju.Map[String, _], k: String) => m.get(k)
    }.asInstanceOf[V]
  }

  def set[T <: Target](o: T, v: Value): T = {
    val (setter, _) = tokens.foldLeft[(Setter, Any)]((identity[Any], o)) {
      case ((prevSetter: Setter, xs: Seq[_]), i: Int) => prevSetter.compose(xs.updated(i, _: Value)) -> xs(i)
      case ((prevSetter: Setter, xs: Array[_]), i: Int) => prevSetter.compose(xs.updated(i, _: Value)) -> xs(i)
      case ((prevSetter: Setter, xs: ju.List[_]), i: Int) => prevSetter.compose((v2: Value) => {
        val xs2 = new ju.ArrayList[Any](xs)
        xs2.set(i, v2)
        xs2
      }) -> xs.get(i)

      case ((prevSetter: Setter, m: Map[String, Any]), k: String) => prevSetter.compose(m.updated(k, _: Value)) -> m(k)
      case ((prevSetter: Setter, m: ju.Map[String, Any]), k: String) => prevSetter.compose((v2: Value) => {
        val m2 = new ju.HashMap[String, Any](m)
        m2.put(k, v2)
        m2
      }) -> m.get(k)
    }

    setter(v).asInstanceOf[T]
  }
}

object JsonPath {

  // fixme: too many Any's. Need to do something with it (perhaps proper algebraic types in Scala 3 will solve it)
  private type Token = Any // String (property) or Int (index)
  private type Target = Any // Map[String, Any] or Seq[Any]
  private type Value = Any
  private type Setter = Value => Target

  /**
   * Only the bracket–notation is supported - $['a']['b'][42]['c'] - no dots, wildcards, unions, filters etc.
   *
   * @param path JSON path
   * @return JsonPath instance
   */
  def parse(path: String): JsonPath = {
    new JsonPath(path
      .stripPrefix("$")
      .stripPrefix("[")
      .stripSuffix("]")
      .split("]\\[")
      .map {
        case key if key startsWith "'" => key.substring(1, key.length - 1)
        case idx => idx.toInt
      })
  }


}
