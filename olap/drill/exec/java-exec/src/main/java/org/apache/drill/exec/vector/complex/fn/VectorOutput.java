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
package org.apache.drill.exec.vector.complex.fn;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.expr.fn.impl.DateUtility;
import org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers;
import org.apache.drill.exec.expr.holders.BigIntHolder;
import org.apache.drill.exec.expr.holders.DateHolder;
import org.apache.drill.exec.expr.holders.Decimal38DenseHolder;
import org.apache.drill.exec.expr.holders.IntervalHolder;
import org.apache.drill.exec.expr.holders.TimeHolder;
import org.apache.drill.exec.expr.holders.TimeStampHolder;
import org.apache.drill.exec.expr.holders.VarBinaryHolder;
import org.apache.drill.exec.expr.holders.VarCharHolder;
import org.apache.drill.exec.vector.DateUtilities;
import org.apache.drill.exec.vector.complex.writer.BaseWriter.ListWriter;
import org.apache.drill.exec.vector.complex.writer.BaseWriter.MapWriter;
import org.apache.drill.exec.vector.complex.writer.BigIntWriter;
import org.apache.drill.exec.vector.complex.writer.DateWriter;
import org.apache.drill.exec.vector.complex.writer.IntervalWriter;
import org.apache.drill.exec.vector.complex.writer.TimeStampWriter;
import org.apache.drill.exec.vector.complex.writer.TimeWriter;
import org.apache.drill.exec.vector.complex.writer.VarBinaryWriter;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.ISOPeriodFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

abstract class VectorOutput {

  private static final Logger logger = LoggerFactory.getLogger(VectorOutput.class);

  final VarBinaryHolder binary = new VarBinaryHolder();
  final TimeHolder time = new TimeHolder();
  final DateHolder date = new DateHolder();
  final TimeStampHolder timestamp = new TimeStampHolder();
  final IntervalHolder interval = new IntervalHolder();
  final BigIntHolder bigint = new BigIntHolder();
  final Decimal38DenseHolder decimal = new Decimal38DenseHolder();
  final VarCharHolder varchar = new VarCharHolder();

  protected final WorkingBuffer work;
  protected JsonParser parser;

  protected DateTimeFormatter isoDateTimeFormatter = new DateTimeFormatterBuilder().append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
      .optionalStart().appendOffset("+HH:MM", "+00:00").optionalEnd()
      .optionalStart().appendOffset("+HHMM", "+0000").optionalEnd()
      .optionalStart().appendOffset("+HH", "Z").optionalEnd()
      .toFormatter();

  public VectorOutput(WorkingBuffer work){
    this.work = work;
  }

  public void setParser(JsonParser parser){
    this.parser = parser;
  }

  protected boolean innerRun() throws IOException{
    JsonToken t = parser.nextToken();
    if(t != JsonToken.FIELD_NAME){
      return false;
    }

    String possibleTypeName = parser.getText();
    if(!possibleTypeName.isEmpty() && possibleTypeName.charAt(0) == '$'){
      switch(possibleTypeName){
      case ExtendedTypeName.BINARY:
        writeBinary(checkNextToken(JsonToken.VALUE_STRING));
        checkCurrentToken(JsonToken.END_OBJECT);
        return true;
      case ExtendedTypeName.TYPE:
        if(checkNextToken(JsonToken.VALUE_NUMBER_INT) || !hasBinary()) {
          throw UserException.parseError()
          .message("Either $type is not an integer or has no $binary")
          .build(logger);
        }
        writeBinary(checkNextToken(JsonToken.VALUE_STRING));
        checkCurrentToken(JsonToken.END_OBJECT);
        return true;
      case ExtendedTypeName.DATE:
        writeDate(checkNextToken(JsonToken.VALUE_STRING));
        checkNextToken(JsonToken.END_OBJECT);
        return true;
      case ExtendedTypeName.TIME:
        writeTime(checkNextToken(JsonToken.VALUE_STRING));
        checkNextToken(JsonToken.END_OBJECT);
        return true;
      case ExtendedTypeName.TIMESTAMP:
        writeTimestamp(checkNextToken(JsonToken.VALUE_STRING, JsonToken.VALUE_NUMBER_INT));
        checkNextToken(JsonToken.END_OBJECT);
        return true;
      case ExtendedTypeName.INTERVAL:
        writeInterval(checkNextToken(JsonToken.VALUE_STRING));
        checkNextToken(JsonToken.END_OBJECT);
        return true;
      case ExtendedTypeName.INTEGER:
        writeInteger(checkNextToken(JsonToken.VALUE_STRING, JsonToken.VALUE_NUMBER_INT));
        checkNextToken(JsonToken.END_OBJECT);
        return true;
      case ExtendedTypeName.DECIMAL:
        writeDecimal(checkNextToken(JsonToken.VALUE_NUMBER_FLOAT, JsonToken.VALUE_NUMBER_INT));
        checkNextToken(JsonToken.END_OBJECT);
        return true;
      }
    }
    return false;
  }

