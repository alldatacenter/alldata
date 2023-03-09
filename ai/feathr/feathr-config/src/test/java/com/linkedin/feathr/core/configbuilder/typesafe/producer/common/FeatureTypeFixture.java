package com.linkedin.feathr.core.configbuilder.typesafe.producer.common;

import com.linkedin.feathr.core.config.producer.common.FeatureTypeConfig;
import com.linkedin.feathr.core.config.producer.definitions.FeatureType;
import java.util.Arrays;


class FeatureTypeFixture {

  static final String simpleTypeConfigStr = "type: {type: VECTOR}";
  static final FeatureTypeConfig expSimpleTypeConfigObj = new FeatureTypeConfig(FeatureType.DENSE_VECTOR);

  static final String simpleTypeWithDocConfigStr = "type: {type: BOOLEAN}";
  static final FeatureTypeConfig expSimpleTypeWithDocConfigObj =
      new FeatureTypeConfig.Builder().setFeatureType(FeatureType.BOOLEAN)
          .build();

  static final String tensorTypeWithUnknownShapeConfigStr = String.join("\n",
      " type: {",
      "   type: \"TENSOR\"",
      "   tensorCategory: \"DENSE\"",
      "   dimensionType: [\"INT\", \"INT\"]",
      "   valType:FLOAT",
      " }");
  static final FeatureTypeConfig expTensorTypeWithUnknownShapeConfig =
      new FeatureTypeConfig.Builder().setFeatureType(FeatureType.DENSE_TENSOR)
          .setDimensionTypes(Arrays.asList("INT", "INT"))
          .setValType("FLOAT")
          .build();

  static final String zeroDimSparseTensorConfigStr = String.join("\n",
      " type: {",
      "   type: \"TENSOR\"",
      "   tensorCategory: \"SPARSE\"",
      "   valType:FLOAT",
      " }");
  static final FeatureTypeConfig expZeroDimSparseTensorConfig =
      new FeatureTypeConfig.Builder().setFeatureType(FeatureType.SPARSE_TENSOR)
          .setValType("FLOAT")
          .build();


  static final String invalidTypeConfigStr = "type: {type: UNKOWN_TYPE, doc: \"this is doc\"}";

  // if tensorCategory is specified, the type should be TENSOR only
  static final String invalidTensorTypeConfigStr = String.join("\n",
      " type: {",
      "   type: \"VECTOR\"",
      "   tensorCategory: \"DENSE\"",
      "   shape: [10]",
      "   dimensionType: [\"INT\"]",
      " }");

  static final String missingTypeConfigStr = "type: {shape:[10], doc: \"this is doc\"}";

  static final String missingValType = String.join("\n",
      " type: {",
      "   type: \"TENSOR\"",
      "   tensorCategory: \"DENSE\"",
      "   shape: [10]",
      "   dimensionType: [\"INT\"]",
      " }");

  static final String shapeAndDimSizeMismatchTypeConfigStr = String.join("\n",
      " type: {",
      "   type: \"TENSOR\"",
      "   tensorCategory: \"DENSE\"",
      "   shape: [10]",
      "   dimensionType: [\"INT\", \"INT\"]",
      "   valType:FLOAT",
      " }");

  static final String nonIntShapeConfigStr = String.join("\n",
      " type: {",
      "   type: \"TENSOR\"",
      "   tensorCategory: \"DENSE\"",
      "   shape: [FLOAT]",
      "   dimensionType: [\"INT\", \"INT\"]",
      "   valType:FLOAT",
      " }");
}
