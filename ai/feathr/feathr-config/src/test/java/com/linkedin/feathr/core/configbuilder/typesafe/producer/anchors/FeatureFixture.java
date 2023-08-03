package com.linkedin.feathr.core.configbuilder.typesafe.producer.anchors;

import com.google.common.collect.ImmutableMap;
import com.linkedin.feathr.core.config.TimeWindowAggregationType;
import com.linkedin.feathr.core.config.WindowType;
import com.linkedin.feathr.core.config.producer.ExprType;
import com.linkedin.feathr.core.config.producer.TypedExpr;
import com.linkedin.feathr.core.config.producer.anchors.ExpressionBasedFeatureConfig;
import com.linkedin.feathr.core.config.producer.anchors.ExtractorBasedFeatureConfig;
import com.linkedin.feathr.core.config.producer.anchors.FeatureConfig;
import com.linkedin.feathr.core.config.producer.anchors.TimeWindowFeatureConfig;
import com.linkedin.feathr.core.config.producer.anchors.WindowParametersConfig;
import com.linkedin.feathr.core.config.producer.common.FeatureTypeConfig;
import com.linkedin.feathr.core.config.producer.definitions.FeatureType;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


class FeatureFixture {

  static final String feature1ConfigStr = String.join("\n",
      "features: {",
      "  member_lixSegment_isStudent: \"is_student\"",
      "  member_lixSegment_isJobSeeker: \"job_seeker_class == 'active'\"",
      "}");

  static final Map<String, FeatureConfig> expFeature1ConfigObj;
  static {
    expFeature1ConfigObj = new HashMap<>();
    expFeature1ConfigObj.put("member_lixSegment_isStudent", new ExtractorBasedFeatureConfig("is_student"));
    expFeature1ConfigObj.put(
        "member_lixSegment_isJobSeeker", new ExtractorBasedFeatureConfig("job_seeker_class == 'active'"));
  }

  static final String feature1ConfigStrWithSpecialChars = String.join("\n",
      "features: {",
      "  \"member:lixSegment.isStudent\": \"is_student\"",
      "  \"member:lixSegment.isJobSeeker\": \"job_seeker_class == 'active'\"",
      "}");

  static final Map<String, FeatureConfig> expFeature1ConfigObjWithSpecialChars;
  static {
    expFeature1ConfigObjWithSpecialChars = new HashMap<>();
    expFeature1ConfigObjWithSpecialChars.put("member:lixSegment.isStudent", new ExtractorBasedFeatureConfig("is_student"));
    expFeature1ConfigObjWithSpecialChars.put(
        "member:lixSegment.isJobSeeker", new ExtractorBasedFeatureConfig("job_seeker_class == 'active'"));
  }

  static final String feature2ConfigStr = String.join("\n",
      "features: [",
      "  waterloo_job_jobTitle,",
      "  waterloo_job_companyId,",
      "  waterloo_job_companySize,",
      "  waterloo_job_companyDesc",
      "]");



  static final Map<String, FeatureConfig> expFeature2ConfigObj;


  static {
    expFeature2ConfigObj = new HashMap<>();
    expFeature2ConfigObj.put("waterloo_job_jobTitle", new ExtractorBasedFeatureConfig("waterloo_job_jobTitle"));
    expFeature2ConfigObj.put("waterloo_job_companyId", new ExtractorBasedFeatureConfig("waterloo_job_companyId"));
    expFeature2ConfigObj.put("waterloo_job_companySize", new ExtractorBasedFeatureConfig("waterloo_job_companySize"));
    expFeature2ConfigObj.put("waterloo_job_companyDesc", new ExtractorBasedFeatureConfig("waterloo_job_companyDesc"));
  }

  static final String feature2ConfigWithTypeStr = String.join("\n",
      "features: {",
      "  waterloo_job_jobTitle : {",
      "    type: BOOLEAN",
      "  },",
      "  waterloo_job_companyId : {",
      "    type: BOOLEAN",
      "    default: true",
      "  },",
      "  waterloo_job_companySize : {},",
      "  waterloo_job_companyDesc: {}",
      "}");

  static final Map<String, FeatureConfig> expFeature2WithTypeConfigObj;

