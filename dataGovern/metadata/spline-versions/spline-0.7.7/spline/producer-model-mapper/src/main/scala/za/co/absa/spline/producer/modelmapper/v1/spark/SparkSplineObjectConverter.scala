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

package za.co.absa.spline.producer.modelmapper.v1.spark

import za.co.absa.spline.producer.model.v1_1.AttrOrExprRef.exprRef
import za.co.absa.spline.producer.modelmapper.v1.{ExpressionConverter, ObjectConverter, TypesV1}

class SparkSplineObjectConverter(
  attrRefConverter: AttributeRefConverter,
  exprConverter: ExpressionConverter,
) extends ObjectConverter {

  override def convert(obj: Any): Any = obj match {
    case attrDef: TypesV1.AttrDef if attrRefConverter.isAttrRef(attrDef) => attrRefConverter.convert(attrDef)
    case exprDef: TypesV1.ExprDef if exprConverter.isExpression(exprDef) => exprRef(exprConverter.convert(exprDef).id)
    case arr: Seq[_] => arr.map(this.convert)
    case m: Map[_, _] => m.mapValues(this.convert).view.force // see: https://github.com/scala/bug/issues/4776
    case _ => obj
  }
}
