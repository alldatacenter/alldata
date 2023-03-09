package com.linkedin.feathr.offline.derived.strategies

import com.linkedin.feathr.common
import com.linkedin.feathr.common.{FeatureAggregationType, FeatureValue}
import com.linkedin.feathr.common.FeatureAggregationType.{AVG, ELEMENTWISE_AVG, ELEMENTWISE_MAX, ELEMENTWISE_MIN, ELEMENTWISE_SUM, MAX, MIN, SUM, UNION}
import com.linkedin.feathr.exception.ErrorLabel.FEATHR_USER_ERROR
import com.linkedin.feathr.exception.FeathrConfigException
import com.linkedin.feathr.offline.join.algorithms.SeqJoinExplodedJoinKeyColumnAppender
import com.linkedin.feathr.offline.transformation.DataFrameDefaultValueSubstituter.substituteDefaults
import com.linkedin.feathr.offline.util.{CoercionUtilsScala, FeaturizedDatasetUtils, FeathrUtils}
import com.linkedin.feathr.sparkcommon.SeqJoinCustomAggregation
import org.apache.spark.sql.functions.{avg, collect_list, expr, first, max, min, sum, udf}
import org.apache.spark.sql.{Column, DataFrame, Row, SparkSession}
import org.apache.spark.sql.types.{ArrayType, DataType, DoubleType, FloatType, IntegerType, LongType, MapType, NumericType, StringType, StructType}

import scala.collection.JavaConverters._
import scala.collection.mutable

/**
 * This class contains the various functions needed to perform sequential join. These functions include substituting default
 * values, performing the aggregation, etc. Most functions were copied from [[SequentialJoinAsDerivation]] and slightly
 * rewritten to work with the compute model inputs.
 */
