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
package org.apache.drill.exec.store.parquet.metadata;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.ComparisonChain;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableSortedSet;
import org.apache.drill.common.exceptions.DrillRuntimeException;

import java.util.SortedSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MetadataVersion implements Comparable<MetadataVersion> {

  private static final String FORMAT = "v?((?!0)\\d+)(\\.(\\d+))?";
  private static final Pattern PATTERN = Pattern.compile(FORMAT);

  private final int major;
  private final int minor;

  public MetadataVersion(int major, int minor) {
    this.major = major;
    this.minor = minor;
  }

  public MetadataVersion(String metadataVersion) {
    Matcher matcher = PATTERN.matcher(metadataVersion);
    if (!matcher.matches()) {
      throw DrillRuntimeException.create(
          "Could not parse metadata version '%s' using format '%s'", metadataVersion, FORMAT);
    }
    this.major = Integer.parseInt(matcher.group(1));
    this.minor = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;
  }

  public int getMajor() {
    return major;
  }

  public int getMinor() {
    return minor;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof MetadataVersion)) {
      return false;
    }
    MetadataVersion that = (MetadataVersion) o;
    return this.major == that.major
        && this.minor == that.minor;
  }

  @Override
  public int hashCode() {
    int result = major;
    result = 31 * result + minor;
    return result;
  }

  /**
   * @return string representation of the metadata file version, for example: "1", "10", "4.13"
   * <p>
   * String metadata version consists of the following characters:<p>
   * major metadata version (any number of digits, except a single zero digit),<p>
   * optional "." delimiter (used if minor metadata version is specified),<p>
   * minor metadata version (not specified for "0" minor version)
   */
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append(major);
    if (minor != 0) {
      builder.append(".").append(minor);
    }
    return builder.toString();
  }

  @Override
  public int compareTo(MetadataVersion o) {
    Preconditions.checkNotNull(o);
    return ComparisonChain.start()
        .compare(this.major, o.major)
        .compare(this.minor, o.minor)
        .result();
  }

  /**
   * Check if this version is at least (equals or higher) the one
   * identified by {@code major} and {@code minor} versions integer literals.
   *
   * @param major major version
   * @param minor minor version
   * @return {@literal true} if the version is equal to or higher than
   *         the one it is being checked against
   */
  public boolean isAtLeast(int major, int minor) {
    return this.major > major || (this.major == major && this.minor >= minor);
  }

  /**
   * Check if the version is the same as the one identified by
   * {@code major} and {@code minor} versions integer literals.
   *
   * @param major major version
   * @param minor minor version
   * @return {@literal true} if the version is equal to the one
   *         it is being checked against
   */
  public boolean isEqualTo(int major, int minor) {
    return this.major == major && this.minor == minor;
  }

  /**
   * Check if this version comes after the one identified by {@code major}
   * and {@code minor} versions integer literals. That is, this one was introduced later.
   *
   * @param major major version
   * @param minor minor version
   * @return {@literal true} if the version is defined later than
   *         the one it is being checked against
   */
  public boolean isHigherThan(int major, int minor) {
    return this.major > major || (this.major == major && this.minor > minor);
  }

/**
 * Supported metadata versions.
 * <p>
 * Note: keep them synchronized with {@link org.apache.drill.exec.store.parquet.metadata.MetadataBase.ParquetTableMetadataBase} versions
 */
  public static class Constants {
    /**
     * Version 1: Introduces parquet file metadata caching.<br>
     * See DRILL-2743
     */
    public static final String V1 = "v1";
    /**
     * Version 2: Metadata cache file size is reduced.<br>
     * See DRILL-4053
     */
    public static final String V2 = "v2";
    /**
     * Version 3: Difference between v3 and v2 : min/max, type_length, precision, scale, repetitionLevel, definitionLevel.<br>
     * Filter pushdown for Parquet is implemented. <br>
     * See DRILL-1950
     */
    public static final String V3 = "v3";
    /**
     * Version 3.1: Absolute paths of files and directories are replaced with relative ones. Metadata version value
     * doesn't contain `v` letter<br>
     * See DRILL-3867, DRILL-5660
     */
    public static final String V3_1 = "3.1";

    /**
     * Version 3.2: An array with the components of the field name in
     * {@link org.apache.drill.exec.store.parquet.metadata.Metadata_V3.ColumnTypeMetadata_v3.Key} class is replaced by the {@link org.apache.drill.common.expression.SchemaPath}.<br>
     * See DRILL-4264
     */
    public static final String V3_2 = "3.2";

    /**
     * Version 3.3: Changed serialization of BINARY and FIXED_LEN_BYTE_ARRAY fields.<br>
     * See DRILL-4139
     */
    public static final String V3_3 = "3.3";

    /**
     *  Version 4.0: Split the metadata cache file into summary and file metadata
     */
    public static final String V4 = "4.0";

    /**
     *  Version 4.1: Added parents' original types in {@link Metadata_V4.ColumnTypeMetadata_v4}
     *  and {@link Metadata_V4.ColumnMetadata_v4}
     */
    public static final String V4_1 = "4.1";

    /**
     * Version 4.2: Added {@link org.apache.parquet.schema.Type.Repetition} to {@link Metadata_V4.ColumnTypeMetadata_v4}.
     */
    public static final String V4_2 = "4.2";

    /**
     * All historical versions of the Drill metadata cache files. In case of introducing a new parquet metadata version
     * please follow the {@link MetadataVersion#FORMAT}.
     */
    public static final SortedSet<MetadataVersion> SUPPORTED_VERSIONS = ImmutableSortedSet.of(
        new MetadataVersion(V1),
        new MetadataVersion(V2),
        new MetadataVersion(V3),
        new MetadataVersion(V3_1),
        new MetadataVersion(V3_2),
        new MetadataVersion(V3_3),
        new MetadataVersion(V4),
        new MetadataVersion(V4_1),
        new MetadataVersion(V4_2)
    );

    /**
     * @param metadataVersion string representation of the parquet metadata version
     * @return true if metadata version is supported, false otherwise
     */
    public static boolean isVersionSupported(String metadataVersion) {
      return SUPPORTED_VERSIONS.contains(new MetadataVersion(metadataVersion));
    }
  }
}
