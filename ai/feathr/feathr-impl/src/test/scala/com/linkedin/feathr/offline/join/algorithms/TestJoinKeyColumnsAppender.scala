package com.linkedin.feathr.offline.join.algorithms

import com.linkedin.feathr.common.exception.FeathrFeatureJoinException
import com.linkedin.feathr.offline.TestFeathr
import org.apache.spark.sql.DataFrame
import org.mockito.Mockito.{verify, when}
import org.scalatest.mockito.MockitoSugar
import org.testng.annotations.Test

class TestJoinKeyColumnsAppender extends TestFeathr with MockitoSugar {

  /**
   * Test when join keys do not exist in the DataFrame.
   */
  @Test(expectedExceptions = Array(classOf[FeathrFeatureJoinException]), expectedExceptionsMessageRegExp = ".*not found in DataFrame.*")
  def testIdentityJoinKeyColumnAppenderWhenColumnIsNotInDF(): Unit = {
    val mockDataFrame = mock[DataFrame]
    when(mockDataFrame.columns).thenReturn(Array("col1", "col2", "col3"))
    val joinKeys = Seq("joinKey1", "joinKey2") // column does not exist in DataFrame
    IdentityJoinKeyColumnAppender.appendJoinKeyColunmns(joinKeys, mockDataFrame)
  }

  /**
   * Test IdentityJoinKeyColumnAppender checks for DF columns.
   */
  @Test
  def testIdentityJoinKeyColumnAppenderChecksDFColumns(): Unit = {
    // mocks
    val mockDataFrame = mock[DataFrame]
    when(mockDataFrame.columns).thenReturn(Array("col1", "col2", "col3"))
    val joinKeys = Seq("col1", "col2") // column does not exist in DataFrame
    IdentityJoinKeyColumnAppender.appendJoinKeyColunmns(joinKeys, mockDataFrame)
    verify(mockDataFrame).columns
  }
}
