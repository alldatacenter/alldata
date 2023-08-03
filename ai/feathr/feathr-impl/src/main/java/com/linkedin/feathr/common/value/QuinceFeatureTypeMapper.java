package com.linkedin.feathr.common.value;

import com.linkedin.feathr.common.tensor.PrimitiveDimensionType;
import com.linkedin.feathr.common.tensor.TensorCategory;
import com.linkedin.feathr.common.tensor.TensorType;
import com.linkedin.feathr.common.types.DenseVectorFeatureType;
import com.linkedin.feathr.common.types.FeatureType;
import com.linkedin.feathr.common.types.PrimitiveType;
import com.linkedin.feathr.common.types.TensorFeatureType;

import java.util.Collections;

/**
 * A mapper, or translator, that provides a Quince TensorType for a given FeatureType.
 *
 * This class is guaranteed to conform to sister class {@link com.linkedin.feathr.common.value.QuinceFeatureFormatMapper}
 * such that for any FeatureType-TensorType mapping provided by this class, any FeatureValue having that FeatureType
 * would be converted to a TensorData conforming to that TensorType.
 */
public class QuinceFeatureTypeMapper {
    private static final TensorType BOOLEAN_TENSOR_TYPE = new TensorType(PrimitiveType.BOOLEAN, Collections.emptyList());
    private static final TensorType NUMERIC_TENSOR_TYPE = new TensorType(PrimitiveType.FLOAT, Collections.emptyList());
    private static final TensorType STRING_FLOAT_TENSOR_TYPE = new TensorType(PrimitiveType.FLOAT,
            Collections.singletonList(PrimitiveDimensionType.STRING));
    private static final TensorType DENSE_VECTOR_UNKNOWN_SIZE_TYPE =
            denseVectorTypeForSize(DenseVectorFeatureType.UNKNOWN_SIZE);

    public static final QuinceFeatureTypeMapper INSTANCE = new QuinceFeatureTypeMapper();

    private QuinceFeatureTypeMapper() {
    }

    /**
     * @param featureType a FeatureType
     * @return a TensorType that would apply to any TensorData produced by QuinceFeatureFormatMapper for an input
     *         FeatureValue having the provided FeatureType.
     */
    public TensorType fromFeatureType(FeatureType featureType) {
        switch (featureType.getBasicType()) {
            case BOOLEAN:
                return BOOLEAN_TENSOR_TYPE;
            case NUMERIC:
                return NUMERIC_TENSOR_TYPE;
            case CATEGORICAL:
            case CATEGORICAL_SET:
            case TERM_VECTOR:
                return STRING_FLOAT_TENSOR_TYPE;
            case DENSE_VECTOR:
                int size = ((DenseVectorFeatureType) featureType).getSize();
                if (size == DenseVectorFeatureType.UNKNOWN_SIZE) {
                    return DENSE_VECTOR_UNKNOWN_SIZE_TYPE;
                } else {
                    return denseVectorTypeForSize(size);
                }
            case TENSOR:
                return ((TensorFeatureType) featureType).getTensorType();
            default:
                throw new IllegalArgumentException("Unexpected featureType: " + featureType);
        }
    }

    private static TensorType denseVectorTypeForSize(int size) {
        return new TensorType(TensorCategory.DENSE, PrimitiveType.FLOAT,
                Collections.singletonList(PrimitiveDimensionType.INT.withShape(size)), null);
    }
}
