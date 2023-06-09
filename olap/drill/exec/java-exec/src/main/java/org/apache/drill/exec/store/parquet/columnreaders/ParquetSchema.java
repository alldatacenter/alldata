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
package org.apache.drill.exec.store.parquet.columnreaders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.Types;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.expr.TypeHelper;
import org.apache.drill.exec.physical.impl.OutputMutator;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.server.options.OptionManager;
import org.apache.drill.exec.store.parquet.ParquetReaderUtility;
import org.apache.drill.exec.vector.NullableIntVector;
import org.apache.parquet.column.ColumnDescriptor;
import org.apache.parquet.format.SchemaElement;
import org.apache.parquet.hadoop.metadata.BlockMetaData;
import org.apache.parquet.hadoop.metadata.ColumnChunkMetaData;
import org.apache.parquet.hadoop.metadata.ParquetMetadata;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

/**
 * Mapping from the schema of the Parquet file to that of the record reader
 * to the schema that Drill and the Parquet reader uses.
 */

public final class ParquetSchema {
  /**
   * Set of columns specified in the SELECT clause. Will be null for
   * a SELECT * query.
   */
  private final Collection<SchemaPath> selectedCols;

  /**
   * Parallel list to the columns list above, it is used to determine the subset of the project
   * pushdown columns that do not appear in this file.
   */
  private final boolean[] columnsFound;
  private final OptionManager options;
  private final int rowGroupIndex;
  private final ParquetMetadata footer;

  /**
   * List of metadata for selected columns. This list does two things.
   * First, it identifies the Parquet columns we wish to select. Second, it
   * provides metadata for those columns. Note that null columns (columns
   * in the SELECT clause but not in the file) appear elsewhere.
   */
  private List<ParquetColumnMetadata> selectedColumnMetadata = new ArrayList<>();
  private int bitWidthAllFixedFields;
  private boolean allFieldsFixedLength;
  private long groupRecordCount;

  /**
   * Build the Parquet schema. The schema can be based on a "SELECT *",
   * meaning we want all columns defined in the Parquet file. In this case,
   * the list of selected columns is null. Or, the query can be based on
   * an explicit list of selected columns. In this case, the
   * columns need not exist in the Parquet file. If a column does not exist,
   * the reader returns null for that column. If no selected column exists
   * in the file, then we return "mock" records: records with only null
   * values, but repeated for the number of rows in the Parquet file.
   *
   * @param options session options
   * @param rowGroupIndex row group to read
   * @param selectedCols columns specified in the SELECT clause, or null if
   * this is a SELECT * query
   */

  public ParquetSchema(OptionManager options, int rowGroupIndex, ParquetMetadata footer, Collection<SchemaPath> selectedCols) {
    this.options = options;
    this.rowGroupIndex = rowGroupIndex;
    this.selectedCols = selectedCols;
    this.footer = footer;
    if (selectedCols == null) {
      columnsFound = null;
    } else {
      columnsFound = new boolean[selectedCols.size()];
    }
  }

  /**
   * Build the schema for this read as a combination of the schema specified in
   * the Parquet footer and the list of columns selected in the query.
   */
  public void buildSchema() {
    BlockMetaData rowGroupMetadata = getRowGroupMetadata();
    groupRecordCount = rowGroupMetadata == null ? 0 : rowGroupMetadata.getRowCount();
    loadParquetSchema();
    computeFixedPart();
  }

  /**
   * Scan the Parquet footer, then map each Parquet column to the list of columns
   * we want to read. Track those to be read.
   */

  private void loadParquetSchema() {
    // TODO - figure out how to deal with this better once we add nested reading, note also look where this map is used below
    // store a map from column name to converted types if they are non-null
    Map<String, SchemaElement> schemaElements = ParquetReaderUtility.getColNameToSchemaElementMapping(footer);

    // loop to add up the length of the fixed width columns and build the schema
    for (ColumnDescriptor column : footer.getFileMetaData().getSchema().getColumns()) {
      ParquetColumnMetadata columnMetadata = new ParquetColumnMetadata(column);
      columnMetadata.resolveDrillType(schemaElements, options);
      if (!columnSelected(column)) {
        continue;
      }
      selectedColumnMetadata.add(columnMetadata);
    }
  }

