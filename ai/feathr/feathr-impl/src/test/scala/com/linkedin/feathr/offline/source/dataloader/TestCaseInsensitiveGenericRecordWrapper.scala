package com.linkedin.feathr.offline.source.dataloader

import com.linkedin.avroutil1.compatibility.AvroCompatibilityHelper
import org.apache.avro.generic.{GenericData, GenericRecord}
import org.apache.avro.{AvroRuntimeException, Schema}
import org.scalatest.testng.TestNGSuite
import org.testng.Assert.{assertEquals, assertFalse, assertTrue}
import org.testng.annotations.Test

import scala.collection.JavaConverters._

/**
 * unit test for CaseInsensitiveGenericRecordWrapper
 */
class TestCaseInsensitiveGenericRecordWrapper extends TestNGSuite{

  val record = createRecord()
  val childRecord = record.get("child").asInstanceOf[GenericRecord]
  val wrapperRecord = new CaseInsensitiveGenericRecordWrapper(record)

  @Test(description = "test get() by field index")
  def testGetByIndex(): Unit = {
    assertEquals(wrapperRecord.get(0), 1)
    assertEquals(wrapperRecord.get(1), new CaseInsensitiveGenericRecordWrapper(childRecord))
  }

  @Test(description = "test get() by field name")
  def testGetByName(): Unit = {
    assertEquals(wrapperRecord.get("a"), 1)
    assertEquals(wrapperRecord.get("A"), 1)
    assertEquals(wrapperRecord.get("child"), new CaseInsensitiveGenericRecordWrapper(childRecord))
    assertEquals(wrapperRecord.get("Child"), new CaseInsensitiveGenericRecordWrapper(childRecord))
  }

  @Test(description = "test put() by field index")
  def testPutByIndex(): Unit = {
    val recordForWrite = new GenericData.Record(record, false)
    val wrapper = new CaseInsensitiveGenericRecordWrapper(recordForWrite)
    wrapper.put(0, 2)
    assertEquals(recordForWrite.get(0), 2)
  }

  @Test(description = "test put() by field name")
  def testPutByName(): Unit = {
    val recordForWrite = new GenericData.Record(record, false)
    val wrapper = new CaseInsensitiveGenericRecordWrapper(recordForWrite)
    wrapper.put("a", 2)
    assertEquals(recordForWrite.get("a"), 2)
    wrapper.put("A", 2)
    assertEquals(recordForWrite.get("a"), 2)
    assertThrows[AvroRuntimeException] { wrapper.put("b", 1) }
  }

  @Test(description = "test getSchema")
  def testGetSchema(): Unit = {
    assertEquals(wrapperRecord.getSchema, record.getSchema)
  }

  @Test(description = "test hashCode")
  def testHashcode(): Unit = {
    assertEquals(wrapperRecord.hashCode(), record.hashCode())
  }

  @Test(description = "test equals")
  def testEquals(): Unit = {
    assertTrue(wrapperRecord.equals(record))
    assertTrue(wrapperRecord.equals(new CaseInsensitiveGenericRecordWrapper(record)))
    assertTrue(wrapperRecord.equals(new CaseInsensitiveGenericRecordWrapper(wrapperRecord)))
    assertFalse(wrapperRecord.equals(childRecord))
  }

  /**
   * create GenericRecord for testing
   * @return
   */
  def createRecord(): GenericData.Record = {
    val childSchema = Schema.createRecord(List(AvroCompatibilityHelper.createSchemaField("f", Schema.create(Schema.Type.INT), null, null)).asJava)
    val childRecord = new GenericData.Record(childSchema)
    childRecord.put("f", 2)
    val schema =
      Schema.createRecord(List(AvroCompatibilityHelper.createSchemaField("a", Schema.create(Schema.Type.INT), null, null), AvroCompatibilityHelper.createSchemaField("child", childSchema, null, null)).asJava)
    val record = new GenericData.Record(schema)
    record.put("a", 1)
    record.put("child", childRecord)
    record
  }
}
