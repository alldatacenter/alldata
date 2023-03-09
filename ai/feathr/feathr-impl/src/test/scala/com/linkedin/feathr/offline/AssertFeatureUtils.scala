package com.linkedin.feathr.offline

import com.linkedin.feathr.common
import com.linkedin.feathr.common.JoiningFeatureParams
import com.linkedin.feathr.offline.util.SparkFeaturizedDataset
import org.apache.avro.generic.{GenericArray, GenericData, GenericRecord}
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema
import org.apache.spark.sql.catalyst.parser.LegacyTypeStringParser
import org.apache.spark.sql.types.{DataType, StructType}
import org.apache.spark.sql.{DataFrame, Row}
import org.testng.Assert._

import scala.collection.convert.wrapAll._
import scala.collection.{Seq, mutable}
import scala.util.Try

object AssertFeatureUtils {
  type TermValueSet = Set[(String, Float)]
  type NTVSet = Set[(String, String, Float)]

  def getSortedFeatures(record: GenericRecord): Seq[GenericRecord] = {
    record
      .get("features")
      .asInstanceOf[GenericArray[GenericRecord]]
      .sortBy(x => x.get("name").toString + x.get("term").toString)
  }

  def assertNameTermValueRecord(input: GenericRecord, name: String, term: String, value: Double): Unit = {
    val TOLERANCE = 1e-6
    assertEquals(input.get("name").toString, name)
    assertEquals(input.get("term").toString, term)
    assertEquals(input.get("value").asInstanceOf[Double], value, TOLERANCE)
  }

  def assertJointFeature(
      input: Map[JoiningFeatureParams, common.FeatureValue],
      featureName: String,
      featureValue: common.FeatureValue,
      keyName: Seq[String] = Seq("IdInObservation")): Unit = {
    val featureKey = JoiningFeatureParams(keyName, featureName)
    assertEquals(input(featureKey), featureValue)
  }

  /**
   * Extract (name, term, value) from feature join output RDD record
   * @param record
   * @return
   */
  def extractNTV(record: GenericRecord): NTVSet = {
    record
      .get("features")
      .asInstanceOf[GenericData.Array[GenericRecord]]
      .map(item => (item.get("name").toString, item.get("term").toString, item.get("value").asInstanceOf[Number].floatValue()))
      .toSet
  }

  // Extract (name, term, value) from feature generation output Dataframe row in NTV format
  def extractNTV(row: Row): NTVSet = {
    row
      .getAs[mutable.WrappedArray[GenericRowWithSchema]]("featureList")
      .map(r => (r.getString(0), r.getString(1), r.getDouble(2).floatValue()))
      .toSet
  }

  // Extract (term, value) from feature generation output Dataframe row in NTV format
  def extractCompactNTV(row: Row): TermValueSet = {
    row
      .getAs[mutable.WrappedArray[GenericRowWithSchema]]("featureList")
      .map(r => (r.getString(0), r.getDouble(1).floatValue()))
      .toSet
  }

  // Extract (term, value) from feature join output Dataframe row in CNTV format
  def extractTermValueMap(row: Row, featureName: String, featureGroupName: String = "features"): Map[String, Double] = {
    row
      .getAs[mutable.WrappedArray[GenericRowWithSchema]](featureGroupName)
      .collect {
        case r if r.getString(0).equals(featureName) =>
          (r.getString(1), r.getDouble(2))
      }
      .toMap
  }

  /**
   * assert a row has feature as expected, the row should have format of array[struct[term,value]], which is returned
   * by feathr join 'unified_df' format
   */
  def assertUnifiedNTVSetEqual(row: Row, featureName: String, expectMap: Map[String, Float]): Unit = {
    val termValues = row
      .getAs[Seq[Row]](featureName)
      .map(tv => {
        val term = tv.get(0).toString
        val value = tv.getAs[Number](1).floatValue()
        term -> value
      })
      .toMap
    assertEquals(termValues, expectMap)
  }

  // test is two NTV set are the same, in NTV, we actually only support float precision
  def assertNTVSetEqual(first: NTVSet, second: NTVSet): Unit = {
    assertEquals(first.size, second.size)
    val sortedFirst = first.toSeq.sortBy(_.toString())
    val sortedSecond = second.toSeq.sortBy(_.toString())
    sortedFirst
      .zip(sortedSecond)
      .foreach(pair => {
        assertEquals(java.lang.Float.compare(pair._1._3.floatValue(), pair._2._3.floatValue()), 0)
      })
  }

  /**
   * Test for equality of two [[SparkFeaturizedDataset]]s.
   * This will only work if the sizes of the two DataFrames are reasonably small to fit in memory.
   *
   * @param actual The observed SparkFeaturizedDataset
   * @param expected The expected SparkFeaturizedDataset
   */
  def assertFDSEquals(actual: SparkFeaturizedDataset, expected: SparkFeaturizedDataset): Unit = {
    assertNotNull(actual)
    assertNotNull(expected)

    // Check metadata
    assertEquals(actual.fdsMetadata, expected.fdsMetadata)
    // Check data
    assertDataFrameEquals(actual.data, expected.data)
  }

