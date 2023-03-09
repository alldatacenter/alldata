package com.databricks.spark.avro
import com.databricks.spark.avro.SchemaConverters.createConverterToSQL
import org.apache.avro.Schema
import org.apache.spark.sql.types._

// We put the class within package com.databricks.spark.avro, as we need to access a private method (createConverterToSQL)
object SchemaConverterUtils {
  def converterSql(schema: Schema, sqlType: DataType): AnyRef => AnyRef = {
    createConverterToSQL(schema, sqlType)
  }
}
