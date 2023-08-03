package com.linkedin.feathr.offline.generation

import com.databricks.spark.avro.SchemaConverters
import com.google.common.collect.Lists
import com.linkedin.feathr.common.JoiningFeatureParams
import com.linkedin.feathr.offline.config.location.KafkaEndpoint
import com.linkedin.feathr.offline.generation.outputProcessor.PushToRedisOutputProcessor.TABLE_PARAM_CONFIG_NAME
import com.linkedin.feathr.offline.generation.outputProcessor.RedisOutputUtils
import com.linkedin.feathr.offline.job.FeatureTransformation.getFeatureKeyColumnNames
import com.linkedin.feathr.offline.job.{FeatureGenSpec, FeatureTransformation}
import com.linkedin.feathr.offline.logical.FeatureGroups
import com.linkedin.feathr.offline.source.accessor.DataPathHandler
import com.linkedin.feathr.offline.transformation.{AnchorToDataSourceMapper, DataFrameBasedSqlEvaluator}
import com.linkedin.feathr.sparkcommon.SimpleAnchorExtractorSpark
import org.apache.avro.Schema
import org.apache.avro.Schema.Type
import org.apache.avro.generic.{GenericDatumReader, GenericRecord}
import org.apache.avro.io.DecoderFactory
import org.apache.spark.customized.CustomGenericRowWithSchema
import org.apache.spark.sql.catalyst.encoders.RowEncoder
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema
import org.apache.spark.sql.functions.{col, udf}
import org.apache.spark.sql.streaming.OutputMode
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.{DataFrame, Row, SaveMode, SparkSession}

import java.nio.ByteBuffer
import scala.collection.JavaConverters._
import scala.collection.convert.wrapAll._

/**
 * Class to ingest streaming features
 */
class StreamingFeatureGenerator(dataPathHandlers: List[DataPathHandler]) {
  @transient val anchorToDataFrameMapper = new AnchorToDataSourceMapper(dataPathHandlers)

  /**
   * Ingest streaming features
   * @param ss spark session
   * @param featureGenSpec feature generation config specification
   * @param featureGroups all features defined in the system
   * @param keyTaggedFeatures streaming features to ingest/generate
   */
  def generateFeatures(ss: SparkSession, featureGenSpec: FeatureGenSpec, featureGroups: FeatureGroups,
                       keyTaggedFeatures: Seq[JoiningFeatureParams]): Unit = {
    val anchors = keyTaggedFeatures.map(streamingFeature => {
      featureGroups.allAnchoredFeatures.get(streamingFeature.featureName).get
    })

    assert(featureGenSpec.getOutputProcessorConfigs.size == 1, "Only one output sink is supported in streaming mode")
    val outputConfig = featureGenSpec.getOutputProcessorConfigs.head
    val timeoutMs = if (!outputConfig.getParams.hasPath("timeoutMs")) {
      Long.MaxValue
    } else {
      outputConfig.getParams.getNumber("timeoutMs").longValue()
    }
    // Load the raw streaming source data
    val anchorDfRDDMap = anchorToDataFrameMapper.getAnchorDFMapForGen(ss, anchors, None, false, true)

    // Remove entries for which feature dataframe cannot be loaded.
    val updatedAnchorDFRDDMap = anchorDfRDDMap.filter(anchorEntry => anchorEntry._2.isDefined).map(anchorEntry => anchorEntry._1 -> anchorEntry._2.get)

    updatedAnchorDFRDDMap.par.map { case (anchor, dfAccessor) => {
      val schemaStr = anchor.source.location.asInstanceOf[KafkaEndpoint].schema.avroJson
      val schemaStruct = SchemaConverters.toSqlType(Schema.parse(schemaStr)).dataType.asInstanceOf[StructType]
      val rowForRecord = (input: Any) => {
        val values = Lists.newArrayList[Any]
        val decoder = DecoderFactory.get().binaryDecoder(input.asInstanceOf[Array[Byte]], null)

        val avroSchema = Schema.parse(schemaStr)
        val reader = new GenericDatumReader[GenericRecord](avroSchema)
        val record = reader.read(null, decoder)
        for (field <- record.getSchema.getFields) {
          val fieldType = if (field.schema.equals(Type.UNION)) {
            field.schema.getTypes.get(1).getType
          } else {
            field.schema.getType
          }
          val fieldValue = Option(record.get(field.name)) match {
            case Some(value) if fieldType.equals(Type.STRING) =>
              // Avro returns Utf8s for strings, which Spark SQL doesn't know how to use
              value.toString
            case Some(value) if fieldType.equals(Type.BYTES) =>
              // Avro returns binary as a ByteBuffer, but Spark SQL wants a byte[]
              value.asInstanceOf[ByteBuffer].array
            case _ => record.get(field.name)
          }
          values.add(fieldValue)
        }

        new CustomGenericRowWithSchema(
          values.asScala.toArray, schemaStruct
        )
      }
      val convertUDF = udf(rowForRecord)

      // Streaming processing each source
      dfAccessor.get().writeStream
        .outputMode(OutputMode.Update)
        .foreachBatch{ (batchDF: DataFrame, batchId: Long) =>
          // Convert each batch dataframe from the kafka built-in schema(which always has 'value' field) to user provided schema
          val convertedDF = batchDF.select(convertUDF(col("value")))
          val encoder = RowEncoder.apply(schemaStruct)
          // Use encoder to add user schema into each row
          val rowDF = convertedDF.map(row=>
            // Has to create a GenericRowWithSchema since encodeRow in Spark-redis expects the schema within each Row
            new GenericRowWithSchema(row.toSeq.flatMap(value=> value.asInstanceOf[GenericRowWithSchema].toSeq).toArray, row.schema)
              // Has to cast of Row to make compile happy due to the encoder
              .asInstanceOf[Row]
          )(encoder)

          val featureNamePrefixPairs = anchor.selectedFeatures.map(f => (f, "_streaming_"))
          val keyExtractor = anchor.featureAnchor.sourceKeyExtractor
          val withKeyColumnDF = keyExtractor.appendKeyColumns(rowDF)
          // Apply feature transformation
          val transformedResult = DataFrameBasedSqlEvaluator.transform(anchor.featureAnchor.extractor.asInstanceOf[SimpleAnchorExtractorSpark],
            withKeyColumnDF, featureNamePrefixPairs, anchor.featureAnchor.featureTypeConfigs)
          val outputJoinKeyColumnNames = getFeatureKeyColumnNames(keyExtractor, withKeyColumnDF)
          val selectedColumns = outputJoinKeyColumnNames ++ anchor.selectedFeatures.filter(keyTaggedFeatures.map(_.featureName).contains(_))
          val cleanedDF = transformedResult.df.select(selectedColumns.head, selectedColumns.tail:_*)
          val keyColumnNames = FeatureTransformation.getStandardizedKeyNames(outputJoinKeyColumnNames.size)
          val resultFDS: DataFrame = PostGenPruner().standardizeColumns(outputJoinKeyColumnNames, keyColumnNames, cleanedDF)
          val tableName = outputConfig.getParams.getString(TABLE_PARAM_CONFIG_NAME)
          val allFeatureCols = resultFDS.columns.diff(keyColumnNames).toSet
          RedisOutputUtils.writeToRedis(ss, resultFDS, tableName, keyColumnNames, allFeatureCols, SaveMode.Append)
        }
        .start()
        .awaitTermination(timeoutMs)
      }
    }
  }
}
