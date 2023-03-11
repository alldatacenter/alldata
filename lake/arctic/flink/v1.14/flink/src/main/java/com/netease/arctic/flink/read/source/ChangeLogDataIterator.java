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

package com.netease.arctic.flink.read.source;

import com.netease.arctic.data.ChangeAction;
import com.netease.arctic.scan.ArcticFileScanTask;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;

import static com.netease.arctic.data.ChangeAction.DELETE;
import static com.netease.arctic.data.ChangeAction.INSERT;
import static com.netease.arctic.data.ChangeAction.UPDATE_AFTER;
import static com.netease.arctic.data.ChangeAction.UPDATE_BEFORE;

/**
 * This is a change log data iterator that replays the change log data appended to arctic change table with ordered.
 */
public class ChangeLogDataIterator<T> extends DataIterator<T> {
  private final DataIterator<T> insertDataIterator;
  private DataIterator<T> deleteDataIterator = DataIterator.empty();

  private final Function<T, T> arcticMetaColumnRemover;
  private final Function<ChangeActionTrans<T>, T> changeActionTransformer;

  private final QueueHolder<T> insertHolder = new QueueHolder<>();
  private final QueueHolder<T> deleteHolder = new QueueHolder<>();

  public ChangeLogDataIterator(
      FileScanTaskReader<T> fileScanTaskReader,
      Collection<ArcticFileScanTask> insertTasks,
      Collection<ArcticFileScanTask> deleteTasks,
      Function<T, Long> arcticFileOffsetGetter,
      Function<T, T> arcticMetaColumnRemover,
      Function<ChangeActionTrans<T>, T> changeActionTransformer) {
    super(fileScanTaskReader, Collections.emptyList(), arcticFileOffsetGetter, arcticMetaColumnRemover);
    this.insertDataIterator =
        new DataIterator<>(fileScanTaskReader, insertTasks, arcticFileOffsetGetter, arcticMetaColumnRemover);
    if (deleteTasks != null && !deleteTasks.isEmpty()) {
      this.deleteDataIterator =
          new DataIterator<>(fileScanTaskReader, deleteTasks, arcticFileOffsetGetter, arcticMetaColumnRemover);
    }
    this.arcticMetaColumnRemover = arcticMetaColumnRemover;
    this.changeActionTransformer = changeActionTransformer;
  }

  public void seek(
      int startingInsertFileOffset,
      int startingDeleteFileOffset,
      long startingInsertRecordOffset,
      long startingDeleteRecordOffset) {
    insertDataIterator.seek(startingInsertFileOffset, startingInsertRecordOffset);
    deleteDataIterator.seek(startingDeleteFileOffset, startingDeleteRecordOffset);
  }

  @Override
  public void seek(int startingFileOffset, long startingRecordOffset) {
    throw new UnsupportedOperationException("This operation is not supported in change log data iterator.");
  }

  private void loadQueueHolder(boolean insert) {
    DataIterator<T> dataIterator = insert ? insertDataIterator : deleteDataIterator;
    QueueHolder<T> holder = insert ? insertHolder : deleteHolder;
    if (dataIterator.hasNext() && holder.isEmpty()) {
      T next = dataIterator.next();
      long nextOffset = dataIterator.currentArcticFileOffset();
      ChangeAction changeAction = insert ? INSERT : DELETE;
      holder.put(next, changeAction, nextOffset);
    }
  }

  @Override

  public boolean hasNext() {
    loadQueueHolder(false);
    loadQueueHolder(true);

    return deleteHolder.isNotEmpty() || insertHolder.isNotEmpty();
  }

  @Override
  public boolean currentFileHasNext() {
    return deleteDataIterator.currentFileHasNext() || insertDataIterator.currentFileHasNext() ||
        deleteHolder.isNotEmpty() || insertHolder.isNotEmpty();
  }

  @Override
  public T next() {
    T row;
    if (deleteHolder.isEmpty() && insertHolder.isNotEmpty()) {
      row = changeActionTransformer.apply(
          ChangeActionTrans.of(insertHolder.nextRow, insertHolder.changeAction));
      insertHolder.clean();
    } else if (deleteHolder.isNotEmpty() && insertHolder.isEmpty()) {
      row = changeActionTransformer.apply(
          ChangeActionTrans.of(deleteHolder.nextRow, deleteHolder.changeAction));
      deleteHolder.clean();
    } else if (deleteHolder.equalTo(insertHolder)) {
      row = changeActionTransformer.apply(
          ChangeActionTrans.of(deleteHolder.nextRow, UPDATE_BEFORE));
      insertHolder.changeAction = UPDATE_AFTER;
      deleteHolder.clean();
    } else if (deleteHolder.lesser(insertHolder)) {
      row = changeActionTransformer.apply(
          ChangeActionTrans.of(deleteHolder.nextRow, deleteHolder.changeAction));
      deleteHolder.clean();
    } else {
      row = changeActionTransformer.apply(
          ChangeActionTrans.of(insertHolder.nextRow, insertHolder.changeAction));
      insertHolder.clean();
    }

    return arcticMetaColumnRemover.apply(row);
  }

  @Override
  public void close() throws IOException {
    insertDataIterator.close();
    deleteDataIterator.close();
  }

  public int insertFileOffset() {
    return insertDataIterator.fileOffset();
  }

  public long insertRecordOffset() {
    return insertDataIterator.recordOffset();
  }

  public int deleteFileOffset() {
    return deleteDataIterator.fileOffset();
  }

  public long deleteRecordOffset() {
    return deleteDataIterator.recordOffset();
  }

  private static class QueueHolder<T> {
    T nextRow;
    ChangeAction changeAction;
    Long nextOffset;

    public QueueHolder() {
    }

    boolean isEmpty() {
      return nextRow == null;
    }

    boolean isNotEmpty() {
      return nextRow != null;
    }

    public void put(T nextRow, ChangeAction changeAction, Long nextOffset) {
      this.nextRow = nextRow;
      this.changeAction = changeAction;
      this.nextOffset = nextOffset;
    }

    public T get() {
      return nextRow;
    }

    boolean lesser(QueueHolder<T> that) {
      return this.nextOffset.compareTo(that.nextOffset) < 0;
    }

    boolean equalTo(QueueHolder<T> that) {
      return this.nextOffset.compareTo(that.nextOffset) == 0;
    }

    void clean() {
      nextRow = null;
      nextOffset = null;
    }
  }

  public static class ChangeActionTrans<T> {
    protected final T row;
    protected final ChangeAction changeAction;

    private ChangeActionTrans(T row, ChangeAction changeAction) {
      this.row = row;
      this.changeAction = changeAction;
    }

    public static <T> ChangeActionTrans<T> of(T row, ChangeAction changeAction) {
      return new ChangeActionTrans<>(row, changeAction);
    }

    public T row() {
      return row;
    }

    public ChangeAction changeAction() {
      return changeAction;
    }
  }
}
