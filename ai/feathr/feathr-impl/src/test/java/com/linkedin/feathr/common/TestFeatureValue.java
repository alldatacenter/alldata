package com.linkedin.feathr.common;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.linkedin.feathr.common.tensor.*;
import com.linkedin.feathr.common.tensorbuilder.DenseTensorBuilder;
import com.linkedin.feathr.common.tensorbuilder.UniversalTensor;
import com.linkedin.feathr.common.types.PrimitiveType;
import com.linkedin.feathr.common.value.BooleanFeatureValue;
import com.linkedin.feathr.common.value.NumericFeatureValue;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.scalatest.testng.TestNGSuite;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static java.util.Collections.*;


public class TestFeatureValue extends TestNGSuite {

  private static final float DEFAULT_VALUE = FeatureValue.DEFAULT_VALUE;
  private static final String EMPTY_TERM = FeatureValue.EMPTY_TERM;

  @Test(description = "verifies equals() and hashCode()")
  public void testEqualsHashCode() {
    EqualsVerifier.forClass(FeatureValue.class)
        .usingGetClass()
        .withNonnullFields("_featureValueInternal")
        .withIgnoredFields("_featureValueInternal")
        .withPrefabValues(com.linkedin.feathr.common.value.FeatureValue.class, NumericFeatureValue.fromFloat(1.0f),
            BooleanFeatureValue.fromBoolean(true))
        .suppress(Warning.NONFINAL_FIELDS)
        .verify();
  }

  @DataProvider
  public Object[][] emptyAndNonEmptyFeatureValue() {
    return new Object[][] {
        // Non-empty FeatureValue
        {FeatureValue.createBoolean(true), false},
        {FeatureValue.createNumeric(1), false},
        {FeatureValue.createCategorical(""), false},
        {FeatureValue.createNumericCategoricalSet(ImmutableSet.of(1, 101)), false},
        {FeatureValue.createStringCategoricalSet(ImmutableSet.of("")), false},
        {FeatureValue.createStringTermVector(ImmutableMap.of("one", 1.0f)), false},
        {createTensorFeature(), false},
        // Empty FeatureValue
        {new FeatureValue(), true},
        {FeatureValue.createBoolean(false), true},
        {FeatureValue.createNumericCategoricalSet(ImmutableSet.of()), true},
        {FeatureValue.createStringCategoricalSet(ImmutableSet.of()), true},
        {FeatureValue.createStringCategoricalSet(ImmutableSet.of()), true},
        {FeatureValue.createStringTermVector(ImmutableMap.of()), true},
        {createEmptyTensorFeature(), true},
    };
  }
  @Test(description = "test isEmpty() API", dataProvider = "emptyAndNonEmptyFeatureValue")
  public void testIsEmpty(FeatureValue featureValue, boolean isEmpty) {
    Assert.assertEquals(featureValue.isEmpty(), isEmpty);
  }

  @DataProvider
  public Object[][] createNumericCases() {
    return new Object[][] {
        {123, 123.0f}, // int
        {27.6d, 27.6f}, // double
        {1.2f, 1.2f}, // float
        {BigDecimal.TEN, 10.0f}, // BigDecimal
    };
  }
  @Test(dataProvider = "createNumericCases")
  public void testCreateNumeric(Number input, Float outputValue) {
    FeatureValue feature = FeatureValue.createNumeric(input);
    Assert.assertEquals(feature.getAsTermVector(), ImmutableMap.of(EMPTY_TERM, outputValue));
    Assert.assertEquals(feature.getAsNumeric(), outputValue);
  }

  @DataProvider
  public Object[][] getAsNumericErrorCases() {
    return new Object[][] {
        {FeatureValue.createCategorical("foo")},
        {FeatureValue.createDenseVector(Arrays.asList(10, 20))},
        {FeatureValue.createStringCategoricalSet(Arrays.asList("foo", "bar"))},
        {FeatureValue.createNumericTermVector(ImmutableMap.of(100, 1.0f))}
    };
  }
  @Test(dataProvider = "getAsNumericErrorCases", expectedExceptions = RuntimeException.class)
  public void testGetAsNumericError(FeatureValue featureValue) {
    featureValue.getAsNumeric();
  }

