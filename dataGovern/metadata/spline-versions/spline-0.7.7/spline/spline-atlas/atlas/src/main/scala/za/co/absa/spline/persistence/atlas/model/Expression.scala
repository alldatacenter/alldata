/*
 * Copyright 2017 ABSA Group Limited
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

package za.co.absa.spline.persistence.atlas.model

import java.util.UUID

import org.apache.atlas.AtlasClient
import org.apache.atlas.v1.model.instance.{Id, Referenceable}

import scala.collection.JavaConverters._

/**
  * The case case represents common properties for all expression types
  * @param qualifiedName An unique expression
  * @param text A textual representation of the expression
  * @param expressionType A type of the expression
  * @param dataType A data type associated with the expression
  * @param children A sequence of sub-expressions
  */
case class ExpressionCommonProperties
(
  qualifiedName : String,
  text: String,
  expressionType: String,
  dataType: Id,
  children: Seq[Expression]
)

/**
  * The class represents Spark expressions for which a dedicated expression node type hasn't been created yet.
  * @param commonProperties Common properties for all expression types
  * @param entityType An Atlas entity type name
  * @param childProperties Properties that are specific for inherited classes
  */
class Expression(
  val commonProperties: ExpressionCommonProperties,
  entityType: String = SparkDataTypes.Expression,
  childProperties : Map[String, AnyRef] = Map.empty
) extends Referenceable (
  entityType,
  new java.util.HashMap[String, Object]{
    put(AtlasClient.NAME, commonProperties.text)
    put(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, commonProperties.qualifiedName)
    put("text", commonProperties.text)
    put("expressionType", commonProperties.expressionType)
    put("dataType", commonProperties.dataType)
    put("children", commonProperties.children.asJava)
    childProperties.foreach(i => put(i._1, i._2))
  }
)

/**
  * The class represents renaming of an underlying expression to a specific alias.
  * @param commonProperties Common properties for all expression types
  * @param alias A final name of the expression
  */
class AliasExpression
(
  commonProperties: ExpressionCommonProperties,
  alias : String
) extends Expression(
  commonProperties,
  SparkDataTypes.AliasExpression,
  Map("alias" -> alias)
)

/**
  * The class represents binary operators like addition, multiplication, string concatenation, etc.
  * @param commonProperties Common properties for all expression types
  * @param symbol A symbol expressing the operation (+, -, *, /, etc. )
  */
class BinaryExpression
(
  commonProperties: ExpressionCommonProperties,
  symbol : String
) extends Expression(
  commonProperties,
  SparkDataTypes.BinaryExpression,
  Map("symbol" -> symbol)
)

/**
  * The class represents a special expression for referencing an attribute from a data set.
  * @param commonProperties Common properties for all expression types
  * @param attributeId An unique of a referenced attribute
  * @param attributeName A name of a referenced attribute
  */
class AttributeReferenceExpression
(
  commonProperties: ExpressionCommonProperties,
  attributeId: UUID,
  attributeName: String
) extends Expression(
  commonProperties,
  SparkDataTypes.AttributeReferenceExpression,
  Map("attributeId" -> attributeId.toString, "attributeName" -> attributeName)
)

/**
  * The class represents a special expression describing an user-defined function of Spark.
  * @param commonProperties Common properties for all expression types
  * @param functionName A name assigned to an user-defined function
  */
class UserDefinedFunctionExpression
(
  commonProperties: ExpressionCommonProperties,
  functionName: String
) extends Expression(
  commonProperties,
  SparkDataTypes.UDFExpression,
  Map("functionName" -> functionName)
)
