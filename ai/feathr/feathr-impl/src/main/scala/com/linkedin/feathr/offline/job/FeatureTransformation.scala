package com.linkedin.feathr.offline.job

import com.linkedin.feathr.common.exception.{ErrorLabel, FeathrException, FeathrFeatureTransformationException}
import com.linkedin.feathr.common.tensor.TensorData
import com.linkedin.feathr.common.types.FeatureType
import com.linkedin.feathr.common.{AnchorExtractorBase, _}
import com.linkedin.feathr.offline.anchored.anchorExtractor.{SQLConfigurableAnchorExtractor, SimpleConfigurableAnchorExtractor, TimeWindowConfigurableAnchorExtractor}
import com.linkedin.feathr.offline.anchored.feature.{FeatureAnchor, FeatureAnchorWithSource}
import com.linkedin.feathr.offline.anchored.keyExtractor.{MVELSourceKeyExtractor, SpecificRecordSourceKeyExtractor}
import com.linkedin.feathr.offline.client.DataFrameColName
import com.linkedin.feathr.offline.config.{MVELFeatureDefinition, TimeWindowFeatureDefinition}
import com.linkedin.feathr.offline.generation.IncrementalAggContext
import com.linkedin.feathr.offline.job.FeatureJoinJob.FeatureName
import com.linkedin.feathr.offline.join.DataFrameKeyCombiner
import com.linkedin.feathr.offline.mvel.plugins.FeathrExpressionExecutionContext
import com.linkedin.feathr.offline.source.accessor.{DataSourceAccessor, NonTimeBasedDataSourceAccessor, TimeBasedDataSourceAccessor}
import com.linkedin.feathr.offline.swa.SlidingWindowFeatureUtils
import com.linkedin.feathr.offline.transformation.FeatureColumnFormat.FeatureColumnFormat
import com.linkedin.feathr.offline.transformation._
import com.linkedin.feathr.offline.util.FeaturizedDatasetUtils.tensorTypeToDataFrameSchema
import com.linkedin.feathr.offline.util._
import com.linkedin.feathr.offline.util.datetime.{DateTimeInterval, OfflineDateTimeUtils}
import com.linkedin.feathr.offline.{FeatureDataFrame, JoinKeys}
import com.linkedin.feathr.sparkcommon.{SimpleAnchorExtractorSpark, SourceKeyExtractor}
import com.linkedin.feathr.swj.aggregate.AggregationType
import com.linkedin.feathr.{common, offline}
import org.apache.avro.generic.IndexedRecord
import org.apache.logging.log4j.LogManager
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import org.apache.spark.sql.{DataFrame, Row, SparkSession}
import org.apache.spark.util.sketch.BloomFilter

import java.util.UUID.randomUUID
import java.util.concurrent.Executors
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

/**
 * represent a group of anchors defined on the same source view and a subset of features defined are requested
 * @param anchorsWithSameSource the required FeatureAnchorWithSource
 * @param requestedFeatures all requested feature names within the anchorsWithSameSource
 */
private[offline] case class AnchorFeatureGroups(anchorsWithSameSource: Seq[FeatureAnchorWithSource], requestedFeatures: Seq[String])

/**
 * Context info needed in feature transformation
 * @param featureAnchorWithSource feature annchor with its source
 * @param featureNamePrefixPairs map of feature name to its prefix
 * @param transformer transformer of anchor
 */
private[offline] case class TransformInfo(featureAnchorWithSource: FeatureAnchorWithSource,
                                 featureNamePrefixPairs: Seq[(FeatureName, FeatureName)],
                                 transformer: AnchorExtractorBase[IndexedRecord])

/**
 * Represent the transformed result of an anchor extractor after evaluating its features
 * @param featureNameAndPrefixPairs pairs of feature name and feature name prefix
 * E.g, if feature is A, prefix is pre_, then the feature column in the df should be pre_A
 * @param df transformed dataframe transformed dataframe
 * the dataframe contains the above features
 * @param featureColumnFormats the feature format in the dataframe
 * @param inferredFeatureTypes the inferred feature types for the features in the dataframe
 */
private[offline] case class TransformedResult(
    featureNameAndPrefixPairs: Seq[(FeatureName, String)],
    df: DataFrame,
    featureColumnFormats: Map[String, FeatureColumnFormat],
    inferredFeatureTypes: Map[String, FeatureTypeConfig])

/**
 * Represent the transformed result of an anchor extractor, with the join key of the features
 * @param joinKey  join key columns of the transformed dataframe
 * @param transformedResult result of a transformation
 */
private[offline] case class KeyedTransformedResult(joinKey: JoinKeys, transformedResult: TransformedResult)