  static {
    expFeature2WithTypeConfigObj = new HashMap<>();
    FeatureTypeConfig featureTypeConfig = new FeatureTypeConfig(FeatureType.BOOLEAN);
    expFeature2WithTypeConfigObj.put("waterloo_job_jobTitle",
        new ExtractorBasedFeatureConfig("waterloo_job_jobTitle", featureTypeConfig));
    expFeature2WithTypeConfigObj.put("waterloo_job_companyId",
        new ExtractorBasedFeatureConfig("waterloo_job_companyId", featureTypeConfig, "true", Collections.emptyMap()));
    expFeature2WithTypeConfigObj.put("waterloo_job_companySize", new ExtractorBasedFeatureConfig("waterloo_job_companySize"));
    expFeature2WithTypeConfigObj.put("waterloo_job_companyDesc", new ExtractorBasedFeatureConfig("waterloo_job_companyDesc"));
  }

  static final String feature3ConfigStr = String.join("\n",
      "features: {",
      "  member_sentInvitations_numIgnoredRejectedInvites: {",
      "    def: \"toNumeric(numIgnoredRejectedInvites)\"",
      "    type: \"BOOLEAN\"",
      "    default: 0",
      "  }",
      "  member_sentInvitations_numGuestInvites: {",
      "    def: \"toNumeric(numGuestInvites)\"",
      "    default: 0",
      "  }",
      "  member_sentInvitations_numMemberInvites: {",
      "    def: \"toNumeric(numMemberInvites)\"",
      "  }",
      "}");

  static final Map<String, FeatureConfig> expFeature3ConfigObj;
  static {
    expFeature3ConfigObj = new HashMap<>();
    String defaultValue = "0";
    FeatureTypeConfig featureTypeConfig = new FeatureTypeConfig(FeatureType.BOOLEAN);
    ExpressionBasedFeatureConfig feature1 = new ExpressionBasedFeatureConfig("toNumeric(numIgnoredRejectedInvites)",
        defaultValue, featureTypeConfig);
    ExpressionBasedFeatureConfig feature2= new ExpressionBasedFeatureConfig("toNumeric(numGuestInvites)",
        defaultValue, (FeatureTypeConfig) null);
    ExpressionBasedFeatureConfig feature3= new ExpressionBasedFeatureConfig("toNumeric(numMemberInvites)", null);

    expFeature3ConfigObj.put("member_sentInvitations_numIgnoredRejectedInvites", feature1);
    expFeature3ConfigObj.put("member_sentInvitations_numGuestInvites", feature2);
    expFeature3ConfigObj.put("member_sentInvitations_numMemberInvites", feature3);
  }

  static final String feature4ConfigStr = String.join("\n",
      "features: {",
      "  simplePageViewCount: {",
      "    def: \"pageView\"",
      "    aggregation: COUNT",
      "    window: 1d",
      "    default: 0",
      "    type: \"BOOLEAN\"",
      "  }",
      "  sumPageView1d: {",
      "    def: \"pageView\"",
      "    aggregation: COUNT",
      "    window: 1d",
      "    filter: \"pageKey = 5\"",
      "  }",
      "  maxPV12h: {",
      "    def: \"pageView\"",
      "    aggregation: MAX",
      "    window: 12h",
      "    groupBy: \"pageKey\"",
      "    limit: 2",
      "  }",
      "  minPV12h: {",
      "    def: \"pageView\"",
      "    aggregation: MIN",
      "    window: 12h",
      "    groupBy: \"pageKey\"",
      "    limit: 2",
      "  }",
      "  timeSincePV: {",
      "     def: \"\"",
      "     aggregation: TIMESINCE",
      "     window: 5d",
      "  }",
      "  nearLine: {",
      "     def.mvel: \"pageView\"",
      "     aggregation: MAX",
      "     windowParameters: {",
      "       type: FIXED",
      "       size: 12h",
      "      }",
      "   }",
      "  latestPV: {",
      "     def: \"pageView\"",
      "     aggregation: LATEST",
      "     window: 5d",
      "  }",
      "  testMinPoolingAndEmbeddingSize: {",
      "     def: \"careersJobEmbedding\"",
      "     filter: \"action IN ('APPLY_OFFSITE', 'APPLY_ONSITE')\"",
      "     aggregation: MIN_POOLING",
      "     window: 4d",
      "     embeddingSize: 200",
      "  }",
      "}");

