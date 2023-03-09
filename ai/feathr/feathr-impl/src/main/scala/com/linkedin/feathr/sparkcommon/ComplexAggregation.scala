package com.linkedin.feathr.sparkcommon

import com.linkedin.feathr.common.configObj.ConfigObj
import org.apache.spark.sql.{Column, DataFrame}
import org.apache.spark.sql.functions.expr
/**
  * Base class to implement different complex feature transformations, such as poisson-gamma decay, normalization, etc.
  * The workflow of this kind of transformer is:
  * 1. feathr calls init to pass definitions to transformer
  * 2. feathr calls getIntermediateColumns to generate all intermediate columns and append to input dataframe
  * 3. feathr group the dataframe by feature join key
  * 4. feathr calls getAggColumn to get aggregation definition and applied on the grouped dataframe
  * 5. feathr calls postProcessing to further process the aggregated column, e.g., pick topK
  */
private[feathr] abstract class ComplexAggregation extends Serializable {

  /**
    * input ConfigObject for this aggregation transformation
    * typically, implementation should store this as member variable for future use
    * other initialization code can also go here
    * @param configObj
    */
  def init(configObj: ConfigObj)

  /**
    * return all intermediate column names
    */
  def getIntermediateColumnNames(): Seq[String]

  /**
    * generate intermediate columns declared in [[getIntermediateColumnNames()]], these columns can later
    * be used to generate aggregation column, Column here could be based on SparkSQL window functions,
    * these columns will be dropped after the evaluation of this complex aggregation
    * @param dataFrameWithKeyColumns source dataframe with join key appended
    * @return pairs of intermediate columns names and their definitions
    */
  def getIntermediateColumns(dataFrameWithKeyColumns: DataFrame): Seq[(String, Column)]

  /**
    * generate aggregation column definitions
    * this aggregation is applied on the dataframe grouped by feature join keys
    * @return aggregation definition, will be appended to the dataframe with feature column
    */
  def getAggColumn(): Column

  /**
    * used to apply various processing after aggregation, e.g., topK
    * @param inputFeatureColumn column name of the feature defined by this ComplexAggregation class
    * @return output feature column definition, this will be the final output feature column
    */
  def postProcessing(inputFeatureColumn: String): Column = expr(inputFeatureColumn)
}