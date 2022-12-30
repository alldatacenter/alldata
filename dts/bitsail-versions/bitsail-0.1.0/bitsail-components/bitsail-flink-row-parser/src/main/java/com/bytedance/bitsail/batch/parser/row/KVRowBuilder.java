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

package com.bytedance.bitsail.batch.parser.row;

import com.bytedance.bitsail.base.enumerate.ContentType;
import com.bytedance.bitsail.batch.file.parser.BytesParseFactory;
import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.column.BytesColumn;
import com.bytedance.bitsail.common.column.Column;
import com.bytedance.bitsail.common.column.StringColumn;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.exception.CommonErrorCode;
import com.bytedance.bitsail.component.format.api.RowBuilder;
import com.bytedance.bitsail.flink.core.parser.BytesParser;

import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.types.Row;

abstract class KVRowBuilder implements RowBuilder {

  private ContentType contentType;
  private BytesParser bytesParser;

  public KVRowBuilder(BitSailConfiguration inputSliceConfig) throws Exception {
    this.contentType = ContentType.valueOf(getContentType(inputSliceConfig).toUpperCase());

    if (contentType == ContentType.PROTOBUF || contentType == ContentType.JSON) {
      this.bytesParser = BytesParseFactory.initBytesParser(inputSliceConfig);
    }
  }

  @Override
  public void build(Object value, Row reuse, String mandatoryEncoding, RowTypeInfo rowTypeInfo) throws BitSailException {
    switch (contentType) {
      case JSON:
      case PROTOBUF:
        buildRowWithParser(value, reuse, mandatoryEncoding, rowTypeInfo, bytesParser);
        break;
      case PLAIN:
        buildTextRow(value, reuse, mandatoryEncoding, rowTypeInfo);
        break;
      default:
        throw BitSailException.asBitSailException(CommonErrorCode.UNSUPPORTED_ENCODING, contentType + " not supported");
    }
  }

  /**
   * key and value is byte[] data, which value is encoded as plaintext
   *
   * @param reuse
   */
  private void buildTextRow(Object obj, Row reuse, String mandatoryEncoding, RowTypeInfo rowTypeInfo) {
    try {
      byte[] key = getKey(obj);
      byte[] value;
      try {
        value = getValue(obj);
      } catch (Exception e) {
        value = null;
      }

      TypeInformation keyTypeInfo = rowTypeInfo.getTypeAt(0);
      TypeInformation valueTypeInfo = rowTypeInfo.getTypeAt(1);

      Column keyColumn = keyTypeInfo.getTypeClass() == BytesColumn.class ? new BytesColumn(key) : new StringColumn(new String(key));
      Column valueColumn = valueTypeInfo.getTypeClass() == BytesColumn.class ? new BytesColumn(value)
          : new StringColumn(value != null ? new String(value) : null);

      reuse.setField(0, keyColumn);
      reuse.setField(1, valueColumn);
    } catch (Exception e) {
      throwBuildException(e);
    }
  }

  /**
   * key and value is byte[] data, which supports protobuf or json format
   *
   * @param reuse
   */
  private void buildRowWithParser(Object obj, Row reuse, String mandatoryEncoding, RowTypeInfo rowTypeInfo, BytesParser bytesParser) {
    try {
      byte[] key = getKey(obj);
      byte[] value = getValue(obj);
      bytesParser.parseKvData(reuse, rowTypeInfo, key, value);
    } catch (Exception e) {
      throwBuildException(e);
    }
  }

  /*
   * get key of object, need to be implemented
   */
  protected abstract String getContentType(BitSailConfiguration inputSliceConfig);

  /*
   * get key of object, need to be implemented
   */
  protected abstract byte[] getKey(Object obj);

  /*
   * get value of object, need to be implemented
   */
  protected abstract byte[] getValue(Object obj);

  /*
   * fail to parse key and value, throw Exception
   */
  protected abstract void throwBuildException(Exception e);
}
