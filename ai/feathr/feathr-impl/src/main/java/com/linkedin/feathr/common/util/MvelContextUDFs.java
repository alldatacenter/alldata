package com.linkedin.feathr.common.util;

import com.linkedin.feathr.common.FeatureValue;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema;
import org.mvel2.ParserConfiguration;

import java.time.LocalDateTime;
import static com.linkedin.feathr.common.FeatureValue.*;


/**
 *
 * MVEL is an open-source expression language and runtime that makes it easy to write concise statements that operate
 * on structured data objects (such as Avro records), among other things.
 *
 * This class contains all the udfs used in Mvel for both online and offline
 */
public class MvelContextUDFs {
  // The udf naming in this class should use a_b_c form. (to be consistent with existing Spark built-in UDFs).
  private MvelContextUDFs() { }

  // register all the udfs defined in this class to the specific parser config
  public static void registerUDFs(Class<?> clazz, ParserConfiguration parserConfig) {
    // Scans the class (MvelContextUDFs) for any methods annotated with ExportToMvel
    for (Method method : clazz.getMethods()) {
      if (method.isAnnotationPresent(ExportToMvel.class)) {
        if (!Modifier.isStatic(method.getModifiers())) {
          throw new Error("MVEL context set up incorrectly. Imported method " + method + " must be static but is not.");
        }
        parserConfig.addImport(method.getName(), method);
      }
    }
  }

  // Scans this class for any functions annotated with ExportToMvel, and adds any such methods to our MVEL
  // parser context. Basically this is a way of adding UDFs.
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  public @interface ExportToMvel { }

  /**
   * Get the class type of the input object
   * WARNING: This is only used for debug for users.
   * @return Type in String form
   */
  @ExportToMvel
  public static String get_data_type(Object input) {
    if (input == null) {
      return "null";
    }
    return input.getClass().getName();
  }

  /**
   * Cast the input to double.
   * If it's null, null is returned. If it's string, it will try to parse it.
   * For number types, it will do standard conversion.
   * For other types, it will be coerced to double.
   *
   */
  @ExportToMvel
  public static Double cast_double(Object input) {
    if (input == null) {
      throw new RuntimeException("Input is null so it can't be casted to Double.");
    }
    if (input instanceof String) {
      return Double.parseDouble((String)input);
    } else if (input instanceof Number) {
      return ((Number)input).doubleValue();
    } else {
      return (Double)input;
    }
  }

  /**
   * Cast the input to float.
   * If it's null, null is returned. If it's string, it will try to parse it.
   * For number types, it will do standard conversion.
   * For other types, it will be coerced to float.
   *
   */
  @ExportToMvel
  public static Float cast_float(Object input) {
    if (input == null) {
      throw new RuntimeException("Input is null so it can't be casted to Float.");
    }
    if (input instanceof String) {
      return Float.parseFloat((String)input);
    } else if (input instanceof Number) {
      return ((Number)input).floatValue();
    } else {
      return (Float)input;
    }
  }

  /**
   * Cast the input to Integer.
   * If it's null, null is returned. If it's string, it will try to parse it.
   * For number types, it will do standard conversion.
   * For other types, it will be coerced to Integer.
   *
   */
  @ExportToMvel
  public static Integer cast_int(Object input) {
    if (input == null) {
      throw new RuntimeException("Input is null so it can't be casted to Integer.");
    }
    if (input instanceof String) {
      return Integer.parseInt((String)input);
    } else if (input instanceof Number) {
      return ((Number)input).intValue();
    } else {
      return (Integer)input;
    }
  }

  @ExportToMvel
  public static boolean and(boolean left, boolean right) {
    return left && right;
  }

  @ExportToMvel
  public static boolean or(boolean left, boolean right) {
    return left || right;
  }

  @ExportToMvel
  public static boolean not(boolean input) {
    return !input;
  }

  @ExportToMvel
  public static boolean isnull(Object input) {
    return input == null;
  }

  @ExportToMvel
  public static boolean isnotnull(Object input) {
    return input != null;
  }

  /**
   * Concatenate two strings into one.
   */
  @ExportToMvel
  public static String concat(String left, String right) {
    return left + right;
  }

  /**
   * Ternary operator. If input is evaluated to true, then first is returned, else second is returned.
   */
  @ExportToMvel
  public static String if_else(boolean input, String first, String second) {
    return input ? first : second;
  }

  @ExportToMvel
  public static Double if_else(boolean input, Double first, Double second) {
    return input ? first : second;
  }

  @ExportToMvel
  public static Float if_else(boolean input, Float first, Float second) {
    return input ? first : second;
  }

  @ExportToMvel
  public static Integer if_else(boolean input, Integer first, Integer second) {
    return input ? first : second;
  }

  @ExportToMvel
  public static boolean if_else(boolean input, boolean first, boolean second) {
    return input ? first : second;
  }

