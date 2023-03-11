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

import com.netease.arctic.data.DataFileType;
import com.netease.arctic.data.DataTreeNode;
import com.netease.arctic.data.PrimaryKeyData;
import com.netease.arctic.io.ArcticFileIO;
import com.netease.arctic.table.PrimaryKeySpec;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.FileFormat;
import org.apache.iceberg.PartitionKey;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.StructLike;
import org.apache.iceberg.io.DataWriter;
import org.apache.iceberg.io.FileAppenderFactory;
import org.apache.iceberg.io.TaskWriter;
import org.apache.iceberg.io.WriteResult;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.iceberg.relocated.com.google.common.collect.Maps;
import org.apache.iceberg.relocated.com.google.common.collect.Sets;
import org.apache.iceberg.util.Tasks;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Abstract implementation of writer for {@link com.netease.arctic.table.BaseTable}.
 * @param <T> to indicate the record data type.
 */
public abstract class BaseTaskWriter<T> implements TaskWriter<T> {

  private final long mask;

  private final PartitionKey partitionKey;
  private final PrimaryKeyData primaryKey;
  private final ArcticFileIO io;
  private final WriterHolder<T> writerHolder;

  protected BaseTaskWriter(
      FileFormat format, FileAppenderFactory<T> appenderFactory,
      OutputFileFactory outputFileFactory, ArcticFileIO io, long targetFileSize, long mask,
      Schema schema, PartitionSpec spec, PrimaryKeySpec primaryKeySpec, boolean orderedWriter
  ) {
    if (orderedWriter) {
      this.writerHolder = new OrderedWriterHolder<>(
          format, appenderFactory, outputFileFactory, io, targetFileSize);
    } else {
      this.writerHolder = new FanoutWriterHolder<>(
          format, appenderFactory, outputFileFactory, io, targetFileSize);
    }
    this.io = io;
    this.mask = mask;
    this.partitionKey = new PartitionKey(spec, schema);
    this.primaryKey = primaryKeySpec == null ? null : new PrimaryKeyData(primaryKeySpec, schema);
  }

  @Override
  public void write(T row) throws IOException {
    DataWriterKey writerKey = buildWriterKey(row);
    TaskDataWriter<T> writer = writerHolder.get(writerKey);
    write(writer, row);
  }

  protected void write(TaskDataWriter<T> writer, T row) throws IOException {
    writer.write(row);
  }

  protected DataWriterKey buildWriterKey(T row) {
    StructLike structLike = asStructLike(row);
    partitionKey.partition(structLike);
    DataTreeNode node;
    if (primaryKey != null) {
      primaryKey.primaryKey(structLike);
      node = primaryKey.treeNode(mask);
    } else {
      node = DataTreeNode.ROOT;
    }
    return new DataWriterKey(partitionKey, node, DataFileType.BASE_FILE);
  }

  @Override
  public void abort() throws IOException {
    writerHolder.close();
    List<DataFile> completedFiles = writerHolder.completedFiles();

    // clean up files created by this writer
    Tasks.foreach(completedFiles)
        .throwFailureWhenFinished()
        .noRetry()
        .run(file -> io.deleteFile(file.path().toString()));
  }

  @Override
  public WriteResult complete() throws IOException {
    writerHolder.close();
    List<DataFile> files = writerHolder.completedFiles();
    return WriteResult.builder().addDataFiles(files.toArray(new DataFile[]{})).build();
  }

  @Override
  public void close() throws IOException {
    writerHolder.close();
  }

  /**
   * Wrap the data as a {@link StructLike}.
   */
  protected abstract StructLike asStructLike(T data);



  protected static class DataWriterKey extends TaskWriterKey {

    final PartitionKey partitionKey;

    public DataWriterKey(PartitionKey partitionKey, DataTreeNode treeNode, DataFileType fileType) {
      super(partitionKey, treeNode, fileType);
      this.partitionKey = partitionKey;
    }

    public DataWriterKey copy() {
      return new DataWriterKey(partitionKey.copy(), getTreeNode(), getFileType());
    }

    @Override
    public PartitionKey getPartitionKey() {
      return partitionKey;
    }

    @Override
    public String toString() {
      return "[" + partitionKey.toString() + " " + getTreeNode().toString() + "]";
    }
  }


  protected abstract static class WriterHolder<T> {

    protected final FileFormat format;
    protected final FileAppenderFactory<T> appenderFactory;
    protected final OutputFileFactory outputFileFactory;
    protected final ArcticFileIO io;
    protected final long targetFileSize;
    protected final List<DataFile> completedFiles = Lists.newArrayList();
    private boolean closed = false;

    public WriterHolder(
        FileFormat format,
        FileAppenderFactory<T> appenderFactory,
        OutputFileFactory outputFileFactory,
        ArcticFileIO io,
        long targetFileSize) {
      this.format = format;
      this.appenderFactory = appenderFactory;
      this.outputFileFactory = outputFileFactory;
      this.io = io;
      this.targetFileSize = targetFileSize;
    }

    protected abstract TaskDataWriter<T> getDataWriter(DataWriterKey writerKey) throws IOException;

    public TaskDataWriter<T> get(DataWriterKey writerKey) throws IOException {
      if (closed) {
        throw new IllegalStateException("The task writer has already been closed.");
      }
      return getDataWriter(writerKey);
    }

