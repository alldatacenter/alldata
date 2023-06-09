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
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.util.ArrayList;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.io.api.Binary;
import org.apache.parquet.schema.OriginalType;
import org.apache.parquet.schema.PrimitiveType;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.drill.exec.store.parquet.metadata.MetadataVersion.Constants.V3_3;
import static org.apache.drill.exec.store.parquet.metadata.MetadataBase.ColumnMetadata;
import static org.apache.drill.exec.store.parquet.metadata.MetadataBase.ParquetFileMetadata;
import static org.apache.drill.exec.store.parquet.metadata.MetadataBase.ParquetTableMetadataBase;
import static org.apache.drill.exec.store.parquet.metadata.MetadataBase.RowGroupMetadata;

public class Metadata_V3 {

  @JsonTypeName(V3_3)
  public static class ParquetTableMetadata_v3 extends ParquetTableMetadataBase {
    @JsonProperty(value = "metadata_version", access = JsonProperty.Access.WRITE_ONLY) private String metadataVersion;
    /*
     ColumnTypeInfo is schema information from all the files and row groups, merged into
     one. To get this info, we pass the ParquetTableMetadata object all the way dow to the
     RowGroup and the column type is built there as it is read from the footer.
     */
    @JsonProperty public ConcurrentHashMap<ColumnTypeMetadata_v3.Key, ColumnTypeMetadata_v3> columnTypeInfo;
    @JsonProperty
    List<ParquetFileMetadata_v3> files;
    @JsonProperty List<Path> directories;
    @JsonProperty String drillVersion;

    /**
     * Default constructor needed for deserialization from Parquet Metadata Cache Files
     */
    public ParquetTableMetadata_v3() {
    }

    /**
     * Used for creating the Parquet Metadata cache files and in the process of reading Parquet metadata footers
     *
     * @param metadataVersion metadata version
     * @param drillVersion  apache drill version
     */
    public ParquetTableMetadata_v3(String metadataVersion, String drillVersion) {
      this.metadataVersion = metadataVersion;
      this.drillVersion = drillVersion;
    }

    public ParquetTableMetadata_v3(String metadataVersion, ParquetTableMetadataBase parquetTable,
                                   List<ParquetFileMetadata_v3> files, List<Path> directories, String drillVersion) {
      this.metadataVersion = metadataVersion;
      this.files = files;
      this.directories = directories;
      this.columnTypeInfo = ((ParquetTableMetadata_v3) parquetTable).columnTypeInfo;
      this.drillVersion = drillVersion;
    }

    public ParquetTableMetadata_v3(String metadataVersion, List<ParquetFileMetadata_v3> files, List<Path> directories,
                                   ConcurrentHashMap<ColumnTypeMetadata_v3.Key, ColumnTypeMetadata_v3> columnTypeInfo,
                                   String drillVersion) {
      this.metadataVersion = metadataVersion;
      this.files = files;
      this.directories = directories;
      this.columnTypeInfo = columnTypeInfo;
      this.drillVersion = drillVersion;
    }

    public ColumnTypeMetadata_v3 getColumnTypeInfo(String[] name) {
      return columnTypeInfo.get(new ColumnTypeMetadata_v3.Key(name));
    }

    @JsonIgnore
    @Override public List<Path> getDirectories() {
      return directories;
    }

    @JsonIgnore
    @Override public String getMetadataVersion() {
      return metadataVersion;
    }

    /**
     * If directories list and file metadata list contain relative paths, update it to absolute ones
     * @param baseDir base parent directory
     */
    @JsonIgnore public void updateRelativePaths(String baseDir) {
      // update directories paths to absolute ones
      this.directories = MetadataPathUtils.convertToAbsolutePaths(directories, baseDir);

      // update files paths to absolute ones
      this.files = (List<ParquetFileMetadata_v3>) MetadataPathUtils.convertToFilesWithAbsolutePaths(files, baseDir);
    }

    @JsonIgnore
    @Override public List<? extends ParquetFileMetadata> getFiles() {
      return files;
    }

    @JsonIgnore
    @Override public void assignFiles(List<? extends ParquetFileMetadata> newFiles) {
      this.files = (List<ParquetFileMetadata_v3>) newFiles;
    }

    @Override public boolean hasColumnMetadata() {
      return true;
    }

