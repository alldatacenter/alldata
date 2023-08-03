package com.linkedin.feathr.offline.source.dataloader

import com.databricks.spark.avro.SchemaConverterUtils
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.jasonclawson.jackson.dataformat.hocon.HoconFactory
import com.linkedin.feathr.offline.util.SourceUtils.processSanityCheckMode
import org.apache.avro.Schema
import org.apache.avro.generic.{GenericDatumReader, GenericRecord}
import org.apache.avro.io.DecoderFactory
import org.apache.avro.specific.{SpecificDatumReader, SpecificRecordBase}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.avro.SchemaConverters
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.{DataFrame, Row, SparkSession}

import java.io.{ByteArrayInputStream, DataInputStream}
import java.util
import scala.collection.convert.wrapAll._
import scala.io.Source
import scala.reflect.ClassTag
import scala.util.Try

/**
 * Load .avro.json file as datafile for unit test
 * @param ss spark session
 * @param path  input resource path
 */
private[offline] class AvroJsonDataLoader(ss: SparkSession, path: String) extends DataLoader {

  /**
   * get the schema of the source. It's only used in the deprecated DataSource.getDataSetAndSchema
   * @return an Avro Schema
   */
  override def loadSchema(): Schema = {
    AvroJsonDataLoader.loadJsonFileAsAvroToRDD(ss, path)._2
  }

  /**
   * load the source data as dataframe.
   * @return an dataframe
   */
  override def loadDataFrame(): DataFrame = {
    val res = AvroJsonDataLoader.loadJsonFileAsAvroToRDD(ss, path)
    val df = AvroJsonDataLoader.convertRDD2DF(ss, res)
    processSanityCheckMode(ss, df)
  }
}

private[offline] object AvroJsonDataLoader {

  /**
   * Convert rdd with schema to dataframe
   * @param ss spark session
   * @param res (RDD[GenericRecord], schema)
   * @return converted dataframe
   */
  def convertRDD2DF(ss: SparkSession, res: (RDD[_], Schema)): DataFrame = {
    val schema = res._2
    val sqlType = SchemaConverters.toSqlType(schema).dataType.asInstanceOf[StructType]
    val converter = SchemaConverterUtils.converterSql(schema, sqlType)
    // Here we need to collect the rows and apply the converter instead of applying the converter to the RDD,
    // because the converter may not be serializable and unable to distribute to the executors.
    // We will trigger a action and collect the data in the driver. It' OK to do so because this function is only
    // used to load local testing files.
    val rows = res._1
      .asInstanceOf[RDD[GenericRecord]]
      .collect()
      .flatMap(record => {
        Try(converter(record).asInstanceOf[Row]).toOption
      })
    ss.createDataFrame(util.Arrays.asList(rows: _*), sqlType)
  }

  /**
   * parse the input dataArray json as RDD
   * @param jsonDataNode input data array as json node
   * @param schemaAsString avro schema of the input data array
   * @return the converted rdd and avro schema
   */
  def parseJsonAsAvroRDD[T](ss: SparkSession, jsonDataNode: JsonNode, schemaAsString: String)(implicit tag: ClassTag[T]): (RDD[_], Schema) = {
    val sc = ss.sparkContext
    val schema = Schema.parse(schemaAsString)
    val records = jsonDataNode.map(jsonNode => {
      val input = new ByteArrayInputStream(jsonNode.toString.getBytes)
      val din = new DataInputStream(input)
      val decoder = DecoderFactory.get().jsonDecoder(schema, din)
      if (!classOf[SpecificRecordBase].isAssignableFrom(scala.reflect.classTag[T].runtimeClass.asInstanceOf[Class[T]])) {
        val reader = new GenericDatumReader[GenericRecord](schema)
        reader.read(null, decoder)
      } else {
        val reader = new SpecificDatumReader[T](scala.reflect.classTag[T].runtimeClass.asInstanceOf[Class[T]])
        reader.read(null.asInstanceOf[T], decoder)
      }
    })
    (sc.parallelize(records.toSeq), schema)
  }

  /**
   * Load .avro.json file as RDD
   *
   * @param tag the class tag for the object in the RDD. It should be GenericRecord or a subclass of SpecificRecord.
   * @return return result RDD and schema
   */
  def loadJsonFileAsAvroToRDD[T](ss: SparkSession, path: String)(implicit tag: ClassTag[T]): (RDD[_], Schema) = {
    val sc = ss.sparkContext
    require(sc.isLocal)
    require(path.endsWith(".avro.json"))
    val contents = Source.fromResource(path).getLines().mkString
    val jackson = new ObjectMapper(new HoconFactory)
    val tree = jackson.readTree(contents)
    val jsonDataArray = tree.get("data")
    parseJsonAsAvroRDD(ss, jsonDataArray, tree.get("schema").toString)
  }

}
