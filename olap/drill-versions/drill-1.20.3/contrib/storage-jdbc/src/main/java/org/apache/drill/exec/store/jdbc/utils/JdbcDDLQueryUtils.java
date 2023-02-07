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

package org.apache.drill.exec.store.jdbc.utils;

import org.apache.calcite.config.Lex;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql.util.SqlBuilder;
import org.apache.calcite.sql.validate.SqlConformanceEnum;
import org.apache.calcite.sql.parser.ddl.SqlDdlParserImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JdbcDDLQueryUtils {

  private static final Logger logger = LoggerFactory.getLogger(JdbcDDLQueryUtils.class);
  /**
   * Converts a given SQL query from the generic dialect to the destination system dialect.  Returns
   * null if the original query is not valid.  This should only be used for CTAS queries.
   *
   * @param query An ANSI SQL statement (CTAS Only)
   * @param dialect The destination system dialect
   * @return A representation of the original query in the destination dialect
   */
  public static String cleanDDLQuery(String query, SqlDialect dialect) {
    SqlParser.Config sqlParserConfig = SqlParser.configBuilder()
      .setParserFactory(SqlDdlParserImpl.FACTORY)
      .setConformance(SqlConformanceEnum.STRICT_2003)
      .setCaseSensitive(true)
      .setLex(Lex.MYSQL)
      .build();

    try {
      SqlNode node = SqlParser.create(query, sqlParserConfig).parseQuery();

      return node.toSqlString(dialect).getSql();
    } catch (SqlParseException e) {
      logger.error(e.getMessage());
      return null;
    }
  }

  /**
   * This function adds backticks around table names.  If the table name already has backticks,
   * it does nothing.
   * @param inputTable The table name with or without backticks
   * @return The table name with backticks added.
   */
  public static String addBackTicksToTable(String inputTable) {
    String[] queryParts = inputTable.split("\\.");
    StringBuilder cleanQuery = new StringBuilder();

    int counter = 0;
    for (String part : queryParts) {
      if (counter > 0) {
        cleanQuery.append(".");
      }

      if (part.startsWith("`") && part.endsWith("`")) {
        cleanQuery.append(part);
      } else {
        cleanQuery.append("`").append(part).append("`");
      }
      counter++;
    }

    return cleanQuery.toString();
  }

  public static void addTableToInsertQuery(SqlBuilder builder, String rawEscapedTables) {
    final String TABLE_REGEX = "(?:`(.+?)`)+";
    Pattern pattern = Pattern.compile(TABLE_REGEX);
    Matcher matcher = pattern.matcher(rawEscapedTables);

    int matchCount = 0;
    while (matcher.find()) {
      if (matchCount > 0) {
        builder.append(".");
      }
      builder.identifier(matcher.group(1));
      matchCount++;
    }
  }

  public static String addBackTicksToField(String field) {
    if (field.startsWith("`") && field.endsWith("`")) {
      return field;
    } else {
      StringBuilder cleanField = new StringBuilder();
      return cleanField.append("`").append(field).append("`").toString();
    }
  }
}
