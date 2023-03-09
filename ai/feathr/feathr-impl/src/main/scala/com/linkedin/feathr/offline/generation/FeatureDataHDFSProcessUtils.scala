package com.linkedin.feathr.offline.generation

import com.databricks.spark.avro.SchemaConverters
import com.linkedin.feathr.common.configObj.generation.OutputProcessorConfig
import com.linkedin.feathr.common.{Header, RichConfig, TaggedFeatureName}
import com.linkedin.feathr.offline.util.{FeatureGenConstants, HdfsUtils}
import com.linkedin.feathr.offline.source.dataloader.DataLoaderHandler
import org.apache.avro.{Schema, SchemaBuilder}
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.{DataFrame, SparkSession}

// write a dataframe to HDFS in specified format for feature generation
private[offline] object FeatureDataHDFSProcessUtils {

  /**
   * write a single dataframe of feature generation output to HDFS in different format
   *
   * @param groupedFeatureToDF a map of feature to its dataframe and the header information, the map should have only
   *                           one distinct dataframe
   * @param parentPath         parent HDFS path to store the dataframe
   * @param skipWrite          skip the write to HDFS, only convert the dataframe and return
   * @param endTimeOpt         optional string of end time, in yyyy/MM/dd format
   * @param timestampOpt       optional string of auto-generated timestamp
   * @param dataLoaderHandlers additional data loader handlers that contain hooks for dataframe creation and manipulation
   */
  def processFeatureDataHDFS(
      ss: SparkSession,
      groupedFeatureToDF: Map[TaggedFeatureName, (DataFrame, Header)],
      parentPath: String,
      config: OutputProcessorConfig,
      skipWrite: Boolean = false,
      endTimeOpt: Option[String],
      timestampOpt: Option[String],
      dataLoaderHandlers: List[DataLoaderHandler]): (DataFrame, Header) = {
    // since these features are in same dataframe, they must share same key tag size
    assert(groupedFeatureToDF.map(_._1.getKeyTag.size()).toSeq.distinct.size == 1)
    // the input should have been grouped, so that there's only one dataframe in the input map
    assert(groupedFeatureToDF.map(_._2._1).toSeq.distinct.size == 1)
    val outputParts = config.getParams().getNumberOpt(NUM_PARTS)
    val saveSchemaMeta = config.getParams().getBooleanWithDefault(FeatureGenConstants.SAVE_SCHEMA_META, false)
    val featureHeaderMap: Map[TaggedFeatureName, Header] = groupedFeatureToDF.mapValues(_._2)
    val (df, header) = groupedFeatureToDF.head._2
    if (skipWrite) {
      (df, header)
    } else {
      RawDataWriterUtils.writeFdsDataToDisk(ss, featureHeaderMap, parentPath, outputParts, endTimeOpt, saveSchemaMeta, df, header, dataLoaderHandlers)
    }
  }

  // convert sql schema to avro schema
  def convertToAvroSchema(sqlSchema: StructType, recordName: String = "topLevelRecord", nameSpace: String = ""): Schema = {
    val build = SchemaBuilder.record(recordName).namespace(nameSpace)
    // prepare AVRO schema for all key tags/alias
    val schema = SchemaConverters.convertStructToAvro(sqlSchema, build, nameSpace)
    schema
  }

  // write an AVRO schema to disk
  def writeSchemaToDisk(ss: SparkSession, schema: Schema, path: String): Unit = {
    // For serialization. Otherwise, Schema type cannot be passed into the below RDD function.
    val outputSchemaString = schema.toString(false)
    // write schema file to HDFS
    val schemaFileRDD = ss.sparkContext.parallelize(Seq(outputSchemaString)).repartition(1)
    HdfsUtils.deletePath(path, true)
    schemaFileRDD.saveAsTextFile(path)
  }

  // write header to disk
  def writeHeaderDataToDF(ss: SparkSession, featureHeaderMap: Map[TaggedFeatureName, Header], metaPath: String): Unit = {
    import ss.implicits._
    // write metadata to disk
    val featuresList = featureHeaderMap
      .map {
        case (taggedFeatureName, _) =>
          taggedFeatureName.getFeatureName()
      }
      .mkString(",")
    val featureListOutputStr = "features: [" + featuresList + "]"
    val featureBasicInfo = featureHeaderMap.map {
      case (taggedFeatureName, dataframeWithHeader) =>
        val featureInfo = dataframeWithHeader.featureInfoMap(taggedFeatureName)
        val featureInfoStr = featureInfo.toString
        (taggedFeatureName.toString(), featureInfoStr, featureListOutputStr)
    }.toSeq
    val metaDF = featureBasicInfo.toDF(metaHeaderColumnName, "header", "features")

    metaDF.write.mode("overwrite").format("com.databricks.spark.csv").option("header", "true").save(metaPath)
  }

  val useFloatInNTV = "useFloatInNTV"
  val valueSchemaFieldZName = "featureList"
  val keySchemaFieldName = "key"
  val featureNameField = "featureName"
  final val metaHeaderColumnName = "taggedFeatureName"
  final val NUM_PARTS = "num-parts"
}
