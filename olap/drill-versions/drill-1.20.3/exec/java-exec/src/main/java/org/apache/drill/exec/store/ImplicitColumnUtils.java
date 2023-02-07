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

package org.apache.drill.exec.store;

import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.resultSet.RowSetLoader;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.MetadataUtils;
import org.apache.drill.exec.vector.accessor.ScalarWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class ImplicitColumnUtils {
  private static final Logger logger = LoggerFactory.getLogger(ImplicitColumnUtils.class);

  /**
   * This class represents an implicit column in a dataset.  These columns are typically used for metadata that is consistent
   * across an entire dataset.  A filename for example, or HTTP response codes.  It is a good practice to name
   * implicit fields with an underscore so that these field names do not conflict with fields from the user's
   * data.  For example _http_response_code.
   *
   * Implicit fields do not appear in star queries so a user must explicitly include them in queries for them to appear.
   */
  public static class ImplicitColumns {
    private final Map<String, ImplicitColumn> implicitColumns;
    private final RowSetLoader rowWriter;

    public ImplicitColumns(RowSetLoader rowWriter) {
      this.implicitColumns = new HashMap<>();
      this.rowWriter = rowWriter;
    }

    /**
     * Adds an implicit column.  If there already is a column with the same name, the previous column
     * will be overwritten.
     * @param fieldName Name of the implicit column. Recommended that this start with an underscore.
     * @param type The Drill MinorType of the implicit column.  Currently only supports simple types.
     */
    public void addImplicitColumn(String fieldName, MinorType type) {
      implicitColumns.put(fieldName, new ImplicitColumn(fieldName, type, rowWriter));
    }

    /**
     * Returns a requested ImplicitColumn.  If the column cannot be found, will return null.
     * @param fieldName The field name of the desired column
     * @return The specific column requested, null if that column does not exist.
     */
    public ImplicitColumn getColumn(String fieldName) {
      return implicitColumns.get(fieldName);
    }

    /**
     * This function writes the data to the implicit columns. This should be called in the next() method
     * in a batch reader so that the columns get populated.  If there are no implicit columns, this function
     * will do nothing, so null checks are not necessary.
     */
    public void writeImplicitColumns() {
      ImplicitColumn column;
      ScalarWriter writer;
      MinorType dataType;
      Object value;

      for (Map.Entry<String, ImplicitColumn> columnEntry : implicitColumns.entrySet()) {
        column = columnEntry.getValue();
        writer = column.writer;
        dataType = column.dataType;
        value = column.value;

        switch (dataType) {
          case INT:
            writer.setInt((Integer) value);
            break;
          case BIGINT:
            writer.setLong((Long) value);
            break;
          case FLOAT4:
            writer.setFloat((Float) value);
            break;
          case FLOAT8:
            writer.setDouble((Double) value);
            break;
          case VARCHAR:
            writer.setString((String) value);
            break;
          case BIT:
            writer.setBoolean((Boolean) value);
            break;
          default:
            logger.warn("{} types are not implemented as implicit fields.", dataType);
        }
      }
    }
  }

  public static class ImplicitColumn {
    private final String fieldName;
    private final MinorType dataType;
    private final int columnIndex;
    private final ScalarWriter writer;
    private Object value;

    public ImplicitColumn(String fieldName, MinorType dataType, RowSetLoader rowWriter) {
      this.dataType = dataType;
      this.fieldName = fieldName;
      this.columnIndex = addImplicitColumnToSchema(this.fieldName, this.dataType, rowWriter);
      this.writer = rowWriter.scalar(this.columnIndex);
    }

    public ImplicitColumn(String fieldName, MinorType dataType, RowSetLoader rowWriter, Object value) {
      this.dataType = dataType;
      this.fieldName = fieldName;
      this.columnIndex = addImplicitColumnToSchema(this.fieldName, this.dataType, rowWriter);
      this.writer = rowWriter.scalar(this.columnIndex);
      this.value = value;
    }

    /**
     * Adds an implicit column to the schema. Implicit columns are by default optional and excluded from wildcard
     * queries.  This should be used for file metadata or other metadata that you want to be present in a query, but only if
     * a user specifically asks for it.
     *
     * @param fieldName The name of the implicit column to be added.  Should start with an underscore
     * @param type The minor type of the implicit field.  Currently only non-complex types are supported with this class
     * @param rowWriter The RowSetLoader
     * @return The index of the newly added column.
     */
    private int addImplicitColumnToSchema(String fieldName, MinorType type, RowSetLoader rowWriter) {
      ColumnMetadata colSchema = MetadataUtils.newScalar(fieldName, type, DataMode.OPTIONAL);
      colSchema.setBooleanProperty(ColumnMetadata.EXCLUDE_FROM_WILDCARD, true);
      return rowWriter.addColumn(colSchema);
    }

    public String getFieldName() { return fieldName; }

    public MinorType getDataType() { return dataType; }

    public int getColumnIndex() { return columnIndex; }

    public Object getValue() { return value; }

    public void setValue(Object v) { value = v; }

    public ScalarWriter getWriter() { return writer; }
  }
}