  /**
   * Return true if a feature has at least 1 term with value not zero.
   *
   * For checking if a feature is null, please use isPresent().
   */
  @ExportToMvel
  public static boolean isNonZero(Object vector) {
    return vector != null && CoercionUtils.coerceToVector(vector)
        .values()
        .stream()
        .anyMatch(x -> x != 0f);
  }

  /**
   * return true if a feature variable is not null.
   */
  @ExportToMvel
  public static boolean isPresent(Object featureValue) {
    return featureValue != null;
  }

  /**
   * Converts an object to a boolean
   */
  @ExportToMvel
  public static Boolean toBoolean(Object item) {
    if (item instanceof Boolean) {
      return (Boolean) item;
    } else if (item instanceof FeatureValue) {
      return ((FeatureValue) item).getAsBoolean();
    } else {
      // Construct a FeatureValue out of the input and then get the boolean representation
      return new FeatureValue(item).getAsBoolean();
    }
  }

  /**
   * Convert input to numeric value
   * Example inputs that can be converted
   * - Map("" -> 2.0) returns 2.0
   * - Map("876" -> 1.0) returns 876
   */
  @ExportToMvel
  public static Object toNumeric(Object item) {
    Map<String, Float> vector = CoercionUtils.coerceToVector(item);
    Set<String> keySet = vector.keySet();

    if (keySet.size() == 1) {
      if (vector.containsKey("")) {
        return vector.get("");
      } else {
        try {
          return Float.parseFloat(keySet.iterator().next());
        } catch (NumberFormatException e) {
          throw new RuntimeException("Feature cannot be converted to Float. Invalid feature: " + vector);
        }
      }
    } else {
      throw new RuntimeException("Feature must have 1 TermValue pair. Invalid feature: " + vector);
    }
  }

  /**
   * Convert input to categorical feature
   * Example inputs that can be converted
   * - Map("a" -> 2.0) return Map("a" -> 2.0)
   * - Map("" -> 2.5) returns ("2.5", 1.0)
   */
  @ExportToMvel
  public static Object toCategorical(Object item) {
    Map<String, Float> vector = CoercionUtils.coerceToVector(item);
    Set<String> keySet = vector.keySet();
    Map<String, Float> categoricalVector = new HashMap<>();
    if (keySet.size() == 1) {
      if (vector.containsKey(EMPTY_TERM)) {
        categoricalVector.put(vector.get(EMPTY_TERM).toString(), DEFAULT_VALUE);
        return categoricalVector;
      }
    }
    return vector;
  }

  @ExportToMvel
  public static Collection<String> getTerms(Object item) {
    if (item == null) {
      return Collections.emptyList();
    }
    return CoercionUtils.coerceToVector(item).keySet();
  }

  /***
   * return an ordered list of terms, based on descending order of corresponding values
   * @param item Object that can be converted to Map of string to float
   * @param k integer, if k<0, do reverse order selection, e.g. -1: select bottom first
   * @return List of string, ordered
   */
  @ExportToMvel
  public static List<String> getTopKTerms(Object item, int k) {
    if (item == null || k == 0) {
      return new ArrayList<>();
    }

    boolean ascending = true;

    if (k < 0) {
      k = -k;
      ascending = false;
    }

    return CoercionUtils.coerceToVector(item).entrySet().stream()
        .sorted(ascending ? Collections.reverseOrder(Map.Entry.comparingByValue()) : Map.Entry.comparingByValue())
        .limit(k)
        .map(Map.Entry::getKey)
        .collect(Collectors.toList());
  }

  /***
   * get terms with the highest value (duplicate will resolved randomly)
   *
   * @param item Object that can be converted to Map of string to float
   * @return String
   */
  @ExportToMvel
  public static String getTopTerm(Object item) {
    if (item == null) {
      return null;
    }

    return getTopKTerms(item, 1).get(0);
  }

  @ExportToMvel
  public static Collection<Object> distinct(Collection<Object> collection) {
    return collection.stream().distinct().collect(Collectors.toList());
  }

  @ExportToMvel
  public static Collection<Object> flatten(Collection<? extends Collection<Object>> collection) {
    return collection.stream().flatMap(Collection::stream).collect(Collectors.toList());
  }

  @ExportToMvel
  public static Float cosineSimilarity(Object obj1, Object obj2) {
    if (obj1 == null || obj2 == null) {
      return null;
    }
    Map<String, Float> mapA = CoercionUtils.coerceToVector(obj1);
    Map<String, Float> mapB = CoercionUtils.coerceToVector(obj2);
    double dotProduct = 0;
    double normA = 0;
    double normB = 0;
    for (Map.Entry<String, Float> entry : mapA.entrySet()) {
      String k = entry.getKey();
      float valA = entry.getValue();
      Float valB = mapB.get(k);
      if (valB != null) {
        dotProduct += (valA * valB);
      }
      normA += valA * valA;
    }
    for (Float valB : mapB.values()) {
      normB += valB * valB;
    }
    if (normA <= 0.0 || normB <= 0.0) {
      return 0.0F;
    } else {
      normA = Math.sqrt(normA);
      normB = Math.sqrt(normB);
      return (float) (dotProduct / (normA * normB));
    }
  }