  @DataProvider
  public Object[][] createCategoricalWithNumberCases() {
    return new Object[][] {
        {123.000000d, "123"}, // int
        {27.00000d, "27"}, // double
        {1.0f, "1"}, // float
        {BigDecimal.TEN, "10"}, // BigDecimal
    };
  }
  @Test(dataProvider = "createCategoricalWithNumberCases")
  public void testCreateCategoricalWithNumber(Number input, String outputTerm) {
    FeatureValue feature = FeatureValue.createCategorical(input);
    Assert.assertEquals(feature.getAsTermVector(), ImmutableMap.of(outputTerm, DEFAULT_VALUE));
    Assert.assertEquals(feature.getAsCategorical(), outputTerm);
  }

  @DataProvider
  public Object[][] createCategoricalWithStringCases() {
    return new Object[][] {
        {"some_term", "some_term"}, // regular string
        {new StringBuffer("some_buffer_term"), "some_buffer_term"} // string buffer
    };
  }
  @Test(dataProvider = "createCategoricalWithStringCases")
  public void testCreateCategoricalWithString(CharSequence term, String outputTerm) {
    FeatureValue feature = FeatureValue.createCategorical(term);
    Assert.assertEquals(feature.getAsTermVector(), ImmutableMap.of(outputTerm, DEFAULT_VALUE));
    Assert.assertEquals(feature.getAsCategorical(), outputTerm);
  }

  @Test
  public void testCreateCategoricalWithCharacter() {
    Character a = 'a';
    FeatureValue feature = FeatureValue.createCategorical(a);
    Assert.assertEquals(feature.getAsTermVector(), ImmutableMap.of(a.toString(), DEFAULT_VALUE));
    Assert.assertEquals(feature.getAsCategorical(), a.toString());
  }

  @DataProvider
  public Object[][] getAsCategoricalErrorCases() {
    return new Object[][] {
        {FeatureValue.createBoolean(false)},
        {FeatureValue.createNumeric(100f)},
        {FeatureValue.createDenseVector(Arrays.asList(10, 20))},
        {FeatureValue.createStringCategoricalSet(Arrays.asList("foo", "bar"))},
        {FeatureValue.createNumericTermVector(ImmutableMap.of(100, 3.0f))}
    };
  }
  @Test(dataProvider = "getAsCategoricalErrorCases", expectedExceptions = RuntimeException.class)
  public void testGetAsCategoricalError(FeatureValue featureValue) {
    featureValue.getAsCategorical();
  }

  @Test
  public void testCreateDenseVectorWithList() {
    List<Integer> vec = ImmutableList.of(1, 5, 7);
    FeatureValue feature = FeatureValue.createDenseVector(vec);
    Assert.assertEquals(feature.getAsTermVector(), ImmutableMap.of("0", 1.0f, "1", 5.0f, "2", 7.0f));
  }

  @Test
  public void testCreateDenseVectorWithArray() {
    Float[] vec = new Float[]{1.0f, 5.0f, 7.0f};
    FeatureValue feature = FeatureValue.createDenseVector(vec);
    Assert.assertEquals(feature.getAsTermVector(), ImmutableMap.of("0", 1.0f, "1", 5.0f, "2", 7.0f));
  }

  @Test
  public void testCreateStringTermVector() {
    Map<String, Integer> termVector = ImmutableMap.of("one", 1, "andAnotherOne", 5 );
    FeatureValue feature = FeatureValue.createStringTermVector(termVector);
    Assert.assertEquals(feature.getAsTermVector(), ImmutableMap.of("one", 1.0f, "andAnotherOne", 5.0f));
  }

  @Test
  public void testCreateNumericTermVector() {
    Map<Double, Float> termVector = ImmutableMap.of(1.0, 1.0f, 2.0, 5.0f );
    FeatureValue feature = FeatureValue.createNumericTermVector(termVector);
    Assert.assertEquals(feature.getAsTermVector(), ImmutableMap.of("1", 1.0f, "2", 5.0f));
  }