  public boolean checkNextToken(final JsonToken expected) throws IOException{
    return checkNextToken(expected, expected);
  }

  public boolean checkCurrentToken(final JsonToken expected) throws IOException{
    return checkCurrentToken(expected, expected);
  }

  public boolean checkNextToken(final JsonToken expected1, final JsonToken expected2) throws IOException{
    return checkToken(parser.nextToken(), expected1, expected2);
  }

  public boolean checkCurrentToken(final JsonToken expected1, final JsonToken expected2) throws IOException{
    return checkToken(parser.getCurrentToken(), expected1, expected2);
  }

  boolean hasType() throws JsonParseException, IOException {
    JsonToken token = parser.nextToken();
    return token == JsonToken.FIELD_NAME && parser.getText().equals(ExtendedTypeName.TYPE);
  }

  boolean hasBinary() throws JsonParseException, IOException {
    JsonToken token = parser.nextToken();
    return token == JsonToken.FIELD_NAME && parser.getText().equals(ExtendedTypeName.BINARY);
  }

  long getType() throws JsonParseException, IOException {
    if (!checkNextToken(JsonToken.VALUE_NUMBER_INT, JsonToken.VALUE_STRING)) {
      long type = parser.getValueAsLong();
      //Advancing the token, as checking current token in binary
      parser.nextToken();
      return type;
    }
    throw new JsonParseException("Failure while reading $type value. Expected a NUMBER or STRING",
        parser.getCurrentLocation());
  }

  public boolean checkToken(final JsonToken t, final JsonToken expected1, final JsonToken expected2) throws IOException{
    if(t == JsonToken.VALUE_NULL){
      return true;
    }else if(t == expected1){
      return false;
    }else if(t == expected2){
      return false;
    }else{
      throw new JsonParseException(String.format("Failure while reading ExtendedJSON typed value. Expected a %s but "
          + "received a token of type %s", expected1, t), parser.getCurrentLocation());
    }
  }

  public abstract void writeBinary(boolean isNull) throws IOException;
  public abstract void writeDate(boolean isNull) throws IOException;
  public abstract void writeTime(boolean isNull) throws IOException;
  public abstract void writeTimestamp(boolean isNull) throws IOException;
  public abstract void writeInterval(boolean isNull) throws IOException;
  public abstract void writeInteger(boolean isNull) throws IOException;
  public abstract void writeDecimal(boolean isNull) throws IOException;

  static class ListVectorOutput extends VectorOutput{
    private ListWriter writer;

    public ListVectorOutput(WorkingBuffer work) {
      super(work);
    }

    public boolean run(ListWriter writer) throws IOException{
      this.writer = writer;
      return innerRun();
    }

    @Override
    public void writeBinary(boolean isNull) throws IOException {
      VarBinaryWriter bin = writer.varBinary();
      if(!isNull){
        byte[] binaryData = parser.getBinaryValue();
        if (hasType()) {
          //Ignoring type info as of now.
          long type = getType();
          if (type < 0 || type > 255) {
            throw UserException.validationError()
            .message("$type should be between 0 to 255")
            .build(logger);
          }
        }
        work.prepareBinary(binaryData, binary);
        bin.write(binary);
      }
    }

    @Override
    public void writeDate(boolean isNull) throws IOException {
      DateWriter dt = writer.date();
      if(!isNull){
        work.prepareVarCharHolder(parser.getValueAsString(), varchar);
        dt.writeDate(StringFunctionHelpers.getDate(varchar.buffer, varchar.start, varchar.end));
      }
    }

    @Override
    public void writeTime(boolean isNull) throws IOException {
      TimeWriter t = writer.time();
      if(!isNull){
        // read time and obtain the local time in the provided time zone.
        LocalTime localTime = OffsetTime.parse(parser.getValueAsString(), DateUtility.isoFormatTime).toLocalTime();
        t.writeTime((int) ((localTime.toNanoOfDay() + 500000L) / 1000000L)); // round to milliseconds
      }
    }

    @Override
    public void writeTimestamp(boolean isNull) throws IOException {
      TimeStampWriter ts = writer.timeStamp();
      if(!isNull){
        switch (parser.getCurrentToken()) {
        case VALUE_NUMBER_INT:
          DateTime dt = new DateTime(parser.getLongValue(), org.joda.time.DateTimeZone.UTC);
          ts.writeTimeStamp(dt.getMillis());
          break;
        case VALUE_STRING:
          // Note mongo use the UTC format to specify the date value by default.
          // See the mongo specs and the Drill handler (in new JSON loader) :
          // 1. https://docs.mongodb.com/manual/reference/mongodb-extended-json
          // 2. org.apache.drill.exec.store.easy.json.values.UtcTimestampValueListener
          Instant instant = isoDateTimeFormatter.parse(parser.getValueAsString(), Instant::from);
          long offset = ZoneId.systemDefault().getRules().getOffset(instant).getTotalSeconds() * 1000;
          ts.writeTimeStamp(instant.toEpochMilli() + offset);
          break;
        default:
          throw UserException.unsupportedError()
              .message(parser.getCurrentToken().toString())
              .build(logger);
        }
      }
    }

