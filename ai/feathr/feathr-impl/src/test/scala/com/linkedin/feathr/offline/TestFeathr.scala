package com.linkedin.feathr.offline

import com.linkedin.feathr.common
import com.linkedin.feathr.common.{AlienMvelContextUDFs, JoiningFeatureParams}
import com.linkedin.feathr.offline.client.FeathrClient
import com.linkedin.feathr.offline.config.{FeathrConfig, FeathrConfigLoader}
import com.linkedin.feathr.offline.mvel.plugins.FeathrExpressionExecutionContext
import com.linkedin.feathr.offline.plugins.{AlienFeatureValue, AlienFeatureValueTypeAdaptor}
import com.linkedin.feathr.offline.util.FeathrTestUtils
import org.apache.avro.generic.GenericRecord
import org.apache.hadoop.conf.Configuration
import org.apache.spark.sql.SparkSession
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.{AfterClass, BeforeClass}

import scala.collection.convert.wrapAll._
import scala.reflect.ClassTag

// abstract class for all feathr tests
abstract class TestFeathr extends TestNGSuite {
  protected var ss: SparkSession = _
  protected var conf: Configuration = _
  protected var feathr: FeathrClient = _
  val FeathrFeatureNamePrefix = "__feathr_feature_"
  protected var feathrConfigLoader: FeathrConfig = FeathrConfigLoader()

  private val mvelContext = new FeathrExpressionExecutionContext()

  @BeforeClass
  def setup(): Unit = {
    setupSpark()
    mvelContext.setupExecutorMvelContext(classOf[AlienFeatureValue],
      new AlienFeatureValueTypeAdaptor(),
      ss.sparkContext,
      Some(classOf[AlienMvelContextUDFs].asInstanceOf[Class[Any]])
    )
  }

  /**
   * Get SparkSession and Hadoop Configuration for tests.
   * By default, get SparkSession without Hive support.
   * If the tests require Hive support, please override this function.
   */
  def setupSpark(): Unit = {
    if (ss == null) {
      conf = TestFeathr.getOrCreateConf
      ss = TestFeathr.getOrCreateSparkSession
    }
  }

  @BeforeClass
  def setFeathrConfig(): Unit = {} // will be overridden by the actual test classes

  @AfterClass
  def cleanup(): Unit = {
  }

  /**
   * destruct the SparkSession used
   */
  def destructSparkSession(): Unit = {
    if (ss != null) {
      ss.stop()
      ss = null
    }
    System.clearProperty("spark.driver.port")
    System.clearProperty("spark.hostport")
  }
}

object TestFeathr {

  final val SPARK_DEFAULT_PARALLELISM: String = "4"
  final val SPARK_SQL_SHUFFLE_PARTITIONS: String = SPARK_DEFAULT_PARALLELISM

  private var _ss: SparkSession = _
  private var _conf: Configuration = _
  private var _ssWithHive: SparkSession = _
  private var _confWithHive: Configuration = _

  def getOrCreateSparkSession: SparkSession = {
    if (_ss == null) {
      _ss = constructSparkSession()
    }
    _ss
  }

  def getOrCreateConf: Configuration = {
    if (_conf == null) {
      getOrCreateSparkSession
      _conf = _ss.sparkContext.hadoopConfiguration
    }
    _conf
  }

  def getOrCreateSparkSessionWithHive: SparkSession = {
    if (_ssWithHive == null) {
      _ssWithHive = constructSparkSession()
    }
    _ssWithHive
  }

  def getOrCreateConfWithHive: Configuration = {
    if (_confWithHive == null) {
      getOrCreateSparkSessionWithHive
      _confWithHive = _ssWithHive.sparkContext.hadoopConfiguration
    }
    _confWithHive
  }

  private def constructSparkSession() = {
    FeathrTestUtils.getSparkSession()
  }

  def flatRecordToStringMap(record: GenericRecord): Map[String, String] = {
    record.getSchema.getFields
      .map(field => {
        val name = field.name
        (name, record.get(name).toString)
      })
      .toMap
  }

  type FV = Option[common.FeatureValue]
  def fv[T](map: Map[String, T])(implicit t: Numeric[T]): FV = Some(FeatureValue(map.mapValues(t.toFloat)))
  def fv(entries: (String, Double)*): FV = fv(entries.toMap)
  def numeric(n: Double): FV = fv("" -> n)
  def numeric(n: Int): FV = fv("" -> n.toDouble)
  def categorical(head: String, tail: String*): FV = fv((head +: tail).map(_ -> 1.0).toMap)
  def categorical(head: Int, tail: Int*): FV = fv((head +: tail).map(_.toString -> 1.0).toMap)
  def binary(b: Int): FV = b match {
    case 1 => numeric(1)
    case 0 => fv()
  }
}

/**
 * This case class describes feature record after join process
 */
case class FeathrJointFeatureRecord[L: ClassTag](observation: L, joinedFeatures: Map[JoiningFeatureParams, common.FeatureValue])
