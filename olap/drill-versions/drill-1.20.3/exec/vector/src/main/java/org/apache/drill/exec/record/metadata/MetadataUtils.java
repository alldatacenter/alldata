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
package org.apache.drill.exec.record.metadata;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.vector.complex.DictVector;

/**
 * A collection of utility methods for working with column and tuple metadata.
 */
public class MetadataUtils {

  public static TupleSchema fromFields(Iterable<MaterializedField> fields) {
    TupleSchema tuple = new TupleSchema();
    for (MaterializedField field : fields) {
      tuple.add(field);
    }
    return tuple;
  }

  /**
   * Create a column metadata object that holds the given
   * {@link MaterializedField}. The type of the object will be either a
   * primitive, map or dict column, depending on the field's type. The logic
   * here mimics the code as written, which is very messy in some places.
   *
   * @param field the materialized field to wrap
   * @return the column metadata that wraps the field
   */
  public static ColumnMetadata fromField(MaterializedField field) {
    MajorType majorType = field.getType();
    MinorType type = majorType.getMinorType();
    switch (type) {
    case DICT:
      return MetadataUtils.newDict(field);
    case MAP:
      return MetadataUtils.newMap(field);
    case UNION:
      if (field.getType().getMode() != DataMode.OPTIONAL) {
        throw new UnsupportedOperationException(type.name() + " type must be nullable");
      }
      return new VariantColumnMetadata(field);
    case VARDECIMAL:
      int precision = majorType.hasPrecision() ? majorType.getPrecision() : Types.maxPrecision(type);
      int scale = majorType.hasScale() ? majorType.getScale() : 0;
      return MetadataUtils.newDecimal(field.getName(), type, majorType.getMode(), precision, scale);
    case LIST:
      switch (field.getType().getMode()) {
      case OPTIONAL:
        return new VariantColumnMetadata(field);
      case REPEATED:

        // Not a list at all, but rather the second (or third...)
        // dimension on a repeated type.

        return new RepeatedListColumnMetadata(field);
      default:

        // List of unions (or a degenerate union of a single type.)
        // Not supported in Drill.

        throw new UnsupportedOperationException(
            String.format("Unsupported mode %s for type %s",
                field.getType().getMode().name(),
                type.name()));
      }
    default:
      return new PrimitiveColumnMetadata(field);
    }
  }

  public static ColumnMetadata fromView(MaterializedField field) {
    if (field.getType().getMinorType() == MinorType.MAP) {
      return new MapColumnMetadata(field, null);
    } else if (field.getType().getMinorType() == MinorType.DICT) {
      return newDict(field);
    } else {
      return new PrimitiveColumnMetadata(field);
    }
  }

  /**
   * Create a tuple given the list of columns that make up the tuple.
   * Creates nested maps as needed.
   *
   * @param columns list of columns that make up the tuple
   * @return a tuple metadata object that contains the columns
   */
  public static TupleSchema fromColumns(List<ColumnMetadata> columns) {
    TupleSchema tuple = new TupleSchema();
    for (ColumnMetadata column : columns) {
      tuple.add(column);
    }
    return tuple;
  }

  /**
   * Create a column metadata object for a map column, given the
   * {@link MaterializedField} that describes the column, and a list
   * of column metadata objects that describe the columns in the map.
   *
   * @param field the materialized field that describes the map column
   * @param schema metadata that describes the tuple of columns in
   * the map
   * @return a map column metadata for the map
   */
  public static MapColumnMetadata newMap(MaterializedField field, TupleSchema schema) {
    return new MapColumnMetadata(field, schema);
  }

  public static MapColumnMetadata newMap(MaterializedField field) {
    return new MapColumnMetadata(field, fromFields(field.getChildren()));
  }

  public static MapColumnMetadata newMap(String name, TupleMetadata schema) {
    return newMap(name, DataMode.REQUIRED, schema);
  }

  public static MapColumnMetadata newMap(String name) {
    return newMap(name, new TupleSchema());
  }

  public static DictColumnMetadata newDict(MaterializedField field) {
    return new DictColumnMetadata(field, fromFields(field.getChildren()));
  }

  public static DictColumnMetadata newDict(MaterializedField field, TupleSchema schema) {
    validateDictChildren(schema.toFieldList());
    return new DictColumnMetadata(field.getName(), field.getDataMode(), schema);
  }

  private static void validateDictChildren(List<MaterializedField> entryFields) {
    Collection<String> children = entryFields.stream()
        .map(MaterializedField::getName)
        .collect(Collectors.toList());
    String message = "DICT does not contain %s.";
    if (!children.contains(DictVector.FIELD_KEY_NAME)) {
      throw new IllegalStateException(String.format(message, DictVector.FIELD_KEY_NAME));
    } else if (!children.contains(DictVector.FIELD_VALUE_NAME)) {
      throw new IllegalStateException(String.format(message, DictVector.FIELD_VALUE_NAME));
    }
  }

  public static DictColumnMetadata newDict(String name) {
    return new DictColumnMetadata(name, DataMode.REQUIRED);
  }

