package com.netease.arctic.server.dashboard.utils;

import com.netease.arctic.optimizing.RewriteFilesOutput;
import com.netease.arctic.server.dashboard.model.FilesStatistics;
import com.netease.arctic.server.dashboard.model.TableOptimizingInfo;
import com.netease.arctic.server.optimizing.MetricsSummary;
import com.netease.arctic.server.optimizing.OptimizingProcess;
import com.netease.arctic.server.table.TableRuntime;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.DeleteFile;

public class OptimizingUtil {

  /**
   * Build current table optimize info.
   *
   * @return TableOptimizeInfo
   */
  public static TableOptimizingInfo buildTableOptimizeInfo(TableRuntime optimizingTableRuntime) {
    OptimizingProcess process = optimizingTableRuntime.getOptimizingProcess();
    TableOptimizingInfo tableOptimizeInfo =
        new TableOptimizingInfo(optimizingTableRuntime.getTableIdentifier());
    tableOptimizeInfo.setOptimizeStatus(optimizingTableRuntime.getOptimizingStatus().displayValue());
    tableOptimizeInfo.setDuration(System.currentTimeMillis() - optimizingTableRuntime.getCurrentStatusStartTime());
    tableOptimizeInfo.setQuota(optimizingTableRuntime.getTargetQuota());
    tableOptimizeInfo.setQuotaOccupation(process == null ? 0 : optimizingTableRuntime.calculateQuotaOccupy());
    FilesStatistics optimizeFileInfo =
        collectOptimizingFileInfo(process == null ? new MetricsSummary() : process.getSummary());
    if (optimizeFileInfo != null) {
      tableOptimizeInfo.setFileCount(optimizeFileInfo.getFileCnt());
      tableOptimizeInfo.setFileSize(optimizeFileInfo.getTotalSize());
    }
    tableOptimizeInfo.setGroupName(optimizingTableRuntime.getOptimizerGroup());
    return tableOptimizeInfo;
  }

  private static FilesStatistics collectOptimizingFileInfo(MetricsSummary metricsSummary) {
    return FilesStatistics.builder()
        .addFiles(metricsSummary.getEqualityDeleteSize(), metricsSummary.getEqDeleteFileCnt())
        .addFiles(metricsSummary.getPositionalDeleteSize(), metricsSummary.getPosDeleteFileCnt())
        .addFiles(metricsSummary.getRewriteDataSize(), metricsSummary.getRewriteDataFileCnt())
        .build();
  }

  public static long getFileSize(RewriteFilesOutput output) {
    int size = 0;
    if (output.getDataFiles() != null) {
      for (DataFile dataFile : output.getDataFiles()) {
        size += dataFile.fileSizeInBytes();
      }
    }
    if (output.getDeleteFiles() != null) {
      for (DeleteFile dataFile : output.getDeleteFiles()) {
        size += dataFile.fileSizeInBytes();
      }
    }
    return size;
  }

  public static int getFileCount(RewriteFilesOutput output) {
    int length = 0;
    if (output.getDataFiles() != null) {
      length += output.getDataFiles().length;
    }
    if (output.getDeleteFiles() != null) {
      length += output.getDeleteFiles().length;
    }
    return length;
  }
}
