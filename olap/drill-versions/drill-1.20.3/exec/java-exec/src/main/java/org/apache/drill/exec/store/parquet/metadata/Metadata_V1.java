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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.io.api.Binary;
import org.apache.parquet.schema.OriginalType;
import org.apache.parquet.schema.PrimitiveType;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.apache.drill.exec.store.parquet.metadata.MetadataVersion.Constants.V1;
import static org.apache.drill.exec.store.parquet.metadata.MetadataBase.ColumnMetadata;
import static org.apache.drill.exec.store.parquet.metadata.MetadataBase.ParquetFileMetadata;
import static org.apache.drill.exec.store.parquet.metadata.MetadataBase.ParquetTableMetadataBase;
import static org.apache.drill.exec.store.parquet.metadata.MetadataBase.RowGroupMetadata;


public class Metadata_V1 {

  @JsonTypeName(V1)
  public static class ParquetTableMetadata_v1 extends ParquetTableMetadataBase {
    @JsonProperty(value = "metadata_version", access = JsonProperty.Access.WRITE_ONLY) private String metadataVersion;
    @JsonProperty
    List<ParquetFileMetadata_v1> files;
    @JsonProperty List<Path> directories;

    public ParquetTableMetadata_v1() {
    }

    public ParquetTableMetadata_v1(String metadataVersion, List<ParquetFileMetadata_v1> files, List<Path> directories) {
      this.metadataVersion = metadataVersion;
      this.files = files;
      this.directories = directories;
    }

    @JsonIgnore
    @Override public List<Path> getDirectories() {
      return directories;
    }

    @JsonIgnore
    @Override public List<? extends ParquetFileMetadata> getFiles() {
      return files;
    }

    @JsonIgnore
    @Override public void assignFiles(List<? extends ParquetFileMetadata> newFiles) {
      this.files = (List<ParquetFileMetadata_v1>) newFiles;
    }

    @Override public boolean hasColumnMetadata() {
      return false;
    }

    @JsonIgnore
    @Override public PrimitiveType.PrimitiveTypeName getPrimitiveType(String[] columnName) {
      return null;
    }

    @JsonIgnore
    @Override public OriginalType getOriginalType(String[] columnName) {
      return null;
    }

    @JsonIgnore
    @Override
    public Integer getRepetitionLevel(String[] columnName) {
      return null;
    }

    @JsonIgnore
    @Override
    public Integer getDefinitionLevel(String[] columnName) {
      return null;
    }

    @JsonIgnore
    @Override
    public Integer getScale(String[] columnName) {
      return null;
    }

    @JsonIgnore
    @Override
    public Integer getPrecision(String[] columnName) {
      return null;
    }

    @JsonIgnore
    @Override
    public boolean isRowGroupPrunable() {
      return false;
    }

    @JsonIgnore
    @Override public MetadataBase.ParquetTableMetadataBase clone() {
      return new ParquetTableMetadata_v1(metadataVersion, files, directories);
    }

    @JsonIgnore
    @Override
    public String getDrillVersion() {
      return null;
    }

    @JsonIgnore
    @Override public String getMetadataVersion() {
      return metadataVersion;
    }

    @JsonIgnore
    @Override
    public List<? extends MetadataBase.ColumnTypeMetadata> getColumnTypeInfoList() {
      return null;
    }
  }


  /**
   * Struct which contains the metadata for a single parquet file
   */
  public static class ParquetFileMetadata_v1 extends ParquetFileMetadata {
    @JsonProperty
    public Path path;
    @JsonProperty
    public Long length;
    @JsonProperty
    public List<RowGroupMetadata_v1> rowGroups;

    public ParquetFileMetadata_v1() {
    }

    public ParquetFileMetadata_v1(Path path, Long length, List<RowGroupMetadata_v1> rowGroups) {
      this.path = path;
      this.length = length;
      this.rowGroups = rowGroups;
    }

    @Override
    public String toString() {
      return String.format("path: %s rowGroups: %s", path, rowGroups);
    }

    @JsonIgnore
    @Override public Path getPath() {
      return path;
    }

    @JsonIgnore
    @Override public Long getLength() {
      return length;
    }

    @JsonIgnore
    @Override public List<? extends RowGroupMetadata> getRowGroups() {
      return rowGroups;
    }
  }


