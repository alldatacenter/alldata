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
package org.apache.drill.exec.vector.complex.fn;

import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.vector.complex.impl.ComplexCopier;
import org.apache.drill.common.expression.PathSegment;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.vector.complex.writer.BaseWriter;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;

public class JsonReaderUtils {

  public static void ensureAtLeastOneField(BaseWriter.ComplexWriter writer,
                                    Collection<SchemaPath> columns,
                                    boolean allTextMode,
                                    List<BaseWriter.ListWriter> emptyArrayWriters) {

    List<BaseWriter.MapWriter> writerList = new ArrayList<>();
    List<PathSegment> fieldPathList = new ArrayList<>();
    BitSet emptyWriters = new BitSet(columns.size());
    int fieldIndex = 0;

    // first pass: collect which fields are empty
    for (SchemaPath schemaPath : columns) {
      PathSegment fieldPath = schemaPath.getRootSegment();
      BaseWriter.MapWriter fieldWriter = writer.rootAsMap();
      while (fieldPath.getChild() != null && !fieldPath.getChild().isArray()) {
        fieldWriter = fieldWriter.map(fieldPath.getNameSegment().getPath());
        fieldPath = fieldPath.getChild();
      }
      writerList.add(fieldWriter);
      fieldPathList.add(fieldPath);
      if (fieldWriter.isEmptyMap()) {
        emptyWriters.set(fieldIndex, true);
      }
      if (fieldIndex == 0 && !allTextMode) {
        // when allTextMode is false, there is not much benefit to producing all
        // the empty fields; just produce 1 field. The reason is that the type of the
        // fields is unknown, so if we produce multiple Integer fields by default, a
        // subsequent batch that contains non-integer fields will error out in any case.
        // Whereas, with allTextMode true, we are sure that all fields are going to be
        // treated as varchar, so it makes sense to produce all the fields, and in fact
        // is necessary in order to avoid schema change exceptions by downstream operators.
        break;
      }
      fieldIndex++;
    }

    // second pass: create default typed vectors corresponding to empty fields
    // Note: this is not easily do-able in 1 pass because the same fieldWriter
    // may be shared by multiple fields whereas we want to keep track of all fields
    // independently, so we rely on the emptyWriters.
    for (int j = 0; j < fieldPathList.size(); j++) {
      BaseWriter.MapWriter fieldWriter = writerList.get(j);
      PathSegment fieldPath = fieldPathList.get(j);
      if (emptyWriters.get(j)) {
        if (allTextMode) {
          fieldWriter.varChar(fieldPath.getNameSegment().getPath());
        } else {
          fieldWriter.integer(fieldPath.getNameSegment().getPath());
        }
      }
    }

    for (BaseWriter.ListWriter field : emptyArrayWriters) {
      // checks that array has not been initialized
      if (field.getValueCapacity() == 0) {
        if (allTextMode) {
          field.varChar();
        } else {
          field.integer();
        }
      }
    }
  }

  /**
   * Creates writers which correspond to the specified schema for specified root writer.
   *
   * @param writer      parent writer for writers to create
   * @param columns     collection of columns for which writers should be created
   * @param schema      table schema
   * @param allTextMode whether all primitive writers should be of varchar type
   */
  public static void writeColumnsUsingSchema(BaseWriter.ComplexWriter writer,
      Collection<SchemaPath> columns, TupleMetadata schema, boolean allTextMode) {
    BaseWriter.MapWriter mapWriter = writer.rootAsMap();
    for (SchemaPath column : columns) {
      if (column.isDynamicStar()) {
        writeSchemaColumns(schema, mapWriter, allTextMode);
      } else {
        ColumnMetadata columnMetadata = schema.metadata(column.getRootSegmentPath());
        writeColumnToMapWriter(mapWriter, column.getRootSegment(), columnMetadata, allTextMode);
      }
    }
  }

  /**
   * Creates writer for column which corresponds to specified {@code PathSegment column} with type taken from
   * the specified {@code ColumnMetadata columnMetadata}.
   * For the case when specified {@code PathSegment column} is map, writers only for its child segments will be created.
   * For the case when specified {@code PathSegment column} is array segment, all child writers will be created.
   *
   * @param writer         parent writer for writers to create
   * @param column         column for which writers should be created
   * @param columnMetadata column metadata
   * @param allTextMode    whether all primitive writers should be of varchar type
   */
  private static void writeColumnToMapWriter(BaseWriter.MapWriter writer,
      PathSegment column, ColumnMetadata columnMetadata, boolean allTextMode) {
    PathSegment child = column.getChild();
    if (child != null && child.isNamed()) {
      String name = column.getNameSegment().getPath();
      ColumnMetadata childMetadata = columnMetadata.tupleSchema().metadata(name);
      writeColumnToMapWriter(writer.map(name), child, childMetadata, allTextMode);
    } else {
      writeSingleOrArrayColumn(columnMetadata, writer, allTextMode);
    }
  }

