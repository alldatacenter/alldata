package com.linkedin.feathr.common;

import com.linkedin.feathr.common.tensor.PrimitiveDimensionType;
import com.linkedin.feathr.common.tensor.TensorCategory;
import com.linkedin.feathr.common.tensor.TensorType;
import com.linkedin.feathr.common.types.PrimitiveType;
import java.util.Optional;

import org.scalatest.testng.TestNGSuite;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static java.util.Collections.*;
import static org.testng.Assert.*;


public class AutoTensorizableTypesTest extends TestNGSuite {
  private static final TensorType NTV_EQUIVALENT_TENSOR_TYPE = new TensorType(PrimitiveType.FLOAT, singletonList(
      PrimitiveDimensionType.STRING));

  @DataProvider
  public Object[][] autoTensorizableTypesDataProvider() {
    return new Object[][]{
        { FeatureTypes.BOOLEAN, new TensorType(TensorCategory.DENSE, PrimitiveType.BOOLEAN, emptyList()) },
        { FeatureTypes.NUMERIC, new TensorType(TensorCategory.DENSE, PrimitiveType.FLOAT, emptyList()) },
        { FeatureTypes.CATEGORICAL, NTV_EQUIVALENT_TENSOR_TYPE },
        { FeatureTypes.CATEGORICAL_SET, NTV_EQUIVALENT_TENSOR_TYPE },
        { FeatureTypes.TERM_VECTOR, NTV_EQUIVALENT_TENSOR_TYPE },
        { FeatureTypes.DENSE_VECTOR, new TensorType(TensorCategory.DENSE, PrimitiveType.FLOAT, singletonList(PrimitiveDimensionType.INT)) },
        { FeatureTypes.TENSOR, null }
    };
  }

  @Test(dataProvider = "autoTensorizableTypesDataProvider")
  public void test(FeatureTypes featureType, TensorType tensorType) {
    assertEquals(AutoTensorizableTypes.getDefaultTensorType(featureType), Optional.ofNullable(tensorType));
  }
}