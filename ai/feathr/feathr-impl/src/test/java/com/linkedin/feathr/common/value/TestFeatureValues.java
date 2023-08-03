package com.linkedin.feathr.common.value;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.Floats;
import com.linkedin.feathr.common.*;
import com.linkedin.feathr.common.tensor.*;
import com.linkedin.feathr.common.tensor.scalar.ScalarBooleanTensor;
import com.linkedin.feathr.common.tensor.scalar.ScalarFloatTensor;
import com.linkedin.feathr.common.tensorbuilder.DenseTensorBuilder;
import com.linkedin.feathr.common.tensorbuilder.UniversalTensorBuilder;
import com.linkedin.feathr.common.types.*;

import java.util.Arrays;
import java.util.Map;

import org.scalatest.testng.TestNGSuite;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static java.util.Collections.*;
import static org.testng.Assert.*;


public class TestFeatureValues extends TestNGSuite {
  private static final Representable[] TENSOR_STRING_FLOAT_TYPE = new Representable[]{Primitive.STRING, Primitive.FLOAT};
  private static final Representable[] TENSOR_INT_FLOAT_TYPE = new Representable[]{Primitive.INT, Primitive.FLOAT};

  @DataProvider
  public Object[][] tensorRepresentation() {
    return new Object[][] {
        { FeatureValues.numeric(-1.0f),
            new ScalarFloatTensor(-1.0f) },

        { FeatureValues.numeric(0.0f),
            new ScalarFloatTensor(0.0f) },

        { FeatureValues.bool(true),
            new ScalarBooleanTensor(true) },

        { FeatureValues.bool(false),
            new ScalarBooleanTensor(false) },

        { FeatureValues.categorical("linkedin"),
            new UniversalTensorBuilder(TENSOR_STRING_FLOAT_TYPE).start()
                .setString(0, "linkedin").setFloat(1, 1f).append().build() },

        { FeatureValues.categoricalSet("alpha", "beta", "gamma"),
            new LOLTensorData(TENSOR_STRING_FLOAT_TYPE,
                singletonList(Arrays.asList("alpha", "beta", "gamma")),Arrays.asList(1f, 1f, 1f)) },

        { FeatureValues.emptyCategoricalSet(),
            new UniversalTensorBuilder(TENSOR_STRING_FLOAT_TYPE).build() },

        { FeatureValues.denseVectorDynamicSize(1.1f, 1.2f, 1.3f, 1.4f),
            new DenseTensorBuilder(TENSOR_INT_FLOAT_TYPE, new long[]{-1}).build(new float[]{1.1f, 1.2f, 1.3f, 1.4f}) },

        { FeatureValues.denseVectorDynamicSize(),
            new DenseTensorBuilder(TENSOR_INT_FLOAT_TYPE, new long[]{-1}).build(new float[0]) },

        { FeatureValues.emptyTermVector(),
            new UniversalTensorBuilder(TENSOR_STRING_FLOAT_TYPE).build() },

        { FeatureValues.termVector("alpha", 0.5f),
            new UniversalTensorBuilder(TENSOR_STRING_FLOAT_TYPE).start()
                .setString(0, "alpha").setFloat(1, 0.5f).append().build() },

        { FeatureValues.termVector("alpha", 0.1f, "beta", 0.2f, "gamma", 0.3f),
            new LOLTensorData(TENSOR_STRING_FLOAT_TYPE,
                singletonList(Arrays.asList("alpha", "beta", "gamma")), Arrays.asList(.1f, .2f, .3f)) },
    };
  }

  @Test(dataProvider = "tensorRepresentation")
  public void testTensorRepresentation(FeatureValue fv, TensorData expectedTensor) {
    TensorData actualTensor = QuinceFeatureFormatMapper.INSTANCE.fromFeatureValue(fv);
    assertTrue(Equal.INSTANCE.apply(actualTensor, expectedTensor));
  }