  /**
   * Fixed-width fields are the easiest to plan. We know the size of each column,
   * making it easy to determine the total length of each vector, once we know
   * the target record count. A special reader is used in the fortunate case
   * that all fields are fixed width.
   */

  private void computeFixedPart() {
    allFieldsFixedLength = true;
    for (ParquetColumnMetadata colMd : selectedColumnMetadata) {
      if (colMd.isFixedLength()) {
        bitWidthAllFixedFields += colMd.length;
      } else {
        allFieldsFixedLength = false;
      }
    }
  }

  public boolean isStarQuery() { return selectedCols == null; }
  public ParquetMetadata footer() { return footer; }
  public int getBitWidthAllFixedFields() { return bitWidthAllFixedFields; }
  public boolean allFieldsFixedLength() { return allFieldsFixedLength; }
  public List<ParquetColumnMetadata> getColumnMetadata() { return selectedColumnMetadata; }

  /**
   * Return the Parquet file row count.
   *
   * @return number of records in the Parquet row group
   */

  public long getGroupRecordCount() { return groupRecordCount; }

  public BlockMetaData getRowGroupMetadata() {
    if (rowGroupIndex == -1) {
      return null;
    }
    return footer.getBlocks().get(rowGroupIndex);
  }

  /**
   * Determine if a Parquet column is selected for the query. It is selected
   * either if this is a star query (we want all columns), or the column
   * appears in the select list.
   *
   * @param column the Parquet column expressed as column descriptor
   * @return true if the column is to be included in the scan, false
   * if not
   */
  private boolean columnSelected(ColumnDescriptor column) {
    if (isStarQuery()) {
      return true;
    }

    int i = 0;
    for (SchemaPath expr : selectedCols) {
      if (ParquetReaderUtility.getFullColumnPath(column).equalsIgnoreCase(expr.getUnIndexed().toString())) {
        columnsFound[i] = true;
        return true;
      }
      i++;
    }
    return false;
  }

  /**
   * Create "dummy" fields for columns which are selected in the SELECT clause, but not
   * present in the Parquet schema.
   * @param output the output container
   * @throws SchemaChangeException should not occur
   */

  public void createNonExistentColumns(OutputMutator output, List<NullableIntVector> nullFilledVectors) throws SchemaChangeException {
    List<SchemaPath> projectedColumns = Lists.newArrayList(selectedCols);
    for (int i = 0; i < columnsFound.length; i++) {
      SchemaPath col = projectedColumns.get(i);
      assert col != null;
      if ( ! columnsFound[i] && ! col.equals(SchemaPath.STAR_COLUMN)) {
        nullFilledVectors.add(createMissingColumn(col, output));
      }
    }
  }

  /**
   * Create a "dummy" column for a missing field. The column is of type optional
   * int, but will always be null.
   *
   * @param col the selected, but non-existent, schema path
   * @param output the output container
   * @return the value vector for the field
   * @throws SchemaChangeException should not occur
   */

  private NullableIntVector createMissingColumn(SchemaPath col, OutputMutator output) throws SchemaChangeException {
    // col.toExpr() is used here as field name since we don't want to see these fields in the existing maps
    MaterializedField field = MaterializedField.create(col.toExpr(),
                                                    Types.optional(TypeProtos.MinorType.INT));
    return (NullableIntVector) output.addField(field,
              TypeHelper.getValueVectorClass(TypeProtos.MinorType.INT, DataMode.OPTIONAL));
  }

  Map<String, Integer> buildChunkMap(BlockMetaData rowGroupMetadata) {
    // the column chunk meta-data is not guaranteed to be in the same order as the columns in the schema
    // a map is constructed for fast access to the correct columnChunkMetadata to correspond
    // to an element in the schema
    Map<String, Integer> columnChunkMetadataPositionsInList = new HashMap<>();

    int colChunkIndex = 0;
    for (ColumnChunkMetaData colChunk : rowGroupMetadata.getColumns()) {
      columnChunkMetadataPositionsInList.put(Arrays.toString(colChunk.getPath().toArray()), colChunkIndex);
      colChunkIndex++;
    }
    return columnChunkMetadataPositionsInList;
  }
}
