package com.linkedin.feathr.offline.source.dataloader

import com.linkedin.avroutil1.compatibility.AvroCompatibilityHelper
import com.linkedin.feathr.offline.TestFeathr
import org.apache.avro.Schema
import org.apache.spark.sql.Row
import org.testng.Assert.assertEquals
import org.testng.annotations.Test

import scala.collection.JavaConverters._

/**
 * unit tests for [[AvroJsonDataLoader]]
 */
class TestAvroJsonDataLoader extends TestFeathr {

  @Test(description = "test loading dataframe with AvroJsonDataLoader")
  def testLoadDataFrame() : Unit = {
    val dataLoader = new AvroJsonDataLoader(ss, "simple-obs2.avro.json")
    val df = dataLoader.loadDataFrame()
    val expectedRows = Array(Row(1L), Row(2L), Row(3L))
    assertEquals(df.collect(), expectedRows)
  }


  @Test(description = "test loading Avro schema with AvroJsonDataLoader")
  def testLoadSchema() : Unit = {
    val dataLoader = new AvroJsonDataLoader(ss, "simple-obs2.avro.json")
    val schema = dataLoader.loadSchema()

    val expectedFields = List(
      AvroCompatibilityHelper.createSchemaField("mId", Schema.create(Schema.Type.LONG), null, null)
    ).asJava
    val expectedSchema = Schema.createRecord("FeathrTest", null, null, false)
    expectedSchema.setFields(expectedFields)
    assertEquals(schema, expectedSchema)
  }

  @Test(description = "test load local avro json file with union type should succeed")
  def testLoadJsonFileAsAvroToDFWithUnionType() : Unit = {
    val dataFrame = new AvroJsonDataLoader(ss, "testAvroUnionType.avro.json").loadDataFrame()
    assertEquals(dataFrame.count, 1)
  }
}