  static final Map<String, FeatureConfig> expFeature4ConfigObj;
  static {
    expFeature4ConfigObj = new HashMap<>();
    FeatureTypeConfig featureTypeConfig = new FeatureTypeConfig(FeatureType.BOOLEAN);
    WindowParametersConfig windowParameters1 = new WindowParametersConfig(WindowType.SLIDING, Duration.ofDays(1), null);
    TimeWindowFeatureConfig feature1 = new TimeWindowFeatureConfig(new TypedExpr("pageView", ExprType.SQL),
        TimeWindowAggregationType.COUNT, windowParameters1, null, null, null, null, null, null, featureTypeConfig, "0");

    WindowParametersConfig windowParameters2 = new WindowParametersConfig(WindowType.SLIDING, Duration.ofDays(1), null);
    TimeWindowFeatureConfig feature2 = new TimeWindowFeatureConfig("pageView",
        TimeWindowAggregationType.COUNT, windowParameters2, "pageKey = 5",null, null, null, null);

    WindowParametersConfig windowParameters3 = new WindowParametersConfig(WindowType.SLIDING, Duration.ofHours(12), null);
    TimeWindowFeatureConfig feature3 = new TimeWindowFeatureConfig("pageView",
        TimeWindowAggregationType.MAX, windowParameters3, null, "pageKey", 2, null,null);

    WindowParametersConfig windowParameters4 = new WindowParametersConfig(WindowType.SLIDING, Duration.ofHours(12), null);
    TimeWindowFeatureConfig feature4 = new TimeWindowFeatureConfig("pageView",
        TimeWindowAggregationType.MIN, windowParameters4, null, "pageKey", 2, null,null);

    WindowParametersConfig windowParameters5 = new WindowParametersConfig(WindowType.SLIDING, Duration.ofDays(5), null);
    TimeWindowFeatureConfig feature5 = new TimeWindowFeatureConfig("",
        TimeWindowAggregationType.TIMESINCE, windowParameters5, null, null, null, null, null);

    WindowParametersConfig windowParameters6 = new WindowParametersConfig(WindowType.FIXED, Duration.ofHours(12), null);
    TimeWindowFeatureConfig feature6 = new TimeWindowFeatureConfig("pageView", ExprType.MVEL,
        TimeWindowAggregationType.MAX, windowParameters6, null, null, null, null, null, null);

    WindowParametersConfig windowParameters7 = new WindowParametersConfig(WindowType.SLIDING, Duration.ofDays(5), null);
    TimeWindowFeatureConfig feature7 = new TimeWindowFeatureConfig("pageView",
        TimeWindowAggregationType.LATEST, windowParameters7, null, null, null, null, null);

    WindowParametersConfig windowParameters8 = new WindowParametersConfig(WindowType.SLIDING, Duration.ofDays(4), null);
    TimeWindowFeatureConfig feature8 = new TimeWindowFeatureConfig(
        new TypedExpr("careersJobEmbedding", ExprType.SQL),
        TimeWindowAggregationType.MIN_POOLING, windowParameters8,
        new TypedExpr("action IN ('APPLY_OFFSITE', 'APPLY_ONSITE')", ExprType.SQL),
        null, null, null, null, 200);

    expFeature4ConfigObj.put("simplePageViewCount", feature1);
    expFeature4ConfigObj.put("sumPageView1d", feature2);
    expFeature4ConfigObj.put("maxPV12h", feature3);
    expFeature4ConfigObj.put("minPV12h", feature4);
    expFeature4ConfigObj.put("timeSincePV", feature5);
    expFeature4ConfigObj.put("nearLine", feature6);
    expFeature4ConfigObj.put("latestPV", feature7);
    expFeature4ConfigObj.put("testMinPoolingAndEmbeddingSize", feature8);
  }

  static final String feature5ConfigWithTypeStr = String.join("\n",
      "features: {",
      "     waterloo_job_jobTitleV2 : {",
      "       parameters: {",
      "         param1 : [waterlooCompany_terms_hashed, waterlooCompany_values]",
      "       }",
      "       default: true",
      "       type: BOOLEAN",
      "    }",
      " }");

  static final Map<String, FeatureConfig> expFeature5WithTypeConfigObj;

  static {
    expFeature5WithTypeConfigObj = new HashMap<>();
    Map<String, String> parameters = ImmutableMap.of("param1", "[\"waterlooCompany_terms_hashed\",\"waterlooCompany_values\"]");
    expFeature5WithTypeConfigObj.put("waterloo_job_jobTitleV2",
        new ExtractorBasedFeatureConfig("waterloo_job_jobTitleV2", new FeatureTypeConfig(FeatureType.BOOLEAN), "true", parameters));
  }
}
