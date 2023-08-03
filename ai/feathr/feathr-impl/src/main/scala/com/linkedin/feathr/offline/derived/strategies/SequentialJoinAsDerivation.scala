package com.linkedin.feathr.offline.derived.strategies

import com.linkedin.feathr.common
import com.linkedin.feathr.common.FeatureAggregationType._
import com.linkedin.feathr.common.exception.{ErrorLabel, FeathrConfigException, FeathrException}
import com.linkedin.feathr.common.{FeatureAggregationType, FeatureValue}
import com.linkedin.feathr.offline.PostTransformationUtil
import com.linkedin.feathr.offline.anchored.feature.FeatureAnchorWithSource
import com.linkedin.feathr.offline.client.DataFrameColName
import com.linkedin.feathr.offline.derived.DerivedFeature
import com.linkedin.feathr.offline.derived.functions.SeqJoinDerivationFunction
import com.linkedin.feathr.offline.derived.strategies.SequentialJoinAsDerivation._
import com.linkedin.feathr.offline.job.FeatureTransformation._
import com.linkedin.feathr.offline.job.{AnchorFeatureGroups, FeatureTransformation, KeyedTransformedResult}
import com.linkedin.feathr.offline.join.algorithms.{JoinType, SeqJoinExplodedJoinKeyColumnAppender, SparkJoinWithJoinCondition}
import com.linkedin.feathr.offline.logical.FeatureGroups
import com.linkedin.feathr.offline.mvel.plugins.FeathrExpressionExecutionContext
import com.linkedin.feathr.offline.source.accessor.DataPathHandler
import com.linkedin.feathr.offline.transformation.DataFrameDefaultValueSubstituter.substituteDefaults
import com.linkedin.feathr.offline.transformation.{AnchorToDataSourceMapper, MvelDefinition}
import com.linkedin.feathr.offline.util.{CoercionUtilsScala, DataFrameSplitterMerger, FeathrUtils, FeaturizedDatasetUtils}
import com.linkedin.feathr.sparkcommon.{ComplexAggregation, SeqJoinCustomAggregation}
import org.apache.logging.log4j.LogManager
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import org.apache.spark.sql.{Column, DataFrame, Row, SparkSession}

import scala.collection.JavaConverters._
import scala.collection.mutable

/**
 * This class executes Sequential Join as a derivation on base and expansion features.
 */