  /**
   * Creates writers for specified {@code ColumnMetadata columnMetadata}.
   * For the case when column is array, creates list writer and required child writers.
   *
   * @param columnMetadata column metadata
   * @param writer         parent writer for writers to create
   * @param allTextMode    whether all primitive writers should be of varchar type
   */
  private static void writeSingleOrArrayColumn(ColumnMetadata columnMetadata,
      BaseWriter.MapWriter writer, boolean allTextMode) {
    if (columnMetadata.isArray()) {
      writeArrayColumn(columnMetadata, writer.list(columnMetadata.name()), allTextMode);
    } else {
      writeColumn(columnMetadata, writer, allTextMode);
    }
  }

  /**
   * Creates writers for all columns taken from {@code TupleMetadata schema}.
   *
   * @param schema      table or map schema
   * @param fieldWriter parent writer for writers to create
   * @param allTextMode whether all primitive writers should be of varchar type
   */
  private static void writeSchemaColumns(TupleMetadata schema, BaseWriter.MapWriter fieldWriter,
      boolean allTextMode) {
    for (ColumnMetadata columnMetadata : schema) {
      writeSingleOrArrayColumn(columnMetadata, fieldWriter, allTextMode);
    }
  }

  /**
   * Creates writers for specified {@code ColumnMetadata columnMetadata} considering its children.
   * For the case of {@code TUPLE} or {@code MULTI_ARRAY}, all child writers will be created recursively.
   *
   * @param columnMetadata column metadata
   * @param fieldWriter    parent writer for writers to create
   * @param allTextMode    whether all primitive writers should be of varchar type
   */
  private static void writeColumn(ColumnMetadata columnMetadata,
      BaseWriter.MapWriter fieldWriter, boolean allTextMode) {
    switch (columnMetadata.structureType()) {
      case DICT:
        writeSchemaColumns(
            columnMetadata.tupleSchema(), fieldWriter.dict(columnMetadata.name()), allTextMode);
        break;
      case TUPLE:
        writeSchemaColumns(
            columnMetadata.tupleSchema(), fieldWriter.map(columnMetadata.name()), allTextMode);
        break;
      case MULTI_ARRAY:
        writeArrayColumn(columnMetadata.childSchema(), fieldWriter.list(columnMetadata.name()), allTextMode);
        break;
      case PRIMITIVE:
        if (allTextMode) {
          fieldWriter.varChar(columnMetadata.name());
        } else {
          ComplexCopier.getMapWriterForType(
              columnMetadata.majorType(), fieldWriter, columnMetadata.name());
        }
        break;
      default:
        throw new UnsupportedOperationException(
            String.format("Unsupported type [%s] for column [%s].", columnMetadata.majorType(), columnMetadata.name()));
    }
  }

  /**
   * Writes column which corresponds to specified {@code ColumnMetadata columnMetadata}
   * into specified {@code BaseWriter.ListWriter fieldWriter}.
   *
   * @param columnMetadata column metadata
   * @param fieldWriter    parent writer for writers to create
   * @param allTextMode    whether all primitive writers should be of varchar type
   */
  private static void writeArrayColumn(ColumnMetadata columnMetadata,
      BaseWriter.ListWriter fieldWriter, boolean allTextMode) {
    switch (columnMetadata.structureType()) {
      case DICT:
        writeSchemaColumns(columnMetadata.tupleSchema(), fieldWriter.dict(), allTextMode);
        break;
      case TUPLE:
        writeSchemaColumns(columnMetadata.tupleSchema(), fieldWriter.map(), allTextMode);
        break;
      case MULTI_ARRAY:
        writeArrayColumn(columnMetadata.childSchema(), fieldWriter.list(), allTextMode);
        break;
      case PRIMITIVE:
        if (allTextMode) {
          fieldWriter.varChar();
        } else {
          ComplexCopier.getListWriterForType(columnMetadata.majorType(), fieldWriter);
        }
        break;
      default:
        throw new UnsupportedOperationException(
            String.format("Unsupported type [%s] for column [%s].", columnMetadata.majorType(), columnMetadata.name()));
    }
  }
}
