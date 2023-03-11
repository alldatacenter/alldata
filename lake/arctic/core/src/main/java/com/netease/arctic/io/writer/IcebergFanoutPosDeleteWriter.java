/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.io.writer;

import com.netease.arctic.utils.TableFileUtils;
import org.apache.iceberg.DeleteFile;
import org.apache.iceberg.FileFormat;
import org.apache.iceberg.StructLike;
import org.apache.iceberg.deletes.PositionDelete;
import org.apache.iceberg.deletes.PositionDeleteWriter;
import org.apache.iceberg.encryption.EncryptedOutputFile;
import org.apache.iceberg.encryption.EncryptionManager;
import org.apache.iceberg.io.DeleteWriteResult;
import org.apache.iceberg.io.FileAppenderFactory;
import org.apache.iceberg.io.FileIO;
import org.apache.iceberg.io.FileWriter;
import org.apache.iceberg.relocated.com.google.common.base.Preconditions;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.iceberg.relocated.com.google.common.collect.Maps;
import org.apache.iceberg.util.CharSequenceSet;
import org.apache.iceberg.util.CharSequenceWrapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Positional delete file writer for iceberg tables. Write to different delete file for every data file.
 * The output delete files are named with pattern: {data_file_name}-delete-{delete_file_suffix}.
 * 
 * @param <T> to indicate the record data type.
 */
public class IcebergFanoutPosDeleteWriter<T> implements FileWriter<PositionDelete<T>, DeleteWriteResult> {

  private final List<DeleteFile> completedFiles = Lists.newArrayList();
  private final Map<CharSequenceWrapper, List<PosRow<T>>> posDeletes = Maps.newHashMap();
  private final CharSequenceSet referencedDataFiles = CharSequenceSet.empty();
  private final CharSequenceWrapper wrapper = CharSequenceWrapper.wrap(null);

  private final FileAppenderFactory<T> appenderFactory;
  private final FileFormat format;
  private final StructLike partition;
  private final FileIO fileIO;
  private final EncryptionManager encryptionManager;
  private final String fileNameSuffix;

  private boolean closed = false;
  private Throwable failure;

  public IcebergFanoutPosDeleteWriter(
      FileAppenderFactory<T> appenderFactory,
      FileFormat format,
      StructLike partition,
      FileIO fileIO,
      EncryptionManager encryptionManager,
      String fileNameSuffix) {
    this.appenderFactory = appenderFactory;
    this.format = format;
    this.partition = partition;
    this.fileIO = fileIO;
    this.encryptionManager = encryptionManager;
    this.fileNameSuffix = fileNameSuffix;
  }

  protected void setFailure(Throwable throwable) {
    if (failure == null) {
      this.failure = throwable;
    }
  }

  @Override
  public long length() {
    throw new UnsupportedOperationException(
        this.getClass().getName() + " does not implement length");
  }

  @Override
  public void write(PositionDelete<T> payload) {
    delete(payload.path(), payload.pos(), payload.row());
  }

  public void delete(CharSequence path, long pos) {
    delete(path, pos, null);
  }

  public void delete(CharSequence path, long pos, T row) {
    List<PosRow<T>> posRows = posDeletes.get(wrapper.set(path));
    if (posRows != null) {
      posRows.add(PosRow.of(pos, row));
    } else {
      posDeletes.put(CharSequenceWrapper.wrap(path), Lists.newArrayList(PosRow.of(pos, row)));
    }
  }

  public List<DeleteFile> complete() throws IOException {
    close();

    Preconditions.checkState(failure == null, "Cannot return results from failed writer", failure);

    return completedFiles;
  }

  public CharSequenceSet referencedDataFiles() {
    return referencedDataFiles;
  }

  @Override
  public void close() throws IOException {
    if (!closed) {
      this.closed = true;
      flushDeletes();
    }
  }

  @Override
  public DeleteWriteResult result() {
    Preconditions.checkState(closed, "Cannot get result from unclosed writer");
    return new DeleteWriteResult(completedFiles, referencedDataFiles);
  }

  private void flushDeletes() {
    if (posDeletes.isEmpty()) {
      return;
    }

    posDeletes.forEach((filePath, posDeletes) -> {
      if (posDeletes.size() <= 0) {
        return;
      }
      posDeletes.sort(Comparator.comparingLong(PosRow::pos));
      String fileName = TableFileUtils.getFileName(filePath.get().toString());
      FileFormat fileFormat = FileFormat.fromFileName(fileName);
      if (fileFormat != null) {
        fileName = fileName.substring(0, fileName.length() - fileFormat.name().length() - 1);
      }
      String fileDir = TableFileUtils.getFileDir(filePath.get().toString());
      String deleteFilePath = format.addExtension(String.format("%s/%s-delete-%s", fileDir, fileName,
          fileNameSuffix));
      EncryptedOutputFile outputFile = encryptionManager.encrypt(fileIO.newOutputFile(deleteFilePath));

      PositionDeleteWriter<T> writer =
          appenderFactory.newPosDeleteWriter(outputFile, format, partition);
      PositionDelete<T> posDelete = PositionDelete.create();
      try (PositionDeleteWriter<T> closeableWriter = writer) {
        posDeletes.forEach(
            posRow -> closeableWriter.write(posDelete.set(filePath.get(), posRow.pos(), posRow.row())));
      } catch (IOException e) {
        setFailure(e);
        throw new UncheckedIOException(
            "Failed to write the sorted path/pos pairs to pos-delete file: " +
                outputFile.encryptingOutputFile().location(),
            e);
      }
      // Add the referenced data files.
      referencedDataFiles.addAll(writer.referencedDataFiles());

      // Add the completed delete files.
      completedFiles.add(writer.toDeleteFile());
    });

    // Clear the buffered pos-deletions.
    posDeletes.clear();
  }

  private static class PosRow<R> {
    private final long pos;
    private final R row;

    static <R> PosRow<R> of(long pos, R row) {
      return new PosRow<>(pos, row);
    }

    private PosRow(long pos, R row) {
      this.pos = pos;
      this.row = row;
    }

    long pos() {
      return pos;
    }

    R row() {
      return row;
    }
  }
}
