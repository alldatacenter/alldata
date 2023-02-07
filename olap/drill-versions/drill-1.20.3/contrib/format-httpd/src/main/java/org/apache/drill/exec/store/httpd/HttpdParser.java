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
package org.apache.drill.exec.store.httpd;

import nl.basjes.parse.useragent.analyze.InvalidParserConfigurationException;
import nl.basjes.parse.useragent.dissector.UserAgentDissector;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.resultSet.RowSetLoader;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.store.dfs.easy.EasySubScan;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import nl.basjes.parse.core.Casts;
import nl.basjes.parse.core.Parser;
import nl.basjes.parse.core.exceptions.DissectionFailure;
import nl.basjes.parse.core.exceptions.InvalidDissectorException;
import nl.basjes.parse.core.exceptions.MissingDissectorsException;
import nl.basjes.parse.httpdlog.HttpdLoglineParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static nl.basjes.parse.core.Casts.DOUBLE;
import static nl.basjes.parse.core.Casts.DOUBLE_ONLY;
import static nl.basjes.parse.core.Casts.LONG;
import static nl.basjes.parse.core.Casts.LONG_ONLY;
import static nl.basjes.parse.core.Casts.STRING;
import static nl.basjes.parse.core.Casts.STRING_ONLY;

public class HttpdParser {

  private static final Logger logger = LoggerFactory.getLogger(HttpdParser.class);

  public static final String PARSER_WILDCARD = ".*";
  private final Parser<HttpdLogRecord> parser;
  private final List<SchemaPath> requestedColumns;
  private final Map<String, MinorType> mappedColumns;
  private final Map<String, Casts> columnCasts;
  private final HttpdLogRecord record;
  private final String logFormat;
  private final boolean parseUserAgent;
  private final String logParserRemapping;
  private Map<String, String> requestedPaths;

  public HttpdParser(
          final String logFormat,
          final String timestampFormat,
          final boolean flattenWildcards,
          final boolean parseUserAgent,
          final String logParserRemapping,
          final EasySubScan scan) {

    Preconditions.checkArgument(logFormat != null && !logFormat.trim().isEmpty(), "logFormat cannot be null or empty");

    this.logFormat = logFormat;
    this.parseUserAgent = parseUserAgent;
    this.record = new HttpdLogRecord(timestampFormat, flattenWildcards);

    this.logParserRemapping = logParserRemapping;

    this.parser = new HttpdLoglineParser<>(HttpdLogRecord.class, this.logFormat, timestampFormat);
    applyRemapping(parser);
    /*
     * The log parser has the possibility of parsing the user agent and extracting additional fields
     * Unfortunately, doing so negatively affects the startup speed of the parser, even if it is not used.
     * So is is only enabled if there is a need for it in the requested columns.
     */
    if (parseUserAgent) {
      parser.addDissector(new UserAgentDissector());
    }


    this.requestedColumns = scan.getColumns();

    if (timestampFormat != null && !timestampFormat.trim().isEmpty()) {
      logger.info("Custom timestamp format has been specified. This is an informational note only as custom timestamps is rather unusual.");
    }
    if (logFormat.contains("\n")) {
      logger.info("Specified logformat is a multiline log format: {}", logFormat);
    }

    mappedColumns = new TreeMap<>();
    columnCasts = new TreeMap<>();
  }

  private void applyRemapping(Parser<?> parser) {
    if (logParserRemapping == null || logParserRemapping.isEmpty()) {
      return;
    }

    for (String rawEntry: logParserRemapping.split(";")) {
      String entry = rawEntry.replaceAll("\n","").replaceAll(" ","").trim();
      if (entry.isEmpty()) {
        continue;
      }

      String[] parts = entry.split(":");
      String field = parts[0];
      String newType = parts[1];
      String castString = parts.length == 3 ? parts[2] : "STRING";

      switch (castString) {
        case "STRING":
          parser.addTypeRemapping(field, newType, STRING_ONLY);
          break;
        case "LONG":
          parser.addTypeRemapping(field, newType, LONG_ONLY);
          break;
        case "DOUBLE":
          parser.addTypeRemapping(field, newType, DOUBLE_ONLY);
          break;
        default:
          throw new InvalidParserConfigurationException("Invalid type remapping cast was specified");
      }
    }
  }

  /**
   * We do not expose the underlying parser or the record which is used to manage the writers.
   *
   * @param line log line to tear apart.
   * @throws DissectionFailure if there is a generic dissector failure
   * @throws InvalidDissectorException if the dissector is not valid
   * @throws MissingDissectorsException if the dissector is missing
   */
  public void parse(final String line) throws DissectionFailure, InvalidDissectorException, MissingDissectorsException {
    parser.parse(record, line);
    record.finishRecord();
  }

