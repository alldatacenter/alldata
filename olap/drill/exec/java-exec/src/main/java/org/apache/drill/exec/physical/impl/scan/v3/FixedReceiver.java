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
package org.apache.drill.exec.physical.impl.scan.v3;

import org.apache.drill.common.exceptions.CustomErrorContext;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.exec.physical.impl.scan.convert.StandardConversions;
import org.apache.drill.exec.physical.resultSet.ResultSetLoader;
import org.apache.drill.exec.physical.resultSet.RowSetLoader;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.MetadataUtils;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.record.metadata.TupleNameSpace;
import org.apache.drill.exec.record.metadata.TupleSchema;
import org.apache.drill.exec.vector.accessor.ScalarWriter;
import org.apache.drill.exec.vector.accessor.ValueWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Layer above the {@code ResultSetLoader} which handles standard conversions
 * for scalar columns where the schema is known up front (i.e. "early schema".)
 * Columns access is by both name and position, though access by position is
 * faster and is preferred where possible for performance.
 */
public class FixedReceiver {
  private static final Logger logger = LoggerFactory.getLogger(FixedReceiver.class);

  public static class Builder {
    private final SchemaNegotiator negotiator;
    private final TupleMetadata providedSchema;
    private final StandardConversions.Builder conversionBuilder = StandardConversions.builder();
    private boolean isComplete;
    private RowSetLoader rowWriter;

    public Builder(SchemaNegotiator negotiator) {
      this.negotiator = negotiator;
      this.providedSchema = negotiator.providedSchema();
    }

    /**
     * Provides access to the conversion builder to add custom properties.
      */
    public StandardConversions.Builder conversionBuilder() {
      return conversionBuilder;
    }

    /**
     * Mark that the reader schema provided to {@link #build(TupleMetadata)}
     * contains all columns that this reader will deliver. Allows some
     * optimizations. See {@link SchemaNegotiator#schemaIsComplete(boolean)}.
     */
    public Builder schemaIsComplete() {
      isComplete = true;
      return this;
    }

    /**
     * Create a fixed receiver for the provided schema (if any) in the
     * scan plan, and the given reader schema. Assumes no new columns will
     * be added later in the read.
     */
    public FixedReceiver build(TupleMetadata readerSchema) {
      StandardConversions conversions = conversionBuilder.build();
      TupleMetadata writerSchema = mergeSchemas(negotiator.providedSchema(), readerSchema);
      negotiator.tableSchema(writerSchema);
      negotiator.schemaIsComplete(isComplete);
      ResultSetLoader loader = negotiator.build();
      rowWriter = loader.writer();
      TupleNameSpace<ValueWriter> writers = new TupleNameSpace<>();
      for (ColumnMetadata col : readerSchema) {
        writers.add(col.name(), writerFor(col, conversions));
      }
      return new FixedReceiver(rowWriter, writers);
    }

    /**
     * Given a desired provided schema and an actual reader schema, create a merged
     * schema that contains the provided column where available, but the reader
     * column otherwise. Copies provided properties to the output schema.
     * <p>
     * The result is the schema to use when creating column writers: it reflects
     * the type of the target vector. The reader is responsible for converting from
     * the (possibly different) reader column type to the provided column type.
     * <p>
     * Note: the provided schema should only contain types that the reader is prepared
     * to offer: there is no requirement that the reader support every possible conversion,
     * only those that make sense for that one reader.
     *
     * @param providedSchema the provided schema from {@code CREATE SCHEMA}
     * @param readerSchema the set of column types that the reader can provide
     * "natively"
     * @return a merged schema to use when creating the {@code ResultSetLoader}
     */
    public static TupleMetadata mergeSchemas(TupleMetadata providedSchema,
        TupleMetadata readerSchema) {
      if (providedSchema == null) {
        return readerSchema;
      }
      final TupleMetadata tableSchema = new TupleSchema();
      for (ColumnMetadata readerCol : readerSchema) {
        final ColumnMetadata providedCol = providedSchema.metadata(readerCol.name());
        tableSchema.addColumn(providedCol == null ? readerCol : providedCol);
      }
      if (providedSchema.hasProperties()) {
        tableSchema.properties().putAll(providedSchema.properties());
      }
      return tableSchema;
    }

    private ValueWriter writerFor(ColumnMetadata readerCol, StandardConversions conversions) {
      if (!MetadataUtils.isScalar(readerCol)) {
        throw UserException.internalError()
            .message("FixedReceiver only works with scalar columns, reader column is not scalar")
            .addContext("Column name", readerCol.name())
            .addContext("Column type", readerCol.type().name())
            .addContext(errorContext())
            .build(logger);
      }
      ScalarWriter baseWriter = rowWriter.scalar(readerCol.name());
      if (!rowWriter.isProjected(readerCol.name())) {
        return baseWriter;
      }
      ColumnMetadata providedCol = providedCol(readerCol.name());
      if (providedCol == null) {
        return baseWriter;
      }
      if (!MetadataUtils.isScalar(providedCol)) {
        throw UserException.validationError()
          .message("FixedReceiver only works with scalar columns, provided column is not scalar")
          .addContext("Provided column name", providedCol.name())
          .addContext("Provided column type", providedCol.type().name())
          .addContext(errorContext())
          .build(logger);
      }
      if (!compatibleModes(readerCol.mode(), providedCol.mode())) {
        throw UserException.validationError()
          .message("Reader and provided columns have incompatible cardinality")
          .addContext("Column name", providedCol.name())
          .addContext("Provided column mode", providedCol.mode().name())
          .addContext("Reader column mode", readerCol.mode().name())
          .addContext(errorContext())
          .build(logger);
      }
      return conversions.converterFor(baseWriter, readerCol.type());
    }

    private boolean compatibleModes(DataMode source, DataMode dest) {
      return source == dest ||
             dest == DataMode.OPTIONAL && source == DataMode.REQUIRED;
    }

    private ColumnMetadata providedCol(String name) {
      return providedSchema == null ? null : providedSchema.metadata(name);
    }

    private CustomErrorContext errorContext( ) {
      return negotiator.errorContext();
    }
  }

  private final RowSetLoader rowWriter;
  private final TupleNameSpace<ValueWriter> writers;

  private FixedReceiver(RowSetLoader rowWriter,
      TupleNameSpace<ValueWriter> writers) {
    this.rowWriter = rowWriter;
    this.writers = writers;
  }

  public static Builder builderFor(SchemaNegotiator negotiator) {
    return new Builder(negotiator);
  }

  public boolean start() {
    return rowWriter.start();
  }

  public ValueWriter scalar(int index) {
    return writers.get(index);
  }

  public ValueWriter scalar(String name) {
    return writers.get(name);
  }

  public void save() {
    rowWriter.save();
  }

  public RowSetLoader rowWriter() { return rowWriter; }
}
