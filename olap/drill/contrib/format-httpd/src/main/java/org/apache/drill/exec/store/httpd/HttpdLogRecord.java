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

import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.resultSet.RowSetLoader;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.MetadataUtils;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.vector.accessor.ScalarWriter;
import org.apache.drill.exec.vector.accessor.TupleWriter;
import org.apache.drill.shaded.guava.com.google.common.collect.Maps;

import java.util.HashMap;
import java.util.Map;

import nl.basjes.parse.core.Casts;
import nl.basjes.parse.core.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;

public class HttpdLogRecord {

  private static final Logger logger = LoggerFactory.getLogger(HttpdLogRecord.class);

  private final Map<String, ScalarWriter> strings = Maps.newHashMap();
  private final Map<String, ScalarWriter> longs = Maps.newHashMap();
  private final Map<String, ScalarWriter> doubles = Maps.newHashMap();
  private final Map<String, ScalarWriter> dates = Maps.newHashMap();
  private final Map<String, ScalarWriter> times = Maps.newHashMap();
  private final Map<String, ScalarWriter> timestamps = new HashMap<>();
  private final Map<String, TupleWriter> wildcards = Maps.newHashMap();
  private final Map<String, String> cleanExtensions = Maps.newHashMap();
  private final Map<String, TupleWriter> startedWildcards = Maps.newHashMap();
  private final Map<String, TupleWriter> wildcardWriters = Maps.newHashMap();
  private final SimpleDateFormat dateFormatter;
  private RowSetLoader rootRowWriter;
  private final boolean flattenWildcards;

  public HttpdLogRecord(String timeFormat, boolean flattenWildcards) {
    if (timeFormat == null) {
      timeFormat = HttpdLogFormatConfig.DEFAULT_TS_FORMAT;
    }
    this.dateFormatter = new SimpleDateFormat(timeFormat);
    this.flattenWildcards = flattenWildcards;
  }

  /**
   * Call this method after a record has been parsed. This finished the lifecycle of any maps that were written and
   * removes all the entries for the next record to be able to work.
   */
  public void finishRecord() {
    wildcardWriters.clear();
    startedWildcards.clear();
  }

  /**
   * This method is referenced and called via reflection. This is added as a parsing target for the parser. It will get
   * called when the value of a log field is a String data type.
   *
   * @param field name of field
   * @param value value of field
   */
  @SuppressWarnings("unused")
  public void set(String field, String value) {
    if (value != null) {
      final ScalarWriter w = strings.get(field);
      if (w != null) {
        logger.debug("Parsed field: {}, as string: {}", field, value);
        w.setString(value);
      } else {
        logger.warn("No 'string' writer found for field: {}", field);
      }
    }
  }

  /**
   * This method is referenced and called via reflection. This is added as a parsing target for the parser. It will get
   * called when the value of a log field is a Long data type.
   *
   * @param field name of field
   * @param value value of field
   */
  @SuppressWarnings("unused")
  public void set(String field, Long value) {
    if (value != null) {
      final ScalarWriter w = longs.get(field);
      if (w != null) {
        logger.debug("Parsed field: {}, as long: {}", field, value);
        w.setLong(value);
      } else {
        logger.warn("No 'long' writer found for field: {}", field);
      }
    }
  }

  /**
   * This method is referenced and called via reflection. This is added as a parsing target for the parser. It will get
   * called when the value of a log field is a Date data type.
   *
   * @param field name of field
   * @param value value of field
   */
  @SuppressWarnings("unused")
  public void setDate(String field, String value) {
    if (value != null) {
      final ScalarWriter w = dates.get(field);
      if (w != null) {
        logger.debug("Parsed field: {}, as long: {}", field, value);
        w.setDate(LocalDate.parse(value));
      } else {
        logger.warn("No 'date' writer found for field: {}", field);
      }
    }
  }

  /**
   * This method is referenced and called via reflection. This is added as a parsing target for the parser. It will get
   * called when the value of a log field is a Time data type.
   *
   * @param field name of field
   * @param value value of field
   */
  @SuppressWarnings("unused")
  public void setTime(String field, String value) {
    if (value != null) {
      final ScalarWriter w = times.get(field);
      if (w != null) {
        logger.debug("Parsed field: {}, as long: {}", field, value);
        w.setTime(LocalTime.parse(value));
      } else {
        logger.warn("No 'date' writer found for field: {}", field);
      }
    }
  }

