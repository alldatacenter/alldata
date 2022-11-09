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
import com.bytedance.bitsail.common.util.FastJsonUtil;
import com.bytedance.bitsail.flink.core.parser.BytesParser;
import com.bytedance.bitsail.parser.option.RowParserOptions;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.types.Row;

import java.io.UnsupportedEncodingException;

/**
 * Created at 2018/10/29.
 */
public class JsonBytesParser extends BytesParser {

  private final boolean isCaseInsensitive;

  public JsonBytesParser(BitSailConfiguration inputSliceConfig) {
    this.isCaseInsensitive = inputSliceConfig.get(RowParserOptions.CASE_INSENSITIVE);
    this.serializerFeatures =
        FastJsonUtil.parseSerializerFeaturesFromConfig(inputSliceConfig.get(RowParserOptions.JSON_SERIALIZER_FEATURES)).toArray(new SerializerFeature[0]);
    this.convertErrorColumnAsNull = inputSliceConfig.get(RowParserOptions.CONVERT_ERROR_COLUMN_AS_NULL);
  }

  public Object convertJsonKeyToLowerCase(Object obj) {
    if (obj instanceof JSONObject) {
      JSONObject jsonObj = (JSONObject) obj;
      JSONObject out = new JSONObject();
      for (Object key : jsonObj.keySet()) {
        String keyString = key.toString().toLowerCase();
        Object valueObject = jsonObj.get(key);
        out.put(keyString, convertJsonKeyToLowerCase(valueObject));
      }
      return out;
    } else if (obj instanceof JSONArray) {
      JSONArray arrayObj = (JSONArray) obj;
      JSONArray out = new JSONArray();
      for (Object elementObject : arrayObj) {
        out.add(convertJsonKeyToLowerCase(elementObject));
      }
      return out;
    } else {
      return obj;
    }
  }

  @Override
  public Row parse(Row row, byte[] bytes, int offset, int numBytes,
                   String charsetName, RowTypeInfo rowTypeInfo)
      throws BitSailException {
    try {
      String line = new String(bytes, offset, numBytes, charsetName);
      return parse(row, line, rowTypeInfo);
    } catch (UnsupportedEncodingException e) {
      throw BitSailException.asBitSailException(CommonErrorCode.UNSUPPORTED_ENCODING, e);
    }
  }

  @Override
  public Row parse(Row row, Object line, RowTypeInfo rowTypeInfo) throws BitSailException {
    String jsonLine;
    if (line instanceof byte[]) {
      jsonLine = new String((byte[]) line);
    } else {
      jsonLine = line.toString();
    }
    JSONObject jsonObj;
    if (this.isCaseInsensitive) {
      jsonObj = (JSONObject) convertJsonKeyToLowerCase(FastJsonUtil.parse(jsonLine));
    } else {
      jsonObj = (JSONObject) FastJsonUtil.parse(jsonLine);
    }

    return setRowByJson(row, rowTypeInfo, jsonObj, 0, row.getArity());
  }

  @Override
  public Row parseKvData(Row row, RowTypeInfo rowTypeInfo, byte[] keyBytes, byte[] valueBytes) {

    row.setField(0, new StringColumn(new String(keyBytes)));

    String line = new String(valueBytes);
    JSONObject jsonObj = (JSONObject) FastJsonUtil.parse(line);

    return setRowByJson(row, rowTypeInfo, jsonObj, 1, row.getArity());
  }

  protected Row setRowByJson(Row row, RowTypeInfo rowTypeInfo, JSONObject jsonObj, int beginIndex, int endIndex) {
    String colName = "";
    try {
      for (int i = beginIndex; i < endIndex; i++) {
        colName = rowTypeInfo.getFieldNames()[i];
        TypeInformation<?> typeInfo = rowTypeInfo.getTypeAt(i);

        Object fieldVal = jsonObj.get(colName);

        Column column = createColumn(typeInfo, fieldVal);
        row.setField(i, column);
      }
      return row;
    } catch (BitSailException e) {
      throw BitSailException.asBitSailException(e.getErrorCode(), String.format("column[%s] %s", colName, e.getErrorMessage()));
    }
  }
}
