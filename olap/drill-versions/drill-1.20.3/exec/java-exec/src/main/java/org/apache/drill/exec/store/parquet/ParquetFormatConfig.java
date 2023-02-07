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
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.drill.common.PlanStringBuilder;
import org.apache.drill.common.logical.FormatPluginConfig;

import java.util.Objects;

@JsonTypeName("parquet") @JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ParquetFormatConfig implements FormatPluginConfig {

  /**
   * Until DRILL-4203 was resolved, Drill could write non-standard dates into
   * parquet files. This issue is related to all drill releases where {@link
   * org.apache.drill.exec.store.parquet.ParquetRecordWriter#WRITER_VERSION_PROPERTY}
   * < {@link org.apache.drill.exec.store.parquet.ParquetReaderUtility#DRILL_WRITER_VERSION_STD_DATE_FORMAT}.

   * The values have been read correctly by Drill, but external tools like
   * Spark reading the files will see corrupted values for all dates that
   * have been written by Drill.  To maintain compatibility with old files,
   * the parquet reader code has been given the ability to check for the
   * old format and automatically shift the corrupted values into corrected
   * ones automatically.
   */
 private final boolean autoCorrectCorruptDates;

  /**
   * Parquet statistics for UTF-8 data in files created prior to 1.9.1 parquet
   * library version were stored incorrectly.  If the user exactly knows that
   * data in binary columns is in ASCII (not UTF-8), turning this property to
   * 'true' enables statistics usage for varchar and decimal columns.
   *
   * {@link org.apache.drill.exec.ExecConstants#PARQUET_READER_STRINGS_SIGNED_MIN_MAX}
   */
  private final boolean enableStringsSignedMinMax;

  // {@link org.apache.drill.exec.ExecConstants#PARQUET_BLOCK_SIZE}
  private final Integer blockSize;

  // {@link org.apache.drill.exec.ExecConstants#PARQUET_PAGE_SIZE}
  private final Integer pageSize;

  // {@link org.apache.drill.exec.ExecConstants#PARQUET_WRITER_USE_SINGLE_FS_BLOCK}
  private final Boolean useSingleFSBlock;

  // {@link org.apache.drill.exec.ExecConstants#PARQUET_WRITER_COMPRESSION_TYPE}
  private final String writerCompressionType;

  // {@link org.apache.drill.exec.ExecConstants#PARQUET_WRITER_LOGICAL_TYPE_FOR_DECIMALS}
  private final String writerLogicalTypeForDecimals;

  // {@link org.apache.drill.exec.ExecConstants#PARQUET_WRITER_USE_PRIMITIVE_TYPES_FOR_DECIMALS}
  private final Boolean writerUsePrimitivesForDecimals;

  // {@link org.apache.drill.exec.ExecConstants#PARQUET_WRITER_FORMAT_VERSION}
  private final String writerFormatVersion;

  public ParquetFormatConfig() {
    // config opts which are also system opts must default to null so as not
    // to override system opts.
    this(true, false, null, null, null, null, null, null, null);
  }

  @JsonCreator
  public ParquetFormatConfig(
    @JsonProperty("autoCorrectCorruptDates") Boolean autoCorrectCorruptDates,
    @JsonProperty("enableStringsSignedMinMax") boolean enableStringsSignedMinMax,
    @JsonProperty("blockSize") Integer blockSize,
    @JsonProperty("pageSize") Integer pageSize,
    @JsonProperty("useSingleFSBlock") Boolean useSingleFSBlock,
    @JsonProperty("writerCompressionType") String writerCompressionType,
    @JsonProperty("writerLogicalTypeForDecimals") String writerLogicalTypeForDecimals,
    @JsonProperty("writerUsePrimitivesForDecimals") Boolean writerUsePrimitivesForDecimals,
    @JsonProperty("writerFormatVersion") String writerFormatVersion
  ) {
    this.autoCorrectCorruptDates = autoCorrectCorruptDates == null ? true : autoCorrectCorruptDates;
    this.enableStringsSignedMinMax = enableStringsSignedMinMax;
    this.blockSize = blockSize;
    this.pageSize = pageSize;
    this.useSingleFSBlock = useSingleFSBlock;
    this.writerCompressionType = writerCompressionType;
    this.writerLogicalTypeForDecimals = writerLogicalTypeForDecimals;
    this.writerUsePrimitivesForDecimals = writerUsePrimitivesForDecimals;
    this.writerFormatVersion = writerFormatVersion;
  }

  public static ParquetFormatConfigBuilder builder() {
    return new ParquetFormatConfigBuilder();
  }

  @Override
  public String toString() {
    return new PlanStringBuilder(this)
      .field("autoCorrectCorruptDates", autoCorrectCorruptDates)
      .field("enableStringsSignedMinMax", enableStringsSignedMinMax)
      .field("blockSize", blockSize)
      .field("pageSize", pageSize)
      .field("useSingleFSBlock", useSingleFSBlock)
      .field("writerCompressionType", writerCompressionType)
      .field("writerLogicalTypeForDecimals", writerLogicalTypeForDecimals)
      .field("writerUsePrimitivesForDecimals", writerUsePrimitivesForDecimals)
      .field("writerFormatVersion", writerFormatVersion)
      .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ParquetFormatConfig that = (ParquetFormatConfig) o;
    return autoCorrectCorruptDates == that.autoCorrectCorruptDates
      && enableStringsSignedMinMax == that.enableStringsSignedMinMax
      && Objects.equals(blockSize, that.blockSize)
      && Objects.equals(pageSize, that.pageSize)
      && Objects.equals(useSingleFSBlock, that.useSingleFSBlock)
      && Objects.equals(writerCompressionType, that.writerCompressionType)
      && Objects.equals(writerLogicalTypeForDecimals, that.writerLogicalTypeForDecimals)
      && Objects.equals(writerUsePrimitivesForDecimals, that.writerUsePrimitivesForDecimals)
      && Objects.equals(writerFormatVersion, that.writerFormatVersion);
  }

  @Override
  public int hashCode() {
    return Objects.hash(autoCorrectCorruptDates, enableStringsSignedMinMax, blockSize, pageSize,
      useSingleFSBlock, writerCompressionType, writerLogicalTypeForDecimals,
      writerUsePrimitivesForDecimals, writerFormatVersion);
  }

  public boolean isAutoCorrectCorruptDates() {
    return this.autoCorrectCorruptDates;
  }

  public boolean isEnableStringsSignedMinMax() {
    return this.enableStringsSignedMinMax;
  }

  public Integer getBlockSize() {
    return this.blockSize;
  }

  public Integer getPageSize() {
    return this.pageSize;
  }

  public Boolean getUseSingleFSBlock() {
    return this.useSingleFSBlock;
  }

  public String getWriterCompressionType() {
    return this.writerCompressionType;
  }

  public String getWriterLogicalTypeForDecimals() {
    return this.writerLogicalTypeForDecimals;
  }

  public Boolean getWriterUsePrimitivesForDecimals() {
    return this.writerUsePrimitivesForDecimals;
  }

  public String getWriterFormatVersion() {
    return this.writerFormatVersion;
  }

  public static class ParquetFormatConfigBuilder {
    private Boolean autoCorrectCorruptDates;

    private boolean enableStringsSignedMinMax;

    private Integer blockSize;

    private Integer pageSize;

    private Boolean useSingleFSBlock;

    private String writerCompressionType;

    private String writerLogicalTypeForDecimals;

    private Boolean writerUsePrimitivesForDecimals;

    private String writerFormatVersion;

    public ParquetFormatConfigBuilder autoCorrectCorruptDates(Boolean autoCorrectCorruptDates) {
      this.autoCorrectCorruptDates = autoCorrectCorruptDates;
      return this;
    }

    public ParquetFormatConfigBuilder enableStringsSignedMinMax(boolean enableStringsSignedMinMax) {
      this.enableStringsSignedMinMax = enableStringsSignedMinMax;
      return this;
    }

    public ParquetFormatConfigBuilder blockSize(Integer blockSize) {
      this.blockSize = blockSize;
      return this;
    }

    public ParquetFormatConfigBuilder pageSize(Integer pageSize) {
      this.pageSize = pageSize;
      return this;
    }

    public ParquetFormatConfigBuilder useSingleFSBlock(Boolean useSingleFSBlock) {
      this.useSingleFSBlock = useSingleFSBlock;
      return this;
    }

    public ParquetFormatConfigBuilder writerCompressionType(String writerCompressionType) {
      this.writerCompressionType = writerCompressionType;
      return this;
    }

    public ParquetFormatConfigBuilder writerLogicalTypeForDecimals(String writerLogicalTypeForDecimals) {
      this.writerLogicalTypeForDecimals = writerLogicalTypeForDecimals;
      return this;
    }

    public ParquetFormatConfigBuilder writerUsePrimitivesForDecimals(Boolean writerUsePrimitivesForDecimals) {
      this.writerUsePrimitivesForDecimals = writerUsePrimitivesForDecimals;
      return this;
    }

    public ParquetFormatConfigBuilder writerFormatVersion(String writerFormatVersion) {
      this.writerFormatVersion = writerFormatVersion;
      return this;
    }

    public ParquetFormatConfig build() {
      return new ParquetFormatConfig(autoCorrectCorruptDates, enableStringsSignedMinMax, blockSize, pageSize, useSingleFSBlock, writerCompressionType, writerLogicalTypeForDecimals, writerUsePrimitivesForDecimals, writerFormatVersion);
    }
  }
}