  @Test
  public void testCreateStringTermVectors() {
    List<Map<String, Integer>> termVectors = ImmutableList.of(
        ImmutableMap.of("one", 1, "andAnotherOne", 5 ),
        ImmutableMap.of("two", 4));

    FeatureValue feature = FeatureValue.createStringTermVector(termVectors);
    Assert.assertEquals(feature.getAsTermVector(), ImmutableMap.of("one", 1.0f, "andAnotherOne", 5.0f, "two", 4.0f));
  }

  @Test
  public void testCreateNumericTermVectors() {
    List<Map<Double, Integer>> termVectors = ImmutableList.of(
        ImmutableMap.of(1.0, 1, 3.0, 5 ),
        ImmutableMap.of(37.0, 4));

    FeatureValue feature = FeatureValue.createNumericTermVector(termVectors);
    Assert.assertEquals(feature.getAsTermVector(), ImmutableMap.of("1", 1.0f, "3", 5.0f, "37", 4.0f));
  }

  @Test
  public void testCreateNumericCategoricalSet() {
    Set<Integer> terms = ImmutableSet.of(1, 101);
    FeatureValue feature = FeatureValue.createNumericCategoricalSet(terms);
    Assert.assertEquals(feature.getAsTermVector(), ImmutableMap.of("1", DEFAULT_VALUE, "101", DEFAULT_VALUE));
  }

  @Test
  public void testCreateStringCategoricalSet() {
    Set<String> terms = ImmutableSet.of("one", "andAnotherOne");
    FeatureValue feature = FeatureValue.createStringCategoricalSet(terms);
    Assert.assertEquals(feature.getAsTermVector(), ImmutableMap.of("one", DEFAULT_VALUE, "andAnotherOne", DEFAULT_VALUE));
  }

  @Test
  public void testCreateCharacterCategoricalSet() {
    Set<Character> terms = ImmutableSet.of('a', 'b');
    FeatureValue feature = FeatureValue.createCharacterCategoricalSet(terms);
    Assert.assertEquals(feature.getAsTermVector(), ImmutableMap.of("a", DEFAULT_VALUE, "b", DEFAULT_VALUE));
  }

  @Test
  public void testCreateCategoricalSetWithDuplicateTerms() {
    List<String> terms = ImmutableList.of("one", "one");
    FeatureValue feature = FeatureValue.createStringCategoricalSet(terms);
    Assert.assertEquals(feature.getAsTermVector(), ImmutableMap.of("one", DEFAULT_VALUE));
  }

  @Test(expectedExceptions = RuntimeException.class)
  public void testCreateNumericTermVectorWithDuplicateTerm() {
    Map<Double, Float> termVector = ImmutableMap.of(2.00000d, 1.0f, 2.0, 5.0f );
    FeatureValue.createNumericTermVector(termVector);
  }

  @Test(expectedExceptions = RuntimeException.class)
  public void testCreateNumericTermVectorWithDuplicateTerms() {
    List<Map<Double, Integer>> termVectors = ImmutableList.of(
        ImmutableMap.of(1.0, 1),
        ImmutableMap.of(1.0, 4));

    FeatureValue.createNumericTermVector(termVectors);
  }

  @Test(expectedExceptions = RuntimeException.class)
  public void testCreateStringTermVectorWithDuplicateTerms() {
    List<Map<String, Integer>> termVectors = ImmutableList.of(
        ImmutableMap.of("one", 1),
        ImmutableMap.of("one", 4));

    FeatureValue.createStringTermVector(termVectors);
  }

  @Test(expectedExceptions = RuntimeException.class)
  public void testCreateCategoricalWithBadNumber() {
    FeatureValue.createCategorical(1.400);
  }

  @Test(description = "Tests creation of FeatureValue with a boolean, and verifies that getAsBoolean returns the correct result")
  public void testCreateAsBoolean() {
    Assert.assertTrue(FeatureValue.createBoolean(true).getAsBoolean());
    Assert.assertFalse(FeatureValue.createBoolean(false).getAsBoolean());
  }

