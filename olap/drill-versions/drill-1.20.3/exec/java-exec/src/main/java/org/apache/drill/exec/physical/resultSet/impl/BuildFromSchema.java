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
package org.apache.drill.exec.physical.resultSet.impl;

import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.record.metadata.VariantMetadata;
import org.apache.drill.exec.vector.accessor.ObjectWriter;
import org.apache.drill.exec.vector.accessor.TupleWriter;
import org.apache.drill.exec.vector.accessor.VariantWriter;
import org.apache.drill.exec.vector.accessor.writer.RepeatedListWriter;

/**
 * Build the set of writers from a defined schema. Uses the same
 * mechanism as dynamic schema: walks the schema tree adding each
 * column, then recursively adds the contents of maps and variants.
 * <p>
 * Recursion is much easier if we can go bottom-up. But, writers
 * require top-down construction.
 * <p>
 * This particular class builds a column and all its contents.
 * For example, given a map, which contains a repeated list which
 * contains a repeated INT, this class first builds the map,
 * then adds the repeated list, then adds the INT array. To do
 * so, it will create a copy of the structured metadata.
 * <p>
 * A drawback of this approach is that the metadata objects used
 * in the "parent" writers will be copies of, not the same as, those
 * in the schema from which we are building the writers. At present,
 * this is not an issue, but it is something to be aware of as uses
 * become more sophisticated.
 * <p>
 * This class contrasts with the @{link ColumnBuilder} class which
 * builds the structure within a single vector and writer.
 */

public class BuildFromSchema {

  /**
   * The shim interface provides a uniform way to add a column
   * to a parent structured column writer without the need for
   * a bunch of if-statements. Tuples (i.e. maps), UNIONs and
   * repeated LISTs all are structured (composite) columns,
   * but have slightly different semantics. This shim wraps
   * the semantics so the builder code is simpler.
   */

  private interface ParentShim {
    ObjectWriter add(ColumnMetadata colSchema);
  }

  /**
   * Shim used for adding a column to a tuple directly.
   * This method will recursively invoke this builder
   * to expand any nested content.
   */
  private static class TupleShim implements ParentShim {
    private final TupleWriter writer;

    public TupleShim(TupleWriter writer) {
      this.writer = writer;
    }

    @Override
    public ObjectWriter add(ColumnMetadata colSchema) {
      final int index = writer.addColumn(colSchema);
      return writer.column(index);
    }
  }

  /**
   * Shim used when implementing the add of a column to
   * a tuple in the result set loader. Directly calls the
   * internal method to add a column to the "tuple state."
   */
  private static class TupleStateShim implements ParentShim {
    private final TupleState state;

    public TupleStateShim(TupleState state) {
      this.state = state;
    }

    @Override
    public ObjectWriter add(ColumnMetadata colSchema) {
      return state.addColumn(colSchema).writer();
    }
  }

  private static class UnionShim implements ParentShim {
    private final VariantWriter writer;

    public UnionShim(VariantWriter writer) {
      this.writer = writer;
    }

    @Override
    public ObjectWriter add(ColumnMetadata colSchema) {
      return writer.addMember(colSchema);
    }
  }

  private static class RepeatedListShim implements ParentShim {
    private final RepeatedListWriter writer;

    public RepeatedListShim(RepeatedListWriter writer) {
      this.writer = writer;
    }

    @Override
    public ObjectWriter add(ColumnMetadata colSchema) {
      return writer.defineElement(colSchema);
    }
  }

  private static BuildFromSchema instance = new BuildFromSchema();

  private BuildFromSchema() { }

  public static BuildFromSchema instance() { return instance; }

  /**
   * When creating a schema up front, provide the schema of the desired tuple,
   * then build vectors and writers to match. Allows up-front schema definition
   * in addition to on-the-fly schema creation handled elsewhere.
   *
   * @param schema desired tuple schema to be materialized
   */

  public void buildTuple(TupleWriter writer, TupleMetadata schema) {
    final ParentShim tupleShim = new TupleShim(writer);
    for (int i = 0; i < schema.size(); i++) {
      final ColumnMetadata colSchema = schema.metadata(i);
      buildColumn(tupleShim, colSchema);
    }
  }

  /**
   * Build a column recursively. Called internally when adding a column
   * via the addColumn() method on the tuple writer.
   *
   * @param state the loader state for the tuple, a row or a map
   * @param colSchema the schema of the column to add
   * @return the object writer for the added column
   */

  public ObjectWriter buildColumn(TupleState state, ColumnMetadata colSchema) {
    return buildColumn(new TupleStateShim(state), colSchema);
  }

  /**
   * Build the column writer, and any nested content, returning the built
   * column writer as a generic object writer.
   *
   * @param parent the shim that implements the logic to add a column
   * to a tuple, list, repeated list, or union.
   * @param colSchema the schema of the column to add
   * @return the object writer for the added column
   */

  private ObjectWriter buildColumn(ParentShim parent, ColumnMetadata colSchema) {
    if (colSchema.isMultiList()) {
      return buildRepeatedList(parent, colSchema);
    } else if (colSchema.isMap()) {
      return buildMap(parent, colSchema);
    } else if (isSingleList(colSchema)) {
      return buildSingleList(parent, colSchema);
    } else if (colSchema.isVariant()) {
      return buildVariant(parent, colSchema);
    } else if (colSchema.isDict()) {
      return buildDict(parent, colSchema);
    } else {
      return buildPrimitive(parent, colSchema);
    }
  }

