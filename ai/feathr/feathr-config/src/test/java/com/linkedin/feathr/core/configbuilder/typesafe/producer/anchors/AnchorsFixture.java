package com.linkedin.feathr.core.configbuilder.typesafe.producer.anchors;

import com.google.common.collect.ImmutableMap;
import com.linkedin.feathr.core.config.TimeWindowAggregationType;
import com.linkedin.feathr.core.config.WindowType;
import com.linkedin.feathr.core.config.producer.ExprType;
import com.linkedin.feathr.core.config.producer.TypedExpr;
import com.linkedin.feathr.core.config.producer.anchors.AnchorConfig;
import com.linkedin.feathr.core.config.producer.anchors.AnchorConfigWithExtractor;
import com.linkedin.feathr.core.config.producer.anchors.AnchorConfigWithKey;
import com.linkedin.feathr.core.config.producer.anchors.AnchorConfigWithKeyExtractor;
import com.linkedin.feathr.core.config.producer.anchors.AnchorsConfig;
import com.linkedin.feathr.core.config.producer.anchors.ExpressionBasedFeatureConfig;
import com.linkedin.feathr.core.config.producer.anchors.ExtractorBasedFeatureConfig;
import com.linkedin.feathr.core.config.producer.anchors.FeatureConfig;
import com.linkedin.feathr.core.config.producer.anchors.LateralViewParams;
import com.linkedin.feathr.core.config.producer.anchors.TimeWindowFeatureConfig;
import com.linkedin.feathr.core.config.producer.anchors.TypedKey;
import com.linkedin.feathr.core.config.producer.anchors.WindowParametersConfig;
import com.linkedin.feathr.core.config.producer.common.FeatureTypeConfig;
import com.linkedin.feathr.core.config.producer.definitions.FeatureType;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class AnchorsFixture {
  static final FeatureTypeConfig expectedFeatureTypeConfig =
      new FeatureTypeConfig.Builder().setFeatureType(FeatureType.DENSE_TENSOR)
          .setShapes(Collections.singletonList(10))
          .setDimensionTypes(Collections.singletonList("INT"))
          .setValType("FLOAT")
          .build();

  static final String anchor1ConfigStr = String.join("\n",
      "member-lix-segment: {",
      "  source: \"/data/derived/lix/euc/member/#LATEST\"",
      "  key: \"id\"",
      "  features: {",
      "    member_lixSegment_isStudent: \"is_student\"",
      "    member_lixSegment_isJobSeeker: \"job_seeker_class == 'active'\"",
      "  }",
      "}");

  public static final AnchorConfigWithKey expAnchor1ConfigObj;
  static {
    String source = "/data/derived/lix/euc/member/#LATEST";
    TypedKey TypedKey = new TypedKey("\"id\"", ExprType.MVEL);
    Map<String, FeatureConfig> features = new HashMap<>();
    features.put("member_lixSegment_isStudent", new ExtractorBasedFeatureConfig("is_student"));
    features.put("member_lixSegment_isJobSeeker", new ExtractorBasedFeatureConfig("job_seeker_class == 'active'"));
    expAnchor1ConfigObj = new AnchorConfigWithKey(source, TypedKey, null, features);
  }

  static final String anchor2ConfigStr = String.join("\n",
      "member-sent-invitations: {",
      "  source: \"/jobs/frame/inlab/data/features/InvitationStats\"",
      "  key: \"x\"",
      "  features: {",
      "    member_sentInvitations_numIgnoredRejectedInvites: {",
      "      def: \"toNumeric(numIgnoredRejectedInvites)\"",
      "      default: 0",
      "      type: {",
      "        type: \"DENSE_TENSOR\"",
      "        shape: [10]",
      "        dimensionType: [\"INT\"]",
      "        valType: \"FLOAT\"",
      "      }",
      "    }",
      "    member_sentInvitations_numGuestInvites: {",
      "      def: \"toNumeric(numGuestInvites)\"",
      "      type: {",
      "        type: \"DENSE_TENSOR\"",
      "        shape: [10]",
      "        dimensionType: [\"INT\"]",
      "        valType: \"FLOAT\"",
      "      }",
      "      default: 0",
      "    }",
      "  }",
      "}");

  static final AnchorConfigWithKey expAnchor2ConfigObj;
  static{
    String source = "/jobs/frame/inlab/data/features/InvitationStats";
    TypedKey TypedKey = new TypedKey("\"x\"", ExprType.MVEL);
    String defaultValue = "0";
    ExpressionBasedFeatureConfig feature1 = new ExpressionBasedFeatureConfig("toNumeric(numIgnoredRejectedInvites)",
        ExprType.MVEL, defaultValue, expectedFeatureTypeConfig);
    ExpressionBasedFeatureConfig feature2= new ExpressionBasedFeatureConfig("toNumeric(numGuestInvites)",
        ExprType.MVEL, defaultValue, expectedFeatureTypeConfig);
    Map<String, FeatureConfig> features = new HashMap<>();
    features.put("member_sentInvitations_numIgnoredRejectedInvites", feature1);
    features.put("member_sentInvitations_numGuestInvites", feature2);
    expAnchor2ConfigObj = new AnchorConfigWithKey(source, TypedKey, null, features);
  }

  static final String anchor3ConfigStr = String.join("\n",
      "swaAnchor: {",
      "   source: \"swaSource\"",
      "   key: \"mid\"",
      "   features: {",
      "     simplePageViewCount: {",
      "       def: \"pageView\"",
      "       aggregation: COUNT",
      "       window: 1d",
      "       type: {",
      "         type: \"DENSE_TENSOR\"",
      "         shape: [10]",
      "         dimensionType: [\"INT\"]",
      "         valType: \"FLOAT\"",
      "         doc: \"this is doc\"",
      "       }",
      "     }",
      "     maxPV12h: {",
      "       def: \"pageView\"",
      "       aggregation: MAX",
      "       window: 12h",
      "       groupBy: \"pageKey\"",
      "       limit: 2",
      "       type: {",
      "         type: \"DENSE_TENSOR\"",
      "         shape: [10]",
      "         dimensionType: [\"INT\"]",
      "         valType: \"FLOAT\"",
      "         doc: \"this is doc\"",
      "       }",
      "     }",
      "   }",
      "}");

  static final AnchorConfigWithKey expAnchor3ConfigObj;
  static{
    String source = "swaSource";
    TypedKey TypedKey = new TypedKey("\"mid\"", ExprType.MVEL);
    TypedExpr typedExpr = new TypedExpr("pageView", ExprType.SQL);

    WindowParametersConfig windowParameters1 = new WindowParametersConfig(WindowType.SLIDING, Duration.ofDays(1), null);
    TimeWindowFeatureConfig feature1 = new TimeWindowFeatureConfig(typedExpr,
        TimeWindowAggregationType.COUNT, windowParameters1, null, null, null, null, null, null, expectedFeatureTypeConfig, null);
    WindowParametersConfig windowParameters2 = new WindowParametersConfig(WindowType.SLIDING, Duration.ofHours(12), null);
    TimeWindowFeatureConfig feature2 = new TimeWindowFeatureConfig(typedExpr,
        TimeWindowAggregationType.MAX, windowParameters2, null, "pageKey",2, null,  null, null, expectedFeatureTypeConfig, null);
    Map<String, FeatureConfig> features = new HashMap<>();
    features.put("simplePageViewCount", feature1);
    features.put("maxPV12h", feature2);
    expAnchor3ConfigObj = new AnchorConfigWithKey(source, TypedKey, null, features);
  }

  static final String anchor4ConfigStr = String.join("\n",
      "waterloo-job-term-vectors: {",
      "  source: \"/data/derived/standardization/waterloo/jobs_std_data/test/#LATEST\"",
      "  extractor: \"com.linkedin.frameproto.foundation.anchor.NiceJobFeatures\"",
      "  features: {",
      "    waterloo_job_jobTitle: {",
      "      type: BOOLEAN",
      "    }",
      "    waterloo_job_companyId: {},",
      "    waterloo_job_companySize: {}",
      "  }",
      "}");

  static final AnchorConfigWithExtractor expAnchor4ConfigObj;
  static{
    FeatureTypeConfig featureTypeConfig = new FeatureTypeConfig(FeatureType.BOOLEAN);

    String source = "/data/derived/standardization/waterloo/jobs_std_data/test/#LATEST";
    String extractor = "com.linkedin.frameproto.foundation.anchor.NiceJobFeatures";
    Map<String, FeatureConfig> features = new HashMap<>();
    features.put("waterloo_job_jobTitle", new ExtractorBasedFeatureConfig("waterloo_job_jobTitle", featureTypeConfig));
    features.put("waterloo_job_companyId", new ExtractorBasedFeatureConfig("waterloo_job_companyId"));
    features.put("waterloo_job_companySize", new ExtractorBasedFeatureConfig("waterloo_job_companySize"));
    expAnchor4ConfigObj = new AnchorConfigWithExtractor(source, extractor, features);
  }

  static final String anchor5ConfigStr = String.join("\n",
      "careers-member-education: {",
      "  source: \"/jobs/liar/jymbii-features-engineering/production/memberFeatures/education/#LATEST\"",
      "  transformer: \"com.linkedin.careers.relevance.feathr.offline.anchor.LegacyFeastFormattedFeatures\"",
      "  features: [",
      "    \"careers_member_degree\",",
      "    \"careers_member_rolledUpDegree\",",
      "    \"careers_member_fieldOfStudy\",",
      "  ]",
      "}");

  static final AnchorConfigWithExtractor expAnchor5ConfigObj;
  static{
    String source = "/jobs/liar/jymbii-features-engineering/production/memberFeatures/education/#LATEST";
    String extractor = "com.linkedin.careers.relevance.feathr.offline.anchor.LegacyFeastFormattedFeatures";
    Map<String, FeatureConfig> features = new HashMap<>();
    features.put("careers_member_degree", new ExtractorBasedFeatureConfig("careers_member_degree"));
    features.put("careers_member_rolledUpDegree", new ExtractorBasedFeatureConfig("careers_member_rolledUpDegree"));
    features.put("careers_member_fieldOfStudy", new ExtractorBasedFeatureConfig("careers_member_fieldOfStudy"));
    expAnchor5ConfigObj = new AnchorConfigWithExtractor(source, extractor, features);
  }

  static final String anchor6ConfigStr = String.join("\n",
      "\"careers-job-embedding-0.0.2\": {",
      "  source: \"/jobs/jobrel/careers-embedding-serving/job-embeddings-versions/0.0.2/#LATEST\"",
      "  key: \"getIdFromRawUrn(key.entityUrn)\"",
      "  features: {",
      "    \"careers_job_embedding_0.0.2\": {",
      "      def: \"value.embedding\"",
      "      type: VECTOR",
      "    }",
      "  }",
      "}");

  static final AnchorConfigWithKey expAnchor6ConfigObj;
  static{
    FeatureTypeConfig featureTypeConfig = new FeatureTypeConfig(FeatureType.VECTOR);
    String source = "/jobs/jobrel/careers-embedding-serving/job-embeddings-versions/0.0.2/#LATEST";
    TypedKey TypedKey = new TypedKey("\"getIdFromRawUrn(key.entityUrn)\"", ExprType.MVEL);
    String featureName = "careers_job_embedding_0.0.2";
    String featureExpr = "value.embedding";
    ExpressionBasedFeatureConfig feature = new ExpressionBasedFeatureConfig(featureExpr, featureTypeConfig);
    Map<String, FeatureConfig> features = new HashMap<>();
    features.put(featureName, feature);
    expAnchor6ConfigObj = new AnchorConfigWithKey(source, TypedKey, null, features);
  }

  static final String anchor7ConfigStr = String.join("\n",
      "\"careers-job-embedding-0.0.2\": {",
      "  source: \"/jobs/jobrel/careers-embedding-serving/job-embeddings-versions/0.0.2/#LATEST\"",
      "  key: \"getIdFromRawUrn(key.entityUrn)\"",
      "  features: {",
      "    \"foo:bar\": {",
      "      def: \"value.embedding\"",
      "      type: VECTOR",
      "    }",
      "  }",
      "}");

  static final AnchorConfigWithKey expAnchor7ConfigObj;
  static{
    FeatureTypeConfig featureTypeConfig = new FeatureTypeConfig(FeatureType.VECTOR);
    String source = "/jobs/jobrel/careers-embedding-serving/job-embeddings-versions/0.0.2/#LATEST";
    TypedKey TypedKey = new TypedKey("\"getIdFromRawUrn(key.entityUrn)\"", ExprType.MVEL);
    String featureName = "foo:bar";
    String featureExpr = "value.embedding";
    String featureType = "VECTOR";
    ExpressionBasedFeatureConfig feature = new ExpressionBasedFeatureConfig(featureExpr, featureTypeConfig);
    Map<String, FeatureConfig> features = new HashMap<>();
    features.put(featureName, feature);
    expAnchor7ConfigObj = new AnchorConfigWithKey(source, TypedKey, null, features);
  }

  static final String anchor8ConfigStr = String.join("\n",
      "swaAnchor: {",
      "  source: \"kafkaTestSource\"",
      "  key: \"mid\"",
      "  features: {",
      "    simplePageViewCount: {",
      "      def: \"pageView\"",
      "      aggregation: COUNT",
      "      window: 1d",
      "    }",
      "    maxPV12h: {",
      "      def: \"pageView\"",
      "      aggregation: MAX",
      "      window: 12h",
      "      groupBy: \"pageKey\"",
      "      limit: 2",
      "    }",
      "  }",
      "}");

  static final AnchorConfigWithKey expAnchor8ConfigObj;
  static {
    String source = "kafkaTestSource";
    TypedKey TypedKey = new TypedKey("\"mid\"", ExprType.MVEL);
    WindowParametersConfig windowParameters1 = new WindowParametersConfig(WindowType.SLIDING, Duration.ofDays(1), null);
    TimeWindowFeatureConfig feature1 = new TimeWindowFeatureConfig("pageView",
        TimeWindowAggregationType.COUNT, windowParameters1, null, null, null, null, null);
    WindowParametersConfig windowParameters2 = new WindowParametersConfig(WindowType.SLIDING, Duration.ofHours(12), null);
    TimeWindowFeatureConfig feature2 = new TimeWindowFeatureConfig("pageView",
        TimeWindowAggregationType.MAX, windowParameters2,
        null, "pageKey", 2, null, null);

    Map<String, FeatureConfig> features = new HashMap<>();
    features.put("simplePageViewCount", feature1);
    features.put("maxPV12h", feature2);
    expAnchor8ConfigObj = new AnchorConfigWithKey(source, TypedKey, null, features);
  }

  static final String anchor9ConfigStr = String.join("\n",
      "swaAnchor2: {",
      "  source: windowAgg1dSource",
      "  key: \"substring(x, 15)\"",
      "  lateralViewParameters: {",
      "    lateralViewDef: \"explode(features)\"",
      "    lateralViewItemAlias: feature",
      "  }",
      "  features: {",
      "    articleCount_sum_1d: {",
      "      def: \"feature.col.value\"",
      "      filter: \"feature.col.name = 'articleCount'\"",
      "      aggregation: LATEST",
      "      window: 2 days",
      "    }",
      "  }",
      "}");

  static final AnchorConfigWithKey expAnchor9ConfigObj;
  static {
    String source = "windowAgg1dSource";
    TypedKey TypedKey = new TypedKey("\"substring(x, 15)\"", ExprType.MVEL);

    LateralViewParams lateralViewParams = new LateralViewParams("explode(features)", "feature");

    WindowParametersConfig windowParameters = new WindowParametersConfig(WindowType.SLIDING, Duration.ofDays(2), null);

    TimeWindowFeatureConfig feature1 = new TimeWindowFeatureConfig("feature.col.value",
        TimeWindowAggregationType.LATEST, windowParameters, "feature.col.name = 'articleCount'", null, null, null,
        null);

    Map<String, FeatureConfig> features = new HashMap<>();
    features.put("articleCount_sum_1d", feature1);
    expAnchor9ConfigObj = new AnchorConfigWithKey(source, TypedKey, lateralViewParams, features);
  }

  static final String anchor10ConfigStr = String.join("\n",
      "swaAnchor2: {",
      "  source: windowAgg1dSource",
      "  key: \"substring(x, 15)\"",
      "  lateralViewParameters: {",
      "    lateralViewDef: \"explode(features)\"",
      "    lateralViewItemAlias: feature",
      "  }",
      "  features: {",
      "    facetTitles_sum_30d: {",
      "      def: \"feature.col.value\"",
      "      aggregation: SUM",
      "      groupBy: \"feature.col.term\"",
      "      window: 30 days",
      "    }",
      "  }",
      "}");

  static final AnchorConfigWithKey expAnchor10ConfigObj;
  static {
    String source = "windowAgg1dSource";
    TypedKey TypedKey = new TypedKey("\"substring(x, 15)\"", ExprType.MVEL);

    LateralViewParams lateralViewParams = new LateralViewParams("explode(features)", "feature");

    WindowParametersConfig windowParameters = new WindowParametersConfig(WindowType.SLIDING, Duration.ofDays(30), null);
    TimeWindowFeatureConfig feature1 = new TimeWindowFeatureConfig("feature.col.value",
        TimeWindowAggregationType.SUM, windowParameters, null, "feature.col.term", null, null, null);

    Map<String, FeatureConfig> features = new HashMap<>();
    features.put("facetTitles_sum_30d", feature1);
    expAnchor10ConfigObj = new AnchorConfigWithKey(source, TypedKey, lateralViewParams, features);
  }

  static final String   anchor11ConfigStr = String.join("\n",
      "nearLineFeatureAnchor: {",
      "  source: kafkaTestSource",
      "  key.mvel: mid",
      "  features: {",
      "    feature1: {",
      "      def.mvel: pageView",
      "      aggregation: MAX",
      "      windowParameters: {",
      "         type: SLIDING",
      "         size: 1h",
      "         slidingInterval: 10m",
      "        }",
      "     groupBy: pageKey",
      "     }",
      "     feature2: {",
      "         def.mvel: pageView",
      "         aggregation: MAX",
      "         windowParameters: {",
      "           type: SLIDING",
      "           size: 1h",
      "           slidingInterval: 10m",
      "        }",
      "      groupBy: pageKey",
      "      filter.mvel: \"$.getAsTermVector().keySet()\"",
      "    }",
      "  }",
      "}");

  static final AnchorConfigWithKey expAnchor11ConfigObj;
  static {
    String source = "kafkaTestSource";
    TypedKey TypedKey = new TypedKey("\"mid\"", ExprType.MVEL);
    WindowParametersConfig windowParametersConfig = new WindowParametersConfig(WindowType.SLIDING, Duration.ofHours(1), Duration.ofMinutes(10));
    TimeWindowFeatureConfig feature1 = new TimeWindowFeatureConfig("pageView", ExprType.MVEL,
        TimeWindowAggregationType.MAX, windowParametersConfig, null, null, "pageKey", null, null, null);
    TimeWindowFeatureConfig feature2 = new TimeWindowFeatureConfig("pageView", ExprType.MVEL,
        TimeWindowAggregationType.MAX, windowParametersConfig, "$.getAsTermVector().keySet()", ExprType.MVEL, "pageKey", null, null, null);
    Map<String, FeatureConfig> features = new HashMap<>();
    features.put("feature1", feature1);
    features.put("feature2", feature2);
    expAnchor11ConfigObj = new AnchorConfigWithKey(source, TypedKey, null, features);
  }

  static final String anchor12ConfigStr = String.join("\n",
      "member-sent-invitations: {",
      "  source: \"/jobs/frame/inlab/data/features/InvitationStats\"",
      "  key.sqlExpr: \"x\"",
      "  features: {",
      "    member_sentInvitations_numIgnoredRejectedInvitesV2: {",
      "      def.sqlExpr: \"numIgnoredRejectedInvites\"",
      "      default: 0",
      "    }",
      "    member_sentInvitations_numGuestInvitesV2: {",
      "      def.sqlExpr: \"numGuestInvites\"",
      "      default: 0",
      "    }",
      "  }",
      "}");

  static final AnchorConfigWithKey expAnchor12ConfigObj;
  static{
    String source = "/jobs/frame/inlab/data/features/InvitationStats";
    String defaultValue = "0";
    ExpressionBasedFeatureConfig feature1 = new ExpressionBasedFeatureConfig("numIgnoredRejectedInvites",
        ExprType.SQL, null, defaultValue);
    ExpressionBasedFeatureConfig feature2= new ExpressionBasedFeatureConfig("numGuestInvites",
        ExprType.SQL,null, defaultValue);
    Map<String, FeatureConfig> features = new HashMap<>();
    features.put("member_sentInvitations_numIgnoredRejectedInvitesV2", feature1);
    features.put("member_sentInvitations_numGuestInvitesV2", feature2);
    expAnchor12ConfigObj = new AnchorConfigWithKey(source, new TypedKey("\"x\"", ExprType.SQL), null, features);
  }

  static final String anchor13ConfigStr = String.join("\n",
      "member-sent-invitationsV3: {",
      "  source: \"/jobs/frame/inlab/data/features/InvitationStats\"",
      "  keyExtractor: \"com.linkedin.frameproto.foundation.anchor.NiceJobFeaturesKeyExtractor\"",
      "  features: {",
      "    member_sentInvitations_numIgnoredRejectedInvitesV3: {",
      "      def.sqlExpr: \"numIgnoredRejectedInvites\"",
      "      default: 0",
      "    }",
      "    member_sentInvitations_numGuestInvitesV3: {",
      "      def.sqlExpr: \"numGuestInvites\"",
      "      default: 0",
      "    }",
      "  }",
      "}");

  static final AnchorConfigWithKeyExtractor expAnchor13ConfigObj;
  static{
    String source = "/jobs/frame/inlab/data/features/InvitationStats";
    String keyExtractor = "com.linkedin.frameproto.foundation.anchor.NiceJobFeaturesKeyExtractor";
    String defaultValue = "0";
    ExpressionBasedFeatureConfig feature1 = new ExpressionBasedFeatureConfig("numIgnoredRejectedInvites",
        ExprType.SQL, null, defaultValue);
    ExpressionBasedFeatureConfig feature2= new ExpressionBasedFeatureConfig("numGuestInvites",
        ExprType.SQL,null, defaultValue);
    Map<String, FeatureConfig> features = new HashMap<>();
    features.put("member_sentInvitations_numIgnoredRejectedInvitesV3", feature1);
    features.put("member_sentInvitations_numGuestInvitesV3", feature2);
    expAnchor13ConfigObj = new AnchorConfigWithKeyExtractor(source, keyExtractor, features);
  }

  static final String anchor14ConfigStr = String.join("\n",
      "waterloo-job-term-vectors: {",
      "  source: \"/data/derived/standardization/waterloo/jobs_std_data/test/#LATEST\"",
      "  keyExtractor: \"com.linkedin.frameproto.foundation.anchor.NiceJobFeaturesKeyExtractor\"",
      "  extractor: \"com.linkedin.frameproto.foundation.anchor.NiceJobFeatures\"",
      "  features: [",
      "    waterloo_job_jobTitleV2,",
      "    waterloo_job_companyIdV2,",
      "    waterloo_job_companySizeV2",
      "  ]",
      "}");

  static final AnchorConfigWithExtractor expAnchor14ConfigObj;
  static{
    String source = "/data/derived/standardization/waterloo/jobs_std_data/test/#LATEST";
    String keyExtractor = "com.linkedin.frameproto.foundation.anchor.NiceJobFeaturesKeyExtractor";
    String extractor = "com.linkedin.frameproto.foundation.anchor.NiceJobFeatures";
    Map<String, FeatureConfig> features = new HashMap<>();
    features.put("waterloo_job_jobTitleV2", new ExtractorBasedFeatureConfig("waterloo_job_jobTitleV2"));
    features.put("waterloo_job_companyIdV2", new ExtractorBasedFeatureConfig("waterloo_job_companyIdV2"));
    features.put("waterloo_job_companySizeV2", new ExtractorBasedFeatureConfig("waterloo_job_companySizeV2"));
    expAnchor14ConfigObj = new AnchorConfigWithExtractor(source, keyExtractor, extractor, features);
  }

  // extractor with keyAlias
  static final String anchor15ConfigStr = String.join("\n",
      "waterloo-job-term-vectors: {",
      "  source: \"/data/derived/standardization/waterloo/jobs_std_data/test/#LATEST\"",
      "  keyAlias: [key1, key2]",
      "  extractor: \"com.linkedin.frameproto.foundation.anchor.NiceJobFeatures\"",
      "  features: {",
      "    waterloo_job_jobTitle: {",
      "      type: BOOLEAN",
      "    }",
      "    waterloo_job_companyId: {},",
      "    waterloo_job_companySize: {}",
      "  }",
      "}");

  static final AnchorConfigWithExtractor expAnchor15ConfigObj;
  static{
    FeatureTypeConfig featureTypeConfig = new FeatureTypeConfig(FeatureType.BOOLEAN);

    String source = "/data/derived/standardization/waterloo/jobs_std_data/test/#LATEST";
    String extractor = "com.linkedin.frameproto.foundation.anchor.NiceJobFeatures";
    Map<String, FeatureConfig> features = new HashMap<>();
    features.put("waterloo_job_jobTitle", new ExtractorBasedFeatureConfig("waterloo_job_jobTitle", featureTypeConfig));
    features.put("waterloo_job_companyId", new ExtractorBasedFeatureConfig("waterloo_job_companyId"));
    features.put("waterloo_job_companySize", new ExtractorBasedFeatureConfig("waterloo_job_companySize"));
    expAnchor15ConfigObj = new AnchorConfigWithExtractor(source, null, null,
        Arrays.asList("key1", "key2"), extractor, features);
  }

  // key and keyAlias co-exist
  static final String anchor16ConfigStr = String.join("\n",
      "\"careers-job-embedding-0.0.2\": {",
      "  source: \"/jobs/jobrel/careers-embedding-serving/job-embeddings-versions/0.0.2/#LATEST\"",
      "  key: \"getIdFromRawUrn(key.entityUrn, key.someProperty)\"",
      "  keyAlias: \"keyAlias1\"",
      "  features: {",
      "    \"foo:bar\": {",
      "      def: \"value.embedding\"",
      "      type: VECTOR",
      "    }",
      "  }",
      "}");

  static final AnchorConfigWithKey expAnchor16ConfigObj;
  static{
    FeatureTypeConfig featureTypeConfig = new FeatureTypeConfig(FeatureType.VECTOR);
    String source = "/jobs/jobrel/careers-embedding-serving/job-embeddings-versions/0.0.2/#LATEST";
    TypedKey TypedKey =
        new TypedKey( "\"getIdFromRawUrn(key.entityUrn, key.someProperty)\"", ExprType.MVEL);
    List<String> keyAlias = Collections.singletonList("keyAlias1");
    String featureName = "foo:bar";
    String featureExpr = "value.embedding";
    String featureType = "VECTOR";
    ExpressionBasedFeatureConfig feature = new ExpressionBasedFeatureConfig(featureExpr, featureTypeConfig);
    Map<String, FeatureConfig> features = new HashMap<>();
    features.put(featureName, feature);
    expAnchor16ConfigObj = new AnchorConfigWithKey(source, TypedKey, keyAlias, null, features);
  }

  // key size and keyAlias size do not match
  static final String anchor17ConfigStr = String.join("\n",
      "\"careers-job-embedding-0.0.2\": {",
      "  source: \"/jobs/jobrel/careers-embedding-serving/job-embeddings-versions/0.0.2/#LATEST\"",
      "  key: \"getIdFromRawUrn(key.entityUrn)\"",
      "  keyAlias: [keyAlias1, keyAlias2]",
      "  features: {",
      "    \"foo:bar\": {",
      "      def: \"value.embedding\"",
      "      type: VECTOR",
      "    }",
      "  }",
      "}");

  // invalid case where keyExtractor and keyAlias coexist
  static final String anchor18ConfigStr = String.join("\n",
      "member-sent-invitationsV3: {",
      "  source: \"/jobs/frame/inlab/data/features/InvitationStats\"",
      "  keyExtractor: \"com.linkedin.frameproto.foundation.anchor.NiceJobFeaturesKeyExtractor\"",
      "  keyAlias: [key1, key2]",
      "  features: {",
      "    member_sentInvitations_numIgnoredRejectedInvitesV3: {",
      "      def.sqlExpr: \"numIgnoredRejectedInvites\"",
      "      default: 0",
      "    }",
      "    member_sentInvitations_numGuestInvitesV3: {",
      "      def.sqlExpr: \"numGuestInvites\"",
      "      default: 0",
      "    }",
      "  }",
      "}");

  // extractor with keyAlias and key
  static final String anchor19ConfigStr = String.join("\n",
      "waterloo-job-term-vectors: {",
      "  source: \"/data/derived/standardization/waterloo/jobs_std_data/test/#LATEST\"",
      "  key.sqlExpr: [key1, key2]",
      "  keyAlias: [keyAlias1, keyAlias2]",
      "  extractor: \"com.linkedin.frameproto.foundation.anchor.NiceJobFeatures\"",
      "  features: {",
      "    waterloo_job_jobTitle: {",
      "      type: BOOLEAN",
      "    }",
      "    waterloo_job_companyId: {},",
      "    waterloo_job_companySize: {}",
      "  }",
      "}");

  static final AnchorConfigWithExtractor expAnchor19ConfigObj;
  static{
    FeatureTypeConfig featureTypeConfig = new FeatureTypeConfig(FeatureType.BOOLEAN);

    String source = "/data/derived/standardization/waterloo/jobs_std_data/test/#LATEST";
    String extractor = "com.linkedin.frameproto.foundation.anchor.NiceJobFeatures";
    TypedKey TypedKey = new TypedKey("[key1, key2]", ExprType.SQL);
    Map<String, FeatureConfig> features = new HashMap<>();
    features.put("waterloo_job_jobTitle", new ExtractorBasedFeatureConfig("waterloo_job_jobTitle", featureTypeConfig));
    features.put("waterloo_job_companyId", new ExtractorBasedFeatureConfig("waterloo_job_companyId"));
    features.put("waterloo_job_companySize", new ExtractorBasedFeatureConfig("waterloo_job_companySize"));
    expAnchor19ConfigObj = new AnchorConfigWithExtractor(source, null, TypedKey,
        Arrays.asList("keyAlias1", "keyAlias2"), extractor, features);
  }

  // extractor with keyExtractor and key
  static final String anchor20ConfigStr = String.join("\n",
      "waterloo-job-term-vectors: {",
      "  source: \"/data/derived/standardization/waterloo/jobs_std_data/test/#LATEST\"",
      "  key.sqlExpr: [key1, key2]",
      "  keyExtractor: \"com.linkedin.frameproto.foundation.anchor.NiceJobFeaturesKeyExtractor\"",
      "  extractor: \"com.linkedin.frameproto.foundation.anchor.NiceJobFeatures\"",
      "  features: {",
      "    waterloo_job_jobTitle: {",
      "      type: BOOLEAN",
      "    }",
      "    waterloo_job_companyId: {},",
      "    waterloo_job_companySize: {}",
      "  }",
      "}");

  // extractor with keyExtractor and lateralViewParameters
  static final String anchor21ConfigStr = String.join("\n",
      "swaAnchor2: {",
      "  source: windowAgg1dSource",
      "  keyExtractor: \"com.linkedin.frameproto.foundation.anchor.NiceJobFeaturesKeyExtractor\"",
      "  lateralViewParameters: {",
      "    lateralViewDef: \"explode(features)\"",
      "    lateralViewItemAlias: feature",
      "  }",
      "  features: {",
      "    facetTitles_sum_30d: {",
      "      def: \"feature.col.value\"",
      "      aggregation: SUM",
      "      groupBy: \"feature.col.term\"",
      "      window: 30 days",
      "    }",
      "  }",
      "}");

  static final AnchorConfigWithKeyExtractor expAnchor21ConfigObj;
  static {
    String source = "windowAgg1dSource";

    String keyExtractor = "com.linkedin.frameproto.foundation.anchor.NiceJobFeaturesKeyExtractor";
    LateralViewParams lateralViewParams = new LateralViewParams("explode(features)", "feature");

    WindowParametersConfig windowParameters = new WindowParametersConfig(WindowType.SLIDING, Duration.ofDays(30), null);
    TimeWindowFeatureConfig feature1 = new TimeWindowFeatureConfig("feature.col.value",
        TimeWindowAggregationType.SUM, windowParameters, null, "feature.col.term", null, null, null);

    Map<String, FeatureConfig> features = new HashMap<>();
    features.put("facetTitles_sum_30d", feature1);
    expAnchor21ConfigObj = new AnchorConfigWithKeyExtractor(source, keyExtractor, features, lateralViewParams);
  }

  static final String anchor22ConfigStr = String.join("\n",
      "waterloo-job-term-vectors: {",
      "  source: \"/data/derived/standardization/waterloo/jobs_std_data/test/#LATEST\"",
      "  keyExtractor: \"com.linkedin.frameproto.foundation.anchor.NiceJobFeaturesKeyExtractor\"",
      "  extractor: \"com.linkedin.frameproto.foundation.anchor.NiceJobFeatures\"",
      "  features: {",
      "    waterloo_job_jobTitleV2 : {",
      "       parameters: {",
      "         param1 : [waterlooCompany_terms_hashed, waterlooCompany_values]",
      "         param2 : [waterlooCompany_terms_hashed, waterlooCompany_values]",
      "       }",
      "    }",
      "  }",
      "}");

  static final AnchorConfigWithExtractor expAnchor22ConfigObj;
  static{
    String source = "/data/derived/standardization/waterloo/jobs_std_data/test/#LATEST";
    String keyExtractor = "com.linkedin.frameproto.foundation.anchor.NiceJobFeaturesKeyExtractor";
    String extractor = "com.linkedin.frameproto.foundation.anchor.NiceJobFeatures";
    Map<String, FeatureConfig> features = new HashMap<>();
    features.put("waterloo_job_jobTitleV2", new ExtractorBasedFeatureConfig(
        "waterloo_job_jobTitleV2", null, null,
        ImmutableMap.of("param1", "[\"waterlooCompany_terms_hashed\",\"waterlooCompany_values\"]",
            "param2", "[\"waterlooCompany_terms_hashed\",\"waterlooCompany_values\"]")));
    expAnchor22ConfigObj = new AnchorConfigWithExtractor(
        source, keyExtractor, null, null, extractor, features);
  }

  static final String anchor23ConfigStr = String.join("\n",
      "waterloo-job-term-vectors: {",
      "  source: \"/data/derived/standardization/waterloo/jobs_std_data/test/#LATEST\"",
      "  keyExtractor: \"com.linkedin.frameproto.foundation.anchor.NiceJobFeaturesKeyExtractor\"",
      "  extractor: \"com.linkedin.frameproto.foundation.anchor.NiceJobFeatures\"",
      "  features: {",
      "    waterloo_job_jobTitleV2 : {",
      "       parameters: {",
      "         param1 : [waterlooCompany_terms_hashed, waterlooCompany_values]",
      "         param2 : [waterlooCompany_terms_hashed, waterlooCompany_values]",
      "       }",
      "       default: true",
      "       type: BOOLEAN",
      "    }",
      "  }",
      "}");

  static final AnchorConfigWithExtractor expAnchor23ConfigObj;
  static{
    String source = "/data/derived/standardization/waterloo/jobs_std_data/test/#LATEST";
    String keyExtractor = "com.linkedin.frameproto.foundation.anchor.NiceJobFeaturesKeyExtractor";
    String extractor = "com.linkedin.frameproto.foundation.anchor.NiceJobFeatures";
    Map<String, String> parameters = new HashMap<>();
    parameters.put("param1", "[\"waterlooCompany_terms_hashed\", \"waterlooCompany_values\"]");
    Map<String, FeatureConfig> features = new HashMap<>();
    features.put("waterloo_job_jobTitleV2", new ExtractorBasedFeatureConfig(
        "waterloo_job_jobTitleV2", new FeatureTypeConfig(FeatureType.BOOLEAN), "true",
        ImmutableMap.of("param1", "[\"waterlooCompany_terms_hashed\",\"waterlooCompany_values\"]",
            "param2", "[\"waterlooCompany_terms_hashed\",\"waterlooCompany_values\"]")));
    expAnchor23ConfigObj = new AnchorConfigWithExtractor(
        source, keyExtractor, null, null, extractor, features);
  }

  static final String anchorsConfigStr = String.join("\n",
      "anchors: {",
      anchor1ConfigStr,
      anchor2ConfigStr,
      anchor3ConfigStr,
      anchor4ConfigStr,
      "}");

  static final AnchorsConfig expAnchorsConfig;
  static{
    Map<String, AnchorConfig> anchors = new HashMap<>();
    anchors.put("member-lix-segment", expAnchor1ConfigObj);
    anchors.put("member-sent-invitations", expAnchor2ConfigObj);
    anchors.put("swaAnchor", expAnchor3ConfigObj);
    anchors.put("waterloo-job-term-vectors", expAnchor4ConfigObj);
    expAnchorsConfig = new AnchorsConfig(anchors);
  }

}
