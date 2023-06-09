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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.server.options.OptionManager;
import org.apache.drill.exec.server.options.OptionValue;
import org.apache.hadoop.conf.Configuration;
import org.apache.parquet.ParquetReadOptions;

import java.util.Objects;

/**
 * Stores consolidated parquet reading configuration. Can obtain config values from various sources:
 * Assignment priority of configuration values is the following:
 * <li>parquet format config</li>
 * <li>Hadoop configuration</li>
 * <li>session options</li>
 *
 * During serialization does not deserialize the default values in keep serialized object smaller.
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ParquetReaderConfig {

  public static final String ENABLE_BYTES_READ_COUNTER = "parquet.benchmark.bytes.read";
  public static final String ENABLE_BYTES_TOTAL_COUNTER = "parquet.benchmark.bytes.total";
  public static final String ENABLE_TIME_READ_COUNTER = "parquet.benchmark.time.read";

  private static final ParquetReaderConfig DEFAULT_INSTANCE = new ParquetReaderConfig();

  private boolean enableBytesReadCounter = false;
  private boolean enableBytesTotalCounter = false;
  private boolean enableTimeReadCounter = false;
  private boolean autoCorrectCorruptedDates = true;
  private boolean enableStringsSignedMinMax = false;

  public static ParquetReaderConfig.Builder builder() {
    return new ParquetReaderConfig.Builder();
  }

  public static ParquetReaderConfig getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  @JsonCreator
  public ParquetReaderConfig(@JsonProperty("enableBytesReadCounter") Boolean enableBytesReadCounter,
                             @JsonProperty("enableBytesTotalCounter") Boolean enableBytesTotalCounter,
                             @JsonProperty("enableTimeReadCounter") Boolean enableTimeReadCounter,
                             @JsonProperty("autoCorrectCorruptedDates") Boolean autoCorrectCorruptedDates,
                             @JsonProperty("enableStringsSignedMinMax") Boolean enableStringsSignedMinMax) {
    this.enableBytesReadCounter = enableBytesReadCounter == null ? this.enableBytesReadCounter : enableBytesReadCounter;
    this.enableBytesTotalCounter = enableBytesTotalCounter == null ? this.enableBytesTotalCounter : enableBytesTotalCounter;
    this.enableTimeReadCounter = enableTimeReadCounter == null ? this.enableTimeReadCounter : enableTimeReadCounter;
    this.autoCorrectCorruptedDates = autoCorrectCorruptedDates == null ? this.autoCorrectCorruptedDates : autoCorrectCorruptedDates;
    this.enableStringsSignedMinMax = enableStringsSignedMinMax == null ? this.enableStringsSignedMinMax : enableStringsSignedMinMax;
  }

  private ParquetReaderConfig() { }

  @JsonProperty("enableBytesReadCounter")
  public boolean enableBytesReadCounter() {
    return enableBytesReadCounter;
  }

  @JsonProperty("enableBytesTotalCounter")
  public boolean enableBytesTotalCounter() {
    return enableBytesTotalCounter;
  }

  @JsonProperty("enableTimeReadCounter")
  public boolean enableTimeReadCounter() {
    return enableTimeReadCounter;
  }

  @JsonProperty("autoCorrectCorruptedDates")
  public boolean autoCorrectCorruptedDates() {
    return autoCorrectCorruptedDates;
  }

  @JsonProperty("enableStringsSignedMinMax")
  public boolean enableStringsSignedMinMax() {
    return enableStringsSignedMinMax;
  }

  public ParquetReadOptions toReadOptions() {
    return ParquetReadOptions.builder()
      .useSignedStringMinMax(enableStringsSignedMinMax)
      .build();
  }

  public Configuration addCountersToConf(Configuration conf) {
    Configuration newConfig = new Configuration(conf);
    newConfig.setBoolean(ENABLE_BYTES_READ_COUNTER, enableBytesReadCounter);
    newConfig.setBoolean(ENABLE_BYTES_TOTAL_COUNTER, enableBytesTotalCounter);
    newConfig.setBoolean(ENABLE_TIME_READ_COUNTER, enableTimeReadCounter);
    return newConfig;
  }

  @Override
  public int hashCode() {
    return Objects.hash(enableBytesReadCounter,
      enableBytesTotalCounter,
      enableTimeReadCounter,
      autoCorrectCorruptedDates,
      enableStringsSignedMinMax);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ParquetReaderConfig that = (ParquetReaderConfig) o;
    return enableBytesReadCounter == that.enableBytesReadCounter
      && enableBytesTotalCounter == that.enableBytesTotalCounter
      && enableTimeReadCounter == that.enableTimeReadCounter
      && autoCorrectCorruptedDates == that.autoCorrectCorruptedDates
      && enableStringsSignedMinMax == that.enableStringsSignedMinMax;
  }

  @Override
  public String toString() {
    return "ParquetReaderConfig{"
      + "enableBytesReadCounter=" + enableBytesReadCounter
      + ", enableBytesTotalCounter=" + enableBytesTotalCounter
      + ", enableTimeReadCounter=" + enableTimeReadCounter
      + ", autoCorrectCorruptedDates=" + autoCorrectCorruptedDates
      + ", enableStringsSignedMinMax=" + enableStringsSignedMinMax
      + '}';
  }

  public static class Builder {

    private ParquetFormatConfig formatConfig;
    private Configuration conf;
    private OptionManager options;

    public Builder withFormatConfig(ParquetFormatConfig formatConfig) {
      this.formatConfig = formatConfig;
      return this;
    }

    public Builder withConf(Configuration conf) {
      this.conf = conf;
      return this;
    }

    public Builder withOptions(OptionManager options) {
      this.options = options;
      return this;
    }

    public ParquetReaderConfig build() {
      ParquetReaderConfig readerConfig = new ParquetReaderConfig();

      // first assign configuration values from format config
      if (formatConfig != null) {
        readerConfig.autoCorrectCorruptedDates = formatConfig.isAutoCorrectCorruptDates();
        readerConfig.enableStringsSignedMinMax = formatConfig.isEnableStringsSignedMinMax();
      }

      // then assign configuration values from Hadoop configuration
      if (conf != null) {
        readerConfig.enableBytesReadCounter = conf.getBoolean(ENABLE_BYTES_READ_COUNTER, readerConfig.enableBytesReadCounter);
        readerConfig.enableBytesTotalCounter = conf.getBoolean(ENABLE_BYTES_TOTAL_COUNTER, readerConfig.enableBytesTotalCounter);
        readerConfig.enableTimeReadCounter = conf.getBoolean(ENABLE_TIME_READ_COUNTER, readerConfig.enableTimeReadCounter);
      }

      // last assign values from session or query scoped options which have higher priority than other configurations
      if (options != null) {
        String optVal  = (String) options.getOption(
          ExecConstants.PARQUET_READER_STRINGS_SIGNED_MIN_MAX
        ).getValueMinScope(OptionValue.OptionScope.SESSION);
        if (optVal != null && !optVal.isEmpty()) {
          readerConfig.enableStringsSignedMinMax = Boolean.valueOf(optVal);
        }
      }

      return readerConfig;
    }

  }

}
