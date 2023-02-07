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
package org.apache.drill.exec.dotdrill;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.calcite.avatica.util.TimeUnit;
import org.apache.calcite.rel.type.DynamicRecordType;
import org.apache.calcite.sql.SqlIntervalQualifier;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.sql.type.SqlTypeFamily;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.planner.StarColumnHelper;
import org.apache.drill.exec.planner.types.RelDataTypeDrillImpl;
import org.apache.drill.exec.planner.types.RelDataTypeHolder;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.sql.type.SqlTypeName;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;


@JsonTypeName("view")
public class View {

  private final String name;
  private String sql;
  private List<Field> fields;

  /* Current schema when view is created (not the schema to which view belongs to) */
  private List<String> workspaceSchemaPath;

  @JsonInclude(Include.NON_NULL)
  public static class Field {

    private final String name;
    private final SqlTypeName type;
    private final Boolean isNullable;
    private Integer precision;
    private Integer scale;
    private SqlIntervalQualifier intervalQualifier;
    private Field keyType;
    private Field valueType;


    @JsonCreator
    public Field(
        @JsonProperty("name")                       String name,
        @JsonProperty("type")                       SqlTypeName type,
        @JsonProperty("precision")                  Integer precision,
        @JsonProperty("scale")                      Integer scale,
        @JsonProperty("startUnit")                  TimeUnit startUnit,
        @JsonProperty("endUnit")                    TimeUnit endUnit,
        @JsonProperty("fractionalSecondPrecision")  Integer fractionalSecondPrecision,
        @JsonProperty("isNullable")                 Boolean isNullable,
        @JsonProperty("keyType") Field keyType,
        @JsonProperty("valueType") Field valueType) {
      // Fix for views which were created on Calcite 1.4.
      // After Calcite upgrade star "*" was changed on dynamic star "**" (SchemaPath.DYNAMIC_STAR)
      // and type of star was changed to SqlTypeName.DYNAMIC_STAR
      this.name = "*".equals(name) ? SchemaPath.DYNAMIC_STAR : name;
      this.type = "*".equals(name) && type == SqlTypeName.ANY ? SqlTypeName.DYNAMIC_STAR : type;
      this.precision = precision;
      this.scale = scale;
      this.intervalQualifier =
          null == startUnit
          ? null
          : new SqlIntervalQualifier(
              startUnit, precision, endUnit, fractionalSecondPrecision, SqlParserPos.ZERO );

      // Property "isNullable" is not part of the initial view definition and
      // was added in DRILL-2342.  If the default value is null, consider it as
      // "true".  It is safe to default to "nullable" than "required" type.
      this.isNullable = isNullable == null || isNullable;
      this.keyType = keyType;
      this.valueType = valueType;
    }

    public Field(String name, RelDataType dataType) {
      this.name = name;
      this.type = dataType.getSqlTypeName();
      this.isNullable = dataType.isNullable();
      this.intervalQualifier = dataType.getIntervalQualifier();
      switch (dataType.getSqlTypeName()) {
        case CHAR:
        case BINARY:
        case VARBINARY:
        case VARCHAR:
          this.precision = dataType.getPrecision();
          break;
        case DECIMAL:
          this.precision = dataType.getPrecision();
          this.scale = dataType.getScale();
          break;
        case INTERVAL_YEAR:
        case INTERVAL_YEAR_MONTH:
        case INTERVAL_MONTH:
        case INTERVAL_DAY:
        case INTERVAL_DAY_HOUR:
        case INTERVAL_DAY_MINUTE:
        case INTERVAL_DAY_SECOND:
        case INTERVAL_HOUR:
        case INTERVAL_HOUR_MINUTE:
        case INTERVAL_HOUR_SECOND:
        case INTERVAL_MINUTE:
        case INTERVAL_MINUTE_SECOND:
        case INTERVAL_SECOND:
          this.precision = dataType.getIntervalQualifier().getStartPrecisionPreservingDefault();
          break;
        case MAP:
          keyType = new Field(dataType.getKeyType());
          valueType = new Field(dataType.getValueType());
          break;
      }
    }

    /**
     * Overloaded constructor for creation of fields
     * which carry only about dataType and don't represent
     * named columns. Example of such usage is key and value types
     * of Map columns.
     *
     * @param dataType field type
     */
    public Field(RelDataType dataType) {
      this(null, dataType);
    }

    /**
     * Gets the name of this field.
     */
    public String getName() {
      return name;
    }

    /**
     * Gets the data type of this field.
     * (Data type only; not full datatype descriptor.)
     */
    public SqlTypeName getType() {
      return type;
    }

    /**
     * Gets the precision of the data type descriptor of this field.
     * The precision is the precision for a numeric type, the length for a
     * string type, or the start unit precision for an interval type.
     * */
    public Integer getPrecision() {
      return precision;
    }

    /**
     * Gets the numeric scale of the data type descriptor of this field,
     * for numeric types.
     */
    public Integer getScale() {
      return scale;
    }

    /**
     * Gets the interval type qualifier of the interval data type descriptor of
     * this field (<i>iff</i> interval type). */
    @JsonIgnore
    public SqlIntervalQualifier getIntervalQualifier() {
      return intervalQualifier;
    }

    /**
     * Gets the time range start unit of the type qualifier of the interval data
     * type descriptor of this field (<i>iff</i> interval type).
     */
    public TimeUnit getStartUnit() {
      return null == intervalQualifier ? null : intervalQualifier.getStartUnit();
    }

    /**
     * Gets the time range end unit of the type qualifier of the interval data
     * type descriptor of this field (<i>iff</i> interval type).
     */
    public TimeUnit getEndUnit() {
      return null == intervalQualifier ? null : intervalQualifier.getEndUnit();
    }

