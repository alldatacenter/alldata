package com.linkedin.feathr.core.configbuilder.typesafe.producer.anchors;

import com.linkedin.feathr.core.configbuilder.typesafe.AbstractConfigBuilderTest;
import com.linkedin.feathr.core.config.ConfigObj;
import com.linkedin.feathr.core.config.producer.anchors.ComplexFeatureConfig;
import com.linkedin.feathr.core.config.producer.anchors.LateralViewParams;
import com.linkedin.feathr.core.config.producer.anchors.SimpleFeatureConfig;
import com.linkedin.feathr.core.config.producer.anchors.TimeWindowFeatureConfig;
import com.linkedin.feathr.core.config.producer.common.FeatureTypeConfig;
import com.linkedin.feathr.core.configbuilder.ConfigBuilderException;
import com.typesafe.config.Config;
import java.util.function.BiFunction;
import org.testng.annotations.Test;

import static com.linkedin.feathr.core.configbuilder.typesafe.producer.anchors.AnchorsFixture.*;


public class AnchorConfigBuilderTest extends AbstractConfigBuilderTest {

  BiFunction<String, Config, ConfigObj> configBuilder = AnchorConfigBuilder::build;

  @Test(description = "Tests build of anchor config object with key and Simple Feature")
  public void testWithSimpleFeature() {
    testConfigBuilder(anchor1ConfigStr, configBuilder, expAnchor1ConfigObj);
  }

  @Test(description = "Tests build of anchor config object with key and Complex Feature")
  public void testWithComplexFeature() {
    testConfigBuilder(anchor2ConfigStr, configBuilder, expAnchor2ConfigObj);
  }

  @Test(description = "Tests build of anchor config object with key and Time-Window Feature")
  public void testWithTimeWindowFeature() {
    testConfigBuilder(anchor3ConfigStr, configBuilder, expAnchor3ConfigObj);
  }

  @Test(description = "Tests build of anchor config object that contains a feature name with forbidden char '.'")
  public void testWithSpecialCharacter1() {
    testConfigBuilder(anchor6ConfigStr, configBuilder, expAnchor6ConfigObj);
  }

  @Test(description = "Tests build of anchor config object that contains a feature name with forbidden char ':'")
  public void testWithSpecialCharacter2() {
    testConfigBuilder(anchor7ConfigStr, configBuilder, expAnchor7ConfigObj);
  }

  @Test(description = "Tests build of anchor config object with key and Time-Window Feature with optional slidingInterval")
  public void testWithTimeWindowFeature2() {
    testConfigBuilder(anchor8ConfigStr, configBuilder, expAnchor8ConfigObj);
  }

  @Test(description = "Tests build of anchor config object with key and Time-Window Feature with lateral view params")
  public void testWithLateralViewParams() {
    testConfigBuilder(anchor9ConfigStr, configBuilder, expAnchor9ConfigObj);
  }

  @Test(description = "Tests build of anchor config object with key and Time-Window Feature with lateral view params with filter")
  public void testWithLateralViewParamsWithFilter() {
    testConfigBuilder(anchor10ConfigStr, configBuilder, expAnchor10ConfigObj);
  }

  @Test(description = "Tests build of anchor config object with key and feature def defined in SQL expression")
  public void testWithSqlExpr() {
    testConfigBuilder(anchor12ConfigStr, configBuilder, expAnchor12ConfigObj);
  }

  @Test(description = "Tests build of anchor config object with keyExtractor only ")
  public void testWithKeyExtractor() {
    testConfigBuilder(anchor13ConfigStr, configBuilder, expAnchor13ConfigObj);
  }

  @Test(description = "Tests build of anchor config object with keyExtractor and extractor ")
  public void testWithKeyExtractorAndExtractor() {
    testConfigBuilder(anchor14ConfigStr, configBuilder, expAnchor14ConfigObj);
  }

  @Test(description = "Tests build of anchor config object with extractor")
  public void testWithExtractor() {
    testConfigBuilder(anchor4ConfigStr, configBuilder, expAnchor4ConfigObj);
  }

  @Test(description = "Tests build of anchor config object with extractor and keyAlias fields")
  public void testExtractorWithKeyAlias() {
    testConfigBuilder(anchor15ConfigStr, configBuilder, expAnchor15ConfigObj);
  }

  @Test(description = "Tests build of anchor config object with key and keyAlias fields")
  public void testKeyWithKeyAlias() {
    testConfigBuilder(anchor16ConfigStr, configBuilder, expAnchor16ConfigObj);
  }

  @Test(description = "Tests build of anchor config object with extractor, key, and keyAlias fields")
  public void testExtractorWithKeyAndKeyAlias() {
    testConfigBuilder(anchor19ConfigStr, configBuilder, expAnchor19ConfigObj);
  }

  @Test(description = "Tests build of anchor config object with extractor, keyExtractor, and lateralView fields")
  public void testExtractorWithKeyExtractorAndLateralView() {
    testConfigBuilder(anchor21ConfigStr, configBuilder, expAnchor21ConfigObj);
  }

  @Test(description = "Tests build of anchor config object with mismatched key and keyAlias",
          expectedExceptions = ConfigBuilderException.class)
  public void testKeyWithKeyAliasSizeMismatch() {
    testConfigBuilder(anchor17ConfigStr, configBuilder, null);
  }

  @Test(description = "Tests build of anchor config object with both keyExtractor and keyAlias",
          expectedExceptions = ConfigBuilderException.class)
  public void testKeyExtractorWithKeyAlias() {
    testConfigBuilder(anchor18ConfigStr, configBuilder, null);
  }

  @Test(description = "Tests build of anchor config object with extractor, keyExtractor, and key fields",
      expectedExceptions = ConfigBuilderException.class)
  public void testExtractorWithKeyAndKeyExtractor() {
    testConfigBuilder(anchor20ConfigStr, configBuilder, null);
  }

  @Test(description = "Tests build of anchor config object with (deprecated) transformer")
  public void testWithTransformer() {
    testConfigBuilder(anchor5ConfigStr, configBuilder, expAnchor5ConfigObj);
  }

  @Test(description = "Tests build of anchor config object with key and NearLine Feature with Window parameters")
  public void testWithNearlineFeature() {
    testConfigBuilder(anchor11ConfigStr, configBuilder, expAnchor11ConfigObj);
  }

  @Test(description = "Tests build of anchor config object with parameterized extractor")
  public void testParameterizedExtractor() {
    testConfigBuilder(anchor22ConfigStr, configBuilder, expAnchor22ConfigObj);
  }

  @Test(description = "Tests build of anchor config object with parameterized extractor with other fields")
  public void testParameterizedExtractorWithOtherFields() {
    testConfigBuilder(anchor23ConfigStr, configBuilder, expAnchor23ConfigObj);
  }

  @Test(description = "Tests equals and hashCode of various config classes")
  public void testEqualsAndHashCode() {
    super.testEqualsAndHashCode(SimpleFeatureConfig.class, "_configStr");
    super.testEqualsAndHashCode(ComplexFeatureConfig.class, "_configStr");
    super.testEqualsAndHashCode(TimeWindowFeatureConfig.class, "_configStr");
    super.testEqualsAndHashCode(LateralViewParams.class, "_configStr");
    super.testEqualsAndHashCode(FeatureTypeConfig.class, "_configStr");
  }
}
