package com.linkedin.feathr.common.featurizeddataset;

import com.linkedin.feathr.common.tensor.TensorData;
import com.linkedin.feathr.common.tensor.TensorType;

/**
 * A converter for a single feature from Spark value to a TensorData of a specific {@link TensorType}.
 */
public interface FeatureDeserializer {
    TensorData deserialize(Object featureValue);
}
