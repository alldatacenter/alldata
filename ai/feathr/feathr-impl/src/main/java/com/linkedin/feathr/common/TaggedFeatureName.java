package com.linkedin.feathr.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


/**
 * A tuple of (key tag, feature name)
 *
 * Essentially this is a feature name annotated with info on how we plan to query it.
 *
 *
 */
public class TaggedFeatureName implements Serializable {
  private final String _featureName;
  private final List<String> _keyTag;

  /** Cache the hash code for the TaggedFeatureName */
  private int _hash; // Default to 0

  @JsonCreator
  public TaggedFeatureName(@JsonProperty("key") List<String> keyTag, @JsonProperty("feature") String featureName) {
    _keyTag = keyTag;
    _featureName = featureName;
  }

  public TaggedFeatureName(String keyTag, String featureName) {
    this(Collections.singletonList(keyTag), featureName);
  }

  public List<String> getKeyTag() {
    return _keyTag;
  }

  public String getFeatureName() {
    return _featureName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TaggedFeatureName that = (TaggedFeatureName) o;
    // _keyTag, as a list, will create iterator objects that results in extra GC. Comparing _featureName first since
    // it doesn't incur additional GC.
    return Objects.equals(_featureName, that._featureName)
        && Objects.equals(_keyTag, that._keyTag);
  }

  @Override
  public int hashCode() {
    // Optimized way to compute hashCode to avoid array creation, and also cache hashcode to avoid re-computation.
    int h = _hash;
    if (h == 0) {
      h = Objects.hashCode(_featureName);
      h = 31 * h + Objects.hashCode(_keyTag);
      _hash = h;
    }
    return h;
  }

  @Override
  public String toString() {
    return "(" + _keyTag.stream().collect(Collectors.joining(",")) + "):" + _featureName;
  }
}
