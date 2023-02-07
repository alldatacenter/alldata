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

import java.time.format.DateTimeFormatter;

import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.vector.accessor.ColumnWriter;

/**
 * Metadata description of a column including names, types and structure
 * information.
 */
public interface ColumnMetadata extends Propertied {

  /**
   * Predicted number of elements per array entry. Default is
   * taken from the often hard-coded value of 10.
   */
  String EXPECTED_CARDINALITY_PROP = DRILL_PROP_PREFIX + "cardinality";

  /**
   * Default value represented as a string.
   */
  String DEFAULT_VALUE_PROP = DRILL_PROP_PREFIX + "default";

  /**
   * Expected (average) width for variable-width columns.
   */
  String EXPECTED_WIDTH_PROP = DRILL_PROP_PREFIX + "width";

  /**
   * Optional format to use when converting to/from string values.
   */
  String FORMAT_PROP = DRILL_PROP_PREFIX + "format";

  /**
   * Indicates how to handle blanks. Must be one of the valid values defined
   * in AbstractConvertFromString. Normally set on the converter by the plugin
   * rather than by the user in the schema.
   */
  String BLANK_AS_PROP = DRILL_PROP_PREFIX + "blank-as";

  /**
   * Convert blanks to null values (if the column is nullable), or
   * fill with the default value (non-nullable.)
   */
  String BLANK_AS_NULL = "null";

  /**
   * Convert blanks for numeric fields to 0. For non-numeric
   * fields, convert to null (for nullable) or the default value
   * (for non-nullable). Works best if non-numeric fields are declared
   * as nullable.
   */
  String BLANK_AS_ZERO = "0";

  /**
   * Indicates whether to project the column in a wildcard (*) query.
   * Special columns may be excluded from projection. Certain "special"
   * columns may be available only when explicitly requested. For example,
   * the log reader has a "_raw" column which includes the entire input
   * line before parsing. This column can be requested explicitly:<br>
   * <tt>SELECT foo, bar, _raw FROM ...</tt><br>
   * but the column will <i>not</i> be included when using the wildcard:<br>
   * <tt>SELECT * FROM ...</tt>
   * <p>
   * Marking a column (either in the provided schema or the reader schema)
   * will prevent that column from appearing in a wildcard expansion.
   */
  String EXCLUDE_FROM_WILDCARD = DRILL_PROP_PREFIX + "special";

  int DEFAULT_ARRAY_SIZE = 10;

  /**
   * Indicates that a provided schema column is an implicit column
   * (one defined by Drill rather than the reader.) Allows the implicit
   * schema to reify partition names, say, as reader-specific names.
   * For example, {@code dir0} might be reified as {@code year}, etc.
   * <p>
   * Available when the underlying reader supports implicit columns.
   * The value is the defined implicit column name (not the name
   * set via system/session options.) Using the defined name makes
   * the provided schema immune from runtime changes to column names.
   * <p>
   * As the result of adding this feature, any column <i>not</i>
   * tagged as implicit is a reader column, even if that column
   * happens to have the same (currently selected runtime) name
   * as an implicit column.
   */
  String IMPLICIT_COL_TYPE = DRILL_PROP_PREFIX + "implicit";

  /**
   * Fully-qualified name implicit column type.
   */
  String IMPLICIT_FQN = "fqn";

  /**
   * File path implicit column type.
   */
  String IMPLICIT_FILEPATH = "filepath";

  /**
   * File name implicit column type.
   */
  String IMPLICIT_FILENAME = "filename";

  /**
   * File suffix implicit column type.
   */
  String IMPLICIT_SUFFIX = "suffix";

  /**
   * Prefix for partition directories. dir0 is the table root
   * folder, dir1 the first subdirectory, and so on. Directories that
   * don't exist in the actual file path take a {@code NULL} value.
   */
  String IMPLICIT_PARTITION_PREFIX = "dir";

  /**
   * Rough characterization of Drill types into metadata categories.
   * Various aspects of Drill's type system are very, very messy.
   * However, Drill is defined by its code, not some abstract design,
   * so the metadata system here does the best job it can to simplify
   * the messy type system while staying close to the underlying
   * implementation.
   */
  enum StructureType {

    /**
     * Primitive column (all types except List, Map and Union.)
     * Includes (one-dimensional) arrays of those types.
     */
    PRIMITIVE,

    /**
     * Map or repeated map. Also describes the row as a whole.
     */
    TUPLE,

    /**
     * Union or (non-repeated) list. (A non-repeated list is,
     * essentially, a repeated union.)
     */
    VARIANT,

    /**
     * A repeated list. A repeated list is not simply the repeated
     * form of a list, it is something else entirely. It acts as
     * a dimensional wrapper around any other type (except list)
     * and adds a non-nullable extra dimension. Hence, this type is
     * for 2D+ arrays.
     * <p>
     * In theory, a 2D list of, say, INT would be an INT column, but
     * repeated in to dimensions. Alas, that is not how it is. Also,
     * if we have a separate category for 2D lists, we should have
     * a separate category for 1D lists. But, again, that is not how
     * the code has evolved.
     */
    MULTI_ARRAY,