    public void close() throws IOException {
      this.closed = true;
      doClose();
    }

    protected abstract void doClose() throws IOException;

    public List<DataFile> completedFiles() {
      return Lists.newArrayList(completedFiles);
    }

    protected boolean shouldRollToNewFile(TaskDataWriter<T> dataWriter) {
      // TODO: ORC file now not support target file size before closed
      return !format.equals(FileFormat.ORC) && dataWriter.length() >= targetFileSize;
    }



    protected TaskDataWriter<T> newWriter(TaskWriterKey writerKey) {
      DataWriter<T> dataWriter = io.doAs(() -> appenderFactory.newDataWriter(
          outputFileFactory.newOutputFile(writerKey), format, writerKey.getPartitionKey()));
      return new TaskDataWriter<>(dataWriter, io);
    }
  }

  /**
   * a fan-out writer holder which will keep an opened writer for all write key.
   * This holder does not require records have been sorted, but will keep open files as many as write keys.
   */
  protected static class FanoutWriterHolder<T> extends WriterHolder<T> {
    private final Map<DataWriterKey, TaskDataWriter<T>> dataWriterMap = Maps.newHashMap();

    public FanoutWriterHolder(
        FileFormat format, FileAppenderFactory<T> appenderFactory,
        OutputFileFactory outputFileFactory, ArcticFileIO io, long targetFileSize) {
      super(format, appenderFactory, outputFileFactory, io, targetFileSize);
    }

    @Override
    public TaskDataWriter<T> getDataWriter(DataWriterKey writerKey) throws IOException {
      TaskDataWriter<T> writer;
      writer = dataWriterMap.get(writerKey);
      if (writer != null && shouldRollToNewFile(writer)) {
        writer.close();
        completedFiles.add(writer.toDataFile());
        dataWriterMap.remove(writerKey);
      }

      if (!dataWriterMap.containsKey(writerKey)) {
        DataWriterKey copiedWriterKey = writerKey.copy();
        writer = newWriter(copiedWriterKey);
        dataWriterMap.put(copiedWriterKey, writer);
      } else {
        writer = dataWriterMap.get(writerKey);
      }
      return writer;
    }

    @Override
    public void doClose() throws IOException {
      for (TaskDataWriter<T> dataWriter : dataWriterMap.values()) {
        dataWriter.close();
        DataFile dataFile = dataWriter.toDataFile();
        if (dataFile != null) {
          completedFiles.add(dataWriter.toDataFile());
        }
      }
      dataWriterMap.clear();
    }

  }

  /**
   * a writer holder which require records had been sorted before write.
   * The holder will hold only one writer in open, and will throw an IllegalStateException exception
   * if TaskWriter request a data writer via a write key which has been already closed.
   */
  protected static class OrderedWriterHolder<T> extends WriterHolder<T> {

    private TaskDataWriter<T> currentWriter;
    private DataWriterKey currentKey;
    private final Set<DataWriterKey> completedKeys = Sets.newHashSet();


    public OrderedWriterHolder(
        FileFormat format,
        FileAppenderFactory<T> appenderFactory,
        OutputFileFactory outputFileFactory,
        ArcticFileIO io,
        long targetFileSize) {
      super(format, appenderFactory, outputFileFactory, io, targetFileSize);
    }

    @Override
    public TaskDataWriter<T> getDataWriter(DataWriterKey writerKey) throws IOException {
      if (!writerKey.equals(currentKey)) {
        if (currentKey != null) {
          closeCurrentWriter();
          completedKeys.add(currentKey);
        }

        if (completedKeys.contains(writerKey)) {
          throw new IllegalStateException("The write key " + writerKey + " has already been completed");
        }

        currentKey = writerKey.copy();
        currentWriter = newWriter(currentKey);
      } else if (shouldRollToNewFile(currentWriter)) {
        closeCurrentWriter();
        currentWriter = newWriter(writerKey);
      }

      return currentWriter;
    }

    private void closeCurrentWriter() throws IOException {
      if (currentWriter != null) {
        currentWriter.close();
        DataFile dataFile = currentWriter.toDataFile();
        if (dataFile != null) {
          completedFiles.add(currentWriter.toDataFile());
        }
        currentWriter = null;
      }
    }

    @Override
    public void doClose() throws IOException {
      closeCurrentWriter();
    }
  }

  protected static class TaskDataWriter<T> {
    private final DataWriter<T> dataWriter;
    private long currentRows = 0;

    private final ArcticFileIO io;

    protected TaskDataWriter(DataWriter<T> dataWriter, ArcticFileIO io) {
      this.dataWriter = dataWriter;
      this.io = io;
    }

    protected void write(T record) {
      dataWriter.write(record);
      currentRows++;
    }

    protected void close() {
      io.doAs(() -> {
        dataWriter.close();
        return null;
      });
      if (currentRows <= 0) {
        try {
          io.deleteFile(dataWriter.toDataFile().path().toString());
        } catch (UncheckedIOException e) {
          // the file may not have been created, and it isn't worth failing the job to clean up, skip deleting
        }
      }
    }

    protected DataFile toDataFile() {
      if (currentRows > 0) {
        return dataWriter.toDataFile();
      } else {
        return null;
      }
    }

    protected long length() {
      return dataWriter.length();
    }
  }
}
