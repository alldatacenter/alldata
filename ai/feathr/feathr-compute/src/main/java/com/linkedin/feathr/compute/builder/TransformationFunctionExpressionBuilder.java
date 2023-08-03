package com.linkedin.feathr.compute.builder;

import com.linkedin.data.template.StringMap;
import com.linkedin.feathr.compute.MvelExpression;
import com.linkedin.feathr.compute.SqlExpression;
import com.linkedin.feathr.compute.UserDefinedFunction;
import com.linkedin.feathr.core.config.TimeWindowAggregationType;
import com.linkedin.feathr.core.config.producer.ExprType;
import com.linkedin.feathr.core.config.producer.anchors.AnchorConfig;
import com.linkedin.feathr.core.config.producer.anchors.AnchorConfigWithExtractor;
import com.linkedin.feathr.core.config.producer.anchors.ExpressionBasedFeatureConfig;
import com.linkedin.feathr.core.config.producer.anchors.ExtractorBasedFeatureConfig;
import com.linkedin.feathr.core.config.producer.anchors.FeatureConfig;
import com.linkedin.feathr.core.config.producer.anchors.TimeWindowFeatureConfig;
import javax.annotation.Nonnull;


/**
 * This class is used to build expression in Transform functions for features.
 */

public class TransformationFunctionExpressionBuilder {
  private final SlidingWindowAggregationBuilder _slidingWindowAggregationBuilder;

  public TransformationFunctionExpressionBuilder(@Nonnull SlidingWindowAggregationBuilder slidingWindowAggregationBuilder) {
    _slidingWindowAggregationBuilder = slidingWindowAggregationBuilder;
  }

  /**
   * Build transform function expression for anchored features.
   *
   * Transform function can be defined in anchor config via extractor field. In this case, we will build
   * UserDefined function.
   *
   * Or it can be defined in the feature config. Feature config can have following formats:
   *
   * 1. Simple feature. In this case, the expression will be treated as a Mvel transform function and an MvelExpression will be returned.
   *
   * 2. Complex feature with SparkSql transform function. In this case, will build SparksqlExpression
   *
   * 3. Complex feature with Mvel transform function. In this case, will build MvelExpression
   *
   * 4. Time Windowed feature. For now, we will build UnspecifieldFunction
   *
   */
  public Object buildTransformationExpression(FeatureConfig featureConfig, AnchorConfig anchorConfig) {
    if (anchorConfig instanceof AnchorConfigWithExtractor) {
      AnchorConfigWithExtractor anchorConfigWithExtractor = (AnchorConfigWithExtractor) anchorConfig;
      UserDefinedFunction userDefinedFunction = new UserDefinedFunction();
      userDefinedFunction.setClazz(anchorConfigWithExtractor.getExtractor());
      userDefinedFunction.setParameters(new StringMap(featureConfig.getParameters()));
      return userDefinedFunction;
    }
    if (featureConfig instanceof ExpressionBasedFeatureConfig) {
      ExpressionBasedFeatureConfig expressionBasedFeatureConfig = (ExpressionBasedFeatureConfig) featureConfig;
      if (expressionBasedFeatureConfig.getExprType() == ExprType.MVEL) {
        MvelExpression mvelExpression = new MvelExpression();
        mvelExpression.setMvel(expressionBasedFeatureConfig.getFeatureExpr());
        return mvelExpression;
      } else if (expressionBasedFeatureConfig.getExprType() == ExprType.SQL) {
        SqlExpression sparkSqlExpression = new SqlExpression();
        sparkSqlExpression.setSql(expressionBasedFeatureConfig.getFeatureExpr());
        return sparkSqlExpression;
      } else {
        throw new IllegalArgumentException(String.format("Expression type %s is unsupported in feature config %s",
            expressionBasedFeatureConfig.getExprType(), featureConfig));
      }
    } else if (featureConfig instanceof ExtractorBasedFeatureConfig) {
      ExtractorBasedFeatureConfig extractorBasedFeatureConfig = (ExtractorBasedFeatureConfig) featureConfig;
      MvelExpression mvelExpression = new MvelExpression();
      mvelExpression.setMvel(extractorBasedFeatureConfig.getFeatureName());
      return mvelExpression;
    } else if (featureConfig instanceof TimeWindowFeatureConfig) {
      TimeWindowFeatureConfig timeWindowFeatureConfig = (TimeWindowFeatureConfig) featureConfig;
      TimeWindowAggregationType timeWindowAggregationType = ((TimeWindowFeatureConfig) featureConfig).getAggregation();
      if (SlidingWindowAggregationBuilder.isSlidingWindowAggregationType(timeWindowAggregationType)) {
        return _slidingWindowAggregationBuilder.build(timeWindowFeatureConfig, anchorConfig);
      } else {
        throw new IllegalArgumentException("Unsupported time window aggregation type " + timeWindowAggregationType);
      }
    } else {
      throw new IllegalArgumentException(String.format("Feature config type %s is not supported in feature "
          + "config %s", featureConfig.getClass(), featureConfig));
    }
  }
}

