package com.linkedin.feathr.common;


import com.linkedin.feathr.common.tensor.PrimitiveDimensionType;
import com.linkedin.feathr.common.tensor.TensorCategory;
import com.linkedin.feathr.common.tensor.TensorType;
import com.linkedin.feathr.common.types.PrimitiveType;

import java.util.Collections;
import java.util.Optional;


/**
 * Mappings from Feathr FeatureTypes to their "auto-tensorized" tensor type. In other words, when the feature owner does
 * not specify a tensor type, Feathr will pick one based on the feature's FeatureType.
 *
 * These mappings define how Feathr will represent features having types NUMERIC, CATEGORICAL, etc., as Quince tensors.
 */
@Experimental
public class AutoTensorizableTypes {
  private AutoTensorizableTypes() { }
  static final TensorType SCALAR_BOOLEAN_TENSOR_TYPE = new TensorType(TensorCategory.DENSE, PrimitiveType.BOOLEAN,
      Collections.emptyList());
  static final TensorType SCALAR_FLOAT_TENSOR_TYPE = new TensorType(TensorCategory.DENSE, PrimitiveType.FLOAT,
      Collections.emptyList());
  static final TensorType TERM_VECTOR_TENSOR_TYPE = new TensorType(PrimitiveType.FLOAT,
      Collections.singletonList(PrimitiveDimensionType.STRING));
  static final TensorType DENSE_1D_FLOAT_TENSOR_TYPE = new TensorType(TensorCategory.DENSE, PrimitiveType.FLOAT,
      Collections.singletonList(PrimitiveDimensionType.INT));

  /**
   * Get the default tensor type for the given Feathr FeatureType, if there is one.
   * @param featureType the Feathr featureType
   * @return the automatically assigned Quince TensorType for the given Feathr FeatureType, if one exists
   */
  public static Optional<TensorType> getDefaultTensorType(FeatureTypes featureType) {
    switch (featureType) {
      case BOOLEAN:
        return Optional.of(SCALAR_BOOLEAN_TENSOR_TYPE);
      case NUMERIC:
        return Optional.of(SCALAR_FLOAT_TENSOR_TYPE);
      case CATEGORICAL:
      case CATEGORICAL_SET:
      case TERM_VECTOR:
      case UNSPECIFIED:
        return Optional.of(TERM_VECTOR_TENSOR_TYPE);
      case DENSE_VECTOR:
        return Optional.of(DENSE_1D_FLOAT_TENSOR_TYPE);
      case TENSOR: // For "TENSOR" we will need more information provided by feature owner, in order to provide a TensorType.
      default:
        return Optional.empty();
    }
  }
}
