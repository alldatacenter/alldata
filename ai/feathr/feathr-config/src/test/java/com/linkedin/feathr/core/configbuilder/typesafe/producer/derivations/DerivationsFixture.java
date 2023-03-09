package com.linkedin.feathr.core.configbuilder.typesafe.producer.derivations;

import com.linkedin.feathr.core.config.producer.ExprType;
import com.linkedin.feathr.core.config.producer.TypedExpr;
import com.linkedin.feathr.core.config.producer.common.FeatureTypeConfig;
import com.linkedin.feathr.core.config.producer.definitions.FeatureType;
import com.linkedin.feathr.core.config.producer.derivations.BaseFeatureConfig;
import com.linkedin.feathr.core.config.producer.derivations.DerivationConfig;
import com.linkedin.feathr.core.config.producer.derivations.DerivationConfigWithExpr;
import com.linkedin.feathr.core.config.producer.derivations.DerivationConfigWithExtractor;
import com.linkedin.feathr.core.config.producer.derivations.DerivationsConfig;
import com.linkedin.feathr.core.config.producer.derivations.KeyedFeature;
import com.linkedin.feathr.core.config.producer.derivations.SequentialJoinConfig;
import com.linkedin.feathr.core.config.producer.derivations.SimpleDerivationConfig;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


class DerivationsFixture {

  static final String derivation1ConfigStr = "featureX: \"featureA + featureB\"";

  static final String derivation1ConfigStrWithSpecialChars = "\"fea:ture.X\": \"\"fe.atureA\" + featureB\"";

  static final SimpleDerivationConfig expDerivation1ConfigObj =
      new SimpleDerivationConfig(new TypedExpr("featureA + featureB", ExprType.MVEL));

  static final SimpleDerivationConfig expDerivation1ConfigObjWithSpecialChars =
      new SimpleDerivationConfig("fe.atureA + featureB");

  static final FeatureTypeConfig expectedFeatureTypeConfig =
      new FeatureTypeConfig.Builder().setFeatureType(FeatureType.DENSE_TENSOR)
          .setShapes(Collections.singletonList(10))
          .setDimensionTypes(Collections.singletonList("INT"))
          .setValType("FLOAT")
          .build();

  static final String derivationConfigStrWithType = String.join("\n",
      "abuse_member_invitation_inboundOutboundSkew:{",
      "  definition: \"case when abuse_member_invitation_numInviters = 0 then -1 else abuse_member_invitation_numInvites/abuse_member_invitation_numInviters end\"",
      "  type: {",
      "    type: \"DENSE_TENSOR\"",
      "    shape: [10]",
      "    dimensionType: [\"INT\"]",
      "    valType: \"FLOAT\"",
      "  }",
      "}");

  static final String derivationConfigStrWithSqlExpr = String.join("\n",
      "abuse_member_invitation_inboundOutboundSkew:{",
      "  sqlExpr: \"case when abuse_member_invitation_numInviters = 0 then -1 else abuse_member_invitation_numInvites/abuse_member_invitation_numInviters end\"",
      "  type: {",
      "    type: \"DENSE_TENSOR\"",
      "    shape: [10]",
      "    dimensionType: [\"INT\"]",
      "    valType: \"FLOAT\"",
      "  }",
      "}");

  static final SimpleDerivationConfig expDerivationConfigObjWithSqlExpr =
      new SimpleDerivationConfig(new TypedExpr("case when abuse_member_invitation_numInviters = 0 then -1 else "
              + "abuse_member_invitation_numInvites/abuse_member_invitation_numInviters end",
          ExprType.SQL), expectedFeatureTypeConfig);

  static final SimpleDerivationConfig expDerivationConfigObjWithDef =
      new SimpleDerivationConfig(new TypedExpr("case when abuse_member_invitation_numInviters = 0 then -1 else "
          + "abuse_member_invitation_numInvites/abuse_member_invitation_numInviters end",
          ExprType.MVEL), expectedFeatureTypeConfig);

