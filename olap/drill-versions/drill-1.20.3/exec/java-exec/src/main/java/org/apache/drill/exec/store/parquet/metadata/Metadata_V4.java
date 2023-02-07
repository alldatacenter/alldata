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
import com.fasterxml.jackson.databind.KeyDeserializer;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.util.DrillVersionInfo;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.schema.OriginalType;
import org.apache.parquet.schema.PrimitiveType;
import org.apache.parquet.schema.Type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.drill.exec.store.parquet.metadata.MetadataBase.ColumnMetadata;
import static org.apache.drill.exec.store.parquet.metadata.MetadataBase.ColumnTypeMetadata;
import static org.apache.drill.exec.store.parquet.metadata.MetadataBase.ParquetFileMetadata;
import static org.apache.drill.exec.store.parquet.metadata.MetadataBase.ParquetTableMetadataBase;
import static org.apache.drill.exec.store.parquet.metadata.MetadataBase.RowGroupMetadata;
import static org.apache.drill.exec.store.parquet.metadata.MetadataVersion.Constants.V4_2;

public class Metadata_V4 {

  public static class ParquetTableMetadata_v4 extends ParquetTableMetadataBase {

    MetadataSummary metadataSummary = new MetadataSummary();
    FileMetadata fileMetadata = new FileMetadata();

    public ParquetTableMetadata_v4() {
      this.metadataSummary = new MetadataSummary(MetadataVersion.Constants.V4_1, DrillVersionInfo.getVersion(), false);
    }

    public ParquetTableMetadata_v4(MetadataSummary metadataSummary) {
      this.metadataSummary = metadataSummary;
    }

    public ParquetTableMetadata_v4(MetadataSummary metadataSummary, FileMetadata fileMetadata) {
      this.metadataSummary = metadataSummary;
      this.fileMetadata = fileMetadata;
    }

    public ParquetTableMetadata_v4(String metadataVersion, ParquetTableMetadataBase parquetTableMetadata,
                                   List<ParquetFileMetadata_v4> files, List<Path> directories, String drillVersion, long totalRowCount, boolean allColumnsInteresting) {
      this.metadataSummary.metadataVersion = metadataVersion;
      this.fileMetadata.files = files;
      this.metadataSummary.directories = directories;
      this.metadataSummary.columnTypeInfo = ((ParquetTableMetadata_v4) parquetTableMetadata).metadataSummary.columnTypeInfo;
      this.metadataSummary.drillVersion = drillVersion;
      this.metadataSummary.totalRowCount = totalRowCount;
      this.metadataSummary.allColumnsInteresting = allColumnsInteresting;
    }

    public ColumnTypeMetadata_v4 getColumnTypeInfo(String[] name) {
      return metadataSummary.getColumnTypeInfo(name);
    }

    @Override
    public List<Path> getDirectories() {
      return metadataSummary.getDirectories();
    }

    @Override
    public List<? extends ParquetFileMetadata> getFiles() {
      return fileMetadata.getFiles();
    }

    @Override
    public String getMetadataVersion() {
      return metadataSummary.getMetadataVersion();
    }

    /**
     * If directories list and file metadata list contain relative paths, update it to absolute ones
     *
     * @param baseDir base parent directory
     */
    public void updateRelativePaths(String baseDir) {
      // update directories paths to absolute ones
      this.metadataSummary.directories = MetadataPathUtils.convertToAbsolutePaths(metadataSummary.directories, baseDir);

      // update files paths to absolute ones
      this.fileMetadata.files = (List<ParquetFileMetadata_v4>) MetadataPathUtils.convertToFilesWithAbsolutePaths(fileMetadata.files, baseDir);
    }

    @Override
    public void assignFiles(List<? extends ParquetFileMetadata> newFiles) {
      this.fileMetadata.assignFiles(newFiles);
    }

    @Override
    public boolean hasColumnMetadata() {
      return true;
    }

    @Override
    public PrimitiveType.PrimitiveTypeName getPrimitiveType(String[] columnName) {
      return getColumnTypeInfo(columnName).primitiveType;
    }

    @Override
    public OriginalType getOriginalType(String[] columnName) {
      return getColumnTypeInfo(columnName).originalType;
    }

