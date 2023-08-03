package com.linkedin.feathr.offline

import org.apache.spark.sql.types.{ArrayType, StructField, StructType}
import org.apache.spark.sql.{DataFrame, Row}

import scala.collection.mutable

/**
 * Some utils functions that can be used to print the 'code' that can be used to validate a dataframe
 */
private[offline] object ValidationCodeGenerator {

  /**
   * This util function produce the 'code' that can be used to validate the output DataFrame 'df'.
   * You can copy/paste the output of this function into your test case.
   * PLEASE make sure that the output value is correct/expected.
   * example output:
  val expectedDf = ss.createDataFrame(
      ss.sparkContext.parallelize(
        Seq(
          Row(
            // x
            "1",
            // a_derived_ct
            Map("us" -> 1.0f),
            // a_ct
            "us",
            // a_sample
            "us:100")),
      StructType(
        List(
          StructField("x", StringType, true),
          StructField("a_derived_ct", MapType(StringType, FloatType, true), true),
          StructField("a_ct", StringType, true),
          StructField("a_sample", StringType, true))))
    def cmpFunc(row: Row): String = row.get(0).toString
    FeathrTestUtils.assertDataFrameApproximatelyEquals(df, expectedDf, cmpFunc)
   * @param sparkSessionName spark session name you want to use in the code, e.g. "ss"
   * @param actualDFName actual dataframe name you want to compare, e.g. "df"
   * @param df the actual dataframe instance to validate
   * @param documentFeatureName whether document feature name in the data or not, useful if you have many features
   * @return the 'code' that can be used to validate the input dataframe
   */
  def printDFInCodeFormat(sparkSessionName: String, actualDFName: String, df: DataFrame, documentFeatureName: Boolean): String = {
    val code = s"""
                  |val expectedDf = ${sparkSessionName}.createDataFrame(
                  |  ${printDataInCodeFormat(sparkSessionName, df, documentFeatureName)},
                  |  ${printSchemaInCodeFormat(df)}
                  |)
                  |def cmpFunc(row: Row): String = row.get(0).toString
                  |FeathrTestUtils.assertDataFrameApproximatelyEquals(${actualDFName}, expectedDf, cmpFunc)
                  |
                  |""".stripMargin
    code
  }

  // return the code that can be used to construct the schema of input dataframe
  private def printSchemaInCodeFormat(df: DataFrame): String = {
    val sb = new mutable.StringBuilder()
    val fields = df.schema.map { (f: StructField) =>
      printStructFieldInCodeFormat(f)
    }
    sb.append("StructType(")
    sb.append("  List(\n")
    sb.append("    " + fields.mkString(",\n"))
    sb.append("  )")
    sb.append(")")
    sb.toString()
  }

  // return the code that can be used to construct the schema of a StructField
  private def printStructFieldInCodeFormat(field: StructField): String = {
    val sb = new mutable.StringBuilder()
    field.dataType match {
      case structField: StructType =>
        val fields = structField.map { (f: StructField) =>
          printStructFieldInCodeFormat(f)
        }
        sb.append(s"""StructField("${field.name}", StructType(""")
        sb.append("  List(\n")
        sb.append("    " + fields.mkString(",\n"))
        sb.append("  )")
        sb.append("))")
      case arrayField: ArrayType =>
        arrayField.elementType match {
          case struct: StructType =>
            val fields = s"""
                            | StructType(List(
                            | ${struct.fields.map(printStructFieldInCodeFormat).mkString(", ")}
                            | )
                            | )
                            |""".stripMargin
            sb.append(s"""StructField("${field.name}", ArrayType(${fields}))""")
          case _ =>
            val fields = s""" StructField("${field.name}", ${field.dataType}, ${field.nullable}) """
            sb.append(fields)
        }
      case _ =>
        val str = s""" StructField("${field.name}", ${field.dataType}, ${field.nullable}) """
        sb.append(str)
    }
    sb.toString()
  }

  // return the code that can be used to construct the data of a field
  private def printFieldInCodeFormat(field: Any): String = {
    field match {
      case str: String => s""""${str}""""
      case arr: mutable.WrappedArray[_] =>
        val inner = printArrayInCodeFormat(arr.toArray)
        s"""mutable.WrappedArray.make(${inner})"""
      case arr: Array[Any] =>
        printArrayInCodeFormat(arr)
      case map: Map[Any, Any] =>
        s"""Map(${map.map { case (k, v) => printFieldInCodeFormat(k) + " -> " + printFieldInCodeFormat(v) }.mkString(",")})"""
      case row: Row =>
        s"""Row(${row.toSeq.map(printFieldInCodeFormat).mkString(",")})"""
      case float: Float =>
        s"""${float}f"""
      case field if field != null =>
        field.toString
      case _ => "null"
    }
  }

  // return the code that can be used to construct the data of an array field
  private def printArrayInCodeFormat(arr: Array[Any]): String = {
    val elements = arr
      .map { ele =>
        printFieldInCodeFormat(ele)
      }
      .mkString(",")
    s"""Array(${elements})"""
  }

  /**
   * return the code that can be used to construct the data of an input dataframe
   * @param sparkSessionName spark session name
   * @param df dataframe to validate
   * @param documentFeatureName document the feature name or not, useful if there're many features
   */
  private def printDataInCodeFormat(sparkSessionName: String, df: DataFrame, documentFeatureName: Boolean): String = {
    val sb = new mutable.StringBuilder()
    val rows = df.collect().map {
      case (r: Row) =>
        val fields = r.toSeq.zipWithIndex
          .map {
            case (field, idx) =>
              val document = if (documentFeatureName) {
                s"\n// ${r.schema.fieldNames(idx)}\n"
              } else {
                ""
              }
              document + printFieldInCodeFormat(field)
          }
          .mkString(",")
        s"""
           |Row(${fields})
           |""".stripMargin
    }
    sb.append(s"${sparkSessionName}.sparkContext.parallelize(")
    sb.append("Seq(\n")
    sb.append(rows.mkString(",\n"))
    sb.append(")")
    sb.append(")")
    sb.toString
  }

  /**
   * Return the schema with StructType and StructFields so it's easy to copy into code
   *
   * Suppose you have the following `sourceDF`:
   *
   * {{{
   * +--------+--------+---------+
   * |    team|   sport|goals_for|
   * +--------+--------+---------+
   * |    jets|football|       45|
   * |nacional|  soccer|       10|
   * +--------+--------+---------+
   *
   * `sourceDF.getSchemaInCodeFormat()` will return the following string
   *
   *   List(
   *     StructField("team", StringType, true),
   *     StructField("sport", StringType, true),
   *     StructField("goals_for", IntegerType, true)
   *   )
   * }}}
   */
  def getSchemaInCodeFormat(df: DataFrame): String = {
    val fields = df.schema.map { (f: StructField) =>
      s"""StructField("${f.name}", ${f.dataType}, ${f.nullable})"""
    }
    s"List(${fields.mkString(",\n")}"
  }
}
