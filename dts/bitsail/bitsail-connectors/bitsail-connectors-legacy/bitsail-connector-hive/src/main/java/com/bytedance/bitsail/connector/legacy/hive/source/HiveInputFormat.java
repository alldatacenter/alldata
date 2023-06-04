/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytedance.bitsail.connector.legacy.hive.source;

import com.bytedance.bitsail.base.execution.ProcessResult;
import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.ddl.source.SourceEngineConnector;
import com.bytedance.bitsail.common.exception.CommonErrorCode;
import com.bytedance.bitsail.common.exception.FrameworkErrorCode;
import com.bytedance.bitsail.common.model.ColumnInfo;
import com.bytedance.bitsail.common.type.filemapping.HiveTypeInfoConverter;
import com.bytedance.bitsail.common.util.JsonSerializer;
import com.bytedance.bitsail.common.util.Pair;
import com.bytedance.bitsail.component.format.api.RowBuilder;
import com.bytedance.bitsail.component.format.hive.HiveGeneralRowBuilder;
import com.bytedance.bitsail.connector.hadoop.source.HadoopInputFormatBasePlugin;
import com.bytedance.bitsail.connector.legacy.hive.common.HiveSourceEngineConnector;
import com.bytedance.bitsail.connector.legacy.hive.option.HiveReaderOptions;
import com.bytedance.bitsail.flink.core.typeutils.ColumnFlinkTypeInfoUtil;

import com.bytedance.bitsail.shaded.hive.client.HiveMetaClientUtil;

import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.ResultTypeQueryable;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.types.Row;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.api.StorageDescriptor;
import org.apache.hadoop.io.ArrayWritable;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.InputFormat;
import org.apache.hadoop.mapred.JobConf;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@Slf4j
public class HiveInputFormat extends HadoopInputFormatBasePlugin<Void, ArrayWritable, Row> implements
    ResultTypeQueryable<Row> {

  private static final Logger LOG = LoggerFactory.getLogger(HiveInputFormat.class);

  private static final long serialVersionUID = 1L;

  private RowBuilder rowBuilder;
  private RowTypeInfo rowTypeInfo;

  private String db;
  private String table;
  private String partition;

  public HiveInputFormat() {
    super(new JobConf());
  }

  private static Map<String, Integer> getMappingFromMetastore(HiveConf hiveConf, BitSailConfiguration inputSliceConfig) throws TException {

    String db = inputSliceConfig.getNecessaryOption(HiveReaderOptions.DB_NAME, FrameworkErrorCode.REQUIRED_VALUE);
    String table = inputSliceConfig.getNecessaryOption(HiveReaderOptions.TABLE_NAME, FrameworkErrorCode.REQUIRED_VALUE);

    Pair<String, String> hiveTableSchema = HiveMetaClientUtil.getTableSchema(hiveConf, db, table);

    return HiveMetaClientUtil.getColumnMapping(hiveTableSchema);
  }

  @Override
  public Row buildRow(Row reuse, String mandatoryEncoding) throws BitSailException {
    rowBuilder.build(value, reuse, mandatoryEncoding, rowTypeInfo);

    return reuse;
  }

  @Override
  public void initPlugin() throws Exception {
    // Hive metastore related
    this.db = inputSliceConfig.getNecessaryOption(HiveReaderOptions.DB_NAME, FrameworkErrorCode.REQUIRED_VALUE);
    this.table = inputSliceConfig.getNecessaryOption(HiveReaderOptions.TABLE_NAME, FrameworkErrorCode.REQUIRED_VALUE);
    this.partition = inputSliceConfig.get(HiveReaderOptions.PARTITION);
    log.info("Init finished. DB: {}, table: {}, partition: {}", db, table, partition);

    HiveConf hiveConf = getHiveConf(inputSliceConfig);
    // construct row type info
    List<ColumnInfo> columnInfos;
    if (inputSliceConfig.fieldExists(HiveReaderOptions.COLUMNS)) {
      columnInfos = inputSliceConfig.getNecessaryOption(
          HiveReaderOptions.COLUMNS, FrameworkErrorCode.REQUIRED_VALUE);
    } else {
      columnInfos = HiveMetaClientUtil.getColumnInfo(hiveConf, db, table);
    }

    rowTypeInfo = ColumnFlinkTypeInfoUtil.getRowTypeInformation(new HiveTypeInfoConverter(), columnInfos);
    LOG.info("Row Type Info: " + rowTypeInfo);

    // initialize mapred input format
    super.mapredInputFormat = getMapredInputFormat(commonConfig, inputSliceConfig);

    this.rowBuilder = new HiveGeneralRowBuilder(getMappingFromMetastore(hiveConf, inputSliceConfig),
        db,
        table,
        JsonSerializer.parseToMap(inputSliceConfig.get(HiveReaderOptions.HIVE_METASTORE_PROPERTIES)));

    setMapredInputJobConf();

    List<String> inputPath = HiveMetaClientUtil.getPartitionPathList(getHiveConf(inputSliceConfig), db, table, partition);
    for (String path : inputPath) {
      FileInputFormat.addInputPath(this.jobConf, new org.apache.hadoop.fs.Path(path));
    }
  }

  @Override
  public TypeInformation<Row> getProducedType() {
    return rowTypeInfo;
  }

  @Override
  public void onSuccessComplete(ProcessResult result) throws Exception {
    super.onSuccessComplete(result);
  }

  @Override
  public String getType() {
    return "Hive";
  }

  @Override
  public SourceEngineConnector initSourceSchemaManager(BitSailConfiguration commonConf, BitSailConfiguration readerConf) {
    return new HiveSourceEngineConnector(commonConf, readerConf, getHiveConf(readerConf));
  }

  private InputFormat<Void, ArrayWritable> getMapredInputFormat(BitSailConfiguration commonConfig, BitSailConfiguration inputConf) throws Exception {
    StorageDescriptor storageDescriptor = HiveMetaClientUtil.getTableFormat(
        getHiveConf(inputConf), db, table);
    log.info("Mapred input format: {}, output format: {}, serializationLib: {}",
        storageDescriptor.getInputFormat(),
        storageDescriptor.getOutputFormat(),
        storageDescriptor.getSerdeInfo().getSerializationLib());
    return (InputFormat) Class.forName(storageDescriptor.getInputFormat(), true, Thread.currentThread().getContextClassLoader()).newInstance();

  }

  private HiveConf getHiveConf(BitSailConfiguration readerConfiguration) {
    Map<String, String> hiveProperties =
        JsonSerializer.parseToMap(readerConfiguration.getNecessaryOption(HiveReaderOptions.HIVE_METASTORE_PROPERTIES, CommonErrorCode.CONFIG_ERROR));
    return HiveMetaClientUtil.getHiveConf(hiveProperties);
  }
}