package com.linkedin.feathr.swj.join

import com.google.common.base.Predicate
import com.google.common.collect.Iterables
import com.linkedin.feathr.swj.{SlidingWindowFeature, SlidingWindowJoin}
import com.linkedin.feathr.swj.join.SlidingWindowJoinIterator._
import com.linkedin.feathr.swj.transformer.FeatureTransformer._
import it.unimi.dsi.fastutil.objects.{Object2ObjectMap, ObjectArrayList}
import org.apache.spark.sql.Row
import org.apache.spark.sql.types._

import scala.collection.JavaConverters._
import scala.util.control.Breaks._

/**
  * Perform sliding window join/aggregation for the corresponding partitions from the label data
  * and the standardized feature data. Left outer join is performed between label and feature
  * dataset. The Label and feature records are presorted based on (join_key, timestamp_col) in
  * [[SlidingWindowJoin]].
  *
  * @param label Label data partition iterator
  * @param feature Standardized feature data partition iterator
  * @param labelSchema Schema of label DataFrame
  * @param labelJoinKeyIndex Index of join key column in label DataFrame schema
  * @param labelTimestampIndex Index of timestamp column in label DataFrame schema
  * @param featureSchema Schema of feature DataFrame
  * @param featureDefs List of [[SlidingWindowFeature]] in the same order as they appear in featureSchema
  * @return Iterator of aggregated results
  */
