package com.linkedin.feathr.common.util;

import com.linkedin.feathr.common.FeatureTypes;
import com.linkedin.feathr.common.FeatureValue;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static java.util.Collections.*;
import static java.util.stream.Collectors.*;
import static com.linkedin.feathr.common.FeatureValue.DEFAULT_VALUE;
import static com.linkedin.feathr.common.FeatureValue.EMPTY_TERM;


/**
 * Utilities to coerce untyped data into feature vectors
 *
 * This is needed in order to make it easy to define features via simple expressions.
 * As a motivating example, I might want to say "A" instead of {"A": 1},
 * or I might want to say ["A", "X", "Z"] instead of {"A":1, "X":1, "Z":1}.
 *
 */
public class CoercionUtils {

  // Floats have 7 digits of precision, so use 5 digits as the tolerance
  private static final float TOLERANCE = 1e-5f;

  private CoercionUtils() { }

  /**
   * Coerce an item to term vector format according to the provided {@link FeatureTypes}.
   *
   * General rule for the dimension (terms) and value in the resulting term-vector:
   * <ol>
   *   <li>Terms are generated from {@link Character} (as is),  {@link CharSequence} (as is) and {@link Number} (whole numbers only)</li>
   *   <li>Values are generated from the float representations of {@link Number}s</li>
   * </ol>
   *
   * Specific rules for each {@link FeatureTypes} are as follows:
   *
   * {@link FeatureTypes#BOOLEAN} accepts {@link Boolean} as input and are interpreted as scalar feature value
   * with the following encoding:
   *
   *   {@code true} => { "": 1.0f }
   *   {@code false} => { } (empty map)
   *
   *
   * {@link FeatureTypes#NUMERIC} accepts {@link Number} as input and it is interpreted as scalar value without any
   * names/dimensions. The encoding produces vectors having "unit" dimension which we represent using the empty-string
   * with the float value.
   *
   *   0.12345f => { "": 0.12345f }
   *   100.01d => { "": 100.01f }
   *   BigDecimal.Ten => {"", 10f}
   *
   *
   * {@link {@link FeatureTypes#DENSE_VECTOR} accepts {@link List<Number>} as input. The terms
   * are the original index of element in the input list.
   *
   *  [ 10.0f, 3.0f, 6.0f ] => { "0": 10.0f, "1": 3.0f, "2": 6.0f }
   *  [ 10, 20, 30 ] => { "0": 10f, "1": 20f, "2": 30f }
   *
   *
   * {@link FeatureTypes#CATEGORICAL} accepts {@link Character}, {@link CharSequence} and whole {@link Number} as input. The
   * term is the string representation of the input withe a value of 1.0f.
   *
   *   "foo" => { "foo": 1.0f }
   *   2.0000f => { "2", 1.0f } // whole number
   *   2.000000000d => { "2", 1.0f } // whole number
   *   1.4f => throws an error since 1.4f cannot be treated as a whole number
   *
   *
   * {@link FeatureTypes#CATEGORICAL_SET} accepts {@link List<String>} as input and are represented as a term vector with
   * each string term having a value of 1.0f. It doesn't matter how many times an element appears in the list, 1.0f is
   * always given as the value. Values in the input list are de-duped.
   *
   *   ["A", "B", "C"] => { "A": 1.0f, "B": 1.0f, "C": 1.0f }
   *   [100, 200, 300] => { "100": 1.0f, "200": 1.0f, "300": 1.0f }
   *   [100, 1.5f, 200] => throws since 1.5f cannot be treated as a whole number
   *
   *
   * {@link FeatureTypes#TERM_VECTOR} accepts {@link Map} and {@link List<Map>} as inputs: 1) Standard {@link Map}s are
   * interpreted as vectors with minimal munging to ensure the dimension can be safely encoded as a string and
   * the value as a float.  2) {@link List<Map>} are first merged into a single map and handled as a Map as described above.
   * In case of any repeated keys, an error will be thrown.
   *
   *   { 123: 20.0f } => { "123": 20.0f }
   *   { 123.000f: 1.0d } => { "123": 1.0f } -> this is acceptable since 123.000f can be treated as the whole number 123
   *   { 123: "1" } -> throws an error since "1" is not a {@link Number}
   *   { 123.1f: 1 } -> throws an error since 123.1f cannot be treated as a whole number
   *
   * @throws RuntimeException when feature type doesn't match its expected data format, will throw exception
   */
  public static Map<String, Float> coerceToVector(Object item, FeatureTypes featureType) {

    switch (featureType) {

      case BOOLEAN:
        if (item instanceof Boolean) {
          return (Boolean) item ? singletonMap(EMPTY_TERM, 1.0f) : emptyMap();
        }
        break;

      case NUMERIC:
        return singletonMap(EMPTY_TERM, safeToFloat(item));

      case DENSE_VECTOR:
        if (item instanceof List) {
          return toDenseVector((List) item);
        }
        break;

      case CATEGORICAL:
        return singletonMap(safeToString(item), DEFAULT_VALUE);

      case CATEGORICAL_SET:
        if (item instanceof Collection) {
          return ((Collection<?>) item).stream().distinct().collect(toMap(CoercionUtils::safeToString, x -> DEFAULT_VALUE));
        } else {
          // A CATEGORICAL_SET feature can have only one CATGORICAL value. In this case, it is the same as the CATEGORICAL feature.
          return singletonMap(safeToString(item), DEFAULT_VALUE);
        }

      case TERM_VECTOR:
        if (item instanceof Map) {
          // maps are straight up converted to term vectors
          return toTermVector((Map) item);

        } else if (item instanceof Collection) {
          // Can be list of map so handle a Collection differently depending on the element type
          Collection<?> list = (Collection<?>) item;
          if (list.isEmpty()) {
            return Collections.emptyMap();
          }
          Object head = list.iterator().next();
          if (head instanceof Map) {
            // If the List's elements are Maps, then expect them to all to look like Map<String, Float> and combine them together
            try {
              return list.stream()
                  // call distinct before collect, as there are some data sources generate duplicate items in the list of map
                  .flatMap(x -> ((Map<?, ?>) x).entrySet().stream()).distinct()
                  .collect(toMap(e -> safeToString(e.getKey()), e -> safeToFloat(e.getValue())));
            } catch (Exception ex) {
              throw new RuntimeException("Input item " + item + " cannot be converted to feature type "
                  + featureType + " due to error: " + ex.getMessage());
            }
          }
        }
        break;

      case UNSPECIFIED:
      default:
        // when we encounter an unknown type, do our best to coerce based on the type of the item
        return coerceToVector(item);
    }
    throw new RuntimeException("Input item " + item + " cannot be converted to feature type "
        + featureType);
  }