    @JsonIgnore
    @Override public PrimitiveType.PrimitiveTypeName getPrimitiveType(String[] columnName) {
      return getColumnTypeInfo(columnName).primitiveType;
    }

    @JsonIgnore
    @Override public OriginalType getOriginalType(String[] columnName) {
      return getColumnTypeInfo(columnName).originalType;
    }

    @JsonIgnore
    @Override
    public Integer getRepetitionLevel(String[] columnName) {
      return getColumnTypeInfo(columnName).repetitionLevel;
    }

    @JsonIgnore
    @Override
    public Integer getDefinitionLevel(String[] columnName) {
      return getColumnTypeInfo(columnName).definitionLevel;
    }

    @JsonIgnore
    @Override
    public Integer getScale(String[] columnName) {
      return getColumnTypeInfo(columnName).scale;
    }

    @JsonIgnore
    @Override
    public Integer getPrecision(String[] columnName) {
      return getColumnTypeInfo(columnName).precision;
    }

    @JsonIgnore
    @Override
    public boolean isRowGroupPrunable() {
      return true;
    }

    @JsonIgnore
    @Override public ParquetTableMetadataBase clone() {
      return new ParquetTableMetadata_v3(metadataVersion, files, directories, columnTypeInfo, drillVersion);
    }

    @JsonIgnore
    @Override
    public String getDrillVersion() {
      return drillVersion;
    }

    @JsonIgnore
    public ConcurrentHashMap<Metadata_V3.ColumnTypeMetadata_v3.Key, Metadata_V3.ColumnTypeMetadata_v3> getColumnTypeInfoMap() {
      return this.columnTypeInfo;
    }

    @Override
    public List<? extends MetadataBase.ColumnTypeMetadata> getColumnTypeInfoList() {
      return new ArrayList<>(this.columnTypeInfo.values());
    }
  }


  /**
   * Struct which contains the metadata for a single parquet file
   */
  public static class ParquetFileMetadata_v3 extends ParquetFileMetadata {
    @JsonProperty public Path path;
    @JsonProperty public Long length;
    @JsonProperty public List<RowGroupMetadata_v3> rowGroups;

    public ParquetFileMetadata_v3() {
    }

    public ParquetFileMetadata_v3(Path path, Long length, List<RowGroupMetadata_v3> rowGroups) {
      this.path = path;
      this.length = length;
      this.rowGroups = rowGroups;
    }