  @DataProvider
  public Object[][] getAsBooleanErrorCases() {
    return new Object[][] {
        {FeatureValue.createNumeric(100f)},
        {FeatureValue.createCategorical("foo")},
        {FeatureValue.createDenseVector(Arrays.asList(10, 20))},
        {FeatureValue.createStringCategoricalSet(Arrays.asList("foo", "bar"))},
        {FeatureValue.createNumericTermVector(ImmutableMap.of(100, 3.0f))}
    };
  }
  @Test(description = "Tests that method invocation to get null tensor as a term-vector returns empty NTV")
  public void testGetNullTensorAsTermVector() {
    FeatureValue featureValue = new FeatureValue(new GenericTypedTensor(null, TensorTypes.parseTensorType("TENSOR<SPARSE>:FLOAT")));
    Assert.assertEquals(featureValue.getAsTermVector(), Collections.emptyMap());
    Assert.assertThrows(NullPointerException.class, featureValue::getAsBoolean);
  }

  @Test(description = "Tests that method invocation to get value as a tensor returns 1-d dense float tensor when the "
      + "requested feature had type DENSE_VECTOR")
  public void testGetAsTensorWithDenseVector() {
    List<Integer> vec = ImmutableList.of(8, 5, 3);
    FeatureValue featureValue = FeatureValue.createDenseVector(vec);

    TensorData expected = new DenseTensorBuilder(new Representable[]{PrimitiveType.INT, PrimitiveType.FLOAT}, new long[] {-1})
        .build(new float[]{8.0f, 5.0f, 3.0f});
    Assert.assertTrue(Equal.INSTANCE.apply(expected, featureValue.getAsTypedTensor().getData()));
    Assert.assertTrue(Equal.INSTANCE.apply(expected, featureValue.getAsTensorData()));
  }

  @Test(description = "ensures the legacy hasTensors method returns false, except when FeatureValue was constructed with"
      + " a FeatureTensor specifically")
  public void testLegacyHasTensorMethodReturnsFalseWhenNotConstructedWithFeatureValue() {
    List<Integer> vec = ImmutableList.of(8, 5, 3);
    FeatureValue featureValue1 = FeatureValue.createDenseVector(vec);

    TensorData tensorData = new UniversalTensor(new int[]{}, new long[]{}, new float[]{4.2f},
        new Representable[]{Primitive.FLOAT});
    TensorType tensorType = new TensorType(new PrimitiveType(Primitive.FLOAT), Collections.emptyList(),
        Collections.emptyList());
    TypedTensor typedTensor = new GenericTypedTensor(tensorData, tensorType);
    FeatureValue featureValue2 = new FeatureValue(typedTensor);
  }

  @DataProvider
  public Object[][] createScalarTensorCases() {
    return new Object[][] {
        {PrimitiveType.LONG, 1L},
        {PrimitiveType.BOOLEAN, true},
        {PrimitiveType.DOUBLE, 1d},
        {PrimitiveType.FLOAT, 1f},
        {PrimitiveType.INT, 1},
        {PrimitiveType.LONG, 1L},
        {PrimitiveType.STRING, "1"},
    };
  }
  @Test(description = "creates a FeatureValue with Scalar tensor.", dataProvider = "createScalarTensorCases")
  public void testCreateTensorFeatureValueWithScalarType(PrimitiveType primitiveType, Object valueObject) {
    TensorType tensorType = new TensorType(primitiveType, emptyList());
    TensorData tensorData = Tensors.asScalarTensor(tensorType, valueObject);
    TypedTensor typedTensor = new GenericTypedTensor(tensorData, tensorType);
    FeatureValue featureValue = FeatureValue.createTensor(valueObject, tensorType);
    Assert.assertTrue(new Equal().apply(featureValue.getAsTypedTensor().getData(), typedTensor.getData()));
  }

  @Test(description = "1-d dense tensor with float array")
  public void testCreateDenseTensorFeatureValueWithTypeWithArray() {
    float[] vec = new float[]{4.2f, 3.1f};

    TensorType tensorType =
        new TensorType(TensorCategory.DENSE, PrimitiveType.FLOAT, singletonList(PrimitiveDimensionType.INT), null);
    TensorData tensorData = Tensors.asDenseTensor(tensorType, vec);
    TypedTensor expectedTypedTensor = new GenericTypedTensor(tensorData, tensorType);

    FeatureValue featureValue = FeatureValue.createTensor(vec, tensorType);
    Assert.assertTrue(new Equal().apply(featureValue.getAsTypedTensor().getData(), expectedTypedTensor.getData()));
  }

