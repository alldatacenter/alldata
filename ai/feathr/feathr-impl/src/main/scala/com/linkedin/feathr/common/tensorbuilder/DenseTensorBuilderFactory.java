package com.linkedin.feathr.common.tensorbuilder;

import com.linkedin.feathr.common.tensor.TensorType;
import com.linkedin.feathr.common.TensorUtils;

/**
 */
public class DenseTensorBuilderFactory implements TensorBuilderFactory {
    public static final DenseTensorBuilderFactory INSTANCE = new DenseTensorBuilderFactory();

    @Override
    public TensorBuilder<?> getTensorBuilder(TensorType tensorType) {
        return getBulkTensorBuilder(tensorType, TensorUtils.getShape(tensorType));
    }

    @Override
    public BulkTensorBuilder getBulkTensorBuilder(TensorType tensorType) {
        return getBulkTensorBuilder(tensorType, TensorUtils.getShape(tensorType));
    }

    @Override
    public DenseTensorBuilder getBulkTensorBuilder(TensorType tensorType, long[] shape) {
        return new DenseTensorBuilder(tensorType.getColumnTypes(), shape);
    }

}
