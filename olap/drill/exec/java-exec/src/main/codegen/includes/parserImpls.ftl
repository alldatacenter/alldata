<#--

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

-->
<#--
  Add implementations of additional parser statements here.
  Each implementation should return an object of SqlNode type.

  Example of SqlShowTables() implementation:
  SqlNode SqlShowTables()
  {
    ...local variables...
  }
  {
    <SHOW> <TABLES>
    ...
    {
      return SqlShowTables(...)
    }
  }
-->
/**
 * Parses statement
 *   SHOW TABLES [{FROM | IN} db_name] [LIKE 'pattern' | WHERE expr]
 */
SqlNode SqlShowTables() :
{
    SqlParserPos pos;
    SqlIdentifier db = null;
    SqlNode likePattern = null;
    SqlNode where = null;
}
{
    <SHOW> { pos = getPos(); }
    <TABLES>
    [
        (<FROM> | <IN>) { db = CompoundIdentifier(); }
    ]
    [
        <LIKE> { likePattern = StringLiteral(); }
        |
        <WHERE> { where = Expression(ExprContext.ACCEPT_SUBQUERY); }
    ]
    {
        return new SqlShowTables(pos, db, likePattern, where);
    }
}

/**
 * Parses statement
 * SHOW FILES [{FROM | IN} schema]
 */
SqlNode SqlShowFiles() :
{
    SqlParserPos pos = null;
    SqlIdentifier db = null;
}
{
    <SHOW> { pos = getPos(); }
    <FILES>
    [
        (<FROM> | <IN>) { db = CompoundIdentifier(); }
    ]
    {
        return new SqlShowFiles(pos, db);
    }
}


/**
 * Parses statement SHOW {DATABASES | SCHEMAS} [LIKE 'pattern' | WHERE expr]
 */
SqlNode SqlShowSchemas() :
{
    SqlParserPos pos;
    SqlNode likePattern = null;
    SqlNode where = null;
}
{
    <SHOW> { pos = getPos(); }
    (<DATABASES> | <SCHEMAS>)
    [
        <LIKE> { likePattern = StringLiteral(); }
        |
        <WHERE> { where = Expression(ExprContext.ACCEPT_SUBQUERY); }
    ]
    {
        return new SqlShowSchemas(pos, likePattern, where);
    }
}

/**
 * Parses statement
 *   { DESCRIBE | DESC } [TABLE] tblname [col_name | wildcard ]
 */
SqlNode SqlDescribeTable() :
{
    SqlParserPos pos;
    SqlIdentifier table;
    SqlIdentifier column = null;
    SqlNode columnPattern = null;
}
{
    (<DESCRIBE> | <DESC>) { pos = getPos(); }
    (<TABLE>)?
    table = CompoundIdentifier()
    (
        column = CompoundIdentifier()
        |
        columnPattern = StringLiteral()
        |
        E()
    )
    {
        return new DrillSqlDescribeTable(pos, table, column, columnPattern);
    }
}

SqlNode SqlUseSchema():
{
    SqlIdentifier schema;
    SqlParserPos pos;
}
{
    <USE> { pos = getPos(); }
    schema = CompoundIdentifier()
    {
        return new SqlUseSchema(pos, schema);
    }
}

/** Parses an optional field list and makes sure no field is a "*". */
SqlNodeList ParseOptionalFieldList(String relType) :
{
    SqlNodeList fieldList;
}
{
    fieldList = ParseRequiredFieldList(relType)
    {
        return fieldList;
    }
    |
    {
        return SqlNodeList.EMPTY;
    }
}

/** Parses a required field list and makes sure no field is a "*". */
SqlNodeList ParseRequiredFieldList(String relType) :
{
    Pair<SqlNodeList, SqlNodeList> fieldList;
}
{
    <LPAREN>
    fieldList = ParenthesizedCompoundIdentifierList()
    <RPAREN>
    {
        for(SqlNode node : fieldList.left)
        {
            if (((SqlIdentifier) node).isStar())
                throw new ParseException(String.format("%s's field list has a '*', which is invalid.", relType));
        }
        return fieldList.left;
    }
}

