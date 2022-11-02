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

/**
  * The object contains names of Atlas entities.
  */
object SparkDataTypes{
  val Job : String = "spark_job"
  val Operation : String = "spark_operation"
  val GenericOperation : String = "spark_generic_operation"
  val JoinOperation : String = "spark_join_operation"
  val FilterOperation : String = "spark_filter_operation"
  val ProjectOperation : String = "spark_project_operation"
  val AliasOperation : String = "spark_alias_operation"
  val SortOperation : String = "spark_sort_operation"
  val SortOrder : String = "spark_sort_order"
  val AggregateOperation : String = "spark_aggregate_operation"
  val WriteOperation : String = "spark_write_operation"
  val Dataset : String = "spark_dataset"
  val EndpointDataset : String = "spark_endpoint_dataset"
  val FileEndpoint : String = "hdfs_path"
  val Attribute : String = "spark_dataset_attribute"
  val SimpleDataType : String = "spark_simple_data_type"
  val StructDataType : String = "spark_struct_data_type"
  val StructField : String = "spark_struct_field"
  val ArrayDataType : String = "spark_array_data_type"
  val Expression : String = "spark_expression"
  val AliasExpression : String = "spark_alias_expression"
  val BinaryExpression : String = "spark_binary_expression"
  val AttributeReferenceExpression : String = "spark_attribute_reference_expression"
  val UDFExpression : String = "spark_udf_expression"
}
