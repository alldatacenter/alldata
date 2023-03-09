package com.linkedin.feathr.core.utils;

import com.typesafe.config.ConfigUtil;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * Utility class with methods to pretty-print different Java collections
 */
public final class Utils {

  private Utils() {
  }

  /*
   * For List
   */
  public static <T> String string(List<T> list, String start, String sep, String end) {
    String mid = list.stream().map(T::toString).collect(Collectors.joining(sep));
    //String mid = String.join(sep, list);
    return start + mid + end;
  }

  public static <T> String string(List<T> list) {
    return string(list, "[", ", ", "]");
  }

  public static <T> String string(List<T> list, String sep) {
    return string(list, "[", sep, "]");
  }

  /*
   * For Set
   */
  public static <T> String string(Set<T> set, String start, String sep, String end) {
    String mid = set.stream().map(T::toString).collect(Collectors.joining(sep));
    return start + mid + end;
  }

  public static <T> String string(Set<T> set) {
    return string(set, "{", ", ", "}");
  }

  public static <T> String string(Set<T> set, String sep) {
    return string(set, "{", sep, "}");
  }

  /*
   * For Map
   */
  public static <K, V> String string(Map<K, V> map, String start, String sep, String end) {
    StringBuilder sb = new StringBuilder();
    sb.append(start);
    map.forEach((k, v) -> sb.append(k.toString()).append(":").append(v.toString()).append(sep));
    sb.append(end);
    return sb.toString();
  }

  public static <K, V> String string(Map<K, V> map) {
    return string(map, "{", ", ", "}");
  }

  public static <K, V> String string(Map<K, V> map, String sep) {
    return string(map, "{", sep, "}");
  }

  /*
   * For Array
   */
  public static <T> String string(T[] array, String start, String sep, String end) {
    String mid = Arrays.stream(array).map(T::toString).collect(Collectors.joining(sep));
    return start + mid + end;
  }

  public static <T> String string(T[] array) {
    return string(array, "[", ", ", "]");
  }

  public static <T> String string(T[] array, String sep) {
    return string(array, "[", sep, "]");
  }

  /*
   * for test, similar to require function in Scala
   */
  public static void require(boolean expression, String message) {
    if (!expression) {
      throw new IllegalArgumentException(message);
    }
  }

  public static void require(boolean expression) {
    if (!expression) {
      throw new IllegalArgumentException();
    }
  }

  /*
   * Quotes a key if
   *    it contains "." or ":"
   *    and it's not already quoted
   * so that the key is not interpreted as a path expression by HOCON/Lightbend
   * Config library. Examples of such keys are names such as anchor names and feature names.
   * @param key the string to be quoted if needed
   * @return quoted string as per JSON specification
   */
  public static String quote(String key) {
    return ((key.contains(".") || key.contains(":")) && !key.startsWith("\"") && !key.endsWith("\""))
        ? ConfigUtil.quoteString(key) : key;
  }
}
