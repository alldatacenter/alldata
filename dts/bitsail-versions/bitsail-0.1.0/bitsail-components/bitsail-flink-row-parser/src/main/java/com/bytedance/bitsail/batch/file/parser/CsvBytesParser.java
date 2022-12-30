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
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.exception.CommonErrorCode;
import com.bytedance.bitsail.flink.core.parser.BytesParser;
import com.bytedance.bitsail.parser.option.RowParserOptions;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.types.Row;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.stream.IntStream;

public class CsvBytesParser extends BytesParser {
  private final String csvDelimiter;
  private final Character csvMultiDelimiterReplaceChar;

  private final CSVFormat csvFormat;

  public CsvBytesParser(BitSailConfiguration inputSliceConfig) {
    this.csvDelimiter = inputSliceConfig.get(RowParserOptions.CSV_DELIMITER);
    this.csvMultiDelimiterReplaceChar = inputSliceConfig.get(RowParserOptions.CSV_MULTI_DELIMITER_REPLACER);

    Character csvEscape = inputSliceConfig.getUnNecessaryOption(RowParserOptions.CSV_ESCAPE, null);
    Character csvQuote = inputSliceConfig.getUnNecessaryOption(RowParserOptions.CSV_QUOTE, null);
    String csvNullString = inputSliceConfig.getUnNecessaryOption(RowParserOptions.CSV_WITH_NULL_STRING, null);
    char csvFormatDelimiter = csvDelimiter.length() > 1 ? csvMultiDelimiterReplaceChar
        : csvDelimiter.charAt(0);
    this.csvFormat = CSVFormat.DEFAULT
        .withDelimiter(csvFormatDelimiter)
        .withEscape(csvEscape)
        .withQuote(csvQuote)
        .withNullString(csvNullString);
  }

  @Override
  public Row parse(Row row, byte[] bytes, int offset, int numBytes, String charsetName, RowTypeInfo rowTypeInfo) throws Exception {
    try {
      String line = new String(bytes, offset, numBytes, charsetName);
      int[] fieldIndexes = IntStream.range(0, row.getArity()).toArray();
      return parse(row, line, null, rowTypeInfo, fieldIndexes);
    } catch (UnsupportedEncodingException e) {
      throw BitSailException.asBitSailException(CommonErrorCode.UNSUPPORTED_ENCODING, e);
    }
  }

  @Override
  public Row parse(Row row, Object line, RowTypeInfo rowTypeInfo) throws Exception {
    int[] fieldIndexes = IntStream.range(0, row.getArity()).toArray();
    return parse(row, line, "UTF-8", rowTypeInfo, fieldIndexes);
  }

  public Row parse(Row row, Object line, String mandatoryEncoding, RowTypeInfo rowTypeInfo, int[] fieldIndexes) throws Exception {
    String csvValue;
    if (StringUtils.isEmpty(mandatoryEncoding)) {
      csvValue = line.toString();
    } else {
      csvValue = new String(((org.apache.hadoop.io.Text) line).getBytes(), 0,
          ((org.apache.hadoop.io.Text) line).getLength(), mandatoryEncoding);
    }

    //not very elegant, but useful for process multi chars delimiter(not a regular csv)
    String formatCsvLine = csvDelimiter.length() > 1 ? csvValue.replaceAll(csvDelimiter, String.valueOf(csvMultiDelimiterReplaceChar))
        : csvValue;

    CSVRecord csvRecord = processCsvLine(formatCsvLine);
    for (int i = 0; i < row.getArity(); i++) {
      TypeInformation<?> typeInfo = rowTypeInfo.getTypeAt(i);

      Object fieldVal = StringEscapeUtils.unescapeCsv(csvRecord.get(fieldIndexes[i]));

      Column column = createColumn(typeInfo, fieldVal);

      row.setField(i, column);
    }
    return row;
  }

  public CSVRecord processCsvLine(String line) throws IOException {
    CSVParser parser = CSVParser.parse(line, csvFormat);
    return parser.getRecords().get(0);
  }
}
