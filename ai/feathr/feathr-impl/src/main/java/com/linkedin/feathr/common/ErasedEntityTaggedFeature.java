package com.linkedin.feathr.common;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


/**
 * A {@link com.linkedin.feathr.common.TaggedFeatureName} whose String tags have been "erased", i.e. replaced with integers.
 * The mapping between the integer identifiers and their original string names is unknown by this class and must be
 * maintained externally.
 */
public class ErasedEntityTaggedFeature implements Serializable {
  private final List<Integer> _binding;
  private final FeatureRef _featureRef;

  public ErasedEntityTaggedFeature(List<Integer> binding, String featureName) {
    this(binding, new FeatureRef(featureName));
  }

  public ErasedEntityTaggedFeature(List<Integer> binding, FeatureRef featureRef) {
    _binding = binding;
    _featureRef = featureRef;
  }

  public List<Integer> getBinding() {
    return _binding;
  }

  public String getFeatureName() {
    return _featureRef.getName();
  }

  public String getErasedTagFeatureName() {
    return _binding.toString() + ":" + _featureRef.getName();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ErasedEntityTaggedFeature that = (ErasedEntityTaggedFeature) o;
    return _binding.equals(that._binding) && _featureRef.equals(that._featureRef);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_binding, _featureRef);
  }

  @Override
  public String toString() {
    return "(" + _binding.stream().map(Object::toString).collect(Collectors.joining(",")) + "):" + _featureRef;
  }
}
