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

package com.bytedance.bitsail.batch.file.parser;

import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.column.Column;
import com.bytedance.bitsail.common.column.StringColumn;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.exception.CommonErrorCode;
import com.bytedance.bitsail.common.util.FieldPathUtils;
import com.bytedance.bitsail.common.util.ProtoVisitor;
import com.bytedance.bitsail.common.util.ProtobufUtil;
import com.bytedance.bitsail.flink.core.parser.BytesParser;
import com.bytedance.bitsail.parser.error.ParserErrorCode;
import com.bytedance.bitsail.parser.option.RowParserOptions;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.types.Row;
import org.apache.hadoop.io.BytesWritable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.Map;

import static com.google.protobuf.Descriptors.Descriptor;

public class PbBytesParser extends BytesParser {
  private static final Logger LOGGER = LoggerFactory.getLogger(PbBytesParser.class);

  private final String columnDelimiter;
  private final String protoClassName;
  private final byte[] descContent;
  private final int contentOffset;
  @Getter
  private final ProtoVisitor.FeatureContext context;

  private transient Descriptor descriptor = null;

  public PbBytesParser(BitSailConfiguration inputSliceConfig) throws Exception {
    //base64ProtoDefine is stored as a base64 string
    this.descContent = Base64.getDecoder().decode(inputSliceConfig.getNecessaryOption(RowParserOptions.PROTO_DESCRIPTOR, ParserErrorCode.REQUIRED_VALUE));
    this.protoClassName = inputSliceConfig.get(RowParserOptions.PROTO_CLASS_NAME);

    this.columnDelimiter = inputSliceConfig.get(RowParserOptions.COLUMN_DELIMITER);
    this.contentOffset = inputSliceConfig.get(RowParserOptions.CONTENT_OFFSET);

    Map<String, String> features = inputSliceConfig
        .getUnNecessaryOption(RowParserOptions.PB_FEATURE, null);
    this.context = ProtoVisitor.genFeatureContext(features);
    convertErrorColumnAsNull = inputSliceConfig.getUnNecessaryOption(RowParserOptions.CONVERT_ERROR_COLUMN_AS_NULL, true);
  }

  public Descriptor getDescriptor() throws Exception {
    if (this.descriptor == null) {
      this.descriptor = ProtobufUtil.getDescriptor(descContent, protoClassName);
    }
    return this.descriptor;

  }

  @Override
  public Row parse(Row row, byte[] bytes, int offset, int numBytes, String charsetName, RowTypeInfo rowTypeInfo) throws BitSailException {

    try {
      Descriptor descriptor = getDescriptor();

      CodedInputStream byteArrayInputStream = CodedInputStream.newInstance(bytes, offset, numBytes);
      DynamicMessage message = DynamicMessage.parseFrom(descriptor, byteArrayInputStream);
      return parse(row, message, rowTypeInfo);
    } catch (Exception e) {
      throw BitSailException.asBitSailException(CommonErrorCode.UNSUPPORTED_ENCODING, e);
    }
  }

  @Override
  public Row parse(Row row, Object line, RowTypeInfo rowTypeInfo) throws BitSailException {
    BytesWritable pbLine = (BytesWritable) line;
    return parse(row, pbLine.getBytes(), contentOffset, pbLine.getLength() - contentOffset, "UTF-8", rowTypeInfo);
  }

  private Row parse(Row row, DynamicMessage message, RowTypeInfo rowTypeInfo) throws BitSailException, InvalidProtocolBufferException {
    for (int i = 0; i < row.getArity(); i++) {
      setIndexOfField(row, message, rowTypeInfo, i);
    }

    return row;
  }

  @Override
  public Row parseKvData(Row row, RowTypeInfo rowTypeInfo, byte[] keyBytes, byte[] valueBytes) throws Exception {
    Descriptor descriptor = getDescriptor();

    row.setField(0, new StringColumn(new String(keyBytes)));

    DynamicMessage message = DynamicMessage.parseFrom(descriptor, valueBytes);

    for (int i = 1; i < row.getArity(); i++) {
      setIndexOfField(row, message, rowTypeInfo, i);
    }

    return row;
  }

  private void setIndexOfField(Row row, DynamicMessage message, RowTypeInfo rowTypeInfo, int index) {
    String colName = rowTypeInfo.getFieldNames()[index];

    TypeInformation<?> typeInfo = rowTypeInfo.getTypeAt(index);

    Object fieldVal = null;
    try {
      fieldVal = ProtoVisitor.getPathValue(message, FieldPathUtils.getPathInfo(colName), context);
    } catch (Exception e) {
      if (!convertErrorColumnAsNull) {
        throw e;
      }
      LOGGER.info("transform to null from invalid field: " + colName, e);
    }

    Column column = createColumn(typeInfo, fieldVal);
    row.setField(index, column);
  }
}
