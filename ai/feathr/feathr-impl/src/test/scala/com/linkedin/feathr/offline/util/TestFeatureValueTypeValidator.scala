package com.linkedin.feathr.offline.util

import java.util
import java.util.Collections
import com.google.common.collect.{ImmutableList, Lists, Sets}
import com.linkedin.feathr.common.exception.FeathrException
import com.linkedin.feathr.common.tensor.{LOLTensorData, PrimitiveDimensionType, TensorCategory, TensorType, Tensors}
import com.linkedin.feathr.common.types.PrimitiveType
import com.linkedin.feathr.common.{FeatureTypeConfig, FeatureTypes, FeatureValue, GenericTypedTensor}
import com.linkedin.feathr.offline.TestFeathr
import org.testng.annotations.{DataProvider, Test}

class TestFeatureValueTypeValidator extends TestFeathr {

  val termVectorData = new util.HashMap[Double, Double]
  termVectorData.put(1, 1)
  termVectorData.put(2, 2)
  termVectorData.put(3, 3)

  val tensorType1 = new TensorType(PrimitiveType.INT, ImmutableList.of(PrimitiveDimensionType.STRING))
  val tensorType2 = new TensorType(PrimitiveType.STRING, ImmutableList.of(PrimitiveDimensionType.STRING))

  @DataProvider(name = "passTestCases")
  def passTestCases() : Array[Array[Any]] = {
    Array(
      Array[Any](true, FeatureTypes.BOOLEAN, FeatureTypes.BOOLEAN, null),
      Array[Any](100, FeatureTypes.NUMERIC, FeatureTypes.NUMERIC, null),
      Array[Any]("1", FeatureTypes.CATEGORICAL, FeatureTypes.CATEGORICAL, null),
      Array[Any](Lists.newArrayList(1, 2, 3), FeatureTypes.DENSE_VECTOR, FeatureTypes.DENSE_VECTOR, null),
      Array[Any](Sets.newHashSet(1, 2, 3), FeatureTypes.CATEGORICAL_SET, FeatureTypes.CATEGORICAL_SET, null),
      Array[Any](termVectorData, FeatureTypes.TERM_VECTOR, FeatureTypes.TERM_VECTOR, null),
      Array[Any](new GenericTypedTensor(
        new LOLTensorData(tensorType1.getColumnTypes, ImmutableList.of(ImmutableList.of("1", "2")), ImmutableList.of(1, 2)), tensorType1),
        FeatureTypes.TENSOR, FeatureTypes.TENSOR, tensorType1),
      Array[Any](true, FeatureTypes.BOOLEAN, FeatureTypes.UNSPECIFIED, null)
    )
  }

  @Test(dataProvider = "passTestCases")
  def validatePass(value : Any, valueFeatureType : Any, configFeatureTypes : Any, configTensorType : Any): Unit = {
    val featureType = valueFeatureType.asInstanceOf[FeatureTypes]
    val featureValue = if (featureType == FeatureTypes.TENSOR) {
      new FeatureValue(value.asInstanceOf[GenericTypedTensor])
    } else {
      new FeatureValue(value, valueFeatureType.asInstanceOf[FeatureTypes]);
    }
    val featureTypeConfig = new FeatureTypeConfig(configFeatureTypes.asInstanceOf[FeatureTypes], configTensorType.asInstanceOf[TensorType], null)
    FeatureValueTypeValidator.validate("", featureValue, featureTypeConfig)
  }

  @DataProvider(name = "failTestCases")
  def failTestCases() : Array[Array[Any]] = {
    Array(
      Array[Any](true, FeatureTypes.BOOLEAN, FeatureTypes.NUMERIC, null),
      Array[Any](100, FeatureTypes.NUMERIC, FeatureTypes.BOOLEAN, null),
      Array[Any]("1", FeatureTypes.CATEGORICAL, FeatureTypes.BOOLEAN, null),
      Array[Any](Lists.newArrayList(1, 2, 3), FeatureTypes.DENSE_VECTOR, FeatureTypes.BOOLEAN, null),
      Array[Any](Sets.newHashSet(1, 2, 3), FeatureTypes.CATEGORICAL_SET, FeatureTypes.BOOLEAN, null),
      Array[Any](termVectorData, FeatureTypes.TERM_VECTOR, FeatureTypes.BOOLEAN, null),
      Array[Any](new GenericTypedTensor(
        new LOLTensorData(tensorType1.getColumnTypes, ImmutableList.of(ImmutableList.of("1", "2")), ImmutableList.of(1, 2)), tensorType1),
        FeatureTypes.TENSOR, FeatureTypes.BOOLEAN, tensorType1),
      Array[Any](new GenericTypedTensor(
        new LOLTensorData(tensorType1.getColumnTypes, ImmutableList.of(ImmutableList.of("1", "2")), ImmutableList.of(1, 2)), tensorType1),
        FeatureTypes.TENSOR, FeatureTypes.TENSOR, tensorType2)
    )
  }

  @Test(dataProvider = "failTestCases", expectedExceptions = Array(classOf[Exception]))
  def validateFail(value : Any, valueFeatureType : Any, configFeatureTypes : Any, configTensorType : Any): Unit = {
    val featureType = valueFeatureType.asInstanceOf[FeatureTypes]
    val featureValue = if (featureType == FeatureTypes.TENSOR) {
      new FeatureValue(value.asInstanceOf[GenericTypedTensor])
    } else {
      new FeatureValue(value, valueFeatureType.asInstanceOf[FeatureTypes]);
    }
    val featureTypeConfig = new FeatureTypeConfig(configFeatureTypes.asInstanceOf[FeatureTypes], configTensorType.asInstanceOf[TensorType], null)
    FeatureValueTypeValidator.validate("", featureValue, featureTypeConfig)
  }


