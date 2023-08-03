package com.linkedin.feathr.common.featurizeddataset;

/**
 * Internal utilities for handling FDS metadata in Avro schema.
 */
public final class SchemaMetadataUtils {
    // Property names for the custom properties used for storing FDS metadata on avro.
    public static final String FDS_METADATA_PROP_NAME = "featurizedDatasetMetadata";

    private static final String FDS_RECORD_NAMESPACE = "com.linkedin.feathr.featurizeddataset";
    private static final String FDS_RECORD_NAME = "FeaturizedDataset";

    private SchemaMetadataUtils() {

    }

    public static String indexFieldName(int i) {
        return InternalFeaturizedDatasetMetadataUtils.indexFieldName(i);
    }

    public static String valueFieldName() {
        return InternalFeaturizedDatasetMetadataUtils.valueFieldName();
    }
}
