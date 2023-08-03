package com.linkedin.feathr.offline.util

import com.linkedin.feathr.offline.config.datasource.{DataSourceConfigUtils, DataSourceConfigs}
import com.linkedin.feathr.offline.util.Transformations.sortColumns
import org.apache.avro.generic.GenericRecord
import org.apache.spark.SparkConf
import org.apache.spark.sql.internal.SQLConf
import org.apache.spark.sql.{DataFrame, Row, SparkSession}

private[offline] object FeathrTestUtils {

  /**
   * Test for approximately equality of two SparkFeaturizedDataset instances.
   * This function does not check the nullable property of dataframe fields and does not check column metadata
   * Due to Spark issue (APA-23642), after saving and loading the dataframe, the nullable
   * information and metadata will lost.  Once the issue is resolved, we should deprecate this function in favor
   * of assertDataFrameEquals
   * @param actual The observed DataFrame
   * @param expected The expected DataFrame
   * @param cmpFunc compare function, used to sort the dataframe, map a row to the sort key
   */
  def assertFDSApproximatelyEquals(actual: SparkFeaturizedDataset, expected: SparkFeaturizedDataset, cmpFunc: Row => String): Unit = {
    assert(actual != null)
    assert(expected != null)
    assertDataFrameApproximatelyEquals(actual.data, expected.data, cmpFunc)
  }

  /**
   * Test for approximately equality of two DataFrames.
   * This function does not check the nullable property of dataframe fields and does not check column metadata
   * Due to Spark issue (APA-23642), after saving and loading the dataframe, the nullable
   * information and metadata will lost.  Once the issue is resolved, we should deprecate this function in favor
   * of assertDataFrameEquals
   * @param actual The observed DataFrame
   * @param expected The expected DataFrame
   * @param cmpFunc compare function, used to sort the dataframe, map a row to the sort key
   */
  def assertDataFrameApproximatelyEquals(actual: DataFrame, expected: DataFrame, cmpFunc: Row => String): Unit = {
    assert(actual != null)
    assert(expected != null)
    val orderedActual = actual.transform(sortColumns("asc"))
    val orderedExpected = expected.transform(sortColumns("asc"))
    val actualRows = orderedActual.collect().sortBy(cmpFunc) toList
    val expectedRows = orderedExpected.collect().sortBy(cmpFunc).toList
    assert(
      actualRows.equals(expectedRows),
      s"DataFrames contents aren't equal," +
        s"\nactual:\n${toDebugString(orderedActual)}, " +
        s"\nexpected:\n${toDebugString(orderedExpected)}.")
  }

  /**
   * Create spark session
   */
  def getSparkSession(): SparkSession = {
    val sparkConf = new SparkConf()
    sparkConf.registerKryoClasses(Array(classOf[GenericRecord]))
    sparkConf.set("spark.default.parallelism", "4")
    sparkConf.set("spark.sql.shuffle.partitions", "4")
    sparkConf.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
    // sparkConf.set("spark.kryo.registrator", "org.apache.spark.serializer.AvroGenericArrayKryoRegistrator")
    // required for VPN environment
    sparkConf.set("spark.driver.host", "localhost")
    createSparkSession(
      enableHiveSupport = true,
      appName = getClass.getName,
      numThreads = "*", // Run Spark locally with as many worker threads as logical cores on the machine
      sparkConf = sparkConf)
  }

  // convert dataframe to a debug string
  private def toDebugString(df: DataFrame): String = {
    df.collect.map(row => row.schema.fieldNames.zip(row.toSeq).mkString(",")).mkString("\n")
  }

  /**
   * Create the local SparkSession used for general-purpose spark unit test
   * end-to-end read/write solution unit test.
   *
   * @param enableHiveSupport: whether to enable HiveSupport. user only needs to enable if they want to test writing to hive.
   * @param appName: name of the local spark app
   * @param numThreads: parallelism of the local spark app, the input of numThreads can either be an integer or
   *                  the character '*' which means spark will use as many worker threads as the logical cores
   * @param sparkConf: provide user specific Spark conf object rather than using default one. The appName and master
   *                   config in sparkConf will not be honored. User should set sparkConf and numThreads explicitly.
   */
  def createSparkSession(enableHiveSupport: Boolean = false, appName: String = "localtest",
                         numThreads: Any = 4, sparkConf: SparkConf = new SparkConf()): SparkSession = {

    val path = ""

    /*
     * Below configs are mimicking the default settings in our Spark cluster so user does not need to set them
     * in their prod jobs.
     *
     * Isolated class loader related configs are needed to support read and write
     * Expression Encoder config is to enable scala case class to understand avro java type as its field
     */
    val conf = sparkConf
    conf.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
    // LIHADOOP-41472
    SQLConf.get.setConfString("spark.sql.legacy.allowUntypedScalaUDF", "true")
    conf.set("spark.kryo.mllib.register", "false")
    conf.set("spark.driver.host", "localhost")
    conf.set("spark.isolated.classloader", "true")
    conf.set("spark.isolated.classes", "org.apache.hadoop.hive.ql.io.CombineHiveInputFormat$CombineHiveInputSplit")
    conf.set("spark.isolated.classloader.driver.jar", path)
    conf.set("spark.expressionencoder.org.apache.avro.specific.SpecificRecord",
      "org.apache.spark.sql.avro.AvroEncoder$")
    DataSourceConfigUtils.setupSparkConf(conf, new DataSourceConfigs())

    // conf.set("spark.kryo.registrator", "org.apache.spark.serializer.AvroGenericArrayKryoRegistrator")

    // numThreads can either be an integer or '*' which means spark will use as many worker threads as the logical cores
    if (!numThreads.isInstanceOf[Int] && !numThreads.equals("*")) {
      throw new IllegalArgumentException(
        s"Invalid arguments: The number of threads (${numThreads}) " +
          s"can only be integers or '*'.")
    }
    val sessionBuilder = SparkSession.builder().appName(appName).master(s"local[${numThreads}]").config(conf)
    sessionBuilder.getOrCreate()
  }
}
