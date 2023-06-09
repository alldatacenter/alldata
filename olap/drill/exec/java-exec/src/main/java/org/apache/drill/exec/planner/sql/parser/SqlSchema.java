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
package org.apache.drill.exec.planner.sql.parser;

import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlCharStringLiteral;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlOperator;
import org.apache.calcite.sql.SqlSpecialOperator;
import org.apache.calcite.sql.SqlWriter;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.sql.util.SqlBasicVisitor;
import org.apache.drill.common.util.DrillStringUtils;
import org.apache.drill.exec.planner.sql.SchemaUtilites;
import org.apache.drill.exec.planner.sql.handlers.AbstractSqlHandler;
import org.apache.drill.exec.planner.sql.handlers.SqlHandlerConfig;
import org.apache.drill.exec.planner.sql.handlers.SchemaHandler;
import org.apache.drill.exec.planner.sql.handlers.SqlHandlerUtil;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Parent class for CREATE, DROP, DESCRIBE, ALTER SCHEMA commands.
 * Holds logic common command property: table, path.
 */
public abstract class SqlSchema extends DrillSqlCall {

  protected final SqlIdentifier table;
  protected final SqlNode path;

  protected SqlSchema(SqlParserPos pos, SqlIdentifier table, SqlNode path) {
    super(pos);
    this.table = table;
    this.path = path;
  }

  @Override
  public void unparse(SqlWriter writer, int leftPrec, int rightPrec) {
    if (table != null) {
      writer.keyword("FOR TABLE");
      table.unparse(writer, leftPrec, rightPrec);
    }

    if (path != null) {
      writer.keyword("PATH");
      path.unparse(writer, leftPrec, rightPrec);
    }
  }

  public boolean hasTable() {
    return table != null;
  }

  public SqlIdentifier getTable() {
    return table;
  }

  public List<String> getSchemaPath() {
    return hasTable() ? SchemaUtilites.getSchemaPath(table) : null;
  }

  public String getTableName() {
    if (hasTable()) {
      String tableName = table.isSimple() ? table.getSimple() : table.names.get(table.names.size() - 1);
      return DrillStringUtils.removeLeadingSlash(tableName);
    }
    return null;
  }

  public String getPath() {
    return path == null ? null : path.accept(LiteralVisitor.INSTANCE);
  }

  protected Map<String, String> getProperties(SqlNodeList properties) {
    if (properties == null) {
      return null;
    }

    // preserve properties order
    Map<String, String> map = new LinkedHashMap<>();
    for (int i = 1; i < properties.size(); i += 2) {
      map.put(properties.get(i - 1).accept(LiteralVisitor.INSTANCE),
        properties.get(i).accept(LiteralVisitor.INSTANCE));
    }
    return map;
  }

  /**
   * Visits literal and returns bare value (i.e. single quotes).
   */
  private static class LiteralVisitor extends SqlBasicVisitor<String> {

    static final LiteralVisitor INSTANCE = new LiteralVisitor();

    @Override
    public String visit(SqlLiteral literal) {
      return literal.toValue();
    }

  }

  /**
   * CREATE SCHEMA sql call.
   */
  public static class Create extends SqlSchema {

    private final SqlCharStringLiteral schema;
    private final SqlNode load;
    private final SqlNodeList properties;
    private final SqlLiteral createType;

    public static final SqlSpecialOperator OPERATOR = new SqlSpecialOperator("CREATE_SCHEMA", SqlKind.OTHER_DDL) {
      @Override
      public SqlCall createCall(SqlLiteral functionQualifier, SqlParserPos pos, SqlNode... operands) {
        return new Create(pos, (SqlCharStringLiteral) operands[0], operands[1],
          (SqlIdentifier) operands[2], operands[3], (SqlNodeList) operands[4], (SqlLiteral) operands[5]);
      }
    };

    public Create(SqlParserPos pos,
                  SqlCharStringLiteral schema,
                  SqlNode load,
                  SqlIdentifier table,
                  SqlNode path,
                  SqlNodeList properties,
                  SqlLiteral createType) {
      super(pos, table, path);
      this.schema = schema;
      this.load = load;
      this.properties = properties;
      this.createType = createType;
    }

    @Override
    public SqlOperator getOperator() {
      return OPERATOR;
    }

    @Override
    public List<SqlNode> getOperandList() {
      return Arrays.asList(schema, load, table, path, properties, createType);
    }