  /**
   * Coerce item to term vector map, try to infer the feature type according to its type
   *
   * Basic rules:
   * 1. Treat single number (int, float, double, etc) as {@link FeatureTypes#NUMERIC}
   * 2. Treat single string as categorical {@link FeatureTypes#CATEGORICAL}
   * 3. Treat vector of numbers (int, float, double, etc) as {@link FeatureTypes#DENSE_VECTOR}
   * 4. Treat a collection of strings as {@link FeatureTypes#CATEGORICAL_SET}
   * 5. Treat map or list of maps as {@link FeatureTypes#TERM_VECTOR}
   * 6. Treat {@link FeatureValue} as {@link FeatureTypes#TERM_VECTOR}
   *
   * The function may be used to handle default value from configuration json file, and handle field values
   * extracted by MVEL expression
   * */
  public static Map<String, Float> coerceToVector(Object item) {
    checkNotNull(item);
    if (item instanceof FeatureValue) {
      // if input is already a FeatureValue, then just return its term vector representation
      return ((FeatureValue) item).getAsTermVector();
    } else if (item instanceof Collection) {
      Collection<?> collection = (Collection<?>) item;
      if (collection.isEmpty()) {
        return Collections.emptyMap();
      }
    }
    FeatureTypes coercedFeatureType = getCoercedFeatureType(item);
    if (coercedFeatureType != FeatureTypes.UNSPECIFIED) {
      return coerceToVector(item, coercedFeatureType);
    } else {
      // getCoercedFeatureType should be able to return the correct feature type, if it returns UNSPECIFIED,
      // it means the raw value cannot be coerced into a term-vector automatically.
      throw new RuntimeException("Input item " + item + " cannot be converted to feature value.");
    }
  }

  /**
   * Get the feature type that the input item would be coerced to
   * @param item input item to coerce
   * @return coerced feature type
   */
  public static FeatureTypes getCoercedFeatureType(Object item) {
    FeatureTypes coercedFeatureType = FeatureTypes.UNSPECIFIED;
    if (item instanceof Number) {
      // Handle a floating point number as a numerical feature value (not as a feature name/term/dimension)
      coercedFeatureType = FeatureTypes.NUMERIC;

    } else if (item instanceof Boolean) {
      coercedFeatureType =  FeatureTypes.BOOLEAN;
    } else if (item instanceof CharSequence || item instanceof Character) {
      coercedFeatureType =  FeatureTypes.CATEGORICAL;
    } else if (item instanceof Collection) {
      Collection<?> collection = (Collection<?>) item;
      if (!collection.isEmpty()) {
        // Handle a Collection differently depending on the element type
        Object head = collection.iterator().next();
        if (head instanceof Number) {
          // otherwise, should treated them as dense vector of numeric values (integers/doubles)
          coercedFeatureType =  FeatureTypes.DENSE_VECTOR;
        } else if (head instanceof CharSequence || head instanceof Character) {
          coercedFeatureType =  FeatureTypes.CATEGORICAL_SET;
        } else {
          // can be list of string, list of map or list of list(tuple)
          coercedFeatureType =  FeatureTypes.TERM_VECTOR;
        }
      }
    } else if (item instanceof Map) {
      coercedFeatureType =  FeatureTypes.TERM_VECTOR;
    }
    return coercedFeatureType;
  }

