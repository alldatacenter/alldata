package com.netease.arctic.io;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.iceberg.io.InputFile;
import org.apache.iceberg.io.OutputFile;
import org.apache.iceberg.relocated.com.google.common.annotations.VisibleForTesting;
import org.apache.iceberg.relocated.com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

/**
 * Implementation of {@link ArcticFileIO} with deleted files recovery support.
 */
public class RecoverableArcticFileIO implements ArcticFileIO {
  private static final Logger LOG = LoggerFactory.getLogger(RecoverableArcticFileIO.class);

  private final ArcticFileIO fileIO;
  private final TableTrashManager trashManager;
  private final String trashFilePattern;
  private final Pattern pattern;

  RecoverableArcticFileIO(
      com.netease.arctic.io.ArcticFileIO fileIO,
      TableTrashManager trashManager,
      String trashFilePattern) {
    this.fileIO = fileIO;
    this.trashManager = trashManager;
    this.trashFilePattern = trashFilePattern;
    this.pattern = Strings.isNullOrEmpty(this.trashFilePattern) ? null : Pattern.compile(this.trashFilePattern);
  }

  @Override
  public <T> T doAs(Callable<T> callable) {
    return fileIO.doAs(callable);
  }

  @Override
  public boolean exists(String path) {
    return fileIO.exists(path);
  }

  @Override
  public void mkdirs(String path) {
    fileIO.mkdirs(path);
  }

  @Override
  public void rename(String oldPath, String newPath) {
    fileIO.rename(oldPath, newPath);
  }

  @Override
  public void deleteDirectoryRecursively(String path) {
    //Do not move trash when deleting directory as it is used for dropping table only
    fileIO.deleteDirectoryRecursively(path);
  }

  @Override
  public List<FileStatus> list(String location) {
    return fileIO.list(location);
  }

  @Override
  public boolean isDirectory(String location) {
    return fileIO.isDirectory(location);
  }

  @Override
  public boolean isEmptyDirectory(String location) {
    return fileIO.isEmptyDirectory(location);
  }

  @Override
  public InputFile newInputFile(String path) {
    return fileIO.newInputFile(path);
  }

  @Override
  public OutputFile newOutputFile(String path) {
    return fileIO.newOutputFile(path);
  }

  @Override
  public void deleteFile(String path) {
    if (matchTrashFilePattern(path)) {
      moveToTrash(path);
    } else {
      fileIO.deleteFile(path);
    }
  }

  @Override
  public void deleteFile(InputFile file) {
    if (matchTrashFilePattern(file.location())) {
      moveToTrash(file.location());
    } else {
      fileIO.deleteFile(file);
    }
  }

  @Override
  public void deleteFile(OutputFile file) {
    if (matchTrashFilePattern(file.location())) {
      moveToTrash(file.location());
    } else {
      fileIO.deleteFile(file);
    }
  }

  @Override
  public void initialize(Map<String, String> properties) {
    fileIO.initialize(properties);
  }

  @Override
  public void close() {
    fileIO.close();
  }


  @VisibleForTesting
  protected boolean matchTrashFilePattern(String path) {
    return pattern.matcher(path).matches();
  }

  public ArcticFileIO getInternalFileIO() {
    return fileIO;
  }

  public TableTrashManager getTrashManager() {
    return trashManager;
  }

  public String getTrashFilePattern() {
    return trashFilePattern;
  }

  private void moveToTrash(String filePath) {
    trashManager.moveFileToTrash(filePath);
    LOG.debug("Move file:{} to table trash", filePath);
  }

  @Override
  public void setConf(Configuration conf) {
    fileIO.setConf(conf);
  }

  @Override
  public Configuration getConf() {
    return fileIO.getConf();
  }
}
