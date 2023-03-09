package com.linkedin.feathr.offline.source.dataloader

import com.fasterxml.jackson.dataformat.csv.{CsvMapper, CsvSchema}
import com.linkedin.avroutil1.compatibility.AvroCompatibilityHelper
import org.apache.avro.Schema
import org.apache.avro.generic.GenericData.{Array, Record}
import org.apache.avro.generic.GenericRecord
import org.apache.avro.util.Utf8
import org.apache.spark.sql.{DataFrame, SparkSession}

import java.io.File
import scala.collection.JavaConverters._
import scala.collection.convert.wrapAll._
import com.linkedin.feathr.offline.util.DelimiterUtils.checkDelimiterOption
import com.linkedin.feathr.offline.util.SourceUtils.processSanityCheckMode

import scala.io.Source

/**
 * Load csv file for testing.
 * @param ss the spark session
 * @param path input resource path
 */
private[offline] class CsvDataLoader(ss: SparkSession, path: String) extends DataLoader {

  /**
   * get the schema of the source. It's only used in the deprecated DataSource.getDataSetAndSchema
   * @return an Avro Schema
   */
  override def loadSchema(): Schema = {
    doLoadCsvDocumentLikeAvro()._2
  }

  /**
   * load the source data as dataframe.
   * @return an dataframe
   */
  override def loadDataFrame(): DataFrame = {

    // Get csvDelimiterOption set with spark.feathr.inputFormat.csvOptions.sep and check if it is set properly (Only for CSV and TSV)
    val csvDelimiterOption = checkDelimiterOption(ss.sqlContext.getConf("spark.feathr.inputFormat.csvOptions.sep", ","))

    try {
      log.debug(s"Loading CSV path :${path}")
      val absolutePath = new File(path).getPath
      log.debug(s"Got absolute CSV path: ${absolutePath}, loading..")
      val df = ss.read.format("csv").option("header", "true").option("delimiter", csvDelimiterOption).load(absolutePath)
      processSanityCheckMode(ss, df)
    } catch {
      case _: Throwable =>
        log.debug(s"Loading CSV failed, retry with class loader..")
        val absolutePath = getClass.getClassLoader.getResource(path).getPath
        log.debug(s"Got absolution CSV path from class loader: ${absolutePath}, loading.. ")
        val df = ss.read.format("csv").option("header", "true").option("delimiter", csvDelimiterOption).load(absolutePath)
        processSanityCheckMode(ss, df)
    }
  }

  /**
   * load the csv file as GenericRecords and get the schema.
   * @return RDD of GenericRecord and the corresponding Avro schema.
   */
  private def doLoadCsvDocumentLikeAvro(): (Seq[GenericRecord], Schema) = {
    val contents = Source.fromInputStream(getClass.getClassLoader.getResourceAsStream(path)).mkString
    val schema = CsvSchema.emptySchema().withHeader()
    val reader = new CsvMapper().readerWithSchemaFor(classOf[java.util.Map[_, _]]).`with`(schema)
    val rows = reader.readValues[java.util.Map[_, _]](contents).toSeq

    require(rows.nonEmpty)
    val fields = rows.head.keys.map(_.asInstanceOf[String]).toSeq.sorted

    // interpret empty string "" as missing
    val rowsCleaned = rows.map(_.filter(_._2.toString != "").toMap.asJava)

    // hackishly convert to Avro GenericRecord format
    val avroSchema = Schema.createRecord(getArbitraryRecordName(fields), null, null, false)
    avroSchema.setFields(
      fields.map(AvroCompatibilityHelper.createSchemaField(_, Schema.createUnion(List(Schema.create(Schema.Type.STRING), Schema.create(Schema.Type.NULL))), null, null)))

    val genericRecords = rowsCleaned.map(coerceToAvro(avroSchema, _).asInstanceOf[GenericRecord])
    (genericRecords, avroSchema)
  }

  /**
   * Constructs an Avro GenericData compatible representation of some object, based on an Avro schema.
   * If the schema is of type RECORD, expects the object to be a Map with all the right fields.
   * If the schema is of type ARRAY, expects the object to be a List with elements of the correct element type.
   * Etc.
   */
  private def coerceToAvro(schema: Schema, obj: Any): Any = {
    schema.getType match {
      case Schema.Type.RECORD =>
        val record = new Record(schema)
        schema.getFields.foreach(field => record.put(field.name, coerceToAvro(field.schema, obj.asInstanceOf[java.util.Map[_, _]].get(field.name))))
        record
      case Schema.Type.ARRAY =>
        val list = obj.asInstanceOf[java.util.List[_]]
        val array = new Array[Any](list.size, schema.getElementType)
        list.map(coerceToAvro(schema.getElementType, _)).foreach(array.add)
        array
      case Schema.Type.STRING =>
        new Utf8(obj.asInstanceOf[String])
      case _ => obj
    }
  }

  /**
   * Avro Record schemas are required to have names. So we will make a random-ish record name.
   * (Pig's AvroStorage also makes random-ish Record names of the form "TUPLE_1", "TUPLE_2", etc.)
   */
  private def getArbitraryRecordName(x: AnyRef): String = {
    "AnonRecord_" + Integer.toHexString(x.hashCode)
  }
}