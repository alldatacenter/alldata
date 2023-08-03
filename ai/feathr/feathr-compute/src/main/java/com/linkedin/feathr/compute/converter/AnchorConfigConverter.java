package com.linkedin.feathr.compute.converter;

import com.linkedin.data.template.RecordTemplate;
import com.linkedin.data.template.StringMap;
import com.linkedin.feathr.compute.AggregationFunction;
import com.linkedin.feathr.compute.ComputeGraph;
import com.linkedin.feathr.compute.ComputeGraphBuilder;
import com.linkedin.feathr.compute.DataSource;
import com.linkedin.feathr.compute.DataSourceType;
import com.linkedin.feathr.compute.FeatureVersion;
import com.linkedin.feathr.compute.KeyExpressionType;
import com.linkedin.feathr.compute.MvelExpression;
import com.linkedin.feathr.compute.NodeReference;
import com.linkedin.feathr.compute.NodeReferenceArray;
import com.linkedin.feathr.compute.OfflineKeyFunction;
import com.linkedin.feathr.compute.Operators;
import com.linkedin.feathr.compute.PegasusUtils;
import com.linkedin.feathr.compute.SlidingWindowFeature;
import com.linkedin.feathr.compute.SqlExpression;
import com.linkedin.feathr.compute.TimestampCol;
import com.linkedin.feathr.compute.TransformationFunction;
import com.linkedin.feathr.compute.Unit;
import com.linkedin.feathr.compute.UserDefinedFunction;
import com.linkedin.feathr.compute.Window;
import com.linkedin.feathr.compute.builder.AnchorKeyFunctionBuilder;
import com.linkedin.feathr.compute.builder.DefaultValueBuilder;
import com.linkedin.feathr.compute.builder.FeatureVersionBuilder;
import com.linkedin.feathr.compute.builder.FrameFeatureTypeBuilder;
import com.linkedin.feathr.compute.builder.SlidingWindowAggregationBuilder;
import com.linkedin.feathr.compute.builder.TensorFeatureFormatBuilderFactory;
import com.linkedin.feathr.compute.builder.TransformationFunctionExpressionBuilder;
import com.linkedin.feathr.core.config.producer.anchors.AnchorConfig;
import com.linkedin.feathr.core.config.producer.sources.HdfsConfig;
import com.linkedin.feathr.core.config.producer.sources.HdfsConfigWithRegularData;
import com.linkedin.feathr.core.config.producer.sources.HdfsConfigWithSlidingWindow;
import com.linkedin.feathr.core.config.producer.sources.PassThroughConfig;
import com.linkedin.feathr.core.config.producer.sources.SourceConfig;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.linkedin.feathr.compute.converter.ConverterUtils.*;


/**
 * Converts a hocon parsed config model [[AnchorConfig]] into the compute model. This class is resposibile for converting
 * anchored and swa feature models into the compute model.
 */
