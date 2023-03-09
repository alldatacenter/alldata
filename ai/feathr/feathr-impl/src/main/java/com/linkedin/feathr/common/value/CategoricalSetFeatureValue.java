package com.linkedin.feathr.common.value;

import com.linkedin.feathr.common.types.CategoricalSetFeatureType;
import com.linkedin.feathr.common.types.FeatureType;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;


/**
 * A specific FeatureValue class for CATEGORICAL_SET features. Underlying representation is a Set of String.
 */
public class CategoricalSetFeatureValue implements FeatureValue {
  private static final FeatureType TYPE = CategoricalSetFeatureType.INSTANCE;
  private final Set<String> _stringSet;

  private CategoricalSetFeatureValue(Set<String> stringSet) {
    _stringSet = Objects.requireNonNull(stringSet);
  }

  private CategoricalSetFeatureValue(Collection<String> strings) {
    this(new HashSet<>(Objects.requireNonNull(strings)));
  }

  /**
   * @param strings a collection of strings, in which duplicates would be ignored
   * @return a CategoricalSetFeatureValue for the provided strings
   */
  public static CategoricalSetFeatureValue fromStrings(Collection<String> strings) {
    return new CategoricalSetFeatureValue(strings);
  }

  /**
   * @param stringSet a Set of strings
   * @return a CategoricalSetFeatureValue for the provided string set
   */
  public static CategoricalSetFeatureValue fromStringSet(Set<String> stringSet) {
    return new CategoricalSetFeatureValue(stringSet);
  }

  /**
   * @return the categorical-set value, as a Set of strings
   */
  public Set<String> getStringSet() {
    return _stringSet;
  }

  @Override
  public FeatureType getFeatureType() {
    return TYPE;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CategoricalSetFeatureValue that = (CategoricalSetFeatureValue) o;
    return _stringSet.equals(that._stringSet);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_stringSet);
  }

  @Override
  public String toString() {
    return "CategoricalSetFeatureValue{" + "_stringSet=" + _stringSet + '}';
  }
}
