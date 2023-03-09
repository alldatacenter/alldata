package com.linkedin.feathr.common;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoSerializable;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.google.common.annotations.VisibleForTesting;
import com.linkedin.feathr.common.tensor.TensorData;
import com.linkedin.feathr.common.tensor.TensorType;
import com.linkedin.feathr.common.tensor.TensorTypes;
import com.linkedin.feathr.common.tensor.Tensors;
import com.linkedin.feathr.common.tensorbuilder.UniversalTensorBuilderFactory;
import com.linkedin.feathr.common.types.FeatureType;
import com.linkedin.feathr.common.types.TensorFeatureType;
import com.linkedin.feathr.common.util.CoercionUtils;
import com.linkedin.feathr.common.value.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;


/**
 * Describes the basic representation of a feature value in Feathr.
 * This is the OLD FeatureValue API. Feathr is migrating to a new FeatureValue API defined at
 * com.linkedin.feathr.common.value.FeatureValue. This implementation of the old API is a wrapper around the new API,
 * with lots of glue to maintain compatibility.
 *
 *
 * SERIALIZATION: For use with Spark RDD API, this class is serializable both by Java Object Serialization and by Kryo.
 *  Note that because the serialized representation is based on NTV (name-term-value), serialization will NOT work for
 *  tensor features having rank greater than 1.
 *
 * MUTABILITY: For legacy compatibility, instances of this class are mutable under the following conditions:
 *  1. When constructed with zero-argument constructor
 *  2. When constructed with Map<String,Float> constructor
 *  3. When constructed specifically as a term-vector using any createStringTermVector factory methods
 *  4. When deserialized with Kryo or Java-Object Serialization
 *  When mutable, instances may be mutated by:
 *  1. Using modifier methods the Map objects returned by .getValue or .getAsTermVector
 *  2. Using modifier methods on ._value public member field.
 *  3. Using the .put method.
 * All mutability support in FeatureValue is DEPRECATED.
 *
 * NOTE: This class will be deprecated soon in favor of {@link com.linkedin.feathr.common.value.FeatureValue}
 */
// WILL BE DEPRECATED SOON (after new anchor extractor, feature derivation function, and FeathrClient APIs have been

public class FeatureValue implements Serializable, KryoSerializable {
  public static final float DEFAULT_VALUE = 1.0f;
  public static final String EMPTY_TERM = "";

  // _featureValueInternal WOULD BE FINAL, but is non-final for compatibility with Java Object Serialization
  private /*final*/ com.linkedin.feathr.common.value.FeatureValue _featureValueInternal;

  @VisibleForTesting
  public FeatureValue(com.linkedin.feathr.common.value.FeatureValue featureValueInternal) {
    _featureValueInternal = Objects.requireNonNull(featureValueInternal);
  }

  /**
   * @deprecated Use the static factory methods such as {@link #createNumeric(Number)} to construct features instead
   *
   * N.B. Although this takes {@link Object}, there are runtime type checks to ensure the passed type can be converted
   * to a feature value.
   */
  @Deprecated
  public FeatureValue(Object value) {
    // follow legacy behavior
    this(value, FeatureTypes.UNSPECIFIED);
  }

  /**
   * @deprecated Use the static factory methods such as {@link #createNumeric(Number)} to construct features instead
   */
  @Deprecated
  public FeatureValue() {
    // follow legacy behavior, where a sadly mutable, empty term vector is returned
    _featureValueInternal = new CompatibilityUtils.MutableTermVectorFeatureValue(new HashMap<>());
  }

