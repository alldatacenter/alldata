package org.apache.spark.customized

import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema
import org.apache.spark.sql.types._

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream};

import org.apache.spark.sql.types.{DataType, StructType}

object CustomGenericRowWithSchema {
  UDTRegistration.register(classOf[CustomGenericRowWithSchema].getName, classOf[GenericRowWithSchemaUDT].getName)
}

/**
 * A customized subclass of Row used in streaming source.
 * This is used to support user-provided schema in streaming/kafka sources.
 * @param values values for row
 * @param inputSchema schema for row
 */
class CustomGenericRowWithSchema(values: Array[Any], inputSchema: StructType)
  extends GenericRowWithSchema(values, inputSchema) {
  /** No-arg constructor for serialization. */
  protected def this() = this(null, null)
}

// User-defined type for customized row
class GenericRowWithSchemaUDT extends UserDefinedType[CustomGenericRowWithSchema] {
  override def sqlType: DataType = org.apache.spark.sql.types.BinaryType
  override def serialize(obj: CustomGenericRowWithSchema): Any = {
    val byteStream = new ByteArrayOutputStream()
    val objectStream = new ObjectOutputStream(byteStream)
    objectStream.writeObject(obj)
    byteStream.toByteArray
  }
  override def deserialize(datum: Any): CustomGenericRowWithSchema = {
    val byteStream = new ByteArrayInputStream(datum.asInstanceOf[Array[Byte]])
    val objectStream = new ObjectInputStream(byteStream)
    val row = objectStream.readObject()
    row.asInstanceOf[CustomGenericRowWithSchema]
  }
  override def userClass: Class[CustomGenericRowWithSchema] = classOf[CustomGenericRowWithSchema]
}