class AnchorConfigConverter implements FeatureDefConfigConverter<AnchorConfig> {
  private final String _passthrough = "passthrough";
  private final String _anchor = "anchor";
  private final String _swa = "_swa";
  private final String _window_unit = "window_unit";
  private final String _lateral_view_expression_ = "lateral_view_expression_";
  private final String _lateral_view_table_alias_ = "lateral_view_table_alias_";
  private final String _group_by_expression = "group_by_expression";
  private final String _filter_expression = "filter_expression";
  private final String _max_number_groups = "max_number_groups";
  private final String _expression = "expression";
  private final String _class = "class";
  private final String _userParam_ = "userParam_";
  @Override
  public ComputeGraph convert(String configElementName, AnchorConfig configObject,
      Map<String, SourceConfig> sourceMap) {
    ComputeGraphBuilder graphBuilder = new ComputeGraphBuilder();

    String keyExpression;
    KeyExpressionType keyExpressionType;

    // Builds a keyFunction. We need this as currently the config can be in different formats, ie - AnchorConfigWithExtractor,
    // AnchorConfigWithMvel, AnchorConfigWithKeyExtractor, AnchorConfigWithKey. The below step consoliates into one single entity.
    OfflineKeyFunction.KeyFunction offlineKeyFunction = new AnchorKeyFunctionBuilder(configObject).build();
    if (offlineKeyFunction.isMvelExpression()) {
      keyExpression = offlineKeyFunction.getMvelExpression().getMvel();
      keyExpressionType = KeyExpressionType.MVEL;
    } else if (offlineKeyFunction.isSqlExpression()) {
      keyExpression = offlineKeyFunction.getSqlExpression().getSql();
      keyExpressionType = KeyExpressionType.SQL;
    } else if (offlineKeyFunction.isUserDefinedFunction()) {
      keyExpression = offlineKeyFunction.getUserDefinedFunction().getClazz();
      keyExpressionType = KeyExpressionType.UDF;
    } else {
      throw new RuntimeException("Unknown key type found in " + configElementName);
    }

    String featureType = getTypeOfFeature(sourceMap, configObject);

    DataSource dataSource = buildDataSource(graphBuilder, configObject, keyExpressionType, keyExpression, sourceMap, featureType);

    // Attach the keys correctly to the datasource.
    NodeReference referenceToSource = makeNodeReferenceWithSimpleKeyReference(dataSource.getId(), 1);

    configObject.getFeatures().forEach((featureName, featureConfig) -> {
      TransformationFunctionExpressionBuilder transformationFunctionExpressionBuilder =
          new TransformationFunctionExpressionBuilder(SlidingWindowAggregationBuilder.getInstance());
      // Build a transformation expression by parsing through the different types of transformation expressions.
      Object expression = transformationFunctionExpressionBuilder.buildTransformationExpression(featureConfig, configObject);

      RecordTemplate operatorReference = getOperator(expression, featureType);

      RecordTemplate operatorNode;

      // Build the [[FeatureVersion]] object.
      FeatureVersionBuilder featureVersionBuilder =
          new FeatureVersionBuilder(new TensorFeatureFormatBuilderFactory(),
              DefaultValueBuilder.getInstance(), FrameFeatureTypeBuilder.getInstance());
      FeatureVersion featureVersion = featureVersionBuilder.build(featureConfig);

      // Construct the agg/transformation node
      if (operatorReference instanceof AggregationFunction) {
        operatorNode = graphBuilder.addNewAggregation()
            .setFunction((AggregationFunction) operatorReference)
            .setInput(referenceToSource)
            .setFeatureName(featureName)
            .setFeatureVersion(featureVersion);
      } else if (operatorReference instanceof TransformationFunction) {
        operatorNode = graphBuilder.addNewTransformation()
            .setFunction((TransformationFunction) operatorReference)
            .setInputs(new NodeReferenceArray(Collections.singleton(referenceToSource)))
            .setFeatureName(featureName)
            .setFeatureVersion(featureVersion);
      } else {
        throw new RuntimeException("Unexpected operator reference type " + operatorReference.getClass() + " - data: "
            + operatorReference);
      }
      graphBuilder.addFeatureName(featureName, PegasusUtils.getNodeId(operatorNode));
    });
    return graphBuilder.build();
  }

  // Get the appropriate transformation operator expression.
  private RecordTemplate getOperator(Object expression, String finalFeatureType) {
    String operator = null;
    RecordTemplate operatorReference;
    if (expression instanceof MvelExpression) {
      if (Objects.equals(finalFeatureType, _anchor)) {
        operator = Operators.OPERATOR_ID_ANCHOR_MVEL;
      } else if (Objects.equals(finalFeatureType, _passthrough)) {
        operator = Operators.OPERATOR_ID_PASSTHROUGH_MVEL;
      }
      operatorReference = makeTransformationFunction(((MvelExpression) expression), operator);
    } else if (expression instanceof SlidingWindowFeature) {
      operatorReference = makeAggregationFunction((SlidingWindowFeature) expression);
    } else if (expression instanceof SqlExpression) {
      if (Objects.equals(finalFeatureType, _anchor)) {
        operator = Operators.OPERATOR_ID_ANCHOR_SPARK_SQL_FEATURE_EXTRACTOR;
      } else if (Objects.equals(finalFeatureType, _passthrough)) {
        operator = Operators.OPERATOR_ID_PASSTHROUGH_SPARK_SQL_FEATURE_EXTRACTOR;
      }
      operatorReference = makeTransformationFunction((SqlExpression) expression, operator);
    } else if (expression instanceof UserDefinedFunction) {
      if (Objects.equals(finalFeatureType, _anchor)) {
        operator = Operators.OPERATOR_ID_ANCHOR_JAVA_UDF_FEATURE_EXTRACTOR;
      } else if (Objects.equals(finalFeatureType, _passthrough)) {
        operator = Operators.OPERATOR_ID_PASSTHROUGH_JAVA_UDF_FEATURE_EXTRACTOR;
      }
      operatorReference = makeTransformationFunction((UserDefinedFunction) expression, operator);
    } else {
      throw new RuntimeException("No known way to handle " + expression);
    }
    return operatorReference;
  }