  /**
   * @deprecated Use the static factory methods such as {@link #createNumeric(Number)} to construct features instead
   */
  @Deprecated
  public FeatureValue(Object value, FeatureTypes type) {
    // expectations about what type `value` may be, for the various type enum values, is defined to match legacy behavior from CoercionUtils
    switch (type) {
      case BOOLEAN:
        boolean booleanValue = (Boolean) value;
        _featureValueInternal = BooleanFeatureValue.fromBoolean(booleanValue);
        break;
      case NUMERIC:
        float floatValue = ((Number) value).floatValue();
        _featureValueInternal = NumericFeatureValue.fromFloat(floatValue);
        break;
      case CATEGORICAL:
        String stringValue = CoercionUtils.safeToString(value);
        _featureValueInternal = CategoricalFeatureValue.fromString(stringValue);
        break;
      case CATEGORICAL_SET:
        Set<String> stringSet = ((Collection<?>) value).stream().map(CoercionUtils::safeToString).collect(toSet());
        _featureValueInternal = CategoricalSetFeatureValue.fromStrings(stringSet);
        break;
      case DENSE_VECTOR:
        List<Number> numberList = ((Collection<?>) value).stream().map(CoercionUtils::safeToFloat).collect(toList());
        _featureValueInternal = DenseVectorFeatureValue.fromNumberList(numberList);
        break;
      case TERM_VECTOR:
      case UNSPECIFIED:
        Map<String, Float> tv = CoercionUtils.coerceToVector(value, type);
        _featureValueInternal = new CompatibilityUtils.MutableTermVectorFeatureValue(tv);
        break;
      case TENSOR:
        throw new IllegalArgumentException("This legacy constructor does not support Tensor");
      default:
        throw new IllegalArgumentException("Unhandled type " + type);
    }
  }

  /**
   * Constructor to create FeatureValue with a tensor object
   * @param tensor {@link TypedTensor}
   * @throws NullPointerException if tensor parameter is null
   */
  public FeatureValue(TypedTensor tensor) {
    Objects.requireNonNull(tensor, "TypedTensor object can't be null");
    _featureValueInternal = TensorFeatureValue.fromTypedTensor(tensor);
  }

  /**
   * Creates a (tensor) FeatureValue based on the provided TensorType.
   * @param value The data used to creates the the tensor.
   * @param type The corresponding type needed to create the tensor.
   * @throws IllegalArgumentException if the data type is not supported or if create a 2-d or higher dimension tensor.
   */
  private FeatureValue(Object value, TensorType type) {
    TensorFeatureType tensorFeatureType = TensorFeatureType.withTensorType(type);
    Objects.requireNonNull(value, "value object can't be null");
    Objects.requireNonNull(type, "TensorType object can't be null");
    if (type.getDimensionTypes() == null || type.getDimensionTypes().size() == 0) {
      TensorData tensorData = Tensors.asScalarTensor(type, value);
      _featureValueInternal = TensorFeatureValue.fromTensorData(tensorFeatureType, tensorData);
    } else if (type.getDimensionTypes().size() == 1) {
      if (value instanceof Map) { // sparse tensor with map
        TensorData tensorData = Tensors.asSparseTensor(type, (Map) value);
        _featureValueInternal = TensorFeatureValue.fromTensorData(tensorFeatureType, tensorData);
      } else if (value instanceof List) {
        // Support a list of maps (typically return by MVEL expression such as ([foo : bar] in baz), and a list of numeric
        TensorData tensorData;
        Collection<?> list = (Collection<?>) value;
        if (list.isEmpty()) {
          tensorData = UniversalTensorBuilderFactory.INSTANCE.getTensorBuilder(tensorFeatureType.getTensorType()).build();
        } else {
          Object head = list.iterator().next();
          if (head instanceof Map) {
            // If the List's elements are Maps, merge them into a single map
            Map<?, ?> mergedMap = list.stream()
                // call distinct before collect, as there are some data sources generate duplicate items in the list of map
                .flatMap(x -> ((Map<?, ?>) x).entrySet().stream())
                .distinct()
                .collect(toMap(e -> e.getKey(), e -> e.getValue()));
            tensorData = Tensors.asSparseTensor(type, mergedMap);
          } else {
            tensorData = Tensors.asDenseTensor(type, (List) value);
          }
        }
        _featureValueInternal = TensorFeatureValue.fromTensorData(tensorFeatureType, tensorData);
      } else if (value instanceof float[]) {
        TensorData tensorData = Tensors.asDenseTensor(type, (float[]) value);
        _featureValueInternal = TensorFeatureValue.fromTensorData(tensorFeatureType, tensorData);
      } else if (value instanceof int[]) {
        TensorData tensorData = Tensors.asDenseTensor(type, (int[]) value);
        _featureValueInternal = TensorFeatureValue.fromTensorData(tensorFeatureType, tensorData);
      } else if (value instanceof long[]) {
        TensorData tensorData = Tensors.asDenseTensor(type, (long[]) value);
        _featureValueInternal = TensorFeatureValue.fromTensorData(tensorFeatureType, tensorData);
      } else if (value instanceof double[]) {
        TensorData tensorData = Tensors.asDenseTensor(type, (double[]) value);
        _featureValueInternal = TensorFeatureValue.fromTensorData(tensorFeatureType, tensorData);
      } else {
        throw new IllegalArgumentException("The value type " + value.getClass() + " is not supported.");
      }
    } else {
      throw new IllegalArgumentException("Only creating of 0-d and 1-d tensor is supported but got: " + type);
    }
  }