  @Test(description = "Validate NTV feature type")
  def testValidateFeatureType(): Unit = {
    val features = Map("f1" -> new FeatureValue())
    val featureTypeConfig = new FeatureTypeConfig(FeatureTypes.TERM_VECTOR)
    val featureTypeConfigs: Map[String, FeatureTypeConfig] = Map("f1" -> featureTypeConfig)

    FeatureValueTypeValidator.validate(features, featureTypeConfigs)
  }

  @Test(description = "When the FeatureTypeConfig is not defined, it should not fail.")
  def testValidateFeatureTypeWhenTypeConfigMissing(): Unit = {
    val features = Map("f1" -> new FeatureValue())
    val featureTypeConfigs: Map[String, FeatureTypeConfig] = Map()

    FeatureValueTypeValidator.validate(features, featureTypeConfigs)
  }

  @Test(description = "validate 0D TensorType.")
  def testValidateTensorFeatureType(): Unit = {
    val tensorType = new TensorType(PrimitiveType.LONG, Collections.emptyList())
    val tensorData = Tensors.asScalarTensor(tensorType, 1L)
    val typedTensor = new GenericTypedTensor(tensorData, tensorType)
    val features = Map("f1" -> new FeatureValue(typedTensor))
    val featureTypeConfigs: Map[String, FeatureTypeConfig] =
      Map("f1" -> new FeatureTypeConfig(FeatureTypes.TENSOR, tensorType, "doc"))

    FeatureValueTypeValidator.validate(features, featureTypeConfigs)
  }

  @Test(description = "validate 1D TensorType.")
  def testValidate1DTensorFeatureType(): Unit = {
    val vec = util.Arrays.asList(1f, 2f)

    val tensorType = new TensorType(null, PrimitiveType.FLOAT, Collections.singletonList(PrimitiveDimensionType.INT), null)
    val tensorData = Tensors.asDenseTensor(tensorType, vec)
    val typedTensor = new GenericTypedTensor(tensorData, tensorType)
    val features = Map("f1" -> new FeatureValue(typedTensor))
    val featureTypeConfigs: Map[String, FeatureTypeConfig] =
      Map("f1" -> new FeatureTypeConfig(FeatureTypes.TENSOR, tensorType, "doc"))

    FeatureValueTypeValidator.validate(features, featureTypeConfigs)
  }

  @Test(description = "validate 1D mismatched TensorType throws exception.", expectedExceptions = Array(classOf[FeathrException]))
  def testValidate1DTensorFeatureTypeFailure(): Unit = {
    val vec = util.Arrays.asList(1f, 2f)

    val actualTensorType = new TensorType(null, PrimitiveType.FLOAT, Collections.singletonList(PrimitiveDimensionType.INT), null)
    val expectedTensorType = new TensorType(null, PrimitiveType.INT, Collections.singletonList(PrimitiveDimensionType.INT), null)
    val tensorData = Tensors.asDenseTensor(actualTensorType, vec)
    val typedTensor = new GenericTypedTensor(tensorData, actualTensorType)
    val features = Map("f1" -> new FeatureValue(typedTensor))
    val featureTypeConfigs: Map[String, FeatureTypeConfig] =
      Map("f1" -> new FeatureTypeConfig(FeatureTypes.TENSOR, expectedTensorType, "doc"))

    FeatureValueTypeValidator.validate(features, featureTypeConfigs)
  }

  @Test(description = "validate 1D mismatched tensor dimension throws exception.", expectedExceptions = Array(classOf[FeathrException]))
  def testValidate1DTensorFeatureTypeTensorDimensionFailure(): Unit = {
    val vec = util.Arrays.asList(1f, 2f)

    val actualTensorType = new TensorType(null, PrimitiveType.FLOAT, Collections.singletonList(PrimitiveDimensionType.INT), null)
    val expectedTensorType = new TensorType(null, PrimitiveType.FLOAT, Collections.singletonList(PrimitiveDimensionType.LONG), null)
    val tensorData = Tensors.asDenseTensor(actualTensorType, vec)
    val typedTensor = new GenericTypedTensor(tensorData, actualTensorType)
    val features = Map("f1" -> new FeatureValue(typedTensor))
    val featureTypeConfigs: Map[String, FeatureTypeConfig] =
      Map("f1" -> new FeatureTypeConfig(FeatureTypes.TENSOR, expectedTensorType, "doc"))

    FeatureValueTypeValidator.validate(features, featureTypeConfigs)
  }

  @Test(description = "validate 1D mismatched tensor category throws exception.", expectedExceptions = Array(classOf[FeathrException]))
  def testValidate1DTensorFeatureTypeTensorCategoryFailure(): Unit = {
    val vec = util.Arrays.asList(1f, 2f)

    val actualTensorType = new TensorType(TensorCategory.DENSE, PrimitiveType.FLOAT, Collections.singletonList(PrimitiveDimensionType.INT), null)
    val expectedTensorType = new TensorType(TensorCategory.SPARSE, PrimitiveType.FLOAT, Collections.singletonList(PrimitiveDimensionType.INT), null)
    val tensorData = Tensors.asDenseTensor(actualTensorType, vec)
    val typedTensor = new GenericTypedTensor(tensorData, actualTensorType)
    val features = Map("f1" -> new FeatureValue(typedTensor))
    val featureTypeConfigs: Map[String, FeatureTypeConfig] =
      Map("f1" -> new FeatureTypeConfig(FeatureTypes.TENSOR, expectedTensorType, "doc"))

    FeatureValueTypeValidator.validate(features, featureTypeConfigs)
  }
}
