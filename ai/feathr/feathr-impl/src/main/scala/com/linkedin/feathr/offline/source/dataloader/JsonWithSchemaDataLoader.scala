package com.linkedin.feathr.offline.source.dataloader

import com.fasterxml.jackson.databind.ObjectMapper
import com.jasonclawson.jackson.dataformat.hocon.HoconFactory
import com.linkedin.feathr.offline.util.SourceUtils.processSanityCheckMode
import org.apache.avro.Schema
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, SparkSession}

import scala.io.Source
import scala.reflect.ClassTag

/**
 * load data from a path which hosts files in separate json format for test framework, e.g.
 * /path/
 *     | mockData.json
 *     | schema.avsc
 * @param ss spark session
 * @param path path to load
 */
private[offline] class JsonWithSchemaDataLoader(ss: SparkSession, path: String) extends DataLoader {

  /**
   * get the schema of the source. It's only used in the deprecated DataSource.getDataSetAndSchema
   * @return an Avro Schema
   */
  override def loadSchema(): Schema = {
    val schemaPath = path + "/schema.avsc"
    val schemaAsString = loadFileAsString(schemaPath)
    new Schema.Parser().parse(schemaAsString)
  }

  /**
   * load the source data as dataframe.
   * @return an dataframe
   */
  override def loadDataFrame(): DataFrame = {
    val res = parseJsonAsAvroRDD()
    val df = AvroJsonDataLoader.convertRDD2DF(ss, res)
    processSanityCheckMode(ss, df)
  }

  /**
   * read the file content as a string
   *
   * @param path the path to the file
   * @return the file content
   */
  private def loadFileAsString(path: String): String = {
    val bufferedSource = Source.fromFile(path)
    val content = bufferedSource.mkString
    bufferedSource.close
    content
  }

  /**
   * parse the input dataArray json as RDD
   * @param tag the class tag for the object in the RDD. It should be GenericRecord or a subclass of SpecificRecord.
   * @return the converted rdd and avro schema
   */
  private def parseJsonAsAvroRDD[T]()(implicit tag: ClassTag[T]): (RDD[_], Schema) = {
    val schema = loadSchema()
    val dataPath = path + "/mockData.json"
    val dataArrayAsJson = loadFileAsString(dataPath)
    val jackson = new ObjectMapper(new HoconFactory)
    val jsonDataArray = jackson.readTree("{ data:" + dataArrayAsJson + " }").get("data")
    AvroJsonDataLoader.parseJsonAsAvroRDD(ss, jsonDataArray, schema.toString)
  }
}
