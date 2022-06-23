/*
 * Copyright 2019 ABSA Group Limited
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

package za.co.absa.spline.harvester.converter

import za.co.absa.commons.lang.OptionImplicits._
import za.co.absa.commons.reflect.ReflectionUtils.ModuleClassSymbolExtractor

sealed trait ValueDecomposer {
  outer =>

  type In = Any
  type Arg = Any
  type Out = Any
  type Call = (In, Arg) => Option[Out]
  type Handler = Call => PartialFunction[(In, Arg), Option[Out]]

  protected def handler: Handler

  def decompose: Call = recursion

  def addHandler(h0: Handler): ValueDecomposer = new ValueDecomposer {
    override val handler: Handler = r => h0(r) orElse outer.handler(r)
  }

  private def recursion: Call = handler(recursion)(_, _)
}

object ValueDecomposer extends ValueDecomposer {

  override protected val handler: Handler = recursion => {
    case (null, _) => None
    case (i: Number, _) => Some(i)
    case (b: Boolean, _) => Some(b)
    case (s: String, _) => Some(s)
    case (opt: Option[_], arg) => opt.flatMap(recursion(_, arg))
    case (map: Map[_, _], arg) => (for ((k, v) <- map; r <- recursion(v, arg)) yield k.toString -> r).asOption
    case (seq: Traversable[_], arg) => seq.map(item => recursion(item, arg).orNull).toVector.asOption
    case (ModuleClassSymbolExtractor(symbol), _) => Some(symbol.name.toString)
    case (x: Any, _) => Some(x.toString)
  }
}
