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
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.util.ArrayList;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.io.api.Binary;
import org.apache.parquet.schema.OriginalType;
import org.apache.parquet.schema.PrimitiveType;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.drill.exec.store.parquet.metadata.MetadataVersion.Constants.V2;
import static org.apache.drill.exec.store.parquet.metadata.MetadataBase.ColumnMetadata;
import static org.apache.drill.exec.store.parquet.metadata.MetadataBase.ParquetFileMetadata;
import static org.apache.drill.exec.store.parquet.metadata.MetadataBase.ParquetTableMetadataBase;
import static org.apache.drill.exec.store.parquet.metadata.MetadataBase.RowGroupMetadata;

public class Metadata_V2 {

  /**
   * Struct which contains the metadata for an entire parquet directory structure
   */
  @JsonTypeName(V2) public static class ParquetTableMetadata_v2 extends ParquetTableMetadataBase {
    @JsonProperty(value = "metadata_version", access = JsonProperty.Access.WRITE_ONLY) private String metadataVersion;
    /*
     ColumnTypeInfo is schema information from all the files and row groups, merged into
     one. To get this info, we pass the ParquetTableMetadata object all the way dow to the
     RowGroup and the column type is built there as it is read from the footer.
     */
    @JsonProperty public ConcurrentHashMap<ColumnTypeMetadata_v2.Key, ColumnTypeMetadata_v2> columnTypeInfo;
    @JsonProperty
    List<ParquetFileMetadata_v2> files;
    @JsonProperty List<Path> directories;
    @JsonProperty String drillVersion;

    public ParquetTableMetadata_v2() {
    }

    public ParquetTableMetadata_v2(String metadataVersion, String drillVersion) {
      this.metadataVersion = metadataVersion;
      this.drillVersion = drillVersion;
    }

    public ParquetTableMetadata_v2(String metadataVersion, ParquetTableMetadataBase parquetTable,
                                   List<ParquetFileMetadata_v2> files, List<Path> directories, String drillVersion) {
      this.metadataVersion = metadataVersion;
      this.files = files;
      this.directories = directories;
      this.columnTypeInfo = ((ParquetTableMetadata_v2) parquetTable).columnTypeInfo;
      this.drillVersion = drillVersion;
    }

    public ParquetTableMetadata_v2(String metadataVersion, List<ParquetFileMetadata_v2> files, List<Path> directories,
                                   ConcurrentHashMap<ColumnTypeMetadata_v2.Key, ColumnTypeMetadata_v2> columnTypeInfo, String drillVersion) {
      this.metadataVersion = metadataVersion;
      this.files = files;
      this.directories = directories;
      this.columnTypeInfo = columnTypeInfo;
      this.drillVersion = drillVersion;
    }