private[offline] object FeatureTransformation {
  private val logger = LogManager.getLogger(getClass)

  val FEATURE_DATA_JOIN_KEY_COL_PREFIX = "FeathrFeatureJoinKeyCol_"
  val FEATURE_NAME_PREFIX = "__feathr_feature_"
  val FEATURE_TAGS_PREFIX = "__feathr_tags_"
  val JOIN_KEY_OBSERVATION_PREFIX = "__feathr_left_join_key_column_"
  val USER_FACING_MULTI_DIM_FDS_TENSOR_UDF_NAME = "FDSExtract"

  // feature name, column prefix
  type FeatureNameAndColumnPrefix = (String, String)

  /**
   * Extract feature key column names from the input feature RDD using the sourceKeyExtractor.
   * @param sourceKeyExtractor key extractor that knows what are the key column in a feature RDD.
   * @param withKeyColumnRDD RDD that contains the key columns.
   * @return feature key column names
   */
  def getFeatureKeyColumnNamesRdd(sourceKeyExtractor: SourceKeyExtractor, withKeyColumnRDD: RDD[_]): Seq[String] = {
    if (withKeyColumnRDD.isEmpty) {
      sourceKeyExtractor.getKeyColumnNames(None)
    } else {
      sourceKeyExtractor.getKeyColumnNames(Some(withKeyColumnRDD.first()))
    }
  }

  /**
   * Extract feature key column names from the input feature DataFrame using the sourceKeyExtractor.
   * @param sourceKeyExtractor key extractor that knows what are the key column in a feature RDD.
   * @param withKeyColumnDF DataFrame that contains the key columns.
   * @return feature key column names
   */
  def getFeatureKeyColumnNames(sourceKeyExtractor: SourceKeyExtractor, withKeyColumnDF: DataFrame): Seq[String] = {
      if (withKeyColumnDF.head(1).isEmpty) {
        sourceKeyExtractor.getKeyColumnNames(None)
      } else {
        sourceKeyExtractor.getKeyColumnNames(Some(withKeyColumnDF.first()))
      }
  }

  // get the feature column prefix which will be appended to all feature columns of the dataframe returned by the transformer
  def getFeatureNamePrefix(transformer: AnyRef): String = {
    transformer.getClass.getSimpleName + "_"
  }

  /**
   * apply aggregations for requested features defined in a set of anchors that share same source views
   * all the requested features will be evaluated in sequential order
   *
   * @param featureAnchorWithSources a set of anchors that share the same sources to apply aggregate
   * @param transformedResultWithKey the non-aggregation transform result of a set of anchors defined on same source
   * @return dataframe with join key and each requested feature as a column
   */
  def applyAggregate(featureAnchorWithSources: Seq[FeatureAnchorWithSource], transformedResultWithKey: KeyedTransformedResult): KeyedTransformedResult = {
    // 1. get the aggregation columns for all the requested features from extractors
    val aggColumns = featureAnchorWithSources.flatMap(featureAnchorWithSource => {
      featureAnchorWithSource.featureAnchor.extractor match {
        case extractor: SimpleAnchorExtractorSpark =>
          val df = transformedResultWithKey.transformedResult.df
          val aggColumns = extractor.aggregateAsColumns(df).collect {
            case pr if featureAnchorWithSource.selectedFeatures.contains(pr._1) => pr._2.as(pr._1) }
          aggColumns
        case _ => Seq()
      }
    })
    val df = transformedResultWithKey.transformedResult.df
    // 2. sequentially apply all the aggregations defined in the columns returned previously
    val aggedDF = if (!aggColumns.isEmpty) {
      val groupColumns = transformedResultWithKey.joinKey
      val grouped = df.groupBy(groupColumns.map(expr): _*)
      // apply aggregations
      grouped.agg(aggColumns.head, aggColumns.tail: _*)
    } else {
      df
    }

    // 3. apply post processing defined by the features
    val postProcessingColumns = featureAnchorWithSources.flatMap(featureAnchorWithSource => {
      featureAnchorWithSource.featureAnchor.extractor match {
        case extractor: SimpleAnchorExtractorSpark =>
          extractor.setInternalParams(SELECTED_FEATURES, s"[${featureAnchorWithSource.selectedFeatures.mkString(",")}]")
          extractor.postProcessing(aggedDF)
        case _ => Seq()
      }
    })

    // 4. remove unnecessary columns returned by extractor and rename feature column as feature name
    val postProcessedDF = postProcessingColumns.foldLeft(aggedDF)((inputDF, pr) => {
      val tempFeatureColumnName = "_temp_column_for_default_value_" + pr._1
      inputDF
        .withColumn(tempFeatureColumnName, pr._2)
        .drop(pr._1)
        .withColumnRenamed(tempFeatureColumnName, pr._1)
    })

    // 5. assemble return results
    val columnNamePairs = transformedResultWithKey.transformedResult.featureNameAndPrefixPairs
    val resultWithoutKey = TransformedResult(
      columnNamePairs,
      postProcessedDF,
      transformedResultWithKey.transformedResult.featureColumnFormats,
      transformedResultWithKey.transformedResult.inferredFeatureTypes)
    KeyedTransformedResult(transformedResultWithKey.joinKey, resultWithoutKey)
  }

  /**
   * transform requested features defined in an anchor
   * all the requested features will be evaluated in sequential order
   *
   * @param featureAnchorWithSource featureAnchorWithSource to evaluate
   * @param df dataframe to apply feature transformations
   * @param requestedFeatureRefString features to transform
   * @return map <feature names, feature column name prefix> sequence to its transformed dataframe,
   *         the feature names does not include the feature column name prefix, however, the corresponding
   *         feature column in the dataframe includes the prefix. E.g, if feature is A, prefix is pre_, then
   *         the feature column in the dataframe should be pre_A
   */
  def transformSingleAnchorDF(
      featureAnchorWithSource: FeatureAnchorWithSource,
      df: DataFrame,
      requestedFeatureRefString: Seq[String],
      inputDateInterval: Option[DateTimeInterval],
      mvelContext: Option[FeathrExpressionExecutionContext]): TransformedResult = {
    val featureNamePrefix = getFeatureNamePrefix(featureAnchorWithSource.featureAnchor.extractor)
    val featureNamePrefixPairs = requestedFeatureRefString.map((_, featureNamePrefix))

    // return the feature dataframe, the feature column format and the actual(inferred or user provided) feature types
    val featureTypeConfigs = featureAnchorWithSource.featureAnchor.featureTypeConfigs
    val transformedFeatureData: TransformedResult = featureAnchorWithSource.featureAnchor.extractor match {
      case transformer: TimeWindowConfigurableAnchorExtractor =>
        WindowAggregationEvaluator.transform(transformer, df, featureNamePrefixPairs, featureAnchorWithSource, inputDateInterval)
      case transformer: SimpleAnchorExtractorSpark =>
        // transform from avro tensor to FDS format, avro tensor can be shared by online/offline
        // so that transformation logic can be written only once
        DataFrameBasedSqlEvaluator.transform(transformer, df, featureNamePrefixPairs, featureTypeConfigs)
      case transformer: AnchorExtractor[_] =>
        DataFrameBasedRowEvaluator.transform(transformer, df, featureNamePrefixPairs, featureTypeConfigs, mvelContext)
      case _ =>
        throw new FeathrFeatureTransformationException(ErrorLabel.FEATHR_USER_ERROR, s"cannot find valid Transformer for ${featureAnchorWithSource}")
    }
    // Check for whether there are duplicate columns in the transformed DataFrame, typically this is because
    // the feature name is the same as some field name
    val withFeatureDF = transformedFeatureData.df
    if (withFeatureDF.columns.distinct.length != withFeatureDF.columns.length) {
      throw new FeathrException(
        ErrorLabel.FEATHR_USER_ERROR,
        "Found duplicate column names in the transformed feature " +
          s"DataFrame, all columns are ${withFeatureDF.columns.mkString(",")}. Please make sure your feature names are different from field " +
          "names in the source data")
    }
    transformedFeatureData
  }

  /**
   * get all feature definitions given the feature anchor extractor. Currently only TimeWindowConfigurableAnchorExtractor is supported
   * @param featureAnchorExtractor the feature anchor extractor
   * @return map of feature name to feature definition
   */
  def getFeatureDefinitions(featureAnchorExtractor: AnyRef): Map[String, TimeWindowFeatureDefinition] = {
    featureAnchorExtractor match {
      case extractor: TimeWindowConfigurableAnchorExtractor =>
        extractor.features
      case _ =>
        throw new FeathrFeatureTransformationException(
          ErrorLabel.FEATHR_USER_ERROR,
          "Only TimeWindowConfigurableAnchorExtractor is supported for this function for now")
    }
  }

  /**
   * get the aggregation window given the anchor group
   * @param anchorFeatureGroups anchors defined on the same source view and window
   * @return aggregation window
   */
  def getFeatureAggWindow(anchorFeatureGroups: AnchorFeatureGroups): Int = {
    val requestedFeatureNames = anchorFeatureGroups.requestedFeatures
    val features = getFeatureDefinitions(anchorFeatureGroups.anchorsWithSameSource.head.featureAnchor.extractor)
    val requestedFeatures = features.filter(x => requestedFeatureNames.contains(x._1))
    requestedFeatures.head._2.window.toDays.toInt
  }

  /**
   * preAggWithoutOldDelta +/- newDeltaAgg, e.g add newDeltaAgg to preAggWithoutOldDelta,
   * or  substract oldDeltaAgg from preAgg
   * @param oldDeltaDF previous aggregation dataframe
   * @param newDeltaDF new delta window aggregation dataframe
   * @param oldDeltaJoinKeys the join keys of old delta
   * @param newDeltaJoinKeys the join keys of new delta
   * @param requestedFeatureColumnNames the requested features to be updated
   * @return
   */
  def mergeDeltaDF(
      oldDeltaDF: DataFrame,
      newDeltaDF: DataFrame,
      oldDeltaJoinKeys: Seq[String],
      newDeltaJoinKeys: Seq[String],
      requestedFeatureColumnNames: Seq[String],
      add: Boolean = true): DataFrame = {
    val tmpPostfix = "_tmp"
    val oldDelta = "oldDelta"
    val newDelta = "newDelta"
    // Renaming the join key column names to make sure both DataFrames have the same names,
    // this will avoid duplicate join key columns in the joined DataFrame.
    // It is safe to do so because the join key columns will be renamed to key0, key1, key2, etc in the end.
    val oldDeltaRenamed =
      oldDeltaJoinKeys.zip(newDeltaJoinKeys).foldLeft(oldDeltaDF)((baseDF, renamePair) => baseDF.withColumnRenamed(renamePair._1, renamePair._2))

    val joinedWithNewDelta = oldDeltaRenamed.as(oldDelta).join(newDeltaDF.as(newDelta), newDeltaJoinKeys, "outer")
    requestedFeatureColumnNames.foldLeft(joinedWithNewDelta)((baseDf, columnName) => {
      val column = if (add) {
        baseDf(s"${oldDelta}." + columnName) + baseDf(s"${newDelta}." + columnName)
      } else {
        baseDf(s"${oldDelta}." + columnName) - baseDf(s"${newDelta}." + columnName)
      }
      val mergedWithNewDelta = baseDf
        .withColumn(
          columnName + tmpPostfix,
          when(baseDf(s"${oldDelta}." + columnName).isNull, baseDf(s"${newDelta}." + columnName))
            .when(baseDf(s"${newDelta}." + columnName).isNull, baseDf(s"${oldDelta}." + columnName))
            .otherwise(column))
        .drop(baseDf(s"${oldDelta}." + columnName))
        .drop(baseDf(s"${newDelta}." + columnName))
      mergedWithNewDelta.withColumnRenamed(columnName + tmpPostfix, columnName)
    })
  }

  /**
   * direct calculate features for a anchor group, comparing to incremental calculate which relies on the previous results
   * @param anchorFeatureGroup a group of anchored features to evaluate that share same source view and aggregation
   *                           window (if aggregation is defined)
   * @param source data source to evaluate
   * @param keyExtractor key extractor of the anchors, all anchors should share same key extractor
   * @param bloomFilter bloomfilter to apply on source view
   * @param inputDateInterval the date parameters passed to the source
   * @param preprocessedDf the preprocessed DataFrame for this anchorFeatureGroup. It will replace the DataFrame loaded
   *                       by the source if it exists.
   * @return a TransformedResultWithKey which contains the dataframe and other info such as feature column, keys
   */
  def directCalculate(
      anchorFeatureGroup: AnchorFeatureGroups,
      source: DataSourceAccessor,
      keyExtractor: SourceKeyExtractor,
      bloomFilter: Option[BloomFilter],
      inputDateInterval: Option[DateTimeInterval],
      preprocessedDf: Option[DataFrame] = None,
      mvelContext: Option[FeathrExpressionExecutionContext]): KeyedTransformedResult = {
    // Can two diff anchors have different keyExtractor?
    assert(anchorFeatureGroup.anchorsWithSameSource.map(_.dateParam).distinct.size == 1)
    val defaultInterval = anchorFeatureGroup.anchorsWithSameSource.head.dateParam.map(OfflineDateTimeUtils.createIntervalFromFeatureGenDateParam)
    val interval = inputDateInterval.orElse(defaultInterval)

    // If there are preprocessed DataFrame by users' Pyspark UDFs, just use the preprocessed DataFrame.
    val sourceDF: DataFrame = preprocessedDf match {
      case Some(existDf) => existDf
      case None => {
        source match {
          case timeBasedDataSourceAccessor: TimeBasedDataSourceAccessor => timeBasedDataSourceAccessor.get(interval)
          case nonTimeBasedDataSourceAccessor: NonTimeBasedDataSourceAccessor => nonTimeBasedDataSourceAccessor.get()
        }
      }
    }

    val withKeyColumnDF = keyExtractor.appendKeyColumns(sourceDF)

    val outputJoinKeyColumnNames = getFeatureKeyColumnNames(keyExtractor, withKeyColumnDF)
    val filteredFactData = applyBloomFilter((keyExtractor, withKeyColumnDF), bloomFilter)

    // 1. apply all transformations on the dataframe in sequential order
    // get the result, mainly just a map from <feature names, prefix> sequence to its transformed dataframe
    val transformedInfoWithoutKey = anchorFeatureGroup.anchorsWithSameSource
      .foldLeft(TransformedResult(Seq[(String, String)](), filteredFactData, Map.empty[String, FeatureColumnFormat], Map()))(
        (prevTransformedResult, featureAnchorWithSource) => {
          val requestedFeatures = featureAnchorWithSource.selectedFeatures
          val transformedResultWithoutKey =
            transformSingleAnchorDF(featureAnchorWithSource, prevTransformedResult.df, requestedFeatures, inputDateInterval, mvelContext)
          val namePrefixPairs = prevTransformedResult.featureNameAndPrefixPairs ++ transformedResultWithoutKey.featureNameAndPrefixPairs
          val columnNameToFeatureNameAndType = prevTransformedResult.inferredFeatureTypes ++ transformedResultWithoutKey.inferredFeatureTypes
          val featureColumnFormats = prevTransformedResult.featureColumnFormats ++ transformedResultWithoutKey.featureColumnFormats
          TransformedResult(namePrefixPairs, transformedResultWithoutKey.df, featureColumnFormats, columnNameToFeatureNameAndType)
        })
    val transformedInfoWithKey = KeyedTransformedResult(outputJoinKeyColumnNames, transformedInfoWithoutKey)
    // 2. apply aggregations sequentially for all the requested features
    val aggResults = applyAggregate(anchorFeatureGroup.anchorsWithSameSource, transformedInfoWithKey)

    val aggedDF = aggResults.transformedResult.df
    // 3. convert to FDS
    val allRequestedFeatures = anchorFeatureGroup.anchorsWithSameSource.map(_.selectedFeatures).reduce(_ ++ _)
    val userProvidedFeatureTypeConfigs = anchorFeatureGroup.anchorsWithSameSource.map(_.featureAnchor.featureTypeConfigs).reduce(_ ++ _)

    val (FeatureDataFrame(convertedDF, inferredFeatureTypes), featureFormat) =
      (convertTransformedDFToFDS(allRequestedFeatures, transformedInfoWithoutKey, aggedDF, userProvidedFeatureTypeConfigs), FeatureColumnFormat.FDS_TENSOR)

    // 4. rename the feature column to the standard column naming convention which has the namespace/version encoded
    val renamedDF = aggResults.transformedResult.featureNameAndPrefixPairs.foldLeft(convertedDF)((baseDF, columnWithName) => {
      val origFeatureColumnName = columnWithName._1
      baseDF.withColumnRenamed(origFeatureColumnName, columnWithName._2 + DataFrameColName.getEncodedFeatureRefStrForColName(columnWithName._1))
    })

    // 5. validate the result

    anchorFeatureGroup.anchorsWithSameSource.foreach { featureAnchorWithSource =>
      val featureColumnInfo = aggResults.transformedResult.featureNameAndPrefixPairs
      val requestedFeatureNames = anchorFeatureGroup.requestedFeatures
      val requestFeatures = featureAnchorWithSource.featureAnchor.getProvidedFeatureNames.filter(requestedFeatureNames.contains(_))
    }
    val featureColumnNames = aggResults.transformedResult.featureNameAndPrefixPairs.map(_._1)
    val featureColumnFormats = featureColumnNames.map(name => name -> featureFormat).toMap
    val result = TransformedResult(aggResults.transformedResult.featureNameAndPrefixPairs, renamedDF, featureColumnFormats, inferredFeatureTypes)
    KeyedTransformedResult(aggResults.joinKey, result)
  }

  /**
   * Convert the extractor returned dataframe to FDS tensors
   * This is used in auto-tensorizing features
   * @param allFeaturesToConvert all features to convert
   * @param transformedResult transformer returned result
   * @param withFeatureDF input dataframe with features
   * @param userProvidedFeatureTypeConfigs user provided feature types
   * @return a pair of (feature dataframe in FDS format, map from feature name to inferred feature type）
   */
  def convertTransformedDFToFDS(
      allFeaturesToConvert: Seq[String],
      transformedResult: TransformedResult,
      withFeatureDF: DataFrame,
      userProvidedFeatureTypeConfigs: Map[String, FeatureTypeConfig] = Map()): FeatureDataFrame = {
    // 1. infer the feature types if they are not done by the transformers above
    val defaultInferredFeatureTypes = inferFeatureTypesFromRawDF(withFeatureDF, allFeaturesToConvert)
    val transformedInferredFeatureTypes = defaultInferredFeatureTypes ++ transformedResult.inferredFeatureTypes
    val featureColNameToFeatureNameAndType =
      allFeaturesToConvert.map { featureName =>
        val userProvidedConfig = userProvidedFeatureTypeConfigs.getOrElse(featureName, FeatureTypeConfig.UNDEFINED_TYPE_CONFIG)
        val userProvidedFeatureType = userProvidedConfig.getFeatureType
        val processedFeatureTypeConfig = if (userProvidedFeatureType == FeatureTypes.UNSPECIFIED) {
          transformedInferredFeatureTypes.getOrElse(featureName, FeatureTypeConfig.UNDEFINED_TYPE_CONFIG)
        } else userProvidedConfig
        val colName = featureName
        (colName, (featureName, processedFeatureTypeConfig))
      }.toMap
    val inferredFeatureTypes = featureColNameToFeatureNameAndType.map {
      case (_, (featureName, featureType)) =>
        featureName -> featureType
    }

    // 2. convert to FDS
    val convertedDF = featureColNameToFeatureNameAndType
      .groupBy(pair => transformedResult.featureColumnFormats(pair._1))
      .foldLeft(withFeatureDF)((inputDF, featureColNameToFeatureNameAndTypeWithFormat) => {
        val fdsDF = featureColNameToFeatureNameAndTypeWithFormat._1 match {
          case FeatureColumnFormat.FDS_TENSOR =>
            inputDF
          case FeatureColumnFormat.RAW =>
            // sql extractor return rawDerivedFeatureEvaluator.scala (Diff rev
            val convertedDF = FeaturizedDatasetUtils.convertRawDFtoQuinceFDS(inputDF, featureColNameToFeatureNameAndType)
            convertedDF
        }
        fdsDF
      })
    offline.FeatureDataFrame(convertedDF, inferredFeatureTypes ++ transformedResult.inferredFeatureTypes)
  }

  /**
   * Group features based on 4 factors. Only if features have the same source key extractor, time window, source
   * and filters, can they be grouped into same dataframe for downstream processing.
   * @param preprocessingUniqueness When there is a preprocessing logic, the anchor should be unique as well. It's similar
   *                                to sourceKeyExtractor.
   */
  private[offline] case class FeatureGroupingCriteria(sourceKeyExtry: String, timeWindowId: String, source: DataSourceAccessor, filter: String, preprocessingUniqueness: String = "")
  /**
    * Case class to define tuple of feature anchor + grouping criteria + extractor class
    */
  private[offline] case class AnchorFeaturesWithGroupingCriteriaAndExtractorClass(
                                                                                   anchor: ((FeatureAnchorWithSource, DataSourceAccessor), FeatureGroupWithSameTimeWindow),
                                                                                   groupingCriteria: FeatureGroupingCriteria, extractorClass: Class[_ <: AnyRef])
  /*
   * a group of features with same time window defined on a source
   */
  private[offline] case class FeatureGroupWithSameTimeWindow(timeWindow: Option[DateParam], featureNames: Seq[FeatureName])

  /**
   * transform anchored features for one join stage
   *
   * @param anchorToSourceDFThisStage  map of anchor with source to its (dataframe, rdd[SpecificRecordBase]) pair, either the
   *                                   dataframe or the rdd will be used to evaluate, the rdd is used to special handle old
   *                                   customized anchor extractor whose getFeature(datum) function expects a subclass of
   *                                   avro SpecificRecord, and we cannot convert the dataframe to the required rdd[SpecificRecord]
   *                                   efficiently.
   * @param requestedFeatureNames      anchored feature names this stage
   * @param bloomFilter                the bloomfilter for current join stage
   * @param incrementalAggContext      Optional parameter which is filled by feature generation. This parameter carries
   *                                   information necessary to perform incremental aggregation.
   * @return map of feature name to its transformed dataframe and the join key of the dataframe,
   *         each column name is generated by [[DataFrameColName.genFeatureColumnName(feature name)]]
   */
  def transformFeatures(
      anchorToSourceDFThisStage: Map[FeatureAnchorWithSource, DataSourceAccessor],
      requestedFeatureNames: Seq[FeatureName],
      bloomFilter: Option[BloomFilter],
      incrementalAggContext: Option[IncrementalAggContext] = None,
      mvelContext: Option[FeathrExpressionExecutionContext]): Map[FeatureName, KeyedTransformedResult] = {
    val executionService = Executors.newFixedThreadPool(MAX_PARALLEL_FEATURE_GROUP)
    implicit val executionContext = ExecutionContext.fromExecutorService(executionService)
    val groupedAnchorToFeatureGroups: Map[FeatureGroupingCriteria, Map[FeatureAnchorWithSource, FeatureGroupWithSameTimeWindow]] =
      groupFeatures(anchorToSourceDFThisStage, requestedFeatureNames.toSet)
    val futures = groupedAnchorToFeatureGroups.map {
      case (featureGroupingFactors, anchorsWithSameSource) =>
        // use future to submit spark job asynchronously so that these feature groups can be evaluated in parallel
        Future {
          // evaluate each group of feature, each group of features are defined on same dataframe
          // we already group by (keyExtractor, dataframe/rdd, timeWindow), so anchorsWithSameSource contains all anchors
          // defined on this dataframe/rdd with same set of keys.
          // hence we can just take any(e.g, the first) extractor (in anchorsWithSameSource.head) to get the join keys to apply
          // bloomfilter and get key column
          val keyExtractor = anchorsWithSameSource.head._1.featureAnchor.sourceKeyExtractor
          val featureAnchorWithSource = anchorsWithSameSource.keys.toSeq
          val selectedFeatures = anchorsWithSameSource.flatMap(_._2.featureNames).toSeq
          val isAvroRddBasedExtractor = featureAnchorWithSource
            .map(_.featureAnchor.extractor)
            .filter(extractor => extractor.isInstanceOf[CanConvertToAvroRDD]
          ).nonEmpty
          val transformedResults: Seq[KeyedTransformedResult] = if (isAvroRddBasedExtractor) {
              // If there are features are defined using AVRO record based extractor, run RDD based feature transformation
              val sourceAccessor = featureGroupingFactors.source
              val sourceRdd = sourceAccessor.asInstanceOf[NonTimeBasedDataSourceAccessor].get()
              val featureTypeConfigs = featureAnchorWithSource.flatMap(featureAnchor => featureAnchor.featureAnchor.featureTypeConfigs).toMap
              Seq(transformFeaturesOnAvroRecord(sourceRdd, keyExtractor, featureAnchorWithSource, bloomFilter, selectedFeatures, featureTypeConfigs))
            } else {
              val sourceDF = featureGroupingFactors.source
              transformFeaturesOnDataFrameRow(sourceDF,
                keyExtractor, featureAnchorWithSource, bloomFilter, selectedFeatures, incrementalAggContext, mvelContext)
            }

          val res = transformedResults
            .map { transformedResultWithKey =>
              // rename feature column names from feature names to prefixed/standard form,
              // so we can avoid feathr name conflicting with data fields
              val transformedDF = transformedResultWithKey.transformedResult.df
              val outputJoinKeyColumnNames = transformedResultWithKey.joinKey
              val featureNamePrefixPair = transformedResultWithKey.transformedResult.featureNameAndPrefixPairs
              val rightJoinKeyPrefix = featureGroupingFactors.sourceKeyExtry.replaceAll("[^\\w]", "") + "_"

              // keep only key columns and feature column in the output column for feature join
              val joinKeyColumnToNewColName = outputJoinKeyColumnNames.map(keyCol => {
                // dataframe column should not have special characters
                val cleanedJoinKeyColumn = (rightJoinKeyPrefix + keyCol).replaceAll("[^\\w]", "_")
                (transformedDF(keyCol), cleanedJoinKeyColumn)
              })
              val joinKeyColumnNames = joinKeyColumnToNewColName.map(_._2)
              val featureColumnToNewColumnName = featureNamePrefixPair.map { featurePair =>
                val featureRefStrInDF = DataFrameColName.getEncodedFeatureRefStrForColName(featurePair._1)
                (transformedDF(featurePair._2 + featureRefStrInDF), DataFrameColName.genFeatureColumnName(featureRefStrInDF))
              }
              val featureNames = featureNamePrefixPair.map(_._1)
              if (featureColumnToNewColumnName.map(_._2).distinct.size != featureColumnToNewColumnName.map(_._2).size) {
                throw new FeathrFeatureTransformationException(
                  ErrorLabel.FEATHR_USER_ERROR,
                  s"Fatal internal error, ${featureColumnToNewColumnName} should be distinct!")
              }
              // only preserve join key columns and feature data column in the output
              val selectedDF = transformedDF.select((joinKeyColumnToNewColName ++ featureColumnToNewColumnName)
                .map { case (column, newColumnName) => column.alias(newColumnName) }: _*)
              // outputJoinKeyColumnNames are the same for the dataframe in one stage, as they have same join key
              val updatedTransformedResult = transformedResultWithKey.transformedResult.copy(df = selectedDF)
              val updatedKeyedTransformedResult = KeyedTransformedResult(joinKeyColumnNames, updatedTransformedResult)
              featureNames.map(featureName => (featureName, updatedKeyedTransformedResult))
            }
            .reduce(_ ++ _)
          val computedFeatures = res.map(_._1)
          if (computedFeatures.distinct.size != computedFeatures.size) {
            throw new FeathrFeatureTransformationException(
              ErrorLabel.FEATHR_ERROR,
              s"Internal error: ${computedFeatures} should be not have duplicate features, " +
                s"this means some features are computed multiple times, current anchors: ${featureAnchorWithSource}")
          }
          res.toMap
        }
    }
    futures.map(k => Await.result(k, Duration.Inf)).reduce(_ ++ _)
  }

  /**
   * Rename transformed feature column name by adding tag information to the feature column names and remove unneeded columns
   * @param contextDF dataframe to process, in feature join case, it is the joined table, in feature generation, it is the
   *                  transformed feature dataframe
   * @param columnsToKeep  columns to keep, e.g. in a feature join case, left table (observation) columns should be kept, in feature
   *                       generation, key column should be kept
   * @param featuresToRename e.g. feature names to rename (adding tag information to it)
   * @param columnsToProcess column to process(rename/remove). e.g. in feature join it is the right table (feature data) columns,
   *                         in feature generation, it is the all the columns from transformed feature data, i.e., they
   *                         must include all featuresToRename
   * @param tagsInfo key tags to add to the processed column names
   * @return processed dataframe (rename some column, remove some columns)
   */
  def pruneAndRenameColumnWithTags(
      contextDF: DataFrame,
      columnsToKeep: Seq[String],
      featuresToRename: Seq[String],
      columnsToProcess: Seq[String],
      tagsInfo: List[String]): DataFrame = {
    // in feature join case, only keep the feature columns from the right table, the other columns are not needed any more
    // as derived feature will only depends on feature columns, and we need to rename the feature columns after the join,
    // since now we know the tag of them and we need tag info to calculate derived features

    // in feature generation case, we still need to add the tag information to the feature columns(rename), and remove the
    // features that are not requested
    val featureColumnsToRename = featuresToRename.map(DataFrameColName.genFeatureColumnName(_)).toSet
    logger.trace(s"featureColumnsToRename = $featureColumnsToRename")

    val featureColumnsRenameMap = columnsToProcess
      .filter(featureColumnsToRename.contains(_))
      .map(featureName => (contextDF.col(featureName), DataFrameColName.genFeatureColumnName(featureName, Some(tagsInfo))))
    logger.trace(s"featureColumnsRenameMap = $featureColumnsRenameMap")

    val columnsToKeepRenameMap = columnsToKeep.map(colName => (contextDF.col(colName), colName))
    logger.trace(s"columnsToKeepRenameMap = $columnsToKeepRenameMap")

    val reservedColumnPairs = columnsToKeepRenameMap ++ featureColumnsRenameMap
    logger.trace(s"reservedColumnPairs = $reservedColumnPairs")

    contextDF.select(reservedColumnPairs.map { case (x, y) => x.alias(y) }: _*)
  }

  /**
   * Drops rows if it contains "null" or NaN for ALL specified columns.
   *
   * @param inputDF input DataFrame.
   * @param columnNames columns of the DataFrame to look for null values.
   * @return
   */
  def dropIfNullValuesForAllColumns(inputDF: DataFrame, columnNames: Seq[String]): DataFrame =
    inputDF.na.drop("all", columnNames)

  /**
   * Get the feature type inference context
   * @param ss spark session
   * @param featureTypes initial feature types, typically provided by users in their feature configs
   * @param featureRefStrs features to infer
   * @return the FeatureTypeInferenceContext
   */
  def getTypeInferenceContext(ss: SparkSession, featureTypes: Map[String, FeatureTypes], featureRefStrs: Seq[String]): FeatureTypeInferenceContext = {
    val featureTypeAccumulators = featureRefStrs.map {
      case featureRef =>
        val defaultType = if (featureTypes.contains(featureRef)) featureTypes(featureRef) else FeatureTypes.UNSPECIFIED
        val accumulator = new FeatureTypeAccumulator(defaultType)
        ss.sparkContext.register(accumulator, featureRef)
        featureRef -> accumulator
    }.toMap
    FeatureTypeInferenceContext(featureTypeAccumulators)
  }

  /**
   * Get the FDS format schema fields for transformed feature dataset
   * @param featureRefStrs features to process
   * @param featureTypeConfigs feature types
   * @param colNamePrefx feature column name prefix
   * @return list of field schema fields
   */
  def getFDSSchemaFields(
      featureRefStrs: Seq[String],
      featureTypeConfigs: Map[String, FeatureTypeConfig] = Map(),
      colNamePrefx: String = ""): Seq[StructField] = {
    val featureTensorTypeInfo = featureRefStrs.map {
      case featureRef =>
        val featureTypeConfig = featureTypeConfigs.getOrElse(featureRef, FeatureTypeConfig.UNDEFINED_TYPE_CONFIG)
        val tensorType = FeaturizedDatasetUtils.lookupTensorTypeForFeatureRef(featureRef, None, featureTypeConfig)
        val schemaType = tensorTypeToDataFrameSchema(tensorType)
        val columnName = colNamePrefx + featureRef
        val dType = StructField(columnName, schemaType)
        dType
    }
    featureTensorTypeInfo
  }

  /**
   * Infer feature types collected by the type accumulators
   * @param featureTypeAccumulators feature type accumulators
   * @param transformedRdd transformed feature dataset
   * @param featureRefStrs features to process
   * @return inferred feature types
   */
  def inferFeatureTypes(
      featureTypeAccumulators: Map[String, FeatureTypeAccumulator],
      transformedRdd: RDD[Row],
      featureRefStrs: Seq[String]): Map[String, FeatureTypes] = {
    val featureNum = featureTypeAccumulators.size
    // Infer each feature type by triggering an action on the RDD
    // We use .take(1) action to trigger an eager computation of the feature
    // In some cases, there might be multiple features taking relatively long to run for type inference.
    // Since we are just looking for the first non-null feature value and its computation is not intensive
    // (compared to a transformation on the full dataset), we can parallelize them to speed up the whole type inference process.
    (1 to featureNum).par.map { idx =>
      transformedRdd
      // Make sure we have at least one non-null row for the feature, since for null rows, we cannot infer the type
        .filter(r => r.get(r.size - idx) != null)
        // Fast Take() action would trigger the computation of the RDD, where it will populate the featureTypeAccumulators
        .take(1)
    }
    val actualFeatureTypes = featureTypeAccumulators.mapValues(featureTypeAcc => {
      featureTypeAcc.value
    })
    actualFeatureTypes
  }

  /**
   * Apply a bloomfilter to a dataframe
   *
   * @param sourceExtractorWithDF
   * @param bloomFilter
   * @return
   */
  private def applyBloomFilter(sourceExtractorWithDF: (SourceKeyExtractor, DataFrame), bloomFilter: Option[BloomFilter]): DataFrame = {
    bloomFilter match {
      case None =>
        // no bloom filter, use data as it
        sourceExtractorWithDF._2
      case Some(filter) =>
        // get the list of join key columns or expression
        val keyColumnsList = sourceExtractorWithDF._1 match {
          case extractor if extractor.isInstanceOf[MVELSourceKeyExtractor] =>
            val mvelSourceKeyExtractor = extractor.asInstanceOf[MVELSourceKeyExtractor]
            if (sourceExtractorWithDF._2.head(1).isEmpty) {
              mvelSourceKeyExtractor.getKeyColumnNames(None)
            } else {
              mvelSourceKeyExtractor.getKeyColumnNames(Some(sourceExtractorWithDF._2.first))
            }
          case extractor: SourceKeyExtractor => extractor.getKeyColumnNames()
          case _ => throw new FeathrFeatureTransformationException(ErrorLabel.FEATHR_USER_ERROR, "No source key extractor found")
        }
        if (!keyColumnsList.isEmpty) {
          // generate the same concatenated-key column for bloom filtering
          val (bfFactKeyColName, factDataWithKeys) =
            DataFrameKeyCombiner().combine(sourceExtractorWithDF._2, keyColumnsList)
          // use bloom filter on generated concat-key column
          val filtered = factDataWithKeys.filter(SlidingWindowFeatureUtils.mightContain(filter)(col(bfFactKeyColName)))
          // remove the concat-key column
          filtered.drop(col(bfFactKeyColName))
        } else {
          // expand feature for seq join does not have right key, so we allow empty here
          sourceExtractorWithDF._2
        }
    }
  }


  /**
   * Apply a bloomfilter to a RDD
   *
   * @param keyExtractor key extractor to extract the key values from the RDD
   * @param rdd RDD to filter
   * @param bloomFilter bloomfilter used to filter out unwanted row in the RDD based on key columns
   * @return filtered RDD
   */

  private def applyBloomFilterRdd(keyExtractor: SourceKeyExtractor, rdd: RDD[IndexedRecord], bloomFilter: Option[BloomFilter]): RDD[IndexedRecord] = {
    bloomFilter match {
      case None =>
        // no bloom filter, use data as it
        rdd
      case Some(filter) =>
        // get the list of join key columns or expression
        keyExtractor match {
          case extractor: MVELSourceKeyExtractor =>
            // get the list of join key columns or expression
            val keyColumnsList = if (rdd.isEmpty) {
              extractor.getKeyColumnNames(None)
            } else {
              extractor.getKeyColumnNames(Some(rdd.first))
            }
            if (!keyColumnsList.isEmpty) {
              val filtered = rdd.filter { record: Any =>
                val keyVals = extractor.getKey(record)
                // if key is not in observation, skip it
                if (keyVals != null && keyVals.count(_ == null) == 0) {
                  filter.mightContainString(SourceUtils.generateFilterKeyString(keyVals))
                } else {
                  false
                }
              }
              filtered
            } else {
              // expand feature for seq join does not have right key, so we allow empty here
              rdd
            }
          case _ => throw new FeathrFeatureTransformationException(ErrorLabel.FEATHR_USER_ERROR, "No source key extractor found")
        }
    }
  }

  /**
   * Transform features defined in a group of anchors based on same source
   * This is for the AVRO record based extractors
   *
   * @param rdd source that requested features are defined on
   * @param keyExtractor key extractor to apply on source rdd
   * @param featureAnchorWithSources feature anchors defined on source rdd to be evaluated
   * @param bloomFilter bloomfilter to apply on source rdd
   * @param requestedFeatureNames requested features
   * @param featureTypeConfigs user specified feature types
   * @return TransformedResultWithKey The output feature DataFrame conforms to FDS format
   */
  private def transformFeaturesOnAvroRecord(df: DataFrame,
                                            keyExtractor: SourceKeyExtractor,
                                            featureAnchorWithSources: Seq[FeatureAnchorWithSource],
                                            bloomFilter: Option[BloomFilter],
                                            requestedFeatureNames: Seq[FeatureName],
                                            featureTypeConfigs: Map[String, FeatureTypeConfig] = Map()): KeyedTransformedResult = {
    if (!keyExtractor.isInstanceOf[MVELSourceKeyExtractor]) {
      throw new FeathrException(ErrorLabel.FEATHR_ERROR, s"Error processing requested Feature :${requestedFeatureNames}. " +
        s"Key extractor ${keyExtractor} must extends MVELSourceKeyExtractor.")
    }
    val extractor = keyExtractor.asInstanceOf[MVELSourceKeyExtractor]
    if (!extractor.anchorExtractorV1.isInstanceOf[CanConvertToAvroRDD]) {
      throw new FeathrException(ErrorLabel.FEATHR_ERROR, s"Error processing requested Feature :${requestedFeatureNames}. " +
        s"isLowLevelRddExtractor() should return true and convertToAvroRdd should be implemented.")
    }
    val rdd = extractor.anchorExtractorV1.asInstanceOf[CanConvertToAvroRDD].convertToAvroRdd(df)
    val filteredFactData = applyBloomFilterRdd(keyExtractor, rdd, bloomFilter)

    // Build a sequence of 3-tuple of (FeatureAnchorWithSource, featureNamePrefixPairs, AnchorExtractorBase)
    val transformInfo = featureAnchorWithSources map { featureAnchorWithSource =>
      val extractor = featureAnchorWithSource.featureAnchor.extractor
      extractor match {
        case transformer: AnchorExtractorBase[IndexedRecord] =>
          // We no longer need prefix for the simplicity of the implementation, instead if there's a feature name
          // and source data field clash, we will throw exception and ask user to rename the feature.
          val featureNamePrefix = ""
          val featureNames = featureAnchorWithSource.selectedFeatures.filter(requestedFeatureNames.contains)
          val featureNamePrefixPairs = featureNames.map((_, featureNamePrefix))
          TransformInfo(featureAnchorWithSource, featureNamePrefixPairs, transformer)

        case _ =>
          throw new FeathrFeatureTransformationException(ErrorLabel.FEATHR_USER_ERROR, s"Unsupported transformer $extractor for features: $requestedFeatureNames")
      }
    }

    // to avoid name conflict between feature names and the raw data field names
    val sourceKeyExtractors = transformInfo.map(_.featureAnchorWithSource.featureAnchor.sourceKeyExtractor)
    assert(sourceKeyExtractors.map(_.toString).distinct.size == 1)

    val transformers = transformInfo map (_.transformer)

    /*
     * Transform the given RDD by applying extractors to each row to create an RDD[Row] where each Row
     * represents keys and feature values
     */
    val spark = SparkSession.builder().getOrCreate()
    val userProvidedFeatureTypes = transformInfo.flatMap(_.featureAnchorWithSource.featureAnchor.getFeatureTypes.getOrElse(Map.empty[String, FeatureTypes])).toMap
    val FeatureTypeInferenceContext(featureTypeAccumulators) =
      FeatureTransformation.getTypeInferenceContext(spark, userProvidedFeatureTypes, requestedFeatureNames)
    val transformedRdd = filteredFactData map { record =>
      val (keys, featureValuesWithType) = transformAvroRecord(requestedFeatureNames, sourceKeyExtractors, transformers, record, featureTypeConfigs)
      requestedFeatureNames.zip(featureValuesWithType).foreach {
        case (featureRef, (_, featureType)) =>
          if (featureTypeAccumulators(featureRef).isZero && featureType != null) {
            // This is lazy evaluated
            featureTypeAccumulators(featureRef).add(FeatureTypes.valueOf(featureType.getBasicType.toString))
          }
      }
      // Create a row by merging a row created from keys and a row created from term-vectors/tensors
      Row.merge(Row.fromSeq(keys), Row.fromSeq(featureValuesWithType.map(_._1)))
    }

    // Create a DataFrame from the above obtained RDD
    val keyNames = getFeatureKeyColumnNamesRdd(sourceKeyExtractors.head, filteredFactData)
    val (outputSchema, inferredFeatureTypeConfigs) = {
      val allFeatureTypeConfigs = featureAnchorWithSources.flatMap(featureAnchorWithSource => featureAnchorWithSource.featureAnchor.featureTypeConfigs).toMap
      val inferredFeatureTypes = inferFeatureTypes(featureTypeAccumulators, transformedRdd, requestedFeatureNames)
      val inferredFeatureTypeConfigs = inferredFeatureTypes.map(x => x._1 -> new FeatureTypeConfig(x._2))
      val mergedFeatureTypeConfig = inferredFeatureTypeConfigs ++ allFeatureTypeConfigs
      val colPrefix = ""
      val featureTensorTypeInfo = getFDSSchemaFields(requestedFeatureNames, mergedFeatureTypeConfig, colPrefix)
      val structFields = keyNames.foldRight(List.empty[StructField]) {
        case (colName, acc) =>
          StructField(colName, StringType) :: acc
      }
      val outputSchema = StructType(StructType(structFields ++ featureTensorTypeInfo))
      (outputSchema, mergedFeatureTypeConfig)
    }
    val transformedDF = spark.createDataFrame(transformedRdd, outputSchema)

    val featureFormat = FeatureColumnFormat.FDS_TENSOR
    val featureColumnFormats = requestedFeatureNames.map(name => name -> featureFormat).toMap
    val transformedInfo = TransformedResult(transformInfo.flatMap(_.featureNamePrefixPairs), transformedDF, featureColumnFormats, inferredFeatureTypeConfigs)
    KeyedTransformedResult(keyNames, transformedInfo)
  }

  /**
   * Apply a keyExtractor and feature transformer on a Record to extractor feature values.
   * @param requestedFeatureNames requested feature names in the output. Extractors may produce more features than requested.
   * @param sourceKeyExtractors extractor to extract the key from the record
   * @param transformers transform to produce the feature value from the record
   * @param record avro record to work on
   * @param featureTypeConfigs user defined feature types
   * @return tuple of (feature join key, sequence of (feature value, feature type) in the order of requestedFeatureNames)
   */
  private def transformAvroRecord(
                                   requestedFeatureNames: Seq[FeatureName],
                                   sourceKeyExtractors: Seq[SourceKeyExtractor],
                                   transformers: Seq[AnchorExtractorBase[IndexedRecord]],
                                   record: IndexedRecord,
                                   featureTypeConfigs: Map[String, FeatureTypeConfig] = Map()): (Seq[String], Seq[(Any, FeatureType)]) = {
    val keys = sourceKeyExtractors.head match {
      case mvelSourceKeyExtractor: MVELSourceKeyExtractor => mvelSourceKeyExtractor.getKey(record)
      case _ => throw new FeathrFeatureTransformationException(ErrorLabel.FEATHR_USER_ERROR, s"${sourceKeyExtractors.head} is not a valid extractor on RDD")
    }

    /*
     * For the given row, apply all extractors to extract feature values. If requested as tensors, each feature value
     * contains a tensor else a term-vector.
     */
    val features = transformers map {
      case extractor: AnchorExtractor[IndexedRecord] =>
        val features = extractor.getFeatures(record)
        print(features)
        FeatureValueTypeValidator.validate(features, featureTypeConfigs)
        features
      case extractor =>
        throw new FeathrFeatureTransformationException(
          ErrorLabel.FEATHR_USER_ERROR,
          s"Invalid extractor $extractor for features:" +
            s"$requestedFeatureNames requested as tensors")
    } reduce (_ ++ _)
    if (logger.isTraceEnabled) {
      logger.trace(s"Extracted features: $features")
    }

    /*
     * Retain feature values for only the requested features, and represent each feature value as
     * a tensor, as specified.
     */
    val featureValuesWithType = requestedFeatureNames map { name =>
      features.get(name) map {
        case featureValue =>
          val tensorData: TensorData = featureValue.getAsTensorData()
          val featureType: FeatureType = featureValue.getFeatureType()
          val row = FeaturizedDatasetUtils.tensorToFDSDataFrameRow(tensorData)
          (row, featureType)
      } getOrElse ((null, null)) // return null if no feature value present
    }
    (keys, featureValuesWithType)
  }

  /**
    * Helper function to be used by groupFeatures. Given a collection of feature anchors which also contains information about grouping
    * criteria and extractor type per feature anchor, returns a map of FeatureGroupingCriteria to
    * Map[FeatureAnchorWithSource, FeatureGroupWithSameTimeWindow]]. Key point here for performance optimization is that
    * within each FeatureGroupingCriteria key, the corresponding value Map has one FeatureAnchorWithSource per extractor type.
    * This is important because for MVEL extractors we need to combine all anchor extractors into a single extractor in order to optimize
    * performance so that we don't have to do multiple MVEL transformation operations sequentially. Note that this function also
    * will clean up the input so that the output is in the format that groupFeatures expects.
    *
    * @param anchorsWithGroupingCriteria: Collection of tuples in the format of (featureAnchor, groupingCriteria, extractorClassType)
    * @return Map[FeatureGroupingCriteria, Map[FeatureAnchorWithSource, FeatureGroupWithSameTimeWindow]]
    */
  private def groupAndMergeAnchors(anchorsWithGroupingCriteria: List[AnchorFeaturesWithGroupingCriteriaAndExtractorClass]):
  Map[FeatureGroupingCriteria, Map[FeatureAnchorWithSource, FeatureGroupWithSameTimeWindow]] = {
    /* group by FeatureGroupingCriteria and extractor class.
     * We group by extractor class so we can combine all MVEL extractors into 1 extractor for performance purposes.
     */
    val groupedByCriteriaAndExtractorType = anchorsWithGroupingCriteria.groupBy(record => (record.groupingCriteria, record.extractorClass))
      .mapValues(groupedValues => {
        // Here we want to further group by extractor type so we can combine all MVEL extractors into 1 extractor for performance purposes.
        val representativeFeatureAnchorWithSource = groupedValues.head.anchor._1._1
        val representativeAnchor = representativeFeatureAnchorWithSource.featureAnchor
        val representativeFeatureGroup = groupedValues.head.anchor._2
        representativeAnchor.extractor match {
          case _: SimpleConfigurableAnchorExtractor =>
            // Here we combine the different values of the MVEL extractor into one extractor
            // and return a Map[FeatureAnchorWithSource, FeatureGroupWithSameTimeWindow] of size 1
            val combinedAnchorDefinitions = groupedValues.foldLeft(Map.empty[String, MVELFeatureDefinition])((result, kv) =>
              result ++ kv.anchor._1._1.featureAnchor.getAsAnchorExtractor.asInstanceOf[SimpleConfigurableAnchorExtractor].getFeaturesDefinitions)
            val keyForAllFeatures = representativeAnchor.getAsAnchorExtractor.asInstanceOf[SimpleConfigurableAnchorExtractor].getKeyExpression()
            val combinedAnchorExtractor = new SimpleConfigurableAnchorExtractor(keyForAllFeatures, combinedAnchorDefinitions)
            val combinedDefaults = groupedValues.foldLeft(Map.empty[String, common.FeatureValue])((result, kv) =>
              result ++ kv.anchor._1._1.featureAnchor.defaults)
            val combinedFeatureTypeConfigs = groupedValues.foldLeft(Map.empty[String, FeatureTypeConfig])((result, kv) =>
              result ++ kv.anchor._1._1.featureAnchor.featureTypeConfigs)
            val combinedAnchorFeatures = groupedValues.foldLeft(Set.empty[String])((result, kv) =>
              result ++ kv.anchor._1._1.featureAnchor.features)
            val combinedFeatureAnchor = FeatureAnchor(representativeAnchor.sourceIdentifier,
              combinedAnchorExtractor,
              combinedDefaults,
              representativeAnchor.lateralViewParams,
              representativeAnchor.sourceKeyExtractor,
              combinedAnchorFeatures,
              combinedFeatureTypeConfigs)
            val combinedSelectedFeatures = groupedValues.foldLeft(Seq.empty[String])((result, kv) => result ++ kv.anchor._1._1.selectedFeatures).distinct
            val combinedFeatureAnchorWithSource = FeatureAnchorWithSource(combinedFeatureAnchor,
              representativeFeatureAnchorWithSource.source,
              representativeFeatureAnchorWithSource.dateParam,
              Some(combinedSelectedFeatures))
            val groupedFeatureGroup = FeatureGroupWithSameTimeWindow(representativeFeatureGroup.timeWindow, combinedSelectedFeatures)
            Map((combinedFeatureAnchorWithSource, groupedFeatureGroup))
          // Other extractors don't need to be grouped since they can be optimized even if we call the transform sequentially, we just need
          // to group for MVEL extractors to optimize performance. In these cases here we just clean up the input and return.
          case _ @ (_: TimeWindowConfigurableAnchorExtractor |
                    _: SimpleAnchorExtractorSpark |
                    _: SQLConfigurableAnchorExtractor |
                    _: AnchorExtractor[_]) => groupedValues
            .foldLeft(Map.empty[FeatureAnchorWithSource, FeatureGroupWithSameTimeWindow])((result, kv) => result ++ Map((kv.anchor._1._1, kv.anchor._2)))
          case _ => throw new FeathrFeatureTransformationException(ErrorLabel.FEATHR_USER_ERROR,
            s"cannot find valid Transformer for ${representativeFeatureAnchorWithSource}")
        }
      })
    // Merge the anchor maps of all anchors with same grouping criteria and drop the extractor class used for grouping purposes.
    groupedByCriteriaAndExtractorType.groupBy(_._1._1).mapValues(groupedByCriteria =>
      groupedByCriteria.foldLeft(Map.empty[FeatureAnchorWithSource, FeatureGroupWithSameTimeWindow])((result, kv) =>
        result ++ kv._2
      ))
  }
  /**
   * group anchorToSourceDFThisStage into groups, all features within same group will be evaluated against one dataframe/rdd in the future

   *
   * @param anchorToSourceDFThisStage map anchor with time info to its source, all anchors must belong to the same join stage,
   *                                  and one anchor in the feathr feature config may appears multiple times in the key of
   *                                  this map, each with a different dataTime a, as features may have different time
   *                                  window although share same anchor config
   * @return map from group factors to a group of anchors along with request feature defined on it
   */
  private[offline] def groupFeatures(
      anchorToSourceDFThisStage: Map[FeatureAnchorWithSource, DataSourceAccessor],
      requestedFeatureNames: Set[FeatureName]): Map[FeatureGroupingCriteria, Map[FeatureAnchorWithSource, FeatureGroupWithSameTimeWindow]] = {
    // Build a map of Anchor(with Source) to group of features that are defined in the anchor and share the same time window
    val anchorToFeatureGroups = anchorToSourceDFThisStage
      .flatMap(anchorWithSourceDF => {
        val anchor = anchorWithSourceDF._1
        // It's possible that only part of the features defined in an anchored are requested,
        // here we filter out the non-requested feature to avoid potential unnecessary feature computation.
        val selectedFeatureNames = anchor.selectedFeatures.filter(requestedFeatureNames.contains)
        val newAnchorWithSelectedFeatures = anchor.copy(selectedFeatureNames = Some(selectedFeatureNames))
        val anchorWithSource = (newAnchorWithSelectedFeatures, anchorWithSourceDF._2)
        val windowParamToFeatureName = selectedFeatureNames.map(f => (anchor.dateParam, f))
        // reduce by key (i.e. timeWindow)
        val windowToFeatureNames = windowParamToFeatureName.groupBy(_._1).mapValues(seq => seq.map(f => Seq(f._2)).reduce((a, b) => a ++ b))
        windowToFeatureNames.map {
          case (timeWindow, featureNames) =>
            (anchorWithSource, FeatureGroupWithSameTimeWindow(timeWindow, featureNames))
        }
      })

    // Within features that share the same time window, further group them based on grouping criteria (FeatureGroupingCriteria)
    val groupedByCriteriaAndExtractorType = anchorToFeatureGroups
      .flatMap(anchorToFeatureGroupEntry => {
        val ((anchorWithSourceDF, timeSeriesSource), featureGroupWithSameTimeWindow) = anchorToFeatureGroupEntry
        val windowParam = featureGroupWithSameTimeWindow.timeWindow
        val timeWindowGroupByIdentifier = windowParam.mkString("__feathr_time_window_groupby__")
        /*
         * A row-level extractor supports row level extractions and will not change the number of input rows.
         *
         * SimpleAnchorExtractorSpark applies transformation and appends transformed columns to input Dataframe.
         * Thus, SimpleAnchorExtractorSpark also does not change the number of input rows.
         *
         * So as long as keyExtractor and time window are the same, then should be grouped together.
         */
        val featureExtractor = anchorWithSourceDF.featureAnchor.extractor
        val sourcekeyExtractorIdentifier =
          if (featureExtractor.isInstanceOf[SimpleAnchorExtractorSpark] ||
              featureExtractor.isInstanceOf[AnchorExtractor[_]]) {
            anchorWithSourceDF.featureAnchor.sourceKeyExtractor.toString
            // identifier string which encodes the information of sourceKeyExtractor and time window info
          } else {
            // otherwise, it's column-wise extractor, e.g. GenericAnchorExtractorSpark, which may change the number of rows
            // of the input dataframe, we just return a random unique id so that each one goes to a separate group
            randomUUID.toString
          }
        /*
         * group the anchors so that anchors within one group can be evaluated with same input dataframe (to save number of joins) when:
         * 1. anchors share same input source path
         * 2. anchors share same sourceKeyExtractor (identified by .toString() result instead of object hashcode)
         * 3. the anchor extractor is SimpleAnchorExtractorSpark or AnchorExtracotr[_}, because GenericAnchorExtractorSpark
         * may not able to passthrough the rows/columns of the input dataframe to the output dataframe.
         * 4. Features with different filter specifications cannot be evaluated against one dataframe.
         * If 2 filters F1 and F2 are evaluated against single dataframe, rows that do not get through
         * the first filter F1 cannot be removed since the rows have to be passed but kept to pass through filter F2.
         *
         * Note: within each FeatureGroupingCriteria, we should group by and merge extractors of the same class.
         */
        // preprocessedDfMap is in the form of feature list of an anchor separated by comma to their preprocessed DataFrame.
        // We use feature list to denote if the anchor has corresponding preprocessing UDF.
        // For example, an anchor have f1, f2, f3, then corresponding preprocessedDfMap is Map("f1,f2,f3"-> df1)
        // (Anchor name is not chosen since it may be duplicated and not unique and stable.)
        // If this anchor has preprocessing UDF, then we need to make its FeatureGroupingCriteria unique so it won't be
        // merged by different anchor of same source. Otherwise our preprocessing UDF may impact anchors that dont'
        // need preprocessing.
        val preprocessedDfMap = PreprocessedDataFrameManager.preprocessedDfMap
        val featuresInAnchor = anchorWithSourceDF.featureAnchor.features.toList
        val sortedMkString = featuresInAnchor.sorted.mkString(",")
        val featureNames = if (preprocessedDfMap.contains(sortedMkString)) sortedMkString else ""
        featureGroupWithSameTimeWindow.featureNames
          .map(
            f =>
              (
                anchorToFeatureGroupEntry,
                FeatureGroupingCriteria(
                  sourcekeyExtractorIdentifier,
                  timeWindowGroupByIdentifier,
                  timeSeriesSource,
                  AnchorUtils.getFilterFromAnchor(anchorWithSourceDF, f).getOrElse(""),
                  featureNames
                ),
                featureExtractor.getClass))
      }).map(f => AnchorFeaturesWithGroupingCriteriaAndExtractorClass(f._1, f._2, f._3)).toList
    // After appending the grouping criteria and extractor class, group and merge the feature anchors.
    groupAndMergeAnchors(groupedByCriteriaAndExtractorType)
  }

  /**
   * transform all features defined on same dataframe (i.e. the source view which is defined by source + keyExtractor)
   *
   * @param source source to transform
   * @param keyExtractor key extractor to apply on source
   * @param anchorsWithSameSource anchors defined on the same source view
   * @param bloomFilter bloomfilter to applied on the source view
   * @param allRequestedFeatures requested features.
   * @param incrementalAggContext contextual information required to perform incremental aggregation.
   * @return transformed result, could be multiple dataframes, if some features are calcuated using incremental aggregation,
   *         others use direct aggregation
   *
   */
  private def transformFeaturesOnDataFrameRow(
      source: DataSourceAccessor,
      keyExtractor: SourceKeyExtractor,
      anchorsWithSameSource: Seq[FeatureAnchorWithSource],
      bloomFilter: Option[BloomFilter],
      allRequestedFeatures: Seq[String],
      incrementalAggContext: Option[IncrementalAggContext],
      mvelContext: Option[FeathrExpressionExecutionContext]): Seq[KeyedTransformedResult] = {

    // based on source and feature definition, divide features into direct transform and incremental
    // transform groups
    val (directTransformAnchorGroup, incrementalTransformAnchorGroup) =
      groupAggregationFeatures(source, AnchorFeatureGroups(anchorsWithSameSource, allRequestedFeatures), incrementalAggContext)

    val preprocessedDf = PreprocessedDataFrameManager.getPreprocessedDataframe(anchorsWithSameSource)

    val directTransformedResult =
      directTransformAnchorGroup.map(anchorGroup => Seq(directCalculate(anchorGroup, source, keyExtractor, bloomFilter, None, preprocessedDf, mvelContext)))

    val incrementalTransformedResult = incrementalTransformAnchorGroup.map { anchorGroup =>
      {
        val requestedFeatures = anchorGroup.requestedFeatures
        // At this point, the group aggregation features has grouped features under incremental calculate.
        // So we can safely assume incrementalAggContext is defined.
        val incrAggCtx = incrementalAggContext.get
        val preAggDFs = incrAggCtx.previousSnapshotMap.collect { case (featureName, df) if requestedFeatures.exists(df.columns.contains) => df }.toSeq.distinct
        // join each previous aggregation dataframe sequentially
        val groupKeys = getFeatureKeyColumnNames(keyExtractor, preAggDFs.head)
        val keyColumnNames = getStandardizedKeyNames(groupKeys.size)
        val firstPreAgg = preAggDFs.head
        val joinedPreAggDFs = preAggDFs
          .slice(1, preAggDFs.size)
          .foldLeft(firstPreAgg)((baseDF, curDF) => {
            baseDF.join(curDF, keyColumnNames)
          })
        val preAggRootDir = incrAggCtx.previousSnapshotRootDirMap(anchorGroup.anchorsWithSameSource.head.selectedFeatures.head)
        Seq(incrementalCalculate(anchorGroup, joinedPreAggDFs, source, keyExtractor, bloomFilter, preAggRootDir, mvelContext))
      }
    }

    (directTransformedResult ++ incrementalTransformedResult).flatten.toSeq
  }

  /**
   * divide requested features into direct transformable and incremental transformable groups.
   * The split relies on: (1) user enable incremental or not, (2) supported aggregation function types, currently we only support sum and count (3)
   * pre-aggregation exists or not
   * @param source the source that all anchorsWithSameSource based on
   * @param anchorFeatureGroups anchor feature groups to process
   * @param incrementalAggContext contains all the contextual information w.r.t incrememntal aggregation.
   * @return feature groups to use direct aggregation and incremental aggregation, respectively
   */
  private def groupAggregationFeatures(
      source: DataSourceAccessor,
      anchorFeatureGroups: AnchorFeatureGroups,
      incrementalAggContext: Option[IncrementalAggContext]): (Option[AnchorFeatureGroups], Option[AnchorFeatureGroups]) = {

    // If incremental context is empty or not enabled or if the snapshot root directory map is empty(i.e. if this the first run),
    // we group all the input anchored features under direct aggregation.
    if (incrementalAggContext.isEmpty ||
        incrementalAggContext.get.previousSnapshotRootDirMap.isEmpty ||
        !incrementalAggContext.get.isIncrementalAggEnabled) {
      return (Some(anchorFeatureGroups), None)
    }
    // Else we start grouping features based on the aggregation operation.
    val requestedFeatureNames = anchorFeatureGroups.requestedFeatures
    val supportedIncrementalAggTypes = List(AggregationType.COUNT, AggregationType.SUM)

    // Each feature is classified as either direct or incremental. The classification rule is whether the
    // aggregation type associated with this feature is supported for incremental process or not. Currently,
    // only SUM and COUNT are supported for incremental case.
    val directIncrementalGroups = requestedFeatureNames.foldLeft(
      Seq(
        AnchorFeatureGroups(Seq.empty[FeatureAnchorWithSource], Seq.empty[String]),
        AnchorFeatureGroups(Seq.empty[FeatureAnchorWithSource], Seq.empty[String])))((aggGroupPairs, featureName) => {
      val selectedFeatureAnchorWithSource =
        anchorFeatureGroups.anchorsWithSameSource.filter(featureAnchorWithSource => {
          featureAnchorWithSource.selectedFeatures.contains(featureName)
        })

      // Each feature name must be unique across all anchors, so it can only and must belong to one feature
      // anchor.
      if (selectedFeatureAnchorWithSource.size != 1) {
        throw new FeathrFeatureTransformationException(
          ErrorLabel.FEATHR_USER_ERROR,
          "Multiple anchors define the same feature name. Please check the " +
            s"feature definitions in each anchor. Duplicated anchors are: $selectedFeatureAnchorWithSource")
      }
      val features = getFeatureDefinitions(selectedFeatureAnchorWithSource.head.featureAnchor.extractor)
      val aggType = features(featureName).aggregationType
      val isSupportedAggType = supportedIncrementalAggTypes.contains(aggType)

      // check if we can find this feature in the previous aggregation snapshot. If not, it is a new feature
      // definition and we should directly aggregate on it.
      val isOldFeature = incrementalAggContext.get.previousSnapshotMap.contains(featureName)
      // The first element of the pairs is direct aggregation group and the second is the incremental
      val localAggGroupPairs =
        if (isSupportedAggType && isOldFeature) {
          Seq(
            AnchorFeatureGroups(Seq.empty[FeatureAnchorWithSource], Seq.empty[String]),
            AnchorFeatureGroups(selectedFeatureAnchorWithSource, Seq(featureName)))
        } else {
          Seq(
            AnchorFeatureGroups(selectedFeatureAnchorWithSource, Seq(featureName)),
            AnchorFeatureGroups(Seq.empty[FeatureAnchorWithSource], Seq.empty[String]))
        }

      Seq(
        AnchorFeatureGroups(
          aggGroupPairs(0).anchorsWithSameSource ++ localAggGroupPairs(0).anchorsWithSameSource,
          aggGroupPairs(0).requestedFeatures ++ localAggGroupPairs(0).requestedFeatures),
        AnchorFeatureGroups(
          aggGroupPairs(1).anchorsWithSameSource ++ localAggGroupPairs(1).anchorsWithSameSource,
          aggGroupPairs(1).requestedFeatures ++ localAggGroupPairs(1).requestedFeatures))
    })

    (
      if (directIncrementalGroups(0).requestedFeatures.isEmpty) None else Some(directIncrementalGroups(0)),
      if (directIncrementalGroups(1).requestedFeatures.isEmpty) None else Some(directIncrementalGroups(1)))
  }

  /**
   * infer feature types for a set of feature columns in the input dataframe
   * @param df input dataframe with features
   * @param featureNames feature names/columns to infer features
   * @return inferred feature types
   */
  private def inferFeatureTypesFromRawDF(df: DataFrame, featureNames: Seq[String]): Map[String, FeatureTypeConfig] = {
    featureNames
      .map(featureName => {
        val dataType = df.schema.fields(df.schema.fieldIndex(featureName)).dataType
        featureName -> new FeatureTypeConfig(FeaturizedDatasetUtils.inferFeatureTypeFromColumnDataType(dataType))
      })
      .toMap
  }

  /**
   * incrementally aggregate on a group of anchors with same source view. Each anchor defines various aggregation features.
   * The high level idea of incremental aggregation is: curAggDF = preAggDF + newDeltaWindowAggDF - oldDeltaWindowAggDF
   * @param featureAnchorWithSource a group of anchors with same source view
   * @param df previous aggregation snapshot
   * @param source source to aggregate on
   * @param keyExtractor key extractor to apply on source
   * @param bloomFilter bloomfilter to apply on the source view
   * @param preAggRootDir incremental aggregation snapshot root dir for the previous run.
   * @return a TransformedResultWithKey which contains the dataframe and other info such as feature column, keys
   */
  private def incrementalCalculate(
      featureAnchorWithSource: AnchorFeatureGroups,
      df: DataFrame,
      source: DataSourceAccessor,
      keyExtractor: SourceKeyExtractor,
      bloomFilter: Option[BloomFilter],
      preAggRootDir: String,
      mvelContext: Option[FeathrExpressionExecutionContext]): KeyedTransformedResult = {
    // get the aggregation window of the feature
    val aggWindow = getFeatureAggWindow(featureAnchorWithSource)

    // calculate the start date and end date of the new delta window
    val newDeltaWindowEndDateStr = featureAnchorWithSource.anchorsWithSameSource.head.dateParam.get.endDate.get
    val (dateParam, newDeltaWindowSize) = IncrementalAggUtils.getNewDeltaWindowInterval(preAggRootDir, aggWindow, newDeltaWindowEndDateStr)

    // directly aggregate on the new delta window
    // the input data in the new delta could be missing, maybe due to some delay of the input data generation workflow.
    // If so, even though the incremental aggregation succeeds, the  result is incorrect.
    // And the incorrect result will be propagated to all subsequent incremental aggregation because the incorrect result will be used as the snapshot.

    val newDeltaSourceAgg = directCalculate(featureAnchorWithSource, source, keyExtractor, bloomFilter, Some(dateParam), None, mvelContext)
    // if the new delta window size is smaller than the request feature window, need to use the pre-aggregated results,
    if (newDeltaWindowSize < aggWindow) {
      // add prefixes to feature columns and keys for the previous aggregation snapshot
      val newDeltaAgg = newDeltaSourceAgg.transformedResult.df
      val newDeltaFeatureNameAndPrefixPairs = newDeltaSourceAgg.transformedResult.featureNameAndPrefixPairs
      val newDeltaFeatureColumnNames = newDeltaFeatureNameAndPrefixPairs.map(x => x._2 + x._1)
      val joinKeys = newDeltaSourceAgg.joinKey
      val renamedPreAgg =
        newDeltaFeatureNameAndPrefixPairs.foldLeft(df)((baseDF, renamePair) => baseDF.withColumnRenamed(renamePair._1, renamePair._2 + renamePair._1))

      // calculate the start date and end date of the old delta window
      val oldDeltaWindowInterval = IncrementalAggUtils.getOldDeltaWindowDateParam(preAggRootDir, aggWindow, newDeltaWindowSize.toInt, newDeltaWindowEndDateStr)
      if (!source.isInstanceOf[TimeBasedDataSourceAccessor]) {
        throw new FeathrException(ErrorLabel.FEATHR_ERROR, "overlapWithInterval should not be called if the source has no time interval.")
      }
      val leftKeyColumnNames = getStandardizedKeyNames(joinKeys.size)
      val preAggWithoutOldDelta = if (!source.asInstanceOf[TimeBasedDataSourceAccessor].overlapWithInterval(oldDeltaWindowInterval)) {
        renamedPreAgg
      } else {
        // preAgg - oldDeltaAgg
        val oldDeltaSourceAgg = directCalculate(featureAnchorWithSource, source, keyExtractor, bloomFilter, Some(oldDeltaWindowInterval), None, mvelContext)
        val oldDeltaAgg = oldDeltaSourceAgg.transformedResult.df
        mergeDeltaDF(renamedPreAgg, oldDeltaAgg, leftKeyColumnNames, joinKeys, newDeltaFeatureColumnNames, false)
      }

      // preAggWithoutOldDelta + newDeltaAgg
      val preAggWithNewDelta = mergeDeltaDF(preAggWithoutOldDelta, newDeltaAgg, leftKeyColumnNames, joinKeys, newDeltaFeatureColumnNames)
      val featureColumnFormats = newDeltaFeatureColumnNames.map(name => name -> FeatureColumnFormat.FDS_TENSOR).toMap
      val resultWithoutKey =
        TransformedResult(newDeltaFeatureNameAndPrefixPairs, preAggWithNewDelta, featureColumnFormats, newDeltaSourceAgg.transformedResult.inferredFeatureTypes)
      KeyedTransformedResult(joinKeys, resultWithoutKey)
    } else {
      newDeltaSourceAgg
    }
  }


  /**
   * Convert the dataframe that results are the end of all node execution to QUINCE_FDS tensors. Note that we expect some
   * columns to already be in FDS format and FeatureColumnFormats map will tell us that. Some transformation operators
   * and nodes will return the column in FDS format so we do not need to do conversion in that instance.
   * @param allFeaturesToConvert all features to convert
   * @param featureColumnFormatsMap transformer returned result
   * @param withFeatureDF input dataframe with all requested features
   * @param userProvidedFeatureTypeConfigs user provided feature types
   * @return dataframe in FDS format
   */
  def convertFCMResultDFToFDS(
    allFeaturesToConvert: Seq[String],
    featureColumnFormatsMap: Map[String, FeatureColumnFormat],
    withFeatureDF: DataFrame,
    userProvidedFeatureTypeConfigs: Map[String, FeatureTypeConfig] = Map()): FeatureDataFrame = {
    // 1. infer the feature types if they are not done by the transformers above
    val defaultInferredFeatureTypes = inferFeatureTypesFromRawDF(withFeatureDF, allFeaturesToConvert)
    val transformedInferredFeatureTypes = defaultInferredFeatureTypes
    val featureColNameToFeatureNameAndType =
      allFeaturesToConvert.map { featureName =>
        val userProvidedConfig = userProvidedFeatureTypeConfigs.getOrElse(featureName, FeatureTypeConfig.UNDEFINED_TYPE_CONFIG)
        val userProvidedFeatureType = userProvidedConfig.getFeatureType
        val processedFeatureTypeConfig = if (userProvidedFeatureType == FeatureTypes.UNSPECIFIED) {
          transformedInferredFeatureTypes.getOrElse(featureName, FeatureTypeConfig.UNDEFINED_TYPE_CONFIG)
        } else userProvidedConfig
        val colName = featureName
        (colName, (featureName, processedFeatureTypeConfig))
      }.toMap
    val inferredFeatureTypes = featureColNameToFeatureNameAndType.map {
      case (_, (featureName, featureType)) =>
        featureName -> featureType
    }

    // 2. convert to QUINCE_FDS
    val convertedDF = featureColNameToFeatureNameAndType
      .groupBy(pair => featureColumnFormatsMap(pair._1))
      .foldLeft(withFeatureDF)((inputDF, featureColNameToFeatureNameAndTypeWithFormat) => {
        val fdsDF = featureColNameToFeatureNameAndTypeWithFormat._1 match {
          case FeatureColumnFormat.FDS_TENSOR =>
            inputDF
          case FeatureColumnFormat.RAW =>
            // sql extractor return rawDerivedFeatureEvaluator.scala (Diff rev
            val convertedDF = FeaturizedDatasetUtils.convertRawDFtoQuinceFDS(inputDF, featureColNameToFeatureNameAndType)
            convertedDF
        }
        fdsDF
      })
    FeatureDataFrame(convertedDF, inferredFeatureTypes)
  }

  /**
   * This method is used to strip off the function name, ie - USER_FACING_MULTI_DIM_FDS_TENSOR_UDF_NAME.
   * For example, if the featureDef: FDSExtract(f1), then only f1 will be returned.
   * @param featureDef  feature definition expression with the keyword (USER_FACING_MULTI_DIM_FDS_TENSOR_UDF_NAME)
   * @return  feature def expression after stripping off the keyword (USER_FACING_MULTI_DIM_FDS_TENSOR_UDF_NAME)
   */
  def parseMultiDimTensorExpr(featureDef: String): String = {
    // String char should be one more than the len of the keyword to account for '('. The end should be 1 less than length of feature string
    // to account for ')'.
    featureDef.substring(featureDef.indexOf("(") + 1, featureDef.indexOf(")"))
  }


  def applyRowBasedTransformOnRdd(userProvidedFeatureTypes: Map[String, FeatureTypes], requestedFeatureNames: Seq[String],
    inputRdd: RDD[_], sourceKeyExtractors: Seq[SourceKeyExtractor], transformers: Seq[AnchorExtractorBase[Any]],
    featureTypeConfigs: Map[String, FeatureTypeConfig]): (DataFrame, Seq[String]) = {
    /*
     * Transform the given RDD by applying extractors to each row to create an RDD[Row] where each Row
     * represents keys and feature values
     */
    val spark = SparkSession.builder().getOrCreate()
    val FeatureTypeInferenceContext(featureTypeAccumulators) =
      FeatureTransformation.getTypeInferenceContext(spark, userProvidedFeatureTypes, requestedFeatureNames)
    val transformedRdd = inputRdd map { row =>
      val (keys, featureValuesWithType) = transformRow(requestedFeatureNames, sourceKeyExtractors, transformers, row, featureTypeConfigs)
      requestedFeatureNames.zip(featureValuesWithType).foreach {
        case (featureRef, (_, featureType)) =>
          if (featureTypeAccumulators(featureRef).isZero && featureType != null) {
            // This is lazy evaluated
            featureTypeAccumulators(featureRef).add(FeatureTypes.valueOf(featureType.getBasicType.toString))
          }
      }
      // Create a row by merging a row created from keys and a row created from term-vectors/tensors
      Row.merge(Row.fromSeq(keys), Row.fromSeq(featureValuesWithType.map(_._1)))
    }

    // Create a DataFrame from the above obtained RDD
    val keyNames = getFeatureKeyColumnNamesRdd(sourceKeyExtractors.head, inputRdd)
    val (outputSchema, inferredFeatureTypeConfigs) = {
      val inferredFeatureTypes = inferFeatureTypes(featureTypeAccumulators, transformedRdd, requestedFeatureNames)
      val inferredFeatureTypeConfigs = inferredFeatureTypes.map(x => x._1 -> new FeatureTypeConfig(x._2))
      val mergedFeatureTypeConfig = inferredFeatureTypeConfigs ++ featureTypeConfigs
      val colPrefix = ""
      val featureTensorTypeInfo = getFDSSchemaFields(requestedFeatureNames, mergedFeatureTypeConfig, colPrefix)
      val structFields = keyNames.foldRight(List.empty[StructField]) {
        case (colName, acc) =>
          StructField(colName, StringType) :: acc
      }
      val outputSchema = StructType(StructType(structFields ++ featureTensorTypeInfo))
      (outputSchema, mergedFeatureTypeConfig)
    }
    (spark.createDataFrame(transformedRdd, outputSchema), keyNames)
  }

  private def transformRow(
    requestedFeatureNames: Seq[FeatureName],
    sourceKeyExtractors: Seq[SourceKeyExtractor],
    transformers: Seq[AnchorExtractorBase[Any]],
    row: Any,
    featureTypeConfigs: Map[String, FeatureTypeConfig] = Map()): (Seq[String], Seq[(Any, FeatureType)]) = {
    val keys = sourceKeyExtractors.head match {
      case mvelSourceKeyExtractor: MVELSourceKeyExtractor => mvelSourceKeyExtractor.getKey(row)
      case specificSourceKeyExtractor: SpecificRecordSourceKeyExtractor => specificSourceKeyExtractor.getKey(row)
      case _ => throw new FeathrFeatureTransformationException(ErrorLabel.FEATHR_USER_ERROR, s"${sourceKeyExtractors.head} is not a valid extractor on RDD")
    }

    /*
     * For the given row, apply all extractors to extract feature values. If requested as tensors, each feature value
     * contains a tensor else a term-vector.
     */
    val features = transformers map {
      case extractor: AnchorExtractor[Any] =>
        val features = extractor.getFeatures(row)
        print(features)
        FeatureValueTypeValidator.validate(features, featureTypeConfigs)
        features
      case extractor =>
        throw new FeathrFeatureTransformationException(
          ErrorLabel.FEATHR_USER_ERROR,
          s"Invalid extractor $extractor for features:" +
            s"$requestedFeatureNames requested as tensors")
    } reduce (_ ++ _)
    if (logger.isTraceEnabled) {
      logger.trace(s"Extracted features: $features")
    }

    /*
     * Retain feature values for only the requested features, and represent each feature value as a term-vector or as
     * a tensor, as specified. If tensors are required, create a row for each feature value (that is, the tensor).
     */
    val featureValuesWithType = requestedFeatureNames map { name =>
      features.get(name) map {
        case featureValue =>
          val tensorData: TensorData = featureValue.getAsTensorData()
          val featureType: FeatureType = featureValue.getFeatureType()
          val row = FeaturizedDatasetUtils.tensorToFDSDataFrameRow(tensorData)
          (row, featureType)
      } getOrElse ((null, null)) // return null if no feature value present
    }
    (keys, featureValuesWithType)
  }

  /**
   * Get standardized key names for feature generation, e.g. key0, key1, key2, etc.
   * @param joinKeySize number of join keys
   */
  private[offline] def getStandardizedKeyNames(joinKeySize: Int) = {
    Range(0, joinKeySize).map("key" + _)
  }
  // max number of feature groups that can be calculated at the same time
  // each group will be a separate spark job
  private val MAX_PARALLEL_FEATURE_GROUP = 10
}

private[offline] case class FeatureTypeInferenceContext(featureTypeAccumulators: Map[String, FeatureTypeAccumulator])
