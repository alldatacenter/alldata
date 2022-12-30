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

package com.bytedance.bitsail.connector.legacy.mongodb.sink;

import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.model.ColumnInfo;
import com.bytedance.bitsail.common.util.DateUtil;
import com.bytedance.bitsail.connector.legacy.mongodb.error.MongoDBPluginsErrorCode;

import com.alibaba.fastjson.JSONObject;
import lombok.Builder;
import org.bson.BsonDouble;
import org.bson.BsonJavaScript;
import org.bson.BsonRegularExpression;
import org.bson.BsonString;
import org.bson.BsonUndefined;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Builder
public class MongoValueConverter implements Serializable {
  public static final String CUSTOM_DATE_FORMAT_KEY = "customDateFormatKey";
  public static final String TIME_ZONE = "timeZone";
  private Map<String, Object> options;

  public MongoValueConverter(Map<String, Object> options) {
    this.options = options;
  }

  public Object convert(int index, Object value, List<ColumnInfo> columnInfos) {
    String mongoType = columnInfos.get(index).getType();

    if (mongoType.startsWith("array")) {
      String elementType = "string";
      if (mongoType.endsWith(">")) {
        elementType = mongoType.substring("array".length() + 1, mongoType.length() - 1);
      }
      if (!(value instanceof List)) {
        throw BitSailException.asBitSailException(MongoDBPluginsErrorCode.UNSUPPORTED_TYPE,
            String.format("The column data type in your configuration is not support. Column index:[%d], Column type:[%s], Element type:[%s]" +
                " Please try to change the column data type or don't transmit this column.", index, mongoType, elementType)
        );
      }

      return value;
    }

    switch (mongoType) {
      case "undefined":
        value = new BsonUndefined();
        break;
      case "string":
        value = new BsonString((String) value);
        break;
      case "objectid":
        value = new ObjectId((String) value);
        break;
      case "date":
        if (!(value instanceof Date)) {
          value = DateUtil.columnToDate(value, (SimpleDateFormat) options.get(CUSTOM_DATE_FORMAT_KEY), (String) options.get(TIME_ZONE));
        }
        break;
      case "timestamp":
        if (!(value instanceof Timestamp)) {
          value = DateUtil.columnToTimestamp(value, (SimpleDateFormat) options.get(CUSTOM_DATE_FORMAT_KEY), (String) options.get(TIME_ZONE));
        }
        break;
      case "bindata":
        if (value instanceof String) {
          value = ((String) value).getBytes();
        }
        break;
      case "bool":
        if (value instanceof String) {
          value = Boolean.parseBoolean((String) value);
        }
        break;
      case "int":
        if (value instanceof String) {
          value = Integer.parseInt((String) value);
        }
        break;
      case "long":
        if (value instanceof String) {
          value = Long.parseLong((String) value);
        }
        break;
      case "object":
        if (value instanceof String) {
          value = JSONObject.parse((String) value);
        }
        break;
      case "javascript":
        if (value instanceof String) {
          value = new BsonJavaScript((String) value);
        }
        break;
      case "regex":
        if (value instanceof String) {
          value = new BsonRegularExpression((String) value);
        }
        break;
      case "double":
        if (value instanceof String) {
          value = new BsonDouble(Double.parseDouble((String) value));
        } else {
          value = new BsonDouble((Double) value);
        }
        break;
      case "decimal":
        if (value instanceof String) {
          value = Decimal128.parse((String) value);
        } else {
          value = new Decimal128((BigDecimal) value);
        }
        break;
      default:
        throw BitSailException.asBitSailException(MongoDBPluginsErrorCode.UNSUPPORTED_TYPE,
            String.format("The column data type in your configuration is not support. Column index:[%d], Column type:[%s]" +
                " Please try to change the column data type or don't transmit this column.", index, mongoType)
        );
    }
    return value;
  }
}