    @Override
    public void writeInterval(boolean isNull) throws IOException {
      IntervalWriter intervalWriter = writer.interval();
      if(!isNull){
        final Period p = ISOPeriodFormat.standard().parsePeriod(parser.getValueAsString());
        int months = DateUtilities.monthsFromPeriod(p);
        int days = p.getDays();
        int millis = DateUtilities.periodToMillis(p);
        intervalWriter.writeInterval(months, days, millis);
      }
    }

    @Override
    public void writeInteger(boolean isNull) throws IOException {
      BigIntWriter intWriter = writer.bigInt();
      if(!isNull){
        intWriter.writeBigInt(Long.parseLong(parser.getValueAsString()));
      }
    }

    @Override
    public void writeDecimal(boolean isNull) throws IOException {
      throw new JsonParseException("Decimal Extended types not yet supported.", parser.getCurrentLocation());
    }

  }

  static class MapVectorOutput extends VectorOutput {

    private MapWriter writer;
    private String fieldName;

    public MapVectorOutput(WorkingBuffer work) {
      super(work);
    }

    public boolean run(MapWriter writer, String fieldName) throws IOException{
      this.fieldName = fieldName;
      this.writer = writer;
      return innerRun();
    }

    @Override
    public void writeBinary(boolean isNull) throws IOException {
      VarBinaryWriter bin = writer.varBinary(fieldName);
      if(!isNull){
        byte[] binaryData = parser.getBinaryValue();
        if (hasType()) {
          //Ignoring type info as of now.
          long type = getType();
          if (type < 0 || type > 255) {
            throw UserException.validationError()
            .message("$type should be between 0 to 255")
            .build(logger);
          }
        }
        work.prepareBinary(binaryData, binary);
        bin.write(binary);
      }
    }

    @Override
    public void writeDate(boolean isNull) throws IOException {
      DateWriter dt = writer.date(fieldName);
      if(!isNull){
        LocalDate    localDate = LocalDate.parse(parser.getValueAsString(), DateUtility.isoFormatDate);
        OffsetDateTime utcDate = OffsetDateTime.of(localDate, LocalTime.MIDNIGHT, ZoneOffset.UTC);

        dt.writeDate(utcDate.toInstant().toEpochMilli()); // round to milliseconds
      }
    }

    @Override
    public void writeTime(boolean isNull) throws IOException {
      TimeWriter t = writer.time(fieldName);
      if(!isNull){
        LocalTime localTime = OffsetTime.parse(parser.getValueAsString(), DateUtility.isoFormatTime).toLocalTime();
        t.writeTime((int) ((localTime.toNanoOfDay() + 500000L) / 1000000L)); // round to milliseconds
      }
    }

    @Override
    public void writeTimestamp(boolean isNull) throws IOException {
      TimeStampWriter ts = writer.timeStamp(fieldName);
      if(!isNull){
        switch (parser.getCurrentToken()) {
        case VALUE_NUMBER_INT:
          DateTime dt = new DateTime(parser.getLongValue(), org.joda.time.DateTimeZone.UTC);
          ts.writeTimeStamp(dt.getMillis());
          break;
        case VALUE_STRING:
          // Note mongo use the UTC format to specify the date value by default.
          // See the mongo specs and the Drill handler (in new JSON loader) :
          // 1. https://docs.mongodb.com/manual/reference/mongodb-extended-json
          // 2. org.apache.drill.exec.store.easy.json.values.UtcTimestampValueListener
          Instant instant = isoDateTimeFormatter.parse(parser.getValueAsString(), Instant::from);
          long offset = ZoneId.systemDefault().getRules().getOffset(instant).getTotalSeconds() * 1000;
          ts.writeTimeStamp(instant.toEpochMilli() + offset);
          break;
        default:
          throw UserException.unsupportedError()
          .message(parser.getCurrentToken().toString())
          .build(logger);
        }
      }
    }

    @Override
    public void writeInterval(boolean isNull) throws IOException {
      IntervalWriter intervalWriter = writer.interval(fieldName);
      if(!isNull){
        final Period p = ISOPeriodFormat.standard().parsePeriod(parser.getValueAsString());
        int months = DateUtilities.monthsFromPeriod(p);
        int days = p.getDays();
        int millis = DateUtilities.periodToMillis(p);
        intervalWriter.writeInterval(months, days, millis);
      }
    }

    @Override
    public void writeInteger(boolean isNull) throws IOException {
      BigIntWriter intWriter = writer.bigInt(fieldName);
      if(!isNull){
        intWriter.writeBigInt(Long.parseLong(parser.getValueAsString()));
      }
    }

    @Override
    public void writeDecimal(boolean isNull) throws IOException {
      throw new IOException("Decimal Extended types not yet supported.");
    }

  }

}
