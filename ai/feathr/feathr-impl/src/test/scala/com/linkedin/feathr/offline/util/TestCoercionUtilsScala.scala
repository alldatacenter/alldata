package com.linkedin.feathr.offline.util

import com.linkedin.feathr.common
import com.linkedin.feathr.offline.util.CoercionUtilsScala.coerceFeatureValueToStringKey
import org.testng.Assert.assertEquals
import org.testng.annotations.Test

import scala.util.Random.shuffle
import scala.collection.JavaConverters._

class TestCoercionUtilsScala {
  @Test(description = "Verifies that coerceFeatureValueToStringKey works properly for unordered NTV values")
  def testFDSTensorSchemas(): Unit = {
    val inputNTV = shuffle(List.range(0, 50).map(_.toString).zipWithIndex).map(kv => (kv._1, kv._2.toFloat)).toMap
    val stringKey = coerceFeatureValueToStringKey(new common.FeatureValue(inputNTV.asJava))
    assertEquals(stringKey.toList, List.range(0, 50).map(_.toString))
  }

}