package com.linkedin.feathr.offline.join

import com.linkedin.feathr.offline.TestFeathr
import org.testng.Assert.assertEquals
import org.testng.annotations.Test

/**
 * unit test for KeyColumnCombiner
 */
class TestDataFrameKeyCombiner extends TestFeathr {

  @Test
  def testGenerateCombinedKeyColumnsWithFilterNull(): Unit = {
    val sqlContext = ss.sqlContext
    import sqlContext.implicits._
    val df = Seq((1, "l1"), (2, null)).toDF("c1", "c2")

    val keyColumnCombiner = DataFrameKeyCombiner()
    // test single key
    val (newColName, withKeyDF) = keyColumnCombiner.combine(df, Seq("c2"), filterNull = true)
    val keys = withKeyDF.select(newColName).collect.map(_.getAs[String](0))
    assertEquals(keys.length, 1)
    assertEquals(keys(0), "l1")

    // test combined keys
    val (newColName2, withKeyDF2) = keyColumnCombiner.combine(df, Seq("c1", "c2"), filterNull = true)
    val keys2 = withKeyDF2.select(newColName2).collect.map(_.getAs[String](0))
    assertEquals(keys2.length, 1)
    assertEquals(keys2(0), "1#l1")
  }

  @Test
  def testGenerateCombinedKeyColumnsWithoutFilterNull(): Unit = {
    val sqlContext = ss.sqlContext
    import sqlContext.implicits._
    val df = Seq((1, "l1"), (2, null)).toDF("c1", "c2")

    val keyColumnCombiner = DataFrameKeyCombiner()
    // test single key
    val (newColName, withKeyDF) = keyColumnCombiner.combine(df, Seq("c2"), filterNull = false)
    val keys = withKeyDF.select(newColName).collect.map(_.getAs[String](0))
    assertEquals(keys(0), "l1")
    assertEquals(keys(1), null)

    // test combined keys
    val (newColName2, withKeyDF2) = keyColumnCombiner.combine(df, Seq("c1", "c2"), filterNull = false)
    val keys2 = withKeyDF2.select(newColName2).collect.map(_.getAs[String](0))
    assertEquals(keys2(0), "1#l1")
    assertEquals(keys2(1), "2#__feathr_null")
  }

}
