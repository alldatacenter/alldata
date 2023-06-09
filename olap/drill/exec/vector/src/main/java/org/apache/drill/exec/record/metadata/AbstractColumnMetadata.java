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

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.metadata.schema.parser.SchemaExprParser;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Abstract definition of column metadata. Allows applications to create
 * specialized forms of a column metadata object by extending from this abstract
 * class.
 * <p>
 * Note that, by design, primitive columns do not have a link to their tuple
 * parent, or their index within that parent. This allows the same metadata to
 * be shared between two views of a tuple, perhaps physical and projected views.
 * This restriction does not apply to map columns, since maps (and the row
 * itself) will, by definition, differ between the two views.
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE,
                getterVisibility = JsonAutoDetect.Visibility.NONE,
                isGetterVisibility = JsonAutoDetect.Visibility.NONE,
                setterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonPropertyOrder({ "name", "type", "mode", "format", "default",
    "properties" })
public abstract class AbstractColumnMetadata extends AbstractPropertied
    implements ColumnMetadata {

  // Capture the key schema information. We cannot use the MaterializedField
  // or MajorType because they encode child information that we encode here
  // as a child schema. Keeping the two in sync is nearly impossible.

  protected final String name;
  protected final MinorType type;
  protected final DataMode mode;
  protected final int precision;
  protected final int scale;

  @JsonCreator
  public static AbstractColumnMetadata createColumnMetadata(
      @JsonProperty("name") String name, @JsonProperty("type") String type,
      @JsonProperty("mode") DataMode mode,
      @JsonProperty("properties") Map<String, String> properties)
      throws IOException {
    ColumnMetadata columnMetadata = SchemaExprParser.parseColumn(name, type,
        mode);
    columnMetadata.setProperties(properties);
    return (AbstractColumnMetadata) columnMetadata;
  }

  public AbstractColumnMetadata(MaterializedField schema) {
    this(schema.getName(), schema.getType());
  }

  public AbstractColumnMetadata(String name, MajorType majorType) {
    this.name = name;
    type = majorType.getMinorType();
    mode = majorType.getMode();
    precision = majorType.hasPrecision() ? majorType.getPrecision() : -1;
    scale = majorType.hasScale() ? majorType.getScale() : -1;
  }

  public AbstractColumnMetadata(String name, MinorType type, DataMode mode) {
    this.name = name;
    this.type = type;
    this.mode = mode;
    precision = -1;
    scale = -1;
  }

  public AbstractColumnMetadata(AbstractColumnMetadata from) {
    super(from);
    this.name = from.name;
    this.type = from.type;
    this.mode = from.mode;
    this.precision = from.precision;
    this.scale = from.scale;
    setProperties(from.properties());
  }

  @Override
  public void bind(TupleMetadata parentTuple) { }

  @JsonProperty("name")
  @Override
  public String name() { return name; }

  @Override
  public MinorType type() { return type; }

  @Override
  public MajorType majorType() {
    return MajorType.newBuilder().setMinorType(type()).setMode(mode()).build();
  }

  @JsonProperty("mode")
  @Override
  public DataMode mode() { return mode; }

  @Override
  public boolean isNullable() {
    return mode() == DataMode.OPTIONAL;
  }

  @Override
  public boolean isArray() {
    return mode() == DataMode.REPEATED;
  }

  @Override
  public int dimensions() {
    return isArray() ? 1 : 0;
  }

  @Override
  public boolean isMap() { return false; }

  @Override
  public boolean isVariant() { return false; }

  @Override
  public boolean isMultiList() { return false; }

  @Override
  public boolean isDict() { return false; }

  @Override
  public boolean isDynamic() { return false; }

  @Override
  public TupleMetadata tupleSchema() { return null; }

  @Override
  public VariantMetadata variantSchema() { return null; }

  @Override
  public ColumnMetadata childSchema() { return null; }

  @Override
  public boolean isVariableWidth() {
    return Types.isVarWidthType(type());
  }

  @Override
  public boolean isEquivalent(ColumnMetadata o) {
    if (o == this) {
      return true;
    }
    if (o == null || o.getClass() != getClass()) {
      return false;
    }

    AbstractColumnMetadata other = (AbstractColumnMetadata) o;
    // Note: don't compare properties; we seldom care if those are identical.
    return name.equalsIgnoreCase(other.name) &&
           Objects.equals(type, other.type) &&
           Objects.equals(mode, other.mode);
  }

  public boolean equalsWithProperties(AbstractColumnMetadata other) {
    if (!super.equals(other)) {
      return false;
    }
    return isEquivalent(other);
  }

  @Override
  public int expectedWidth() { return 0; }

  @Override
  public void setExpectedWidth(int width) { }

  /**
   * Returns precision for current column. For the case when precision is not
   * set or column type does not support precision, negative value will be
   * returned.
   *
   * @return precision for current column
   */
  @Override
  public int precision() { return -1; }

  /**
   * Returns scale for current column. For the case when scale is not set or
   * column type does not support scale, negative value will be returned.
   *
   * @return scale for current column
   */
  @Override
  public int scale() { return -1; }

  @Override
  public void setExpectedElementCount(int childCount) {
    // The allocation utilities don't like an array size of zero, so set to
    // 1 as the minimum. Adjusted to avoid trivial errors if the caller
    // makes an error.

    if (isArray()) {
      PropertyAccessor.set(this, EXPECTED_CARDINALITY_PROP,
          Math.max(1, childCount));
    }
  }

  @Override
  public int expectedElementCount() {
    if (isArray()) {
      // Not set means default size
      return PropertyAccessor.getInt(this, EXPECTED_CARDINALITY_PROP,
          DEFAULT_ARRAY_SIZE);
    } else {
      // Cardinality always 1 for optional, repeated modes
      return 1;
    }
  }

  @Override
  public void setFormat(String value) {
    setProperty(FORMAT_PROP, value);
  }

  @Override
  public String format() {
    return property(FORMAT_PROP);
  }

  @Override
  public DateTimeFormatter dateTimeFormatter() {
    throw new UnsupportedOperationException(
        "Date/time not supported for non-scalar columns");
  }

  @Override
  public void setDefaultValue(String value) {
    setProperty(DEFAULT_VALUE_PROP, value);
  }

  @Override
  public String defaultValue() {
    return property(DEFAULT_VALUE_PROP);
  }

  @Override
  public Object decodeDefaultValue() {
    return valueFromString(defaultValue());
  }

  @Override
  public Object valueFromString(String value) {
    throw new UnsupportedOperationException(
        "Value conversion not supported for non-scalar columns");
  }

  @Override
  public String valueToString(Object value) {
    throw new UnsupportedOperationException(
        "Value conversion not supported for non-scalar columns");
  }

  @JsonProperty("properties")
  @Override
  public Map<String, String> properties() {
    return super.properties();
  }

  @Override
  public String toString() {
    final StringBuilder buf = new StringBuilder().append("[")
        .append(getClass().getSimpleName()).append(" ")
        .append(schema().toString(false));
    appendContents(buf);
    if (hasProperties()) {
      buf.append(", properties: ").append(properties());
    }
    return buf.append("]").toString();
  }

  protected void appendContents(StringBuilder buf) { }

  // All subclasses must implement this in a way that the
  // parser can parse back to type the same instance.
  @JsonProperty("type")
  @Override
  public abstract String typeString();

  @Override
  public String columnString() {
    StringBuilder builder = new StringBuilder();
    builder.append("`").append(escapeSpecialSymbols(name())).append("`");
    builder.append(" ");
    builder.append(typeString());

    // Drill does not have nullability notion for complex types
    if (!isNullable() && !isArray() && !isMap() && !isDict()) {
      builder.append(" NOT NULL");
    }

    if (hasProperties()) {
      if (format() != null) {
        builder.append(" FORMAT '").append(format()).append("'");
      }

      if (defaultValue() != null) {
        builder.append(" DEFAULT '").append(defaultValue()).append("'");
      }

      Map<String, String> copy = new HashMap<>(properties());
      copy.remove(FORMAT_PROP);
      copy.remove(DEFAULT_VALUE_PROP);
      if (!copy.isEmpty()) {
        builder.append(" PROPERTIES { ");
        builder.append(copy.entrySet().stream()
            .map(e -> String.format("'%s' = '%s'", e.getKey(), e.getValue()))
            .collect(Collectors.joining(", ")));
        builder.append(" }");
      }
    }

    return builder.toString();
  }

  @Override
  public boolean equals(Object o) {
    // Equals not implemented in this class because we don't compare
    // all fields. And, since this object should never be a key, we do
    // not implement hashCode(). Per project conventions, if we don't
    // implement hashCode(), we cannot implement equals().
    throw new UnsupportedOperationException("Use isEquivalent() instead.");
   }

  public boolean equalsWithProperties(Object o) {
    if (!equals(o)) {
      return false;
    }
    return super.equals(o);
  }

  /**
   * If given value contains backticks (`) or backslashes (\), escapes them.
   *
   * @param value
   *          string value
   * @return updated value
   */
  protected String escapeSpecialSymbols(String value) {
    return value.replaceAll("(\\\\)|(`)", "\\\\$0");
  }
}