/**
* Parses CREATE [OR REPLACE] command for VIEW, TABLE or SCHEMA.
*/
SqlNode SqlCreateOrReplace() :
{
    SqlParserPos pos;
    String createType = "SIMPLE";
    boolean isTemporary = false;
    boolean isPublic = false;
}
{
    <CREATE> { pos = getPos(); }
    [ <OR> <REPLACE> { createType = "OR_REPLACE"; } ]
    [ <TEMPORARY> { isTemporary = true; } ]
    (
        <VIEW>
            {
                if (isTemporary) {
                    throw new ParseException("Create view statement does not allow <TEMPORARY> keyword.");
                }
                return SqlCreateView(pos, createType);
            }
    |
        <TABLE>
            {
                if (createType == "OR_REPLACE") {
                    throw new ParseException("Create table statement does not allow <OR><REPLACE>.");
                }
                return SqlCreateTable(pos, isTemporary);

            }
    |
        <SCHEMA>
             {
                 if (isTemporary) {
                     throw new ParseException("Create schema statement does not allow <TEMPORARY> keyword.");
                 }
                 return SqlCreateSchema(pos, createType);
             }
    |
        [ <PUBLIC> { isPublic = true; } ]
        <ALIAS>
              {
                 if (isTemporary) {
                     throw new ParseException("Create alias statement does not allow <TEMPORARY> keyword.");
                 }
                 return SqlCreateAlias(pos, createType.equals("OR_REPLACE"), isPublic);
             }
    )
}

/**
 * Parses a create view or replace existing view statement.
 * after CREATE OR REPLACE VIEW statement which is handled in the SqlCreateOrReplace method.
 *
 * CREATE { [OR REPLACE] VIEW | VIEW [IF NOT EXISTS] | VIEW } view_name [ (field1, field2 ...) ] AS select_statement
 */
SqlNode SqlCreateView(SqlParserPos pos, String createType) :
{
    SqlIdentifier viewName;
    SqlNode query;
    SqlNodeList fieldList;
}
{
    [
        <IF> <NOT> <EXISTS> {
            if (createType == "OR_REPLACE") {
                throw new ParseException("Create view statement cannot have both <OR REPLACE> and <IF NOT EXISTS> clause");
            }
            createType = "IF_NOT_EXISTS";
        }
    ]
    viewName = CompoundIdentifier()
    fieldList = ParseOptionalFieldList("View")
    <AS>
    query = OrderedQueryOrExpr(ExprContext.ACCEPT_QUERY)
    {
        return new SqlCreateView(pos, viewName, fieldList, query, SqlLiteral.createCharString(createType, getPos()));
    }
}

/**
 * Parses a CTAS or CTTAS statement after CREATE [TEMPORARY] TABLE statement
 * which is handled in the SqlCreateOrReplace method.
 *
 * CREATE [TEMPORARY] TABLE [IF NOT EXISTS] tblname [ (field1, field2, ...) ] AS select_statement.
 */
SqlNode SqlCreateTable(SqlParserPos pos, boolean isTemporary) :
{
    SqlIdentifier tblName;
    SqlNodeList fieldList;
    SqlNodeList partitionFieldList;
    SqlNode query;
    boolean tableNonExistenceCheck = false;
}
{
    {
        partitionFieldList = SqlNodeList.EMPTY;
    }
    ( <IF> <NOT> <EXISTS> { tableNonExistenceCheck = true; } )?
    tblName = CompoundIdentifier()
    fieldList = ParseOptionalFieldList("Table")
    (   <PARTITION> <BY>
        partitionFieldList = ParseRequiredFieldList("Partition")
    )?
    <AS>
    query = OrderedQueryOrExpr(ExprContext.ACCEPT_QUERY)
    {
        return new SqlCreateTable(pos, tblName, fieldList, partitionFieldList, query,
                                    SqlLiteral.createBoolean(isTemporary, getPos()),
                                    SqlLiteral.createBoolean(tableNonExistenceCheck, getPos()));
    }
}

