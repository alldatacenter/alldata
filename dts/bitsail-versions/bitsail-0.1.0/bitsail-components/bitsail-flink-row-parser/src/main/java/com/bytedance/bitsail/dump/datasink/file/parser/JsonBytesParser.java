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

import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.column.Column;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.model.ColumnInfo;
import com.bytedance.bitsail.common.util.FastJsonUtil;
import com.bytedance.bitsail.common.util.FieldPathUtils;
import com.bytedance.bitsail.common.util.JsonVisitor;
import com.bytedance.bitsail.parser.option.RowParserOptions;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.types.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @class: JsonBytesParser
 * @desc:
 **/
public class JsonBytesParser extends BytesParser {
  private static final Logger LOG = LoggerFactory.getLogger(JsonBytesParser.class);

  private boolean isCaseInsensitive;
  private Map<String, FieldPathUtils.PathInfo> pathInfos;

  public JsonBytesParser(BitSailConfiguration jobConf, List<ColumnInfo> columnInfos) {
    this.isCaseInsensitive = jobConf.get(RowParserOptions.CASE_INSENSITIVE);
    this.convertErrorColumnAsNull = jobConf.get(RowParserOptions.CONVERT_ERROR_COLUMN_AS_NULL);
    this.useArrayMapColumn = jobConf.get(RowParserOptions.USE_ARRAY_MAP_COLUMN);
    this.rawColumnIndex = getRawColumnIndexByName(columnInfos, jobConf.get(RowParserOptions.RAW_COLUMN).trim());
    pathInfos = columnInfos.stream().collect(Collectors.toMap(ColumnInfo::getName,
        columnInfo -> FieldPathUtils.getPathInfo(columnInfo.getName())));
    this.serializerFeatures =
        FastJsonUtil.parseSerializerFeaturesFromConfig(jobConf.get(RowParserOptions.JSON_SERIALIZER_FEATURES)).toArray(new SerializerFeature[0]);
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
      for (int i = 0; i < arrayObj.size(); i++) {
        Object elementObject = arrayObj.get(i);
        out.add(convertJsonKeyToLowerCase(elementObject));
      }
      return out;
    } else {
      return obj;
    }
  }

  protected JSONObject parseJSON(byte[] bytes) {
    String jsonLine = new String(bytes);
    JSONObject jsonObj = null;
    if (this.isCaseInsensitive) {
      jsonObj = (JSONObject) convertJsonKeyToLowerCase(FastJsonUtil.parse(jsonLine));
    } else {
      jsonObj = (JSONObject) FastJsonUtil.parse(jsonLine);
    }
    return jsonObj;
  }

  @Override
  public Row parse(Row row, byte[] bytes, RowTypeInfo rowTypeInfo) throws Exception {
    JSONObject jsonObj = parseJSON(bytes);
    return setRowByJson(row, rowTypeInfo, jsonObj, 0, row.getArity());
  }

  @Override
  public Tuple2<Row, Object> parse(Row row, byte[] bytes, RowTypeInfo rowTypeInfo, List<FieldPathUtils.PathInfo> pathInfos) throws BitSailException {
    JSONObject jsonObj = parseJSON(bytes);
    row = setRowByJson(row, rowTypeInfo, jsonObj, 0, row.getArity());
    if (rawColumnIndex != -1) {
      setRawColumn(row, rowTypeInfo.getTypeAt(rawColumnIndex), rawColumnIndex, bytes);
    }
    Object fieldValue = extractFieldValue(jsonObj, pathInfos);
    return new Tuple2<>(row, fieldValue);
  }

  protected Object extractFieldValue(JSONObject record, List<FieldPathUtils.PathInfo> pathInfos) {

    Object fieldValue = null;
    for (FieldPathUtils.PathInfo pathInfo : pathInfos) {
      try {
        fieldValue = JsonVisitor.getPathValue(record, pathInfo);
        if (Objects.nonNull(fieldValue)) {
          break;
        }
      } catch (Exception e) {
        LOG.error("Path info: {} parse failed.", pathInfo.getName(), e);
      }
    }
    return fieldValue;

  }

  private Row setRowByJson(Row row, RowTypeInfo rowTypeInfo, JSONObject jsonObj, int beginIndex, int endIndex) {
    String colName = "";
    try {
      for (int i = beginIndex; i < endIndex; i++) {
        colName = rowTypeInfo.getFieldNames()[i];
        TypeInformation typeInfo = rowTypeInfo.getTypeAt(i);

        FieldPathUtils.PathInfo pathInfo = pathInfos.get(colName);
        Object fieldVal = JsonVisitor.getPathValue(jsonObj, pathInfo);

        Column column = createColumn(typeInfo, fieldVal);
        row.setField(i, column);
      }
      return row;
    } catch (BitSailException e) {
      throw BitSailException.asBitSailException(e.getErrorCode(), String.format("column[%s] %s", colName, e.getErrorMessage()));
    }
  }
}
