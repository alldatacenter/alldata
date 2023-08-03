package com.linkedin.feathr.core.configvalidator.typesafe;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class JoinConfFixture {

  static final String joinConf1 = String.join("\n",
      "featureBag1: [ ",
      "  { ",
      "    key: [id1] ",
      "    featureList: [ ",
      "      offline_feature1_1,",
      "      offline_feature2_1,",
      "      offline_feature4_1,",
      "    ] ",
      "  } ",
      "] ",

      "featureBag2: [",
      "  {",
      "    key: [id1]",
      "    featureList: [",
      "      derived_feature_1,",
      "      derived_feature_2,",
      "      derived_feature_4",
      "    ]",
      "  }",
      "]");

  static final Set<String> requestedFeatureNames1;
  static {
    requestedFeatureNames1 = Stream.of("offline_feature1_1", "offline_feature2_1", "offline_feature4_1",
        "derived_feature_1", "derived_feature_2", "derived_feature_4").collect(Collectors.toSet());
  }
}
