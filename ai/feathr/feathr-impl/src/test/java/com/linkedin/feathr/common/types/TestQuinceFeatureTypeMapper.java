package com.linkedin.feathr.common.types;

import java.util.Collections;

import com.linkedin.feathr.common.tensor.PrimitiveDimensionType;
import com.linkedin.feathr.common.tensor.TensorCategory;
import com.linkedin.feathr.common.tensor.TensorType;
import com.linkedin.feathr.common.value.QuinceFeatureTypeMapper;
import org.scalatest.testng.TestNGSuite;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static java.util.Collections.*;
import static org.testng.Assert.*;


public class TestQuinceFeatureTypeMapper extends TestNGSuite {
  @DataProvider
  public Object[][] quinceTypeMapperCases() {
    return new Object[][] {
        {BooleanFeatureType.INSTANCE,
            new TensorType(PrimitiveType.BOOLEAN, Collections.emptyList())},
        {NumericFeatureType.INSTANCE,
            new TensorType(PrimitiveType.FLOAT, Collections.emptyList())},
        {CategoricalFeatureType.INSTANCE,
            new TensorType(PrimitiveType.FLOAT, singletonList(PrimitiveDimensionType.STRING))},
        {CategoricalSetFeatureType.INSTANCE,
            new TensorType(PrimitiveType.FLOAT, singletonList(PrimitiveDimensionType.STRING))},
        {TermVectorFeatureType.INSTANCE,
            new TensorType(PrimitiveType.FLOAT, singletonList(PrimitiveDimensionType.STRING))},
        {DenseVectorFeatureType.withUnknownSize(),
            new TensorType(TensorCategory.DENSE, PrimitiveType.FLOAT, singletonList(PrimitiveDimensionType.INT), null)},
        {DenseVectorFeatureType.withSize(100),
            new TensorType(TensorCategory.DENSE, PrimitiveType.FLOAT, singletonList(PrimitiveDimensionType.INT.withShape(100)), null)},
        {TensorFeatureType.withTensorType(new TensorType(PrimitiveType.FLOAT, singletonList(PrimitiveDimensionType.INT.withShape(100)))),
            new TensorType(PrimitiveType.FLOAT, singletonList(PrimitiveDimensionType.INT.withShape(100)))}
    };
  }

  @Test(dataProvider = "quinceTypeMapperCases")
  public void testQuinceTensorTypeMappings(FeatureType featureType, TensorType tensorType) {
    TensorType actual = QuinceFeatureTypeMapper.INSTANCE.fromFeatureType(featureType);
    assertEquals(actual, tensorType);
  }
}
