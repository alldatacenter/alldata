package com.netease.arctic.spark.writer;

import com.netease.arctic.data.ChangeAction;
import com.netease.arctic.spark.SparkInternalRowCastWrapper;
import org.apache.iceberg.io.TaskWriter;
import org.apache.iceberg.io.WriteResult;
import org.apache.spark.sql.catalyst.InternalRow;
import org.apache.spark.sql.connector.write.DataWriter;
import org.apache.spark.sql.connector.write.WriterCommitMessage;
import org.apache.spark.sql.types.StructType;

import java.io.IOException;
import java.util.Arrays;

import static org.apache.spark.sql.types.DataTypes.StringType;

/**
 * unkeyed table upsert data writer
 */
public class SimpleUnkeyedUpsertDataWriter implements DataWriter<InternalRow> {
  final TaskWriter<InternalRow> writer;
  final StructType schema;

  public SimpleUnkeyedUpsertDataWriter(TaskWriter<InternalRow> writer, StructType schemaNum) {
    this.writer = writer;
    this.schema = schemaNum;
  }

  @Override
  public void write(InternalRow record) throws IOException {
    String action = record.get(0, StringType).toString();
    if (action.equals("D")) {
      SparkInternalRowCastWrapper delete = new SparkInternalRowCastWrapper(record, schema, ChangeAction.DELETE, false);
      writer.write(delete);
    } else {
      SparkInternalRowCastWrapper insert = new SparkInternalRowCastWrapper(record, schema, ChangeAction.INSERT);
      writer.write(insert);
    }
  }

  private boolean isDelete(StructType schema) {
    return Arrays.stream(schema.fieldNames()).findFirst().get().equals("_arctic_upsert_op");
  }

  @Override
  public WriterCommitMessage commit() throws IOException {
    WriteResult result = writer.complete();
    return new WriteTaskCommit(result.dataFiles(), result.deleteFiles());
  }

  @Override
  public void abort() throws IOException {
    if (this.writer != null) {
      this.writer.abort();
    }
  }

  @Override
  public void close() throws IOException {
    if (this.writer != null) {
      writer.close();
    }
  }
}