  /**
   * This method is referenced and called via reflection. This is added as a parsing target for the parser. It will get
   * called when the value of a log field is a timestamp data type.
   *
   * @param field name of field
   * @param value value of field
   */
  @SuppressWarnings("unused")
  public void setTimestampFromEpoch(String field, Long value) {
    if (value != null) {
      final ScalarWriter w = timestamps.get(field);
      if (w != null) {
        logger.debug("Parsed field: {}, as timestamp: {}", field, value);
        w.setTimestamp(Instant.ofEpochMilli(value));
      } else {
        logger.warn("No 'timestamp' writer found for field: {}", field);
      }
    }
  }

  /**
   * This method is referenced and called via reflection. This is added as a parsing target for the parser. It will get
   * called when the value of a log field is a timestamp data type.
   *
   * @param field name of field
   * @param value value of field
   */
  @SuppressWarnings("unused")
  public void setTimestamp(String field, String value) {
    if (value != null) {
      //Convert the date string into a long
      long ts = 0;
      try {
        Date d = this.dateFormatter.parse(value);
        ts = d.getTime();
      } catch (Exception e) {
        //If the date formatter does not successfully create a date, the timestamp will fall back to zero
        //Do not throw exception
      }
      final ScalarWriter tw = timestamps.get(field);
      if (tw != null) {
        logger.debug("Parsed field: {}, as time: {}", field, value);
        tw.setTimestamp(Instant.ofEpochMilli(ts));
      } else {
        logger.warn("No 'timestamp' writer found for field: {}", field);
      }
    }
  }

  /**
   * This method is referenced and called via reflection. This is added as a parsing target for the parser. It will get
   * called when the value of a log field is a Double data type.
   *
   * @param field name of field
   * @param value value of field
   */
  @SuppressWarnings("unused")
  public void set(String field, Double value) {
    if (value != null) {
      final ScalarWriter w = doubles.get(field);
      if (w != null) {
        logger.debug("Parsed field: {}, as double: {}", field, value);
        w.setDouble(value);
      } else {
        logger.warn("No 'double' writer found for field: {}", field);
      }
    }
  }

  /**
   * This method is referenced and called via reflection. When the parser processes a field like:
   * HTTP.URI:request.firstline.uri.query.* where star is an arbitrary field that the parser found this method will be
   * invoked. <br>
   *
   * @param field name of field
   * @param value value of field
   */
  @SuppressWarnings("unused")
  public void setWildcard(String field, String value) {
    if (value != null) {
      String cleanedField = HttpdUtils.getFieldNameFromMap(field);
      if (flattenWildcards) {
        String drillFieldName = HttpdUtils.drillFormattedFieldName(field);
        ScalarWriter writer = getColWriter(rootRowWriter, drillFieldName, MinorType.VARCHAR);
        writer.setString(value);
      } else {
        final TupleWriter mapWriter = getWildcardWriter(field);
        logger.debug("Parsed wildcard field: {}, as String: {}", field, value);
        writeStringColumn(mapWriter, cleanedField, value);
      }
    }
  }

  /**
   * This method is referenced and called via reflection. When the parser processes a field like:
   * HTTP.URI:request.firstline.uri.query.* where star is an arbitrary field that the parser found this method will be
   * invoked. <br>
   *
   * @param field name of field
   * @param value value of field
   */
  @SuppressWarnings("unused")
  public void setWildcard(String field, Long value) {
    if (value != null) {
      String cleanedField = HttpdUtils.getFieldNameFromMap(field);

      if (flattenWildcards) {
        String drillFieldName = HttpdUtils.drillFormattedFieldName(field);
        ScalarWriter writer = getColWriter(rootRowWriter, drillFieldName, MinorType.BIGINT);
        writer.setLong(value);
      } else {
        final TupleWriter mapWriter = getWildcardWriter(field);
        logger.debug("Parsed wildcard field: {}, as long: {}", field, value);
        writeLongColumn(mapWriter, cleanedField, value);
      }
    }
  }

