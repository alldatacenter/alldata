package com.linkedin.feathr.offline.generation

import com.linkedin.feathr.offline.config.location.{DataLocation, SimplePath}
import com.linkedin.feathr.offline.source.dataloader.hdfs.FileFormat
import com.linkedin.feathr.offline.source.dataloader.DataLoaderHandler
import org.apache.avro.generic.GenericRecord
import org.apache.hadoop.mapred.JobConf
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, SaveMode, SparkSession}
import scala.util.control.Breaks._

object SparkIOUtils {

  def createUnionDataFrame(existingHdfsPaths: Seq[String], dataIOParameters: Map[String, String] = Map(), jobConf: JobConf, dataLoaderHandlers: List[DataLoaderHandler]): DataFrame = {
    var dfOpt: Option[DataFrame] = None
    val firstPath = existingHdfsPaths.head //Making the assumption that all paths are the same format. If not, will break during the createUnionDataFrame step.
    breakable {
      for (dataLoaderHandler <- dataLoaderHandlers) {
        if (dataLoaderHandler.validatePath(firstPath)) {
          dfOpt = Some(dataLoaderHandler.createUnionDataFrame(existingHdfsPaths,dataIOParameters,jobConf))
          break
        }
      }
    }
    val df = dfOpt match {
      case Some(_) => dfOpt.get
      case _ => {
        // existingHdfsPaths may be folder or file with suffix
        // Currently only support parquet file but not folder with parquet files
        val format = FileFormat.getTypeForUnionDF(existingHdfsPaths, dataIOParameters)
        FileFormat.loadHdfsDataFrame(format, existingHdfsPaths)
      }
    }
    df
  }

  def createDataFrame(location: DataLocation, dataIOParams: Map[String, String] = Map(), jobConf: JobConf, dataLoaderHandlers: List[DataLoaderHandler]): DataFrame = {
    var dfOpt: Option[DataFrame] = None
    breakable {
      for (dataLoaderHandler <- dataLoaderHandlers) {
        if (dataLoaderHandler.validatePath(location.getPath)) {
          dfOpt = Some(dataLoaderHandler.createDataFrame(location.getPath,dataIOParams,jobConf))
          break
        }
      }
    }
    val df = dfOpt match {
      case Some(_) => dfOpt.get
      case _ => {
        location.loadDf(SparkSession.builder.getOrCreate, dataIOParams)
      }
    }
    df
  }

  def writeDataFrame( outputDF: DataFrame, outputLocation: DataLocation, parameters: Map[String, String] = Map(), dataLoaderHandlers: List[DataLoaderHandler]): DataFrame = {
    var dfWritten = false
    breakable {
      for (dataLoaderHandler <- dataLoaderHandlers) {
        outputLocation match {
          case SimplePath(path) => {
            if (dataLoaderHandler.validatePath(path)) {
              dataLoaderHandler.writeDataFrame(outputDF, path, parameters)
              dfWritten = true
              break
            }
          }
        }
      }
    }
    if(!dfWritten) {
      outputLocation match {
        case SimplePath(path) => {
          val output_format = outputDF.sqlContext.getConf("spark.feathr.outputFormat", "avro")
          // if the output format is set by spark configurations "spark.feathr.outputFormat"
          // we will use that as the job output format; otherwise use avro as default for backward compatibility
          if(!outputDF.isEmpty) {
            outputDF.write.mode(SaveMode.Overwrite).format(output_format).save(path)
          }
        }
        case _ => outputLocation.writeDf(SparkSession.builder().getOrCreate(), outputDF, None)
      }
    }
    outputDF
  }

  def createGenericRDD(inputPath: String, dataIOParameters: Map[String, String], jobConf: JobConf): RDD[GenericRecord] = ???

  val OUTPUT_SCHEMA = "output.schema"
  val DATA_FORMAT = "data.format"
  val OUTPUT_PARALLELISM = "output.parallelism"
  val SPLIT_SIZE = "split.size"
  val OVERWRITE_MODE = "override.mode"
  val FILTER_EXP = "filter.exp"
}
