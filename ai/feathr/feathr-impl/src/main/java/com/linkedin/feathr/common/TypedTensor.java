package com.linkedin.feathr.common;

import com.linkedin.feathr.common.tensor.TensorData;
import com.linkedin.feathr.common.tensor.TensorType;

/**
 * TypedTensor builds on top of @link{TensorType} and @{TensorData} and can be generated from tensor
 * operators that change the shape.
 */
public interface TypedTensor {
    TensorData getData();

    TensorType getType();

    TypedTensor slice(Object val);

    TypedTensor subSlice(Object val);

    String toDebugString();

    String toDebugString(int maxStringLenLimit);
}