  /**
   * This method is referenced and called via reflection. When the parser processes a field like:
   * HTTP.URI:request.firstline.uri.query.* where star is an arbitrary field that the parser found this method will be
   * invoked. <br>
   *
   * @param field name of field
   * @param value value of field
   */
  @SuppressWarnings("unused")
  public void setWildcard(String field, Double value) {
    if (value != null) {
      String cleanedField = HttpdUtils.getFieldNameFromMap(field);

      if (flattenWildcards) {
        String drillFieldName = HttpdUtils.drillFormattedFieldName(field);
        ScalarWriter writer = getColWriter(rootRowWriter, drillFieldName, MinorType.FLOAT8);
        writer.setDouble(value);
      } else {
        final TupleWriter mapWriter = getWildcardWriter(field);
        logger.debug("Parsed wildcard field: {}, as double: {}", field, value);
        writeFloatColumn(mapWriter, cleanedField, value);
      }
    }
  }

  /**
   * For a configuration like HTTP.URI:request.firstline.uri.query.*, a writer was created with name
   * HTTP.URI:request.firstline.uri.query, we traverse the list of wildcard writers to see which one is the root of the
   * name of the field passed in like HTTP.URI:request.firstline.uri.query.old. This is writer entry that is needed.
   *
   * @param field like HTTP.URI:request.firstline.uri.query.old where 'old' is one of many different parameter names.
   * @return the writer to be used for this field.
   */
  private TupleWriter getWildcardWriter(String field) {

    TupleWriter writer = startedWildcards.get(field);
    if (writer == null) {
      for (Map.Entry<String, TupleWriter> entry : wildcards.entrySet()) {
        String root = entry.getKey();
        if (field.startsWith(root)) {
          writer = entry.getValue();
          /*
           * In order to save some time, store the cleaned version of the field extension. It is possible it will have
           * unsafe characters in it.
           */
          if (!cleanExtensions.containsKey(field)) {
            String extension = field.substring(root.length() + 1);
            String cleanExtension = HttpdUtils.drillFormattedFieldName(extension);
            cleanExtensions.put(field, cleanExtension);
            logger.debug("Added extension: field='{}' with cleanExtension='{}'", field, cleanExtension);
          }

          /*
           * We already know we have the writer, but if we have put this writer in the started list, do NOT call start
           * again.
           */
          if (!wildcardWriters.containsKey(root)) {
            /*
             * Start and store this root map writer for later retrieval.
             */
            logger.debug("Starting new wildcard field writer: {}", field);
            startedWildcards.put(field, writer);
            wildcardWriters.put(root, writer);
          }
          /*
           * Break out of the for loop when we find a root writer that matches the field.
           */
          break;
        }
      }
    }

    return writer;
  }

  public Map<String, ScalarWriter> getStrings() {
    return strings;
  }

  public Map<String, ScalarWriter> getLongs() {
    return longs;
  }

  public Map<String, ScalarWriter> getDoubles() {
    return doubles;
  }

  public Map<String, ScalarWriter> getTimestamps() {
    return timestamps;
  }

