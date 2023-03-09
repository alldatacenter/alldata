package com.linkedin.feathr.offline.transformation

import com.linkedin.feathr.offline.AssertFeatureUtils._
import com.linkedin.feathr.offline.TestFeathr
import com.linkedin.feathr.offline.transformation.DataFrameExt._
import org.testng.annotations.Test
/**
 * Unit test class for DataFrameExt.
 */
class TestDataFrameExt extends TestFeathr {
  @Test
  def testFuzzyUnion(): Unit = {
    val sqlContext = new org.apache.spark.sql.SQLContext(ss.sparkContext)
    import sqlContext.implicits._
    val df1 = Seq((1, 2, 3)).toDF("col0", "col1", "col2")
    val df2 = Seq((4, 5, 6)).toDF("col1", "col0", "col3")
    val expectDf = Seq((1, 2, Some(3), None), (5, 4, None, Some(6))).toDF("col0", "col1", "col2", "col3")
    val unionDf = df1.fuzzyUnion(df2)
    assertDataFrameEquals(unionDf, expectDf)
  }

  @Test(description = "Test with different join key column name in left and right")
  def testAppendRows(): Unit = {
    /*
     * left          right
     * id1 id2 v1 v2       id3 id4 v3
     * 0    5  a   b        0   5   e
     * 1    6  c   d        10  11  f
     *
     * will return:
     * id1 id2 v1 v2
     * 0  5  a  b
     * 1  6  c  d
     * 0  5  a  b
     * 10  11  c d
     */
    val sqlContext = new org.apache.spark.sql.SQLContext(ss.sparkContext)
    import sqlContext.implicits._
    val df1 = Seq((0, "5", "a", "b"),
                  (1, "6", "c", "d")
                 ).toDF("id1", "id2", "v1", "v2")
    val df2 = Seq((0, 5, "e"),
                  (10, 11, "f")
                 ).toDF("id3", "id4", "v3")
    val expectDf = Seq(
      (0, "5", "a", "b"),
      (1, "6", "c", "d"),
      (0, "5", "a", "b"),
      (10, "11", "c", "d")
    ).toDF("id1", "id2", "v1", "v2")
    val unionDf = df1.appendRows(Seq("id1", "id2"), Seq("id3", "id4"), df2)
    assertDataFrameEquals(unionDf, expectDf)
  }


  @Test(description = "Test with same join key column name in left and right")
  def testAppendRowsWithSameKeyColumn(): Unit = {
    /*
     * left          right
     * id1 id2 v1 v2       id1 id2 v3
     * 0    5  a   b        0   5   e
     * 1    6  c   d        10  11  f
     *
     * will return:
     * id1 id2 v1 v2
     * 0  5  a  b
     * 1  6  c  d
     * 0  5  a  b
     * 10  11  c d
     */
    val sqlContext = new org.apache.spark.sql.SQLContext(ss.sparkContext)
    import sqlContext.implicits._
    val df1 = Seq((0, "5", "a", "b"),
      (1, "6", "c", "d")
    ).toDF("id1", "id2", "v1", "v2")
    val df2 = Seq((0, 5, "e"),
      (10, 11, "f")
    ).toDF("id1", "id2", "v3")
    val expectDf = Seq(
      (0, "5", "a", "b"),
      (1, "6", "c", "d"),
      (0, "5", "a", "b"),
      (10, "11", "c", "d")
    ).toDF("id1", "id2", "v1", "v2")
    val unionDf = df1.appendRows(Seq("id1", "id2"), Seq("id1", "id2"), df2)
    assertDataFrameEquals(unionDf, expectDf)
  }
}