    @Override
    public Integer getRepetitionLevel(String[] columnName) {
      return getColumnTypeInfo(columnName).repetitionLevel;
    }

    @Override
    public Integer getDefinitionLevel(String[] columnName) {
      return getColumnTypeInfo(columnName).definitionLevel;
    }

    @Override
    public Integer getScale(String[] columnName) {
      return getColumnTypeInfo(columnName).scale;
    }

    @Override
    public Integer getPrecision(String[] columnName) {
      return getColumnTypeInfo(columnName).precision;
    }

    @Override
    public boolean isRowGroupPrunable() {
      return true;
    }

    @Override
    public ParquetTableMetadataBase clone() {
      return new ParquetTableMetadata_v4(metadataSummary, fileMetadata);
    }

    @Override
    public String getDrillVersion() {
      return metadataSummary.drillVersion;
    }

    @Override
    public Type.Repetition getRepetition(String[] columnName) {
      return getColumnTypeInfo(columnName).repetition;
    }

    public MetadataSummary getSummary() {
      return metadataSummary;
    }

    public long getTotalRowCount() {
      return metadataSummary.getTotalRowCount();
    }

    public long getTotalNullCount(String[] columnName) {
      return getColumnTypeInfo(columnName).totalNullCount;
    }

    public boolean isAllColumnsInteresting() {
      return metadataSummary.isAllColumnsInteresting();
    }

    public ConcurrentHashMap<ColumnTypeMetadata_v4.Key, ColumnTypeMetadata_v4> getColumnTypeInfoMap() {
      return metadataSummary.columnTypeInfo;
    }

    @Override
    public List<? extends MetadataBase.ColumnTypeMetadata> getColumnTypeInfoList() {
      return new ArrayList<>(metadataSummary.columnTypeInfo.values());
    }

    public void setTotalRowCount(long totalRowCount) {
      metadataSummary.setTotalRowCount(totalRowCount);
    }
  }

  /**
   * Struct which contains the metadata for a single parquet file
   */
  public static class ParquetFileMetadata_v4 extends ParquetFileMetadata {
    @JsonProperty
    public Path path;
    @JsonProperty
    public Long length;
    @JsonProperty
    public List<RowGroupMetadata_v4> rowGroups;

    public ParquetFileMetadata_v4() {

    }

    public ParquetFileMetadata_v4(Path path, Long length, List<RowGroupMetadata_v4> rowGroups) {
      this.path = path;
      this.length = length;
      this.rowGroups = rowGroups;
    }

    @Override
    public String toString() {
      return String.format("path: %s rowGroups: %s", path, rowGroups);
    }

    @JsonIgnore
    @Override
    public Path getPath() {
      return path;
    }

    @JsonIgnore
    @Override
    public Long getLength() {
      return length;
    }

    @JsonIgnore
    @Override
    public List<? extends RowGroupMetadata> getRowGroups() {
      return rowGroups;
    }
  }


  /**
   * A struct that contains the metadata for a parquet row group
   */
  public static class RowGroupMetadata_v4 extends RowGroupMetadata {
    @JsonProperty
    public Long start;
    @JsonProperty
    public Long length;
    @JsonProperty
    public Long rowCount;
    @JsonProperty
    public Map<String, Float> hostAffinity;
    @JsonProperty
    public List<ColumnMetadata_v4> columns;

    public RowGroupMetadata_v4() {
    }

    public RowGroupMetadata_v4(Long start, Long length, Long rowCount, Map<String, Float> hostAffinity,
                               List<ColumnMetadata_v4> columns) {
      this.start = start;
      this.length = length;
      this.rowCount = rowCount;
      this.hostAffinity = hostAffinity;
      this.columns = columns;
    }

    @Override
    public Long getStart() {
      return start;
    }

    @Override
    public Long getLength() {
      return length;
    }

    @Override
    public Long getRowCount() {
      return rowCount;
    }

    @Override
    public Map<String, Float> getHostAffinity() {
      return hostAffinity;
    }

    @Override
    public List<? extends ColumnMetadata> getColumns() {
      return columns;
    }
  }


