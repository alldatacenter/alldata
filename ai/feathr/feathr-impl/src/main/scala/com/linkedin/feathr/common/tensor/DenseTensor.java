package com.linkedin.feathr.common.tensor;

import com.linkedin.feathr.common.tensor.TensorData;

import java.util.List;

/**
 * A dense subkind of tensor data.
 * Guarantees that values in all columns except the last fill the n-dimensional rectangle in lexicographic order, e.g. for a shape [2, 3]:
 * (0, 0, value)
 * (0, 1, value)
 * (0, 2, value)
 * (1, 0, value)
 * (1, 1, value)
 * (1, 2, value)
 *
 * Note that this implies that only integral types (INT and LONG) can be used as dimensions in dense tensors.
 *
 * Can be backed by byte buffers, java.util collections, Avro, Spark, etc.
 */
public abstract class DenseTensor implements TensorData {
    /**
     * @return the list of values in their unraveled order.
     * This in combination with the shape is enough to restore the entire tensor, as the dimensions can be calculated from the shape.
     */
    public abstract List<?> asList();
}
