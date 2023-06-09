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
package org.apache.drill.exec.store.parquet;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.drill.categories.ParquetTest;
import org.apache.drill.categories.UnlikelyTest;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.server.options.SessionOptionManager;
import org.apache.drill.exec.server.options.SystemOptionManager;
import org.apache.drill.test.BaseTest;
import org.apache.hadoop.conf.Configuration;
import org.apache.parquet.ParquetReadOptions;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@Category({ParquetTest.class, UnlikelyTest.class})
public class TestParquetReaderConfig extends BaseTest {

  @Test
  public void testDefaultsDeserialization() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    ParquetReaderConfig readerConfig = ParquetReaderConfig.builder().build(); // all defaults
    String value = mapper.writeValueAsString(readerConfig);
    assertEquals(ParquetReaderConfig.getDefaultInstance(), readerConfig); // compare with default instance
    assertEquals("{}", value);

    readerConfig = mapper.readValue(value, ParquetReaderConfig.class);
    assertTrue(readerConfig.autoCorrectCorruptedDates()); // check that default value is restored

    // change the default: set autoCorrectCorruptedDates to false
    // keep the default: set enableStringsSignedMinMax to false
    readerConfig = new ParquetReaderConfig(false, false, false, false, false);

    value = mapper.writeValueAsString(readerConfig);
    assertEquals("{\"autoCorrectCorruptedDates\":false}", value);
  }

  @Test
  public void testAddConfigToConf() {
    Configuration conf = new Configuration();
    conf.setBoolean(ParquetReaderConfig.ENABLE_BYTES_READ_COUNTER, true);
    conf.setBoolean(ParquetReaderConfig.ENABLE_BYTES_TOTAL_COUNTER, true);
    conf.setBoolean(ParquetReaderConfig.ENABLE_TIME_READ_COUNTER, true);

    ParquetReaderConfig readerConfig = ParquetReaderConfig.builder().withConf(conf).build();
    Configuration newConf = readerConfig.addCountersToConf(new Configuration());
    checkConfigValue(newConf, ParquetReaderConfig.ENABLE_BYTES_READ_COUNTER, "true");
    checkConfigValue(newConf, ParquetReaderConfig.ENABLE_BYTES_TOTAL_COUNTER, "true");
    checkConfigValue(newConf, ParquetReaderConfig.ENABLE_TIME_READ_COUNTER, "true");

    conf = new Configuration();
    conf.setBoolean(ParquetReaderConfig.ENABLE_BYTES_READ_COUNTER, false);
    conf.setBoolean(ParquetReaderConfig.ENABLE_BYTES_TOTAL_COUNTER, false);
    conf.setBoolean(ParquetReaderConfig.ENABLE_TIME_READ_COUNTER, false);

    readerConfig = ParquetReaderConfig.builder().withConf(conf).build();
    newConf = readerConfig.addCountersToConf(new Configuration());
    checkConfigValue(newConf, ParquetReaderConfig.ENABLE_BYTES_READ_COUNTER, "false");
    checkConfigValue(newConf, ParquetReaderConfig.ENABLE_BYTES_TOTAL_COUNTER, "false");
    checkConfigValue(newConf, ParquetReaderConfig.ENABLE_TIME_READ_COUNTER, "false");
  }

  @Test
  public void testReadOptions() {
    // set enableStringsSignedMinMax to true
    ParquetReaderConfig readerConfig = new ParquetReaderConfig(false, false, false, true, true);
    ParquetReadOptions readOptions = readerConfig.toReadOptions();
    assertTrue(readOptions.useSignedStringMinMax());

    // set enableStringsSignedMinMax to false
    readerConfig = new ParquetReaderConfig(false, false, false, true, false);
    readOptions = readerConfig.toReadOptions();
    assertFalse(readOptions.useSignedStringMinMax());
  }

  @Test
  public void testPriorityAssignmentForStringsSignedMinMax() throws Exception {
    @SuppressWarnings("resource")
    SystemOptionManager sysOpts = new SystemOptionManager(DrillConfig.create()).init();
    SessionOptionManager sessOpts = new SessionOptionManager(sysOpts, null);

    // use value from format config
    ParquetFormatConfig formatConfig = new ParquetFormatConfig();
    ParquetReaderConfig readerConfig = ParquetReaderConfig.builder().withFormatConfig(formatConfig).build();
    assertEquals(formatConfig.isEnableStringsSignedMinMax(), readerConfig.enableStringsSignedMinMax());

    // change format config value
    formatConfig = ParquetFormatConfig.builder()
      .autoCorrectCorruptDates(true)
      .enableStringsSignedMinMax(true)
      .build();

    readerConfig = ParquetReaderConfig.builder().withFormatConfig(formatConfig).build();
    assertEquals(formatConfig.isEnableStringsSignedMinMax(), readerConfig.enableStringsSignedMinMax());

    // set system option, option value should not have higher priority
    sysOpts.setLocalOption(ExecConstants.PARQUET_READER_STRINGS_SIGNED_MIN_MAX, "false");
    readerConfig = ParquetReaderConfig.builder().withFormatConfig(formatConfig).withOptions(sysOpts).build();
    assertTrue(readerConfig.enableStringsSignedMinMax());

    // set session option, option value should have higher priority
    sessOpts.setLocalOption(ExecConstants.PARQUET_READER_STRINGS_SIGNED_MIN_MAX, "false");
    readerConfig = ParquetReaderConfig.builder().withFormatConfig(formatConfig).withOptions(sessOpts).build();
    assertFalse(readerConfig.enableStringsSignedMinMax());
  }


  private void checkConfigValue(Configuration conf, String name, String expectedValue) {
    String actualValue = conf.get(name);
    assertNotNull(actualValue);
    assertEquals(expectedValue, actualValue);
  }

}
