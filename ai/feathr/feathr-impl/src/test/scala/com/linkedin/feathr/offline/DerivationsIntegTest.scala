package com.linkedin.feathr.offline

import com.linkedin.feathr.offline.util.FeathrTestUtils.assertDataFrameApproximatelyEquals
import org.apache.spark.sql.Row
import org.apache.spark.sql.types._
import org.testng.annotations.Test

class DerivationsIntegTest extends FeathrIntegTest {

  /**
   * Test multi-key derived feature and multi-tagged feature.
   * This test covers the following:-
   * -> sql based custom extractor
   */
  @Test
  def testMultiKeyDerivedFeatureDFWithSQL: Unit = {
    val df = runLocalFeatureJoinForTest(
      joinConfigAsString = """
                             | features: [ {
                             |   key: ["concat('',viewer)", viewee]
                             |   featureList: [ "foo_square_distance_sql"]
                             | } ,
                             |  {
                             |   key: [viewee, viewer]
                             |   featureList: [ "foo_square_distance_sql"]
                             | },
                             | {
                             |   key: [viewee, viewer]
                             |   featureList: [ "square_fooFeature_sql"]
                             | }
                             | ]
      """.stripMargin,
      featureDefAsString = """
                             | anchors: {
                             |   anchor1: {
                             |     source: anchorAndDerivations/derivations/anchor6-source.csv
                             |     key.sqlExpr: [sourceId, destId]
                             |     features: {
                             |       fooFeature: {
                             |         def.sqlExpr: cast(source as int)
                             |         type: NUMERIC
                             |       }
                             |     }
                             |   }
                             | }
                             | derivations: {
                             |
                             |   square_fooFeature_sql: {
                             |     key: [m1, m2]
                             |     inputs: {
                             |       a: { key: [m1, m2], feature: fooFeature }
                             |     }
                             |     definition.sqlExpr: "a * a"
                             |   }
                             |   foo_square_distance_sql: {
                             |     key: [m1, m2]
                             |     inputs: {
                             |       a1: { key: [m1, m2], feature: square_fooFeature_sql }
                             |       a2: { key: [m2, m1], feature: square_fooFeature_sql }
                             |     }
                             |     definition.sqlExpr: "a1 - a2"
                             |   }
                             | }
        """.stripMargin,
      observationDataPath = "anchorAndDerivations/derivations/test2-observations.csv")

    val expectedDf = ss.createDataFrame(
      ss.sparkContext.parallelize(
        Seq(
          Row(
            // viewer
            "1",
            // viewee
            "3",
            // label
            "1.0",
            // square_fooFeature_sql
            4.0f,
            // viewee_viewer__foo_square_distance_sql
            -21.0f,
            // concat____viewer__viewee__foo_square_distance_sql
            21.0f),
          Row(
            // viewer
            "2",
            // viewee
            "1",
            // label
            "-1.0",
            // square_fooFeature_sql
            9.0f,
            // viewee_viewer__foo_square_distance_sql
            -27.0f,
            // concat____viewer__viewee__foo_square_distance_sql
            27.0f),
          Row(
            // viewer
            "3",
            // viewee
            "6",
            // label
            "1.0",
            // square_fooFeature_sql
            null,
            // viewee_viewer__foo_square_distance_sql
            null,
            // concat____viewer__viewee__foo_square_distance_sql
            null),
          Row(
            // viewer
            "3",
            // viewee
            "5",
            // label
            "-1.0",
            // square_fooFeature_sql
            null,
            // viewee_viewer__foo_square_distance_sql
            null,
            // concat____viewer__viewee__foo_square_distance_sql
            null),
          Row(
            // viewer
            "5",
            // viewee
            "10",
            // label
            "1.0",
            // square_fooFeature_sql
            null,
            // viewee_viewer__foo_square_distance_sql
            null,
            // concat____viewer__viewee__foo_square_distance_sql
            null))),
      StructType(
        List(
          StructField("viewer", StringType, true),
          StructField("viewee", StringType, true),
          StructField("label", StringType, true),
          StructField("square_fooFeature_sql", FloatType, true),
          StructField("viewee_viewer__foo_square_distance_sql", FloatType, true),
          StructField("concat____viewer__viewee__foo_square_distance_sql", FloatType, true))))
    def cmpFunc(row: Row): String = if (row.get(0) != null) row.get(0).toString else "null"
    assertDataFrameApproximatelyEquals(df.data, expectedDf, cmpFunc)
  }
}