    /**
     * Dict or repeated dict.
     */
    DICT,

    /**
     * Unknown, specified at runtime. (Only for logical columns,
     * not for physical columns.)
     */
    DYNAMIC
  }

  StructureType structureType();

  /**
   * Schema for <tt>TUPLE</tt> columns.
   *
   * @return the tuple schema
   */
  TupleMetadata tupleSchema();

  /**
   * Schema for <tt>VARIANT</tt> columns.
   *
   * @return the variant schema
   */
  VariantMetadata variantSchema();

  /**
   * Schema of inner dimension for <tt>MULTI_ARRAY<tt> columns.
   * If an array is 3D, the outer column represents all 3 dimensions.
   * <tt>outer.childSchema()</tt> gives another <tt>MULTI_ARRAY</tt>
   * for the inner 2D array.
   * <tt>outer.childSchema().childSchema()</tt> gives a column
   * of some other type (but repeated) for the 1D array.
   * <p>
   * Sorry for the mess, but it is how the code works and we are not
   * in a position to revisit data type fundamentals.
   *
   * @return the description of the (n-1) st dimension.
   */
  ColumnMetadata childSchema();
  MaterializedField schema();
  MaterializedField emptySchema();
  String name();
  MinorType type();
  MajorType majorType();
  DataMode mode();
  int dimensions();
  boolean isNullable();
  boolean isArray();
  boolean isVariableWidth();
  boolean isMap();
  boolean isVariant();
  boolean isDict();

  /**
   * Reports if the column is dynamic. A dynamic column is one with
   * a "type to be named later." It is valid for describing a dynamic
   * schema, but not for creating vectors; to create a vector the
   * column must be resolved to a concrete type. The context should
   * make it clear if any columns can be dynamic.
   * @return {@code true} if the column does not yet have a concrete
   * type, {@code false} if the column type is concrete
   */
  boolean isDynamic();

  /**
   * Determine if the schema represents a column with a LIST type with
   * UNION elements. (Lists can be of a single
   * type (with nullable elements) or can be of unions.)
   *
   * @return true if the column is of type LIST of UNIONs
   */
  boolean isMultiList();

  /**
   * Report whether one column is equivalent to another. Columns are equivalent
   * if they have the same name, type and structure (ignoring internal structure
   * such as properties.)
   */
  boolean isEquivalent(ColumnMetadata other);

  /**
   * For variable-width columns, specify the expected column width to be used
   * when allocating a new vector. Does nothing for fixed-width columns.
   *
   * @param width the expected column width
   */
  void setExpectedWidth(int width);

  /**
   * Get the expected width for a column. This is the actual width for fixed-
   * width columns, the specified width (defaulting to 50) for variable-width
   * columns.
   * @return the expected column width of the each data value. Does not include
   * "overhead" space such as for the null-value vector or offset vector
   */
  int expectedWidth();

  /**
   * For an array column, specify the expected average array cardinality.
   * Ignored for non-array columns. Used when allocating new vectors.
   *
   * @param childCount the expected average array cardinality. Defaults to
   * 1 for non-array columns, 10 for array columns
   */
  void setExpectedElementCount(int childCount);

  /**
   * Returns the expected array cardinality for array columns, or 1 for
   * non-array columns.
   *
   * @return the expected value cardinality per value (per-row for top-level
   * columns, per array element for arrays within lists)
   */
  int expectedElementCount();

  void setFormat(String value);

  String format();

  /**
   * Returns the formatter to use for date/time values. Only valid for
   * date/time columns.
   *
   * @return
   */
  DateTimeFormatter dateTimeFormatter();

  /**
   * Sets the default value property using the string-encoded form of the value.
   * The default value is used for filling a vector when no real data is available.
   *
   * @param value the default value in String representation
   */
  void setDefaultValue(String value);

  /**
   * Returns the default value for this column in String literal representation.
   *
   * @return the default value in String literal representation, or null if no
   * default value has been set
   */
  String defaultValue();

  /**
   * Returns the default value decoded into object form. This is the same as:
   * <pre><code>decodeValue(defaultValue());
   * </code></pre>
   *
   * @return the default value decode as an object that can be passed to
   * the {@link ColumnWriter#setObject()} method.
   */
  Object decodeDefaultValue();

  String valueToString(Object value);
  Object valueFromString(String value);

  /**
   * Create an empty version of this column. If the column is a scalar,
   * produces a simple copy. If a map, produces a clone without child
   * columns.
   *
   * @return empty clone of this column
   */
  ColumnMetadata cloneEmpty();

  int precision();
  int scale();

  void bind(TupleMetadata parentTuple);

  ColumnMetadata copy();

  /**
   * Converts type metadata into string representation
   * accepted by the table schema parser.
   *
   * @return type metadata string representation
   */
  String typeString();

  /**
   * Converts column metadata into string representation
   * accepted by the table schema parser.
   *
   * @return column metadata string representation
   */
  String columnString();
}
