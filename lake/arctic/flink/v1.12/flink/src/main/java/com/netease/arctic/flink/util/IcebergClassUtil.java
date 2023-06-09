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

package com.netease.arctic.flink.util;

import com.netease.arctic.flink.interceptor.ProxyFactory;
import com.netease.arctic.io.ArcticFileIO;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.apache.flink.streaming.api.operators.AbstractStreamOperator;
import org.apache.flink.streaming.api.operators.AbstractUdfStreamOperator;
import org.apache.flink.streaming.api.operators.MailboxExecutor;
import org.apache.flink.streaming.api.operators.OneInputStreamOperator;
import org.apache.flink.streaming.api.operators.OneInputStreamOperatorFactory;
import org.apache.flink.streaming.runtime.tasks.ProcessingTimeService;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.types.logical.RowType;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.encryption.EncryptionManager;
import org.apache.iceberg.flink.TableLoader;
import org.apache.iceberg.flink.sink.TaskWriterFactory;
import org.apache.iceberg.flink.source.FlinkInputFormat;
import org.apache.iceberg.flink.source.StreamingReaderOperator;
import org.apache.iceberg.io.FileIO;
import org.apache.iceberg.io.WriteResult;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * An util generates Apache Iceberg writer and committer operator w
 */
public class IcebergClassUtil {
  private static final String ICEBERG_SCAN_CONTEXT_CLASS = "org.apache.iceberg.flink.source.ScanContext";
  private static final String ICEBERG_PARTITION_SELECTOR_CLASS = "org.apache.iceberg.flink.sink.PartitionKeySelector";
  private static final String ICEBERG_FILE_COMMITTER_CLASS = "org.apache.iceberg.flink.sink.IcebergFilesCommitter";
  private static final String ICEBERG_FILE_WRITER_CLASS = "org.apache.iceberg.flink.sink.IcebergStreamWriter";

  public static KeySelector<RowData, Object> newPartitionKeySelector(
      PartitionSpec spec, Schema schema, RowType flinkSchema) {
    try {
      Class<?> clazz = forName(ICEBERG_PARTITION_SELECTOR_CLASS);
      Constructor<?> c = clazz.getConstructor(PartitionSpec.class, Schema.class, RowType.class);
      c.setAccessible(true);
      return (KeySelector<RowData, Object>) c.newInstance(spec, schema, flinkSchema);
    } catch (NoSuchMethodException | IllegalAccessException |
        InvocationTargetException | InstantiationException e) {
      throw new RuntimeException(e);
    }
  }

  public static OneInputStreamOperator<WriteResult, Void> newIcebergFilesCommitter(
      TableLoader tableLoader, boolean replacePartitions) {
    try {
      Class<?> clazz = forName(ICEBERG_FILE_COMMITTER_CLASS);
      Constructor<?> c = clazz.getDeclaredConstructor(TableLoader.class, boolean.class);
      c.setAccessible(true);
      return (OneInputStreamOperator<WriteResult, Void>) c.newInstance(tableLoader, replacePartitions);
    } catch (NoSuchMethodException | IllegalAccessException |
        InvocationTargetException | InstantiationException e) {
      throw new RuntimeException(e);
    }
  }

  public static OneInputStreamOperator<WriteResult, Void> newIcebergFilesCommitter(TableLoader tableLoader,
                                                                                   boolean replacePartitions,
                                                                                   ArcticFileIO arcticFileIO) {
    OneInputStreamOperator<WriteResult, Void> obj = newIcebergFilesCommitter(tableLoader, replacePartitions);
    return (OneInputStreamOperator) ProxyUtil.getProxy(obj, arcticFileIO);
  }

  public static ProxyFactory<AbstractStreamOperator> getIcebergStreamWriterProxyFactory(
      String fullTableName, TaskWriterFactory taskWriterFactory, ArcticFileIO arcticFileIO) {
    Class<?> clazz = forName(ICEBERG_FILE_WRITER_CLASS);
    return (ProxyFactory<AbstractStreamOperator>) ProxyUtil.getProxyFactory(clazz, arcticFileIO,
        new Class[]{String.class, TaskWriterFactory.class},
        new Object[]{fullTableName, taskWriterFactory});
  }

  public static StreamingReaderOperator newStreamingReaderOperator(FlinkInputFormat format,
                                                                   ProcessingTimeService timeService,
                                                                   MailboxExecutor mailboxExecutor) {
    try {
      Constructor<StreamingReaderOperator> c = StreamingReaderOperator.class.getDeclaredConstructor(
          FlinkInputFormat.class,
          ProcessingTimeService.class, MailboxExecutor.class);
      c.setAccessible(true);
      return c.newInstance(format, timeService, mailboxExecutor);
    } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException | InstantiationException e) {
      throw new RuntimeException(e);
    }
  }

  public static FlinkInputFormat getInputFormat(OneInputStreamOperatorFactory operatorFactory) {
    try {
      Class<?>[] classes = StreamingReaderOperator.class.getDeclaredClasses();
      Class<?> clazz = null;
      for (Class<?> c : classes) {
        if ("OperatorFactory".equals(c.getSimpleName())) {
          clazz = c;
          break;
        }
      }
      Field field = clazz.getDeclaredField("format");
      field.setAccessible(true);
      return (FlinkInputFormat) (field.get(operatorFactory));
    } catch (IllegalAccessException | NoSuchFieldException e) {
      throw new RuntimeException(e);
    }
  }

  public static ProxyFactory<FlinkInputFormat> getInputFormatProxyFactory(OneInputStreamOperatorFactory operatorFactory,
                                                                          ArcticFileIO arcticFileIO,
                                                                          Schema tableSchema) {
    FlinkInputFormat inputFormat = getInputFormat(operatorFactory);
    TableLoader tableLoader = ReflectionUtil.getField(FlinkInputFormat.class, inputFormat, "tableLoader");
    FileIO io = ReflectionUtil.getField(FlinkInputFormat.class, inputFormat, "io");
    EncryptionManager encryption = ReflectionUtil.getField(FlinkInputFormat.class, inputFormat, "encryption");
    Object context = ReflectionUtil.getField(FlinkInputFormat.class, inputFormat, "context");

    Class<?> scanContextClass = forName(ICEBERG_SCAN_CONTEXT_CLASS);

    return ProxyUtil.getProxyFactory(FlinkInputFormat.class, arcticFileIO,
        new Class[]{TableLoader.class, Schema.class, FileIO.class, EncryptionManager.class, scanContextClass},
        new Object[]{tableLoader, tableSchema, io, encryption, context});
  }

  private static Class<?> forName(String className) {
    try {
      return Class.forName(className);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public static SourceFunction getSourceFunction(AbstractUdfStreamOperator source) {
    try {
      Field field = AbstractUdfStreamOperator.class.getDeclaredField("userFunction");
      field.setAccessible(true);
      return (SourceFunction) (field.get(source));
    } catch (IllegalAccessException | NoSuchFieldException e) {
      throw new RuntimeException(e);
    }
  }

  public static void clean(StreamExecutionEnvironment env) {
    try {
      Field field = StreamExecutionEnvironment.class.getDeclaredField("transformations");
      field.setAccessible(true);
      ((List) (field.get(env))).clear();
    } catch (IllegalAccessException | NoSuchFieldException e) {
      throw new RuntimeException(e);
    }
  }
}
