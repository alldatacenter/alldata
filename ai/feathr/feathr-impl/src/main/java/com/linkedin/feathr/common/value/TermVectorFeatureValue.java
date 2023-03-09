package com.linkedin.feathr.common.value;

import com.linkedin.feathr.common.types.FeatureType;
import com.linkedin.feathr.common.types.TermVectorFeatureType;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;


/**
 * A specific FeatureValue class for TERM_VECTOR features. Underlying representation is a Map of String to Float.
 */
public class TermVectorFeatureValue implements FeatureValue {
  private static final FeatureType TYPE = TermVectorFeatureType.INSTANCE;
  protected final Map<String, Float> _termVector;

  protected TermVectorFeatureValue(Map<String, Float> termVector) {
    _termVector = Objects.requireNonNull(termVector);
  }

  /**
   * @return a term-vector feature value for the given term-vector map
   */
  public static TermVectorFeatureValue fromMap(Map<String, Float> map) {
    return new TermVectorFeatureValue(map);
  }

  /**
   * @return the contained term-vector map
   */
  public Map<String, Float> getTermVector() {
    return Collections.unmodifiableMap(_termVector);
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
    TermVectorFeatureValue that = (TermVectorFeatureValue) o;
    return _termVector.equals(that._termVector);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_termVector);
  }

  @Override
  public String toString() {
    return "TermVectorFeatureValue{" + "_termVector=" + _termVector + '}';
  }
}
