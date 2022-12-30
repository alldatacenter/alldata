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

package com.bytedance.bitsail.conversion.hive;

import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.column.BytesColumn;
import com.bytedance.bitsail.common.column.Column;
import com.bytedance.bitsail.common.column.DateColumn;
import com.bytedance.bitsail.common.exception.CommonErrorCode;
import com.bytedance.bitsail.conversion.hive.extractor.ParquetWritableExtractor.RawValueToWritableConverter;
import com.bytedance.bitsail.conversion.hive.extractor.StringText;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;

import java.io.Serializable;
import java.util.Objects;

@Builder
@Data
@AllArgsConstructor
public class ConvertToHiveObjectOptions implements Serializable {
  /*
   * Convert to null when failing to convert basic value type
   */
  private boolean convertErrorColumnAsNull;

  /*
   * Whether convert to string type as long value when read date type as string
   */
  private boolean dateTypeToStringAsLong;

  /*
   * Whether convert to string type as null when read null string
   */
  private boolean nullStringAsNull;

  /**
   * Whether convert string column to StringText
   */
  private boolean useStringText;

  /*
   * Indicate the datePrecision of string type as long value as date type
   * SECOND: 10 numbers of timestamp
   * MILLISECOND: 13 numbers of timestamp
   */
  private DatePrecision datePrecision;

  /**
   * there are a few different handling logic between dump and batch job
   */
  @Builder.Default
  private JobType jobType = JobType.BATCH;

  @SuppressWarnings("checkstyle:MagicNumber")
  public static Object toIntOrBigintHiveObject(Column column, boolean intOrBigInt, ConvertToHiveObjectOptions options) {
    Long longValue = column.asLong();

    if (column instanceof DateColumn) {
      if (longValue == null) {
        return null;
      }
      //todo specify precision
      if (options.getDatePrecision() != ConvertToHiveObjectOptions.DatePrecision.MILLISECOND) {
        longValue /= 1000;
      }
    }

    if (intOrBigInt) {
      final int toIntExact;
      try {
        toIntExact = Math.toIntExact(longValue);
      } catch (Exception e) {
        throw BitSailException.asBitSailException(CommonErrorCode.CONVERT_OVER_FLOW,
            String.format("[%d], long convert to int overflow.", longValue));
      }
      return toIntExact;
    } else {
      return longValue;
    }
  }

  // todo: merge dump and batch convert method
  public static Writable convertStringToWritable(Column column, ConvertToHiveObjectOptions convertOptions) {
    switch (convertOptions.getJobType()) {
      case BATCH:
        return RawValueToWritableConverter.createStringAsBytes(
            convertToString(column, convertOptions), true);
      default:
        if (column.getRawData() == null && convertOptions.isNullStringAsNull()) {
          return RawValueToWritableConverter.createNull();
        }
        if (column instanceof BytesColumn) {
          return RawValueToWritableConverter.createBinary(column.asBytes());
        } else {
          if (convertOptions.isUseStringText()) {
            String value = column.asString();
            return Objects.isNull(value) ? new Text() : new StringText(value);
          }
          return RawValueToWritableConverter.createStringAsBytes(column.asString(), false);
        }
    }
  }

  @SuppressWarnings("checkstyle:MagicNumber")
  public static String convertToString(Column column, ConvertToHiveObjectOptions options) {
    if (column.getRawData() == null) {
      if (options.isNullStringAsNull()) {
        return null;
      }
      return "";
    }

    if (options.isDateTypeToStringAsLong() && column instanceof DateColumn && ((DateColumn) column).getSubType().equals(DateColumn.DateType.DATETIME)) {
      if (options.getDatePrecision() == ConvertToHiveObjectOptions.DatePrecision.MILLISECOND) {
        return String.valueOf((column).asLong());
      } else {
        return String.valueOf((column).asLong() / 1000);
      }
    }
    return (column).asString();
  }

  public static enum DatePrecision {
    SECOND, MILLISECOND
  }

  // todo: merge batch and dump hive parquet extractor
  public static enum JobType {
    BATCH, DUMP
  }
}
