package com.linkedin.feathr.common;

import com.linkedin.feathr.common.tensor.TensorType;
import org.scalatest.testng.TestNGSuite;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * Unit tests for {@link FeatureTypeConfig}
 *
 */
public class FeatureTypeConfigTest extends TestNGSuite {

  @DataProvider
  public Object[][] FeatureTypeConstructorWithAutoTZTestCases() {
    return new Object[][]{
        { FeatureTypes.BOOLEAN, AutoTensorizableTypes.SCALAR_BOOLEAN_TENSOR_TYPE },
        { FeatureTypes.NUMERIC, AutoTensorizableTypes.SCALAR_FLOAT_TENSOR_TYPE },
        { FeatureTypes.CATEGORICAL, AutoTensorizableTypes.TERM_VECTOR_TENSOR_TYPE},
        { FeatureTypes.CATEGORICAL_SET, AutoTensorizableTypes.TERM_VECTOR_TENSOR_TYPE },
        { FeatureTypes.TERM_VECTOR, AutoTensorizableTypes.TERM_VECTOR_TENSOR_TYPE },
        { FeatureTypes.DENSE_VECTOR, AutoTensorizableTypes.DENSE_1D_FLOAT_TENSOR_TYPE },
        { FeatureTypes.UNSPECIFIED, null },
        { FeatureTypes.TENSOR, null }
    };
  }
  @Test(dataProvider = "FeatureTypeConstructorWithAutoTZTestCases",
      description = "verifies that the proper auto-TZ TensorType is created for the legacy FeatureTypes constructor")
  public void testFeatureTypeConstructorWithAutoTZ(FeatureTypes featureType, TensorType expectedTensorType) {
    FeatureTypeConfig featureTypeConfig = new FeatureTypeConfig(featureType);
    Assert.assertEquals(featureType, featureTypeConfig.getFeatureType());
    Assert.assertEquals(expectedTensorType, featureTypeConfig.getTensorType());
  }

  @Test(description = "verifies that the proper auto-TZ TensorType is created for the legacy FeatureTypes constructor")
  public void testTensorTypeConstructor() {
    FeatureTypeConfig featureTypeConfig = new FeatureTypeConfig(TensorType.EMPTY);
    Assert.assertEquals(featureTypeConfig.getFeatureType(), FeatureTypes.TENSOR);
  }

  @Test(description = "verifies the null check in the new FeatureTypeConfig(TensorType) constructor", expectedExceptions = NullPointerException.class)
  public void testNullCheckingTensorTypeConstructor() {
    TensorType nullType = null;
    new FeatureTypeConfig(nullType);
  }

  @Test(description = "verifies the empty/default constructor")
  public void testEmptyConstructor() {
    FeatureTypeConfig featureTypeConfig = new FeatureTypeConfig();
    Assert.assertEquals(featureTypeConfig.getFeatureType(), FeatureTypes.UNSPECIFIED);
    Assert.assertEquals(featureTypeConfig.getTensorType(), null);
  }
}