  // Get the feature type correctly to attach the right transformation function operator. The featureType depends on the config source class.
  private String getTypeOfFeature(Map<String, SourceConfig> sourceMap, AnchorConfig configObject) {
    String featureType;
    if (sourceMap.containsKey(configObject.getSource()) && sourceMap.get(configObject.getSource()).getClass() == PassThroughConfig.class) {
      featureType = _passthrough;
    } else if (sourceMap.containsKey(configObject.getSource()) && sourceMap.get(configObject.getSource()).getClass() == HdfsConfigWithSlidingWindow.class) {
      String swa = _swa;
      featureType = swa;
    } else {
      if (sourceMap.containsKey(configObject.getSource())) {
        HdfsConfigWithRegularData sourceConfig = (HdfsConfigWithRegularData) sourceMap.get(configObject.getSource());
        if (sourceConfig.getTimePartitionPattern().isPresent()) {
          featureType = _swa;
        } else {
          featureType = _anchor;
        }
      } else {
        featureType = _anchor;
      }
    }
    return featureType;
  }

  /**
   * Builds and adds a datasource object into the graphbuilder using the configObject.
   * @param graphBuilder  The [[GraphBuilder]] object to which the newly created datasource object should get appended to.
   * @param configObject  The [[AnchorConfig]] object
   * @param keyExpressionType The key expression type, ie - mvel, sql or udf
   * @param keyExpression The actual key expression
   * @param sourceMap Map of source name to source Config
   * @param featureType
   * @return  The created datasource object
   */
  private DataSource buildDataSource(ComputeGraphBuilder graphBuilder, AnchorConfig configObject, KeyExpressionType keyExpressionType,
      String keyExpression, Map<String, SourceConfig> sourceMap, String featureType) {
    DataSource dataSourceNode = null;
    String sourcePath;
    // If the sourceMap contains the sourceName, we know that it is a compound source and we need to read the source information from the
    // sourceMap.
    if (sourceMap.containsKey(configObject.getSource())) {
      if (Objects.equals(featureType, _anchor)) { // simple anchor
        HdfsConfigWithRegularData sourceConfig = (HdfsConfigWithRegularData) sourceMap.get(configObject.getSource());
        sourcePath = sourceConfig.getPath();
        dataSourceNode = graphBuilder.addNewDataSource().setExternalSourceRef(sourcePath)
            .setSourceType(DataSourceType.UPDATE).setKeyExpression(keyExpression)
            .setKeyExpressionType(keyExpressionType);
      } else if (Objects.equals(featureType, _swa)) { // SWA source
        HdfsConfig sourceConfig = (HdfsConfig) sourceMap.get(configObject.getSource());
        sourcePath = sourceConfig.getPath();
        dataSourceNode = graphBuilder.addNewDataSource().setExternalSourceRef(sourcePath)
            .setSourceType(DataSourceType.EVENT).setKeyExpression(keyExpression)
            .setKeyExpressionType(keyExpressionType);

        String filePartitionFormat = null;
        if (sourceConfig.getTimePartitionPattern().isPresent()) {
          filePartitionFormat = sourceConfig.getTimePartitionPattern().get();
        }

        TimestampCol timestampCol = null;
        if (sourceConfig.getClass() == HdfsConfigWithSlidingWindow.class) {
          HdfsConfigWithSlidingWindow swaConfig = (HdfsConfigWithSlidingWindow) sourceConfig;
          if (swaConfig.getSwaConfig().getTimeWindowParams() != null) {
            String timestampColFormat = swaConfig.getSwaConfig().getTimeWindowParams().getTimestampFormat();
            String timestampColExpr = swaConfig.getSwaConfig().getTimeWindowParams().getTimestampField();
            timestampCol = new TimestampCol().setExpression(timestampColExpr).setFormat(timestampColFormat);
          }
        }

        if (filePartitionFormat != null && timestampCol != null) {
          dataSourceNode.setSourceType(DataSourceType.EVENT).setFilePartitionFormat(filePartitionFormat).setTimestampColumnInfo(timestampCol);
        } else if (timestampCol != null) {
          dataSourceNode.setSourceType(DataSourceType.EVENT).setTimestampColumnInfo(timestampCol);
        } else {
          dataSourceNode.setSourceType(DataSourceType.EVENT).setFilePartitionFormat(filePartitionFormat);
        }
      } else if (Objects.equals(featureType, _passthrough)) {
        dataSourceNode = graphBuilder.addNewDataSource()
            .setSourceType(DataSourceType.CONTEXT).setKeyExpression(keyExpression)
            .setKeyExpressionType(keyExpressionType);
      }
    } else { // source is not an object, so it should be a path.
      sourcePath = configObject.getSource();
      dataSourceNode = graphBuilder.addNewDataSource().setExternalSourceRef(sourcePath)
          .setSourceType(DataSourceType.UPDATE).setKeyExpression(keyExpression)
          .setKeyExpressionType(keyExpressionType);
    }
    return dataSourceNode;
  }