  /**
   * Returns a standard dotProduct of two vector objects.
   * Use {@link MvelContextUDFs#cosineSimilarity(Object, Object)} for normalized dot-product.
   */
  @ExportToMvel
  public static Double dotProduct(Object obj1, Object obj2) {
    if (obj1 == null || obj2 == null) {
      return null;
    }
    Map<String, Float> mapA = CoercionUtils.coerceToVector(obj1);
    Map<String, Float> mapB = CoercionUtils.coerceToVector(obj2);
    double dotProduct = 0;

    for (Map.Entry<String, Float> entry : mapA.entrySet()) {
      String k = entry.getKey();
      float valA = entry.getValue();
      Float valB = mapB.get(k);
      if (valB != null) {
        dotProduct += ((double) valA * valB);
      }
    }

    return dotProduct;
  }

  /**
   * convert input to lower case string
   * @param input input string
   * @return lower case input
   */
  @ExportToMvel
  public static String toLowerCase(String input) {
    return input.toLowerCase();
  }

  /**
   * convert input to upper case string
   * @param input input string
   * @return upper case input
   */
  @ExportToMvel
  public static String toUpperCase(String input) {
    return input.toUpperCase();
  }

  @ExportToMvel
  public static long time_duration(Object startTime, Object endTime, String outputGranularity) {
    if (!outputGranularity.equals("minutes")) {
      throw new IllegalArgumentException(outputGranularity + " is not support in time_duration");
    }
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    LocalDateTime startDate = LocalDateTime.parse(startTime.toString(), formatter);
    LocalDateTime endDate = LocalDateTime.parse(endTime.toString(), formatter);
    return Duration.between(startDate, endDate).toMinutes();
  }

  // UDF naming follows Spark built-in UDFs convention. It's the same as the ones in spark.
  @ExportToMvel
  public static int dayofweek(Object input) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    LocalDateTime localDate = LocalDateTime.parse(input.toString(), formatter);
    return localDate.getDayOfWeek().getValue();
  }

  @ExportToMvel
  public static int dayofmonth(Object input) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    LocalDateTime localDate = LocalDateTime.parse(input.toString(), formatter);
    return localDate.getDayOfMonth();
  }

  @ExportToMvel
  public static int hourofday(Object input) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    LocalDateTime localDate = LocalDateTime.parse(input.toString(), formatter);
    return localDate.getHour();
  }

  @ExportToMvel
  public static Map<String, Float> extract_term_value_from_array(ArrayList<GenericRowWithSchema> array,
                                                                String termFieldName,
                                                                String valueFieldName) {
    return extract_term_value_from_array(array, termFieldName, valueFieldName, EMPTY_FILTER_EXPRESSION);
  }
  private static String EMPTY_FILTER_EXPRESSION = "";
  private static String EQUAL_FILTER_OPERATION = "==";
  private static String UNEQUAL_FILTER_OPERATION = "!=";
  @ExportToMvel
  public static Map<String, Float> extract_term_value_from_array(ArrayList<GenericRowWithSchema> array,
                                                                String termFieldName,
                                                                String valueFieldName,
                                                                String filterExpr) {
    Map<String, Float> result = new HashMap<>();
    // parse filter expression
    String filterName = "";
    String filterValue = "";
    Boolean hasFilter = !filterExpr.equals(EMPTY_FILTER_EXPRESSION);
    Boolean equalFilter = filterExpr.contains(EQUAL_FILTER_OPERATION);
    Boolean filterBasedOnConstant = true;
    if (hasFilter) {
      String delimiter = equalFilter ? EQUAL_FILTER_OPERATION: UNEQUAL_FILTER_OPERATION;
      String[] parts = filterExpr.split(delimiter);
      if (parts.length != 2) {
        throw new IllegalArgumentException("Filter expression: " + filterExpr + " in select_term_value_from_array is invalid." +
                " It must conform to [fieldName] == [value] or [fieldName] != [value]");
      }
      filterName = parts[0].trim();
      filterValue = parts[1].trim();
      filterBasedOnConstant = filterValue.startsWith("'") || filterValue.startsWith("\"");
      if (filterBasedOnConstant) {
        // extract the constant
        filterValue = filterValue.substring(1, filterValue.length()-1);
      }
    }
    for (GenericRowWithSchema item : array){
      Object resolvedFilterValue = filterValue;
      if (!filterBasedOnConstant) {
        resolvedFilterValue = item.getAs(filterValue);
      }
      Boolean equalFilterMet = hasFilter && equalFilter && item.getAs(filterName).equals(resolvedFilterValue);
      Boolean unEqualFilterMet = hasFilter && !equalFilter && !item.getAs(filterName).equals(resolvedFilterValue);
      if (!hasFilter || equalFilterMet || unEqualFilterMet) {
        result.put(item.getAs(termFieldName), item.getAs(valueFieldName));
      }
    }
    return result;
  }
}

