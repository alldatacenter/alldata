package com.linkedin.feathr.common.tensorbuilder;


import com.linkedin.feathr.common.tensor.TensorType;

public final class UniversalTensorBuilderFactory implements TensorBuilderFactory {
    public static final UniversalTensorBuilderFactory INSTANCE = new UniversalTensorBuilderFactory();

    private UniversalTensorBuilderFactory() {
    }

    @Override
    public TensorBuilder<?> getTensorBuilder(TensorType tensorType) {
        return new UniversalTensorBuilder(tensorType.getColumnTypes());
    }
}
