package com.netease.arctic.iceberg;

import org.apache.iceberg.Accessor;
import org.apache.iceberg.MetadataColumns;
import org.apache.iceberg.Schema;
import org.apache.iceberg.StructLike;
import org.apache.iceberg.types.TypeUtil;

import java.util.Set;

public class StructForDelete<T extends StructLike> {

  private T structLike;

  private StructProjection pkProjection;

  private final Accessor<StructLike> posAccessor;
  private final Accessor<StructLike> filePathAccessor;
  private final Accessor<StructLike> dataTransactionIdAccessor;

  public StructForDelete(Schema schema, Set<Integer> deleteIds) {
    this.pkProjection = StructProjection.create(schema, TypeUtil.select(schema, deleteIds));
    this.dataTransactionIdAccessor = schema
        .accessorForField(com.netease.arctic.table.MetadataColumns.TRANSACTION_ID_FILED_ID);
    this.posAccessor = schema.accessorForField(MetadataColumns.ROW_POSITION.fieldId());
    this.filePathAccessor = schema.accessorForField(MetadataColumns.FILE_PATH.fieldId());
  }

  public StructForDelete<T> wrap(T structLike) {
    this.structLike = structLike;
    return this;
  }

  public StructLike getPk() {
    return pkProjection.copyWrap(structLike);
  }

  public Long getLsn() {
    return (Long) dataTransactionIdAccessor.get(structLike);
  }

  public Long getPosition() {
    return (Long) posAccessor.get(structLike);
  }

  public String filePath() {
    return (String) filePathAccessor.get(structLike);
  }

  public T recover() {
    return structLike;
  }
}