  /**
   * Test for equality of two DataFrames. This will work if the sizes of the two DataFrames are reasonably small,
   * such as those that can fit in memory.
   * @param actual The observed DataFrame
   * @param expected The expected DataFrame
   */
  def assertDataFrameEquals(actual: DataFrame, expected: DataFrame): Unit = {
    assertNotNull(actual)
    assertNotNull(expected)

    // first check equality of schema without metadata
    val pairs = actual.schema.fields.sortBy(_.name).zip(expected.schema.fields.sortBy(_.name))

    pairs foreach {
      case (fieldA, fieldB) => assertEquals(fieldA.toString(), fieldB.toString(), "DataFrames have different fields in schema:")
    }

    val actualRowsAsString = rowsAsSortedString(actual.collect().toList)
    val expectRowsAsString = rowsAsSortedString(expected.collect().toList)
    assertEquals(actualRowsAsString, expectRowsAsString, "DataFrames contents aren't equal:")
  }

  def rowsAsSortedString(rows: Seq[Row]): String = {
    rows.map(_.toSeq.sortBy(field => if (field == null) "null" else field.toString).mkString).sorted.mkString
  }

  def assertRowsSortedEquals(rows: Seq[Row], otherRows: Seq[Row], message: String): Unit = {
    assertEquals(rowsAsSortedString(rows), rowsAsSortedString(otherRows), message)
  }

  def assertRowTensorEquals[K, V](row: Row, indices: List[K], values: List[V]): Unit = {
    assertEquals(row.getSeq[K](0).toList, indices)
    assertEquals(row.getSeq[V](1).toList, values)
  }

  /**
   * Naive validator method that validates the actual rows returned match the expectation.
   *
   * Following validations are done -
   * 1. actualRows passed to the method is not null
   * 2. Number of rows in actual result match the expected number.
   * 3. The data in each row is the same as the corresponding row in expected.
   * @param actualRows
   * @param expectedRows
   */
  def validateRows(actualRows: Array[Row], expectedRows: Array[GenericRowWithSchema]): Unit = {
    assertNotNull(actualRows)
    assertEquals(actualRows.length, expectedRows.length)

    for ((actual, expected) <- actualRows zip expectedRows) {
      assertEquals(actual, expected)
    }
  }

  /**
   * Approximate equality between 2 rows, based on equals from [[org.apache.spark.sql.Row]]
   *
   * @param r1  left row to compare
   * @param r2  right row to compare
   * @param tol max acceptable tolerance for comparing Double values, should be less than 1
   * @return (true if equality respected given the tolerance, false if not; error message)
   */
  def rowApproxEquals(r1: Row, r2: Row, tol: Double = 0.000001): Boolean = {
    if (r1 == null && r2 == null) {
      return true
    } else if (r1 == null) {
      return false
    } else if (r2 == null) {
      return false
    } else if (r1.length != r2.length) {
      return false
    } else {
      var idx = 0
      val length = r1.length
      while (idx < length) {
        if (r1.isNullAt(idx) != r2.isNullAt(idx)) {
          return false
        }

        if (!r1.isNullAt(idx)) {
          val o1 = r1.get(idx)
          val o2 = r2.get(idx)
          o1 match {
            case b1: Array[Byte] =>
              if (!java.util.Arrays.equals(b1, o2.asInstanceOf[Array[Byte]])) return false
            case b1: Array[_] => // Need to use Float so it can match
              if (!b1.sameElements(o2.asInstanceOf[Array[_]])) return false
            case f1: Float =>
              if (java.lang.Float.isNaN(f1) != java.lang.Float.isNaN(o2.asInstanceOf[Float])) return false
              if (Math.abs(f1 - o2.asInstanceOf[Float]) > tol) return false

            case d1: Double =>
              if (java.lang.Double.isNaN(d1) != java.lang.Double.isNaN(o2.asInstanceOf[Double])) return false
              if (Math.abs(d1 - o2.asInstanceOf[Double]) > tol) return false

            case d1: java.math.BigDecimal =>
              if (d1.compareTo(o2.asInstanceOf[java.math.BigDecimal]) != 0) return false

            case _ =>
              if (!o1.equals(o2)) return false
          }
        }
        idx += 1
      }
      return true
    }
  }

  def assertDoubleArrayEquals(one: Seq[Double], other: Seq[Double]) = {
    assertEquals(one.length, other.length)
    one.zip(other).foreach { case (a, b) => assertTrue(Math.abs(a - b) < 0.0001) }
  }

  def assertFloatArrayEquals(one: Seq[Float], other: Seq[Float]) = {
    assertEquals(one.length, other.length)
    one.zip(other).foreach { case (a, b) => assertTrue(Math.abs(a - b) < 0.0001) }
  }

  // Get the current test method name
  private def currentTestMethodName(): String = Thread.currentThread.getStackTrace()(3).getMethodName

  /**
   * get the base path to store test result
   * @param className the class name of the test case
   * @return
   */
  // scalastyle:off println
  def getTestResultBasePath(className: String): String = {
    val path = "src/test/resources/testExpectedOutput/" + className + "/" + currentTestMethodName()
    println(s"Get test result base path: ${path}")
    path
  }

  /** Produce a StructType schema object from a JSON string */
  def deserializeSchema(json: String): StructType = {
    Try(DataType.fromJson(json)).getOrElse(LegacyTypeStringParser.parseString(json)) match {
      case t: StructType => t
      case _ => throw new RuntimeException(s"Failed parsing StructType: $json")
    }
  }
}
