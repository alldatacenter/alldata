package com.linkedin.feathr.core.configvalidator;

/**
 * Fixture used during validation testing
 */
public class ConfigValidatorFixture {
  public static final String invalidHoconStr1 = String.join("\n",
      "sources: {",
      "  // Source name is incorrect since ':' isn't permitted in the key name if the key name isn't quoted.",
      "  invalid:source: {",
      "     type: VENICE",
      "     storeName: \"someStore\"",
      "     keyExpr: \"some key expression\"",
      "  }",
      "}");

  public static final String invalidHoconStr2 = String.join("\n",
      "anchors: {",
      "  a1: {",
      "    source: \"some/source\"",
      "    key: \"someKey\"",
      "    features: {",
      "      // Character '$' is forbidden if present in unquoted string",
      "      $feature_name_is_invalid: \"some feature expr\"",
      "    }",
      "  }",
      "}");

  public static final String validFeatureDefConfig = String.join("\n",
      "anchors: {",
      "  A1: {",
      "    source: \"/data/databases/CareersPreferenceDB/MemberPreference/#LATEST\"",
      "    extractor: \"com.linkedin.jymbii.frame.anchor.PreferencesFeatures\"",
      "    keyAlias: \"x\"",
      "    features: [",
      "      jfu_preference_companySize",
      "    ]",
      "  }",
      "}"
      );

  public static final String validFeatureDefConfigWithParameters = String.join("\n",
      "anchors: {",
      "  A1: {",
      "    source: \"/data/databases/CareersPreferenceDB/MemberPreference/#LATEST\"",
      "    extractor: \"com.linkedin.jymbii.frame.anchor.PreferencesFeatures\"",
      "    keyAlias: \"x\"",
      "    features: {",
      "      jfu_preference_companySize : {",
      "         parameters : {",
      "           param0 : \" some param 1\"",
      "           param1 : some_param",
      "           param2 : true",
      "           param3 : [p1, p2]",
      "           param4 : {java : 3}",
      "           param5 : {\"key1\":[\"v1\",\"v2\"]}",
      "           param6 : [{\"key1\":[\"v1\",\"v2\"]}, {\"key2\":[\"v1\",\"v2\"]}]",
      "         }",
      "      }",
      "    }",
      "  }",
      "}"
  );

  /**
   * The parameters are invalid because param1 and param2 are not of string type.
   */
  public static final String invalidFeatureDefConfigWithParameters = String.join("\n",
      "anchors: {",
      "  A1: {",
      "    source: \"/data/databases/CareersPreferenceDB/MemberPreference/#LATEST\"",
      "    extractor: \"com.linkedin.jymbii.frame.anchor.PreferencesFeatures\"",
      "    keyAlias: \"x\"",
      "    features: {",
      "      jfu_preference_companySize : {",
      "         parameters : param",
      "      }",
      "    }",
      "  }",
      "}"
  );

  public static final String legacyFeatureDefConfigWithGlobals = String.join("\n",
          "globals: {",
          "}",
          "anchors: {",
          "}",
          "sources: {",
          "}"
  );

  public static final String invalidFeatureDefConfig = String.join("\n",
      "anchors: {",
      " A1: {",
      "   source: \"some/path/in/HDFS/#LATEST\"",
      "    key: \"x\"",
      "    features: {",
      "      f1: 4.2",
      "      default: 123.0",
      "    }",
      "  }",

      "  A2: {",
      "    key: \"x\"",
      "    features: [\"f2\", \"f3\"]",
      "  }",

      "  // This anchor contains valid features, there shouldn't be any error flagged here",
      "  A3: {",
      "    source: \"/data/databases/CareersPreferenceDB/MemberPreference/#LATEST\"",
      "    extractor: \"com.linkedin.jymbii.frame.anchor.PreferencesFeatures\"",
      "    keyAlias: \"x\"",
      "    features: [",
      "      jfu_preference_companySize",
      "    ]",
      "  }",
      "}");

  public static final String invalidFeatureDefConfig2 = String.join("\n",
      "anchors: {",
      "  A1: {",
      "    source: \"/data/databases/CareersPreferenceDB/MemberPreference/#LATEST\"",
      "    extractor: \"com.linkedin.jymbii.frame.anchor.PreferencesFeatures\"",
      "    keyAlias: \"x\"",
      "    features: [",
      "      jfu_preference_companySize.0.0.1",
      "    ]",
      "  }",
      "}"
  );

  public static final String validJoinConfigWithSingleFeatureBag = String.join("\n",
      "myFeatureBag: [",
      "  {",
      "    key: \"targetId\"",
      "    featureList: [waterloo_job_location, waterloo_job_jobTitle, waterloo_job_jobSeniority]",
      "  }",
      "  {",
      "    key: sourceId",
      "    featureList: [jfu_resolvedPreference_seniority]",
      "  }",
      "  {",
      "    key: [sourceId, targetId]",
      "    featureList: [memberJobFeature1, memberJobFeature2]",
      "  }",
      "]");

  public static final String validJoinConfigWithMultFeatureBags = String.join("\n",
      "featuresGroupA: [",
      "    {",
      "      key: \"viewerId\"",
      "      featureList: [",
      "        waterloo_member_currentCompany,",
      "        waterloo_job_jobTitle,",
      "      ]",
      "    }",
      "]",
      "featuresGroupB: [",
      "    {",
      "      key: \"viewerId\"",
      "      featureList: [",
      "        waterloo_member_location,",
      "        waterloo_job_jobSeniority",
      "      ]",
      "    }",
      "]");

  public static final String invalidJoinConfig = String.join("\n",
      "features: [",
      "    {",
      "        // Missing key",
      "        featureList: [",
      "          jfu_resolvedPreference_seniority, ",
      "          jfu_resolvedPreference_country",
      "        ]",
      "    }",
      "]");

  public static final String validPresentationConfig = String.join("\n",
      "presentations: {",
      "  my_ccpa_feature: {",
      "    linkedInViewFeatureName: decision_makers_score",
      "    featureDescription: \"feature description that shows to the users\"",
      "    valueTranslation: \"translateLikelihood(this)\"",
      "  }",
      "}");

  /*
   * Join config request features that are defined in FeatureDef config, but not reachable
   */
  public static final String joinConfig1 = String.join("\n",
      "features: [",
      "    {",
      "      key: \"viewerId\"",
      "      featureList: [",
      "        feature_not_defined_1,",
      "        feature_not_defined_2,",
      "      ]",
      "    }",
      "]");

  /*
   * Join config request features that are not defined in FeatureDef config
   *  "resources/invalidSemanticsConfig/feature-not-reachable-def.conf"
   */
  public static final String joinConfig2 = String.join("\n",
      "features: [",
      "    {",
      "      key: [\"m\", \"j\"]",
      "      featureList: [",
      "        derived_feature_3",
      "      ]",
      "    }",
      "]");
}
