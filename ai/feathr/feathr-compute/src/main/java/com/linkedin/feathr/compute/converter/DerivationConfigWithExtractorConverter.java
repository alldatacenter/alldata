package com.linkedin.feathr.compute.converter;

import com.linkedin.data.template.StringMap;
import com.linkedin.feathr.compute.ComputeGraph;
import com.linkedin.feathr.compute.ComputeGraphBuilder;
import com.linkedin.feathr.compute.External;
import com.linkedin.feathr.compute.FeatureVersion;
import com.linkedin.feathr.compute.KeyReference;
import com.linkedin.feathr.compute.KeyReferenceArray;
import com.linkedin.feathr.compute.NodeReference;
import com.linkedin.feathr.compute.NodeReferenceArray;
import com.linkedin.feathr.compute.Operators;
import com.linkedin.feathr.compute.Transformation;
import com.linkedin.feathr.compute.TransformationFunction;
import com.linkedin.feathr.compute.builder.DefaultValueBuilder;
import com.linkedin.feathr.compute.builder.FeatureVersionBuilder;
import com.linkedin.feathr.compute.builder.FrameFeatureTypeBuilder;
import com.linkedin.feathr.compute.builder.TensorFeatureFormatBuilderFactory;
import com.linkedin.feathr.core.config.producer.derivations.DerivationConfigWithExtractor;
import com.linkedin.feathr.core.config.producer.derivations.KeyedFeature;
import com.linkedin.feathr.core.config.producer.sources.SourceConfig;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Converts a [[DerivationConfigWithExtractor]] object into compute model.
 */
class DerivationConfigWithExtractorConverter implements FeatureDefConfigConverter<DerivationConfigWithExtractor> {
  @Override
  public ComputeGraph convert(String configElementName, DerivationConfigWithExtractor configObject,
      Map<String, SourceConfig> sourceMap) {
    ComputeGraphBuilder graphBuilder = new ComputeGraphBuilder();
    List<String> entityParameters = configObject.getKeys();
    // Create an external feature node with this feature name.
    Map<String, External> externalFeatureNodes = configObject.getInputs().stream()
        .map(KeyedFeature::getFeature)
        .distinct()
        .collect(Collectors.toMap(
            Function.identity(),
            name -> graphBuilder.addNewExternal().setName(name)));


    NodeReferenceArray inputs = configObject.getInputs().stream().map(keyedFeature -> {
      String inputFeatureName = keyedFeature.getFeature();
      List<String> entityArgs = keyedFeature.getKey();

      // The entity parameters will have a subset of the keys and we need to set the key position correctly.
      KeyReferenceArray keyReferenceArray = entityArgs.stream()
          .map(entityParameters::indexOf) // entityParameters should always be small (no 10+ dimensional keys etc)
          .map(position -> new KeyReference().setPosition(position))
          .collect(Collectors.toCollection(KeyReferenceArray::new));
      int nodeId = externalFeatureNodes.get(inputFeatureName).getId();

      return new NodeReference().setId(nodeId).setKeyReference(keyReferenceArray);
    }).collect(Collectors.toCollection(NodeReferenceArray::new));

    TransformationFunction transformationFunction = makeTransformationFunction(configObject.getClassName());
    FeatureVersionBuilder featureVersionBuilder =
        new FeatureVersionBuilder(new TensorFeatureFormatBuilderFactory(),
            DefaultValueBuilder.getInstance(), FrameFeatureTypeBuilder.getInstance());
    FeatureVersion featureVersion = featureVersionBuilder.build(configObject);

    Transformation transformation = graphBuilder.addNewTransformation()
        .setInputs(inputs)
        .setFunction(transformationFunction)
        .setFeatureName(configElementName)
        .setFeatureVersion(featureVersion);
    graphBuilder.addFeatureName(configElementName, transformation.getId());
    return graphBuilder.build();
  }

  private TransformationFunction makeTransformationFunction(String className) {
    Map<String, String> parameterMap = new HashMap<>();
    parameterMap.put("class", className);
    return new TransformationFunction()
        .setOperator(Operators.OPERATOR_ID_DERIVED_JAVA_UDF_FEATURE_EXTRACTOR)
        .setParameters(new StringMap(parameterMap));
  }
}
