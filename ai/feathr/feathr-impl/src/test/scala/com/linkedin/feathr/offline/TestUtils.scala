package com.linkedin.feathr.offline

import com.linkedin.feathr.offline.config.FeatureJoinConfig
import com.linkedin.feathr.offline.util.datetime.{DateTimeInterval, OfflineDateTimeUtils}
import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord
import org.apache.avro.mapred.AvroKey
import org.apache.avro.mapreduce.{AvroJob, AvroKeyOutputFormat}
import org.apache.hadoop.io.NullWritable
import org.apache.hadoop.mapred.JobConf
import org.apache.hadoop.mapreduce.Job
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{Row, SparkSession}
import org.testng.Assert.assertTrue

import java.io.{File, PrintWriter}
import java.time.LocalDateTime
import scala.collection.mutable
import scala.reflect.ClassTag

object TestUtils {  /**
  * Write a given list of Avro GenericRecords to a given local path
  *
  * @param ss a local SparkSession
  * @param mockData list of Avro specific records to write to disk
  * @param outputPath path to write Avro records to, relative to the root of the submodule.
  */
  def writeGenericRecordToFile(ss: SparkSession, mockData: Seq[GenericRecord], outputPath: String, schemaString: String): Unit = {
    val avroRDD: RDD[GenericRecord] = ss.sparkContext.parallelize(mockData)
    val jobConf: JobConf = new JobConf()
    val job = Job.getInstance(jobConf)

    val parser = new Schema.Parser()
    val schema = parser.parse(schemaString)
    AvroJob.setOutputKeySchema(job, schema)

    // Serialize the Avro RDD onto HDFS
    avroRDD.map(record => (new AvroKey[GenericRecord](record), NullWritable.get()))
      .saveAsNewAPIHadoopFile(outputPath,
        classOf[AvroKey[GenericRecord]],
        classOf[NullWritable],
        classOf[AvroKeyOutputFormat[GenericRecord]],
        job.getConfiguration)
  }


  val TEST_ROOT = "src/test/"

  /**
   * Write a given input string to a given output path
   *
   * @param text       the text to write to the output path
   * @param outputPath the output path
   */
  def writeToFile(text: String, outputPath: String): Unit = {

  val outputFolder = new File(outputPath)
    if (!outputFolder.getParentFile.exists) outputFolder.getParentFile.mkdirs

    val pw = new PrintWriter(new File(outputPath))
    pw.write(text)
    pw.close()
  }


  /**
   * Util that parses string join config and returns a [[FeatureJoinConfig]].
   */
  def parseFeatureJoinConfig(joinConfig: String): FeatureJoinConfig = FeatureJoinConfig.parseJoinConfig(joinConfig)


  /**
   * Use to time and print the running time of a code block
   *
   * @param block code blocks to get running time
   * @tparam R return type of the code
   * @return result of the evaluated code
   */
  def collectTime[R](block: => R): R = {
    val start = System.currentTimeMillis()
    val result = block
    val end = System.currentTimeMillis()
    result
  }

  // Build 1d tensor row in FDS format
  def build1dSparseTensorFDSRow(dimensions: Array[_], values: Array[_]): Row = {
    Row(mutable.WrappedArray.make(dimensions), mutable.WrappedArray.make(values))
  }

  /**
   * This function is borrowed from org.scalatest.Assertions.
   * Ensure that an expected exception is thrown by the passed function value. The thrown exception must be an instance of the
   * type specified by the type parameter of this method. This method invokes the passed
   * function.
   *
   * @param f the function value that should throw the expected exception
   * @param classTag an implicit <code>ClassTag</code> representing the type of the specified
   * type parameter.
   */
  def assertThrows[T <: AnyRef](f: => Any)(implicit classTag: ClassTag[T]): Unit = {
    val clazz = classTag.runtimeClass
    val threwExpectedException = try {
      f
      false
    } catch {
      case u: Throwable =>
        assertTrue(clazz.isAssignableFrom(u.getClass), s"wrong exception thrown. expected: ${clazz.getName} but found ${u.getClass.getName}.")
        true
    }
    assertTrue(threwExpectedException, s"exception expected ${clazz.getName}.")
  }

  /**
   * create an interval with daily start and end string
   */
  def createDailyInterval(start: String, end: String): DateTimeInterval = {
    new DateTimeInterval(OfflineDateTimeUtils.createTimeFromString(start, "yyyy-MM-dd"), OfflineDateTimeUtils.createTimeFromString(end, "yyyy-MM-dd"))
  }

  /**
   * create an interval with hourly start and end string
   */
  def createHourlyInterval(start: String, end: String): DateTimeInterval = {
    new DateTimeInterval(OfflineDateTimeUtils.createTimeFromString(start, "yyyy-MM-dd-HH"), OfflineDateTimeUtils.createTimeFromString(end, "yyyy-MM-dd-HH"))
  }

  /**
   * Utility method for creating interval
   */
  def createIntervalFromLocalTime(startTime: LocalDateTime, endTime: LocalDateTime): DateTimeInterval = {
    val start = startTime.atZone(OfflineDateTimeUtils.DEFAULT_ZONE_ID)
    val end = endTime.atZone(OfflineDateTimeUtils.DEFAULT_ZONE_ID)
    new DateTimeInterval(start, end)
  }
}