  /**
   * Determine if the schema represents a column with a LIST type that
   * includes elements all of a single type. (Lists can be of a single
   * type (with nullable elements) or can be of unions.)
   *
   * @param colSchema schema for the column
   * @return true if the column is of type LIST with a single
   * element type
   */

  private boolean isSingleList(ColumnMetadata colSchema) {
    return colSchema.isVariant() && colSchema.isArray() && colSchema.variantSchema().isSingleType();
  }

  private ObjectWriter buildPrimitive(ParentShim parent, ColumnMetadata colSchema) {
    return parent.add(colSchema);
  }

  private ObjectWriter buildMap(ParentShim parent, ColumnMetadata colSchema) {
    final ObjectWriter colWriter = parent.add(colSchema.cloneEmpty());
    expandMap(colWriter, colSchema);
    return colWriter;
  }

  private void expandMap(ObjectWriter colWriter, ColumnMetadata colSchema) {
    if (colSchema.isArray()) {
      buildTuple(colWriter.array().tuple(), colSchema.tupleSchema());
    } else {
      buildTuple(colWriter.tuple(), colSchema.tupleSchema());
    }
  }

  /**
   * Expand a variant column. We use the term "variant" to mean either
   * a UNION, or a LIST of UNIONs. (A LIST of UNIONs is, conceptually,
   * a repeated UNION, though it is far more complex.)
   *
   * @param parent shim object used to associate the UNION types with its
   * parent column (which is a UNION or a LIST). Since UNION and LIST are
   * far different types, the shim provides a facade that encapsulates
   * the common behavior
   * @param colSchema the schema of the variant (LIST or UNION) column
   */

  private ObjectWriter buildVariant(ParentShim parent, ColumnMetadata colSchema) {
    final ObjectWriter colWriter = parent.add(colSchema.cloneEmpty());
    expandVariant(colWriter, colSchema);
    return colWriter;
  }

  private void expandVariant(ObjectWriter colWriter, ColumnMetadata colSchema) {
    if (colSchema.isArray()) {
      buildUnion(colWriter.array().variant(), colSchema.variantSchema());
    } else {
      buildUnion(colWriter.variant(), colSchema.variantSchema());
    }
  }

  public void buildUnion(VariantWriter writer, VariantMetadata schema) {
    final UnionShim unionShim = new UnionShim(writer);
    for (final ColumnMetadata member : schema.members()) {
      buildColumn(unionShim, member);
    }
  }

  private ObjectWriter buildSingleList(ParentShim parent, ColumnMetadata colSchema) {
    final ColumnMetadata seed = colSchema.cloneEmpty();
    final ColumnMetadata subtype = colSchema.variantSchema().listSubtype();
    seed.variantSchema().addType(subtype.cloneEmpty());
    seed.variantSchema().becomeSimple();
    final ObjectWriter listWriter = parent.add(seed);
    expandColumn(listWriter, subtype);
    return listWriter;
  }

  /**
   * Expand a repeated list. The list may be multi-dimensional, meaning that
   * it may have may layers of other repeated lists before we get to the element
   * (inner-most) array.
   *
   * @param parent tuple writer for the tuple that holds the array
   * @param colSchema schema definition of the array
   */

  private ObjectWriter buildRepeatedList(ParentShim parent, ColumnMetadata colSchema) {
    final ObjectWriter objWriter = parent.add(colSchema.cloneEmpty());
    final RepeatedListWriter listWriter = (RepeatedListWriter) objWriter.array();
    final ColumnMetadata elements = colSchema.childSchema();
    if (elements != null) {
      final RepeatedListShim listShim = new RepeatedListShim(listWriter);
      buildColumn(listShim, elements);
    }
    return objWriter;
  }

  /**
   * We've just built a writer for column. If the column is structured
   * (AKA "complex", meaning a map, list, array or dict), then we need to
   * build writer for the components of the column. We do that recursively
   * here.
   *
   * @param colWriter the writer for the (possibly structured) column
   * @param colSchema the schema definition for the column
   */

  private void expandColumn(ObjectWriter colWriter, ColumnMetadata colSchema) {

    if (colSchema.isMultiList()) {
      // For completeness, should never occur.
      assert false;
    } else if (colSchema.isMap()) {
      expandMap(colWriter, colSchema);
    } else if (isSingleList(colSchema)) {
      // For completeness, should never occur.
      assert false;
    } else if (colSchema.isVariant()) {
      expandVariant(colWriter, colSchema);
    } else if (colSchema.isDict()) {
      expandDict(colWriter, colSchema);
    // } else {
      // Nothing to expand for primitives
    }
  }

  private ObjectWriter buildDict(ParentShim parent, ColumnMetadata colSchema) {
    final ObjectWriter colWriter = parent.add(colSchema.cloneEmpty());
    expandDict(colWriter, colSchema);
    return colWriter;
  }

  private void expandDict(ObjectWriter colWriter, ColumnMetadata colSchema) {
    if (colSchema.isArray()) {
      buildTuple(colWriter.array().dict().tuple(), colSchema.tupleSchema());
    } else {
      buildTuple(colWriter.dict().tuple(), colSchema.tupleSchema());
    }
  }
}