  @Test(description = "Test singleton tensor representation for empty FeatureValue")
  public void testSingletonTensorRepresentation() {
    com.linkedin.feathr.common.FeatureValue legacyFeatureValue1 = new com.linkedin.feathr.common.FeatureValue();
    com.linkedin.feathr.common.FeatureValue legacyFeatureValue2 = new com.linkedin.feathr.common.FeatureValue();
    TensorData tensorData1 = legacyFeatureValue1.getAsTensorData();
    TensorData tensorData2 = legacyFeatureValue2.getAsTensorData();
    // the returned TensorData are the same
    assertTrue(tensorData1 == tensorData2);
  }

  @Test
  public void testExtractingRawValues() {
    {
      BooleanFeatureValue fv = BooleanFeatureValue.fromBoolean(false);
      assertFalse(fv.getBooleanValue());
      assertEquals(fv.getFeatureType(), BooleanFeatureType.INSTANCE);
    }
    {
      NumericFeatureValue fv = NumericFeatureValue.fromFloat(1.23f);
      assertEquals(fv.getFloatValue(), 1.23f);
      assertEquals(fv.getFeatureType(), NumericFeatureType.INSTANCE);
    }
    {
      CategoricalFeatureValue fv = CategoricalFeatureValue.fromString("foo");
      assertEquals(fv.getStringValue(), "foo");
      assertEquals(fv.getFeatureType(), CategoricalFeatureType.INSTANCE);
    }
    {
      CategoricalSetFeatureValue fv = CategoricalSetFeatureValue.fromStringSet(ImmutableSet.of("xyz", "abc"));
      assertEquals(fv.getStringSet(), ImmutableSet.of("abc", "xyz"));
      assertEquals(fv.getFeatureType(), CategoricalSetFeatureType.INSTANCE);
    }
    {
      TermVectorFeatureValue fv = TermVectorFeatureValue.fromMap(ImmutableMap.of("linkedin", 0.1f, "microsoft", 0.2f));
      assertEquals(fv.getTermVector(), ImmutableMap.of("microsoft", 0.2f, "linkedin", 0.1f));
      assertEquals(fv.getFeatureType(), TermVectorFeatureType.INSTANCE);
    }
    {
      TermVectorFeatureValue fv = TermVectorFeatureValue.fromMap(emptyMap());
      assertEquals(fv.getTermVector(), emptyMap());
      assertEquals(fv.getFeatureType(), TermVectorFeatureType.INSTANCE);
    }
    {
      DenseVectorFeatureValue fv = DenseVectorFeatureValue.fromFloatArray(new float[]{-0.1f, 0.2f, -0.3f});
      assertEquals(Floats.asList(fv.getFloatArray()), ImmutableList.of(-0.1f, 0.2f, -0.3f));
      assertEquals(fv.getFeatureType(), DenseVectorFeatureType.withUnknownSize());
    }
    {
      TensorType tensorType = new TensorType(PrimitiveType.DOUBLE,
          ImmutableList.of(PrimitiveDimensionType.STRING, PrimitiveDimensionType.INT));
      TensorData tensorData = new LOLTensorData(tensorType.getColumnTypes(), ImmutableList.of(
          ImmutableList.of("alpha", "alpha", "beta", "beta"), ImmutableList.of(1, 10, 1, 20)),
          ImmutableList.of(0.5, 10.5, 20.5, 30.5));
      TensorFeatureValue fv = TensorFeatureValue.fromTensorData(TensorFeatureType.withTensorType(tensorType), tensorData);
      assertTrue(Equal.INSTANCE.apply(fv.getAsTensor(), TensorUtils.convertToLOLTensor(tensorData))); // hack to clone the expected value
      assertEquals(fv.getFeatureType(), TensorFeatureType.withTensorType(tensorType));
    }
  }

