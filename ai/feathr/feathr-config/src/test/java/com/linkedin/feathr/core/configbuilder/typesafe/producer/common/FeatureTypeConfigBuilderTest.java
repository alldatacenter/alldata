package com.linkedin.feathr.core.configbuilder.typesafe.producer.common;

import com.linkedin.feathr.core.config.producer.common.FeatureTypeConfig;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.testng.annotations.Test;

import static com.linkedin.feathr.core.configbuilder.typesafe.producer.common.FeatureTypeFixture.*;
import static org.testng.Assert.*;


/**
 * Tests for {@link FeatureTypeConfigBuilder}
 */
public class FeatureTypeConfigBuilderTest {

  @Test
  public void testOnlyType() {
    testFeatureTypeConfig(simpleTypeConfigStr, expSimpleTypeConfigObj);
  }

  @Test
  public void testTypeWithDocumentation() {
    testFeatureTypeConfig(simpleTypeWithDocConfigStr, expSimpleTypeWithDocConfigObj);
  }

  @Test
  public void testTensorTypeWithUnknownShape() {
    testFeatureTypeConfig(tensorTypeWithUnknownShapeConfigStr, expTensorTypeWithUnknownShapeConfig);
  }

  @Test
  public void test0DSparseTensorType() {
    testFeatureTypeConfig(zeroDimSparseTensorConfigStr, expZeroDimSparseTensorConfig);
  }

  @Test(expectedExceptions = RuntimeException.class)
  public void testInvalidType() {
    createFeatureTypeConfig(invalidTypeConfigStr);
  }

  @Test(expectedExceptions = RuntimeException.class)
  public void testInvalidTensorCategory() {
    createFeatureTypeConfig(invalidTensorTypeConfigStr);
  }

  @Test(expectedExceptions = RuntimeException.class)
  public void testMissingType() {
    createFeatureTypeConfig(missingTypeConfigStr);
  }

  @Test(expectedExceptions = RuntimeException.class)
  public void testMissingValType() {
    createFeatureTypeConfig(missingValType);
  }

  @Test(expectedExceptions = RuntimeException.class)
  public void testTensorTypeSizeMismatchException() {
    createFeatureTypeConfig(shapeAndDimSizeMismatchTypeConfigStr);
  }

  @Test(expectedExceptions = RuntimeException.class)
  public void tesNonIntShapeValType() {
    createFeatureTypeConfig(nonIntShapeConfigStr);
  }


  private FeatureTypeConfig createFeatureTypeConfig(String configStr) {
    Config fullConfig = ConfigFactory.parseString(configStr);
    return FeatureTypeConfigBuilder.build(fullConfig);
  }

  private void testFeatureTypeConfig(String configStr, FeatureTypeConfig expFeatureTypeConfig) {
    FeatureTypeConfig featureTypeConfig = createFeatureTypeConfig(configStr);
    assertEquals(featureTypeConfig, expFeatureTypeConfig);
  }
}
