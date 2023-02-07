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

import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.MetadataUtils;
import org.apache.drill.exec.store.easy.json.parser.ElementParser;
import org.apache.drill.exec.store.easy.json.parser.ValueDef;
import org.apache.drill.exec.store.easy.json.parser.ValueDef.JsonType;
import org.apache.drill.exec.store.easy.json.values.VarCharListener;
import org.apache.drill.exec.store.easy.json.parser.ValueParser;
import org.apache.drill.exec.vector.accessor.ScalarWriter;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Create Drill field listeners based on the observed look-ahead
 * tokens in JSON.
 */
public class InferredFieldFactory extends BaseFieldFactory {
  protected static final Logger logger = LoggerFactory.getLogger(InferredFieldFactory.class);

  public InferredFieldFactory(JsonLoaderImpl loader) {
    super(loader);
  }

  /**
   * Build a column and its listener based on a look-ahead hint.
   */
  @Override
  public ElementParser fieldParser(FieldDefn fieldDefn) {
    ValueDef valueDef = fieldDefn.lookahead();
    if (valueDef.type().isUnknown()) {
      return parserForUnknown(fieldDefn);
    } else {
      return resolveField(fieldDefn);
    }
  }

  /**
   * Create a listener when we don't have type information. For the case
   * {@code null} appears before other values.
   */
  private ElementParser parserForUnknown(FieldDefn fieldDefn) {
    ValueDef valueDef = fieldDefn.lookahead();
    if (!valueDef.isArray()) {

      // For the case null appears before other values.
      return new NullFieldParser(fieldDefn.tupleParser(), fieldDefn.key());
    } else if (valueDef.dimensions() > 1) {

      // An unknown nested array: [[]], etc. Must guess a type.
      return forceRepeatedListResolution(fieldDefn);
    } else if (valueDef.type() == JsonType.NULL) {

      // For the case of [null], must force resolution
      return forceArrayResolution(fieldDefn);
    } else {

      // For the case [] appears before other values.
      return new EmptyArrayFieldParser(fieldDefn.tupleParser(), fieldDefn.key());
    }
  }

  private ElementParser forceRepeatedListResolution(FieldDefn fieldDefn) {
    ColumnMetadata innerSchema = schemaForUnknown(fieldDefn, true);
    int dims = fieldDefn.lookahead().dimensions();
    ColumnMetadata fieldSchema = repeatedListSchemaFor(fieldDefn.key(), dims, innerSchema);
    return buildOuterArrays(
        fieldDefn.fieldWriterFor(fieldSchema), dims,
        innerWriter -> scalarArrayParserFor(
            unknownParserFor(innerWriter.array().scalar())));
  }

  @Override
  public ElementParser forceNullResolution(FieldDefn fieldDefn) {
    logger.warn("Ambiguous type! JSON field {}" +
        " contains all nulls. Assuming JSON text.", fieldDefn.key());
    return forceResolution(fieldDefn, false);
  }

  @Override
  public ElementParser forceArrayResolution(FieldDefn fieldDefn) {
    logger.warn("Ambiguous type! JSON field {}" +
        " contains all empty arrays. Assuming array of JSON text.", fieldDefn.key());
    return scalarArrayParserFor(forceResolution(fieldDefn, true));
  }

  private ValueParser forceResolution(FieldDefn fieldDefn, boolean isArray) {
    return unknownParserFor(
        fieldDefn.scalarWriterFor(
            schemaForUnknown(fieldDefn, isArray)));
  }

  private ColumnMetadata schemaForUnknown(FieldDefn fieldDefn, boolean isArray) {
    if (loader.options().unknownsAsJson) {
      return fieldDefn.schemaFor(MinorType.VARCHAR, isArray);
    } else {
      return fieldDefn.schemaFor(loader.options().nullType, isArray);
    }
  }

  private ValueParser unknownParserFor(ScalarWriter writer) {
    if (loader.options().unknownsAsJson) {
      return parserFactory().jsonTextParser(new VarCharListener(loader, writer));
    } else {
      return parserFactory().simpleValueParser(scalarListenerFor(writer));
    }
  }

