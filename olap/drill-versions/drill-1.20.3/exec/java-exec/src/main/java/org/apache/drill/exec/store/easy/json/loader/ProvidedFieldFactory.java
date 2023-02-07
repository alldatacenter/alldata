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
package org.apache.drill.exec.store.easy.json.loader;

import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.MetadataUtils;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.store.easy.json.parser.ElementParser;
import org.apache.drill.exec.store.easy.json.parser.FieldParserFactory;
import org.apache.drill.exec.store.easy.json.parser.ValueParser;
import org.apache.drill.exec.store.easy.json.values.VarCharListener;
import org.apache.drill.exec.vector.accessor.ObjectWriter;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

/**
 * Create a Drill field listener based on a provided schema. The schema
 * takes precedence over the JSON syntax: the schema is expected to
 * accurately describe what will occur for this field in the JSON
 * input.
 */
public class ProvidedFieldFactory extends BaseFieldFactory {

  public ProvidedFieldFactory(JsonLoaderImpl loader, FieldFactory child) {
    super(loader, child);
  }

  /**
   * Build a column and its listener based a provided schema.
   * The user is responsible to ensure that the provided schema
   * accurately reflects the structure of the JSON being parsed.
   */
  @Override
  public ElementParser fieldParser(FieldDefn fieldDefn) {
    if (fieldDefn.providedColumn() == null) {
      return child.fieldParser(fieldDefn);
    } else {
      return parserFor(fieldDefn);
    }
  }

  public ElementParser parserFor(FieldDefn fieldDefn) {
    ColumnMetadata providedCol = fieldDefn.providedColumn();
    switch (providedCol.structureType()) {
      case PRIMITIVE:
        return primitiveParserFor(fieldDefn);
      case TUPLE:
        return objectParserForSchema(fieldDefn);
      case VARIANT:
        return variantParserForSchema(fieldDefn);
      case MULTI_ARRAY:
        return multiDimArrayParserForSchema(fieldDefn);
      default:
        throw loader().unsupportedType(providedCol);
    }
  }

  private ElementParser primitiveParserFor(FieldDefn fieldDefn) {
    ColumnMetadata providedCol = fieldDefn.providedColumn();
    if (providedCol.type() == MinorType.VARCHAR) {
      return stringParserFor(fieldDefn);
    } else {
      return basicParserFor(fieldDefn);
    }
  }

  private ElementParser basicParserFor(FieldDefn fieldDefn) {

    // Delegate parser creation downward: will be done by the extended
    // types factory, if present, else by inferred field factory.
    ColumnMetadata colSchema = fieldDefn.providedColumn().copy();
    ValueParser scalarParser = child.scalarParserFor(fieldDefn, colSchema);
    if (colSchema.isArray()) {
      return scalarArrayParserFor(scalarParser);
    } else {
      return scalarParser;
    }
  }

  private ElementParser stringParserFor(FieldDefn fieldDefn) {
    String mode = fieldDefn.providedColumn().property(JsonLoader.JSON_MODE);
    if (mode == null) {
      return basicParserFor(fieldDefn);
    }
    FieldParserFactory parserFactory = parserFactory();
    switch (mode) {
      case JsonLoader.JSON_TEXT_MODE:
        return parserFactory.textValueParser(varCharListenerFor(fieldDefn));
      case JsonLoader.JSON_LITERAL_MODE:
        return parserFactory.jsonTextParser(varCharListenerFor(fieldDefn));
      default:
        return basicParserFor(fieldDefn);
    }
  }

  private VarCharListener varCharListenerFor(FieldDefn fieldDefn) {
    return new VarCharListener(loader,
        fieldDefn.scalarWriterFor(fieldDefn.providedColumn().copy()));
  }

  private ElementParser objectParserForSchema(FieldDefn fieldDefn) {
    ColumnMetadata providedCol = fieldDefn.providedColumn();

    // Propagate the provided map schema into the object
    // listener as a provided tuple schema.
    ColumnMetadata colSchema = providedCol.cloneEmpty();
    TupleMetadata providedSchema = providedCol.tupleSchema();
    if (providedCol.isArray()) {
      return objectArrayParserFor(fieldDefn, colSchema, providedSchema);
    } else {
      return objectParserFor(fieldDefn, colSchema, providedSchema);
    }
  }

  /**
   * Create a repeated list column and its multiple levels of inner structure
   * from a provided schema. Repeated lists can nest to any number of levels to
   * provide any number of dimensions. In general, if an array is <i>n</i>-dimensional,
   * then there are <i>n</i>-1 repeated lists with some array type as the
   * innermost dimension.
   */
  private ElementParser multiDimArrayParserForSchema(FieldDefn fieldDefn) {
    // Parse the stack of repeated lists to count the "outer" dimensions and
    // to locate the innermost array (the "list" which is "repeated").
    int dims = 1; // For inner array
    ColumnMetadata elementSchema = fieldDefn.providedColumn();
    while (MetadataUtils.isRepeatedList(elementSchema)) {
      dims++;
      elementSchema = elementSchema.childSchema();
      Preconditions.checkArgument(elementSchema != null);
    }

    ColumnMetadata colSchema = repeatedListSchemaFor(fieldDefn.key(), dims,
        elementSchema.cloneEmpty());
    ObjectWriter fieldWriter = fieldDefn.fieldWriterFor(colSchema);
    switch (elementSchema.structureType()) {
      case PRIMITIVE:
        return multiDimScalarArrayFor(fieldWriter, dims);
      case TUPLE:
        return multiDimObjectArrayFor(fieldWriter,
            dims, elementSchema.tupleSchema());
      case VARIANT:
        return multiDimVariantArrayParserFor(fieldWriter, dims);
      default:
        throw loader().unsupportedType(fieldDefn.providedColumn());
    }
  }

  private ElementParser variantParserForSchema(FieldDefn fieldDefn) {
    // A variant can contain multiple types. The schema does not
    // declare the types; rather they are discovered by the reader.
    // That is, there is no VARIANT<INT, DOUBLE>, there is just VARIANT.
    ColumnMetadata colSchema = fieldDefn.providedColumn().cloneEmpty();
    ObjectWriter fieldWriter = fieldDefn.fieldWriterFor(colSchema);
    if (colSchema.isArray()) {
      return variantArrayParserFor(fieldWriter.array());
    } else {
      return variantParserFor(fieldWriter.variant());
    }
  }
}
