package com.linkedin.feathr.compute.converter;

import com.linkedin.data.template.StringMap;
import com.linkedin.feathr.compute.ComputeGraph;
import com.linkedin.feathr.compute.ComputeGraphBuilder;
import com.linkedin.feathr.compute.External;
import com.linkedin.feathr.compute.FeatureVersion;
import com.linkedin.feathr.compute.NodeReferenceArray;
import com.linkedin.feathr.compute.Operators;
import com.linkedin.feathr.compute.SqlUtil;
import com.linkedin.feathr.compute.Transformation;
import com.linkedin.feathr.compute.TransformationFunction;
import com.linkedin.feathr.compute.builder.DefaultValueBuilder;
import com.linkedin.feathr.compute.builder.FeatureVersionBuilder;
import com.linkedin.feathr.compute.builder.FrameFeatureTypeBuilder;
import com.linkedin.feathr.compute.builder.TensorFeatureFormatBuilderFactory;
import com.linkedin.feathr.core.config.producer.ExprType;
import com.linkedin.feathr.core.config.producer.derivations.SimpleDerivationConfig;
import com.linkedin.feathr.core.config.producer.sources.SourceConfig;
import com.linkedin.feathr.core.utils.MvelInputsResolver;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.linkedin.feathr.compute.converter.ConverterUtils.*;

/**
 * Converts a [[SimpleDerivationConfig]] object into compute model.
 */
class SimpleDerivationConfigConverter implements FeatureDefConfigConverter<SimpleDerivationConfig> {
  @Override
  public ComputeGraph convert(String configElementName, SimpleDerivationConfig configObject,
      Map<String, SourceConfig> sourceMap) {
    List<String> inputFeatureNames = null;
    TransformationFunction transformationFunction = null;
    ComputeGraphBuilder graphBuilder = new ComputeGraphBuilder();
    if (configObject.getFeatureTypedExpr().getExprType().equals(ExprType.MVEL)) {
      String mvel = configObject.getFeatureTypedExpr().getExpr();
      inputFeatureNames = MvelInputsResolver.getInstance().getInputFeatures(mvel);
      transformationFunction = new TransformationFunction()
          .setOperator(Operators.OPERATOR_ID_DERIVED_MVEL)
          .setParameters(new StringMap(Collections.singletonMap("expression", mvel)));
      transformationFunction.getParameters().put("parameterNames", String.join(",", inputFeatureNames));
    } else if (configObject.getFeatureTypedExpr().getExprType().equals(ExprType.SQL)) {
      String sql = configObject.getFeatureTypedExpr().getExpr();
      inputFeatureNames = SqlUtil.getInputsFromSqlExpression(sql);
      transformationFunction = new TransformationFunction()
          .setOperator(Operators.OPERATOR_ID_DERIVED_SPARK_SQL_FEATURE_EXTRACTOR)
          .setParameters(new StringMap(Collections.singletonMap("expression", sql)));
      transformationFunction.getParameters().put("parameterNames", String.join(",", inputFeatureNames));
    }

    Map<String, External> externalFeatureNodes = inputFeatureNames.stream()
        .collect(Collectors.toMap(Function.identity(),
            name -> graphBuilder.addNewExternal().setName(name)));
    NodeReferenceArray nodeReferences = inputFeatureNames.stream().map(inputFeatureName -> {
          int featureDependencyNodeId = externalFeatureNodes.get(inputFeatureName).getId();
          // WE HAVE NO WAY OF KNOWING how many keys the feature has. Perhaps this ambiguity should be specifically
          // allowed for in the compute model. We assume the number of key part is always 1 as the simple derivation
          // does not have a key field.
          return makeNodeReferenceWithSimpleKeyReference(featureDependencyNodeId, 1);
        }
    ).collect(Collectors.toCollection(NodeReferenceArray::new));

    FeatureVersionBuilder featureVersionBuilder =
        new FeatureVersionBuilder(new TensorFeatureFormatBuilderFactory(),
            DefaultValueBuilder.getInstance(), FrameFeatureTypeBuilder.getInstance());
    FeatureVersion featureVersion = featureVersionBuilder.build(configObject);
    Transformation transformation = graphBuilder.addNewTransformation()
        .setInputs(nodeReferences)
        .setFunction(transformationFunction)
        .setFeatureName(configElementName)
        .setFeatureVersion(featureVersion);
    graphBuilder.addFeatureName(configElementName, transformation.getId());

    return graphBuilder.build();
  }
}
