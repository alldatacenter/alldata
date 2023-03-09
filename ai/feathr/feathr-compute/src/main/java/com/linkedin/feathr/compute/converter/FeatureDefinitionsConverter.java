package com.linkedin.feathr.compute.converter;

import com.linkedin.feathr.compute.ComputeGraph;
import com.linkedin.feathr.compute.ComputeGraphs;
import com.linkedin.feathr.core.config.producer.FeatureDefConfig;
import com.linkedin.feathr.core.config.producer.anchors.AnchorConfigWithExtractor;
import com.linkedin.feathr.core.config.producer.anchors.AnchorConfigWithKey;
import com.linkedin.feathr.core.config.producer.anchors.AnchorConfigWithKeyExtractor;
import com.linkedin.feathr.core.config.producer.anchors.AnchorConfigWithOnlyMvel;
import com.linkedin.feathr.core.config.producer.derivations.DerivationConfigWithExpr;
import com.linkedin.feathr.core.config.producer.derivations.DerivationConfigWithExtractor;
import com.linkedin.feathr.core.config.producer.derivations.SequentialJoinConfig;
import com.linkedin.feathr.core.config.producer.derivations.SimpleDerivationConfig;
import com.linkedin.feathr.core.config.producer.sources.SourceConfig;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Converts a {@link FeatureDefConfig} (parsed HOCON feature definitions) into Feathr Compute Model represented as
 * {@link ComputeGraph}.
 */
public class FeatureDefinitionsConverter {
  Map<String, SourceConfig> sourcesMap = new HashMap<>();

  private final Map<Class<?>, FeatureDefConfigConverter<?>> _configClassConverterMap = new HashMap<>();

  {
    registerConverter(AnchorConfigWithExtractor.class, new AnchorConfigConverter());
    registerConverter(AnchorConfigWithKey.class, new AnchorConfigConverter());
    registerConverter(AnchorConfigWithKeyExtractor.class, new AnchorConfigConverter());
    registerConverter(AnchorConfigWithOnlyMvel.class, new AnchorConfigConverter());
    registerConverter(DerivationConfigWithExpr.class, new DerivationConfigWithExprConverter());
    registerConverter(DerivationConfigWithExtractor.class, new DerivationConfigWithExtractorConverter());
    registerConverter(SimpleDerivationConfig.class, new SimpleDerivationConfigConverter());
    registerConverter(SequentialJoinConfig.class, new SequentialJoinConfigConverter());
  }

  public ComputeGraph convert(FeatureDefConfig featureDefinitions) throws CloneNotSupportedException {
    List<ComputeGraph> graphParts = new ArrayList<>();

    featureDefinitions.getSourcesConfig().map(sourcesConfig -> sourcesConfig.getSources().entrySet())
        .orElse(Collections.emptySet())
        .forEach(entry -> sourcesMap.put(entry.getKey(), entry.getValue()));

    featureDefinitions.getAnchorsConfig().map(anchorsConfig -> anchorsConfig.getAnchors().entrySet())
        .orElse(Collections.emptySet()).stream()
        .map(entry -> convert(entry.getKey(), entry.getValue(), sourcesMap))
        .forEach(graphParts::add);

    featureDefinitions.getDerivationsConfig().map(derivationsConfig -> derivationsConfig.getDerivations().entrySet())
        .orElse(Collections.emptySet()).stream()
        .map(entry -> convert(entry.getKey(), entry.getValue(), sourcesMap))
        .forEach(graphParts::add);

    return ComputeGraphs.removeRedundancies(ComputeGraphs.merge(graphParts));
  }

  /**
   * Register a converter for a particular kind of config object class. The purpose of this private method (which we
   * will only use during construction time) is to prevent accidental mismatches. Via the type parameter we guarantee
   * that the converter should always match the corresponding class.
   */
  private <T> void registerConverter(Class<T> clazz, FeatureDefConfigConverter<?> converter) {
    _configClassConverterMap.put(clazz, converter);
  }

  @SuppressWarnings("unchecked")
  private <T> FeatureDefConfigConverter<T> getConverter(T configObject) {
    return (FeatureDefConfigConverter<T>) _configClassConverterMap.get(configObject.getClass());
  }

  private <T> ComputeGraph convert(String name, T config, Map<String, SourceConfig> sourcesMap) {
    FeatureDefConfigConverter<T> converter = getConverter(config);
    if (converter != null) {
      return converter.convert(name, config, sourcesMap);
    } else {
      throw new RuntimeException("Unhandled config class: " + name + ": " + config);
    }
  }
}