  private ElementParser resolveField(FieldDefn fieldDefn) {
    ValueDef valueDef = fieldDefn.lookahead();
    Preconditions.checkArgument(!valueDef.type().isUnknown());
    if (!valueDef.isArray()) {
      if (valueDef.type().isObject()) {
        return objectParserFor(fieldDefn);
      } else {
        return scalarParserFor(fieldDefn, false);
      }
    } else if (valueDef.dimensions() == 1) {
      if (valueDef.type().isObject()) {
        return objectArrayParserFor(fieldDefn);
      } else {
        return scalarArrayParserFor(scalarParserFor(fieldDefn, true));
      }
    } else { // 2+ dimensions
      if (valueDef.type().isObject()) {
        return multiDimObjectArrayParserFor(fieldDefn);
      } else {
        return multiDimScalarArrayParserFor(fieldDefn);
      }
    }
  }

  public ValueParser scalarParserFor(FieldDefn fieldDefn, boolean isArray) {
    if (loader.options().allTextMode) {
      return parserFactory().textValueParser(
          new VarCharListener(loader,
              fieldDefn.scalarWriterFor(MinorType.VARCHAR, isArray)));
    } else {
      return scalarParserFor(fieldDefn,
              fieldDefn.schemaFor(scalarTypeFor(fieldDefn), isArray));
    }
  }

  /**
   * Create a multi- (2+) dimensional scalar array from a JSON value description.
   */
  private ElementParser multiDimScalarArrayParserFor(FieldDefn fieldDefn) {
    ColumnMetadata innerSchema = fieldDefn.schemaFor(scalarTypeFor(fieldDefn), true);
    int dims = fieldDefn.lookahead().dimensions();
    ColumnMetadata fieldSchema = repeatedListSchemaFor(fieldDefn.key(), dims, innerSchema);
    return multiDimScalarArrayFor(
        fieldDefn.fieldWriterFor(fieldSchema), dims);
  }

  /**
   * Create a map array column and its associated object array listener
   * for the given key.
   */
  public ElementParser objectArrayParserFor(FieldDefn fieldDefn) {
    return objectArrayParserFor(fieldDefn, MetadataUtils.newMapArray(fieldDefn.key()), null);
  }

  /**
   * Create a RepeatedList which contains (empty) Map objects using the provided
   * schema. That is, create a multi-dimensional array of maps.
   * The map fields are created on the fly, optionally using the provided schema.
   */
  private ElementParser multiDimObjectArrayParserFor(FieldDefn fieldDefn) {
    ColumnMetadata innerSchema =  MetadataUtils.newMapArray(fieldDefn.key());
    int dims = fieldDefn.lookahead().dimensions();
    ColumnMetadata fieldSchema = repeatedListSchemaFor(fieldDefn.key(), dims, innerSchema);
    return multiDimObjectArrayFor(fieldDefn.fieldWriterFor(fieldSchema), dims, null);
  }

  /**
   * Create a RepeatedList which contains Unions. (Actually, this is an
   * array of List objects internally.) The variant is variable, it makes no
   * sense to specify a schema for the variant. Also, omitting the schema
   * save a large amount of complexity that will likely never be needed.
   */
  @SuppressWarnings("unused")
  private ElementParser repeatedListOfVariantListenerFor(FieldDefn fieldDefn) {
    ColumnMetadata innerSchema =  MetadataUtils.newVariant(fieldDefn.key(), DataMode.REPEATED);
    int dims = fieldDefn.lookahead().dimensions();
    ColumnMetadata fieldSchema = repeatedListSchemaFor(fieldDefn.key(), dims, innerSchema);
    return multiDimVariantArrayParserFor(fieldDefn.fieldWriterFor(fieldSchema), dims);
  }

  /**
   * Convert the JSON type, obtained by looking ahead one token, to a Drill
   * scalar type. Report an error if the JSON type does not map to a Drill
   * type (which can occur in a context where we expect a scalar, but got
   * an object or array.)
   */
  private MinorType scalarTypeFor(FieldDefn fieldDefn) {
    MinorType colType = drillTypeFor(fieldDefn.lookahead().type());
    if (colType == null) {
      throw loader().unsupportedJsonTypeException(
          fieldDefn.key(), fieldDefn.lookahead().type());
    }
    return colType;
  }

  public MinorType drillTypeFor(JsonType type) {
    if (loader().options().allTextMode) {
      return MinorType.VARCHAR;
    }
    switch (type) {
    case BOOLEAN:
      return MinorType.BIT;
    case FLOAT:
      return MinorType.FLOAT8;
    case INTEGER:
      if (loader().options().readNumbersAsDouble) {
        return MinorType.FLOAT8;
      } else {
        return MinorType.BIGINT;
      }
    case STRING:
      return MinorType.VARCHAR;
    default:
      throw new IllegalStateException(type.name());
    }
  }
}