    /**
     * Gets the fractional second precision of the type qualifier of the interval
     * data type descriptor of this field (<i>iff</i> interval type).
     * Gets the interval type descriptor's fractional second precision
     * (<i>iff</i> interval type).
     */
    public Integer getFractionalSecondPrecision() {
      return null == intervalQualifier ? null : intervalQualifier.getFractionalSecondPrecisionPreservingDefault();
    }

    /**
     * Gets the nullability of the data type desription of this field.
     */
    public Boolean getIsNullable() {
      return isNullable;
    }

    /**
     * Gets key type for fields whose type is
     * {@link org.apache.calcite.sql.type.SqlTypeName#MAP}
     *
     * @return key type of map
     */
    public Field getKeyType() {
      return keyType;
    }

    /**
     * Gets value type for fields whose type is
     * {@link org.apache.calcite.sql.type.SqlTypeName#MAP}
     *
     * @return value type of map
     */
    public Field getValueType() {
      return valueType;
    }


    /**
     * Checks whether this field type is interval
     * by comparing current type family with known
     * INTERVAL_YEAR_MONTH and INTERVAL_DAY_TIME families
     *
     * @return whether current type relates to any known
     *         interval families
     */
    @JsonIgnore
    boolean isInterval() {
      SqlTypeFamily family = type.getFamily();
      return family == SqlTypeFamily.INTERVAL_YEAR_MONTH || family == SqlTypeFamily.INTERVAL_DAY_TIME;
    }

    /**
     * Checks that for MAP fields key and value types
     * were persisted
     *
     * @return is map key and value types present
     */
    @JsonIgnore
    boolean isMapTypesPresent() {
      return keyType != null && valueType != null;
    }

  }


  public View(String name, String sql, RelDataType rowType, List<String> workspaceSchemaPath) {
    this(name,
        sql,
        rowType.getFieldList().stream()
            .map(f -> new Field(f.getName(), f.getType()))
            .collect(Collectors.toList()),
        workspaceSchemaPath);
  }

  @JsonCreator
  public View(@JsonProperty("name") String name,
              @JsonProperty("sql") String sql,
              @JsonProperty("fields") List<Field> fields,
              @JsonProperty("workspaceSchemaPath") List<String> workspaceSchemaPath) {
    this.name = name;
    this.sql = sql;
    this.fields = fields;
    // for backward compatibility since now all schemas and workspaces are case insensitive and stored in lower case
    // make sure that given workspace schema path is also in lower case
    this.workspaceSchemaPath = workspaceSchemaPath == null ? Collections.EMPTY_LIST :
        workspaceSchemaPath.stream()
            .map(String::toLowerCase)
            .collect(Collectors.toList());
  }


  /**
   * If view fields are present then attempts to gather them
   * into struct type, otherwise returns extension of  {@link DynamicRecordType}.
   *
   * @param factory factory for rel data types creation
   * @return struct type that describes names and types of all
   *         view fields or extension of {@link DynamicRecordType}
   *         when view fields are empty
   */
  public RelDataType getRowType(RelDataTypeFactory factory) {

    // if there are no fields defined, this is a dynamic view.
    if (isDynamic()) {
      return new RelDataTypeDrillImpl(new RelDataTypeHolder(), factory);
    }

    List<RelDataType> types = new ArrayList<>(fields.size());
    List<String> names = new ArrayList<>(fields.size());
    for (Field field : fields) {
      names.add(field.getName());
      types.add(getType(field, factory));
    }
    return factory.createStructType(types, names);
  }

  private RelDataType getType(Field field, RelDataTypeFactory factory) {
    RelDataType type;
    final SqlTypeName typeName = field.getType();
    final Integer precision = field.getPrecision();
    final Integer scale = field.getScale();

    if (field.isInterval()) {
      type = factory.createSqlIntervalType(field.getIntervalQualifier());
    } else if (precision != null) {
      type = scale != null
          ? factory.createSqlType(typeName, precision, scale)
          : factory.createSqlType(typeName, precision);
    } else if (typeName == SqlTypeName.MAP) {
      if (field.isMapTypesPresent()) {
        type = factory.createMapType(getType(field.getKeyType(), factory), getType(field.getValueType(), factory));
      } else {
         /*
            For older views that doesn't have info about map key and value types,
            chosen type is ANY. Because use of raw MAP type causes creation of
            MAP cast expression that can't be serialized by ExpressionStringBuilder's
            visitCastExpression(CastExpression e, StringBuilder sb) method.
            See DRILL-6944 for more details.
         */
        type = factory.createSqlType(SqlTypeName.ANY);
      }
    } else if (typeName == SqlTypeName.ARRAY || typeName == SqlTypeName.ROW) {
      /*
       * Treat as ANY to avoid generation of unsupported casts, like:
       * CAST(fieldName,'ARRAY') or CAST(fieldName,'ROW')
       */
      type = factory.createSqlType(SqlTypeName.ANY);
    } else {
      type = factory.createSqlType(field.getType());
    }

    return field.getIsNullable() ? factory.createTypeWithNullability(type, true) : type;
  }

  @JsonIgnore
  public boolean isDynamic(){
    return fields.isEmpty();
  }

  @JsonIgnore
  public boolean hasStar() {
    for (Field field : fields) {
      if (StarColumnHelper.isNonPrefixedStarColumn(field.getName())) {
        return true;
      }
    }
    return false;
  }

  public String getSql() {
    return sql;
  }

  public void setSql(String sql) {
    this.sql = sql;
  }

  public String getName() {
    return name;
  }

  public List<Field> getFields() {
    return fields;
  }

  public List<String> getWorkspaceSchemaPath() {
    return workspaceSchemaPath;
  }

}
