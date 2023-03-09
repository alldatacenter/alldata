package com.linkedin.feathr.core.configbuilder.typesafe.producer.derivations;

import com.linkedin.feathr.core.configbuilder.typesafe.AbstractConfigBuilderTest;
import org.testng.annotations.Test;


public class DerivationsConfigBuilderTest extends AbstractConfigBuilderTest {

  @Test
  public void derivationsTest() {
    testConfigBuilder(
        DerivationsFixture.derivationsConfigStr, DerivationsConfigBuilder::build, DerivationsFixture.expDerivationsConfigObj);
  }
}
