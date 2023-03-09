package com.linkedin.feathr.offline.config.location

import com.linkedin.feathr.offline.TestFeathr
import com.linkedin.feathr.offline.source.accessor.DataSourceAccessor
import com.linkedin.feathr.offline.source.{DataSource, SourceFormatType}
import org.apache.spark.sql.Row
import org.apache.spark.sql.types.{IntegerType, StringType, StructField, StructType}
import org.testng.Assert.assertEquals
import org.testng.annotations.{BeforeClass, Test}

import scala.collection.JavaConversions._

class TestSparkSqlLocation extends TestFeathr{
  @BeforeClass
  override def setup(): Unit = {
    ss = TestFeathr.getOrCreateSparkSessionWithHive
    super.setup()
  }

  @Test(description = "It should read a Spark SQL as the DataSource")
  def testCreateWithFixedPath(): Unit = {
    val schema = StructType(Array(
      StructField("language", StringType, true),
      StructField("users", IntegerType, true)
    ))
    val rowData= Seq(Row("Java", 20000),
      Row("Python", 100000),
      Row("Scala", 3000))
    val tempDf = ss.createDataFrame(rowData, schema)
    tempDf.createTempView("test_spark_sql_table")

    val loc = SparkSqlLocation(sql = Some("select * from test_spark_sql_table order by users asc"))
    val dataSource = DataSource(loc, SourceFormatType.FIXED_PATH)
    val accessor = DataSourceAccessor(ss, dataSource, None, None, failOnMissingPartition = false, dataPathHandlers=List())
    val df = accessor.get()
    val expectedRows = Array(
      Row("Scala", 3000),
      Row("Java", 20000),
      Row("Python", 100000)
    )
    assertEquals(df.collect(), expectedRows)

    ss.sqlContext.dropTempTable("test_spark_sql_table")
  }
}
