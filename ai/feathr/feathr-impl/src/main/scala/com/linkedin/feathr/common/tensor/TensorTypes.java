package com.linkedin.feathr.common.tensor;

import com.google.common.collect.Lists;
import com.linkedin.feathr.common.types.PrimitiveType;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility functions for working with {@link TensorType}
 */
public final class TensorTypes {
    private static final Pattern CAPTURING_DIMENSION_TYPE =
            Pattern.compile("\\[(?<base>INT|LONG|STRING)(?:\\((?<shape>\\d+)\\))?]");
    private static final Pattern TENSOR_TYPE = Pattern.compile("TENSOR<(?<category>SPARSE|DENSE|RAGGED)>"
            + "(?<dimensions>" + "(?:\\[" + "(?:INT|LONG|STRING|BYTES)(?:\\(\\d+\\))?" + "])*)"
            + ":(?<value>INT|LONG|FLOAT|DOUBLE|STRING|BOOLEAN|BYTES)");

    private TensorTypes() { }

    /**
     * Creates a TensorType from a string representation of the type.
     * Intended to simplify creation of automated tests.
     * It is NOT robust (e.g., the only whitespace allowed is the single mandatory space after comma).
     *
     * Some examples:
     * TENSOR<SPARSE>[STRING][LONG]:DOUBLE
     * TENSOR<DENSE>[INT(3)]:FLOAT
     * TENSOR<DENSE>:BOOLEAN
     */
    public static TensorType parseTensorType(String syntax) {
        Matcher matcher = TENSOR_TYPE.matcher(syntax);
        boolean matches = matcher.matches();
        if (matches) {
            TensorCategory category = TensorCategory.valueOf(matcher.group("category"));
            String dimensions = matcher.group("dimensions");
            String value = matcher.group("value");
            return new TensorType(category, new PrimitiveType(Primitive.valueOf(value)), parseDimensions(dimensions), null);
        } else {
            throw new IllegalArgumentException("Not a valid tensor type: " + syntax);
        }
    }

    /**
     * Creates a synthetic TensorType from the information available on TensorData.
     * This is a stop-gap solution for integrations where consumer requires TensorType but the producer does not have it.
     * Please use sparingly.
     *
     * NOTE that the static shape is not recoverable, so all dimensions in the TensorType will have the unknown shape.
     * RAGGED tensors are not supported.
     *
     * @param sparse if the type should be sparse
     * @param representables primitive types of the tensor's columns
     * @return a TensorType corresponding to the inputs
     */
    public static TensorType fromRepresentables(boolean sparse, Representable[] representables) {
        PrimitiveType valueType = new PrimitiveType(representables[representables.length - 1].getRepresentation());
        List<DimensionType> dimensionTypes = Lists.newArrayListWithCapacity(representables.length - 1);
        for (int i = 0; i < representables.length - 1; i++) {
            dimensionTypes.add(new PrimitiveDimensionType(representables[i].getRepresentation()));
        }
        return new TensorType(sparse ? TensorCategory.SPARSE : TensorCategory.DENSE, valueType, dimensionTypes);
    }

    /**
     * Creates a synthetic TensorType from a TensorData.
     * This is a stop-gap solution for integrations where consumer requires TensorType but the producer does not have it.
     * Please use sparingly.
     *
     * NOTE that the static shape is not recoverable
     * (for dense tensors the static shape can have some of the dimensions unknown,
     * so it is not recoverable from the dynamic shape;
     * sparse tensors do not have even the dynamic shape),
     * so all dimensions in the TensorType will have the unknown shape.
     * RAGGED tensors are not supported.
     *
     * @param tensorData an instance of tensor from which to derive type information
     * @return a TensorType corresponding to the input
     */
    public static TensorType fromTensorData(TensorData tensorData) {
        return fromRepresentables(!(tensorData instanceof DenseTensor), tensorData.getTypes());
    }

    private static List<DimensionType> parseDimensions(String syntax) {
        List<DimensionType> dimensions = new ArrayList<>();
        if (syntax != null) {
            Matcher matcher = CAPTURING_DIMENSION_TYPE.matcher(syntax);
            while (matcher.find()) {
                PrimitiveDimensionType dimensionType = new PrimitiveDimensionType(Primitive.valueOf(matcher.group("base")));
                String shape = matcher.group("shape");
                PrimitiveDimensionType dimension =
                        shape == null ? dimensionType : dimensionType.withShape(Integer.parseInt(shape));
                dimensions.add(dimension);
            }
        }
        return dimensions;
    }
}
