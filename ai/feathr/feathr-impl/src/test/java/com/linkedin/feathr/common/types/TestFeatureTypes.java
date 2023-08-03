package com.linkedin.feathr.common.types;

import java.util.Collections;

import com.linkedin.feathr.common.tensor.TensorType;
import org.scalatest.testng.TestNGSuite;
import org.testng.annotations.Test;

import static org.testng.Assert.*;


public class TestFeatureTypes extends TestNGSuite {
  @Test
  public void checkBasicTypes() {
    assertEquals(BooleanFeatureType.INSTANCE.getBasicType(), FeatureType.BasicType.BOOLEAN);
    assertEquals(NumericFeatureType.INSTANCE.getBasicType(), FeatureType.BasicType.NUMERIC);
    assertEquals(CategoricalFeatureType.INSTANCE.getBasicType(), FeatureType.BasicType.CATEGORICAL);
    assertEquals(CategoricalSetFeatureType.INSTANCE.getBasicType(), FeatureType.BasicType.CATEGORICAL_SET);
    assertEquals(TermVectorFeatureType.INSTANCE.getBasicType(), FeatureType.BasicType.TERM_VECTOR);
    assertEquals(DenseVectorFeatureType.withUnknownSize().getBasicType(), FeatureType.BasicType.DENSE_VECTOR);
    assertEquals(TensorFeatureType.withTensorType(new TensorType(PrimitiveType.FLOAT, Collections.emptyList())).getBasicType(),
        FeatureType.BasicType.TENSOR);
  }

  @Test
  public void testDenseVectorType() {
    DenseVectorFeatureType denseVectorFeatureType = DenseVectorFeatureType.withSize(10);
    assertEquals(denseVectorFeatureType.getSize(), 10);
    assertEquals(DenseVectorFeatureType.withUnknownSize().getSize(), DenseVectorFeatureType.UNKNOWN_SIZE);
  }

  @Test
  public void testTensorFeatureType() {
    TensorType tensorType = new TensorType(PrimitiveType.FLOAT, Collections.emptyList());
    TensorFeatureType tensorFeatureType = TensorFeatureType.withTensorType(tensorType);
    assertEquals(tensorFeatureType.getTensorType(), tensorType);
  }
}