  /**
   * @deprecated Use the static factory methods such as {@link #createStringTermVector(Map)} to construct features instead
   */
  @Deprecated
  public FeatureValue(Map<String, Float> value) {
    Objects.requireNonNull(value);
    _featureValueInternal = new CompatibilityUtils.MutableTermVectorFeatureValue(value);
  }


  /**
   * Modify a term-vector FeatureValue by adding a term-value mapping. Mutability is only allowed under certain
   * conditions, described in the class-level Javadoc.
   *
   * @throws RuntimeException if not mutable
   * @deprecated mutability in FeatureValue is deprecated; see class-level Javadoc
   */
  @Deprecated
  public void put(String key, Float value) {
    if (_featureValueInternal instanceof CompatibilityUtils.MutableTermVectorFeatureValue) {
      ((CompatibilityUtils.MutableTermVectorFeatureValue) _featureValueInternal).getMutableView().put(key, value);
    } else {
      throw new RuntimeException("Cannot modify FeatureValue");
    }
  }

  /**
   * Check if the FeatureValue is empty or not. When it's empty, it return true; otherwise false.
   * Emptiness of FeatureValue is defined in the following way:
   * 1. For tensor based features, empty means an tensor.
   * 2. For legacy term vector based features, empty means an empty vector.
   * Note: Since Boolean NTV feature false is equivalent to empty map, so it's false is considered as empty as well.
   */
  public boolean isEmpty() {
    if (getFeatureType().getBasicType().equals(FeatureType.BasicType.TENSOR)) {
      return getAsTensorData().isEmpty();
    } else {
      return getAsTermVector().isEmpty();
    }
  }

  /**
   * @deprecated Prefer to use {@link #getNumTerms()} which has a more descriptive name
   * @throws RuntimeException if this instance stores a tensor of rank greater than 1
   */
  @Deprecated
  public int size() {
    return getAsTermVector().size();
  }

  /**
   * @deprecated Prefer to use {@link #getAsTermVector()} which has a more descriptive name
   */
  @Deprecated
  public Map<String, Float> getValue() {
    return getAsTermVector();
  }

  /**
   * Get the number of terms in feature when interpreted as a term vector.
   *
   * <li>For a numeric feature, this will always be 1</li>
   * <li>For a categorical feature, this will always be 1</li>
   * <li>For a categorical set feature, this will be the number of terms in the set</li>
   * <li>For any vector feature, it will be the number of explicitly present elements in the vector</li>
   *
   * @return the number of terms in this feature
   * @throws RuntimeException if this instance stores a tensor of rank greater than 1
   */
  @Deprecated
  public int getNumTerms() {
    return size();
  }

  public FeatureType getFeatureType() {
    return _featureValueInternal.getFeatureType();
  }

  /**
   * @return a Quince TypedTensor representation of this feature value
   */
  public TypedTensor getAsTypedTensor() {
    return CompatibilityUtils.TypedTensorFormatMapper.INSTANCE.fromFeatureValue(_featureValueInternal);
  }

