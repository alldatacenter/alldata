package com.linkedin.feathr.common.tensorbuilder;

import com.linkedin.feathr.common.tensor.TensorData;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * An interface for building Tensors from arrays and collections of values
 * This is targeted towards building DenseTensor. The input is usually just values
 * and dimensions are implicit and can be determined by looking at TensorType
 */
public interface BulkTensorBuilder {
    /**
     * The full count of the tensor values, ignoring a possible unknown dimension.
     */
    int getStaticCardinality();

    /**
     * If the actual cardinality can be a multiple of the static cardinality, as opposed to exactly equal.
     */
    boolean hasVariableCardinality();

    TensorData build(float[] floats);
    TensorData build(int[] ints);
    TensorData build(long[] longs);
    TensorData build(double[] doubles);
    TensorData build(boolean[] booleans);
    TensorData build(List<?> values);
    TensorData build(ByteBuffer values);
}
