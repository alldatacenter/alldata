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
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.schema.OriginalType;
import org.apache.parquet.schema.PrimitiveType;
import org.apache.parquet.schema.Type;

import java.util.List;
import java.util.Map;

import static org.apache.drill.exec.store.parquet.metadata.MetadataVersion.Constants.V1;
import static org.apache.drill.exec.store.parquet.metadata.MetadataVersion.Constants.V2;
import static org.apache.drill.exec.store.parquet.metadata.MetadataVersion.Constants.V3;
import static org.apache.drill.exec.store.parquet.metadata.MetadataVersion.Constants.V3_1;
import static org.apache.drill.exec.store.parquet.metadata.MetadataVersion.Constants.V3_2;
import static org.apache.drill.exec.store.parquet.metadata.MetadataVersion.Constants.V3_3;
import static org.apache.drill.exec.store.parquet.metadata.MetadataVersion.Constants.V4;
import static org.apache.drill.exec.store.parquet.metadata.MetadataVersion.Constants.V4_1;
import static org.apache.drill.exec.store.parquet.metadata.MetadataVersion.Constants.V4_2;

public class MetadataBase {

  /**
   * Basic class for parquet metadata. Inheritors of this class are json serializable structures which contain
   * different metadata versions for an entire parquet directory structure
   * <p>
   * If any new code changes affect on the metadata files content, please update metadata version in such manner:
   * Bump up metadata major version if metadata structure is changed.
   * Bump up metadata minor version if only metadata content is changed, but metadata structure is the same.
   * <p>
   * Note: keep metadata versions synchronized with {@link MetadataVersion.Constants}
   */
  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "metadata_version", visible = true)
  @JsonSubTypes({
      @JsonSubTypes.Type(value = Metadata_V1.ParquetTableMetadata_v1.class, name = V1),
      @JsonSubTypes.Type(value = Metadata_V2.ParquetTableMetadata_v2.class, name = V2),
      @JsonSubTypes.Type(value = Metadata_V3.ParquetTableMetadata_v3.class, name = V3),
      @JsonSubTypes.Type(value = Metadata_V3.ParquetTableMetadata_v3.class, name = V3_1),
      @JsonSubTypes.Type(value = Metadata_V3.ParquetTableMetadata_v3.class, name = V3_2),
      @JsonSubTypes.Type(value = Metadata_V3.ParquetTableMetadata_v3.class, name = V3_3),
      @JsonSubTypes.Type(value = Metadata_V4.ParquetTableMetadata_v4.class, name = V4),
      @JsonSubTypes.Type(value = Metadata_V4.ParquetTableMetadata_v4.class, name = V4_1),
      @JsonSubTypes.Type(value = Metadata_V4.ParquetTableMetadata_v4.class, name = V4_2),

  })
  public static abstract class ParquetTableMetadataBase {

    @JsonIgnore
    public abstract List<Path> getDirectories();

    @JsonIgnore public abstract List<? extends ParquetFileMetadata> getFiles();

    @JsonIgnore public abstract void assignFiles(List<? extends ParquetFileMetadata> newFiles);

    public abstract boolean hasColumnMetadata();

    @JsonIgnore public abstract PrimitiveType.PrimitiveTypeName getPrimitiveType(String[] columnName);

    @JsonIgnore public abstract OriginalType getOriginalType(String[] columnName);

    @JsonIgnore public abstract Integer getRepetitionLevel(String[] columnName);

    @JsonIgnore public abstract Integer getDefinitionLevel(String[] columnName);

    @JsonIgnore public abstract Integer getScale(String[] columnName);

    @JsonIgnore public abstract Integer getPrecision(String[] columnName);

    @JsonIgnore public abstract boolean isRowGroupPrunable();

    @JsonIgnore public abstract ParquetTableMetadataBase clone();

    @JsonIgnore public abstract String getDrillVersion();

    @JsonIgnore public abstract String getMetadataVersion();

    @JsonIgnore  public abstract List<? extends ColumnTypeMetadata> getColumnTypeInfoList();

    @JsonIgnore
    public Type.Repetition getRepetition(String[] columnName) {
      return null;
    }
  }

  public static abstract class ParquetFileMetadata {
    @JsonIgnore public abstract Path getPath();

    @JsonIgnore public abstract Long getLength();

    @JsonIgnore public abstract List<? extends RowGroupMetadata> getRowGroups();
  }


  public static abstract class RowGroupMetadata {
    @JsonIgnore public abstract Long getStart();

    @JsonIgnore public abstract Long getLength();

    @JsonIgnore public abstract Long getRowCount();

    @JsonIgnore public abstract Map<String, Float> getHostAffinity();

    @JsonIgnore public abstract List<? extends ColumnMetadata> getColumns();

    @JsonIgnore public boolean isEmpty() {
      Long rowCount = getRowCount();
      return rowCount != null && rowCount == 0;
    }
  }

  public static abstract class ColumnMetadata {

    /**
     * Number of nulls is considered to be valid if its value is not null and -1.
     *
     * @return true if nulls value is defined, false otherwise
     */
    @JsonIgnore
    public boolean isNumNullsSet() {
      Long nulls = getNulls();
      return nulls != null && nulls != -1;
    }

    public abstract String[] getName();

    public abstract Long getNulls();

    public abstract boolean hasSingleValue(long rowCount);

    public abstract Object getMinValue();

    public abstract Object getMaxValue();

    /**
     * Set the max value recorded in the parquet metadata statistics.
     *
     * This object would just be immutable, but due to Drill-4203 we need to correct
     * date values that had been corrupted by earlier versions of Drill.
     */
    public abstract void setMax(Object newMax);

    /**
     * Set the min value recorded in the parquet metadata statistics.
     *
     * This object would just be immutable, but due to Drill-4203 we need to correct
     * date values that had been corrupted by earlier versions of Drill.
     */
    public abstract void setMin(Object newMax);

    public abstract PrimitiveType.PrimitiveTypeName getPrimitiveType();

    public abstract OriginalType getOriginalType();

  }

  public static abstract class ColumnTypeMetadata {

    public abstract PrimitiveType.PrimitiveTypeName getPrimitiveType();

    public abstract String[] getName();

  }
}
