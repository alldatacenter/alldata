/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.store.hive.readers;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.ops.ExecutorFragmentContext;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.store.RecordReader;
import org.apache.drill.exec.store.hive.HivePartition;
import org.apache.drill.exec.store.hive.HiveSubScan;
import org.apache.drill.exec.store.hive.HiveTableWithColumnCache;
import org.apache.drill.exec.util.ImpersonationUtil;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.mapred.InputSplit;
import org.apache.hadoop.mapred.TextInputFormat;
import org.apache.hadoop.security.UserGroupInformation;

/**
 * Factory for creation of Hive record readers used by {@link org.apache.drill.exec.store.hive.HiveScanBatchCreator}.
 */
public class ReadersInitializer {

  private static final String TEXT_FORMAT = TextInputFormat.class.getCanonicalName();

  /**
   * Selects reader constructor reference as {@link HiveReaderFactory} readerFactory.
   * Then check if input splits are empty creates empty record reader, or one reader per split otherwise.
   *
   * @param ctx    context related to fragment
   * @param config context which holds different Hive configurations
   * @return list containing one or more readers
   */
  public static List<RecordReader> init(ExecutorFragmentContext ctx, HiveSubScan config) {
    final HiveReaderFactory readerFactory = getReaderFactory(config);
    final UserGroupInformation proxyUgi = ImpersonationUtil.createProxyUgi(config.getUserName(), ctx.getQueryUserName());
    final List<List<InputSplit>> inputSplits = config.getInputSplits();
    final HiveConf hiveConf = config.getHiveConf();

    if (inputSplits.isEmpty()) {
      return Collections.singletonList(
          readerFactory.createReader(config.getTable(), null /*partition*/, null /*split*/, config.getColumns(), ctx, hiveConf, proxyUgi)
      );
    } else {
      IndexedPartitions partitions = getPartitions(config);
      return IntStream.range(0, inputSplits.size())
          .mapToObj(idx ->
              readerFactory.createReader(
                  config.getTable(),
                  partitions.get(idx),
                  inputSplits.get(idx),
                  config.getColumns(),
                  ctx, hiveConf, proxyUgi))
          .collect(Collectors.toList());
    }
  }

  /**
   * Returns reference to {@link HiveTextRecordReader}'s constructor if
   * file being read has text format or reference to {@link HiveDefaultRecordReader}'s
   * constructor otherwise.
   *
   * @param config context which holds different Hive configurations
   * @return reference to concrete reader constructor which is unified under type {@link HiveReaderFactory}
   */
  private static HiveReaderFactory getReaderFactory(HiveSubScan config) {
    String inputFormat = config.getTable().getSd().getInputFormat();
    return TEXT_FORMAT.equals(inputFormat) ? HiveTextRecordReader::new : HiveDefaultRecordReader::new;
  }

  /**
   * Used to select logic for partitions retrieval by index.
   * If table hasn't partitions, get by index just returns null.
   *
   * @param config context which holds different Hive configurations
   * @return get by index lambda expression unified under type {@link IndexedPartitions}
   */
  private static IndexedPartitions getPartitions(HiveSubScan config) {
    return (config.getPartitions() == null || config.getPartitions().isEmpty()) ? (idx) -> null : config.getPartitions()::get;
  }


  /**
   * Functional interface used to describe parameters accepted by concrete
   * readers constructor.
   */
  @FunctionalInterface
  private interface HiveReaderFactory {

    RecordReader createReader(HiveTableWithColumnCache table, HivePartition partition,
                              Collection<InputSplit> inputSplits, List<SchemaPath> projectedColumns,
                              FragmentContext context, HiveConf hiveConf, UserGroupInformation proxyUgi);

  }

  /**
   * Functional interface used to represent get partition
   * by index logic.
   */
  @FunctionalInterface
  private interface IndexedPartitions {

    HivePartition get(int idx);

  }

}
