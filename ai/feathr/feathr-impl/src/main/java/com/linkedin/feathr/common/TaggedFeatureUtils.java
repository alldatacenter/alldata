package com.linkedin.feathr.common;

import java.util.List;
import java.util.stream.Collectors;


public class TaggedFeatureUtils {
  private TaggedFeatureUtils() { }


  public static void checkArgument(Boolean value) {
    if (!value) {
      throw new IllegalArgumentException("Unexpected false");
    }
  }
  public static ErasedEntityTaggedFeature eraseStringTags(TaggedFeatureName input, List<String> keyNames) {
    /*
     * This function should be run rarely (during setup etc).
     * Hence we will do safety checks that aren't free.
     * If it were going to be run very frequently, the keyNames param arguably shouldn't be a list, but a
     * String -> Int trie index.
     */
    checkArgument(!keyNames.isEmpty());
    checkArgument(keyNames.size() == keyNames.stream().distinct().count());

    List<Integer> keyBindingIndexes = input.getKeyTag().stream().map(keyNames::indexOf).collect(Collectors.toList());
    if(keyBindingIndexes.contains(-1)) {
       throw new IllegalArgumentException("input " + input + " contained some key not present in " + keyNames);
    }
    return new ErasedEntityTaggedFeature(keyBindingIndexes, input.getFeatureName());
  }


  /**
   * Use {@link #getTaggedFeatureNameFromStringTags} instead.
   */
  @Deprecated
  public static TaggedFeatureName applyStringTags(ErasedEntityTaggedFeature input, List<String> keyNames) {
    return getTaggedFeatureNameFromStringTags(input, keyNames);
  }

  public static TaggedFeatureName getTaggedFeatureNameFromStringTags(ErasedEntityTaggedFeature input, List<String> keyNames) {
    List<String> stringKeys = input.getBinding()
        .stream()
        // will throw exception if the input doesn't match the given list of key names
        .map(keyNames::get)
        .collect(Collectors.toList());
    return new TaggedFeatureName(stringKeys, input.getFeatureName());
  }
}