  public static class ColumnTypeMetadata_v4 extends ColumnTypeMetadata {
    @JsonProperty
    public String[] name;
    @JsonProperty
    public PrimitiveType.PrimitiveTypeName primitiveType;
    @JsonProperty
    public OriginalType originalType;
    @JsonProperty
    public List<OriginalType> parentTypes;
    @JsonProperty
    public int precision;
    @JsonProperty
    public int scale;
    @JsonProperty
    public int repetitionLevel;
    @JsonProperty
    public int definitionLevel;
    @JsonProperty
    public long totalNullCount = 0;
    @JsonProperty
    public boolean isInteresting = false;
    @JsonProperty
    public Type.Repetition repetition;

    // Key to find by name only
    @JsonIgnore
    private Key key;

    public ColumnTypeMetadata_v4() {
    }

    private ColumnTypeMetadata_v4(Builder builder) {
      this.name = builder.name;
      this.primitiveType = builder.primitiveType;
      this.originalType = builder.originalType;
      this.precision = builder.precision;
      this.scale = builder.scale;
      this.repetitionLevel = builder.repetitionLevel;
      this.definitionLevel = builder.definitionLevel;
      this.key = new Key(name);
      this.totalNullCount = builder.totalNullCount;
      this.isInteresting = builder.isInteresting;
      this.parentTypes = Collections.unmodifiableList(builder.parentTypes);
      this.repetition = builder.repetition;
    }

