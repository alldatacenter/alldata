package com.linkedin.feathr.core.configbuilder.typesafe.producer.common;

import com.google.common.base.Preconditions;
import com.linkedin.feathr.core.config.producer.common.FeatureTypeConfig;
import com.linkedin.feathr.core.config.producer.definitions.FeatureType;
import com.linkedin.feathr.core.config.producer.definitions.TensorCategory;
import com.linkedin.feathr.core.configbuilder.ConfigBuilderException;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueType;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.linkedin.feathr.core.config.producer.common.FeatureTypeConfig.*;
import static com.linkedin.feathr.core.config.producer.derivations.DerivationConfig.TYPE;


/**
 * Builds a {@link FeatureTypeConfig} object
 */
public class FeatureTypeConfigBuilder {
  private static final Set<FeatureType> SUPPORTED_TENSOR_TYPES =
      new HashSet<>(Arrays.asList(FeatureType.DENSE_TENSOR, FeatureType.SPARSE_TENSOR, FeatureType.RAGGED_TENSOR));

  private FeatureTypeConfigBuilder() {
  }

  public static FeatureTypeConfig build(Config config) {
    FeatureTypeConfig featureTypeConfig = null;
    if (config.hasPath(TYPE)) {
      ConfigValue configValue = config.getValue(TYPE);
      ConfigValueType configValueType = configValue.valueType();

      switch (configValueType) {
        case STRING:
          featureTypeConfig = new FeatureTypeConfig(FeatureType.valueOf(config.getString(TYPE)));
          break;
        case OBJECT:
          featureTypeConfig = FeatureTypeConfigBuilder.buildComplexTypeConfig(config.getConfig(TYPE));
          break;
        default:
          throw new ConfigBuilderException(
              "Expected " + TYPE + "  config value type should be String or Object, got " + configValueType);
      }
    }
    return featureTypeConfig;
  }

  private static FeatureTypeConfig buildComplexTypeConfig(Config config) {
    Preconditions.checkArgument(config.hasPath(TYPE), "The config should contain \"type\" child node.");
    FeatureType featureType = FeatureType.valueOf(config.getString(TYPE));

    // If config has `tensorCategory` field, the TENSOR featureType will be refined with tensorCategory:
    // e.g. DENSE tensorCategory + TENSOR featureType -> DENSE_TENSOR featureType.
    // The same for SPARSE and RAGGED category.
    // If the featureType is not TENSOR, will throw exception.
    if (config.hasPath(TENSOR_CATEGORY)) {
      if (featureType != FeatureType.TENSOR) {
        throw new ConfigBuilderException("tensorCategory field is specified but the feature type is not TENSOR: \n"
            + config.root().render());
      }
      TensorCategory tensorCategory = TensorCategory.valueOf(config.getString(TENSOR_CATEGORY));
      switch (tensorCategory) {
        case DENSE:
          featureType = FeatureType.DENSE_TENSOR;
          break;
        case SPARSE:
          featureType = FeatureType.SPARSE_TENSOR;
          break;
        case RAGGED:
          featureType = FeatureType.RAGGED_TENSOR;
          break;
        default:
          throw new ConfigBuilderException("The feature type tensorCategory is not supported: " + tensorCategory);
      }
    }

    List<Integer> shapes = null;
    if (config.hasPath(SHAPE)) {
      shapes = config.getIntList(SHAPE);
    }

    List<String> dimensionTypes = null;
    if (config.hasPath(DIMENSION_TYPE)) {
      dimensionTypes = config.getStringList(DIMENSION_TYPE);
    }

    if (shapes != null && dimensionTypes != null && shapes.size() != dimensionTypes.size()) {
      throw new RuntimeException(
          "Sizes of dimensionType and shape should match but got: " + dimensionTypes + " and " + shapes);
    }

    String valType = null;
    if (config.hasPath(VAL_TYPE)) {
      valType = config.getString(VAL_TYPE);
    } else {
      // For tensor, valType is required.
      if (SUPPORTED_TENSOR_TYPES.contains(featureType)) {
        throw new RuntimeException("valType field is required for tensor types but is missing in the config: " + config);
      }
    }

    return new FeatureTypeConfig.Builder().setFeatureType(featureType)
        .setShapes(shapes)
        .setDimensionTypes(dimensionTypes)
        .setValType(valType)
        .build();
  }
}
