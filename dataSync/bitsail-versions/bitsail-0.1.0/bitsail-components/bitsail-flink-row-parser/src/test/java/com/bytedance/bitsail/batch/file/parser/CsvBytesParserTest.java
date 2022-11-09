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

import com.bytedance.bitsail.common.column.BooleanColumn;
import com.bytedance.bitsail.common.column.LongColumn;
import com.bytedance.bitsail.common.column.StringColumn;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.flink.core.typeinfo.PrimitiveColumnTypeInfo;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.types.Row;
import org.apache.hadoop.io.Text;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class CsvBytesParserTest {

  private static String[] csvRecords = new String[] {
      "First#Id#Score#Last",
      "\"Mike\"#\"1\"#\"12.3\"#\"Smith\"",
      "\"Bob\"#2#45.6#Taylor",
      "Sam#3#7.89#Miller",
      "Peter#4#0.12#Smith",
      "Liz#5#34.5#Williams",
      "Sally#6#6.78#Miller",
      "Alice#7#90.1#Smith",
      "Kelly#8#2.34#Williams"
  };

  private final String content = "test_str,100,true";
  private final RowTypeInfo rowTypeInfo = new RowTypeInfo(
      PrimitiveColumnTypeInfo.STRING_COLUMN_TYPE_INFO,
      PrimitiveColumnTypeInfo.LONG_COLUMN_TYPE_INFO,
      PrimitiveColumnTypeInfo.BOOL_COLUMN_TYPE_INFO
  );

  @Test
  public void testCsvQuote() throws Exception {
    CSVFormat csvFormat = CSVFormat.DEFAULT
        .withDelimiter('#')
        .withEscape('\\')
        .withQuote(null);
    CSVRecord record = getRecordByOneLine(csvRecords[2], csvFormat);
    Assert.assertNotNull(record);
  }

  @Test
  public void testParseLine() throws Exception {
    CsvBytesParser parser = new CsvBytesParser(BitSailConfiguration.newDefault());

    byte[] bytes = content.getBytes();
    Row row = new Row(3);
    row = parser.parse(row, bytes, 0, bytes.length, "UTF-8", rowTypeInfo);
    Assert.assertEquals("test_str", ((StringColumn) (row.getField(0))).asString());
    Assert.assertEquals(100L, ((LongColumn) (row.getField(1))).asLong().longValue());
    Assert.assertEquals(true, ((BooleanColumn) (row.getField(2))).asBoolean());
  }

  @Test
  public void testParseText() throws Exception {
    CsvBytesParser parser = new CsvBytesParser(BitSailConfiguration.newDefault());

    Text text = new Text(content);
    Row row = new Row(3);
    row = parser.parse(row, text, rowTypeInfo);
    Assert.assertEquals("test_str", ((StringColumn) (row.getField(0))).asString());
    Assert.assertEquals(100L, ((LongColumn) (row.getField(1))).asLong().longValue());
    Assert.assertEquals(true, ((BooleanColumn) (row.getField(2))).asBoolean());
  }

  private CSVRecord getRecordByOneLine(String line, CSVFormat csvFormat) throws IOException {
    CSVParser parser = CSVParser.parse(line, csvFormat);
    return parser.getRecords().get(0);
  }
}
