package com.linkedin.feathr.offline.source.dataloader

import org.apache.avro.{AvroRuntimeException, Schema}
import org.apache.avro.Schema.Field
import org.apache.avro.generic.GenericRecord

/**
 * a wrapper of [[GenericRecord]] that can fetch a lowercase column with uppercase key.
 * This is used to solve the lowercase field names

 * @param record the original GenericRecord
 */
private[offline] class CaseInsensitiveGenericRecordWrapper(private val record: GenericRecord) extends GenericRecord {

  override def put(key: String, v: Any): Unit = {
    val field = getField(key)
    if (field == null) throw new AvroRuntimeException("Not a valid schema field: " + key)
    put(field.pos(), v)
  }

  override def get(key: String): AnyRef = {
    val field = getField(key)
    if (field == null) null
    else get(field.pos())
  }

  override def put(i: Int, v: Any): Unit = record.put(i, v)

  override def get(i: Int): AnyRef = {
    wrap(record.get(i))
  }

  override def getSchema: Schema = record.getSchema

  override def equals(obj: Any): Boolean = obj match {
    case r: CaseInsensitiveGenericRecordWrapper => equals(r.record)
    case _ => record.equals(obj)
  }

  override def hashCode(): Int = record.hashCode()

  /**
   * wrap as CaseInsensitiveGenericRecordWrapper if the input is a GenericRecord, otherwise, return the input object
   * @param obj input object
   * @return the wrapped object
   */
  private def wrap(obj: AnyRef): AnyRef = {
    obj match {
      case r: GenericRecord => new CaseInsensitiveGenericRecordWrapper(r)
      case _ => obj
    }
  }

  /**
   * get the field by its name. If it's not found, try to find by lowercase name
   * @param key the field name
   * @return the field. Return null if it's not found
   */
  private def getField(key: String): Field = {
    val field = record.getSchema.getField(key)
    if (field != null) return field
    record.getSchema.getField(key.toLowerCase)
  }
}
