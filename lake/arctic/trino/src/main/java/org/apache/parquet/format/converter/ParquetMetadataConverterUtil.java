package org.apache.parquet.format.converter;

import org.apache.parquet.format.ConvertedType;
import org.apache.parquet.format.LogicalType;
import org.apache.parquet.format.SchemaElement;
import org.apache.parquet.schema.LogicalTypeAnnotation;

/**
 * Copy from hive-apache package, because include hive-apache will cause class conflict
 */
public final class ParquetMetadataConverterUtil {
  private ParquetMetadataConverterUtil() {

  }

  public static LogicalTypeAnnotation getLogicalTypeAnnotation(
      ParquetMetadataConverter parquetMetadataConverter,
      ConvertedType convertedType,
      SchemaElement schemaElement) {
    return parquetMetadataConverter.getLogicalTypeAnnotation(convertedType, schemaElement);
  }

  public static LogicalTypeAnnotation getLogicalTypeAnnotation(
      ParquetMetadataConverter parquetMetadataConverter,
      LogicalType logicalType) {
    return parquetMetadataConverter.getLogicalTypeAnnotation(logicalType);
  }

  public static LogicalType convertToLogicalType(
      ParquetMetadataConverter parquetMetadataConverter,
      LogicalTypeAnnotation logicalTypeAnnotation) {
    return parquetMetadataConverter.convertToLogicalType(logicalTypeAnnotation);
  }
}
