package com.linkedin.feathr.compute.converter;

import com.linkedin.feathr.compute.ComputeGraph;
import com.linkedin.feathr.compute.ComputeGraphs;
import com.linkedin.feathr.core.config.producer.sources.SourceConfig;
import java.util.Map;


interface FeatureDefConfigConverter<T> {
  /**
   * It may be necessary for different "subgraphs" to refer to other subgraphs via nodes that are not actually named
   * features. Currently the graph operations e.g. {@link ComputeGraphs#merge} provide useful capabilities to merge
   * subgraphs together but expect them to reference each other based on named features (which are the only things
   * External node knows how to reference). To take advantage of those capabilities for nodes that aren't actually
   * named features, e.g. source nodes, we'll use a prefix to make synthetic feature names for such references.
   */
  String SYNTHETIC_SOURCE_FEATURE_NAME_PREFIX = "__SOURCE__";

  ComputeGraph convert(String configElementName, T configObject, Map<String, SourceConfig> sourceMap);
}
