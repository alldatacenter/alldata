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

import org.apache.atlas.AtlasClient
import org.apache.atlas.v1.model.instance.{Id, Referenceable}

import scala.collection.JavaConverters._

/**
  * The case class represents operation properties that are common for all operation types.
  * @param name A name
  * @param qualifiedName An unique identifier
  * @param inputs A sequence of input dataset ids
  * @param outputs A sequence of output dataset ids
  */
case class OperationCommonProperties
(
  name : String,
  qualifiedName: String,
  inputs: Seq[Id],
  outputs: Seq[Id]
)

/**
  * The class represents a base for operation hierarchy
  * @param commonProperties Common properties of all operation types
  * @param operationType An Atlas entity type name
  * @param childProperties Properties that are specific for derived classes.
  */
class Operation(
  commonProperties: OperationCommonProperties,
  operationType: String = SparkDataTypes.Operation,
  childProperties : Map[String, Object] = Map.empty
) extends Referenceable(
  operationType,
  new java.util.HashMap[String, Object]() {
    put(AtlasClient.NAME, commonProperties.name)
    put(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, commonProperties.qualifiedName)
    put("inputs", commonProperties.inputs.asJava)
    put("outputs", commonProperties.outputs.asJava)
    childProperties.foreach(i => put(i._1, i._2))
  }
)

/**
  * The class represents an arbitrary Spark operation that doesn't have corresponding Spline operation.
  * @param commonProperties Common properties of all operation types
  * @param rawString A string representing the Spline operation
  */
class GenericOperation(
  commonProperties: OperationCommonProperties,
  rawString: String
) extends Operation(
  commonProperties,
  SparkDataTypes.GenericOperation,
  Map("rawString" -> rawString)
)

/**
  * The class represents Spark join operation.
  * @param commonProperties Common properties of all operation types
  * @param joinType A string description of a join type ("inner", "left_outer", right_outer", "outer")
  * @param condition An expression deciding how two data sets will be join together
  */
class JoinOperation(
  commonProperties: OperationCommonProperties,
  joinType: String,
  condition : Option[Expression]
) extends Operation(
  commonProperties,
  SparkDataTypes.JoinOperation,
  {
    val extraParameters = Map("joinType" -> joinType)
    condition match {
      case Some(c) =>  extraParameters + ("condition" -> c)
      case None => extraParameters
    }
  }
)

/**
  * The class represents Spark filter (where) operation.
  * @param commonProperties Common properties of all operation types
  * @param condition An expression deciding what records will survive filtering
  */
class FilterOperation(
  commonProperties: OperationCommonProperties,
  condition : Expression
) extends Operation(
  commonProperties,
  SparkDataTypes.FilterOperation,
  Map("condition" -> condition)
)

/**
  * The class represents Spark projective operations (select, drop, withColumn, etc.)
  * @param commonProperties Common properties of all operation types
  * @param transformations Sequence of expressions defining how input set of attributes will be affected by the projection.
  *                        (Introduction of a new attribute, Removal of an unnecessary attribute)
  */
class ProjectOperation(
  commonProperties: OperationCommonProperties,
  transformations : Seq[Expression]
) extends Operation(
  commonProperties,
  SparkDataTypes.ProjectOperation,
  Map("transformations" -> transformations.asJava)
)

/**
  * The class represents Spark alias (as) operation for assigning a label to data set.
  * @param commonProperties Common properties of all operation types
  * @param alias An assigned label
  */
class AliasOperation(
  commonProperties: OperationCommonProperties,
  alias : String
) extends Operation(
  commonProperties,
  SparkDataTypes.AliasOperation,
  Map("alias" -> alias)
)

/**
  * The class represents a sort order expression and its direction
  *
  * @param qualifiedName An unique identifier
  * @param expression An expression that returns values to sort on
  * @param direction Sorting direction
  * @param nullOrder Ordering for null values
  */
class SortOrder(
  qualifiedName: String,
  expression: Expression,
  direction: String,
  nullOrder: String
) extends Referenceable(
  SparkDataTypes.SortOrder,
  new java.util.HashMap[String, Object]() {
    put(AtlasClient.NAME, expression.commonProperties.text)
    put(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, qualifiedName)
    put("expression", expression)
    put("direction", direction)
    put("nullOrder", nullOrder)
  }
)

/**
  * The class represents Spark sort operation.
  *
  * @param commonProps Common node properties
  * @param orders Sort orders
  */
class SortOperation(
  commonProperties: OperationCommonProperties,
  orders: Seq[SortOrder]
) extends Operation(
  commonProperties,
  SparkDataTypes.SortOperation,
  Map("orders" -> orders.asJava)
)

/**
  * The class represents Spark aggregate operation.
  * @param commonProperties Common node properties
  * @param groupings Grouping expressions
  * @param aggregations Aggregation expressions
  */
class AggregateOperation(
  commonProperties: OperationCommonProperties,
  groupings: Seq[Expression],
  aggregations: Seq[Expression]
) extends Operation(
  commonProperties,
  SparkDataTypes.AggregateOperation,
  Map("groupings" -> groupings.asJava, "aggregations" -> aggregations.asJava)
)

/**
  * The class represents Spark operations for persisting data sets to HDFS, Hive etc. Operations are usually performed via DataFrameWriters.
  *
  * @param commonProperties Common node properties
  * @param append `true` for "APPEND" write mode, `false` otherwise.
  */
class WriteOperation(
  commonProperties: OperationCommonProperties,
  append: Boolean
) extends Operation(
  commonProperties,
  SparkDataTypes.WriteOperation,
  Map("appendMode" -> Boolean.box(append))
)