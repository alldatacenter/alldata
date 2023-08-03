package com.linkedin.feathr.offline.source.dataloader

import com.linkedin.avroutil1.compatibility.AvroCompatibilityHelper
import com.linkedin.feathr.offline.TestFeathr
import com.linkedin.feathr.offline.util.LocalFeatureJoinUtils
import org.apache.avro.Schema
import org.apache.spark.sql.Row
import org.testng.Assert.assertEquals
import org.testng.annotations.Test

import scala.collection.JavaConverters._

/**
 * unit tests for [[JsonWithSchemaDataLoader]]
 */
class TestJsonWithSchemaDataLoader extends TestFeathr {

  @Test(description = "test loading dataframe with JsonWithSchemaDataLoader")
  def testLoadDataFrame() : Unit = {
    val dataLoader = new JsonWithSchemaDataLoader(ss, LocalFeatureJoinUtils.getMockPath("simple-obs2"))
    val df = dataLoader.loadDataFrame()
    val expectedRows = Array(Row(1L), Row(2L), Row(3L))
    assertEquals(df.collect(), expectedRows)
  }


  @Test(description = "test loading Avro schema with JsonWithSchemaDataLoader")
  def testLoadSchema() : Unit = {
    val dataLoader = new JsonWithSchemaDataLoader(ss, LocalFeatureJoinUtils.getMockPath("simple-obs2"))
    val schema = dataLoader.loadSchema()

    val expectedFields = List(
      AvroCompatibilityHelper.createSchemaField("mId", Schema.create(Schema.Type.LONG), null, null)
    ).asJava
    val expectedSchema = Schema.createRecord("FeathrTest", null, null, false)
    expectedSchema.setFields(expectedFields)
    assertEquals(schema, expectedSchema)
  }
}