/**
* Parses create table schema statement after CREATE OR REPLACE SCHEMA statement
* which is handled in the SqlCreateOrReplace method.
*
* CREATE [OR REPLACE] SCHEMA
* [
*   LOAD 'file:///path/to/raw_schema'
* |
*   (
*     col1 int,
*     col2 varchar(10) not null
*   )
* ]
* [FOR TABLE dfs.my_table]
* [PATH 'file:///path/to/schema']
* [PROPERTIES ('prop1'='val1', 'prop2'='val2')]
*/
SqlNode SqlCreateSchema(SqlParserPos pos, String createType) :
{
    SqlCharStringLiteral schema = null;
    SqlNode load = null;
    SqlIdentifier table = null;
    SqlNode path = null;
    SqlNodeList properties = null;
}
{
    {
            token_source.pushState();
            token_source.SwitchTo(SCH);
    }
    (
        <SCH_LOAD>
        {
            load = StringLiteral();
        }
    |
        <SCH_PAREN_STRING>
        {
            schema = SqlLiteral.createCharString(token.image, getPos());
        }
    )
    (
        <FOR> <TABLE> { table = CompoundIdentifier(); }
        |
        <PATH>
        {
            path = StringLiteral();
            if (createType == "OR_REPLACE") {
                throw new ParseException("<OR REPLACE> cannot be used with <PATH> property.");
            }
        }
    )
    [
        <PROPERTIES> <LPAREN>
        {
            properties = new SqlNodeList(getPos());
            addProperty(properties);
        }
        (
            <COMMA>
            { addProperty(properties); }
        )*
        <RPAREN>
    ]
    {
        return new SqlSchema.Create(pos, schema, load, table, path, properties,
            SqlLiteral.createCharString(createType, getPos()));
    }
}

/**
* Helper method to add string literals divided by equals into SqlNodeList.
*/
void addProperty(SqlNodeList properties) :
{}
{
    { properties.add(StringLiteral()); }
    <EQ>
    { properties.add(StringLiteral()); }
}

<SCH> SKIP :
{
    " "
|   "\t"
|   "\n"
|   "\r"
}

<SCH> TOKEN : {
    < SCH_LOAD: "LOAD" > { popState(); }
  | < SCH_NUM: <DIGIT> (" " | "\t" | "\n" | "\r")* >
    // once schema is found, switch back to initial lexical state
    // must be enclosed in the parentheses
    // inside may have left parenthesis only if number precedes (covers cases with varchar(10)),
    // if left parenthesis is present in column name, it must be escaped with backslash
  | < SCH_PAREN_STRING: <LPAREN> ((~[")"]) | (<SCH_NUM> ")") | ("\\)"))* <RPAREN> > { popState(); }
}

/**
 * Parses DROP command for VIEW, TABLE and SCHEMA.
 */
SqlNode SqlDrop() :
{
    SqlParserPos pos;
}
{
    <DROP> { pos = getPos(); }
    (
        <VIEW>
        {
            return SqlDropView(pos);
        }
    |
        <TABLE>
        {
            return SqlDropTable(pos);
        }
    |
        <SCHEMA>
        {
            return SqlDropSchema(pos);
        }
    )
}

/**
 * Parses a drop view or drop view if exists statement
 * after DROP VIEW statement which is handled in SqlDrop method.
 *
 * DROP VIEW [IF EXISTS] view_name;
 */
SqlNode SqlDropView(SqlParserPos pos) :
{
    boolean viewExistenceCheck = false;
}
{
    [ <IF> <EXISTS> { viewExistenceCheck = true; } ]
    {
        return new SqlDropView(pos, CompoundIdentifier(), viewExistenceCheck);
    }
}

/**
 * Parses a drop table or drop table if exists statement
 * after DROP TABLE statement which is handled in SqlDrop method.
 *
 * DROP TABLE [IF EXISTS] table_name;
 */
SqlNode SqlDropTable(SqlParserPos pos) :
{
    boolean tableExistenceCheck = false;
}
{
    [ <IF> <EXISTS> { tableExistenceCheck = true; } ]
    {
        return new SqlDropTable(pos, CompoundIdentifier(), tableExistenceCheck);
    }
}

