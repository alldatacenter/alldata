package com.netease.arctic.server.dashboard.model;

public class PlatformFileInfo {
  Integer fileId;
  String fileName;
  String fileContent;
  String filePath;

  public PlatformFileInfo(String fileName, String fileContent) {
    this.fileId = 0;
    this.fileName = fileName;
    this.fileContent = fileContent;
    this.filePath = null;
  }

  public Integer getFileId() {
    return fileId;
  }

  public void setFileId(Integer fileId) {
    this.fileId = fileId;
  }

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public String getFilePath() {
    return filePath;
  }

  public void setFilePath(String filePath) {
    this.filePath = filePath;
  }

  public String getFileContent() {
    return fileContent;
  }

  public void setFileContent(String fileContent) {
    this.fileContent = fileContent;
  }
}
