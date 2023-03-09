package com.linkedin.feathr.common.featurizeddataset;

import com.linkedin.feathr.common.tensor.Representable;
import com.linkedin.feathr.common.tensor.TensorCategory;
import com.linkedin.feathr.common.tensor.TensorData;
import com.linkedin.feathr.common.tensor.TensorType;
import com.linkedin.feathr.common.tensor.scalar.ScalarTensor;
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema;
import scala.collection.Seq;

/**
 * A converter from FDS Spark objects to TensorData.
 * Instead of creating a new memory copy, a thin wrapper (view) around the Spark structure is created.
 * This reduces GC pressure, but some operations (e.g., cardinality or isEmpty) are more expensive than usual.
 *
 *
 * For working with individual feature values, use {@link #getFeatureDeserializer}.
 *
 * NOTE that this class directly operates with scala.collection.Seq values coming from Spark DataFrame.
 * An alternative would be converting those to java.util.List, but given the performance requirements of this class, that was not chosen.
 */
public final class SparkDeserializerFactory {
    private SparkDeserializerFactory() {
    }

    /**
     * Creates a converter from Spark feature value to a TensorData.
     *
     * @param tensorType the type of the converted values
     * @return a converter from Spark feature value to a TensorData of tensorType
     */
    public static FeatureDeserializer getFeatureDeserializer(TensorType tensorType) {
        TensorCategory tensorCategory = tensorType.getTensorCategory();
        FeatureDeserializer converter;
        // DenseDeserializer also supports TensorCategory.RAGGED, but this functionality is disabled here until there is matching serialization support.
        if (tensorCategory == TensorCategory.DENSE) {
            if (tensorType.getDimensionTypes().isEmpty()) {
                converter = new ScalarDeserializer();
            } else {
                converter = new DenseDeserializer(tensorType.getColumnTypes(), tensorCategory == TensorCategory.DENSE);
            }
        } else if (tensorCategory == TensorCategory.SPARSE) {
            converter = new SparseDeserializer(tensorType.getColumnTypes());
        } else {
            throw new IllegalArgumentException("Unsupported tensor category " + tensorCategory);
        }
        return converter;
    }

    // Only works for dense scalars; sparse scalars are handled by SparseSerializer.
    private static class ScalarDeserializer implements FeatureDeserializer {
        @Override
        public TensorData deserialize(Object featureValue) {
            if (featureValue == null) {
                return null;
            }
            return ScalarTensor.wrap(featureValue);
        }
    }

    private static class DenseDeserializer implements FeatureDeserializer {
        private final Representable[] _columnTypes;
        private final boolean _regular;

        DenseDeserializer(Representable[] columnTypes, boolean regular) {
            _columnTypes = columnTypes;
            _regular = regular;
        }

        @Override
        public TensorData deserialize(Object featureValue) {
            if (featureValue == null) {
                return null;
            }
            return new FDSDenseTensorWrapper(_columnTypes, _regular, (Seq<Object>) featureValue);
        }
    }

    private static class SparseDeserializer implements FeatureDeserializer {
        private final Representable[] _columnTypes;

        SparseDeserializer(Representable[] columnTypes) {
            _columnTypes = columnTypes;
        }

        @Override
        public TensorData deserialize(Object featureValue) {
            if (featureValue == null) {
                return null;
            }
            return new FDSSparseTensorWrapper(_columnTypes, (GenericRowWithSchema) featureValue);
        }
    }
}

