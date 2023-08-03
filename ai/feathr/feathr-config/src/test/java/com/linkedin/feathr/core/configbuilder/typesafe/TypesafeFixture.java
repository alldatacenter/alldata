package com.linkedin.feathr.core.configbuilder.typesafe;

import com.linkedin.feathr.core.config.producer.FeatureDefConfig;
import com.linkedin.feathr.core.config.producer.anchors.AnchorConfig;
import com.linkedin.feathr.core.config.producer.anchors.AnchorsConfig;
import com.linkedin.feathr.core.config.producer.sources.SourceConfig;
import com.linkedin.feathr.core.config.producer.sources.SourcesConfig;
import java.util.HashMap;
import java.util.Map;

import static com.linkedin.feathr.core.configbuilder.typesafe.producer.anchors.AnchorsFixture.*;
import static com.linkedin.feathr.core.configbuilder.typesafe.producer.sources.SourcesFixture.*;


class TypesafeFixture {

  static final FeatureDefConfig expFeatureDef1ConfigObj;
  static {
    Map<String, AnchorConfig> anchors = new HashMap<>();
    anchors.put("member-lix-segment", expAnchor1ConfigObj);
    AnchorsConfig anchorsConfigObj = new AnchorsConfig(anchors);
    expFeatureDef1ConfigObj = new FeatureDefConfig(null, anchorsConfigObj, null);
  }

  static final FeatureDefConfig expFeatureDef2ConfigObj;
  static {
    Map<String, SourceConfig> sources = new HashMap<>();
    sources.put("MemberPreferenceData", expEspressoSource1ConfigObj);
    sources.put("member_derived_data", expHdfsSource1ConfigObj);
    SourcesConfig sourcesConfigObj = new SourcesConfig(sources);

    Map<String, AnchorConfig> anchors = new HashMap<>();
    anchors.put("member-lix-segment", expAnchor1ConfigObj);
    AnchorsConfig anchorsConfigObj = new AnchorsConfig(anchors);
    expFeatureDef2ConfigObj = new FeatureDefConfig(sourcesConfigObj, anchorsConfigObj, null);
  }
}
