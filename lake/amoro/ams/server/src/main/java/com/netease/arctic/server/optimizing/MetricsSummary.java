package com.netease.arctic.server.optimizing;

import com.netease.arctic.data.IcebergContentFile;
import com.netease.arctic.data.IcebergDataFile;
import com.netease.arctic.optimizing.RewriteFilesInput;
import org.apache.iceberg.FileContent;
import org.apache.iceberg.relocated.com.google.common.base.MoreObjects;

import java.util.Collection;

public class MetricsSummary {
  private long newFileSize = 0;
  private int newFileCnt = 0;
  private long rewriteDataSize = 0;
  private long rewritePosDataSize = 0;
  private long equalityDeleteSize = 0;
  private long positionalDeleteSize = 0;
  private int rewriteDataFileCnt = 0;
  private int reRowDeletedDataFileCnt = 0;
  private int eqDeleteFileCnt = 0;
  private int posDeleteFileCnt = 0;

  public MetricsSummary() {
  }

  protected MetricsSummary(RewriteFilesInput input) {
    rewriteDataFileCnt = input.rewrittenDataFiles().length;
    reRowDeletedDataFileCnt = input.rePosDeletedDataFiles().length;
    for (IcebergDataFile rewriteFile : input.rewrittenDataFiles()) {
      rewriteDataSize += rewriteFile.fileSizeInBytes();
    }
    for (IcebergDataFile rewritePosDataFile : input.rePosDeletedDataFiles()) {
      rewritePosDataSize += rewritePosDataFile.fileSizeInBytes();
    }
    for (IcebergContentFile<?> delete : input.deleteFiles()) {
      if (delete.content() == FileContent.POSITION_DELETES) {
        positionalDeleteSize += delete.fileSizeInBytes();
        posDeleteFileCnt++;
      } else {
        equalityDeleteSize += delete.fileSizeInBytes();
        eqDeleteFileCnt++;
      }
    }
  }

  public MetricsSummary(Collection<TaskRuntime> taskRuntimes) {
    taskRuntimes.stream().map(TaskRuntime::getMetricsSummary).forEach(metrics -> {
      rewriteDataFileCnt += metrics.getRewriteDataFileCnt();
      reRowDeletedDataFileCnt += metrics.getReRowDeletedDataFileCnt();
      rewriteDataSize += metrics.getRewriteDataSize();
      rewritePosDataSize += metrics.getRewritePosDataSize();
      posDeleteFileCnt += metrics.getPosDeleteFileCnt();
      positionalDeleteSize += metrics.getPositionalDeleteSize();
      eqDeleteFileCnt += metrics.getEqDeleteFileCnt();
      equalityDeleteSize += metrics.getEqualityDeleteSize();
      newFileCnt += metrics.getNewFileCnt();
      newFileSize += metrics.getNewFileSize();
    });
  }

  public long getNewFileSize() {
    return newFileSize;
  }

  public int getNewFileCnt() {
    return newFileCnt;
  }

  public long getRewriteDataSize() {
    return rewriteDataSize;
  }

  public long getRewritePosDataSize() {
    return rewritePosDataSize;
  }

  public long getEqualityDeleteSize() {
    return equalityDeleteSize;
  }

  public long getPositionalDeleteSize() {
    return positionalDeleteSize;
  }

  public int getRewriteDataFileCnt() {
    return rewriteDataFileCnt;
  }

  public int getReRowDeletedDataFileCnt() {
    return reRowDeletedDataFileCnt;
  }

  public int getEqDeleteFileCnt() {
    return eqDeleteFileCnt;
  }

  public int getPosDeleteFileCnt() {
    return posDeleteFileCnt;
  }

  protected void setNewFileSize(long newFileSize) {
    this.newFileSize = newFileSize;
  }

  protected void setNewFileCnt(int newFileCnt) {
    this.newFileCnt = newFileCnt;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("newFileSize", newFileSize)
        .add("newFileCnt", newFileCnt)
        .add("rewriteDataSize", rewriteDataSize)
        .add("rewritePosDataSize", rewritePosDataSize)
        .add("equalityDeleteSize", equalityDeleteSize)
        .add("positionalDeleteSize", positionalDeleteSize)
        .add("rewriteDataFileCnt", rewriteDataFileCnt)
        .add("reRowDeletedDataFileCnt", reRowDeletedDataFileCnt)
        .add("eqDeleteFileCnt", eqDeleteFileCnt)
        .add("posDeleteFileCnt", posDeleteFileCnt)
        .toString();
  }
}
