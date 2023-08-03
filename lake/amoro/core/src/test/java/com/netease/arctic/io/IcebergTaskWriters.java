package com.netease.arctic.io;

import com.netease.arctic.table.TableProperties;
import org.apache.iceberg.FileFormat;
import org.apache.iceberg.PartitionKey;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.StructLike;
import org.apache.iceberg.Table;
import org.apache.iceberg.data.GenericAppenderFactory;
import org.apache.iceberg.data.InternalRecordWrapper;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.io.FileAppenderFactory;
import org.apache.iceberg.io.FileIO;
import org.apache.iceberg.io.OutputFileFactory;
import org.apache.iceberg.io.PartitionedWriter;
import org.apache.iceberg.io.TaskWriter;
import org.apache.iceberg.io.UnpartitionedWriter;
import org.apache.iceberg.util.PropertyUtil;

public class IcebergTaskWriters {

  public static TaskWriter<Record> buildFor(Table table) {
    long fileSizeBytes = PropertyUtil.propertyAsLong(table.properties(), TableProperties.WRITE_TARGET_FILE_SIZE_BYTES,
        TableProperties.WRITE_TARGET_FILE_SIZE_BYTES_DEFAULT);
    if (table.spec().isPartitioned()) {
      return new GenericPartitionedWriter(
          table.schema(), table.spec(), FileFormat.PARQUET,
          new GenericAppenderFactory(table.schema(), table.spec()),
          OutputFileFactory.builderFor(table, 0, 0)
              .build(),
          table.io(), fileSizeBytes
      );
    } else {
      return new UnpartitionedWriter<>(
          table.spec(), FileFormat.PARQUET,
          new GenericAppenderFactory(table.schema(), table.spec()),
          OutputFileFactory.builderFor(table, 0, 0)
              .build(),
          table.io(), fileSizeBytes
      );
    }
  }


  public static class GenericPartitionedWriter extends PartitionedWriter<Record> {

    final PartitionKey partitionKey;
    final InternalRecordWrapper wrapper;

    protected GenericPartitionedWriter(
        Schema schema, PartitionSpec spec, FileFormat format,
        FileAppenderFactory<Record> appenderFactory,
        OutputFileFactory fileFactory,
        FileIO io, long targetFileSize) {
      super(spec, format, appenderFactory, fileFactory, io, targetFileSize);
      this.partitionKey = new PartitionKey(spec, schema);
      this.wrapper = new InternalRecordWrapper(schema.asStruct());
    }

    @Override
    protected PartitionKey partition(Record row) {
      StructLike structLike = wrapper.wrap(row);
      partitionKey.partition(structLike);
      return partitionKey.copy();
    }
  }
}
