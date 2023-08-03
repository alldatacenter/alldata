package com.netease.arctic.server.dashboard.utils;


import com.netease.arctic.server.dashboard.model.FilesStatistics;

public class FilesStatisticsBuilder {
  private int fileCnt = 0;
  private long totalSize = 0;

  public void addFile(long fileSize) {
    this.totalSize += fileSize;
    this.fileCnt++;
  }

  public FilesStatisticsBuilder addFilesStatistics(FilesStatistics fs) {
    this.totalSize += fs.getTotalSize();
    this.fileCnt += fs.getFileCnt();
    return this;
  }

  public void addFiles(long totalSize, int fileCnt) {
    this.totalSize += totalSize;
    this.fileCnt += fileCnt;
  }

  public FilesStatistics build() {
    return new FilesStatistics(fileCnt, totalSize);
  }

  public static FilesStatistics build(int fileCnt, long totalSize) {
    return new FilesStatistics(fileCnt, totalSize);
  }
}