class SlidingWindowJoinIterator(
    val label: Iterator[Row],
    val feature: Iterator[Row],
    val labelSchema: StructType,
    val labelJoinKeyIndex: Int,
    val labelTimestampIndex: Int,
    val featureSchema: StructType,
    val featureDefs: List[SlidingWindowFeature]) extends Iterator[Row] {

  // Current label record fetched from label iterator
  private var currentLabel: Row = _
  // Current feature record fetched from feature iterator
  private var currentFeature: Row = _
  // Record the first feature record in a block of feature records that are cached in memory
  private var firstFeatureRecordInBlock: Row = _
  // Flag indicating whether the current cached feature records are outdated
  private var consumeNextBlock: Boolean = true
  // Flag indicating whether to consume next feature record from feature iterator
  private var loadNextFeature: Boolean = true

  // Seq of nulls having the same number of nulls as the number of sliding window features
  // This is reused to generate null aggregate results
  private lazy val NULL_SEQ: Seq[Any] = featureDefs.map(_ => null)
  // In memory cache of feature records sharing the same join_key. Initialized with 100 elements.
  private val rowCache: ObjectArrayList[Row] = new ObjectArrayList[Row](100)
  // List of FeatureColumnMetaData providing the necessary information to perform sliding window
  // aggregation for each sliding window feature column.
  private val featureColumns: List[FeatureColumnMetaData] = generateFeatureColumns(featureDefs, featureSchema)

  override def hasNext: Boolean = label.hasNext

  override def next(): Row = {
    currentLabel = label.next()
    if (consumeNextBlock) {
      generateFirstRowInBlock()
    } else {
      if (compareJoinKey(currentLabel, firstFeatureRecordInBlock) == 0) {
        generateRowWithAggregatedFeatures()
      } else {
        consumeNextBlock = true
        generateFirstRowInBlock()
      }
    }
  }

  /**
    * When consumeNextBlock is true, the previously cached feature records (if any) become
    * outdated. Given the current label record, realign feature records based on join_key
    * comparison. After the realignment, generate the first result Row.
    * @return
    */
  private def generateFirstRowInBlock(): Row = {
    if (feature.hasNext) {
      // loadNextFeature could be false after loading all feature records sharing the same join_key
      // in memory. At this time, we have already consumed one feature record from iterator
      // whose join_key value is different. Similarly, if the current feature record has a larger
      // join key value compared with current label record, we would also consumed one feature
      // record from the iterator that's waiting to be joined. Skip loading next feature record so
      // we don't miss any feature records.
      if (loadNextFeature) {
        currentFeature = feature.next()
      } else {
        loadNextFeature = true
      }
      compareJoinKey(currentLabel, currentFeature) match {
        // If currentLabel.join_key < currentFeature.join_key, generate null result due to
        // left outer join
        case result if result < 0 =>
          loadNextFeature = false
          generateRowWithNullFeatures()
        // If currentLabel.join_key > currentFeature.join_key, realign feature records with
        // currentLabel by draining all feature records whose join_key value is smaller than
        // currentLabel.join_key. Based on the join_key value of the next feature record,
        // either generate a null result or an aggregated result.
        case result if result > 0 =>
          drainRecordAndGenerateRow()
        // If currentLabel.join_key == currentFeature.join_key, load all feature records sharing
        // the same key into memory before generating an aggregated result for currentLabel
        case 0 =>
          loadCacheAndGenerateRow()
      }
    } else if (null != currentFeature && 0 == compareJoinKey(currentLabel, currentFeature)) {
      // This means the current feature is the only feature record for the current label,
      // and it has been loaded previously but not yet processed. This happens to the last row
      // in a partition. Since the join key matches, we need to generate row using the current feature row.
      loadCacheAndGenerateRow()
    } else {
      // If feature iterator is fully drained, generate null result. Notice that this
      // does not apply to the situation where consumeNextBlock is false. So even if feature
      // iterator is fully drained, as far as currentLabel still has the same join_key value
      // as the most recent firstFeatureRecordInBlock, aggregated result will still be
      // generated from cached feature records
      generateRowWithNullFeatures()
    }
  }

  /**
    * First drain feature iterator for all records that have smaller join key than the current
    * label record, since we are doing left outer join. After draining the records, generate a
    * Row representing the result of joining sliding window aggregated features on the current
    * label record
    */
  private def drainRecordAndGenerateRow(): Row = {
    var result: Row = null
    breakable {
      while (true) {
        if (feature.hasNext) {
          currentFeature = feature.next()
          val compareResult = compareJoinKey(currentLabel, currentFeature)
          if (compareResult < 0) {
            result = generateRowWithNullFeatures()
            loadNextFeature = false
            break()
          } else if (compareResult == 0) {
            result = loadCacheAndGenerateRow()
            break()
          }
        } else {
          result = generateRowWithNullFeatures()
          break()
        }
      }
    }
    result
  }

  /**
    * Load all feature records sharing the same join_key in memory, and generate an aggregate
    * result for currentLabel.
    */
  private def loadCacheAndGenerateRow(): Row = {
    // Record the first feature record cached in memory in the current block. This is used
    // later to check if currentLabel still has the same join_key as the cached feature records
    firstFeatureRecordInBlock = currentFeature
    loadRowCache()
    // Feature records have been cached in memory now. Invoking generateFirstRowInBlock to
    // realign currentLabel with currentFeature is not needed until currentLabel no longer
    // share the same join_key as the cached feature records
    consumeNextBlock = false
    featureColumns.foreach(_.reset())
    generateRowWithAggregatedFeatures()
  }

  /**
    * Advance the startIndex in the given FeatureColumnMetaData to identify the beginning
    * of the current window
    */
  private def moveStartIndex(
      featureColMeta: FeatureColumnMetaData,
      currentLabelTimestamp: Long): Unit = {
    // Reset window index and aggregate result
    featureColMeta.endIndex = featureColMeta.startIndex
    featureColMeta.aggResult = null
    // Move both startIndex and endIndex together.
    // The feature window should be left-exclusive. e.g. if label is 2020-08-11:00:00, and window is 1 day, then we
    // want to include 2020-08-11:00, but exclude feature record with timestamp equals to 2020-08-10:00:00,
    // using '<=' to compare timestamp here
    while (featureColMeta.startIndex < rowCache.size() &&
      fetchTimestampFromRow(rowCache.get(featureColMeta.startIndex)) <=
        currentLabelTimestamp - featureColMeta.windowSpec.width.getSeconds -
          featureColMeta.windowSpec.delay.getSeconds) {
      featureColMeta.startIndex += 1
      featureColMeta.endIndex +=1
    }
  }

  /**
    * Advance the endIndex in the given FeatureColumnMetaData to identify the end of the
    * current window. In the process, perform feature data aggregation
    */
  private def moveEndIndex(
      featureColMeta: FeatureColumnMetaData,
      currentLabelTimestamp: Long,
      index: Int): Unit = {
    // Keep moving endIndex until the entire window is identified
    // The feature window should be right-inclusive. e.g. if label is 2020-08-11:00:00, and window is 1 day, then we
    // want to include 2020-08-11:00, but exclude feature record with timestamp equals to 2020-08-10:00:00,
    // using '<=' to compare timestamp here
    while (featureColMeta.endIndex < rowCache.size() &&
      fetchTimestampFromRow(rowCache.get(featureColMeta.endIndex)) <=
        currentLabelTimestamp - featureColMeta.windowSpec.delay.getSeconds) {
      if (featureColMeta.groupSpec.isEmpty) {
        featureColMeta.aggResult = featureColMeta.aggSpec.agg(featureColMeta.aggResult,
          rowCache.get(featureColMeta.endIndex).getStruct(index).get(0), featureColMeta.dataType)
      } else {
        getOrElseUpdateGroupAggResult(rowCache.get(featureColMeta.endIndex).getStruct(index).get(1),
          rowCache.get(featureColMeta.endIndex).getStruct(index).get(0), featureColMeta)
      }
      featureColMeta.endIndex += 1
    }
  }

  /**
    * Given the timestamp in the current label record and a feature column metadata, identify the
    * range of feature records from rowCache for the given sliding window, and generate the aggregation
    * results from the identified range of feature records for the given feature column
    * @param featureColMeta Metadata for the feature column
    * @param index Index of the feature column. Used to fetch corresponding column from the cached
    *              Rows
    * @return Aggregated result for the current label and the specified feature column
    */
  private def generateFeatureColumn(
      featureColMeta: FeatureColumnMetaData,
      index: Int): Any = {
    // Fetch current label record timestamp column value. It should be epoch.
    val currentLabelTimestamp = fetchTimestampFromRowWithIndex(currentLabel)
    // When endIndex <= startIndex, it is either the first window aggregate on
    // the cached feature records, or an empty window from the previous window aggregate.
    // In this case, the aggregate will be calculated from scratch instead of incrementally.
    if (featureColMeta.endIndex <= featureColMeta.startIndex) {
      moveStartIndex(featureColMeta, currentLabelTimestamp)
      moveEndIndex(featureColMeta, currentLabelTimestamp, index)
      if (featureColMeta.groupSpec.isEmpty) {
        featureColMeta.aggSpec.calculateAggregate(featureColMeta.aggResult, featureColMeta.dataType)
      } else {
        takeTopK(featureColMeta)
      }
    } else {
      // We could potentially calculate aggregate result incrementally, depending on
      // if the aggregation type is incremental or not. Incremental aggregation is
      // not used for grouping aggregations to avoid accumulating too large a HashMap
      // due to irrelevant grouping keys for each sliding window.
      if (featureColMeta.aggSpec.isIncrementalAgg && featureColMeta.groupSpec.isEmpty) {
        // For incremental aggregation, we first move startIndex forward. Each record
        // removed from the window would also update the value of the aggregate result
        while (featureColMeta.startIndex < featureColMeta.endIndex &&
          featureColMeta.startIndex < rowCache.size() &&
          fetchTimestampFromRow(rowCache.get(featureColMeta.startIndex)) <
            currentLabelTimestamp - featureColMeta.windowSpec.width.getSeconds -
              featureColMeta.windowSpec.delay.getSeconds) {
          featureColMeta.aggResult = featureColMeta.aggSpec.deagg(featureColMeta.aggResult,
            rowCache.get(featureColMeta.startIndex).getStruct(index).get(0), featureColMeta.dataType)
          featureColMeta.startIndex += 1
        }
        if (featureColMeta.startIndex >= featureColMeta.endIndex) {
          moveStartIndex(featureColMeta, currentLabelTimestamp)
        }
        // After startIndex for the current window is identified, we further move endIndex
        // forward. For each record added into the window, we update the value of the
        // aggregate result
        moveEndIndex(featureColMeta, currentLabelTimestamp, index)
        featureColMeta.aggSpec.calculateAggregate(featureColMeta.aggResult, featureColMeta.dataType)
      } else {
        // For non-incremental aggregation, we need to calculate from scratch again.
        // Reset aggregate result to null first
        featureColMeta.tripMap()
        // Identify the start and end index for the current window
        // Update aggregate result value with each record in the current window
        moveStartIndex(featureColMeta, currentLabelTimestamp)
        moveEndIndex(featureColMeta, currentLabelTimestamp, index)
        if (featureColMeta.groupSpec.isEmpty) {
          featureColMeta.aggSpec.calculateAggregate(featureColMeta.aggResult, featureColMeta.dataType)
        } else {
          takeTopK(featureColMeta)
        }
      }
    }
  }

  /**
    * Generate a Row with feature columns aggregated by corresponding range of feature
    * in cache. The existing columns in label record will be put in the same order in
    * front of the result Row. The remaining columns are set to null representing all
    * the sliding window aggregated features.
    */
  private def generateRowWithAggregatedFeatures(): Row = {
    val columns: Seq[Any] = featureColumns.zipWithIndex.map{
      case (featureCol, index) => generateFeatureColumn(featureCol, index)
    }
    Row.merge(currentLabel, Row.fromSeq(columns))
  }

  /**
    * Generate a Row with feature columns set to null. The existing columns in label record
    * will be put in the same order in front of the result Row. The remaining columns are
    * set to null representing all the sliding window aggregated features.
    */
  private def generateRowWithNullFeatures(): Row = {
    // Note: Comments in object Row mentioned potential perf issue with this merge API.
    Row.merge(currentLabel, Row.fromSeq(NULL_SEQ))
  }

  /**
    * Load all feature records sharing the same join_key and cache them in memory
    */
  private def loadRowCache(): Unit = {
    rowCache.clear()
    rowCache.add(currentFeature)
    breakable {
      while (true) {
        if (feature.hasNext) {
          currentFeature = feature.next()
          if (compareJoinKey(currentLabel, currentFeature) == 0) {
            rowCache.add(currentFeature)
          } else {
            loadNextFeature = false
            break()
          }
        } else {
          break()
        }
      }
    }
  }

  /**
    * Get timestamp field value from a given Row. The timestamp column is assumed to be of type Long.
    */
  private def fetchTimestampFromRow(record: Row): Long = {
    record.getAs[Long](TIMESTAMP_COL_NAME)
  }

  private def fetchTimestampFromRowWithIndex(record: Row): Long = {
    record.getAs[Long](labelTimestampIndex)
  }

  /**
    * Utility method to extract join key column values from a label Row and a feature Row object.
    * Compare the types of the join key columns from label and feature Row to make sure they are
    * compatible, and compare the join key values. Only primitive types (Int, Long, Float, Double,
    * and String) are supported as join key types. Note that, if the label join key is null, or
    * one of the fields in a multi-field label join key is null, this method will return -1 such
    * that the label key is not joined with any feature.
    *
    * @return An integer < 0 if label Row join key is smaller than feature Row join key
    *         0 if label Row join key is equal to feature Row join key
    *         An integer > 0 if label Row join key is larger than feature Row join key
    */
  private def compareJoinKey(
      labelRow: Row,
      featureRow: Row): Int = {
    val labelJoinKeyType = labelSchema(JOIN_KEY_COL_NAME).dataType
    val featureJoinKeyType = featureSchema(JOIN_KEY_COL_NAME).dataType
    labelJoinKeyType match {
      case IntegerType =>
        val labelJoinKeyOpt = Option(labelRow.getAs[Int](labelJoinKeyIndex))
        labelJoinKeyOpt.map { labelJoinKey =>
          featureJoinKeyType match {
            case IntegerType => labelJoinKey.compare(featureRow.getAs[Int](JOIN_KEY_COL_NAME))
            case LongType => labelJoinKey.toLong.compare(featureRow.getAs[Long](JOIN_KEY_COL_NAME))
            case FloatType | DoubleType | StringType =>
              throw new RuntimeException(s"Feature data join key type ${featureJoinKeyType.typeName} is incompatible " +
                s"with label data join key type ${labelJoinKeyType.typeName}")
            case _ => throw new RuntimeException(s"Unsupported join key type ${featureJoinKeyType.typeName} " +
              s"for feature data. Only primitive types are supported.")
          }
        }.getOrElse(-1)
      case LongType =>
        val labelJoinKeyOpt = Option(labelRow.getAs[Long](labelJoinKeyIndex))
        labelJoinKeyOpt.map { labelJoinKey =>
          featureJoinKeyType match {
            case IntegerType => labelJoinKey.compare(featureRow.getAs[Int](JOIN_KEY_COL_NAME))
            case LongType => labelJoinKey.compare(featureRow.getAs[Long](JOIN_KEY_COL_NAME))
            case FloatType | DoubleType | StringType =>
              throw new RuntimeException(s"Feature data join key type ${featureJoinKeyType.typeName} is incompatible " +
                s"with label data join key type ${labelJoinKeyType.typeName}")
            case _ => throw new RuntimeException(s"Unsupported join key type ${featureJoinKeyType.typeName} " +
              s"for feature data. Only primitive types are supported.")
          }
        }.getOrElse(-1)
      case FloatType =>
        val labelJoinKeyOpt = Option(labelRow.getAs[Float](labelJoinKeyIndex))
        labelJoinKeyOpt.map { labelJoinKey =>
          featureJoinKeyType match {
            case FloatType => labelJoinKey.compare(featureRow.getAs[Float](JOIN_KEY_COL_NAME))
            case DoubleType => labelJoinKey.toDouble.compare(featureRow.getAs[Double](JOIN_KEY_COL_NAME))
            case IntegerType | LongType | StringType =>
              throw new RuntimeException(s"Feature data join key type ${featureJoinKeyType.typeName} is incompatible " +
                s"with label data join key type ${labelJoinKeyType.typeName}")
            case _ => throw new RuntimeException(s"Unsupported join key type ${featureJoinKeyType.typeName} " +
              s"for feature data. Only primitive types are supported.")
          }
        }.getOrElse(-1)
      case DoubleType =>
        val labelJoinKeyOpt = Option(labelRow.getAs[Double](labelJoinKeyIndex))
        labelJoinKeyOpt.map { labelJoinKey =>
          featureJoinKeyType match {
            case FloatType => labelJoinKey.compare(featureRow.getAs[Float](JOIN_KEY_COL_NAME))
            case DoubleType => labelJoinKey.compare(featureRow.getAs[Double](JOIN_KEY_COL_NAME))
            case IntegerType | LongType | StringType =>
              throw new RuntimeException(s"Feature data join key type ${featureJoinKeyType.typeName} is incompatible " +
                s"with label data join key type ${labelJoinKeyType.typeName}")
            case _ => throw new RuntimeException(s"Unsupported join key type ${featureJoinKeyType.typeName} " +
              s"for feature data. Only primitive types are supported.")
          }
        }.getOrElse(-1)
      case StringType =>
        val labelJoinKeyOpt = Option(labelRow.getAs[String](labelJoinKeyIndex))
        labelJoinKeyOpt.map { labelJoinKey =>
          featureJoinKeyType match {
            case StringType => labelJoinKey.compare(featureRow.getAs[String](JOIN_KEY_COL_NAME))
            case FloatType | DoubleType | IntegerType | LongType =>
              throw new RuntimeException(s"Feature data join key type ${featureJoinKeyType.typeName} is incompatible " +
                s"with label data join key type ${labelJoinKeyType.typeName}")
            case _ => throw new RuntimeException(s"Unsupported join key type ${featureJoinKeyType.typeName} " +
              s"for feature data. Only primitive types are supported.")
          }
        }.getOrElse(-1)
      // When multi-column join key is specified, we get StructType for the join key
      case StructType(labelFields) =>
        val labelJoinKey = labelRow.getStruct(labelJoinKeyIndex)
        val featureJoinKey = featureRow.getAs[Row](JOIN_KEY_COL_NAME)
        var result = 0
        featureJoinKeyType match {
          // Only when both label/feature dataset join keys are StructType and share the same number
          // of fields are these 2 keys compared
          case StructType(featureFields) if (labelFields.length == featureFields.length) =>
            breakable {
              for (index <- labelFields.indices) {
                result = compareJoinKeyStructSubField(labelJoinKey, featureJoinKey,
                  labelFields(index), featureFields(index), index)
                if (result != 0) {
                  break()
                }
              }
            }
          case _ =>
            throw new RuntimeException(s"Feature data join key type ${featureJoinKeyType.typeName} is incompatible " +
              s"with label data join key type ${labelJoinKeyType.typeName}")
        }
        result
      case _ => throw new RuntimeException(s"Unsupported join key type ${labelJoinKeyType.typeName} " +
        s"for label data. Only primitive types are supported.")
    }
  }

  /**
    * Utility method to compare matching fields of multi-column join key struct type from
    * the label and feature dataset.
    */
  private def compareJoinKeyStructSubField(
      labelJoinKey: Row,
      featureJoinKey: Row,
      labelField: StructField,
      featureField: StructField,
      index: Int): Int = {
    val labelFieldType = labelField.dataType
    val featureFieldType = featureField.dataType
    labelField.dataType match {
      case IntegerType =>
        val labelFieldValOpt = Option(labelJoinKey.getAs[Int](index))
        labelFieldValOpt.map { labelFieldVal =>
          featureFieldType match {
            case IntegerType => labelFieldVal.compare(featureJoinKey.getAs[Int](index))
            case LongType => labelFieldVal.toLong.compare(featureJoinKey.getAs[Long](index))
            case FloatType | DoubleType | StringType =>
              throw new RuntimeException(s"Feature data join key type ${featureFieldType.typeName} for " +
                s"${featureField.name} is incompatible with label data join key type ${labelFieldType.typeName} " +
                s"for ${labelField.name}")
            case _ => throw new RuntimeException(s"Unsupported join key type ${featureFieldType.typeName} " +
              s"for ${featureField.name} in feature data. Only primitive types are supported.")
          }
        }.getOrElse(-1)
      case LongType =>
        val labelFieldValOpt = Option(labelJoinKey.getAs[Long](index))
        labelFieldValOpt.map { labelFieldVal =>
          featureFieldType match {
            case IntegerType => labelFieldVal.compare(featureJoinKey.getAs[Int](index))
            case LongType => labelFieldVal.compare(featureJoinKey.getAs[Long](index))
            case FloatType | DoubleType | StringType =>
              throw new RuntimeException(s"Feature data join key type ${featureFieldType.typeName} for " +
                s"${featureField.name} is incompatible with label data join key type ${labelFieldType.typeName} " +
                s"for ${labelField.name}")
            case _ => throw new RuntimeException(s"Unsupported join key type ${featureFieldType.typeName} " +
              s"for ${featureField.name} in feature data. Only primitive types are supported.")
          }
        }.getOrElse(-1)
      case FloatType =>
        val labelFieldValOpt = Option(labelJoinKey.getAs[Float](index))
        labelFieldValOpt.map { labelFieldVal =>
          featureFieldType match {
            case FloatType => labelFieldVal.compare(featureJoinKey.getAs[Float](index))
            case DoubleType => labelFieldVal.toDouble.compare(featureJoinKey.getAs[Double](index))
            case IntegerType | LongType | StringType =>
              throw new RuntimeException(s"Feature data join key type ${featureFieldType.typeName} for " +
                s"${featureField.name} is incompatible with label data join key type ${labelFieldType.typeName} " +
                s"for ${labelField.name}")
            case _ => throw new RuntimeException(s"Unsupported join key type ${featureFieldType.typeName} " +
              s"for ${featureField.name} in feature data. Only primitive types are supported.")
          }
        }.getOrElse(-1)
      case DoubleType =>
        val labelFieldValOpt = Option(labelJoinKey.getAs[Double](index))
        labelFieldValOpt.map { labelFieldVal =>
          featureFieldType match {
            case FloatType => labelFieldVal.compare(featureJoinKey.getAs[Float](index))
            case DoubleType => labelFieldVal.compare(featureJoinKey.getAs[Double](index))
            case IntegerType | LongType | StringType =>
              throw new RuntimeException(s"Feature data join key type ${featureFieldType.typeName} for " +
                s"${featureField.name} is incompatible with label data join key type ${labelFieldType.typeName} " +
                s"for ${labelField.name}")
            case _ => throw new RuntimeException(s"Unsupported join key type ${featureFieldType.typeName} " +
              s"for ${featureField.name} in feature data. Only primitive types are supported.")
          }
        }.getOrElse(-1)
      case StringType =>
        val labelFieldValOpt = Option(labelJoinKey.getAs[String](index))
        labelFieldValOpt.map { labelFieldVal =>
          featureFieldType match {
            case StringType => labelFieldVal.compare(featureJoinKey.getAs[String](index))
            case FloatType | DoubleType | IntegerType | LongType =>
              throw new RuntimeException(s"Feature data join key type ${featureFieldType.typeName} for " +
                s"${featureField.name} is incompatible with label data join key type ${labelFieldType.typeName} " +
                s"for ${labelField.name}")
            case _ => throw new RuntimeException(s"Unsupported join key type ${featureFieldType.typeName} " +
              s"for ${featureField.name} in feature data. Only primitive types are supported.")
          }
        }.getOrElse(-1)
      case _ => throw new RuntimeException(s"Unsupported join key type ${labelFieldType.typeName} " +
        s"for ${labelField.name} in label data. Only primitive types are supported.")
    }
  }
}