  static final String derivation2ConfigStr = String.join("\n",
      "featureZ: {",
      " key: [m, j]",
      " inputs: {",
      "   foo: {key: m, feature: featureA},",
      "   bar: {key: j, feature: featureC}",
      " }",
      " definition: \"cosineSimilarity(foo, bar)\"",
      " type: {",
      "   type: \"DENSE_TENSOR\"",
      "   shape: [10]",
      "   dimensionType: [\"INT\"]",
      "   valType: \"FLOAT\"",
      " }",
      "}");

  static final DerivationConfigWithExpr expDerivation2ConfigObj;
  static {
    List<String> keys = Arrays.asList("m", "j");
    Map<String, KeyedFeature> inputs = new HashMap<>();
    inputs.put("foo", new KeyedFeature("m", "featureA"));
    inputs.put("bar", new KeyedFeature("j", "featureC"));

    String definition = "cosineSimilarity(foo, bar)";
    expDerivation2ConfigObj = new DerivationConfigWithExpr(keys, inputs, new TypedExpr(definition, ExprType.MVEL), expectedFeatureTypeConfig);
  }

  static final String derivation3ConfigStr = String.join("\n",
      "jfu_member_placeSimTopK: {",
      "  key: [member]",
      "  inputs: [{key: member, feature: jfu_resolvedPreference_location}]",
      "  class: \"com.linkedin.jymbii.nice.derived.MemberPlaceSimTopK\"",
      "  type: {",
      "    type: \"DENSE_TENSOR\"",
      "    shape: [10]",
      "    dimensionType: [\"INT\"]",
      "    valType: \"FLOAT\"",
      "  }",
      "}");

  static final DerivationConfigWithExtractor expDerivation3ConfigObj;
  static {
    List<String> keys = Collections.singletonList("member");
    List<KeyedFeature> inputs = Collections.singletonList(
        new KeyedFeature("member", "jfu_resolvedPreference_location"));
    String className = "com.linkedin.jymbii.nice.derived.MemberPlaceSimTopK";
    expDerivation3ConfigObj = new DerivationConfigWithExtractor(keys, inputs, className, expectedFeatureTypeConfig);
  }

  static final String derivation4ConfigStr = String.join("\n",
      "sessions_v2_macrosessions_sum_sqrt_7d: {",
      "  key: id",
      "  inputs: {",
      "    sessions_v2_macrosessions_sum_7d: {key: id, feature: sessions_v2_macrosessions_sum_7d},",
      "  }\n",
      "  definition.sqlExpr: \"sqrt(sessions_v2_macrosessions_sum_7d)\"",
      "  type: {",
      "    type: \"DENSE_TENSOR\"",
      "    shape: [10]",
      "    dimensionType: [\"INT\"]",
      "    valType: \"FLOAT\"",
      "  }",
      "}");

  static final DerivationConfigWithExpr expDerivation4ConfigObj;
  static {
    List<String> keys = Collections.singletonList("id");
    Map<String, KeyedFeature> inputs = new HashMap<>();
    inputs.put("sessions_v2_macrosessions_sum_7d",
        new KeyedFeature("id", "sessions_v2_macrosessions_sum_7d"));

    String definition = "sqrt(sessions_v2_macrosessions_sum_7d)";
    expDerivation4ConfigObj = new DerivationConfigWithExpr(keys, inputs, new TypedExpr(definition, ExprType.SQL), expectedFeatureTypeConfig);
  }

  static final String sequentialJoin1ConfigStr = String.join("\n",
      "seq_join_feature1: { ",
      "  key: \"x\" ",
      "  join: { ",
      "    base: { key: x, feature: MemberIndustryId } ",
      "    expansion: { key: skillId, feature: MemberIndustryName } ",
      "  } ",
      "  aggregation:\"\"",
      "}");

  static final SequentialJoinConfig expSequentialJoin1ConfigObj;
  static {
    List<String> keys = Collections.singletonList("x");
    String baseKeyExpr = "\"x\"";
    BaseFeatureConfig base = new BaseFeatureConfig(baseKeyExpr, "MemberIndustryId", null, null, null);
    KeyedFeature expansion = new KeyedFeature("skillId", "MemberIndustryName");
    expSequentialJoin1ConfigObj = new SequentialJoinConfig(keys, base, expansion, "");
  }