/**
* Parses drop schema or drop schema if exists statement
* after DROP SCHEMA statement which is handled in SqlDrop method.
*
* DROP SCHEMA [IF EXISTS]
* FOR TABLE dfs.my_table
*/
SqlNode SqlDropSchema(SqlParserPos pos) :
{
    SqlIdentifier table = null;
    boolean existenceCheck = false;
}
{
    [ <IF> <EXISTS> { existenceCheck = true; } ]
    <FOR> <TABLE> { table = CompoundIdentifier(); }
    {
        return new SqlSchema.Drop(pos, table, SqlLiteral.createBoolean(existenceCheck, getPos()));
    }
}

/**
 * Parse refresh table metadata statement.
 * REFRESH TABLE METADATA [COLUMNS ((field1, field2,..) | NONE)] table_name
 */
SqlNode SqlRefreshMetadata() :
{
    SqlParserPos pos;
    SqlIdentifier tblName;
    SqlNodeList fieldList = null;
    SqlNode query;
    boolean allColumnsInteresting = true;
}
{
    <REFRESH> { pos = getPos(); }
    <TABLE>
    <METADATA>
    [
        <COLUMNS> { allColumnsInteresting = false; }
        (   fieldList = ParseRequiredFieldList("Table")
            |
            <NONE>
        )
    ]
    tblName = CompoundIdentifier()
    {
        return new SqlRefreshMetadata(pos, tblName, SqlLiteral.createBoolean(allColumnsInteresting, getPos()), fieldList);
    }
}

/**
* Parses statement
*   { DESCRIBE | DESC } { SCHEMA | DATABASE } name
*   { DESCRIBE | DESC } SCHEMA FOR TABLE dfs.my_table [AS (JSON | STATEMENT)]
*/
SqlNode SqlDescribeSchema() :
{
   SqlParserPos pos;
   SqlIdentifier table;
   String format = "JSON";
}
{
   (<DESCRIBE> | <DESC>) { pos = getPos(); }
   (
       <SCHEMA>
         (
              <FOR> <TABLE> { table = CompoundIdentifier(); }
              [
                  <AS>
                  (
                       <JSON> { format = "JSON"; }
                  |
                       <STATEMENT> { format = "STATEMENT"; }
                  )
              ]
              {
                   return new SqlSchema.Describe(pos, table, SqlLiteral.createCharString(format, getPos()));
              }

         |
             {
                  return new SqlDescribeSchema(pos, CompoundIdentifier());
             }
         )
   |
       <DATABASE>
            {
                 return new SqlDescribeSchema(pos, CompoundIdentifier());
            }
   )
}

/**
* Parses ALTER SCHEMA statements:
*
* ALTER SCHEMA
* (FOR TABLE dfs.tmp.nation | PATH '/tmp/schema.json')
* ADD [OR REPLACE]
* [COLUMNS (col1 int, col2 varchar)]
* [PROPERTIES ('prop1'='val1', 'prop2'='val2')]
*
* ALTER SCHEMA
* (FOR TABLE dfs.tmp.nation | PATH '/tmp/schema.json')
* REMOVE
* [COLUMNS (`col1`, `col2`)]
* [PROPERTIES ('prop1', 'prop2')]
*/
SqlNode SqlAlterSchema() :
{
   SqlParserPos pos;
   SqlIdentifier table = null;
   SqlNode path = null;
}
{
   <ALTER> { pos = getPos(); }
   <SCHEMA>
    (
        <FOR> <TABLE> { table = CompoundIdentifier(); }
        |
        <PATH> { path = StringLiteral(); }
    )
    (
        <ADD> { return SqlAlterSchemaAdd(pos, table, path); }
        |
        <REMOVE> { return SqlAlterSchemaRemove(pos, table, path); }
    )
}

SqlNode SqlAlterSchemaAdd(SqlParserPos pos, SqlIdentifier table, SqlNode path) :
{
   boolean replace = false;
   SqlCharStringLiteral schema = null;
   SqlNodeList properties = null;
}
{
   [ <OR> <REPLACE> { replace = true; } ]
   [ <COLUMNS> { schema = ParseSchema(); } ]
   [
       <PROPERTIES> <LPAREN>
        {
             properties = new SqlNodeList(getPos());
             addProperty(properties);
        }
        (
             <COMMA> { addProperty(properties); }
        )*
        <RPAREN>
   ]
   {
        if (schema == null && properties == null) {
             throw new ParseException("ALTER SCHEMA ADD command must have at least <COLUMNS> or <PROPERTIES> keyword indicated.");
        }
        return new SqlSchema.Add(pos, table, path, SqlLiteral.createBoolean(replace, getPos()), schema, properties);
   }
}

