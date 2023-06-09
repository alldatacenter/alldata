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

import java.util.function.Function;

import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.MetadataUtils;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.store.easy.json.loader.SimpleArrayListener.ListArrayListener;
import org.apache.drill.exec.store.easy.json.loader.SimpleArrayListener.StructureArrayListener;
import org.apache.drill.exec.store.easy.json.parser.ElementParser;
import org.apache.drill.exec.store.easy.json.parser.FieldParserFactory;
import org.apache.drill.exec.store.easy.json.parser.ValueParser;
import org.apache.drill.exec.store.easy.json.values.BigIntListener;
import org.apache.drill.exec.store.easy.json.values.BinaryValueListener;
import org.apache.drill.exec.store.easy.json.values.BooleanListener;
import org.apache.drill.exec.store.easy.json.values.DateValueListener;
import org.apache.drill.exec.store.easy.json.values.DecimalValueListener;
import org.apache.drill.exec.store.easy.json.values.DoubleListener;
import org.apache.drill.exec.store.easy.json.values.IntervalValueListener;
import org.apache.drill.exec.store.easy.json.values.ScalarListener;
import org.apache.drill.exec.store.easy.json.values.StrictIntValueListener;
import org.apache.drill.exec.store.easy.json.values.TimeValueListener;
import org.apache.drill.exec.store.easy.json.values.TimestampValueListener;
import org.apache.drill.exec.store.easy.json.values.VarCharListener;
import org.apache.drill.exec.vector.accessor.ArrayWriter;
import org.apache.drill.exec.vector.accessor.ObjectWriter;
import org.apache.drill.exec.vector.accessor.ScalarWriter;
import org.apache.drill.exec.vector.accessor.TupleWriter;
import org.apache.drill.exec.vector.accessor.VariantWriter;

/**
 * Base field factor class which handles the common tasks for
 * building column writers and JSON listeners.
 */
public abstract class BaseFieldFactory implements FieldFactory {

  protected final JsonLoaderImpl loader;
  protected final FieldFactory child;

  public BaseFieldFactory(JsonLoaderImpl loader) {
    this(loader, null);
  }

  public BaseFieldFactory(JsonLoaderImpl loader, FieldFactory child) {
    this.loader = loader;
    this.child = child;
  }

  protected FieldParserFactory parserFactory() {
    return loader().parser().fieldFactory();
  }

  @Override
  public ElementParser ignoredFieldParser() {
    return parserFactory().ignoredFieldParser();
  }

  protected JsonLoaderImpl loader() { return loader; }

  @Override
  public ValueParser scalarParserFor(FieldDefn fieldDefn, ColumnMetadata colSchema) {
    return scalarParserFor(fieldDefn.scalarWriterFor(colSchema));
  }

  public ValueParser scalarParserFor(ScalarWriter writer) {
    return parserFactory().simpleValueParser(scalarListenerFor(writer));
  }

  protected ElementParser scalarArrayParserFor(ValueParser element) {
    return parserFactory().scalarArrayValueParser(
        new SimpleArrayListener(), element);
  }

  protected ElementParser scalarArrayParserFor(ArrayWriter writer) {
    return scalarArrayParserFor(scalarParserFor(writer.scalar()));
  }

  /**
   * Create a repeated list listener for a scalar value.
   */
  protected ElementParser multiDimScalarArrayFor(ObjectWriter writer, int dims) {
    return buildOuterArrays(writer, dims,
        innerWriter -> scalarArrayParserFor(innerWriter.array()));
  }

  /**
   * Create a map column and its associated object value listener for the
   * a JSON object value given the value's key.
   */
  public ElementParser objectParserFor(FieldDefn fieldDefn) {
    return objectParserFor(fieldDefn, MetadataUtils.newMap(fieldDefn.key()), null);
  }

  /**
   * Create a map column and its associated object value listener for the
   * given key and optional provided schema.
   */
  protected ElementParser objectParserFor(FieldDefn fieldDefn,
      ColumnMetadata colSchema, TupleMetadata providedSchema) {
    return objectParserFor(
            fieldDefn.fieldWriterFor(colSchema).tuple(),
            providedSchema);
  }

  /**
   * Create a map array column and its associated parsers and listeners
   * for the given column schema and optional provided schema.
   */
  protected ElementParser objectArrayParserFor(
      FieldDefn fieldDefn, ColumnMetadata colSchema, TupleMetadata providedSchema) {
    return objectArrayParserFor(fieldDefn.fieldWriterFor(colSchema).array(), providedSchema);
  }