    @Override
    public void unparse(SqlWriter writer, int leftPrec, int rightPrec) {
      writer.keyword("CREATE");

      if (SqlCreateType.OR_REPLACE == getSqlCreateType()) {
        writer.keyword("OR");
        writer.keyword("REPLACE");
      }

      if (schema != null) {
        writer.keyword("SCHEMA");
        writer.literal(getSchema());
      }

      if (load != null) {
        writer.keyword("LOAD");
        load.unparse(writer, leftPrec, rightPrec);
      }

      super.unparse(writer, leftPrec, rightPrec);

      if (properties != null) {
        writer.keyword("PROPERTIES");
        SqlHandlerUtil.unparseKeyValuePairs(writer, leftPrec, rightPrec, properties);
      }
    }

    @Override
    public AbstractSqlHandler getSqlHandler(SqlHandlerConfig config) {
      return new SchemaHandler.Create(config);
    }

    public boolean hasSchema() {
      return schema != null;
    }

    public String getSchema() {
      return hasSchema() ? schema.toValue() : null;
    }

    public String getLoad() {
      return load == null ? null : load.accept(LiteralVisitor.INSTANCE);
    }

    public Map<String, String> getProperties() {
      return getProperties(properties);
    }

    public SqlCreateType getSqlCreateType() {
      return SqlCreateType.valueOf(createType.toValue());
    }
  }

  /**
   * DROP SCHEMA sql call.
   */
  public static class Drop extends SqlSchema {

    private final SqlLiteral existenceCheck;

    public static final SqlSpecialOperator OPERATOR = new SqlSpecialOperator("DROP_SCHEMA", SqlKind.OTHER_DDL) {
      @Override
      public SqlCall createCall(SqlLiteral functionQualifier, SqlParserPos pos, SqlNode... operands) {
        return new Drop(pos, (SqlIdentifier) operands[0], (SqlLiteral) operands[1]);
      }
    };

    public Drop(SqlParserPos pos, SqlIdentifier table, SqlLiteral existenceCheck) {
      super(pos, table, null);
      this.existenceCheck = existenceCheck;
    }

    @Override
    public SqlOperator getOperator() {
      return OPERATOR;
    }

    @Override
    public List<SqlNode> getOperandList() {
      return Arrays.asList(table, existenceCheck);
    }

    @Override
    public void unparse(SqlWriter writer, int leftPrec, int rightPrec) {
      writer.keyword("DROP");
      writer.keyword("SCHEMA");

      if (ifExists()) {
        writer.keyword("IF");
        writer.keyword("EXISTS");
      }

      super.unparse(writer, leftPrec, rightPrec);
    }

    @Override
    public AbstractSqlHandler getSqlHandler(SqlHandlerConfig config) {
      return new SchemaHandler.Drop(config);
    }

    public boolean ifExists() {
      return existenceCheck.booleanValue();
    }
  }

  /**
   * DESCRIBE SCHEMA FOR TABLE sql call.
   */
  public static class Describe extends SqlSchema {

    private final SqlLiteral format;

    public static final SqlSpecialOperator OPERATOR = new SqlSpecialOperator(SqlKind.DESCRIBE_SCHEMA.name(), SqlKind.DESCRIBE_SCHEMA) {
      @Override
      public SqlCall createCall(SqlLiteral functionQualifier, SqlParserPos pos, SqlNode... operands) {
        return new Describe(pos, (SqlIdentifier) operands[0], (SqlLiteral) operands[1]);
      }
    };

    public Describe(SqlParserPos pos, SqlIdentifier table, SqlLiteral format) {
      super(pos, table, null);
      this.format = format;
    }

    @Override
    public SqlOperator getOperator() {
      return OPERATOR;
    }

    @Override
    public List<SqlNode> getOperandList() {
      return Arrays.asList(table, format);
    }

    @Override
    public void unparse(SqlWriter writer, int leftPrec, int rightPrec) {
      writer.keyword("DESCRIBE");
      writer.keyword("SCHEMA");

      super.unparse(writer, leftPrec, rightPrec);

      writer.keyword("AS");
      writer.keyword(getFormat().name());
    }

    public Describe.Format getFormat() {
      return Format.valueOf(format.toValue());
    }

    /**
     * Enum which specifies format of DESCRIBE SCHEMA FOR table output.
     */
    public enum Format {

      /**
       * Schema will be output in JSON format used to store schema
       * in {@link org.apache.drill.exec.record.metadata.schema.SchemaProvider#DEFAULT_SCHEMA_NAME} file.
       */
      JSON,

      /**
       * Schema will be output in CREATE SCHEMA command syntax.
       */
      STATEMENT
    }
  }

  public static class Add extends SqlSchema {