  /**
   * A struct that contains the metadata for a parquet row group
   */
  public static class RowGroupMetadata_v1 extends RowGroupMetadata {
    @JsonProperty
    public Long start;
    @JsonProperty
    public Long length;
    @JsonProperty
    public Long rowCount;
    @JsonProperty
    public Map<String, Float> hostAffinity;
    @JsonProperty
    public List<ColumnMetadata_v1> columns;

    public RowGroupMetadata_v1() {
    }

    public RowGroupMetadata_v1(Long start, Long length, Long rowCount, Map<String, Float> hostAffinity,
                               List<ColumnMetadata_v1> columns) {
      this.start = start;
      this.length = length;
      this.rowCount = rowCount;
      this.hostAffinity = hostAffinity;
      this.columns = columns;
    }

    @Override public Long getStart() {
      return start;
    }

    @Override public Long getLength() {
      return length;
    }

    @Override public Long getRowCount() {
      return rowCount;
    }

    @Override public Map<String, Float> getHostAffinity() {
      return hostAffinity;
    }

    @Override public List<? extends ColumnMetadata> getColumns() {
      return columns;
    }
  }


  /**
   * A struct that contains the metadata for a column in a parquet file
   */
  public static class ColumnMetadata_v1 extends ColumnMetadata {
    @JsonProperty
    public SchemaPath name;
    @JsonProperty
    public PrimitiveType.PrimitiveTypeName primitiveType;
    @JsonProperty
    public OriginalType originalType;
    @JsonProperty
    public Long nulls;

    // JsonProperty for these are associated with the getters and setters
    public Object max;
    public Object min;


    public ColumnMetadata_v1() {
    }

    public ColumnMetadata_v1(SchemaPath name, PrimitiveType.PrimitiveTypeName primitiveType, OriginalType originalType,
                             Object max, Object min, Long nulls) {
      this.name = name;
      this.primitiveType = primitiveType;
      this.originalType = originalType;
      this.max = max;
      this.min = min;
      this.nulls = nulls;
    }

    @JsonProperty(value = "min")
    public Object getMin() {
      if (primitiveType == PrimitiveType.PrimitiveTypeName.BINARY && min != null) {
        return new String(((Binary) min).getBytes());
      }
      return min;
    }

    @JsonProperty(value = "max")
    public Object getMax() {
      if (primitiveType == PrimitiveType.PrimitiveTypeName.BINARY && max != null) {
        return new String(((Binary) max).getBytes());
      }
      return max;
    }

    @Override public PrimitiveType.PrimitiveTypeName getPrimitiveType() {
      return primitiveType;
    }

    @Override public OriginalType getOriginalType() {
      return originalType;
    }

    /**
     * setter used during deserialization of the 'min' field of the metadata cache file.
     *
     * @param min
     */
    @JsonProperty(value = "min")
    public void setMin(Object min) {
      this.min = min;
    }

    /**
     * setter used during deserialization of the 'max' field of the metadata cache file.
     *
     * @param max
     */
    @JsonProperty(value = "max")
    public void setMax(Object max) {
      this.max = max;
    }

    @Override public String[] getName() {
      String[] s = new String[1];
      String nameString = name.toString();
      // Strip out the surrounding backticks.
      s[0]=nameString.substring(1, nameString.length()-1);
      return s;
    }

    @Override public Long getNulls() {
      return nulls;
    }

    /**
     * Checks that the column chunk has a single value.
     * Returns {@code true} if {@code min} and {@code max} are the same but not null
     * and nulls count is 0 or equal to the rows count.
     * <p>
     * Returns {@code true} if {@code min} and {@code max} are null and the number of null values
     * in the column chunk is equal to the rows count.
     * <p>
     * Comparison of nulls and rows count is needed for the cases:
     * <ul>
     * <li>column with primitive type has single value and null values</li>
     *
     * <li>column <b>with primitive type</b> has only null values, min/max couldn't be null,
     * but column has single value</li>
     * </ul>
     *
     * @param rowCount rows count in column chunk
     * @return true if column has single value
     */
    @Override
    public boolean hasSingleValue(long rowCount) {
      if (nulls != null) {
        if (min != null) {
          // Objects.deepEquals() is used here, since min and max may be byte arrays
          return Objects.deepEquals(min, max) && (nulls == 0 || nulls == rowCount);
        } else {
          return nulls == rowCount && max == null;
        }
      }
      return false;
    }

    @Override public Object getMinValue() {
      return min;
    }

    @Override public Object getMaxValue() {
      return max;
    }

  }

}
