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

package com.bytedance.bitsail.connector.legacy.jdbc.source;

import com.bytedance.bitsail.connector.legacy.jdbc.converter.OracleValueConverter;

import oracle.jdbc.OracleResultSet;
import oracle.jdbc.OracleResultSetMetaData;
import oracle.jdbc.OracleTypes;
import oracle.sql.TIMESTAMP;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.sql.Timestamp;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

public class OracleInputFormatTest {

  @Test
  public void getSupportedTypeRowDataTest() throws Exception {
    int columnType = OracleTypes.TIMESTAMPTZ;
    TIMESTAMP timestamp = new TIMESTAMP(new Timestamp(1539492540));
    OracleResultSet resultSet = Mockito.mock(OracleResultSet.class);
    OracleResultSetMetaData metaData = Mockito.mock(OracleResultSetMetaData.class);
    when(resultSet.getTIMESTAMP(anyInt())).thenReturn(timestamp);
    when(metaData.getColumnType(anyInt())).thenReturn(columnType);

    OracleValueConverter oracleValueConverter = new OracleValueConverter(OracleValueConverter.IntervalHandlingMode.NUMERIC);
    Object convert = oracleValueConverter.convert(metaData, resultSet, 1, null);
    Assert.assertEquals(convert, timestamp.timestampValue());
  }
}
