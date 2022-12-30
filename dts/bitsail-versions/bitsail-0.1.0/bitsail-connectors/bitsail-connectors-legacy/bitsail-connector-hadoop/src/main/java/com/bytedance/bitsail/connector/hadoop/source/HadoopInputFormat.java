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

package com.bytedance.bitsail.connector.hadoop.source;

import com.bytedance.bitsail.batch.parser.row.TextRowBuilder;
import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.model.ColumnInfo;
import com.bytedance.bitsail.common.type.BitSailTypeInfoConverter;
import com.bytedance.bitsail.component.format.api.RowBuilder;
import com.bytedance.bitsail.connector.hadoop.common.TextInputFormatErrorCode;
import com.bytedance.bitsail.connector.hadoop.option.HadoopReaderOptions;
import com.bytedance.bitsail.flink.core.typeutils.ColumnFlinkTypeInfoUtil;

import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.ResultTypeQueryable;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.types.Row;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.InputFormat;
import org.apache.hadoop.mapred.JobConf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.IntStream;

/**
 * @desc: Wrapper for using HadoopInputFormats (mapred-variant) with Flink.
 */
public class HadoopInputFormat<K, V> extends
    HadoopInputFormatBasePlugin<K, V, Row> implements
    ResultTypeQueryable<Row> {

  private static final Logger LOG = LoggerFactory.getLogger(HadoopInputFormat.class);

  private static final long serialVersionUID = 1L;

  protected RowTypeInfo rowTypeInfo;
  protected int[] fieldIndex;
  protected RowBuilder rowBuilder;

  public HadoopInputFormat() {
    super(new JobConf());
  }

  public HadoopInputFormat(InputFormat<K, V> mapredInputFormat,
                           Class<K> key, Class<V> value, JobConf job) {
    super(mapredInputFormat, key, value, job);
  }

  public HadoopInputFormat(org.apache.hadoop.mapred.InputFormat<K, V> mapredInputFormat, Class<K> key, Class<V> value) {
    super(mapredInputFormat, key, value, new JobConf());
  }

  private static int[] createFieldIndexes(List<ColumnInfo> columnInfos) {
    return IntStream.range(0, columnInfos.size()).toArray();
  }

  @Override
  public void initPlugin() throws Exception {
    String[] paths = inputSliceConfig.getNecessaryOption(HadoopReaderOptions.PATH_LIST, TextInputFormatErrorCode.REQUIRED_VALUE).split(",");

    this.rowBuilder = new TextRowBuilder(inputSliceConfig);

    //todo spi format api.
    String inputClassName = inputSliceConfig.get(HadoopReaderOptions.HADOOP_INPUT_FORMAT_CLASS);
    Class<?> inputClass = Class.forName(inputClassName);
    this.mapredInputFormat = (InputFormat<K, V>) inputClass.newInstance();

    setMapredInputJobConf();

    for (String path : paths) {
      FileInputFormat.addInputPath(this.jobConf, new Path(path));
    }

    List<ColumnInfo> columnInfos = inputSliceConfig.getNecessaryOption(HadoopReaderOptions.COLUMNS, TextInputFormatErrorCode.REQUIRED_VALUE);
    this.rowTypeInfo = ColumnFlinkTypeInfoUtil.getRowTypeInformation(new BitSailTypeInfoConverter(), columnInfos);
    this.fieldIndex = createFieldIndexes(columnInfos);

    LOG.info("Row Type Info: " + rowTypeInfo);
  }

  @Override
  public String getType() {
    return "Hadoop";
  }

  @Override
  public Row buildRow(Row reuse, String mandatoryEncoding) throws BitSailException {
    rowBuilder.build(value, reuse, mandatoryEncoding, rowTypeInfo, fieldIndex);

    return reuse;
  }

  @Override
  public TypeInformation<Row> getProducedType() {
    return rowTypeInfo;
  }
}