  /**
   * This record will be used with a single parser. For each field that is to be parsed a setter will be called. It
   * registers a setter method for each field being parsed. It also builds the data writers to hold the data beings
   * parsed.
   *
   * @param parser The initialized HttpdParser
   * @param rowWriter An initialized RowSetLoader object
   * @param columnCasts The logparser casts used to get the right data from the parser
   * @param parserFieldName The field name which is generated by the Httpd Parser.  These are not "Drill safe"
   * @param drillFieldName The Drill safe field name
   * @param mappedColumns A list of columns mapped to their correct Drill data type
   * @throws NoSuchMethodException Thrown in the event that the parser does not have a correct setter method
   */
  public void addField(final Parser<HttpdLogRecord> parser,
                       final RowSetLoader rowWriter,
                       final Map<String, Casts> columnCasts,
                       final String parserFieldName,
                       final String drillFieldName,
                       Map<String, MinorType> mappedColumns) throws NoSuchMethodException {
    final boolean hasWildcard = parserFieldName.endsWith(HttpdParser.PARSER_WILDCARD);

    final Casts type = columnCasts.getOrDefault(drillFieldName, Casts.STRING);

    logger.debug("Field name: {}", parserFieldName);
    rootRowWriter = rowWriter;
    /*
     * This is a dynamic way to map the setter for each specified field type. <br/>
     * e.g. a TIME.EPOCH may map to a LONG while a referrer may map to a STRING
     */
    if (hasWildcard) {
      final String cleanName = parserFieldName.substring(0, parserFieldName.length() - HttpdParser.PARSER_WILDCARD.length());
      logger.debug("Adding WILDCARD parse target: {} as {}, with field name: {}", parserFieldName, cleanName, drillFieldName);
      parser.addParseTarget(this.getClass().getMethod("setWildcard", String.class, String.class), parserFieldName);
      parser.addParseTarget(this.getClass().getMethod("setWildcard", String.class, Double.class), parserFieldName);
      parser.addParseTarget(this.getClass().getMethod("setWildcard", String.class, Long.class), parserFieldName);
      wildcards.put(cleanName, getMapWriter(drillFieldName, rowWriter));
    } else if (type.equals(Casts.DOUBLE) || mappedColumns.get(drillFieldName) == MinorType.FLOAT8) {
      parser.addParseTarget(this.getClass().getMethod("set", String.class, Double.class), parserFieldName);
      doubles.put(parserFieldName, rowWriter.scalar(drillFieldName));
    } else if (type.equals(Casts.LONG) || mappedColumns.get(drillFieldName) == MinorType.BIGINT) {
        parser.addParseTarget(this.getClass().getMethod("set", String.class, Long.class), parserFieldName);
        longs.put(parserFieldName, rowWriter.scalar(drillFieldName));
    } else {
      if (parserFieldName.startsWith("TIME.STAMP:")) {
        parser.addParseTarget(this.getClass().getMethod("setTimestamp", String.class, String.class), parserFieldName);
        timestamps.put(parserFieldName, rowWriter.scalar(drillFieldName));
      } else if (parserFieldName.startsWith("TIME.EPOCH:")) {
        parser.addParseTarget(this.getClass().getMethod("setTimestampFromEpoch", String.class, Long.class), parserFieldName);
        timestamps.put(parserFieldName, rowWriter.scalar(drillFieldName));
      } else if (parserFieldName.startsWith("TIME.DATE")) {
        parser.addParseTarget(this.getClass().getMethod("setDate", String.class, String.class), parserFieldName);
        dates.put(parserFieldName, rowWriter.scalar(drillFieldName));
      } else if (parserFieldName.startsWith("TIME.TIME")) {
        parser.addParseTarget(this.getClass().getMethod("setTime", String.class, String.class), parserFieldName);
        times.put(parserFieldName, rowWriter.scalar(drillFieldName));
      } else {
        parser.addParseTarget(this.getClass().getMethod("set", String.class, String.class), parserFieldName);
        strings.put(parserFieldName, rowWriter.scalar(drillFieldName));
      }
    }
  }

  private TupleWriter getMapWriter(String mapName, RowSetLoader rowWriter) {
    int index = rowWriter.tupleSchema().index(mapName);
    if (index == -1) {
      index = rowWriter.addColumn(SchemaBuilder.columnSchema(mapName, TypeProtos.MinorType.MAP, TypeProtos.DataMode.REQUIRED));
    }
    return rowWriter.tuple(index);
  }

  /**
   * Helper function to write a 1D long column
   *
   * @param rowWriter The row to which the data will be written
   * @param name The column name
   * @param value The value to be written
   */
  private void writeLongColumn(TupleWriter rowWriter, String name, long value) {
    ScalarWriter colWriter = getColWriter(rowWriter, name, MinorType.BIGINT);
    colWriter.setLong(value);
  }

  /**
   * Helper function to write a 1D String column
   *
   * @param rowWriter The row to which the data will be written
   * @param name The column name
   * @param value The value to be written
   */
  private void writeStringColumn(TupleWriter rowWriter, String name, String value) {
    ScalarWriter colWriter = getColWriter(rowWriter, name, MinorType.VARCHAR);
    colWriter.setString(value);
  }

  /**
   * Helper function to write a 1D String column
   *
   * @param rowWriter The row to which the data will be written
   * @param name The column name
   * @param value The value to be written
   */
  private void writeFloatColumn(TupleWriter rowWriter, String name, double value) {
    ScalarWriter colWriter = getColWriter(rowWriter, name, MinorType.FLOAT8);
    colWriter.setDouble(value);
  }

  private ScalarWriter getColWriter(TupleWriter tupleWriter, String fieldName, TypeProtos.MinorType type) {
    int index = tupleWriter.tupleSchema().index(fieldName);
    if (index == -1) {
      ColumnMetadata colSchema = MetadataUtils.newScalar(fieldName, type, TypeProtos.DataMode.OPTIONAL);
      index = tupleWriter.addColumn(colSchema);
    }
    return tupleWriter.scalar(index);
  }
}
