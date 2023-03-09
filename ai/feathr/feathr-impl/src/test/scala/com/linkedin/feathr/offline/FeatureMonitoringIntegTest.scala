package com.linkedin.feathr.offline

import org.testng.annotations.Test

/**
 * Integration tests to test feature monitoring APIs in feathr offline.
 */
class FeatureMonitoringIntegTest extends FeathrIntegTest {
  /**
   * Test scalar features
   */
  @Test(enabled = true)
  def testFeatureGenWithApplicationConfig(): Unit = {
    val applicationConfig =
      s"""
         | operational: {
         |  name: generateWithDefaultParams
         |  endTime: 2021-01-02
         |  endTimeFormat: "yyyy-MM-dd"
         |  resolution: DAILY
         |  output:[
         |  {
         |      name: MONITORING
         |      params: {
         |        table_name: "monitoringFeatures"
         |      }
         |   }
         |  ]
         |}
         |features: [f_string, f_int, f_null, f_double, f_null, f_boolean
         |]
      """.stripMargin
    val featureDefConfig =
      """
        |anchors: {
        |  anchor: {
        |    source: featureMonitoringSource
        |    key: user_id
        |    features: {
        |      f_string: {
        |        def: "value_string"
        |        type : {
        |            type: TENSOR
        |            tensorCategory: DENSE
        |            dimensionType: []
        |            valType: STRING
        |        }
        |      }
        |      f_int: {
        |        def: "import java.util.Random; Random random = new Random(); random.nextInt(2)"
        |        type : {
        |            type: TENSOR
        |            tensorCategory: DENSE
        |            dimensionType: []
        |            valType: INT
        |        }
        |      }
        |      f_null: {
        |        def: "null"
        |        type : {
        |            type: TENSOR
        |            tensorCategory: DENSE
        |            dimensionType: []
        |            valType: DOUBLE
        |        }
        |      }
        |      f_double: {
        |        def: "import java.util.Random; Random random = new Random(); random.nextDouble()"
        |        type : {
        |            type: TENSOR
        |            tensorCategory: DENSE
        |            dimensionType: []
        |            valType: DOUBLE
        |        }
        |      }
        |      f_boolean: {
        |        def: "Boolean.valueOf(value_boolean)"
        |        type : {
        |            type: TENSOR
        |            tensorCategory: DENSE
        |            dimensionType: []
        |            valType: BOOLEAN
        |        }
        |      }
        |    }
        |  }
        |}
        |
        |derivations: {
        |   f_derived: {
        |     definition: "f_double * f_double"
        |     type: NUMERIC
        |   }
        |}
        |sources: {
        |  featureMonitoringSource: {
        |    location: { path: "/feature_monitoring_mock_data/feature_monitoring_data.csv" }
        |  }
        |}
        |""".stripMargin

    val res = localFeatureGenerate(applicationConfig, featureDefConfig)
    res.head._2.data.show(100)
  }
}
