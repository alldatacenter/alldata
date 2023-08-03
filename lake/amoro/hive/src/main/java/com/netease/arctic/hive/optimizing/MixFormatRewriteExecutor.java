package com.netease.arctic.hive.optimizing;

import com.netease.arctic.data.PrimaryKeyedFile;
import com.netease.arctic.hive.io.writer.AdaptHiveGenericTaskWriterBuilder;
import com.netease.arctic.io.writer.ArcticTreeNodePosDeleteWriter;
import com.netease.arctic.optimizing.AbstractRewriteFilesExecutor;
import com.netease.arctic.optimizing.OptimizingDataReader;
import com.netease.arctic.optimizing.RewriteFilesInput;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.WriteOperationKind;
import com.netease.arctic.utils.map.StructLikeCollections;
import org.apache.commons.lang3.StringUtils;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.deletes.PositionDelete;
import org.apache.iceberg.io.DataWriteResult;
import org.apache.iceberg.io.DeleteWriteResult;
import org.apache.iceberg.io.FileAppenderFactory;
import org.apache.iceberg.io.FileWriter;
import org.apache.iceberg.io.TaskWriter;
import org.apache.iceberg.io.WriteResult;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class MixFormatRewriteExecutor extends AbstractRewriteFilesExecutor {

  private String outputDir;

  public MixFormatRewriteExecutor(
      RewriteFilesInput input,
      ArcticTable table,
      StructLikeCollections structLikeCollections,
      String outputDir) {
    super(input, table, structLikeCollections);
    this.outputDir = outputDir;
  }

  @Override
  protected OptimizingDataReader dataReader() {
    return new MixFormatOptimizingDataReader(table, structLikeCollections, input);
  }

  @Override
  protected FileWriter<PositionDelete<Record>, DeleteWriteResult> posWriter() {
    FileAppenderFactory<Record> appenderFactory = fullMetricAppenderFactory();
    return new ArcticTreeNodePosDeleteWriter<>(
        appenderFactory, deleteFileFormat(), partition(),
        io, encryptionManager(), getTransactionId(input.rePosDeletedDataFilesForMixed()), baseLocation(), table.spec());
  }

  @Override
  protected FileWriter<Record, DataWriteResult> dataWriter() {
    TaskWriter<Record> writer = AdaptHiveGenericTaskWriterBuilder.builderFor(table)
        .withTransactionId(table.isKeyedTable() ? getTransactionId(input.rewrittenDataFilesForMixed()) : null)
        .withTaskId(0)
        .withCustomHiveSubdirectory(outputDir)
        .withTargetFileSize(targetSize())
        .buildWriter(StringUtils.isBlank(outputDir) ?
            WriteOperationKind.MAJOR_OPTIMIZE : WriteOperationKind.FULL_OPTIMIZE);
    return wrapTaskWriter2FileWriter(writer);
  }

  public long getTransactionId(List<PrimaryKeyedFile> dataFiles) {
    return dataFiles.stream().mapToLong(PrimaryKeyedFile::transactionId).max().getAsLong();
  }

  public String baseLocation() {
    if (table.isKeyedTable()) {
      return table.asKeyedTable().baseTable().location();
    } else {
      return table.asUnkeyedTable().location();
    }
  }

  public FileWriter<Record, DataWriteResult> wrapTaskWriter2FileWriter(TaskWriter<Record> writer) {
    return new FileWriter<Record, DataWriteResult>() {
      @Override
      public void write(Record row) {
        try {
          writer.write(row);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }

      @Override
      public long length() {
        return 0;
      }

      @Override
      public DataWriteResult result() {
        try {
          WriteResult complete = writer.complete();
          return new DataWriteResult(Arrays.asList(complete.dataFiles()));
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }

      @Override
      public void close() throws IOException {
        writer.close();
      }
    };
  }
}
