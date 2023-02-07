/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.store.hive.inspectors;

import org.apache.drill.exec.store.hive.readers.inspectors.SkipFooterRecordsInspector;
import org.apache.drill.test.BaseTest;
import org.apache.hadoop.mapred.RecordReader;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SkipFooterRecordsInspectorTest extends BaseTest {

  private static RecordReader<Object, Object> recordReader;

  @BeforeClass
  @SuppressWarnings("unchecked")
  public static void init() {
    recordReader = mock(RecordReader.class);
    when(recordReader.createValue()).thenReturn(new Object());
  }

  @Test
  public void testHolderReUsage() {
    SkipFooterRecordsInspector inspector = new SkipFooterRecordsInspector(recordReader, 1);
    // store first value holder
    Object firstHolder = inspector.getValueHolder();

    // return null since one record was buffered as footer
    assertNull(inspector.getNextValue());

    // store first value holder
    Object secondHolder = inspector.getValueHolder();

    // return value stored in first holder  now second holder is buffering the footer
    assertEquals(secondHolder, inspector.getValueHolder());
    assertEquals(firstHolder, inspector.getNextValue());

    // return value stored in second holder, as now first holder is buffering the footer
    assertEquals(firstHolder, inspector.getValueHolder());
    assertEquals(secondHolder, inspector.getNextValue());
  }

  @Test
  public void testReset() {
    SkipFooterRecordsInspector inspector = new SkipFooterRecordsInspector(recordReader, 2);
    assertEquals(0, inspector.getProcessedRecordCount());

    // store second holder
    inspector.getNextValue();
    Object secondHolder = inspector.getValueHolder();
    inspector.getNextValue();

    // process n records and increment count, so we stop at second holder
    for (int i = 0; i < 4; i++) {
      inspector.getNextValue();
      inspector.incrementProcessedRecordCount();
    }
    assertEquals(4, inspector.getProcessedRecordCount());
    assertEquals(secondHolder, inspector.getValueHolder());

    // reset and make sure we start from the last available holder
    inspector.reset();
    assertEquals(0, inspector.getProcessedRecordCount());
    assertEquals(secondHolder, inspector.getValueHolder());
  }
}
