package com.linkedin.feathr.offline.transformation

import com.linkedin.feathr.common.exception.FeathrException
import com.linkedin.feathr.offline.TestFeathr
import org.scalatest.mockito.MockitoSugar
import org.testng.Assert.assertEquals
import org.testng.annotations.{DataProvider, Test}

import scala.collection.JavaConverters._
import scala.collection.convert.Wrappers.JMapWrapper

/**
 * Unit test class for FDSConversionUtils.
 */
class TestFDSConversionUtils extends TestFeathr with MockitoSugar {
  @DataProvider(name = "expectedBooleanDataProvider")
  def expectedBooleanDataProvider(): Array[Array[Any]] = {
    Array(
      Array[Any](true, true),
      Array[Any](false, false),
      Array[Any](JMapWrapper[String, Float](Map("" -> 1.0f).asJava), true),
      Array[Any](JMapWrapper[Nothing, Nothing](Map().asJava), false)
    )
  }
  @Test(dataProvider = "expectedBooleanDataProvider")
  def testBoolean(input: Any, expected: Boolean): Unit = {
    assertEquals(FDSConversionUtils.parseBooleanValue(input), expected)
  }

  @DataProvider(name = "exceptionalCases")
  def exceptionalCases(): Array[Array[Any]] = {
    Array(
      Array[Any](JMapWrapper[String, Float](Map("1" -> 1.0f).asJava)), // can't handle random map
      Array[Any]("") // can't handle string input
    )
  }
  @Test(dataProvider = "exceptionalCases", expectedExceptions = Array(classOf[FeathrException]))
  def testExceptionalCases(input: Any): Unit = {
    FDSConversionUtils.parseBooleanValue(input)
  }
}
