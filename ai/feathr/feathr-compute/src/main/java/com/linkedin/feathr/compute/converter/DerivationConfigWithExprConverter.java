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
import com.linkedin.feathr.core.config.producer.derivations.DerivationConfigWithExpr;
import com.linkedin.feathr.core.config.producer.derivations.KeyedFeature;
import com.linkedin.feathr.core.config.producer.sources.SourceConfig;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * Converts a [[DerivationConfigWithExpr]] object into compute model.
 */
class DerivationConfigWithExprConverter implements FeatureDefConfigConverter<DerivationConfigWithExpr> {
  @Override
  public ComputeGraph convert(String configElementName, DerivationConfigWithExpr configObject,
      Map<String, SourceConfig> sourceMap) {
    ComputeGraphBuilder graphBuilder = new ComputeGraphBuilder();
    List<String> entityParameters = configObject.getKeys();
    Map<String, External> externalFeatureNodes = new HashMap<>();
    Set<String> uniqueValues = new HashSet<>();
    for (Map.Entry<String, KeyedFeature> input : configObject.getInputs().entrySet()) {
      String featureName = input.getValue().getFeature();
      if (uniqueValues.add(featureName)) {
        if (externalFeatureNodes.put(featureName, graphBuilder.addNewExternal().setName(featureName)) != null) {
          throw new IllegalStateException("Duplicate key found in " + configElementName);
        }
      }
    }

    NodeReferenceArray inputs = configObject.getInputs().entrySet().stream().map(mapEntry -> {
      String inputFeatureName = mapEntry.getValue().getFeature();
      List<String> entityArgs = mapEntry.getValue().getKey();

      KeyReferenceArray keyReferenceArray = entityArgs.stream()
          .map(entityParameters::indexOf)
          .map(position -> new KeyReference().setPosition(position))
          .collect(Collectors.toCollection(KeyReferenceArray::new));
      int inputNodeId = externalFeatureNodes.get(inputFeatureName).getId();

      /**
       * If there is a featureAlias, add a feature alias transformation node on top of the external node which
       * represents the input feature.
       * Something like:-
       * derivedFeature: {
       *  key: x
       *  inputs: {
       *       arg1: { key: viewerId, feature: AA }
       *       arg2: { key: vieweeId, feature: BB }
       *     }
       *  definition: arg1 + arg2
       * }
       *
       * We will create a new transformation node for arg1 and arg2.
       */

      if (!Objects.equals(mapEntry.getKey(), "")) {
        ArrayList regularKeyReferenceArray = new ArrayList<KeyReference>();
        for (int i = 0; i < entityArgs.size(); i++) {
          regularKeyReferenceArray.add(new KeyReference().setPosition(i));
        }
        KeyReferenceArray simpleKeyReferenceArray = new KeyReferenceArray(regularKeyReferenceArray);
        NodeReference inputNodeReference =
            new NodeReference().setId(inputNodeId).setKeyReference(simpleKeyReferenceArray);

        TransformationFunction featureAliasFunction = new TransformationFunction().setOperator(Operators.OPERATOR_FEATURE_ALIAS);
        Transformation transformation = graphBuilder.addNewTransformation()
            .setInputs(new NodeReferenceArray(Collections.singleton(inputNodeReference)))
            .setFunction(featureAliasFunction)
            .setFeatureVersion((new FeatureVersion()))
            .setFeatureName(mapEntry.getKey());
        inputNodeId = transformation.getId();
      }
      return new NodeReference().setId(inputNodeId).setKeyReference(keyReferenceArray);
    }).collect(Collectors.toCollection(NodeReferenceArray::new));

    List<String> inputParameterNames = new ArrayList<>(configObject.getInputs().keySet());
    TransformationFunction transformationFunction = new TransformationFunction().setOperator(Operators.OPERATOR_ID_EXTRACT_FROM_TUPLE)
        .setParameters(new StringMap(Collections.singletonMap("expression", configObject.getTypedDefinition().getExpr())));;
    transformationFunction.getParameters().put("parameterNames", String.join(",", inputParameterNames));
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
}