    private final SqlLiteral replace;
    private final SqlCharStringLiteral schema;
    private final SqlNodeList properties;

    public static final SqlSpecialOperator OPERATOR = new SqlSpecialOperator("ALTER_SCHEMA_ADD", SqlKind.OTHER_DDL) {
      @Override
      public SqlCall createCall(SqlLiteral functionQualifier, SqlParserPos pos, SqlNode... operands) {
        return new Add(pos, (SqlIdentifier) operands[0], operands[1], (SqlLiteral) operands[2],
          (SqlCharStringLiteral) operands[3], (SqlNodeList) operands[4]);
      }
    };

    public Add(SqlParserPos pos,
               SqlIdentifier table,
               SqlNode path,
               SqlLiteral replace,
               SqlCharStringLiteral schema,
               SqlNodeList properties) {
      super(pos, table, path);
      this.replace = replace;
      this.schema = schema;
      this.properties = properties;
    }

    @Override
    public SqlOperator getOperator() {
      return OPERATOR;
    }

    @Override
    public List<SqlNode> getOperandList() {
      return Arrays.asList(table, path, replace, schema, properties);
    }

    @Override
    public void unparse(SqlWriter writer, int leftPrec, int rightPrec) {
      writer.keyword("ALTER");
      writer.keyword("SCHEMA");
      writer.keyword("ADD");

      if (replace.booleanValue()) {
        writer.keyword("OR");
        writer.keyword("REPLACE");
      }

      super.unparse(writer, leftPrec, rightPrec);

      if (schema != null) {
        writer.keyword("COLUMNS");
        writer.literal(getSchema());
      }

      if (properties != null) {
        writer.keyword("PROPERTIES");
        SqlHandlerUtil.unparseKeyValuePairs(writer, leftPrec, rightPrec, properties);
      }
    }

    @Override
    public AbstractSqlHandler getSqlHandler(SqlHandlerConfig config) {
      return new SchemaHandler.Add(config);
    }

    public boolean isReplace() {
      return replace.booleanValue();
    }

    public boolean hasSchema() {
      return schema != null;
    }

    public String getSchema() {
      return hasSchema() ? schema.toValue() : null;
    }

    public boolean hasProperties() {
      return properties != null;
    }

    public Map<String, String> getProperties() {
      return getProperties(properties);
    }
  }

  public static class Remove extends SqlSchema {

    private final SqlNodeList columns;
    private final SqlNodeList properties;

    public static final SqlSpecialOperator OPERATOR = new SqlSpecialOperator("ALTER_SCHEMA_REMOVE", SqlKind.OTHER_DDL) {
      @Override
      public SqlCall createCall(SqlLiteral functionQualifier, SqlParserPos pos, SqlNode... operands) {
        return new Remove(pos, (SqlIdentifier) operands[0], operands[1],
          (SqlNodeList) operands[2], (SqlNodeList) operands[3]);
      }
    };

    public Remove(SqlParserPos pos,
                  SqlIdentifier table,
                  SqlNode path,
                  SqlNodeList columns,
                  SqlNodeList properties) {
      super(pos, table, path);
      this.columns = columns;
      this.properties = properties;
    }

    @Override
    public SqlOperator getOperator() {
      return OPERATOR;
    }

    @Override
    public List<SqlNode> getOperandList() {
      return Arrays.asList(table, path, columns, properties);
    }

    @Override
    public void unparse(SqlWriter writer, int leftPrec, int rightPrec) {
      writer.keyword("ALTER");
      writer.keyword("SCHEMA");
      writer.keyword("REMOVE");

      super.unparse(writer, leftPrec, rightPrec);

      if (columns != null) {
        writer.keyword("COLUMNS");
        SqlHandlerUtil.unparseSqlNodeList(writer, leftPrec, rightPrec, columns);
      }

      if (properties != null) {
        writer.keyword("PROPERTIES");
        SqlHandlerUtil.unparseSqlNodeList(writer, leftPrec, rightPrec, properties);
      }
    }

    @Override
    public AbstractSqlHandler getSqlHandler(SqlHandlerConfig config) {
      return new SchemaHandler.Remove(config);
    }

    public List<String> getColumns() {
      if (columns == null) {
        return null;
      }
      return columns.getList().stream()
        .map(SqlNode::toString)
        .collect(Collectors.toList());
    }

    public boolean hasProperties() {
      return properties != null;
    }

    public List<String> getProperties() {
      if (properties == null) {
        return null;
      }
      return properties.getList().stream()
        .map(property -> property.accept(LiteralVisitor.INSTANCE))
        .collect(Collectors.toList());
    }
  }
}
