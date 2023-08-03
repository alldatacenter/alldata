package com.linkedin.feathr.compute.converter;

import com.linkedin.data.template.StringMap;
import com.linkedin.feathr.compute.ComputeGraph;
import com.linkedin.feathr.compute.ComputeGraphBuilder;
import com.linkedin.feathr.compute.External;
import com.linkedin.feathr.compute.FeatureVersion;
import com.linkedin.feathr.compute.KeyReference;
import com.linkedin.feathr.compute.KeyReferenceArray;
import com.linkedin.feathr.compute.Lookup;
import com.linkedin.feathr.compute.MvelExpression;
import com.linkedin.feathr.compute.NodeReference;
import com.linkedin.feathr.compute.NodeReferenceArray;
import com.linkedin.feathr.compute.Operators;
import com.linkedin.feathr.compute.Transformation;
import com.linkedin.feathr.compute.TransformationFunction;
import com.linkedin.feathr.compute.builder.DefaultValueBuilder;
import com.linkedin.feathr.compute.builder.FeatureVersionBuilder;
import com.linkedin.feathr.compute.builder.FrameFeatureTypeBuilder;
import com.linkedin.feathr.compute.builder.TensorFeatureFormatBuilderFactory;
import com.linkedin.feathr.core.config.producer.derivations.SequentialJoinConfig;
import com.linkedin.feathr.core.config.producer.sources.SourceConfig;
import com.linkedin.feathr.core.utils.MvelInputsResolver;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Converts a [[SequentialJoinConfig]] object into compute model.
 */
class SequentialJoinConfigConverter implements FeatureDefConfigConverter<SequentialJoinConfig> {

  @Override
  public ComputeGraph convert(String configElementName, SequentialJoinConfig configObject,
      Map<String, SourceConfig> sourceMap) {
    ComputeGraphBuilder graphBuilder = new ComputeGraphBuilder();
    String baseFeatureName = configObject.getBase().getFeature();
    List<String> baseFeatureKeys = configObject.getBase().getKey();
    List<String> entityParameters = configObject.getKeys();
    External baseExternalFeatureNode = graphBuilder.addNewExternal().setName(baseFeatureName);
    KeyReferenceArray keyReferenceArray = baseFeatureKeys.stream()
        .map(entityParameters::indexOf)
        .map(position -> new KeyReference().setPosition(position))
        .collect(Collectors.toCollection(KeyReferenceArray::new));
    int nodeId = baseExternalFeatureNode.getId();
    NodeReference baseNodeReference = new NodeReference().setId(nodeId).setKeyReference(keyReferenceArray);
    Lookup.LookupKey lookupKey;
    String featureNameAlias;
    if (configObject.getBase().getOutputKeys().isPresent()) {
      featureNameAlias = configObject.getBase().getOutputKeys().get().get(0);
    } else {
      featureNameAlias = "__SequentialJoinDefaultOutputKey__0";
    }
    // Here we want to check if there is an expansion key function and add a transformation node on top of the
    // base external feature node in that case. Note we only support MVEL in this case in the HOCON config.
    if (configObject.getBase().getTransformation().isPresent()) {
      // We only support mvel expression here.
      MvelExpression baseFeatureTransformationExpression = new MvelExpression().setMvel(configObject.getBase().getTransformation().get());
      // Should be just the base feature.
      List<String> inputFeatureNames = MvelInputsResolver.getInstance().getInputFeatures(baseFeatureTransformationExpression.getMvel());
      TransformationFunction transformationFunction = makeTransformationFunction(baseFeatureTransformationExpression,
          inputFeatureNames, Operators.OPERATOR_ID_LOOKUP_MVEL);
      // Note here we specifically do not set the base feature name or add a feature definition because this is not a named feature,
      // it is a intermediate feature that will only be used for sequential join so a name will be generated for it.
      Transformation transformationNode = graphBuilder.addNewTransformation()
          .setInputs(new NodeReferenceArray(Collections.singleton(baseNodeReference)))
          .setFunction(transformationFunction)
          .setFeatureVersion(new FeatureVersion())
          .setFeatureName(featureNameAlias);
      int transformationNodeId = transformationNode.getId();

      NodeReference baseTransformationNodeReference = new NodeReference().setId(transformationNodeId).setKeyReference(keyReferenceArray);
      lookupKey = new Lookup.LookupKey().create(baseTransformationNodeReference);
    } else {
      lookupKey = new Lookup.LookupKey().create(baseNodeReference);
    }

    // Create lookup key array based on key reference and base node reference.
    List<String> expansionKeysArray = configObject.getExpansion().getKey();
    Lookup.LookupKeyArray lookupKeyArray = expansionKeysArray.stream()
        .map(entityParameters::indexOf)
        .map(position -> position == -1 ? lookupKey
            : entityParameters.get(position).equals(featureNameAlias) ? lookupKey
                : new Lookup.LookupKey().create(new KeyReference().setPosition(position))
        )
        .collect(Collectors.toCollection(Lookup.LookupKeyArray::new));

    // create an external node without key reference for expansion.
    String expansionFeatureName = configObject.getExpansion().getFeature();
    External expansionExternalFeatureNode = graphBuilder.addNewExternal().setName(expansionFeatureName);

    // get aggregation function
    String aggType = configObject.getAggregation();
    FeatureVersionBuilder featureVersionBuilder =
        new FeatureVersionBuilder(new TensorFeatureFormatBuilderFactory(),
            DefaultValueBuilder.getInstance(), FrameFeatureTypeBuilder.getInstance());
    FeatureVersion featureVersion = featureVersionBuilder.build(configObject);
    Lookup lookup = graphBuilder.addNewLookup().setLookupNode(expansionExternalFeatureNode.getId())
        .setLookupKey(lookupKeyArray).setAggregation(aggType).setFeatureName(configElementName).setFeatureVersion(featureVersion);
    graphBuilder.addFeatureName(configElementName, lookup.getId());
    return graphBuilder.build();
  }

  // This one will operate on a tuple of inputs (the Feature Derivation case). In this case, the transform function
  // will consume a tuple. A list of names will inform the transformer about how to apply the elements in the tuple
  // (based on their order) to the variable names used in the MVEL expression itself (e.g. feature1, feature2).
  private TransformationFunction makeTransformationFunction(
      MvelExpression input, List<String> parameterNames, String operator) {
    // Treat derivation mvel derived features differently?
    TransformationFunction tf = makeTransformationFunction(input, operator);
    tf.getParameters().put("parameterNames", String.join(",", parameterNames));
    return tf;
  }

  private TransformationFunction makeTransformationFunction(
      MvelExpression input, String operator) {
    return new TransformationFunction()
        .setOperator(operator)
        .setParameters(new StringMap(Collections.singletonMap("expression", input.getMvel())));
  }
}
