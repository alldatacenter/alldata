package com.linkedin.feathr.offline.mvel

object FeathrMvelFixture {
  val wrongMVELExpressionFeatureConf =
    """
      |anchors: {
      |  anchor1: {
      |    source: "anchor1-source.csv"
      |    key: "mId"
      |    features: {
      |      wrongExpFeature: "foo.bar(alpha)"
      |    }
      |  }
      |}
    """.stripMargin

  val mvelFeatureWithNullValueConf =
    """
      |anchors: {
      |  anchor1: {
      |    source: "nullValue-source.avro.json"
      |    key: "mId"
      |    features: {
      |      featureWithNull: "toNumeric(value) + 1"
      |    }
      |  }
      |
      |  anchor2: {
      |    source: "nullValue-source2.avro.json"
      |    key: "mId"
      |    features: {
      |      featureWithNull2: "toNumeric(datum.x) + 1"
      |    }
      |  }
      |
      |  anchor3:{
      |    source: "nullValue-source1.avro.json"
      |    key: "mId"
      |    features: {
      |      featureWithNull3: {
      |        def: "embedding"
      |        type: "DENSE_VECTOR"
      |      }
      |    }
      |  }
      |}
    """.stripMargin

  val mvelDerivedFeatureCheckingNullConf =
    """
      |anchors: {
      |  anchor1: {
      |    source: "nullValue-source.avro.json"
      |    key: "mId"
      |    features: {
      |      defaultZero: {
      |        def: "(Float) value"
      |        default: 0 //all numeric default value will be parsed to Double, and then cast to Float by coerceToVector
      |      }
      |      defaultEmpty: {
      |        def: "(Float) value"
      |      }
      |    }
      |  }
      |}
      |
      |derivations: {
      |  //isNonZero checks if featureWithNull contains null value
      |  checkingZero: "isNonZero(defaultZero) ? defaultZero : -1.0"
      |  //use isEmpty to check for missing FeatureValue
      |  checkingEmpty: "isPresent(defaultEmpty) ? defaultEmpty : 0.0"
      |}
    """.stripMargin
}
