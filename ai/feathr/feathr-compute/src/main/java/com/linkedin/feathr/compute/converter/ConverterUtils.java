package com.linkedin.feathr.compute.converter;

import com.linkedin.feathr.compute.KeyReference;
import com.linkedin.feathr.compute.KeyReferenceArray;
import com.linkedin.feathr.compute.NodeReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * Common utility methods that can be shared between the different converters.
 */
public class ConverterUtils {
  /**
   * For a transformation or aggregation node, we need to fix the input node reference. In this method, we will create that
   * node reference, which will be updated in the resolver once we have the join config.
   * For now, we will only create a placeholder for the number of keys.
   * @param nodeId
   * @param nKeyParts
   * @return
   */
  public static NodeReference makeNodeReferenceWithSimpleKeyReference(int nodeId, int nKeyParts) {
    return new NodeReference()
        .setId(nodeId)
        .setKeyReference(IntStream.range(0, nKeyParts)
            .mapToObj(i -> new KeyReference().setPosition(i))
            .collect(Collectors.toCollection(KeyReferenceArray::new)));
  }
}