  public TupleMetadata setupParser()
          throws NoSuchMethodException, MissingDissectorsException, InvalidDissectorException {

    SchemaBuilder builder = new SchemaBuilder();

    /*
     * If the user has selected fields, then we will use them to configure the parser because this would be the most
     * efficient way to parse the log.
     */
    List<String> allParserPaths = parser.getPossiblePaths();
    allParserPaths.sort(String::compareTo);

    /*
     * Use all possible paths that the parser has determined from the specified log format.
     */

    // Create a mapping table to each allParserPaths field from their corresponding Drill column name.
    requestedPaths = new TreeMap<>(); // Treemap to have a stable ordering!
    for (final String parserPath : allParserPaths) {
      requestedPaths.put(HttpdUtils.drillFormattedFieldName(parserPath), parserPath);
    }

    /*
     * By adding the parse target to the dummy instance we activate it for use. Which we can then use to find out which
     * paths cast to which native data types. After we are done figuring this information out, we throw this away
     * because this will be the slowest parsing path possible for the specified format.
     */
    Parser<Object> dummy = new HttpdLoglineParser<>(Object.class, logFormat);
    applyRemapping(dummy);

    if (parseUserAgent) {
      dummy.addDissector(new UserAgentDissector());
    }

    dummy.addParseTarget(String.class.getMethod("indexOf", String.class), allParserPaths);

    /*
    If the column is not requested explicitly, remove it from the requested path list.
     */
    if (!isStarQuery() &&
        !isMetadataQuery() &&
        !isOnlyImplicitColumns()) {
      requestedPaths = getRequestedColumnPaths();
    }

    EnumSet<Casts> allCasts;
    for (final Map.Entry<String, String> entry : requestedPaths.entrySet()) {
      allCasts = dummy.getCasts(entry.getValue());

      // Select the cast we want to receive from the parser
      Casts dataType = STRING;
      if (allCasts.contains(DOUBLE)) {
        dataType = DOUBLE;
      } else if (allCasts.contains(LONG)) {
        dataType = LONG;
      }

      columnCasts.put(entry.getKey(), dataType);

      switch (dataType) {
        case STRING:
          if (entry.getValue().startsWith("TIME.STAMP:")) {
            builder.addNullable(entry.getKey(), MinorType.TIMESTAMP);
            mappedColumns.put(entry.getKey(), MinorType.TIMESTAMP);
          } else if (entry.getValue().startsWith("TIME.DATE:")) {
            builder.addNullable(entry.getKey(), MinorType.DATE);
            mappedColumns.put(entry.getKey(), MinorType.DATE);
          } else if (entry.getValue().startsWith("TIME.TIME:")) {
            builder.addNullable(entry.getKey(), MinorType.TIME);
            mappedColumns.put(entry.getKey(), MinorType.TIME);
          } else if (HttpdUtils.isWildcard(entry.getValue())) {
            builder.addMap(entry.getValue());
            mappedColumns.put(entry.getKey(), MinorType.MAP);
          }
          else {
            builder.addNullable(entry.getKey(), TypeProtos.MinorType.VARCHAR);
            mappedColumns.put(entry.getKey(), MinorType.VARCHAR);
          }
          break;
        case LONG:
          if (entry.getValue().startsWith("TIME.EPOCH:")) {
            builder.addNullable(entry.getKey(), MinorType.TIMESTAMP);
            mappedColumns.put(entry.getKey(), MinorType.TIMESTAMP);
          } else {
            builder.addNullable(entry.getKey(), TypeProtos.MinorType.BIGINT);
            mappedColumns.put(entry.getKey(), MinorType.BIGINT);
          }
          break;
        case DOUBLE:
          builder.addNullable(entry.getKey(), TypeProtos.MinorType.FLOAT8);
          mappedColumns.put(entry.getKey(), MinorType.FLOAT8);
          break;
        default:
          logger.error("HTTPD Unsupported data type {} for field {}", dataType.toString(), entry.getKey());
          break;
      }
    }
    return builder.build();
  }

  private Map<String, String> getRequestedColumnPaths() {
    Map<String, String> requestedColumnPaths = new TreeMap<>();
    for (SchemaPath requestedColumn : requestedColumns) {
      String columnName = requestedColumn.getRootSegmentPath();
      String parserPath = requestedPaths.get(columnName);
      if (parserPath != null) {
        requestedColumnPaths.put(columnName, parserPath);
      } else {
        requestedPaths.keySet()
          .stream()
          .filter(colName -> colName.endsWith(HttpdUtils.SAFE_WILDCARD)
            && requestedColumn.rootName().startsWith(colName.substring(0, colName.length() - HttpdUtils.SAFE_WILDCARD.length())))
          .findAny()
          .ifPresent(colName -> requestedColumnPaths.put(colName, requestedPaths.get(colName)));
      }
    }
    return requestedColumnPaths;
  }

  public void addFieldsToParser(RowSetLoader rowWriter) {
    for (final Map.Entry<String, String> entry : requestedPaths.entrySet()) {
      try {
        record.addField(parser, rowWriter, columnCasts, entry.getValue(), entry.getKey(), mappedColumns);
      } catch (NoSuchMethodException e) {
        logger.error("Error adding fields to parser.");
      }
    }
    logger.debug("Added Fields to Parser");
  }

  public boolean isStarQuery() {
    return requestedColumns.stream()
      .anyMatch(SchemaPath::isDynamicStar);
  }

  public boolean isMetadataQuery() {
    return requestedColumns.size() == 0;
  }

  /*
  This is for the edge case where a query only contains the implicit fields.
   */
  public boolean isOnlyImplicitColumns() {

    // If there are more than two columns, this isn't an issue.
    if (requestedColumns.size() > 2) {
      return false;
    }

    if (requestedColumns.size() == 1) {
      return requestedColumns.get(0).nameEquals(HttpdLogBatchReader.RAW_LINE_COL_NAME) ||
        requestedColumns.get(0).nameEquals(HttpdLogBatchReader.MATCHED_COL_NAME);
    } else {
      return (requestedColumns.get(0).nameEquals(HttpdLogBatchReader.RAW_LINE_COL_NAME) ||
        requestedColumns.get(0).nameEquals(HttpdLogBatchReader.MATCHED_COL_NAME)) &&
        (requestedColumns.get(1).nameEquals(HttpdLogBatchReader.RAW_LINE_COL_NAME) ||
        requestedColumns.get(1).nameEquals(HttpdLogBatchReader.MATCHED_COL_NAME));
    }
  }
}