  @DataProvider
  public Object[][] equalFVs() {
    return new Object[][] {
        { FeatureValues.numeric(5.0f),
            NumericFeatureValue.fromFloat(5.0f) },

        { FeatureValues.numeric(5.0f),
            FeatureValues.numeric(new ScalarFloatTensor(5.0f)) },

        { FeatureValues.numeric(5.0f),
            FeatureValues.numeric(new LOLTensorData(new Representable[]{Primitive.FLOAT}, emptyList(), singletonList(5.0f))) },

        { FeatureValues.bool(true),
            FeatureValues.bool(new ScalarBooleanTensor(true)) },

        { FeatureValues.categorical("bar"),
            FeatureValues.categorical(new LOLTensorData(TENSOR_STRING_FLOAT_TYPE,
                singletonList(singletonList("bar")), singletonList(1.0f))) },

        { FeatureValues.categoricalSet("foo", "bar", "baz", "quux"),
            FeatureValues.categoricalSet(new LOLTensorData(TENSOR_STRING_FLOAT_TYPE,
                singletonList(Arrays.asList("foo", "bar", "baz", "quux")), Arrays.asList(1.0f, 1.0f, 1.0f, 1.0f))) },

        { FeatureValues.emptyTermVector(),
            FeatureValues.termVector(new UniversalTensorBuilder(TENSOR_STRING_FLOAT_TYPE).build()) }
    };
  }

  @DataProvider
  public Object[][] nonEqualFVs() {
    return new Object[][]{
        { FeatureValues.numeric(5.0f),
            FeatureValues.numeric(10.0f) },

        { FeatureValues.numeric(5.0f),
            FeatureValues.categorical("FOO") },

        { FeatureValues.bool(true),
            FeatureValues.bool(false) },
    };
  }

  @DataProvider
  public Object[][] nonEqualFVsThatWouldHaveBeenEqualInOldAPI() {
    return new Object[][]{
        { FeatureValues.numeric(10.0f),
            FeatureValues.termVector("", 10.f) },

        { FeatureValues.numeric(1.0f),
            FeatureValues.bool(true) },

        { FeatureValues.bool(false),
            FeatureValues.emptyTermVector() },

        { FeatureValues.bool(true),
            FeatureValues.termVector("", 1.0f) },

        { FeatureValues.categorical("foo"),
            FeatureValues.termVector("foo", 1.0f) },

        { FeatureValues.denseVectorDynamicSize(0.5f, 0.1f, 1.0f),
            FeatureValues.termVector("0", 0.5f, "1", 0.1f, "2", 1.0f) }
    };
  }

  @Test(dataProvider = "equalFVs")
  public void testEquality(FeatureValue fv1, FeatureValue fv2) {
    assertEquals(fv1, fv2);
  }

  @Test(dataProvider = "equalFVs")
  public void testHaveEqualTensorRepresentations(FeatureValue fv1, FeatureValue fv2) {
    TensorData tensor1 = QuinceFeatureFormatMapper.INSTANCE.fromFeatureValue(fv1);
    TensorData tensor2 = QuinceFeatureFormatMapper.INSTANCE.fromFeatureValue(fv2);
    assertTrue(Equal.INSTANCE.apply(tensor1, tensor2));
  }

  @Test(dataProvider = "nonEqualFVs")
  public void testNonEquality(FeatureValue fv1, FeatureValue fv2) {
    assertNotEquals(fv1, fv2);
    // non-equal feature value doesn't require that their tensors be non-equal, so we won't check tensor non-equality
  }

  @Test(dataProvider = "nonEqualFVsThatWouldHaveBeenEqualInOldAPI")
  public void testNonEqualityForCasesThatWouldHaveBeenEqualInOldAPI(FeatureValue fv1, FeatureValue fv2) {
    assertNotEquals(fv1, fv2);
    // non-equal feature value doesn't require that their tensors be non-equal, so we won't check tensor non-equality
  }

  private void assertEqualsConditionally(Object o1, Object o2, boolean expectEquality) {
    if (expectEquality) {
      assertEquals(o1, o2);
    } else {
      assertNotEquals(o1, o2);
    }
  }

  @Test(expectedExceptions = {RuntimeException.class})
  public void testIllegalConstruction() {
    FeatureValues.denseVector(new LOLTensorData(TENSOR_STRING_FLOAT_TYPE,
        singletonList(Arrays.asList("alpha", "beta", "gamma")), Arrays.asList(.1f, .2f, .3f)));
  }
}