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

package com.bytedance.bitsail.connector.legacy.hive.sink;

import com.bytedance.bitsail.base.dirty.impl.NoOpDirtyCollector;
import com.bytedance.bitsail.base.execution.ProcessResult;
import com.bytedance.bitsail.common.column.BooleanColumn;
import com.bytedance.bitsail.common.column.DateColumn;
import com.bytedance.bitsail.common.column.DoubleColumn;
import com.bytedance.bitsail.common.column.ListColumn;
import com.bytedance.bitsail.common.column.LongColumn;
import com.bytedance.bitsail.common.column.MapColumn;
import com.bytedance.bitsail.common.column.StringColumn;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.option.CommonOptions;
import com.bytedance.bitsail.common.util.Pair;
import com.bytedance.bitsail.connector.legacy.hive.runtime.MockStreamingRuntimeContextForTest;

import com.bytedance.bitsail.shaded.hive.client.HiveMetaClientUtil;
import com.bytedance.bitsail.shaded.hive.shim.HiveShim;
import com.bytedance.bitsail.shaded.hive.shim.HiveShimLoader;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.types.Row;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.api.SerDeInfo;
import org.apache.hadoop.hive.metastore.api.StorageDescriptor;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.sql.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.any;

/**
 * Created 2022/7/22
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({HiveMetaClientUtil.class})
@PowerMockIgnore(value = {"javax.management.*", "org.apache.hive.*"})
@SuppressStaticInitializationFor("javax.security.auth.kerberos.KeyTab")
public class HiveParquetOutputFailoverTest {
  static final BitSailConfiguration DEFAULT_CONF;

  static {
    try {
      DEFAULT_CONF =
          BitSailConfiguration.from(new File(Paths.get(HiveParquetOutputFailoverTest.class.getClassLoader().getResource("hive/hive_writer.json").toURI()).toString()));
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  Long id = 0L;
  String filePath = "file:///tmp/tablefortest/";
  String parentPath = "/tmp/tablefortest/";
  String resultFileName = "";
  String columnNames = "f_string,f_bigint,f_int,f_float,f_decimal,f_double,f_boolean,f_date,f_map,f_array";
  String columnTypes = "string:bigint:int:float:decimal:double:boolean:date:map<string,string>:array<string>";

  @Before
  public void before() throws Exception {
    File testDir = new File("/tmp/tablefortest");
    if (!testDir.exists()) {
      testDir.mkdirs();
    }

    String hadoopHome = Paths.get(HiveParquetOutputFailoverTest.class
        .getClassLoader().getResource("hadoop").toURI()).toString();

    System.setProperty("HADOOP_HOME", hadoopHome);
    System.setProperty("hadoop.home.dir", hadoopHome);
    System.setProperty("HADOOP_USER_NAME", "root");
    PowerMockito.mockStatic(HiveMetaClientUtil.class);

    PowerMockito.when(HiveMetaClientUtil.getTablePath(any(), any(), any(), any())).thenReturn(filePath);
    Pair<String, String> tableSchema = new Pair<>(columnNames, columnTypes);
    PowerMockito.when(HiveMetaClientUtil.getTableSchema(any(), any(), any())).thenReturn(tableSchema);
    Map<String, Integer> columnMappings = new HashMap<>();
    columnMappings.put("F_STRING", 0);
    columnMappings.put("F_BIGINT", 1);
    columnMappings.put("F_INT", 2);
    columnMappings.put("F_FLOAT", 3);
    columnMappings.put("F_DECIMAL", 4);
    columnMappings.put("F_DOUBLE", 5);
    columnMappings.put("F_BOOLEAN", 6);
    columnMappings.put("F_DATE", 7);
    columnMappings.put("F_MAP", 8);
    columnMappings.put("F_ARRAY", 9);
    HiveShim hiveShim = HiveShimLoader.loadHiveShim();
    PowerMockito.when(HiveMetaClientUtil.getColumnMapping(any())).thenReturn(columnMappings);
    PowerMockito.when(HiveMetaClientUtil.getHiveShim()).thenReturn(hiveShim);
    StorageDescriptor storageDescriptor = PowerMockito.mock(StorageDescriptor.class);
    PowerMockito.when(HiveMetaClientUtil.getTableFormat(any(), any(), any())).thenReturn(storageDescriptor);
    PowerMockito.when(storageDescriptor.getInputFormat()).thenReturn("org.apache.hadoop.hive.ql.io.parquet.MapredParquetInputFormat");
    PowerMockito.when(storageDescriptor.getOutputFormat()).thenReturn("org.apache.hadoop.hive.ql.io.parquet.MapredParquetOutputFormat");
    SerDeInfo serDeInfo = PowerMockito.mock(SerDeInfo.class);
    PowerMockito.when(storageDescriptor.getSerdeInfo()).thenReturn(serDeInfo);
    PowerMockito.when(serDeInfo.getSerializationLib()).thenReturn("org.apache.hadoop.hive.ql.io.parquet.serde.ParquetHiveSerDe");
    Map<String, String> hiveSerdeParams = new HashMap<>();
    PowerMockito.when(HiveMetaClientUtil.getSerdeParameters(any(), any(), any())).thenReturn(hiveSerdeParams);
    PowerMockito.when(HiveMetaClientUtil.hasPartition(any(), any(), any(), any())).thenReturn(false);
    PowerMockito.doNothing()
        .when(HiveMetaClientUtil.class, "addPartition", any(HiveConf.class), any(String.class), any(String.class), any(String.class), any(String.class), any(long.class));
    PowerMockito.doNothing().when(HiveMetaClientUtil.class, "dropPartition", any(HiveConf.class), any(String.class), any(String.class), any(String.class));
  }

  public BitSailConfiguration getCommonConf() {
    //use boe cid(5) and boe yarn cluster and queue
    return BitSailConfiguration.newDefault()
        .set(CommonOptions.INTERNAL_INSTANCE_ID, getInstanceId())
        .set(CommonOptions.USER_NAME, "root")
        .set(CommonOptions.JOB_ID, getJobId())
        .set(CommonOptions.SYNC_DDL, false);
  }

  protected String getInstanceId() {
    return String.valueOf(-System.currentTimeMillis());
  }

  protected Long getJobId() {
    return (-System.currentTimeMillis());
  }

  @Test
  public void testOutputWithFailOver() throws Exception {
    int parallelism = 2;
    HiveOutputFormat<Row> outputFormat = new HiveOutputFormat();
    outputFormat.initFromConf(getCommonConf(), DEFAULT_CONF);
    outputFormat.setEmptyMessenger();
    outputFormat.setDirtyCollector(new NoOpDirtyCollector());
    MockStreamingRuntimeContextForTest runtimeContext = new MockStreamingRuntimeContextForTest();
    runtimeContext.setAttempNum(0);
    outputFormat.setRuntimeContext(runtimeContext);
    outputFormat.configure(new Configuration());
    outputFormat.initializeGlobal(parallelism);
    outputWithFailOver(outputFormat, this::getOneFullRow, 0, 2, true);
    resultFileName = outputFormat.getCommitFileName().getName();
    File resultFile = new File(parentPath + resultFileName);
    Assert.assertTrue(resultFile.exists());
  }

  @Test
  public void testAndCompareOutputWithDiffWritableExtractor() throws Exception {
    int parallelism = 2;
    HiveOutputFormat<Row> parquetOutputFormat = new HiveOutputFormat();
    parquetOutputFormat.initFromConf(getCommonConf(), DEFAULT_CONF);
    parquetOutputFormat.setEmptyMessenger();
    parquetOutputFormat.setDirtyCollector(new NoOpDirtyCollector());
    MockStreamingRuntimeContextForTest runtimeContext1 = new MockStreamingRuntimeContextForTest();
    runtimeContext1.setAttempNum(0);
    parquetOutputFormat.setRuntimeContext(runtimeContext1);
    parquetOutputFormat.configure(new Configuration());
    parquetOutputFormat.initializeGlobal(parallelism);
    outputWithFailOver(parquetOutputFormat, this::getOneFullRow, 0, 1, false);
    File resultFileByParquetExtractor = new File(parentPath + parquetOutputFormat.getCommitFileName().getName());
    InputStream fileStreamParquet = new FileInputStream(resultFileByParquetExtractor);
    String md5Parquet = DigestUtils.md5Hex(fileStreamParquet);
    fileStreamParquet.close();

    HiveOutputFormat<Row> outputFormat = new HiveOutputFormat();
    id = 0L;
    DEFAULT_CONF.set("job.writer.writable_extractor_type", "general");
    outputFormat.initFromConf(getCommonConf(), DEFAULT_CONF);
    outputFormat.setEmptyMessenger();
    outputFormat.setDirtyCollector(new NoOpDirtyCollector());
    MockStreamingRuntimeContextForTest runtimeContext2 = new MockStreamingRuntimeContextForTest();
    runtimeContext2.setAttempNum(0);
    outputFormat.setRuntimeContext(runtimeContext2);
    outputFormat.configure(new Configuration());
    outputFormat.initializeGlobal(parallelism);
    outputWithFailOver(outputFormat, this::getOneFullRow, 0, 1, false);
    resultFileName = outputFormat.getCommitFileName().getName();
    File resultFileByGeneralExtractor = new File(parentPath + resultFileName);
    InputStream fileStreamGeneral = new FileInputStream(resultFileByGeneralExtractor);
    String md5General = DigestUtils.md5Hex(fileStreamGeneral);
    fileStreamGeneral.close();
    Assert.assertEquals(md5Parquet, md5General);
  }

  private Row getOneFullRow() {
    Row row = new Row(10);
    row.setField(0, new StringColumn("f_string"));
    row.setField(1, new LongColumn(1000000L));
    row.setField(2, new LongColumn(id++));
    row.setField(3, new DoubleColumn(1.0));
    row.setField(4, new DoubleColumn(1.256));
    row.setField(5, new DoubleColumn(1.25));
    row.setField(6, new BooleanColumn(true));
    row.setField(7, new DateColumn(Date.parse("2019/04/01 01:23:45")));
    MapColumn<StringColumn, StringColumn> mapColumn = new MapColumn<>(StringColumn.class, StringColumn.class);
    mapColumn.put(new StringColumn("key"), new StringColumn("value"));
    row.setField(8, mapColumn);
    ListColumn<StringColumn> listColumn = new ListColumn<>(StringColumn.class);
    listColumn.add(new StringColumn("ele1"));
    listColumn.add(new StringColumn("ele2"));
    listColumn.add(new StringColumn("ele3"));
    row.setField(9, listColumn);
    return row;
  }

  public void outputWithFailOver(HiveOutputFormat<Row> outputFormat, Supplier<Row> getMockRowFunction, int taskNum, int parallelism, boolean failover) throws Exception {
    outputFormat.open(taskNum, parallelism);
    if (failover) {
      //outputFormat.tryCleanupOnError();
      outputFormat.close();
      outputFormat.open(taskNum, parallelism);
    }
    for (int i = 0; i < 10; i++) {
      outputFormat.writeRecord(getMockRowFunction.get());
    }
    outputFormat.close();
    outputFormat.onSuccessComplete(ProcessResult.builder().build());
    outputFormat.onDestroy();
  }

  @After
  public void after() {
    tryDeleteFile(parentPath + "_SUCCESS");
    tryDeleteFile(parentPath + resultFileName);
    tryDeleteFile(parentPath);
  }

  public void tryDeleteFile(String deletePath) {
    File testDir = new File(deletePath);
    if (testDir.exists()) {
      testDir.delete();
    }
  }
}