  public static VariantColumnMetadata newVariant(MaterializedField field, VariantSchema schema) {
    return VariantColumnMetadata.unionOf(field, schema);
  }

  public static VariantColumnMetadata newVariant(String name, DataMode cardinality) {
    switch (cardinality) {
    case OPTIONAL:
      return VariantColumnMetadata.union(name);
    case REPEATED:
      return VariantColumnMetadata.list(name);
    default:
      throw new IllegalArgumentException();
    }
  }

  public static RepeatedListColumnMetadata newRepeatedList(String name, ColumnMetadata child) {
    return new RepeatedListColumnMetadata(name, child);
  }

  public static ColumnMetadata newMapArray(String name, TupleMetadata schema) {
    return newMap(name, DataMode.REPEATED, schema);
  }

  public static MapColumnMetadata newMap(String name, DataMode dataMode, TupleMetadata schema) {
    return new MapColumnMetadata(name, dataMode, (TupleSchema) schema);
  }

  public static ColumnMetadata newMapArray(String name) {
    return newMapArray(name, new TupleSchema());
  }

  public static DictColumnMetadata newDictArray(String name) {
    return new DictColumnMetadata(name, DataMode.REPEATED);
  }

  public static PrimitiveColumnMetadata newScalar(String name, MinorType type,
      DataMode mode) {
    assert isScalar(type);
    return new PrimitiveColumnMetadata(name, type, mode);
  }

  public static PrimitiveColumnMetadata newScalar(String name, MajorType type) {
    MinorType minorType = type.getMinorType();
    assert isScalar(minorType);
    return new PrimitiveColumnMetadata(name, type);
  }

  public static ColumnMetadata newDecimal(String name, DataMode mode,
      int precision, int scale) {
    return newDecimal(name, MinorType.VARDECIMAL, mode, precision, scale);
  }

  public static ColumnMetadata newDecimal(String name, MinorType type, DataMode mode,
      int precision, int scale) {
    if (precision < 0 ) {
      throw new IllegalArgumentException("Precision cannot be negative : " +
          precision);
    }
    if (scale < 0 ) {
      throw new IllegalArgumentException("Scale cannot be negative : " +
          scale);
    }
    int maxPrecision = Types.maxPrecision(type);
    if (precision > maxPrecision) {
      throw new IllegalArgumentException(String.format(
          "%s(%d, %d) exceeds maximum suppored precision of %d",
        type, precision, scale, maxPrecision));
    }
    if (scale > precision) {
      throw new IllegalArgumentException(String.format(
          "%s(%d, %d) scale exceeds precision",
        type, precision, scale));
    }
    MaterializedField field = new ColumnBuilder(name, type)
        .setMode(mode)
        .setPrecisionAndScale(precision, scale)
        .build();
    return new PrimitiveColumnMetadata(field);
  }

  public static boolean isScalar(ColumnMetadata col) {
    return isScalar(col.type());
  }

  public static boolean isScalar(MinorType type) {
    return !isComplex(type);
  }

  public static boolean isComplex(MinorType type) {
    switch (type) {
      case MAP:
      case UNION:
      case LIST:
      case DICT:
        return true;
      default:
        return false;
    }
  }

  public static ColumnMetadata newDynamic(String name) {
    return new DynamicColumn(name);
  }

  public static ColumnMetadata wildcard() {
    return DynamicColumn.WILDCARD_COLUMN;
  }

  public static boolean isWildcard(ColumnMetadata col) {
    return col.isDynamic() &&
           col.name().equals(DynamicColumn.WILDCARD);
  }

  public static ColumnMetadata cloneMapWithSchema(ColumnMetadata source,
      TupleMetadata members) {
    return newMap(source.name(), source.mode(), members);
  }

  public static ColumnMetadata diffMap(ColumnMetadata map, ColumnMetadata other) {
    TupleMetadata diff = diffTuple(map.tupleSchema(), other.tupleSchema());
    if (!diff.isEmpty()) {
      return MetadataUtils.cloneMapWithSchema(map, diff);
    } else {
      return null;
    }
  }

  public static TupleMetadata diffTuple(TupleMetadata base,
      TupleMetadata subtend) {
    TupleMetadata diff = new TupleSchema();
    for (ColumnMetadata col : base) {
      ColumnMetadata other = subtend.metadata(col.name());
      if (other == null) {
        diff.addColumn(col);
      } else if (col.isMap()) {
        ColumnMetadata mapDiff = diffMap(col, other);
        if (mapDiff != null) {
          diff.addColumn(mapDiff);
        }
      }
    }
    return diff;
  }

  public static boolean hasDynamicColumns(TupleMetadata schema) {
    for (ColumnMetadata col : schema) {
      if (col.isDynamic()) {
        return true;
      }
    }
    return false;
  }

  public static boolean isRepeatedList(ColumnMetadata col) {
    return col.type() == MinorType.LIST &&
           col.mode() == DataMode.REPEATED;
  }
}