  // Builds the aggregation function
  private AggregationFunction makeAggregationFunction(SlidingWindowFeature input) {
    Map<String, String> parameterMap = new HashMap<>();
    String target_column = "target_column";
    parameterMap.put(target_column, input.getTargetColumn().getSqlExpression().getSql());
    String aggregation_type = "aggregation_type";
    parameterMap.put(aggregation_type, input.getAggregationType().name());
    Duration window = convert(input.getWindow());
    String window_size = "window_size";
    parameterMap.put(window_size, window.toString());
    parameterMap.put(_window_unit, input.getWindow().getUnit().name());
    // lateral view expression capability should be rethought
    for (int i = 0; i < input.getLateralViews().size(); i++) {
      parameterMap.put(_lateral_view_expression_ + i, input.getLateralViews().get(i)
          .getTableGeneratingFunction().getSqlExpression().getSql());
      parameterMap.put(_lateral_view_table_alias_ + i, input.getLateralViews().get(i)
          .getVirtualTableAlias());
    }
    if (input.hasFilter()) {
      parameterMap.put(_filter_expression, Objects.requireNonNull(input.getFilter()).getSqlExpression().getSql());
    }
    if (input.hasGroupBy()) {
      parameterMap.put(_group_by_expression, Objects.requireNonNull(input.getGroupBy()).getSqlExpression().getSql());
    }
    if (input.hasLimit()) {
      parameterMap.put(_max_number_groups, Objects.requireNonNull(input.getLimit()).toString());
    }
    return new AggregationFunction()
        .setOperator(Operators.OPERATOR_ID_SLIDING_WINDOW_AGGREGATION)
        .setParameters(new StringMap(parameterMap));
  }

  // Build the transformation function given an mvel expression
  private TransformationFunction makeTransformationFunction(MvelExpression input, String operator) {
    return new TransformationFunction()
        .setOperator(operator)
        .setParameters(new StringMap(Collections.singletonMap(_expression, input.getMvel())));
  }

  // Build the transformation function given a sql expression
  private TransformationFunction makeTransformationFunction(SqlExpression input, String operator) {
    return new TransformationFunction().setOperator(operator)
        .setParameters(new StringMap(Collections.singletonMap(_expression, input.getSql())));
  }

  // Build the transformation function given a java udf expression
  private TransformationFunction makeTransformationFunction(UserDefinedFunction input, String operator) {
    Map<String, String> parameterMap = new HashMap<>();
    parameterMap.put(_class, input.getClazz());
    input.getParameters().forEach((userParamName, userParamValue) -> {
      parameterMap.put(_userParam_ + userParamName, userParamValue);
    });
    return new TransformationFunction()
        .setOperator(operator)
        .setParameters(new StringMap(parameterMap));
  }

  private Duration convert(Window frWindow) {
    int size = frWindow.getSize();
    if (frWindow.getUnit() == Unit.DAY) {
      return Duration.ofDays(size);
    } else if (frWindow.getUnit() == Unit.HOUR) {
      return Duration.ofHours(size);
    } else if (frWindow.getUnit() == Unit.MINUTE) {
      return Duration.ofMinutes(size);
    } else if (frWindow.getUnit() == Unit.SECOND) {
      return Duration.ofSeconds(size);
    } else {
      throw new RuntimeException("'We only support day, hour, minute, and second time units for window field. The correct example \" +\n"
          + "            \"can be '1d'(1 day) or '2h'(2 hour) or '3m'(3 minute) or '4s'(4 second) ");
    }
  }
}