private[offline] object SeqJoinAggregator {
  def substituteDefaultValuesForSeqJoinFeature(
    inputDF: DataFrame,
    seqJoinFeatureColumnName: String,
    expansionDefaultValue: Option[FeatureValue],
    ss: SparkSession): DataFrame = {
    val defaultValue = expansionDefaultValue match {
      case Some(x) => Map(seqJoinFeatureColumnName -> x)
      case None => Map.empty[String, FeatureValue]
    }
    // derived feature does not have feature type
    substituteDefaults(inputDF, Seq(seqJoinFeatureColumnName), defaultValue, Map(), ss)
  }

  def coerceLeftDfForSeqJoin(
    featureColumnNames: Seq[String],
    contextDF: DataFrame
  ): DataFrame = {

    // Transform the features with the provided transformations
    val featureValueColumn = featureColumnNames.map {
      case columnName =>
        val fieldIndex = contextDF.schema.fieldIndex(columnName.split("\\.").head)
        val fieldType = contextDF.schema.toList(fieldIndex)
        getDefaultTransformation(fieldType.dataType, columnName)
    }

    val featureValueToJoinKeyColumnName = featureValueColumn zip featureColumnNames
    featureValueToJoinKeyColumnName.foldLeft(contextDF)((s, x) => s.withColumn(x._2, x._1))
  }
  /**
   * Utility method to coerce left join key columns for seq join.
   * @param dataType
   * @param columnName
   * @return
   */
  def getDefaultTransformation(dataType: DataType, columnName: String): Column = {
    // Convert 1d tensor FDS row to seq[string] for sequential join
    def oneDTensorFDSStructToString(row: Row): Seq[String] = {
      if (row != null) {
        val dimensions = row.getAs[Seq[_]](FeaturizedDatasetUtils.FDS_1D_TENSOR_DIM)
        if (dimensions.nonEmpty) {
          dimensions.map(_.toString)
        } else null
      } else null
    }

    def fvArrayToString(inputArray: Seq[Any]): Seq[String] = {
      if (inputArray == null) {
        Seq()
      } else {
        CoercionUtilsScala.coerceFeatureValueToStringKey(new common.FeatureValue(inputArray.asJava))
      }
    }

    def fvMapToString(inputMap: Map[String, Float]): Seq[String] = {
      if (inputMap == null) {
        Seq()
      } else {
        CoercionUtilsScala.coerceFeatureValueToStringKey(new common.FeatureValue(inputMap.asJava))
      }
    }
    val coerceMapToStringKey = udf(fvMapToString(_: Map[String, Float]))
    val coerceArrayToStringKey = udf(fvArrayToString(_: Seq[Any]))
    val coerce1dTensorFDSStructToStringKey = udf(oneDTensorFDSStructToString(_: Row))
    dataType match {
      case _: StringType => expr(columnName)
      case _: NumericType => expr(columnName)
      case _: MapType => coerceMapToStringKey(expr(columnName))
      case _: ArrayType => coerceArrayToStringKey(expr(columnName))
      case _: StructType => coerce1dTensorFDSStructToStringKey(expr(columnName))
      case fType => throw new FeathrConfigException(FEATHR_USER_ERROR, s"Cannot coerce feature with type ${fType} to join key in SequentialJoin")
    }
  }

  /**
   * Apply aggregation for SeqJoin. We always groupBy the entire left dataframe to keep the original number of rows intact.
   * @param derivedFeature    Name of the derived feature
   * @param seqJoinProducedFeatureName name of the column which will have the seqJoin feature
   * @param joined           Dataframe produced after the SeqJoin and before aggregation
   * @param aggregationFunction Name of the aggregation function, could be a class extending [[ComplexAggregation]] or
   *                            one of the functions mentioned in [[FeatureAggregationType]]
   * @return  dataframe with only the groupBy columns and the aggregated feature value result
   */
  def applyAggregationFunction(
    producedFeatureName: String,
    seqJoinProducedFeatureName: String,
    joined: DataFrame,
    aggregationFunction: String,
    groupByCol: String): DataFrame = {
    if (aggregationFunction.isEmpty) {
      // Sequential Join does not support empty aggregation function.
      // This is checked when loading config but also here to cover all cases.
      throw new FeathrConfigException(
        FEATHR_USER_ERROR,
        s"Empty aggregation is not supported for feature ${producedFeatureName}, in sequential join.")
    } else if (aggregationFunction == UNION.toString) {
      applyUnionAggregation(seqJoinProducedFeatureName, joined, groupByCol)
    } else if (Seq(SUM, MAX, MIN, AVG).map(_.toString).contains(aggregationFunction)) {
      applyNumericAggregation(FeatureAggregationType.valueOf(aggregationFunction), seqJoinProducedFeatureName, joined, groupByCol)
    } else if (Seq(ELEMENTWISE_MIN, ELEMENTWISE_MAX, ELEMENTWISE_SUM, ELEMENTWISE_AVG).map(_.toString).contains(aggregationFunction)) {
      applyElementWiseAggregation(FeatureAggregationType.valueOf(aggregationFunction), seqJoinProducedFeatureName, joined, groupByCol)
    } else {
      val aggTypeClass = Class.forName(aggregationFunction).newInstance()
      aggTypeClass match {
        case derivationFunction: SeqJoinCustomAggregation => // Custom aggregation class
          val featureNameToJoinedColMap = Map(producedFeatureName -> seqJoinProducedFeatureName)
          val (groupedDF, preservedColumns) = getGroupedDF(joined, groupByCol, seqJoinProducedFeatureName)
          groupedDF.agg(
            derivationFunction
              .applyAggregation(featureNameToJoinedColMap)(producedFeatureName)
              .alias(seqJoinProducedFeatureName),
            preservedColumns: _*)
        case _ => // Unsupported Aggregation type
          throw new FeathrConfigException(
            FEATHR_USER_ERROR,
            s"Unsupported aggregation type ${aggregationFunction} for the seqJoin feature ${producedFeatureName}")
      }
    }
  }

  /**
   * Explode left join key column if necessary. The spark join condition for sequential join is capable of handling an array
   * type as the left join key (it will join if element from right is in the array in the left). However, in some cases,
   * we have seen performance improvements when instead the left join key array is exploded into individual rows. Thus this
   * function will perform the explode as necessary. The following conditions should be satisfied -
   * 1. The optimization should be enabled.
   * 2. The join key column should contain an array type column.
   * @param ss                  spark session
   * @param inputDF             Input Datafeathr.
   * @param joinKeys            Join key columns for the Datafeathr.
   * @param seqJoinFeatureName  Sequential Join feature name (used for providing more context in case of errors).
   * @return adjusted join key column names and DataFrame with exploded column appended.
   */
  private[feathr] def explodeLeftJoinKey(ss: SparkSession, inputDF: DataFrame, joinKeys: Seq[String], seqJoinFeatureName: String): (Seq[String], DataFrame) = {
    // isSeqJoinArrayExplodeEnabled flag is controlled "spark.feathr.seq.join.array.explode.enabled" config.
    // When enabled, array columns are exploded to avoid BroadcastNestedLoopJoin
    val isSeqJoinArrayExplodeEnabled = FeathrUtils.getFeathrJobParam(ss, FeathrUtils.SEQ_JOIN_ARRAY_EXPLODE_ENABLED).toBoolean
    if (isSeqJoinArrayExplodeEnabled) {
      val joinKeyColumnAppender = new SeqJoinExplodedJoinKeyColumnAppender(seqJoinFeatureName)
      joinKeyColumnAppender.appendJoinKeyColunmns(joinKeys, inputDF)
    } else {
      (joinKeys, inputDF)
    }
  }

  /**
   * Apply Union aggregation for SeqJoin.
   * @param groupByCol  groupby column
   * @param seqJoinProducedFeatureName name of the column which will have the seqJoin feature
   * @param joinedDF           Dataframe produced after the SeqJoin and before aggregation
   * @return  dataframe with only the groupBy columns and the aggregated feature value result
   */
  private[feathr] def applyUnionAggregation(seqJoinProducedFeatureName: String, joinedDF: DataFrame, groupByCol: String): DataFrame = {
    def union1DFDSTensor(row: Row, otherRow: Row): Row = {
      val indices = row.getAs[mutable.WrappedArray[_]](0).union(otherRow.getAs[mutable.WrappedArray[_]](0))
      val values = row.getAs[mutable.WrappedArray[_]](1) ++ otherRow.getAs[mutable.WrappedArray[_]](1)
      Row.apply(indices, values)
    }
    val flatten_map = udf((featureValues: Seq[Map[String, Float]]) => featureValues.flatten.toMap)
    val fieldIndex = joinedDF.schema.fieldIndex(seqJoinProducedFeatureName)
    val fieldType = joinedDF.schema.toList(fieldIndex)
    val (groupedDF, preservedColumns) = getGroupedDF(joinedDF, groupByCol, seqJoinProducedFeatureName)
    val aggDF: DataFrame = {
      fieldType.dataType match {
        case _: StringType => groupedDF.agg(collect_list(seqJoinProducedFeatureName).alias(seqJoinProducedFeatureName), preservedColumns: _*)
        case _: NumericType => groupedDF.agg(collect_list(seqJoinProducedFeatureName).alias(seqJoinProducedFeatureName), preservedColumns: _*)
        case _: MapType => groupedDF.agg(flatten_map(collect_list(seqJoinProducedFeatureName)).alias(seqJoinProducedFeatureName), preservedColumns: _*)
        // FDS 1d Tensor
        case structType: StructType if structType.fields.length == 2 =>
          val flatten_FDSStruct = udf((featureValues: Seq[Row]) => {
            val mergedRow =
            // If the feature values are null then return empty indices and values for 1d FDS tensor
              if (featureValues.isEmpty) Row.apply(mutable.WrappedArray.empty, mutable.WrappedArray.empty)
              else featureValues.reduce((row, otherRow) => union1DFDSTensor(row, otherRow))
            mergedRow
          }, structType)
          groupedDF.agg(flatten_FDSStruct(collect_list(seqJoinProducedFeatureName)).alias(seqJoinProducedFeatureName), preservedColumns: _*)
        case fType => throw new FeathrConfigException(FEATHR_USER_ERROR, s"Union aggregation of type {$fType} for SeqJoin is not supported.")
      }
    }
    aggDF
  }

  /**
   * utility function for sequential join wit aggregation
   * @param joinedDF dataframe after sequential expansion feature joined
   * @param groupByCol groupby column for the sequential join aggregation
   * @param excludeColumn column that should not be included in the output column
   * @return (grouped input dataframe, column to preserved in the output dataframe)
   */
  private def getGroupedDF(joinedDF: DataFrame, groupByCol: String, excludeColumn: String) = {
    val groupedDF = joinedDF.groupBy(expr(groupByCol))
    val presevedColumns = joinedDF.columns.collect {
      case colName if (!colName.equals(groupByCol) && !colName.equals(excludeColumn)) =>
        first(expr(colName)).as(colName)
    }
    (groupedDF, presevedColumns)
  }

  /* Given input parameters of the indices and values arrays of 2 FDS 1d sparse tensors, this function will apply
     * the appropriate elementwise aggregation (max, min, or sum). Note that we apply sum in the case of ELEMENTWISE_AVG
     * and ELEMENTWISE_SUM because we will be dividing by the number of rows at the end for ELEMENTWISE_AVG. The elementwise
     * component is accomplished by converting the tensor into a map where indices are the keys and values are the values.
     * The map is then converted to a list which we can then apply elementwise aggregation functions via groupBy.
     */
  private def applyElementwiseOnRow[T: Numeric](
    indices1: mutable.WrappedArray[_],
    indices2: mutable.WrappedArray[_],
    values1: mutable.WrappedArray[T],
    values2: mutable.WrappedArray[T],
    aggType: FeatureAggregationType) = {
    val map1 = (indices1 zip values1).toMap
    val map2 = (indices2 zip values2).toMap
    val union_list = map1.toList ++ map2.toList
    aggType match {
      case ELEMENTWISE_AVG | ELEMENTWISE_SUM => union_list.groupBy(_._1).mapValues(_.map(_._2).sum)
      case ELEMENTWISE_MIN => union_list.groupBy(_._1).mapValues(_.map(_._2).min)
      case ELEMENTWISE_MAX => union_list.groupBy(_._1).mapValues(_.map(_._2).max)
    }
  }

  /* Element wise aggregation UDF that takes 2 rows that are of the format of 1d FDS tensor and performs the appropriate
   * elementwise aggregation between the two rows. The DataType of the values in the FDS tensor is also passed in as
   * the last parameter so we can extract the values.
   */
  private def tensorElementWiseAggregate(row: Row, otherRow: Row, valueType: DataType, aggType: FeatureAggregationType): Row = {
    // Grab the indicies and values of the tensor
    val indices1 = row.getAs[mutable.WrappedArray[_]](0)
    val indices2 = otherRow.getAs[mutable.WrappedArray[_]](0)
    val union_map = valueType match {
      case _: FloatType =>
        val values1 = row.getAs[mutable.WrappedArray[Float]](1)
        val values2 = otherRow.getAs[mutable.WrappedArray[Float]](1)
        applyElementwiseOnRow(indices1, indices2, values1, values2, aggType)
      case _: IntegerType =>
        val values1 = row.getAs[mutable.WrappedArray[Int]](1)
        val values2 = otherRow.getAs[mutable.WrappedArray[Int]](1)
        applyElementwiseOnRow(indices1, indices2, values1, values2, aggType)
      case _: DoubleType =>
        val values1 = row.getAs[mutable.WrappedArray[Double]](1)
        val values2 = otherRow.getAs[mutable.WrappedArray[Double]](1)
        applyElementwiseOnRow(indices1, indices2, values1, values2, aggType)
      case _: LongType =>
        val values1 = row.getAs[mutable.WrappedArray[Long]](1)
        val values2 = otherRow.getAs[mutable.WrappedArray[Long]](1)
        applyElementwiseOnRow(indices1, indices2, values1, values2, aggType)
      case badType => throw new UnsupportedOperationException(
        s"${badType} is not supported as a value type for 1d sparse tensors in elementwise aggregation. The only types" +
          s"supported are Floats, Integers, Doubles, and Longs.")
    }
    Row.apply(union_map.keySet.toList, union_map.values.toList)
  }

  /**
   * Apply element wise aggregation for SeqJoin
   * @param groupByCol  groupby column
   * @param aggType            Name of the aggregation function as mentioned in [[FeatureAggregationType]]
   * @param seqJoinProducedFeatureName name of the column which will have the seqJoin feature
   * @param joinedDF           Dataframe produced after thee SeqJoin and before aggregation
   * @return  dataframe with only the groupBy columns and the aggregated feature value result
   */
  private[offline] def applyElementWiseAggregation(
    aggType: FeatureAggregationType,
    seqJoinProducedFeatureName: String,
    joinedDF: DataFrame,
    groupByCol: String): DataFrame = {
    val fieldIndex = joinedDF.schema.fieldIndex(seqJoinProducedFeatureName)
    val fieldType = joinedDF.schema.toList(fieldIndex)
    def sumArr =
      udf((a: Seq[Seq[Float]]) => {
        if (a.isEmpty) {
          Seq()
        } else {
          val zeroSeq = Seq.fill[Float](a.head.size)(0.0f)
          a.foldLeft(zeroSeq)((a, x) => (a zip x).map { case (u, v) => u + v })
        }
      })
    def avgArr =
      udf((a: Seq[Seq[Float]]) => {
        if (a.isEmpty) {
          Seq()
        } else {
          val zeroSeq = Seq.fill[Float](a.head.size)(0.0f)
          val sum = a.foldLeft(zeroSeq)((a, x) => (a zip x).map { case (u, v) => u + v })
          sum map (value => value / a.size)
        }
      })
    def minArr =
      udf((a: Seq[Seq[Float]]) => {
        val newList = a.transpose
        newList map (list => list.min)
      })
    def maxArr =
      udf((a: Seq[Seq[Float]]) => {
        val newList = a.transpose
        newList map (list => list.max)
      })
    // Explicitly cast Array(Double) to Float before applying aggregate
    def transformToFloat(elementType: DataType, column: Column): Column = {
      elementType match {
        case _: NumericType if elementType != FloatType => column.cast("array<float>")
        case _: FloatType => column
        case _ =>
          throw new UnsupportedOperationException(
            s"${aggType} aggregation type not supported for feature '${seqJoinProducedFeatureName}', " +
              s"${aggType} only supports array of numeric type but found array of ${elementType}")

      }
    }

    // Return element-wise aggregate UDF based on the element type of the array.
    def aggregate(elementType: DataType, column: Column): Column = {
      val columnAsList = collect_list(transformToFloat(elementType, column))
      aggType match {
        case ELEMENTWISE_SUM => sumArr(columnAsList)
        case ELEMENTWISE_AVG => avgArr(columnAsList)
        case ELEMENTWISE_MIN => minArr(columnAsList)
        case ELEMENTWISE_MAX => maxArr(columnAsList)
      }
    }

    val (groupedDF, preservedColumns) = getGroupedDF(joinedDF, groupByCol, seqJoinProducedFeatureName)
    fieldType.dataType match {
      case ftype: ArrayType =>
        groupedDF.agg(
          aggregate(ftype.elementType, expr(seqJoinProducedFeatureName))
            .alias(seqJoinProducedFeatureName),
          preservedColumns: _*)
      // 1D Sparse tensor case
      case structType: StructType if structType.fields.length == 2 =>
        val valueType = structType.apply("values").dataType.asInstanceOf[ArrayType].elementType
        val flatten_FDSStruct = udf((featureValues: Seq[Row]) => {
          val mergedRow =
          // If the feature values are null then return empty indices and values for 1d FDS tensor
            if (featureValues.isEmpty) Row.apply(List.empty, List.empty)
            else featureValues.reduce((row, nextRow) => tensorElementWiseAggregate(row, nextRow, valueType, aggType))
          // Note the elementWiseSum1DFDSTensor function returns the row where the values are Lists and not WrappedArray
          // Note that here we have to duplicate the code to divide by the length to get the average because we can't
          // easily extract out the division operation into a method that takes numerics.
          val indices = mergedRow.getAs[List[_]](0)
          val values = valueType match {
            case _: FloatType =>
              val rawValues = mergedRow.getAs[List[Float]](1)
              if (aggType == ELEMENTWISE_AVG) {
                rawValues.map(_ / featureValues.length)
              } else {
                rawValues
              }
            case _: IntegerType =>
              val rawValues = mergedRow.getAs[List[Int]](1)
              if (aggType == ELEMENTWISE_AVG) {
                rawValues.map(_ / featureValues.length)
              } else {
                rawValues
              }
            case _: DoubleType =>
              val rawValues = mergedRow.getAs[List[Double]](1)
              if (aggType == ELEMENTWISE_AVG) {
                rawValues.map(_ / featureValues.length)
              } else {
                rawValues
              }
            case _: LongType =>
              val rawValues = mergedRow.getAs[List[Long]](1)
              if (aggType == ELEMENTWISE_AVG) {
                rawValues.map(_ / featureValues.length)
              } else {
                rawValues
              }
            case badType => throw new UnsupportedOperationException(
              s"${badType} is not supported as a value type for 1d sparse tensors in elementwise aggregation.")
          }
          Row.apply(indices, values)
        }, structType)
        groupedDF.agg(flatten_FDSStruct(collect_list(seqJoinProducedFeatureName)).alias(seqJoinProducedFeatureName), preservedColumns: _*)
      case _ =>
        throw new UnsupportedOperationException(
          s"${aggType} aggregation type not supported for feature ${seqJoinProducedFeatureName}, " +
            s"${aggType} only supports array and 1d sparse tensor type features")
    }
  }

  /**
   * Apply arithmetic aggregation for SeqJoin
   * @param groupByCol  groupby column
   * @param aggType            Name of the aggregation function as mentioned in [[FeatureAggregationType]]
   * @param seqJoinproducedFeatureName name of the column which will have the seqJoin feature
   * @param joinedDF           Dataframe produced after thee SeqJoin and before aggregation
   * @return  dataframe with only the groupBy columns and the aggregated feature value result
   */
  private def applyNumericAggregation(
    aggType: FeatureAggregationType,
    seqJoinproducedFeatureName: String,
    joinedDF: DataFrame,
    groupByCol: String): DataFrame = {
    val fieldIndex = joinedDF.schema.fieldIndex(seqJoinproducedFeatureName)
    val fieldType = joinedDF.schema.toList(fieldIndex)
    val (groupedDF, presevedColumns) = getGroupedDF(joinedDF, groupByCol, seqJoinproducedFeatureName)
    fieldType.dataType match {
      case ftype: NumericType =>
        val aggDF: DataFrame = aggType match {
          case SUM => groupedDF.agg(sum(seqJoinproducedFeatureName).alias(seqJoinproducedFeatureName), presevedColumns: _*)
          case MAX => groupedDF.agg(max(seqJoinproducedFeatureName).alias(seqJoinproducedFeatureName), presevedColumns: _*)
          case MIN => groupedDF.agg(min(seqJoinproducedFeatureName).alias(seqJoinproducedFeatureName), presevedColumns: _*)
          case AVG => groupedDF.agg(avg(seqJoinproducedFeatureName).alias(seqJoinproducedFeatureName), presevedColumns: _*)
        }
        aggDF
      case _ => throw new FeathrConfigException(FEATHR_USER_ERROR, s"${aggType} aggregation type is not supported for type ${fieldType}")
    }
  }
}