SqlCharStringLiteral ParseSchema() :
{}
{
   {
        token_source.pushState();
        token_source.SwitchTo(SCH);
   }
    <SCH_PAREN_STRING>
   {
        return SqlLiteral.createCharString(token.image, getPos());
   }
}

SqlNode SqlAlterSchemaRemove(SqlParserPos pos, SqlIdentifier table, SqlNode path) :
{
   SqlNodeList columns = null;
   SqlNodeList properties = null;
}
{
    [ <COLUMNS> { columns = ParseRequiredFieldList("Schema"); } ]
    [
        <PROPERTIES> <LPAREN>
        {
            properties = new SqlNodeList(getPos());
            properties.add(StringLiteral());
        }
        (
            <COMMA>
            { properties.add(StringLiteral()); }
        )*
        <RPAREN>
    ]
    {
         if (columns == null && properties == null) {
             throw new ParseException("ALTER SCHEMA REMOVE command must have at least <COLUMNS> or <PROPERTIES> keyword indicated.");
         }
         return new SqlSchema.Remove(pos, table, path, columns, properties);
    }
}

/**
* Parse create UDF statement
* CREATE FUNCTION USING JAR 'jar_name'
*/
SqlNode SqlCreateFunction() :
{
   SqlParserPos pos;
   SqlNode jar;
}
{
   <CREATE> { pos = getPos(); }
   <FUNCTION>
   <USING>
   <JAR>
   jar = StringLiteral()
   {
       return new SqlCreateFunction(pos, jar);
   }
}

/**
* Parse drop UDF statement
* DROP FUNCTION USING JAR 'jar_name'
*/
SqlNode SqlDropFunction() :
{
   SqlParserPos pos;
   SqlNode jar;
}
{
   <DROP> { pos = getPos(); }
   <FUNCTION>
   <USING>
   <JAR>
   jar = StringLiteral()
   {
       return new SqlDropFunction(pos, jar);
   }
}

<#if !parser.includeCompoundIdentifier >
/**
* Parses a comma-separated list of simple identifiers.
*/
Pair<SqlNodeList, SqlNodeList> ParenthesizedCompoundIdentifierList() :
{
    List<SqlIdentifier> list = new ArrayList<SqlIdentifier>();
    SqlIdentifier id;
}
{
    id = CompoundIdentifier() {list.add(id);}
    (
   <COMMA> id = CompoundIdentifier() {list.add(id);}) *
    {
       return Pair.of(new SqlNodeList(list, getPos()), null);
    }
}
</#if>

/**
 * Parses a analyze statements:
 * <ul>
 * <li>ANALYZE TABLE [table_name | table({table function name}(parameters))] [COLUMNS {(col1, col2, ...) | NONE}] REFRESH METADATA ['level' LEVEL] [{COMPUTE | ESTIMATE} | STATISTICS [ SAMPLE number PERCENT ]]
 * <li>ANALYZE TABLE [table_name] DROP [METADATA|STATISTICS] [IF EXISTS]
 * <li>ANALYZE TABLE [table_name | table({table function name}(parameters))] {COMPUTE | ESTIMATE} | STATISTICS [(column1, column2, ...)] [ SAMPLE numeric PERCENT ]
 * </ul>
 */