  static final String sequentialJoin2ConfigStr = String.join("\n",
      "seq_join_feature2: { ",
      "  key: \"x\"",
      "  join: { ",
      "    base: { key: x,",
      "      feature: MemberIndustryId,",
      "      outputKey: x,",
      "      transformation: \"import com.linkedin.frame.MyFeatureUtils; MyFeatureUtils.dotProduct(MemberIndustryId);\"} ",
      "    expansion: { key: key.entityUrn, feature: MemberIndustryName }",
      "  } ",
      "  aggregation:\"ELEMENTWISE_MAX\"",
      "  type: {",
      "    type: \"DENSE_TENSOR\"",
      "    shape: [10]",
      "    dimensionType: [\"INT\"]",
      "    valType: \"FLOAT\"",
      "  }",
      "}");

  static final SequentialJoinConfig expSequentialJoin2ConfigObj;
  static {
    List<String> keys = Collections.singletonList("x");
    String baseKeyExpr = "\"x\"";
    List<String> baseOutputKeys = Collections.singletonList("x");
    BaseFeatureConfig base = new BaseFeatureConfig(baseKeyExpr, "MemberIndustryId", baseOutputKeys,
        "import com.linkedin.frame.MyFeatureUtils; MyFeatureUtils.dotProduct(MemberIndustryId);", null);
    KeyedFeature expansion = new KeyedFeature("\"key.entityUrn\"", "MemberIndustryName");
    expSequentialJoin2ConfigObj = new SequentialJoinConfig(keys, base, expansion, "ELEMENTWISE_MAX", expectedFeatureTypeConfig);
  }

  static final String sequentialJoinWithTransformationClassConfigStr = String.join("\n",
      "seq_join_feature: { ",
      "  key: \"x\"",
      "  join: { ",
      "    base: { key: x,",
      "      feature: MemberIndustryId,",
      "      outputKey: x,",
      "      transformationClass: \"com.linkedin.frame.MyFeatureTransformer\"} ",
      "    expansion: { key: key.entityUrn, feature: MemberIndustryName }",
      "  } ",
      "  aggregation:\"ELEMENTWISE_MAX\"",
      "}");

  static final SequentialJoinConfig expSequentialJoinWithTransformationClassConfigObj;
  static {
    List<String> keys = Collections.singletonList("x");
    String baseKeyExpr = "\"x\"";
    List<String> baseOutputKeys = Collections.singletonList("x");
    BaseFeatureConfig base = new BaseFeatureConfig(baseKeyExpr, "MemberIndustryId", baseOutputKeys, null,
        "com.linkedin.frame.MyFeatureTransformer");
    KeyedFeature expansion = new KeyedFeature("\"key.entityUrn\"", "MemberIndustryName");
    expSequentialJoinWithTransformationClassConfigObj = new SequentialJoinConfig(keys, base, expansion, "ELEMENTWISE_MAX");
  }

  static final String sequentialJoinWithInvalidTransformationConfigStr = String.join("\n",
      "seq_join_feature: { ",
      "  key: \"x\"",
      "  join: { ",
      "    base: { key: x,",
      "      feature: MemberIndustryId,",
      "      outputKey: x,",
      "      transformation: \"import com.linkedin.frame.MyFeatureUtils; MyFeatureUtils.dotProduct(MemberIndustryId);\"",
      "      transformationClass: \"com.linkedin.frame.MyFeatureTransformer\"} ",
      "    expansion: { key: key.entityUrn, feature: MemberIndustryName }",
      "  } ",
      "  aggregation:\"ELEMENTWISE_MAX\"",
      "}");

  static final String derivationsConfigStr = String.join("\n",
      "derivations: {",
      derivation1ConfigStr,
      derivation2ConfigStr,
      derivation3ConfigStr,
      "}");

  static final DerivationsConfig expDerivationsConfigObj;
  static {
    Map<String, DerivationConfig> derivations = new HashMap<>();

    derivations.put("featureX", expDerivation1ConfigObj);
    derivations.put("featureZ", expDerivation2ConfigObj);
    derivations.put("jfu_member_placeSimTopK", expDerivation3ConfigObj);

    expDerivationsConfigObj = new DerivationsConfig(derivations);
  }
}
