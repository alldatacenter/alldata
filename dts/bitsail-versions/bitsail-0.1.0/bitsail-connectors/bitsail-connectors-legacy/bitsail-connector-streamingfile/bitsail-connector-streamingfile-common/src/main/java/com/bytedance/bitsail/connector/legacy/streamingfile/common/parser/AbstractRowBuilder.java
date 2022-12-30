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
import com.bytedance.bitsail.common.exception.CommonErrorCode;
import com.bytedance.bitsail.common.model.ColumnInfo;
import com.bytedance.bitsail.common.type.filemapping.FileMappingTypeInfoConverter;
import com.bytedance.bitsail.common.util.FieldPathUtils;
import com.bytedance.bitsail.common.util.TypeConvertUtil;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.validator.StreamingFileSystemValidator;
import com.bytedance.bitsail.dump.datasink.file.parser.BytesParser;
import com.bytedance.bitsail.flink.core.typeutils.ColumnFlinkTypeInfoUtil;
import com.bytedance.bitsail.parser.error.ParserErrorCode;

import com.google.common.collect.Lists;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.types.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.List;

/**
 * @class: AbstractRowBuilder
 * @desc:
 **/
public abstract class AbstractRowBuilder implements Serializable {
  private static final Logger LOG = LoggerFactory.getLogger(AbstractRowBuilder.class);

  private static final List<String> PARSER_SUPPORT_DUMP_TYPE = Lists.newArrayList(
      StreamingFileSystemValidator.HDFS_DUMP_TYPE_JSON,
      StreamingFileSystemValidator.HDFS_DUMP_TYPE_PB);

  private String dumpType;
  private BytesParser bytesParser;

  public AbstractRowBuilder(BitSailConfiguration jobConf, List<ColumnInfo> columnInfos, String dumpType) throws Exception {
    this.dumpType = dumpType;
    if (PARSER_SUPPORT_DUMP_TYPE.contains(dumpType)) {
      this.bytesParser = BytesParseFactory.initBytesParser(jobConf, dumpType, columnInfos);
    }
  }

  public static RowTypeInfo getBitSailRowTypeInfo(List<ColumnInfo> columns, TypeConvertUtil.StorageEngine storageType) {
    return ColumnFlinkTypeInfoUtil.getRowTypeInformation(new FileMappingTypeInfoConverter(storageType.name()), columns);
  }

  /**
   * return the parsed row, and return the value of given field.
   */
  public abstract Tuple2<Row, Object> parseBitSailRowAndEventTime(byte[] rowData, List<FieldPathUtils.PathInfo> pathInfos) throws BitSailException;

  public Tuple2<Row, Object> build(byte[] value, Row reuse, String mandatoryEncoding, RowTypeInfo rowTypeInfo, List<FieldPathUtils.PathInfo> pathInfos)
      throws BitSailException {
    switch (dumpType) {
      case StreamingFileSystemValidator.HDFS_DUMP_TYPE_JSON:
      case StreamingFileSystemValidator.HDFS_DUMP_TYPE_PB:
        return buildRowWithParser(value, reuse, mandatoryEncoding, rowTypeInfo, bytesParser, pathInfos);
      case StreamingFileSystemValidator.HDFS_DUMP_TYPE_TEXT:
        return buildPlainTextRow(new String(value), reuse, mandatoryEncoding, rowTypeInfo, pathInfos);
      default:
        throw BitSailException.asBitSailException(CommonErrorCode.UNSUPPORTED_ENCODING, dumpType + " not supported");
    }
  }

  /**
   * text file is a plain-text file which does not need a parser to parse
   */
  private Tuple2<Row, Object> buildPlainTextRow(String value, Row reuse, String mandatoryEncoding, RowTypeInfo rowTypeInfo, List<FieldPathUtils.PathInfo> pathInfos)
      throws BitSailException {
    try {
      reuse.setField(0, value);
      return new Tuple2<>(reuse, null);
    } catch (Exception e) {
      throw BitSailException.asBitSailException(ParserErrorCode.ILLEGAL_TEXT, "value: " + value, e);
    }
  }

  /**
   * text file is a json/protobuf file which needs a parser to parse
   */
  private Tuple2<Row, Object> buildRowWithParser(byte[] value, Row reuse, String mandatoryEncoding, RowTypeInfo rowTypeInfo, BytesParser bytesParser,
                                                 List<FieldPathUtils.PathInfo> pathInfos)
      throws BitSailException {
    try {
      return bytesParser.parse(reuse, value, rowTypeInfo, pathInfos);
    } catch (Exception e) {
      throw BitSailException.asBitSailException(ParserErrorCode.ILLEGAL_TEXT, "value: " + new String(value), e);
    }
  }
}