  /**
   * Returns true if the input {@link FeatureValue} is a numeric feature and false otherwise
   *
   * Numeric features when represented as a term vector have the form: {""=3.0f}
   */
  public static boolean isNumeric(FeatureValue featureValue) {
    Map<String, Float> featureVector = featureValue.getAsTermVector();
    return featureVector.size() == 1 && featureVector.containsKey(EMPTY_TERM);
  }

  /**
   * Returns true if the input {@link FeatureValue} is a boolean feature and false otherwise
   *
   * Boolean features when represented as a term vector has the form: ""=1.0f (true) and empty map (false)
   */
  public static boolean isBoolean(FeatureValue featureValue) {
    Map<String, Float> featureVector = featureValue.getAsTermVector();
    return featureVector.isEmpty()
        || ((featureVector.size() == 1) && equalsWithEpsilon(featureVector.get(EMPTY_TERM), DEFAULT_VALUE, TOLERANCE));
  }

  /**
   * Returns true the input {@link FeatureValue} is a categorical feature and false otherwise
   *
   * Categorical features when represented as a term vector has the form: {"term"=1.0f}
   */
  public static boolean isCategorical(FeatureValue featureValue) {
    Map<String, Float> featureVector = featureValue.getAsTermVector();
    return (featureVector.size() == 1)
        && equalsWithEpsilon(featureVector.entrySet().iterator().next().getValue(), DEFAULT_VALUE, TOLERANCE)
        && (!featureVector.entrySet().iterator().next().getKey().equals(EMPTY_TERM));
  }

  /**
   * Safely convert an input object into its string representation.
   *
   * Safe conversions are supported for the following:
   * <ol>
   *  <li>{@link CharSequence} and {@link Character} are simply converted to String</li>
   *  <li>{@link Number} converted to String from its {@code longValue()}</li>
   * </ol>
   *
   * @throws RuntimeException if the input {@link Number} is not a whole number within some precision
   */
  public static String safeToString(Object item) {
    checkNotNull(item);
    if (item instanceof CharSequence || item instanceof Character) {
      return item.toString();
    }
    // this happens when reading JSON config files, some whole numbers may be parsed with trailing zeros (e.g. 1.0000)
    // In this case, we can safely treat them as as whole number and use its long representation
    if (item instanceof Number) {
      double numDouble = ((Number) item).doubleValue();
      // it would be hard to
      if ((numDouble - Math.round(numDouble)) <= TOLERANCE) {
        return String.valueOf(((Number) item).longValue());
      } else {
        throw new RuntimeException(
            String.format("Input: %f was not within %f of an integral value", numDouble, TOLERANCE));
      }
    } else {
      throw new RuntimeException(
          "Couldn't safely coerce to String, only {String, Character and Number} are allowed but got unexpected type "
              + item.getClass().getName() + " (value = " + item + ")");
    }
  }

  /**
   * Safely convert an input object to its float representation.
   * If the input is not a valid float, then an error will be thrown
   */
  public static float safeToFloat(Object item) {
    checkNotNull(item);
    if (item instanceof Number) {
      return ((Number) item).floatValue();
    } else {
      throw new RuntimeException(
          "Couldn't safely coerce to Float, expecting input to be a Number but got " + item.getClass()
              + " instead. (Input = " + item + ")");
    }
  }

  private static <T> void checkNotNull(T item) {
    if (null == item) {
      throw new NullPointerException("Unexpected null");
    }
  }

  /**
   * Convert handle a Map as a term->value vector. Expect the keys to be safely coercible to string, and values to be safely
   * coercible to float
   */
  private static Map<String, Float> toTermVector(Map<?, ?> inputMap) {
    return inputMap.entrySet()
        .stream()
        .collect(toMap(e -> safeToString(e.getKey()), e -> safeToFloat(e.getValue())));
  }


  /**
   * Create a dense vector from a List by walking through the list items one by one.
   * The elements of the input list must be {@link Number}
   */
  private static Map<String, Float> toDenseVector(List<?> list) {
    if (list.isEmpty()) {
      return Collections.emptyMap();
    } else {
      return IntStream.range(0, list.size())
          .boxed()
          .collect(toMap(CoercionUtils::safeToString, i -> safeToFloat(list.get(i))));
    }
  }

  private static boolean equalsWithEpsilon(Float f1, Float f2, Float epsilon) {
    return Math.abs(f1 - f2) <= epsilon;
  }
}