private[offline] class SequentialJoinAsDerivation(ss: SparkSession,
                                                  featureGroups: FeatureGroups,
                                                  joiner: SparkJoinWithJoinCondition,
                                                  dataPathHandlers: List[DataPathHandler])
    extends SequentialJoinDerivationStrategy
    with Serializable {
  @transient private val log = LogManager.getLogger(getClass)
  private val colPrefix = "_feathr_seq_join_coerced_left_join_key_"

  override def apply(
      keyTags: Seq[Int],
      keyTagList: Seq[String],
      df: DataFrame,
      derivedFeature: DerivedFeature,
      derivationFunction: SeqJoinDerivationFunction,
      mvelContext: Option[FeathrExpressionExecutionContext]): DataFrame = {
    val allAnchoredFeatures = featureGroups.allAnchoredFeatures
    // gather sequential join feature info
    val seqJoinDerivationFunction = derivationFunction
    val baseFeatureName = seqJoinDerivationFunction.left.feature
    val expansionFeatureName = seqJoinDerivationFunction.right.feature
    val aggregationFunction = seqJoinDerivationFunction.aggregation
    val tagStrList = Some(keyTags.map(keyTagList).toList)
    val outputKey = seqJoinDerivationFunction.left.outputKey
    val seqJoinColumnName = DataFrameColName.genFeatureColumnName(derivedFeature.producedFeatureNames.head, tagStrList)
    if (outputKey.isDefined && outputKey.get.size > 1) {

      throw new FeathrException(
        ErrorLabel.FEATHR_ERROR,
        "We do not support multiple output key in sequential join yet. The size of the output key field can be" +
          s"at most 1. Current output key provided for ${derivedFeature.producedFeatureNames.head} is ${outputKey.get}.")
    }

    /*
     * Load expansion feature.
     * Passthrough feature for expansion does not make sense as the expansion feature should be joined
     * to observation with base feature value participating in the join.

     */
    val (expansion, expansionJoinKey): (DataFrame, Seq[String]) = if (allAnchoredFeatures.contains(expansionFeatureName)) {
      // prepare and get right table
      loadExpansionAnchor(expansionFeatureName, derivedFeature, allAnchoredFeatures, seqJoinColumnName, mvelContext)
    } else {
      throw new FeathrException(
        ErrorLabel.FEATHR_ERROR,
        s"Derived expansion feature is not supported by Sequential Join. sequential join feature ${derivedFeature.producedFeatureNames.head}")
    }

    // Join the observation data with the base feature. Base feature could be anchored or derived
    val (baseFeatureJoinKey, obsWithLeftJoined, toDropColumns): (Seq[String], DataFrame, Seq[String]) =
      prepareLeftForJoin(df, baseFeatureName, featureGroups.allPassthroughFeatures)

    val leftJoinKey = if (allAnchoredFeatures.contains(baseFeatureName)) {
      getJoinKeyForAnchoredFeatures(derivedFeature, tagStrList, baseFeatureJoinKey)
    } else {
      baseFeatureJoinKey
    }

    val featureNameColumnTuples = Seq(seqJoinDerivationFunction.left.feature) zip baseFeatureJoinKey
    val baseTaggedDependency = seqJoinDerivationFunction.left
    val transformationDef = baseTaggedDependency.transformation map { transformation =>
      Map(baseTaggedDependency.feature -> MvelDefinition(transformation))
    } getOrElse Map.empty[String, MvelDefinition]

    val left: DataFrame = PostTransformationUtil.transformFeatures(featureNameColumnTuples, obsWithLeftJoined, transformationDef, getDefaultTransformation, mvelContext)

    // Partition build side of the join based on null values
    val (dfWithNoNull, dfWithNull) = DataFrameSplitterMerger.splitOnNull(left, baseFeatureJoinKey.head)

    // join left and right table (e.g, base and expand feature)
    val groupByColumn = "__feathr_seq_join_group_by_id"
    // We group by the monotonically_increasing_id to ensure we do not lose any of the observation data.
    // This is essentially grouping by all the columns in the left table
    // Note: we cannot add the monotonically_increasing_id before DataFrameSplitterMerger.splitOnNull.
    // the implementation of monotonically_increasing_id is non-deterministic because its result depends on partition IDs.
    // and it can generate duplicate ids between the withNoNull and WithNull part.

    val leftWithUidDF = dfWithNoNull.withColumn(groupByColumn, monotonically_increasing_id)

    // explode left join key columns if optimization is enabled and if left join key is ArrayType
    val (adjustedLeftJoinKey, explodedLeft) = explodeLeftJoinKey(leftWithUidDF, leftJoinKey, derivedFeature.producedFeatureNames.head)

    val joined = joiner.join(adjustedLeftJoinKey, explodedLeft, expansionJoinKey, expansion, JoinType.left_outer)

    /*
     * Substitute defaults. The Sequential Join inherits the default values from the expansion feature definition.
     * This step is done before applying aggregations because the default values should be factored in.
     */
    val withDefaultDF =
      substituteDefaultValues(joined, seqJoinColumnName, allAnchoredFeatures(expansionFeatureName).featureAnchor.defaults.get(expansionFeatureName), ss)

    // Apply aggregations on grouped data only if the aggregation function is non-empty.
    val aggDF = applyAggregationFunction(derivedFeature, seqJoinColumnName, withDefaultDF, aggregationFunction, groupByColumn)

    // Similarly, substitute the default values and apply aggregation function to the null part.
    val dfWithNullWithDefault = substituteDefaultValues(
      dfWithNull.withColumn(seqJoinColumnName, lit(null).cast(joined.schema(seqJoinColumnName).dataType)),
      seqJoinColumnName,
      allAnchoredFeatures(expansionFeatureName).featureAnchor.defaults.get(expansionFeatureName),
      ss)

    val dfWithNullWithAgg = applyAggregationFunction(
      derivedFeature,
      seqJoinColumnName,
      dfWithNullWithDefault.withColumn(groupByColumn, monotonically_increasing_id),
      aggregationFunction,
      groupByColumn)

    // Union the rows that participated in the join and the rows with nulls
    val finalRes = DataFrameSplitterMerger.merge(aggDF, dfWithNullWithAgg)

    // filter result dataframe
    // only keep the feature columns from the right table, the other columns are not needed any more
    // as derived feature will only depends on feature columns
    val featureColumnsRenameMap = Seq((finalRes.col(seqJoinColumnName), seqJoinColumnName))
    // keep columns from left table
    val origLeftColumns = left.columns.map(colName => (finalRes.col(colName), colName)).toSeq
    val reservedColumnPairs = origLeftColumns ++ featureColumnsRenameMap
    val result = finalRes.select(reservedColumnPairs.map { case (x, y) => x.alias(y) }: _*)

    // Drop any additional columns we appended to our left of the table. These columns were added during the course of joining obs
    // with the base feature, but these are not part of the required features. So, we need to drop them.
    toDropColumns.foldLeft(result)((result, col) => result.drop(col))
  }

  /**
   * Sequential join feature's default value is defined by expansion feature's default value.
   * @param inputDF                   input DataFrame.
   * @param seqJoinFeatureColumnName  sequential join feature column name.
   * @param expansionDefaultValue     default value of expansion feature (None if no default value exists).
   * @return DataFrame with default values substituted
   */
  private def substituteDefaultValues(
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

  /**
   * Expode left join key column if necessary. The following conditions should be satisfied -
   * 1. The optimization should be enabled.
   * 2. The join key column should contain an array type column.
   * @param inputDF             Input DataFrame.
   * @param joinKeys            Join key columns for the DataFrame.
   * @param seqJoinFeatureName  Sequential Join feature name (used for providing more context in case of errors).
   * @return adjusted join key column names and DataFrame with exploded column appended.
   */
  def explodeLeftJoinKey(inputDF: DataFrame, joinKeys: Seq[String], seqJoinFeatureName: String): (Seq[String], DataFrame) = {
    // isSeqJoinArrayExplodeEnabled flag is controlled "spark.feathr.seq.join.array.explode.enabled" config.
    // This is a hidden config used by FEATHR DEV ONLY. This knob is required for performance tuning.
    // When enabled, array columns are exploded to avoid BroadcastNestedLoopJoin
    val isSeqJoinArrayExplodeEnabled = FeathrUtils.getFeathrJobParam(ss, FeathrUtils.SEQ_JOIN_ARRAY_EXPLODE_ENABLED).toBoolean
    if (isSeqJoinArrayExplodeEnabled) {
      log.info(s"Sequential Join: exploding join keys: $joinKeys, for feature ${seqJoinFeatureName}")
      val joinKeyColumnAppender = new SeqJoinExplodedJoinKeyColumnAppender(seqJoinFeatureName)
      joinKeyColumnAppender.appendJoinKeyColunmns(joinKeys, inputDF)
    } else {
      (joinKeys, inputDF)
    }
  }

  /**
   * This method extracts the expansion anchored feature.
   * @param allAnchoredFeatures       Anchor name to anchor definition map.
   * @param anchorFeatureName         Anchored feature that should be extracted
   * @param anchorToDataSourceMapper  Anchor to DataSource mapper that should be used get the DataSource.
   * @return Extracted and Transformed feature w/ relevant metadata.
   */
  def getAnchorFeatureDF(
      allAnchoredFeatures: Map[String, FeatureAnchorWithSource],
      anchorFeatureName: String,
      anchorToDataSourceMapper: AnchorToDataSourceMapper,
      mvelContext: Option[FeathrExpressionExecutionContext]): KeyedTransformedResult = {
    val featureAnchor = allAnchoredFeatures(anchorFeatureName)
    val requestedFeatures = featureAnchor.featureAnchor.getProvidedFeatureNames
    val anchorGroup = AnchorFeatureGroups(Seq(featureAnchor), requestedFeatures)
    val ss = SparkSession.builder().getOrCreate()
    val failOnMissingPartition = FeathrUtils.getFeathrJobParam(ss, FeathrUtils.FAIL_ON_MISSING_PARTITION).toBoolean
    val anchorDFMap1 = anchorToDataSourceMapper.getBasicAnchorDFMapForJoin(ss, Seq(featureAnchor), failOnMissingPartition)
    val updatedAnchorDFMap = anchorDFMap1.filter(anchorEntry => anchorEntry._2.isDefined)
      .map(anchorEntry => anchorEntry._1 -> anchorEntry._2.get)
    // We dont need to check if the anchored feature's dataframes are missing (due to skip missing feature) as such
    // seq join features have already been removed in the FeatureGroupsUpdater#getUpdatedFeatureGroupsWithoutInvalidPaths.
    val featureInfo = FeatureTransformation.directCalculate(
      anchorGroup: AnchorFeatureGroups,
      updatedAnchorDFMap(featureAnchor),
      featureAnchor.featureAnchor.sourceKeyExtractor,
      None,
      None,
      None,
      mvelContext)
    (featureInfo)
  }

  /**
   * Retrieve and validate the join keys from the base feature to be used for joining with expansion.
   *
   * @param derivedFeature   the seq join feature being produced.
   * @param tagStrList       key tags of the current join stage
   * @param baseFeatureValueCol the feature value column name produced by the base feature.
   * @return the join key columns on which the base will join the expansion.
   */
  def getJoinKeyForAnchoredFeatures(derivedFeature: DerivedFeature, tagStrList: Option[Seq[String]], baseFeatureValueCol: Seq[String]): Seq[String] = {
    val seqJoinFunction = derivedFeature.derivation.asInstanceOf[SeqJoinDerivationFunction]
    val outputKey = seqJoinFunction.left.outputKey // keytags specified in the output key field in the base feature.
    val baseFeatureKeys = seqJoinFunction.left.key // keytags specified in the base feature key field
    val expansionKeys = seqJoinFunction.right.key // keytags specified in the expansion feature key field
    if (outputKey.isDefined) { // Iterate over the expansion feature key list, and subsitute the output key with computed baseFeatureJoinKey,
      // which is the feature value column of the base feature.
      expansionKeys map (key => {
        if (key == outputKey.get.head) { // If there is only one expansion feature key, it has to be the output key.
          baseFeatureValueCol.head
        } else {
          if (tagStrList.get.diff(baseFeatureKeys).contains(key)) { // the remaining keys must come from the seq join keytags.
            key
          } else {
            throw new FeathrException(
              ErrorLabel.FEATHR_ERROR,
              (s"The expansion key ${key} does not belong to the output key (in base feature) or is not a " +
                s"part of the ${derivedFeature.producedFeatureNames.head}'s keytags."))
          }
        }
      })
    } else {
      if (expansionKeys.size > 1) { // If there is no outputKey, we wont know which is the outputKey column, though we can
        // determine it, we will keep it this way to keep it compatible with online.
        throw new FeathrException(
          ErrorLabel.FEATHR_ERROR,
          "Output key is required in the base feature if expansion field has more than one key. " )
      } else {
        baseFeatureValueCol
      }
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
      derivedFeature: DerivedFeature,
      seqJoinProducedFeatureName: String,
      joined: DataFrame,
      aggregationFunction: String,
      groupByCol: String): DataFrame = {
    if (aggregationFunction.isEmpty) {
      // Sequential Join does not support empty aggregation function.
      // This is checked when loading config but also here to cover all cases.
      throw new FeathrConfigException(
        ErrorLabel.FEATHR_USER_ERROR,
        s"Empty aggregation is not supported for feature ${derivedFeature.producedFeatureNames.head}, in sequential join.")
    } else if (aggregationFunction == FIRST.toString) {
      applyFirstAggregation(seqJoinProducedFeatureName, joined, groupByCol)
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
          val featureNameToJoinedColMap = Map(derivedFeature.producedFeatureNames.head -> seqJoinProducedFeatureName)
          val (groupedDF, presevedColumns) = getGroupedDF(joined, groupByCol, seqJoinProducedFeatureName)
          groupedDF.agg(
            derivationFunction
              .applyAggregation(featureNameToJoinedColMap)(derivedFeature.producedFeatureNames.head)
              .alias(seqJoinProducedFeatureName),
            presevedColumns: _*)
        case _ => // Unsupported Aggregation type
          throw new FeathrConfigException(
            ErrorLabel.FEATHR_USER_ERROR,
            s"Unsupported aggregation type ${aggregationFunction} for the seqJoin feature ${derivedFeature.producedFeatureNames.head}")
      }
    }
  }

  /**
   * Apply FIRST Aggregate function for SeqJoin/LookUp Feature.
   * Note: The function is non-deterministic because its results depends on the order of the rows which may be non-deterministic after a shuffle.
   * This is designed to be used only when the there's only one looked up feature value in a LookUp feature
   * @param seqJoinProducedFeatureName name of the column which will have the seqJoin feature
   * @param joinedDF           Dataframe produced after the SeqJoin and before aggregation
   * @param groupByCol  groupby column
   * @return  dataframe with only the groupBy columns and the aggregated feature value result
   */
  private[feathr] def applyFirstAggregation(seqJoinProducedFeatureName: String, joinedDF: DataFrame, groupByCol: String): DataFrame = {
    val (groupedDF, preservedColumns) = getGroupedDF(joinedDF, groupByCol, seqJoinProducedFeatureName)
    groupedDF.agg(first(seqJoinProducedFeatureName).alias(seqJoinProducedFeatureName), preservedColumns: _*)
  }

  /**
   * Apply Union aggregation for SeqJoin. For more information on the union aggregation rules see the wiki

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
        case fType => throw new FeathrConfigException(ErrorLabel.FEATHR_USER_ERROR, s"Union aggregation of type {$fType} for SeqJoin is not supported.")
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
      case _ => throw new FeathrConfigException(ErrorLabel.FEATHR_USER_ERROR, s"${aggType} aggregation type is not supported for type ${fieldType}")
    }
  }

  /**
   * Load the expansion anchored feature of the seqJoin feature
   *
   * @param expansionFeatureName      name of the seqJoin expansion feature which is an anchored feature
   * @param derivedFeature            name of the derived feature
   * @param allAnchoredFeatures       List of all anchored features
   * @param seqJoinproducedFeatureName name of the column which will have the seqJoin feature
   * @return  dataframe containing the right side of the seqJoin, the produced feature name and the right join key
   */
  private def loadExpansionAnchor(
      expansionFeatureName: String,
      derivedFeature: DerivedFeature,
      allAnchoredFeatures: Map[String, FeatureAnchorWithSource],
      seqJoinproducedFeatureName: String,
      mvelContext: Option[FeathrExpressionExecutionContext]): (DataFrame, Seq[String]) = {
    val expansionFeatureKeys = (derivedFeature.derivation.asInstanceOf[SeqJoinDerivationFunction].right.key)
    val expansionAnchor = allAnchoredFeatures(expansionFeatureName)
    val expandFeatureInfo = getAnchorFeatureDF(allAnchoredFeatures, expansionFeatureName, new AnchorToDataSourceMapper(dataPathHandlers), mvelContext)
    val transformedFeatureDF = expandFeatureInfo.transformedResult.df
    val expansionAnchorKeyColumnNames = expandFeatureInfo.joinKey
    if (expansionFeatureKeys.size != expansionAnchorKeyColumnNames.size) {
      throw new FeathrException(
        ErrorLabel.FEATHR_ERROR,
        s"The keys specified for the expansion feature anchor ${expansionFeatureName} are ${expansionAnchorKeyColumnNames}," +
          s"but the keys specified in the derived feature ${derivedFeature.producedFeatureNames.head} are ${expansionFeatureKeys}. The number" +
          s"of keys in both should be the same.")
    }
    val expansionJoinKeyPrefix = expansionAnchor.featureAnchor.sourceKeyExtractor.toString.replaceAll("[^\\w]", "") + "_"
    // Spark does not support right join key with "." (dot).

    val expansionJoinKeys: Seq[String] = expansionFeatureKeys.map(a => { expansionJoinKeyPrefix + replaceDotInColumnName(a) })

    // keep only key columns and feature column in the output column for feature join
    val joinKeyColumnMaps = expansionAnchorKeyColumnNames.map(z => transformedFeatureDF(z)) zip expansionJoinKeys
    val expandFeatureColumnNamePrefix = expandFeatureInfo.transformedResult.featureNameAndPrefixPairs.head._2
    val featureColumnMaps = Seq(
      (transformedFeatureDF(expandFeatureColumnNamePrefix + expansionFeatureName), DataFrameColName.genFeatureColumnName(seqJoinproducedFeatureName)))
    val expansionFeature = transformedFeatureDF.select((joinKeyColumnMaps ++ featureColumnMaps).map { case (x, y) => x.alias(y) }: _*)

    (expansionFeature, expansionJoinKeys)
  }

  /**
   * Prepare left side of join (observation + base feature) before joining with expansion.
   * Passthrough features and anchored features should already be present in the left side of the join.
   * @param leftDf                   left side of sequential join.
   * @param baseFeatureName          base feature name.
   * @param allPassthroughFeatures   map of all passthrough features to FeatureAnchorWithSource.
   * @return
   */
  def prepareLeftForJoin(
      leftDf: DataFrame,
      baseFeatureName: String,
      allPassthroughFeatures: Map[String, FeatureAnchorWithSource]): (Seq[String], DataFrame, Seq[String]) = {
    if (allPassthroughFeatures.contains(baseFeatureName)) {
      (
        Seq(colPrefix.concat(baseFeatureName)),
        leftDf.withColumn(colPrefix.concat(baseFeatureName), col(FEATURE_NAME_PREFIX.concat(baseFeatureName))),
        Seq(colPrefix.concat(baseFeatureName)))
    } else {
      val baseFeatureColName = DataFrameColName.genFeatureColumnName(baseFeatureName)
      leftDf.columns.find(c => c.split(FEATURE_TAGS_PREFIX).head == baseFeatureColName) match {
        case Some(x) =>
          val seqJoinBaseColName = colPrefix.concat(baseFeatureName)
          (Seq(seqJoinBaseColName), leftDf.withColumn(seqJoinBaseColName, col(x)), Seq(seqJoinBaseColName))
        case None =>
          throw new FeathrException(
            ErrorLabel.FEATHR_ERROR,
            s"Could not find base feature column that starts with $baseFeatureColName, in context DataFrame [${leftDf.columns.mkString(", ")}]")
      }
    }
  }
}
/*
 * Utilites methods to calculate a seq join feature using dataframes
 */
private[offline] object SequentialJoinAsDerivation {
  // Spark does not support right join key with "." (dot).

  private val withDotLiteral = "__feathr_dot__"

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
      case fType => throw new FeathrConfigException(ErrorLabel.FEATHR_USER_ERROR, s"Cannot coerce feature with type ${fType} to join key in SequentialJoin")
    }
  }

  /**
   * Utility method to replace "." (dot) in column name with "__feathr_dot__".
   * Example: a.b.c => a__feathr_dot__b__feathr_dot__c
   */
  def replaceDotInColumnName(columnName: String): String = columnName.replaceAll("\\.", withDotLiteral)
}