    @Override public String toString() {
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
  public static class RowGroupMetadata_v3 extends RowGroupMetadata {
    @JsonProperty public Long start;
    @JsonProperty public Long length;
    @JsonProperty public Long rowCount;
    @JsonProperty public Map<String, Float> hostAffinity;
    @JsonProperty public List<ColumnMetadata_v3> columns;

    public RowGroupMetadata_v3() {
    }

    public RowGroupMetadata_v3(Long start, Long length, Long rowCount, Map<String, Float> hostAffinity,
                               List<ColumnMetadata_v3> columns) {
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

  public static class ColumnTypeMetadata_v3 extends MetadataBase.ColumnTypeMetadata {
    @JsonProperty public String[] name;
    @JsonProperty public PrimitiveType.PrimitiveTypeName primitiveType;
    @JsonProperty public OriginalType originalType;
    @JsonProperty public int precision;
    @JsonProperty public int scale;
    @JsonProperty public int repetitionLevel;
    @JsonProperty public int definitionLevel;

    // Key to find by name only
    @JsonIgnore private Key key;

    public ColumnTypeMetadata_v3() {
    }

    public ColumnTypeMetadata_v3(String[] name, PrimitiveType.PrimitiveTypeName primitiveType, OriginalType originalType, int precision, int scale, int repetitionLevel, int definitionLevel) {
      this.name = name;
      this.primitiveType = primitiveType;
      this.originalType = originalType;
      this.precision = precision;
      this.scale = scale;
      this.repetitionLevel = repetitionLevel;
      this.definitionLevel = definitionLevel;
      this.key = new Key(name);
    }

    @JsonIgnore private Key key() {
      return this.key;
    }

    public static class Key {
      private SchemaPath name;
      private int hashCode = 0;

      public Key(String[] name) {
        this.name = SchemaPath.getCompoundPath(name);
      }

      public Key(SchemaPath name) {
        this.name = new SchemaPath(name);
      }

      @Override public int hashCode() {
        if (hashCode == 0) {
          hashCode = name.hashCode();
        }
        return hashCode;
      }

      @Override public boolean equals(Object obj) {
        if (obj == null) {
          return false;
        }
        if (getClass() != obj.getClass()) {
          return false;
        }
        final Key other = (Key) obj;
        return this.name.equals(other.name);
      }

      @Override public String toString() {
        return name.toString();
      }

      public static class DeSerializer extends KeyDeserializer {

        public DeSerializer() {
        }

        @Override
        public Object deserializeKey(String key, com.fasterxml.jackson.databind.DeserializationContext ctxt) {
          // key string should contain '`' char if the field was serialized as SchemaPath object
          if (key.contains("`")) {
            return new Key(SchemaPath.parseFromString(key));
          }
          return new Key(key.split("\\."));
        }
      }
    }

    @Override
    public PrimitiveType.PrimitiveTypeName getPrimitiveType() {
      return primitiveType;
    }

    @Override
    public String[] getName() {
      return name;
    }
  }


  /**
   * A struct that contains the metadata for a column in a parquet file
   */
  public static class ColumnMetadata_v3 extends ColumnMetadata {
    // Use a string array for name instead of Schema Path to make serialization easier
    @JsonProperty public String[] name;
    @JsonProperty public Long nulls;

    public Object minValue;
    public Object maxValue;

    @JsonIgnore private PrimitiveType.PrimitiveTypeName primitiveType;

    public ColumnMetadata_v3() {
    }

    public ColumnMetadata_v3(String[] name, PrimitiveType.PrimitiveTypeName primitiveType, Object minValue, Object maxValue, Long nulls) {
      this.name = name;
      this.minValue = minValue;
      this.maxValue = maxValue;
      this.nulls = nulls;
      this.primitiveType = primitiveType;
    }

    @JsonProperty(value = "minValue") public void setMin(Object minValue) {
      this.minValue = minValue;
    }

    @JsonProperty(value = "maxValue") public void setMax(Object maxValue) {
      this.maxValue = maxValue;
    }

    @Override public String[] getName() {
      return name;
    }

    @Override public Long getNulls() {
      return nulls;
    }

    /**
     * Checks that the column chunk has a single value.
     * Returns {@code true} if {@code minValue} and {@code maxValue} are the same but not null
     * and nulls count is 0 or equal to the rows count.
     * <p>
     * Returns {@code true} if {@code minValue} and {@code maxValue} are null and the number of null values
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
      if (isNumNullsSet()) {
        if (minValue != null) {
          // Objects.deepEquals() is used here, since min and max may be byte arrays
          return (nulls == 0 || nulls == rowCount) && Objects.deepEquals(minValue, maxValue);
        } else {
          return nulls == rowCount && maxValue == null;
        }
      }
      return false;
    }

    @Override public Object getMinValue() {
      return minValue;
    }

    @Override public Object getMaxValue() {
      return maxValue;
    }

    @Override public PrimitiveType.PrimitiveTypeName getPrimitiveType() {
      return null;
    }

    @Override public OriginalType getOriginalType() {
      return null;
    }

    // We use a custom serializer and write only non null values.
    public static class Serializer extends JsonSerializer<ColumnMetadata_v3> {
      @Override
      public void serialize(ColumnMetadata_v3 value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        jgen.writeStartObject();
        jgen.writeArrayFieldStart("name");
        for (String n : value.name) {
          jgen.writeString(n);
        }
        jgen.writeEndArray();
        if (value.minValue != null) {
          Object val;
          if (value.primitiveType == PrimitiveType.PrimitiveTypeName.BINARY
              || value.primitiveType == PrimitiveType.PrimitiveTypeName.FIXED_LEN_BYTE_ARRAY) {
            val = ((Binary) value.minValue).getBytes();
          } else {
            val = value.minValue;
          }
          jgen.writeObjectField("minValue", val);
        }
        if (value.maxValue != null) {
          Object val;
          if (value.primitiveType == PrimitiveType.PrimitiveTypeName.BINARY
              || value.primitiveType == PrimitiveType.PrimitiveTypeName.FIXED_LEN_BYTE_ARRAY) {
            val = ((Binary) value.maxValue).getBytes();
          } else {
            val = value.maxValue;
          }
          jgen.writeObjectField("maxValue", val);
        }

        if (value.nulls != null) {
          jgen.writeObjectField("nulls", value.nulls);
        }
        jgen.writeEndObject();
      }
    }

  }

}
