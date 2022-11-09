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

package com.bytedance.bitsail.dump.datasink.file.parser;

import com.bytedance.bitsail.common.column.Column;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.model.ColumnInfo;
import com.bytedance.bitsail.common.util.FieldPathUtils;
import com.bytedance.bitsail.common.util.ProtoVisitor;
import com.bytedance.bitsail.common.util.ProtobufUtil;
import com.bytedance.bitsail.parser.option.RowParserOptions;

import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.types.Row;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created 2020/3/30.
 */
public class PbBytesParser extends BytesParser {

  private String classname;
  private String descriptorStr;
  private transient Descriptors.Descriptor descriptor;
  private Map<String, FieldPathUtils.PathInfo> pathInfos;
  private ProtoVisitor.FeatureContext context;

  public PbBytesParser(BitSailConfiguration jobConf, List<ColumnInfo> columnInfos) throws Exception {
    jobConf.getUnNecessaryOption(RowParserOptions.CONVERT_ERROR_COLUMN_AS_NULL, true);
    useArrayMapColumn = jobConf.get(RowParserOptions.USE_ARRAY_MAP_COLUMN);
    rawColumnIndex = getRawColumnIndexByName(columnInfos, jobConf.get(RowParserOptions.RAW_COLUMN).trim());
    descriptorStr = jobConf.get(RowParserOptions.PROTO_DESCRIPTOR);
    classname = jobConf.get(RowParserOptions.PROTO_CLASS_NAME);
    pathInfos = columnInfos.stream().collect(Collectors.toMap(ColumnInfo::getName,
        columnInfo -> FieldPathUtils.getPathInfo(columnInfo.getName())));
    context = genFeatureContext(jobConf);
    checkDescriptor();
  }

  private static ProtoVisitor.FeatureContext genFeatureContext(BitSailConfiguration jobConf) {
    Map<String, String> features = jobConf.getUnNecessaryMap(RowParserOptions.PB_FEATURE);
    return ProtoVisitor.genFeatureContext(features);
  }

  private void checkDescriptor() throws Exception {
    if (Objects.isNull(descriptor)) {
      this.descriptor = ProtobufUtil.getDescriptor(Base64.getDecoder().decode(descriptorStr), classname);
      if (descriptor == null) {
        throw new RuntimeException(String.format("Class name %s should exist in proto descriptor.", classname));
      }
    }
  }

  @Override
  public Row parse(Row row, byte[] bytes, RowTypeInfo rowTypeInfo) throws Exception {
    checkDescriptor();

    DynamicMessage.Builder builder = DynamicMessage.newBuilder(descriptor);
    DynamicMessage message = builder.mergeFrom(bytes).build();
    return convertToBitSailRow(row, message, rowTypeInfo, 0, row.getArity());
  }

  @Override
  public Tuple2<Row, Object> parse(Row row, byte[] bytes, RowTypeInfo rowTypeInfo, List<FieldPathUtils.PathInfo> pathInfos) throws Exception {
    checkDescriptor();

    DynamicMessage.Builder builder = DynamicMessage.newBuilder(descriptor);
    DynamicMessage message = builder.mergeFrom(bytes).build();
    row = convertToBitSailRow(row, message, rowTypeInfo, 0, row.getArity());
    if (rawColumnIndex != -1) {
      setRawColumn(row, rowTypeInfo.getTypeAt(rawColumnIndex), rawColumnIndex, bytes);
    }
    Object fieldValue = extractPathValue(message, pathInfos);
    return new Tuple2<>(row, fieldValue);
  }

  private Object extractPathValue(DynamicMessage message, List<FieldPathUtils.PathInfo> pathInfos) {
    try {
      Object fieldValue = null;
      for (FieldPathUtils.PathInfo pathInfo : pathInfos) {
        fieldValue = ProtoVisitor.getPathValue(message, pathInfo);
        if (Objects.nonNull(fieldValue)) {
          break;
        }
      }
      return fieldValue;
    } catch (Throwable t) {
      return null;
    }
  }

  private Row convertToBitSailRow(Row row, DynamicMessage message, RowTypeInfo rowTypeInfo,
                                  int startIndex, int endIndex) throws Exception {

    for (int index = startIndex; index < endIndex; index++) {
      TypeInformation<?> typeInformation = rowTypeInfo.getTypeAt(index);
      String fieldName = (rowTypeInfo.getFieldNames())[index];
      FieldPathUtils.PathInfo fieldPathInfo = pathInfos.get(fieldName);
      Object value = ProtoVisitor.getPathValue(message, fieldPathInfo, context);

      Column column = createColumn(typeInformation, value);
      row.setField(index, column);
    }
    return row;
  }
}