    @JsonIgnore
    private Key key() {
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

      @Override
      public int hashCode() {
        if (hashCode == 0) {
          hashCode = name.hashCode();
        }
        return hashCode;
      }

      @Override
      public boolean equals(Object obj) {
        if (obj == null) {
          return false;
        }
        if (getClass() != obj.getClass()) {
          return false;
        }
        final Key other = (Key) obj;
        return this.name.equals(other.name);
      }

      @Override
      public String toString() {
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

    @JsonIgnore
    @Override
    public PrimitiveType.PrimitiveTypeName getPrimitiveType() {
      return primitiveType;
    }

    @JsonIgnore
    @Override
    public String[] getName() {
      return name;
    }

    public static class Builder {

      private String[] name;
      private PrimitiveType.PrimitiveTypeName primitiveType;
      private OriginalType originalType;
      private List<OriginalType> parentTypes;
      private int precision;
      private int scale;
      private int repetitionLevel;
      private int definitionLevel;
      private long totalNullCount;
      private boolean isInteresting;
      private Type.Repetition repetition;

      public Builder name(String[] name) {
        this.name = name;
        return this;
      }

      public Builder primitiveType(PrimitiveType.PrimitiveTypeName primitiveType) {
        this.primitiveType = primitiveType;
        return this;
      }

      public Builder originalType(OriginalType originalType) {
        this.originalType = originalType;
        return this;
      }

      public Builder parentTypes(List<OriginalType> parentTypes) {
        this.parentTypes = parentTypes;
        return this;
      }

      public Builder precision(int precision) {
        this.precision = precision;
        return this;
      }

      public Builder scale(int scale) {
        this.scale = scale;
        return this;
      }

      public Builder repetitionLevel(int repetitionLevel) {
        this.repetitionLevel = repetitionLevel;
        return this;
      }

      public Builder definitionLevel(int definitionLevel) {
        this.definitionLevel = definitionLevel;
        return this;
      }

      public Builder totalNullCount(long totalNullCount) {
        this.totalNullCount = totalNullCount;
        return this;
      }

      public Builder interesting(boolean isInteresting) {
        this.isInteresting = isInteresting;
        return this;
      }

      public Builder repetition(Type.Repetition repetition) {
        this.repetition = repetition;
        return this;
      }

      public ColumnTypeMetadata_v4 build() {
        return new ColumnTypeMetadata_v4(this);
      }
    }
  }

  /**
   * A struct that contains the metadata for a column in a parquet file.
   * Note: Since the structure of column metadata hasn't changes from v3, ColumnMetadata_v4 extends ColumnMetadata_v3
   */
  public static class ColumnMetadata_v4 extends Metadata_V3.ColumnMetadata_v3 {
    public ColumnMetadata_v4() {
    }

    public ColumnMetadata_v4(String[] name, PrimitiveType.PrimitiveTypeName primitiveType, Object minValue, Object maxValue, Long nulls) {
      super(name, primitiveType, minValue, maxValue, nulls);
    }
  }

  @JsonTypeName(V4_2)
  public static class MetadataSummary {

    @JsonProperty(value = "metadata_version")
    private String metadataVersion;
    /*
     ColumnTypeInfo is schema information from all the files and row groups, merged into
     one. To get this info, we pass the ParquetTableMetadata object all the way down to the
     RowGroup and the column type is built there as it is read from the footer.
     */
    @JsonProperty
    ConcurrentHashMap<ColumnTypeMetadata_v4.Key, ColumnTypeMetadata_v4> columnTypeInfo = new ConcurrentHashMap<>();
    @JsonProperty
    List<Path> directories;
    @JsonProperty
    String drillVersion;
    @JsonProperty
    long totalRowCount = 0;
    @JsonProperty
    boolean allColumnsInteresting = false;

    public MetadataSummary() {

    }

    public MetadataSummary(String metadataVersion, String drillVersion, boolean allColumnsInteresting) {
      this(metadataVersion, drillVersion, new ArrayList<>(), allColumnsInteresting);
    }

    public MetadataSummary(String metadataVersion, String drillVersion, List<Path> directories, boolean allColumnsInteresting) {
      this.metadataVersion = metadataVersion;
      this.drillVersion = drillVersion;
      this.directories = directories;
      this.allColumnsInteresting = allColumnsInteresting;
    }

    @JsonIgnore
    public ColumnTypeMetadata_v4 getColumnTypeInfo(String[] name) {
      return columnTypeInfo.get(new ColumnTypeMetadata_v4.Key(name));
    }

    @JsonIgnore
    public ColumnTypeMetadata_v4 getColumnTypeInfo(ColumnTypeMetadata_v4.Key key) {
      return columnTypeInfo.get(key);
    }

    @JsonIgnore
    public List<Path> getDirectories() {
      return directories;
    }

    @JsonIgnore
    public String getMetadataVersion() {
      return metadataVersion;
    }

    @JsonIgnore
    public boolean isAllColumnsInteresting() {
      return allColumnsInteresting;
    }

    @JsonIgnore
    public void setAllColumnsInteresting(boolean allColumnsInteresting) {
      this.allColumnsInteresting = allColumnsInteresting;
    }

    @JsonIgnore
    public void setTotalRowCount(Long totalRowCount) {
      this.totalRowCount = totalRowCount;
    }

    @JsonIgnore
    public Long getTotalRowCount() {
      return this.totalRowCount;
    }
  }

  /*
   * A struct that holds list of file metadata in a directory
   */
  public static class FileMetadata {

    @JsonProperty
    List<ParquetFileMetadata_v4> files;

    public FileMetadata() {
    }

    @JsonIgnore
    public List<ParquetFileMetadata_v4> getFiles() {
      return files;
    }

    @JsonIgnore
    public void assignFiles(List<? extends ParquetFileMetadata> newFiles) {
      this.files = (List<ParquetFileMetadata_v4>) newFiles;
    }
  }

  /*
   * A struct that holds file metadata and row count and null count of a single file
   */
  public static class ParquetFileAndRowCountMetadata {
    ParquetFileMetadata_v4 fileMetadata;
    Map<ColumnTypeMetadata_v4.Key, Long> totalNullCountMap;
    long fileRowCount;

    public ParquetFileAndRowCountMetadata() {
    }

    public ParquetFileAndRowCountMetadata(ParquetFileMetadata_v4 fileMetadata, Map<ColumnTypeMetadata_v4.Key, Long> totalNullCountMap, long fileRowCount) {
      this.fileMetadata = fileMetadata;
      this.totalNullCountMap = totalNullCountMap;
      this.fileRowCount = fileRowCount;
    }

    public ParquetFileMetadata_v4 getFileMetadata() {
      return this.fileMetadata;
    }

    public long getFileRowCount() {
      return this.fileRowCount;
    }

    public Map<ColumnTypeMetadata_v4.Key, Long> getTotalNullCountMap() {
      return totalNullCountMap;
    }
  }
}
