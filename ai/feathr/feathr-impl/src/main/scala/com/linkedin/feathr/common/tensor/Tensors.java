package com.linkedin.feathr.common.tensor;
import com.linkedin.feathr.common.tensor.scalar.ScalarTensor;
import com.linkedin.feathr.common.tensorbuilder.BulkTensorBuilder;
import com.linkedin.feathr.common.tensorbuilder.DenseTensorBuilderFactory;
import com.linkedin.feathr.common.tensorbuilder.TensorBuilder;
import com.linkedin.feathr.common.tensorbuilder.UniversalTensorBuilderFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Utility methods for converting various collections to tensors.
 * Lists and arrays are represented as dense tensors.
 * Maps are represented as sparse tensors.
 * Sets are represented as sparse tensors with 1f as values.
 */
public final class Tensors {
    private Tensors() {
    }

    private static BulkTensorBuilder getBulkBuilder(TensorType type, int size) {
        BulkTensorBuilder builder = DenseTensorBuilderFactory.INSTANCE.getBulkTensorBuilder(type);
        if (!builder.hasVariableCardinality() && builder.getStaticCardinality() != size) {
            throw new IllegalArgumentException(
                    "The number of values " + size + " is not equal to the size of the type " + builder.getStaticCardinality()
                            + ".");
        }
        if (size % builder.getStaticCardinality() != 0) {
            throw new IllegalArgumentException(
                    "The number of values " + size + " is not a multiple of the static size of the type "
                            + builder.getStaticCardinality() + ".");
        }
        return builder;
    }

    /**
     * Convert a scalar value to a tensor.
     * @param type the type of the result.
     * @param scalar the scalar value.
     * @return a tensor with 0 dimensions and encapsulating the value.
     */
    public static TensorData asScalarTensor(TensorType type, Object scalar) {
        if (type.getDimensionTypes().size() > 0) {
            throw new IllegalArgumentException("Scalar tensors cannot have dimensions.");
        }
        return ScalarTensor.wrap(scalar, type.getValueType().getRepresentation());
    }

    /**
     * Convert an array value to a dense tensor.
     * @param type the type of the result.
     * @param floats the array value.
     * @return a tensor with 1 dimension (corresponding to the array's index) and encapsulating the values.
     */
    public static TensorData asDenseTensor(TensorType type, float[] floats) {
        BulkTensorBuilder builder = getBulkBuilder(type, floats.length);
        return builder.build(floats);
    }

    /**
     * Convert an array value to a dense tensor.
     * @param type the type of the result.
     * @param ints the array value.
     * @return a tensor with 1 dimension (corresponding to the array's index) and encapsulating the values.
     */
    public static TensorData asDenseTensor(TensorType type, int[] ints) {
        BulkTensorBuilder builder = getBulkBuilder(type, ints.length);
        return builder.build(ints);
    }

    /**
     * Convert an array value to a dense tensor.
     * @param type the type of the result.
     * @param longs the array value.
     * @return a tensor with 1 dimension (corresponding to the array's index) and encapsulating the values.
     */
    public static TensorData asDenseTensor(TensorType type, long[] longs) {
        BulkTensorBuilder builder = getBulkBuilder(type, longs.length);
        return builder.build(longs);
    }

    /**
     * Convert an array value to a dense tensor.
     * @param type the type of the result.
     * @param doubles the array value.
     * @return a tensor with 1 dimension (corresponding to the array's index) and encapsulating the values.
     */
    public static TensorData asDenseTensor(TensorType type, double[] doubles) {
        BulkTensorBuilder builder = getBulkBuilder(type, doubles.length);
        return builder.build(doubles);
    }

    /**
     * Convert a list value to a dense tensor.
     * @param type the type of the result.
     * @param values the list value.
     * @return a tensor with 1 dimension (corresponding to the array's index) and encapsulating the values.
     */
    public static TensorData asDenseTensor(TensorType type, List<?> values) {
        BulkTensorBuilder builder = getBulkBuilder(type, values.size());
        return builder.build(values);
    }

    /**
     * Convert a set value to a sparse tensor.
     * @param type the type of the result.
     * @param dimensionValues the values for the single dimension.
     * @return a tensor with 1 dimension (corresponding to the set's values) and the tensor values set to 1.0.
     */
    public static TensorData asSparseTensor(TensorType type, Set<?> dimensionValues) {
        if (type.getDimensionTypes().size() != 1) {
            throw new IllegalArgumentException("Only one-dimensional tensors can represent sets.");
        }
        TensorBuilder<?> builder = UniversalTensorBuilderFactory.INSTANCE.getTensorBuilder(type);
        builder.start(dimensionValues.size());
        for (Object value : dimensionValues) {
            builder.setValue(0, value);
            builder.setValue(1, 1);
            builder.append();
        }
        return builder.build();
    }

    /**
     * Convert a map value to a sparse tensor.
     * @param type the type of the result.
     * @param values the values for the single dimension and corresponding tensor values.
     * @return a tensor with 1 dimension (corresponding to the map's values) and the tensor values set to the map's values.
     */
    public static TensorData asSparseTensor(TensorType type, Map<?, ?> values) {
        if (type.getDimensionTypes().size() != 1) {
            throw new IllegalArgumentException("Only one-dimensional tensors can represent maps.");
        }
        TensorBuilder<?> builder = UniversalTensorBuilderFactory.INSTANCE.getTensorBuilder(type);
        builder.start(values.size());
        for (Map.Entry entry : values.entrySet()) {
            builder.setValue(0, entry.getKey());
            builder.setValue(1, entry.getValue());
            builder.append();
        }
        return builder.build();
    }
}
