package com.linkedin.feathr.core.configbuilder.typesafe.producer.derivations;

import com.linkedin.feathr.core.config.producer.derivations.DerivationConfig;
import com.linkedin.feathr.core.configbuilder.ConfigBuilderException;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;


public class DerivationConfigBuilderTest {

  @Test
  public void testSimpleDerivation() {
    testDerivation(DerivationsFixture.derivation1ConfigStr, DerivationsFixture.expDerivation1ConfigObj);
  }

  @Test
  public void testSimpleDerivationWithSpecialCharacters() {
    testDerivation(
        DerivationsFixture.derivation1ConfigStrWithSpecialChars, DerivationsFixture.expDerivation1ConfigObjWithSpecialChars);
  }

  @Test
  public void testSimpleDerivationWithSqlExpr() {
    testDerivation(
        DerivationsFixture.derivationConfigStrWithSqlExpr, DerivationsFixture.expDerivationConfigObjWithSqlExpr);
  }

  @Test
  public void testSimpleDerivationWithType() {
    testDerivation(DerivationsFixture.derivationConfigStrWithType, DerivationsFixture.expDerivationConfigObjWithDef);
  }

  @Test
  public void testDerivationWithMvelExpr() {
    testDerivation(DerivationsFixture.derivation2ConfigStr, DerivationsFixture.expDerivation2ConfigObj);
  }

  @Test
  public void testDerivationWithExtractor() {
    testDerivation(DerivationsFixture.derivation3ConfigStr, DerivationsFixture.expDerivation3ConfigObj);
  }

  @Test
  public void testDerivationWithSqlExpr() {
    testDerivation(DerivationsFixture.derivation4ConfigStr, DerivationsFixture.expDerivation4ConfigObj);
  }

  @Test
  public void testSequentialJoinConfig() {
    testDerivation(DerivationsFixture.sequentialJoin1ConfigStr, DerivationsFixture.expSequentialJoin1ConfigObj);
  }

  @Test(description = "test sequential join config where base feature has outputKey and transformation field")
  public void testSequentialJoinConfig2() {
    testDerivation(DerivationsFixture.sequentialJoin2ConfigStr, DerivationsFixture.expSequentialJoin2ConfigObj);
  }

  @Test(description = "test sequential join config with transformation class")
  public void testSequentialJoinWithTransformationClass() {
    testDerivation(
        DerivationsFixture.sequentialJoinWithTransformationClassConfigStr, DerivationsFixture.expSequentialJoinWithTransformationClassConfigObj);
  }

  @Test(description = "test sequential join config with both transformation and transformationClass", expectedExceptions = ConfigBuilderException.class)
  public void testSequentialJoinWithInvalidTransformation() {
    Config fullConfig = ConfigFactory.parseString(DerivationsFixture.sequentialJoinWithInvalidTransformationConfigStr);
    DerivationConfigBuilder.build("seq_join_feature", fullConfig);
  }

  private void testDerivation(String configStr, DerivationConfig expDerivationConfig) {
    Config fullConfig = ConfigFactory.parseString(configStr);
    String derivedFeatureName = fullConfig.root().keySet().iterator().next();

    DerivationConfig obsDerivationConfigObj = DerivationConfigBuilder.build(derivedFeatureName, fullConfig);

    assertEquals(obsDerivationConfigObj, expDerivationConfig);
  }
}
