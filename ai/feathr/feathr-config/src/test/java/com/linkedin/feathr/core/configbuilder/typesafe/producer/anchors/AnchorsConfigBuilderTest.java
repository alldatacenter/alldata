package com.linkedin.feathr.core.configbuilder.typesafe.producer.anchors;

import com.linkedin.feathr.core.configbuilder.typesafe.AbstractConfigBuilderTest;
import org.testng.annotations.Test;

import static com.linkedin.feathr.core.configbuilder.typesafe.producer.anchors.AnchorsFixture.*;


public class AnchorsConfigBuilderTest extends AbstractConfigBuilderTest {

  @Test(description = "Tests build of all anchor config objects that may contain key or extractor")
  public void anchorsTest() {
    testConfigBuilder(anchorsConfigStr, AnchorsConfigBuilder::build, expAnchorsConfig);
  }
}