object SlidingWindowJoinIterator {
  private[swj] def generateFeatureColumns(
      featureDefs: List[SlidingWindowFeature],
      featureSchema: StructType): List[FeatureColumnMetaData] = {
    featureDefs.zipWithIndex.map {
      case (featureDef, index) =>
        val meta = new FeatureColumnMetaData(featureDef.name, featureDef.windowSpec, featureDef.agg, featureDef.groupBy,
          featureSchema.fields(index).dataType.asInstanceOf[StructType].fields(0).dataType,
          featureDef.groupBy.map(spec =>
            featureSchema.fields(index).dataType.asInstanceOf[StructType].fields(1).dataType))
        featureDef.agg.updateAggDataType(meta)
        meta
    }
  }

  private def getOrElseUpdateGroupAggResult(key: Any, value: Any, featureColMeta: FeatureColumnMetaData): Unit = {
    val map = featureColMeta.groupAggResult
    if (map.containsKey(key)) {
      map.put(key, featureColMeta.aggSpec.agg(map.get(key), value, featureColMeta.dataType))
    } else {
      map.put(key, value)
    }
  }

  private def takeTopK(featureColMeta: FeatureColumnMetaData): Array[Row] = {
    // Invoke calculateAggregate for each entry in the map if needed. This is the case for AVG aggregate type.
    if (featureColMeta.aggSpec.isCalculateAggregateNeeded) {
      featureColMeta.groupAggResult.object2ObjectEntrySet().asScala.map{
        entry =>
          featureColMeta.groupAggResult.put(entry.getKey,
            featureColMeta.aggSpec.calculateAggregate(entry.getValue, featureColMeta.dataType))
      }
    }
    // Filter null values from the entry set which might be brought in with the filter condition
    val filteredList =
      Iterables.filter(featureColMeta.groupAggResult.object2ObjectEntrySet(),
        new Predicate[Object2ObjectMap.Entry[Any, Any]]() {
          override def apply(entry: Object2ObjectMap.Entry[Any, Any]): Boolean = entry.getValue != null
        })
    filteredList.asScala.map(entry => Row.fromTuple((entry.getKey, entry.getValue))).toArray
  }
}