SqlNode SqlAnalyzeTable() :
{
    SqlParserPos pos;
    SqlNode tableRef;
    Span s = null;
    SqlNodeList fieldList = null;
    SqlNode level = null;
    SqlLiteral estimate = null;
    SqlLiteral dropMetadata = null;
    SqlLiteral checkMetadataExistence = null;
    SqlNumericLiteral percent = null;
}
{
    <ANALYZE> { pos = getPos(); }
    <TABLE>
    (
        tableRef = CompoundIdentifier()
    |
        <TABLE> { s = span(); } <LPAREN>
        tableRef = TableFunctionCall(s.pos())
        <RPAREN>
    )
    [
        (
            (
                <COMPUTE> { estimate = SqlLiteral.createBoolean(false, pos); }
                |
                <ESTIMATE> {
                  if (true) {
                    throw new ParseException("ESTIMATE statistics collecting is not supported. See DRILL-7438.");
                  }
                  estimate = SqlLiteral.createBoolean(true, pos);
                }
            )
            <STATISTICS>
            [
                (fieldList = ParseRequiredFieldList("Table"))
            ]
            [
                <SAMPLE> percent = UnsignedNumericLiteral() <PERCENT>
                {
                    BigDecimal rate = percent.bigDecimalValue();
                    if (rate.compareTo(BigDecimal.ZERO) <= 0 ||
                        rate.compareTo(BigDecimal.valueOf(100L)) > 0)
                    {
                        throw new ParseException("Invalid percentage for ANALYZE TABLE");
                    }
                }
            ]
            {
                if (percent == null) { percent = SqlLiteral.createExactNumeric("100.0", pos); }
                return new SqlAnalyzeTable(pos, tableRef, estimate, fieldList, percent);
            }
        )
        |
        (
            [
                <COLUMNS>
                (
                    fieldList = ParseRequiredFieldList("Table")
                    |
                    <NONE> {fieldList = SqlNodeList.EMPTY;}
                )
            ]
            <REFRESH>
            <METADATA>
            [
                level = StringLiteral()
                <LEVEL>
            ]
            [
                (
                    <COMPUTE> { estimate = SqlLiteral.createBoolean(false, pos); }
                    |
                    <ESTIMATE> {
                      if (true) {
                        throw new ParseException("ESTIMATE statistics collecting is not supported. See DRILL-7438.");
                      }
                      estimate = SqlLiteral.createBoolean(true, pos);
                    }
                )
                <STATISTICS>
            ]
            [
                <SAMPLE> percent = UnsignedNumericLiteral() <PERCENT>
                {
                    BigDecimal rate = percent.bigDecimalValue();
                    if (rate.compareTo(BigDecimal.ZERO) <= 0 ||
                        rate.compareTo(BigDecimal.valueOf(100L)) > 0) {
                      throw new ParseException("Invalid percentage for ANALYZE TABLE");
                    }
                }
            ]
            {
                return new SqlMetastoreAnalyzeTable(pos, tableRef, fieldList, level, estimate, percent);
            }
        )
        |
        (
            <DROP>
            [
                <METADATA> { dropMetadata = SqlLiteral.createCharString("METADATA", pos); }
                |
                <STATISTICS> {
                  if (true) {
                    throw new ParseException("DROP STATISTICS is not supported.");
                  }
                  dropMetadata = SqlLiteral.createCharString("STATISTICS", pos);
                }
            ]
            [
                <IF>
                <EXISTS> { checkMetadataExistence = SqlLiteral.createBoolean(false, pos); }
            ]
            {
                if (checkMetadataExistence == null) {
                  checkMetadataExistence = SqlLiteral.createBoolean(true, pos);
                }
                if (s != null) {
                  throw new ParseException("Table functions shouldn't be used in DROP METADATA statement.");
                }
                return new SqlDropTableMetadata(pos, (SqlIdentifier) tableRef, dropMetadata, checkMetadataExistence);
            }
        )
    ]
    { throw generateParseException(); }
}


/**
 * Parses a SET statement without a leading "ALTER <SCOPE>":
 *
 * SET &lt;NAME&gt; [ = VALUE ]
 * <p>
 * Statement handles in: {@link SetAndResetOptionHandler}
 */
DrillSqlSetOption DrillSqlSetOption(Span s, String scope) :
{
    SqlParserPos pos;
    SqlIdentifier name;
    SqlNode val = null;
}
{
    <SET> {
        s.add(this);
    }
    name = CompoundIdentifier()
    (
        <EQ>
        (
            val = Literal()
        |
            val = SimpleIdentifier()
        )
    )?
    {
      pos = (val == null) ? s.end(name) : s.end(val);

      return new DrillSqlSetOption(pos, scope, name, val);
    }
}

