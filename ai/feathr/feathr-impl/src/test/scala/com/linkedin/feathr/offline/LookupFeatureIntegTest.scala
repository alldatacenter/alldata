package com.linkedin.feathr.offline

import com.linkedin.feathr.offline.util.FeathrTestUtils.assertDataFrameApproximatelyEquals
import org.apache.spark.sql.Row
import org.apache.spark.sql.types._
import org.testng.annotations.Test

class LookupFeatureIntegTest extends FeathrIntegTest {

  /**
   * Test look up feature with first aggregation
   */
  @Test
  def testLookupFeatureWithFirstAgg: Unit = {
    val df = runLocalFeatureJoinForTest(
      joinConfigAsString =
        """
          | features: [ {
          |   key: [mId]
          |   featureList: [ "item_price"]
          | }
          | ]
          | """.stripMargin,
      featureDefAsString =
        """
          |    anchors: {
          |     anchor1: {
          |       source: "anchor1-source.csv"
          |         key: "mId"
          |         features: {
          |            f_alpha: "alpha"
          |        }
          |      }
          |      anchor2: {
          |       source: "anchor1-source-lookup.csv"
          |         key: "alpha"
          |         features: {
          |            f_price: "(float)price"
          |        }
          |      }
          |    }
          |    derivations: {
          |      item_price: {
          |        key: [mId]
          |        join: {
          |            base: {key: [mId], feature: f_alpha}
          |            expansion: {key: [item_id], feature: f_price}
          |        }
          |        aggregation: FIRST
          |        type: {
          |            type: TENSOR
          |            tensorCategory: DENSE
          |            dimensionType: []
          |            valType: FLOAT
          |        }
          |      }
          |  }
          |""".stripMargin,
      observationDataPath = "anchor1-obs.csv")
    val selectedColumns = Seq("mId", "item_price")
    val filteredDf = df.data.select(selectedColumns.head, selectedColumns.tail: _*)

    val expectedDf = ss.createDataFrame(
      ss.sparkContext.parallelize(
        Seq(
          Row(
            "1",
            1.0f
          ), Row(
            "2",
            2.0f
          ), Row(
            "3",
            3.0f
          ),
        )
      ),
      StructType(
        List(
          StructField("mId", StringType, true),
          StructField("item_price", FloatType, true))))

    def cmpFunc(row: Row): String = if (row.get(0) != null) row.get(0).toString else "null"

    assertDataFrameApproximatelyEquals(filteredDf, expectedDf, cmpFunc)
  }
}
