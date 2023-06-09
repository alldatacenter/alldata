package com.netease.arctic.spark.io;

import com.netease.arctic.data.ChangeAction;
import com.netease.arctic.io.ArcticFileIO;
import com.netease.arctic.io.writer.ChangeTaskWriter;
import com.netease.arctic.io.writer.OutputFileFactory;
import com.netease.arctic.spark.SparkInternalRowCastWrapper;
import com.netease.arctic.spark.SparkInternalRowWrapper;
import com.netease.arctic.table.PrimaryKeySpec;
import org.apache.iceberg.FileFormat;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.StructLike;
import org.apache.iceberg.io.FileAppenderFactory;
import org.apache.iceberg.spark.SparkSchemaUtil;
import org.apache.spark.sql.catalyst.InternalRow;

/**
 * change task writer
 */
public class ArcticSparkChangeTaskWriter extends ChangeTaskWriter<InternalRow> {
  private final Schema schema;

  protected ArcticSparkChangeTaskWriter(
      FileFormat format,
      FileAppenderFactory<InternalRow> appenderFactory,
      OutputFileFactory outputFileFactory,
      ArcticFileIO io,
      long targetFileSize,
      long mask,
      Schema schema,
      PartitionSpec spec,
      PrimaryKeySpec primaryKeySpec,
      boolean orderedWriter
  ) {
    super(format, appenderFactory, outputFileFactory, io,
        targetFileSize, mask, schema, spec, primaryKeySpec, orderedWriter);
    this.schema = schema;
  }

  @Override
  protected StructLike asStructLike(InternalRow data) {
    return new SparkInternalRowWrapper(SparkSchemaUtil.convert(schema)).wrap(data);
  }

  @Override
  protected InternalRow appendMetaColumns(InternalRow data, Long fileOffset) {
    SparkInternalRowCastWrapper row = (SparkInternalRowCastWrapper) data;
    return row.setFileOffset(fileOffset);
  }

  @Override
  protected ChangeAction action(InternalRow data) {
    SparkInternalRowCastWrapper row = (SparkInternalRowCastWrapper) data;
    return row.getChangeAction();
  }

}