  /**
   * @return a Quince TensorData representation of this feature value
   */
  public TensorData getAsTensorData() {
    return QuinceFeatureFormatMapper.INSTANCE.fromFeatureValue(_featureValueInternal);
  }

  /**
   * Gets the feature in an NTV term-vector representation.
   *
   * @return {@link Map} from {@link String} term names to {@link Float} values
   * @throws RuntimeException if the feature is a tensor of rank greater than 1, then this feature cannot be represented
   *                          as an NTV term vector
   */
  public Map<String, Float> getAsTermVector() {
    if (_featureValueInternal instanceof CompatibilityUtils.MutableTermVectorFeatureValue) {
      return ((CompatibilityUtils.MutableTermVectorFeatureValue) _featureValueInternal).getMutableView();
    } else {
      return NTVFeatureFormatMapper.INSTANCE.fromFeatureValue(_featureValueInternal);
    }
  }

  /**
   * get feature value as number
   * @return number value
   * @throws RuntimeException if this is not a valid numeric feature
   */
  public Float getAsNumeric() {
    return CompatibilityUtils.getNumeric(_featureValueInternal);
  }

  /**
   * Gets a CATEGORICAL feature value represented as a string.
   * @return string value
   * @deprecated use {@link #getAsCategorical()} instead
   */
  @Deprecated
  public String getAsString() {
    return getAsCategorical();
  }

  /**
   * Returns the single categorical value associated with the feature
   *
   * Note: this method will return {@link String} even if the feature was created using {@link FeatureValue#createCategorical(Number)}
   * because categorical are meant to contain only discrete values which are represented by String
   *
   * @return the categorical term as a String
   * @throws RuntimeException if this is not a valid categorical feature
   */
  public String getAsCategorical() {
    return CompatibilityUtils.getCategoricalString(_featureValueInternal);
  }

  /**
   * Get feature value as boolean
   * @return boolean value
   * @throws RuntimeException if this is not a valid boolean feature
   */
  public Boolean getAsBoolean() {
    return CompatibilityUtils.getBoolean(_featureValueInternal);
  }

  /**
   * Creates a numeric feature.
   *
   * @param num {@link Number}
   * @return {@link FeatureValue}
   */
  public static FeatureValue createNumeric(Number num) {
    return new FeatureValue(NumericFeatureValue.fromFloat(num.floatValue()));
  }

  /**
   * Creates a categorical feature using a whole number term which is within some precision of a whole number. The
   * whole number term will be stored as a {@link String}.
   *
   * Only whole number terms are accepted because categorical are meant to contain discrete values. The common use case
   * for using number as the term would be to represent some entity ID (e.g. y, x)
   *
   * @param term {@link Number} the numeric term which will be stored as a {@link String} term
   * @return a new instance of {@link FeatureValue}
   * @throws RuntimeException if num is not within some precision of a whole number
   */
  @Deprecated
  public static FeatureValue createCategorical(Number term) {
    return createCategorical(CoercionUtils.safeToString(term));
  }

  /**
   * Creates a categorical feature from a string term
   *
   * @param term {@link CharSequence}
   * @return a new instance of {@link FeatureValue}
   */
  public static FeatureValue createCategorical(CharSequence term) {
    return new FeatureValue(CategoricalFeatureValue.fromString(term.toString()));
  }

  /**
   * Creates a categorical feature from a character term
   *
   * @param term {@link Character}
   * @return a new instance of {@link FeatureValue}
   */
  public static FeatureValue createCategorical(Character term) {
    return createCategorical(String.valueOf(term));
  }

  /**
   * Creates a dense vector feature from a {@link List} of numbers
   *
   * @param vec {@link List}
   * @param <T> the type of {@link Number}
   * @return a new instance of {@link FeatureValue}
   */
  public static <T extends Number> FeatureValue createDenseVector(List<T> vec) {
    return new FeatureValue(DenseVectorFeatureValue.fromNumberList(vec));
  }