    public ColumnTypeMetadata_v2 getColumnTypeInfo(String[] name) {
      return columnTypeInfo.get(new ColumnTypeMetadata_v2.Key(name));
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
      this.files = (List<ParquetFileMetadata_v2>) newFiles;
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
    @Override public ParquetTableMetadataBase clone() {
      return new ParquetTableMetadata_v2(metadataVersion, files, directories, columnTypeInfo, drillVersion);
    }

    @JsonIgnore
    @Override
    public String getDrillVersion() {
      return drillVersion;
    }

    @JsonIgnore
    @Override public String getMetadataVersion() {
      return metadataVersion;
    }

    @JsonIgnore
    public ConcurrentHashMap<ColumnTypeMetadata_v2.Key, ColumnTypeMetadata_v2> getColumnTypeInfoMap() {
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
  public static class ParquetFileMetadata_v2 extends ParquetFileMetadata {
    @JsonProperty public Path path;
    @JsonProperty public Long length;
    @JsonProperty public List<RowGroupMetadata_v2> rowGroups;

    public ParquetFileMetadata_v2() {
    }

    public ParquetFileMetadata_v2(Path path, Long length, List<RowGroupMetadata_v2> rowGroups) {
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
  public static class RowGroupMetadata_v2 extends RowGroupMetadata {
    @JsonProperty public Long start;
    @JsonProperty public Long length;
    @JsonProperty public Long rowCount;
    @JsonProperty public Map<String, Float> hostAffinity;
    @JsonProperty public List<ColumnMetadata_v2> columns;

    public RowGroupMetadata_v2() {
    }

    public RowGroupMetadata_v2(Long start, Long length, Long rowCount, Map<String, Float> hostAffinity,
                               List<ColumnMetadata_v2> columns) {
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


  public static class ColumnTypeMetadata_v2 extends MetadataBase.ColumnTypeMetadata {
    @JsonProperty public String[] name;
    @JsonProperty public PrimitiveType.PrimitiveTypeName primitiveType;
    @JsonProperty public OriginalType originalType;

    // Key to find by name only
    @JsonIgnore private Key key;

    public ColumnTypeMetadata_v2() {
    }

    public ColumnTypeMetadata_v2(String[] name, PrimitiveType.PrimitiveTypeName primitiveType, OriginalType originalType) {
      this.name = name;
      this.primitiveType = primitiveType;
      this.originalType = originalType;
      this.key = new Key(name);
    }

    @JsonIgnore private Key key() {
      return this.key;
    }

    public static class Key {
      private String[] name;
      private int hashCode = 0;

      public Key(String[] name) {
        this.name = name;
      }

      @Override public int hashCode() {
        if (hashCode == 0) {
          hashCode = Arrays.hashCode(name);
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
        return Arrays.equals(this.name, other.name);
      }

      @Override public String toString() {
        String s = null;
        for (String namePart : name) {
          if (s != null) {
            s += ".";
            s += namePart;
          } else {
            s = namePart;
          }
        }
        return s;
      }

      public static class DeSerializer extends KeyDeserializer {

        public DeSerializer() {
        }

        @Override
        public Object deserializeKey(String key, com.fasterxml.jackson.databind.DeserializationContext ctxt)
            throws IOException, com.fasterxml.jackson.core.JsonProcessingException {
          return new Key(key.split("\\."));
        }
      }
    }

    @Override public PrimitiveType.PrimitiveTypeName getPrimitiveType() {
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
  public static class ColumnMetadata_v2 extends ColumnMetadata {
    // Use a string array for name instead of Schema Path to make serialization easier
    @JsonProperty public String[] name;
    @JsonProperty public Long nulls;

    public Object mxValue;

    @JsonIgnore private PrimitiveType.PrimitiveTypeName primitiveType;

    public ColumnMetadata_v2() {
    }

    public ColumnMetadata_v2(String[] name, PrimitiveType.PrimitiveTypeName primitiveType, Object mxValue, Long nulls) {
      this.name = name;
      this.mxValue = mxValue;
      this.nulls = nulls;
      this.primitiveType = primitiveType;
    }

    @JsonProperty(value = "mxValue") public void setMax(Object mxValue) {
      this.mxValue = mxValue;
    }

    @Override public String[] getName() {
      return name;
    }

    @Override public Long getNulls() {
      return nulls;
    }

    /**
     * Checks that the column chunk has a single value.
     * Returns {@code true} if {@code mxValue} is not null
     * and nulls count is 0 or if nulls count is equal to the rows count.
     * <p>
     * Comparison of nulls and rows count is needed for the cases:
     * <ul>
     * <li>column with primitive type has single value and null values</li>
     *
     * <li>column <b>with binary type</b> has only null values, so column has single value</li>
     * </ul>
     *
     * @param rowCount rows count in column chunk
     * @return true if column has single value
     */
    @Override
    public boolean hasSingleValue(long rowCount) {
      if (nulls != null) {
        return (mxValue != null && nulls == 0) || nulls == rowCount;
      }
      return false;
    }

    @Override public Object getMinValue() {
      return mxValue;
    }

    @Override public Object getMaxValue() {
      return mxValue;
    }

    @Override
    public void setMin(Object newMin) {
      // noop - min value not stored in this version of the metadata
    }

    @Override public PrimitiveType.PrimitiveTypeName getPrimitiveType() {
      return primitiveType;
    }

    @Override public OriginalType getOriginalType() {
      return null;
    }

    public static class DeSerializer extends JsonDeserializer<ColumnMetadata_v2> {
      @Override public ColumnMetadata_v2 deserialize(JsonParser jp, DeserializationContext ctxt)
          throws IOException, JsonProcessingException {
        return null;
      }
    }


    // We use a custom serializer and write only non null values.
    public static class Serializer extends JsonSerializer<ColumnMetadata_v2> {
      @Override
      public void serialize(ColumnMetadata_v2 value, JsonGenerator jgen, SerializerProvider provider)
          throws IOException, JsonProcessingException {
        jgen.writeStartObject();
        jgen.writeArrayFieldStart("name");
        for (String n : value.name) {
          jgen.writeString(n);
        }
        jgen.writeEndArray();
        if (value.mxValue != null) {
          Object val;
          if (value.primitiveType == PrimitiveType.PrimitiveTypeName.BINARY && value.mxValue != null) {
            val = new String(((Binary) value.mxValue).getBytes());
          } else {
            val = value.mxValue;
          }
          jgen.writeObjectField("mxValue", val);
        }
        if (value.nulls != null) {
          jgen.writeObjectField("nulls", value.nulls);
        }
        jgen.writeEndObject();
      }
    }

  }

}