/**
 * Parses a RESET statement without a leading "ALTER <SCOPE>":
 *
 *  RESET { <NAME> | ALL }
 * <p>
 * Statement handles in: {@link SetAndResetOptionHandler}
 */
DrillSqlResetOption DrillSqlResetOption(Span s, String scope) :
{
    SqlIdentifier name;
}
{
    <RESET> {
        s.add(this);
    }
    (
        name = CompoundIdentifier()
    |
        <ALL> {
            name = new SqlIdentifier(token.image.toUpperCase(Locale.ROOT), getPos());
        }
    )
    {
        return new DrillSqlResetOption(s.end(name), scope, name);
    }
}

/**
 * Parses CREATE ALIAS statement
 * CREATE [OR REPLACE] [PUBLIC] ALIAS `alias` FOR [TABLE | STORAGE] `table/storage` [AS USER 'username']
 */
SqlNode SqlCreateAlias(SqlParserPos pos, boolean replace, boolean isPublic) :
{
   SqlIdentifier alias = null;
   String aliasTarget = "TABLE";
   SqlIdentifier source = null;
   SqlNode user = null;
}
{
   { alias = CompoundIdentifier(); }
   <FOR>
   [
        <TABLE> { aliasTarget = "TABLE"; }
   |
        <STORAGE> { aliasTarget = "STORAGE"; }
   ]
   { source = CompoundIdentifier(); }
   [ <AS> <USER> { user = StringLiteral(); } ]
   {
       return SqlCreateAlias.builder()
              .pos(pos)
              .alias(alias)
              .source(source)
              .aliasKind(SqlLiteral.createCharString(aliasTarget, pos))
              .replace(SqlLiteral.createBoolean(replace, pos))
              .isPublic(SqlLiteral.createBoolean(isPublic, pos))
              .user(user)
              .build();
   }
}

/**
 * Parses DROP ALIAS statement
 * DROP [PUBLIC] ALIAS [IF EXISTS] `employee-alias` [FOR (TABLE | STORAGE)] [AS USER 'username']
 */
SqlNode SqlDropAlias() :
{
   SqlParserPos pos;
   boolean isPublic = false;
   boolean ifExists = false;
   SqlIdentifier alias = null;
   String aliasTarget = "TABLE";
   SqlNode user = null;
}
{
   <DROP> { pos = getPos(); }
   [ <PUBLIC> { isPublic = true; } ]
   <ALIAS>
   [ <IF> <EXISTS> { ifExists = true; } ]
   { alias = CompoundIdentifier(); }
   [ <FOR>
        (
             <TABLE> { aliasTarget = "TABLE"; }
        |
             <STORAGE> { aliasTarget = "STORAGE"; }
        )
   ]
   [ <AS> <USER> { user = StringLiteral(); } ]
   {
       return SqlDropAlias.builder()
              .pos(pos)
              .alias(alias)
              .aliasKind(SqlLiteral.createCharString(aliasTarget, pos))
              .ifExists(SqlLiteral.createBoolean(ifExists, pos))
              .isPublic(SqlLiteral.createBoolean(isPublic, pos))
              .user(user)
              .build();
   }
}

/**
 * Parses DROP ALL ALIASES statement
 * DROP ALL [PUBLIC] ALIASES [FOR (TABLE | STORAGE)] [AS USER 'username']
 */
SqlNode SqlDropAllAliases() :
{
   SqlParserPos pos;
   boolean isPublic = false;
   String aliasTarget = "TABLE";
   SqlNode user = null;
}
{
   <DROP> { pos = getPos(); }
   <ALL>
   [ <PUBLIC> { isPublic = true; } ]
   <ALIASES>
   [ <FOR>
        (
             <TABLE> { aliasTarget = "TABLE"; }
        |
             <STORAGE> { aliasTarget = "STORAGE"; }
        )
   ]
   [ <AS> <USER> { user = StringLiteral(); } ]
   {
       return SqlDropAllAliases.builder()
              .pos(pos)
              .aliasKind(SqlLiteral.createCharString(aliasTarget, pos))
              .isPublic(SqlLiteral.createBoolean(isPublic, pos))
              .user(user)
              .build();
   }
}