  @Test(description = "1-d dense tensor with float list")
  public void testCreateDenseTensorFeatureValueWithTypeWithList() {
    List<Float> vec = Arrays.asList(1f, 2f);

    TensorType tensorType =
        new TensorType(TensorCategory.DENSE, PrimitiveType.FLOAT, singletonList(PrimitiveDimensionType.INT), null);
    TensorData tensorData = Tensors.asDenseTensor(tensorType, vec);
    TypedTensor expectedTypedTensor = new GenericTypedTensor(tensorData, tensorType);

    FeatureValue featureValue = FeatureValue.createTensor(vec, tensorType);
    Assert.assertTrue(new Equal().apply(featureValue.getAsTypedTensor().getData(), expectedTypedTensor.getData()));
  }

  @Test(description = "1-d sparse tensor with list of map")
  public void testCreateDenseTensorFeatureValueWithTypeWithListOfMap() {
    Map<Long, Long> firstMap = new HashMap<>();
    firstMap.put(123L, 1L);
    Map<Long, Long> secondMap = new HashMap<>();
    secondMap.put(234L, 2L);
    Map<Long, Long> combinedMap = new HashMap<>();
    combinedMap.putAll(firstMap);
    combinedMap.putAll(secondMap);
    List<Map<?, ?>> vec = Arrays.asList(firstMap, secondMap);
    TensorType tensorType =
        new TensorType(TensorCategory.SPARSE, PrimitiveType.LONG, singletonList(PrimitiveDimensionType.LONG), null);
    TensorData tensorData = Tensors.asSparseTensor(tensorType, combinedMap);
    TypedTensor expectedTypedTensor = new GenericTypedTensor(tensorData, tensorType);

    FeatureValue featureValue = FeatureValue.createTensor(vec, tensorType);
    Assert.assertTrue(new Equal().apply(featureValue.getAsTypedTensor().getData(), expectedTypedTensor.getData()));
  }

  @Test(description = "1-d sparse tensor")
  public void testCreateSparseTensorFeatureValueWithType() {
    Map<String, Integer> input = new HashMap<>();
    input.put("1", 1);

    TensorType tensorType =
        new TensorType(TensorCategory.DENSE, PrimitiveType.FLOAT, singletonList(PrimitiveDimensionType.INT), null);
    TensorData tensorData = Tensors.asSparseTensor(tensorType, input);
    TypedTensor expectedTypedTensor = new GenericTypedTensor(tensorData, tensorType);

    FeatureValue featureValue = FeatureValue.createTensor(input, tensorType);
    Assert.assertTrue(new Equal().apply(featureValue.getAsTypedTensor().getData(), expectedTypedTensor.getData()));
  }

  @Test(description = "creating 2-d tensor should throw exception.", expectedExceptions = RuntimeException.class,
      expectedExceptionsMessageRegExp = "Only creating of 0-d and 1-d tensor is supported .*")
  public void testCreateTensorFeatureValueWith2dDimension() {
    List<DimensionType> primitiveDimensionTypes =
        Arrays.asList(PrimitiveDimensionType.STRING, PrimitiveDimensionType.STRING);
    TensorType tensorType = new TensorType(TensorCategory.DENSE, PrimitiveType.FLOAT, primitiveDimensionTypes, null);

    FeatureValue.createTensor(Collections.emptyMap(), tensorType);
  }

  private static FeatureValue createTensorFeature() {
    TensorData tensorData = new UniversalTensor(new int[]{}, new long[]{}, new float[]{1.0f},
        new Representable[]{Primitive.FLOAT});
    TensorType type = new TensorType(new PrimitiveType(Primitive.FLOAT), Collections.emptyList());
    TypedTensor typedTensor = new GenericTypedTensor(tensorData, type);

    return new FeatureValue(typedTensor);
  }

  private static FeatureValue createEmptyTensorFeature() {
    TensorData tensorData = new UniversalTensor(new int[]{}, new long[]{}, new float[]{},
        new Representable[]{Primitive.FLOAT});
    TensorType type = new TensorType(new PrimitiveType(Primitive.FLOAT), Collections.emptyList());
    TypedTensor typedTensor = new GenericTypedTensor(tensorData, type);

    return new FeatureValue(typedTensor);
  }
}