  /**
   * Creates a dense vector feature from an array of numbers
   *
   * @param vec {@link T[]} array of items
   * @param <T> the type of {@link Number}
   * @return a new instance of {@link FeatureValue}
   */
  public static <T extends Number> FeatureValue createDenseVector(T[] vec) {
    return createDenseVector(Arrays.asList(vec));
  }

  private static FeatureValue createTermVectorFromRawMap(Map<?, ?> inputMap) {
    Map<String, Float> stringFloatMap = inputMap.entrySet()
        .stream()
        .collect(toMap(e -> CoercionUtils.safeToString(e.getKey()), e -> CoercionUtils.safeToFloat(e.getValue())));
    return new FeatureValue(stringFloatMap);
  }

  /**
   * Creates a term vector feature (aka sparse vector) using strings for the terms
   *
   * @param inputMap {@link Map} from a string term to numeric values
   * @param <K> the string term type
   * @param <V> the numeric value type
   * @return a new instance of {@link FeatureValue}
   */
  public static <K extends CharSequence, V extends Number> FeatureValue createStringTermVector(Map<K, V> inputMap) {
    return createTermVectorFromRawMap(inputMap);
  }

  /**
   * Creates a term vector feature (e.g. sparse vector) using a numeric to numeric map
   *
   * @param inputMap {@link Map} from a number term (within some precision of a whole numbers) to numeric value
   * @param <K> the numeric term type
   * @param <V> the numeric value type
   * @return a new instance of {@link FeatureValue}
   * @throws RuntimeException if input numeric key is a not within some precision of a whole numbers (e.g. 1.5f)
   * @throws RuntimeException if multiple input numeric keys map to the same whole number (e.g. 1.00f and 1.0000d)
   */
  public static <K extends Number, V extends Number> FeatureValue createNumericTermVector(Map<K, V> inputMap) {
    return createTermVectorFromRawMap(inputMap);
  }

  private static <K, V> Map<K, V> mergeMaps(Collection<Map<K, V>> maps) {
    return maps.stream()
        .flatMap(x -> x.entrySet().stream())
        .collect(toMap(x -> x.getKey(), x -> x.getValue()));
  }

  /**
   * Creates a term vector feature (AKA sparse vector) from multiple term vectors with string terms
   *
   * @param termValues {@link Collection} of {@link Map} from string term to numeric value
   * @param <K> the string term type
   * @param <V> the numeric value type
   * @return a new instance of {@link FeatureValue}
   * @throws RuntimeException if input numeric key is a not within some precision of a whole numbers
   * @throws RuntimeException if multiple input numeric keys map to the same whole number (e.g. 1.00f and 1.0000d)
   */
  public static <K extends CharSequence, V extends Number> FeatureValue createStringTermVector(Collection<Map<K, V>> termValues) {
    return createTermVectorFromRawMap(mergeMaps(termValues));
  }

  /**
   * Creates a term vector feature (e.g. sparse vector) by merging multiple numeric to numeric map
   *
   * @param termValues {@link Map} from a number term to numeric value
   * @param <K> the numeric term type
   * @param <V> the numeric value type
   * @return a new instance of {@link FeatureValue}
   * @throws RuntimeException if input numeric keys are not within some precision of a whole numbers (e.g. 1.5f)
   * @throws RuntimeException if multiple input numeric keys map to the same whole number (e.g. 1.00f and 1.0000d)
   */
  public static <K extends Number, V extends Number> FeatureValue createNumericTermVector(Collection<Map<K, V>> termValues) {
    return createTermVectorFromRawMap(mergeMaps(termValues));
  }

  private static Collection<String> safeToStringCollection(Collection<?> collection) {
    return collection.stream().map(CoercionUtils::safeToString).collect(Collectors.toList());
  }

  /**
   * Creates a categorical set feature from a collection of Numbers that are whole numbers
   *
   * @param terms {@link Collection} of {@link Number} terms
   * @param <T> the numeric type
   * @return a new instance of {@link FeatureValue}
   * @throws RuntimeException if input numeric elements are not within some precision of a whole numbers (e.g. 1.5f)
   */
  public static <T extends Number> FeatureValue createNumericCategoricalSet(Collection<T> terms) {
    return createStringCategoricalSet(safeToStringCollection(terms));
  }

