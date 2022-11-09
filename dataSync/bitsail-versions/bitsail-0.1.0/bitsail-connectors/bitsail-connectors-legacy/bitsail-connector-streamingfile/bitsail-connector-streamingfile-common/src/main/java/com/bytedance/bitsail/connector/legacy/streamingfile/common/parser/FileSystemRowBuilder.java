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

package com.bytedance.bitsail.connector.legacy.streamingfile.common.parser;

import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.model.ColumnInfo;
import com.bytedance.bitsail.common.util.FieldPathUtils;
import com.bytedance.bitsail.common.util.TypeConvertUtil.StorageEngine;

import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.types.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @class: HiveRowBuilder
 * @desc:
 **/
public class FileSystemRowBuilder extends AbstractRowBuilder {
  private static final Logger LOG = LoggerFactory.getLogger(FileSystemRowBuilder.class);

  protected RowTypeInfo sourceRowTypeInfo;

  public FileSystemRowBuilder(BitSailConfiguration jobConf, List<ColumnInfo> columnInfos, String dumpType) throws Exception {
    super(jobConf, columnInfos, dumpType);

    LOG.info("Source Row Type Info: {}.", sourceRowTypeInfo);
    sourceRowTypeInfo = getBitSailRowTypeInfo(columnInfos, StorageEngine.hive);
  }

  @Override
  public Tuple2<Row, Object> parseBitSailRowAndEventTime(byte[] record, List<FieldPathUtils.PathInfo> pathInfos) throws BitSailException {
    Row bitSailRow = new Row(sourceRowTypeInfo.getArity());
    return build(record, bitSailRow, "", sourceRowTypeInfo, pathInfos);
  }

}