  protected ElementParser objectArrayParserFor(ArrayWriter arrayWriter, TupleMetadata providedSchema) {
    return parserFactory().arrayValueParser(
        new StructureArrayListener(arrayWriter),
        objectParserFor(arrayWriter.tuple(), providedSchema));
  }

  protected ElementParser objectParserFor(TupleWriter writer, TupleMetadata providedSchema) {
    return parserFactory().objectValueParser(
        new TupleParser(loader, writer, providedSchema));
  }

  /**
   * Create a repeated list listener for a Map.
   */
  public ElementParser multiDimObjectArrayFor(
      ObjectWriter writer, int dims, TupleMetadata providedSchema) {
    return buildOuterArrays(writer, dims,
        innerWriter ->
          objectArrayParserFor(innerWriter.array(), providedSchema));
  }

  /**
   * Create a variant (UNION) column and its associated parser given
   * a column schema.
   */
  protected ElementParser variantParserFor(VariantWriter writer) {
    return new VariantParser(loader, writer);
  }

  /**
   * Create a variant array (LIST) column and its associated parser given
   * a column schema.
   */
  protected ElementParser variantArrayParserFor(ArrayWriter arrayWriter) {
    return parserFactory().arrayValueParser(
        new ListArrayListener(arrayWriter),
        variantParserFor(arrayWriter.variant()));
  }

  /**
   * Create a repeated list listener for a variant. Here, the inner
   * array is provided by a List (which is a repeated Union.)
   */
  protected ElementParser multiDimVariantArrayParserFor(
      ObjectWriter writer, int dims) {
    return buildOuterArrays(writer, dims,
        innerWriter -> variantArrayParserFor(innerWriter.array()));
  }

  /**
   * Create layers of repeated list listeners around the type-specific
   * array. If the JSON has three array levels, the outer two are repeated
   * lists, the inner is type-specific: say an array of {@code BIGINT} or
   * a map array.
   */
  public ElementParser buildOuterArrays(ObjectWriter writer, int dims,
      Function<ObjectWriter, ElementParser> innerCreator) {
    ObjectWriter writers[] = new ObjectWriter[dims];
    writers[0] = writer;
    for (int i = 1; i < dims; i++) {
      writers[i] = writers[i-1].array().entry();
    }
    ElementParser prevElementParser = innerCreator.apply(writers[dims - 1]);
    for (int i = dims - 2; i >= 0; i--) {
      prevElementParser = parserFactory().arrayValueParser(
          new StructureArrayListener(writers[i].array()), prevElementParser);
    }
    return prevElementParser;
  }

  /**
   * Build up a repeated list column definition given a specification of the
   * number of dimensions and the JSON type. Creation of the element type is
   * via a closure that builds the needed schema.
   */
  protected ColumnMetadata repeatedListSchemaFor(String key, int dims,
      ColumnMetadata innerArray) {
    ColumnMetadata prev = innerArray;
    for (int i = 1; i < dims; i++) {
      prev = MetadataUtils.newRepeatedList(key, prev);
    }
    return prev;
  }

  public ScalarListener scalarListenerFor(ScalarWriter writer) {
    switch (writer.schema().type()) {
      case BIGINT:
        return new BigIntListener(loader, writer);
      case BIT:
        return new BooleanListener(loader, writer);
      case FLOAT4:
      case FLOAT8:
        return new DoubleListener(loader, writer);
      case VARCHAR:
        return new VarCharListener(loader, writer);
      case INT:
      case SMALLINT:
        return new StrictIntValueListener(loader, writer);
      case INTERVAL:
      case INTERVALDAY:
      case INTERVALYEAR:
        return new IntervalValueListener(loader, writer);
      case DATE:
        return new DateValueListener(loader, writer);
      case TIME:
        return new TimeValueListener(loader, writer);
      case TIMESTAMP:
        return new TimestampValueListener(loader, writer);
      case VARBINARY:
        return new BinaryValueListener(loader, writer);
      case VARDECIMAL:
        return new DecimalValueListener(loader, writer);
      default:
        throw loader.buildError(
            UserException.internalError(null)
              .message("Unsupported JSON reader type: %s",
                  writer.schema().type().name()));
    }
  }

  @Override
  public ElementParser forceNullResolution(FieldDefn fieldDefn) {
    return child.forceArrayResolution(fieldDefn);
  }

  @Override
  public ElementParser forceArrayResolution(FieldDefn fieldDefn) {
    return child.forceArrayResolution(fieldDefn);
  }
}
