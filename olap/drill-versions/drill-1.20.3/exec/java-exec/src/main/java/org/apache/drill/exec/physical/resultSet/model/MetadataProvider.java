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
package org.apache.drill.exec.physical.resultSet.model;

import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.MetadataUtils;
import org.apache.drill.exec.record.metadata.RepeatedListColumnMetadata;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.record.metadata.TupleSchema;
import org.apache.drill.exec.record.metadata.VariantMetadata;
import org.apache.drill.exec.record.metadata.VariantSchema;

/**
 * Interface for retrieving and/or creating metadata given a vector.
 * Subclasses either generate metadata to match an existing schema
 * (such as in a vector batch), or walk a metadata schema to drive
 * writer creation.
 */

public interface MetadataProvider {
  ColumnMetadata metadata(int index, MaterializedField field);
  MetadataProvider childProvider(ColumnMetadata colMetadata);
  TupleMetadata tuple();
  VariantMetadata variant();

  class VectorDescrip {
    public final MetadataProvider parent;
    public final ColumnMetadata metadata;

    public VectorDescrip(MetadataProvider provider, ColumnMetadata metadata) {
      this.parent = provider;
      this.metadata = metadata;
    }

    public VectorDescrip(MetadataProvider provider, int index, MaterializedField field) {
      this(provider, provider.metadata(index, field));
    }

    public MetadataProvider childProvider() {
      return parent.childProvider(metadata);
    }
  }

  class MetadataCreator implements MetadataProvider {

    private final TupleSchema tuple;

    public MetadataCreator() {
      tuple = new TupleSchema();
    }

    public MetadataCreator(TupleSchema tuple) {
      this.tuple = tuple;
    }

    @Override
    public ColumnMetadata metadata(int index, MaterializedField field) {
      return tuple.addView(field);
    }

    @Override
    public MetadataProvider childProvider(ColumnMetadata colMetadata) {
      return makeProvider(colMetadata);
    }

    public static MetadataProvider makeProvider(ColumnMetadata colMetadata) {
      switch (colMetadata.structureType()) {
      case MULTI_ARRAY:
        return new ArraySchemaCreator((RepeatedListColumnMetadata) colMetadata);
      case VARIANT:
        return new VariantSchemaCreator((VariantSchema) colMetadata.variantSchema());
      case DICT:
      case TUPLE:
        return new MetadataCreator((TupleSchema) colMetadata.tupleSchema());
      default:
        throw new UnsupportedOperationException();
      }
    }

    @Override
    public TupleMetadata tuple() { return tuple; }

    @Override
    public VariantMetadata variant() {
      throw new UnsupportedOperationException();
    }
  }

  class VariantSchemaCreator implements MetadataProvider {

    private final VariantSchema variantSchema;

    public VariantSchemaCreator(VariantSchema variantSchema) {
      this.variantSchema = variantSchema;
    }

    @Override
    public ColumnMetadata metadata(int index, MaterializedField field) {
      return variantSchema.addType(field);
    }

    @Override
    public MetadataProvider childProvider(ColumnMetadata colMetadata) {
      return MetadataCreator.makeProvider(colMetadata);
    }

    @Override
    public TupleMetadata tuple() {
      throw new UnsupportedOperationException();
    }

    @Override
    public VariantMetadata variant() {
      return variantSchema;
    }
  }

  class ArraySchemaCreator implements MetadataProvider {

    private final RepeatedListColumnMetadata arraySchema;

    public ArraySchemaCreator(RepeatedListColumnMetadata arraySchema) {
      this.arraySchema = arraySchema;
    }

    @Override
    public ColumnMetadata metadata(int index, MaterializedField field) {
      assert index == 0;
      assert arraySchema.childSchema() == null;
      ColumnMetadata childSchema = MetadataUtils.fromField(field.cloneEmpty());
      arraySchema.childSchema(childSchema);
      return childSchema;
    }

    @Override
    public MetadataProvider childProvider(ColumnMetadata colMetadata) {
      return MetadataCreator.makeProvider(colMetadata);
    }

    @Override
    public TupleMetadata tuple() {
      throw new UnsupportedOperationException();
    }

    @Override
    public VariantMetadata variant() {
      throw new UnsupportedOperationException();
    }
  }

  class MetadataRetrieval implements MetadataProvider {

    private final TupleMetadata tuple;

    public MetadataRetrieval(TupleMetadata schema) {
      tuple = schema;
    }

    @Override
    public ColumnMetadata metadata(int index, MaterializedField field) {
      return tuple.metadata(index);
    }

    @Override
    public MetadataProvider childProvider(ColumnMetadata colMetadata) {
      return makeProvider(colMetadata);
    }

    public static MetadataProvider makeProvider(ColumnMetadata colMetadata) {
      switch (colMetadata.structureType()) {
      case MULTI_ARRAY:
        return new ArraySchemaRetrieval(colMetadata);
      case VARIANT:
        return new VariantSchemaRetrieval((VariantSchema) colMetadata.variantSchema());
      case DICT:
      case TUPLE:
        return new MetadataRetrieval(colMetadata.tupleSchema());
      default:
        throw new UnsupportedOperationException();
      }
    }

    @Override
    public TupleMetadata tuple() { return tuple; }

    @Override
    public VariantMetadata variant() {
      throw new UnsupportedOperationException();
    }
  }

  class VariantSchemaRetrieval implements MetadataProvider {

    private final VariantSchema variantSchema;

    public VariantSchemaRetrieval(VariantSchema variantSchema) {
      this.variantSchema = variantSchema;
    }

    @Override
    public ColumnMetadata metadata(int index, MaterializedField field) {
      return variantSchema.member(field.getType().getMinorType());
    }

    @Override
    public MetadataProvider childProvider(ColumnMetadata colMetadata) {
      return MetadataRetrieval.makeProvider(colMetadata);
    }

    @Override
    public TupleMetadata tuple() {
      throw new UnsupportedOperationException();
    }

    @Override
    public VariantMetadata variant() {
      return variantSchema;
    }
  }

  class ArraySchemaRetrieval implements MetadataProvider {

    private final ColumnMetadata arraySchema;

    public ArraySchemaRetrieval(ColumnMetadata arraySchema) {
      this.arraySchema = arraySchema;
    }

    @Override
    public ColumnMetadata metadata(int index, MaterializedField field) {
      assert index == 0;
      return arraySchema.childSchema();
    }

    @Override
    public MetadataProvider childProvider(ColumnMetadata colMetadata) {
      return MetadataRetrieval.makeProvider(colMetadata);
    }

    @Override
    public TupleMetadata tuple() {
      throw new UnsupportedOperationException();
    }

    @Override
    public VariantMetadata variant() {
      throw new UnsupportedOperationException();
    }
  }
}