  /**
   * Creates a categorical set feature from string terms
   *
   * @param terms {@link Collection} of {@link CharSequence} terms
   * @param <T> the string term type
   * @return a new instance of {@link FeatureValue}
   */
  public static <T extends CharSequence> FeatureValue createStringCategoricalSet(Collection<T> terms) {
    return new FeatureValue(CategoricalSetFeatureValue.fromStrings(safeToStringCollection(terms)));
  }

  /**
   * Creates a categorical set feature from character terms
   *
   * @param characters {@link Collection} of {@link Character}
   * @return a new instance of {@link FeatureValue}
   */
  public static FeatureValue createCharacterCategoricalSet(Collection<Character> characters) {
    return createStringCategoricalSet(safeToStringCollection(characters));
  }

  /**
   * Creates a boolean feature.
   *
   * @param b {@link Boolean}
   * @return a new instance of {@link FeatureValue}
   */
  public static FeatureValue createBoolean(Boolean b) {
    return new FeatureValue(BooleanFeatureValue.fromBoolean(b));
  }

  /**
   * Creates a (tensor) FeatureValue based on the provided TensorType.
   * @param value The data used to creates the the tensor.
   * @param type The corresponding type needed to create the tensor.
   * @throws IllegalArgumentException if the data type is not supported or if create a 2-d or higher dimension tensor.
   */
  public static FeatureValue createTensor(Object value, TensorType type) {
    return new FeatureValue(value, type);
  }

  /**
   * Create a FeatureValue from a TensorData (adding a TensorType inferred from the primitives of the TensorData).
   *
   * NOTE that the TensorType will not have any shape, so may be NOT reused for constructing new dense tensors from arrays and lists.
   */
  public static FeatureValue createTensor(TensorData tensorData) {
    return new FeatureValue(new GenericTypedTensor(tensorData, TensorTypes.fromTensorData(tensorData)));
  }

  @Override
  public String toString() {
    return Objects.toString(_featureValueInternal);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FeatureValue that = (FeatureValue) o;
    // This is a shortcut case.
    if (_featureValueInternal.equals(that._featureValueInternal)) {
      return true;
    } else {
      // Equality for legacy FeatureValue API is defined as the two values having equal NTV term-vector representations.
      return getAsTermVector().equals(that.getAsTermVector());
    }
  }

  @Override
  public int hashCode() {
    // To maintain backward-compatibility, the NTV _featureValueInternal should only use getAsTermVector().hashCode().
    // For tensor _featureValueInternal, we just call _featureValueInternal.hashCode().
    if (_featureValueInternal instanceof TensorFeatureValue) {
      return _featureValueInternal.hashCode();
    } else {
      return getAsTermVector().hashCode();
    }
  }

  /**
   * Java Object Serialization support for compatibility with legacy Feathr-Offline RDD-based API use.
   * The serialized form is based on the NTV representation, since Quince tensors are not Serializable.
   * So this does NOT work for arbitrary tensors (rank > 1) since those are not NTV-compatible.
   */
  private void writeObject(ObjectOutputStream oos) throws IOException {
    oos.writeObject(new HashMap<>(getAsTermVector()));
  }
  /**
   * Java Object Serialization support for compatibility with legacy Feathr-Offline RDD-based API use.
   */
  @SuppressWarnings("unchecked")
  private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
    Map<String, Float> map = (Map<String, Float>) ois.readObject();
    _featureValueInternal = new CompatibilityUtils.MutableTermVectorFeatureValue(map);
  }

  @Override
  public void write(Kryo kryo, Output output) {
    kryo.writeObject(output, new HashMap<>(getAsTermVector()));
  }

  @SuppressWarnings("unchecked")
  @Override
  public void read(Kryo kryo, Input input) {
    Map<String, Float> map = (Map<String, Float>) kryo.readObject(input, HashMap.class);
    _featureValueInternal = new CompatibilityUtils.MutableTermVectorFeatureValue(map);
  }
}
