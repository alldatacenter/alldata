package com.linkedin.feathr.common.tensorbuilder;

import com.linkedin.feathr.common.tensor.TensorType;

public interface TensorBuilderFactory {
    TensorBuilder<?> getTensorBuilder(TensorType tensorType);

    default BulkTensorBuilder getBulkTensorBuilder(TensorType tensorType) {
        throw new UnsupportedOperationException("getBulkTensorBuilder is not supported");
    }

    default BulkTensorBuilder getBulkTensorBuilder(TensorType tensorType, long[] shape) {
        throw new UnsupportedOperationException("getBulkTensorBuilder with shape is not supported");
    }
}
