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

package za.co.absa.spline.producer.model.v1_1

/**
 * A wrapper class that holds either Attribute ID or Expression ID in one of the corresponding optional properties
 * (the other property is respectively `None`).
 *
 * The class is designed to support (de-)serialization as well as data structures where
 * Expression ID and Attribute ID are mixed with other potentially arbitrary data type, so that
 * from the context of that data structure it's hard to tell if a given value represents one of
 * those references (and which one if so) or any other value with different semantics.
 *
 * It can be seen as an alternative to a `_typeHint` property, but instead of a separate discriminator property
 * holding an enum value, here the value property name serves as a discriminator.
 *
 * The assumption is that there is no object with a single property named as `__attrId` or `__exprId`
 * that is not a representation of this class.
 *
 * E.g. JSON object `{ __attrId: 42 }` represents a reference to an attribute with ID 42, while
 * `{ _attrId: 42 }` or `{ __attrId: 42, foo: 1 }` represent arbitrary key-value pairs.
 *
 * @param __attrId attribute ID
 * @param __exprId expression ID
 */

case class AttrOrExprRef(
  __attrId: Option[Attribute.Id],
  __exprId: Option[ExpressionLike.Id]) {

  require(
    __attrId.isDefined ^ __exprId.isDefined,
    s"Either `__attrId` or `__exprId` should be defined. Was: ${__attrId}, ${__exprId}")
}

object AttrOrExprRef {

  implicit class AttrOrExprOps(val ref: AttrOrExprRef) extends AnyVal {
    def refId: ExpressionLike.Id = (ref.__attrId orElse ref.__exprId).get

    def isAttribute: Boolean = ref.__attrId.isDefined

    def isExpression: Boolean = ref.__exprId.isDefined
  }

  def attrRef(attrId: Attribute.Id): AttrOrExprRef = AttrOrExprRef(Option(attrId), None)

  def exprRef(exprId: ExpressionLike.Id): AttrOrExprRef = AttrOrExprRef(None, Option(exprId))

  def fromMap(obj: Map[String, Any]): Option[AttrOrExprRef] = {
    if (obj.size != 1) None
    else obj.head match {
      case ("__attrId", attrId: Attribute.Id) => Some(attrRef(attrId))
      case ("__exprId", exprId: ExpressionLike.Id) => Some(exprRef(exprId))
      case _ => None
    }
  }
}
