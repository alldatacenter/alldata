package com.linkedin.feathr.common.featurizeddataset;

import com.linkedin.feathr.common.InternalApi;
import com.linkedin.feathr.common.tensor.Primitive;
import com.linkedin.feathr.common.tensor.TensorType;
import org.apache.avro.Schema;

/**
 * Utility methods for working with FeaturizedDatasetMetadata.
 */
@InternalApi
public final class InternalFeaturizedDatasetMetadataUtils {
    private static final String INDICES = "indices";
    private static final String VALUES = "values";
    private static final String FDS_RECORD_NAMESPACE = "com.linkedin.feathr.featurizeddataset";

    private InternalFeaturizedDatasetMetadataUtils() {
    }


    // Also covers ragged and scalar cases.
    static Schema toDenseSchema(TensorType type) {
        Schema schema = toScalarSchema(type.getValueType().getRepresentation());
        int rank = type.getDimensionTypes().size();
        for (int i = 0; i < rank; i++) {
            schema = Schema.createArray(schema);
        }
        return schema;
    }

    private static Schema toScalarSchema(Primitive primitive) {
        return Schema.create(toPrimitiveType(primitive));
    }

    private static Schema.Type toPrimitiveType(Primitive primitive) {
        switch (primitive) {
            case INT:
                return Schema.Type.INT;
            case LONG:
                return Schema.Type.LONG;
            case FLOAT:
                return Schema.Type.FLOAT;
            case DOUBLE:
                return Schema.Type.DOUBLE;
            case BOOLEAN:
                return Schema.Type.BOOLEAN;
            case STRING:
                return Schema.Type.STRING;
            case BYTES:
                return Schema.Type.BYTES;
            default:
                throw new IllegalArgumentException("Unsupported primitive: " + primitive);
        }
    }

    public static String indexFieldName(int i) {
        return INDICES + i;
    }

    public static String valueFieldName() {
        return VALUES;
    }
}
