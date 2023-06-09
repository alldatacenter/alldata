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
package org.apache.drill.exec.store.bson;

import io.netty.buffer.DrillBuf;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.common.expression.PathSegment;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.expr.holders.BigIntHolder;
import org.apache.drill.exec.expr.holders.BitHolder;
import org.apache.drill.exec.expr.holders.Float8Holder;
import org.apache.drill.exec.expr.holders.IntHolder;
import org.apache.drill.exec.expr.holders.VarBinaryHolder;
import org.apache.drill.exec.expr.holders.VarCharHolder;
import org.apache.drill.exec.physical.base.GroupScan;
import org.apache.drill.exec.vector.complex.impl.MapOrListWriterImpl;
import org.apache.drill.exec.vector.complex.writer.BaseWriter;
import org.apache.drill.exec.vector.complex.writer.BaseWriter.ComplexWriter;
import org.apache.drill.exec.vector.complex.writer.TimeStampWriter;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.bson.BsonBinary;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.joda.time.DateTime;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.List;

public class BsonRecordReader {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(BsonRecordReader.class);
  public final static int MAX_RECORD_SIZE = 128 * 1024;
  private final List<SchemaPath> columns;
  private boolean atLeastOneWrite = false;
  private final boolean readNumbersAsDouble;
  protected DrillBuf workBuf;
  private String currentFieldName;
  // Used for error context
  private BsonReader reader;

  public BsonRecordReader(DrillBuf managedBuf, boolean allTextMode, boolean readNumbersAsDouble) {
    this(managedBuf, GroupScan.ALL_COLUMNS, readNumbersAsDouble);
  }

  public BsonRecordReader(DrillBuf managedBuf, List<SchemaPath> columns, boolean readNumbersAsDouble) {
    assert Preconditions.checkNotNull(columns).size() > 0 : "bson record reader requires at least a column";
    this.readNumbersAsDouble = readNumbersAsDouble;
    this.workBuf = managedBuf;
    this.columns = columns;
  }

  public void write(ComplexWriter writer, BsonReader reader) throws IOException {
    this.reader = reader;
    reader.readStartDocument();
    BsonType readBsonType = reader.getCurrentBsonType();
    switch (readBsonType) {
    case DOCUMENT:
      writeToListOrMap(reader, new MapOrListWriterImpl(writer.rootAsMap()), false, null);
      break;
    default:
      throw new DrillRuntimeException("Root object must be DOCUMENT type. Found: " + readBsonType);
    }
  }

  private void writeToListOrMap(BsonReader reader, final MapOrListWriterImpl writer, boolean isList, String fieldName) {
    writer.start();
    // If isList is true, then filedName can be null as it is not required while
    // writing
    while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
      if (!isList) {
        fieldName = reader.readName();
      }
      BsonType currentBsonType = reader.getCurrentBsonType();
      switch (currentBsonType) {
      case INT32:
        int readInt32 = reader.readInt32();
        if (readNumbersAsDouble) {
          writeDouble(readInt32, writer, fieldName, isList);
        } else {
          writeInt32(readInt32, writer, fieldName, isList);
        }
        atLeastOneWrite = true;
        break;
      case INT64:
        long readInt64 = reader.readInt64();
        if (readNumbersAsDouble) {
          writeDouble(readInt64, writer, fieldName, isList);
        } else {
          writeInt64(readInt64, writer, fieldName, isList);
        }
        atLeastOneWrite = true;
        break;
      case ARRAY:
        reader.readStartArray();
        writeToListOrMap(reader, (MapOrListWriterImpl) writer.list(fieldName), true, fieldName);
        atLeastOneWrite = true;
        break;
      case BINARY:
        // handle types
        writeBinary(reader, writer, fieldName, isList);
        atLeastOneWrite = true;
        break;
      case BOOLEAN:
        boolean readBoolean = reader.readBoolean();
        writeBoolean(readBoolean, writer, fieldName, isList);
        atLeastOneWrite = true;
        break;
      case DATE_TIME:
        long readDateTime = reader.readDateTime();
        writeDateTime(readDateTime, writer, fieldName, isList);
        atLeastOneWrite = true;
        break;
      case DOCUMENT:
        reader.readStartDocument();
        // To handle nested Documents.
        MapOrListWriterImpl _writer = writer;
        if (!isList) {
          _writer = (MapOrListWriterImpl) writer.map(fieldName);
        } else {
          _writer = (MapOrListWriterImpl) writer.listoftmap(fieldName);
        }
        writeToListOrMap(reader, _writer, false, fieldName);
        atLeastOneWrite = true;
        break;
      case DOUBLE:
        double readDouble = reader.readDouble();
        writeDouble(readDouble, writer, fieldName, isList);
        atLeastOneWrite = true;
        break;
      case JAVASCRIPT:
        final String readJavaScript = reader.readJavaScript();
        writeString(readJavaScript, writer, fieldName, isList);
        atLeastOneWrite = true;
        break;
      case JAVASCRIPT_WITH_SCOPE:
        final String readJavaScriptWithScopeString = reader.readJavaScriptWithScope();
        writeString(readJavaScriptWithScopeString, writer, fieldName, isList);
        atLeastOneWrite = true;
        break;
      case NULL:
        // just read and ignore.
        reader.readNull();
        break;
      case OBJECT_ID:
        writeObjectId(reader, writer, fieldName, isList);
        atLeastOneWrite = true;
        break;
      case STRING:
        final String readString = reader.readString();
        writeString(readString, writer, fieldName, isList);
        atLeastOneWrite = true;
        break;
      case SYMBOL:
        final String readSymbol = reader.readSymbol();
        writeString(readSymbol, writer, fieldName, isList);
        atLeastOneWrite = true;
        break;
      case TIMESTAMP:
        int time = reader.readTimestamp().getTime();
        writeTimeStamp(time, writer, fieldName, isList);
        atLeastOneWrite = true;
        break;
      case DECIMAL128:
         BigDecimal readBigDecimalAsDecimal128 = reader.readDecimal128().bigDecimalValue();
         writeDecimal128(readBigDecimalAsDecimal128, writer, fieldName, isList);
         atLeastOneWrite = true;
         break;
      default:
        // Didn't handled REGULAR_EXPRESSION and DB_POINTER types
        throw new DrillRuntimeException("UnSupported Bson type: " + currentBsonType);
      }
    }
    if (!isList) {
      reader.readEndDocument();
    } else {
      reader.readEndArray();
    }
  }

  private void writeBinary(BsonReader reader, final MapOrListWriterImpl writer, String fieldName, boolean isList) {
    final VarBinaryHolder vb = new VarBinaryHolder();
    BsonBinary readBinaryData = reader.readBinaryData();
    byte[] data = readBinaryData.getData();
    Byte type = (Byte) readBinaryData.getType();
    // Based on specified binary type, cast it accordingly
    switch (type.intValue()) {
    case 1:
      // Double 1
      writeDouble(ByteBuffer.wrap(data).getDouble(), writer, fieldName, isList);
      break;
    case 2:
      // String 2
      writeString(new String(data), writer, fieldName, isList);
      break;
    case 8:
      // Boolean 8
      boolean boolValue = (data == null || data.length == 0) ? false : data[0] != 0x00;
      writeBoolean(boolValue, writer, fieldName, isList);
      break;
    case 9:
      // Date 9
      writeDateTime(ByteBuffer.wrap(data).getLong(), writer, fieldName, isList);
      break;
    case 13:
      // JavaScript 13
      writeString(new String(data), writer, fieldName, isList);
      break;
    case 14:
      // Symbol 14
      writeString(new String(data), writer, fieldName, isList);
      break;
    case 15:
      // JavaScript (with scope) 15
      writeString(new String(data), writer, fieldName, isList);
      break;
    case 16:
      // 32-bit integer 16
      writeInt32(ByteBuffer.wrap(data).getInt(), writer, fieldName, isList);
      break;
    case 17:
      // Timestamp 17
      writeTimeStamp(ByteBuffer.wrap(data).getInt(), writer, fieldName, isList);
      break;
    case 18:
      // 64-bit integer 18
      writeInt64(ByteBuffer.wrap(data).getInt(), writer, fieldName, isList);
      break;
    default:
      // In case of Object(3)/Binary data (5)/Object id (7) or in other case
      // considering as VarBinary
      final byte[] bytes = readBinaryData.getData();
      writeBinary(writer, fieldName, isList, vb, bytes);
      break;
    }
  }

  private void writeTimeStamp(int timestamp, final MapOrListWriterImpl writer, String fieldName, boolean isList) {
    DateTime dateTime = new DateTime(timestamp*1000L);
    TimeStampWriter t;
    if (isList == false) {
      t = writer.map.timeStamp(fieldName);
    } else {
      t = writer.list.timeStamp();
    }
    t.writeTimeStamp(dateTime.withZoneRetainFields(org.joda.time.DateTimeZone.UTC).getMillis());
  }

  private void writeString(String readString, final MapOrListWriterImpl writer, String fieldName, boolean isList) {
    int length;
    byte[] strBytes;
    try {
      strBytes = readString.getBytes("UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new DrillRuntimeException("Unable to read string value for field: " + fieldName, e);
    }
    length = strBytes.length;
    ensure(length);
    workBuf.setBytes(0, strBytes);
    final VarCharHolder vh = new VarCharHolder();
    vh.buffer = workBuf;
    vh.start = 0;
    vh.end = length;
    if (isList == false) {
      writer.varChar(fieldName).write(vh);
    } else {
      writer.list.varChar().write(vh);
    }
  }

  private void writeObjectId(BsonReader reader, final MapOrListWriterImpl writer, String fieldName, boolean isList) {
    final VarBinaryHolder vObj = new VarBinaryHolder();
    final byte[] objBytes = reader.readObjectId().toByteArray();
    writeBinary(writer, fieldName, isList, vObj, objBytes);
  }

  private void writeDouble(double readDouble, final MapOrListWriterImpl writer, String fieldName, boolean isList) {
    final Float8Holder f8h = new Float8Holder();
    f8h.value = readDouble;
    if (isList == false) {
      writer.float8(fieldName).write(f8h);
    } else {
      writer.list.float8().write(f8h);
    }
  }

  private void writeDateTime(long readDateTime, final MapOrListWriterImpl writer, String fieldName, boolean isList) {
    DateTime date = new DateTime(readDateTime);
    TimeStampWriter dt;
    if (isList == false) {
      dt = writer.map.timeStamp(fieldName);
    } else {
      dt = writer.list.timeStamp();
    }
    dt.writeTimeStamp(date.withZoneRetainFields(org.joda.time.DateTimeZone.UTC).getMillis());
  }

  private void writeBoolean(boolean readBoolean, final MapOrListWriterImpl writer, String fieldName, boolean isList) {
    final BitHolder bit = new BitHolder();
    bit.value = readBoolean ? 1 : 0;
    if (isList == false) {
      writer.bit(fieldName).write(bit);
    } else {
      writer.list.bit().write(bit);
    }
  }

  private void writeBinary(final MapOrListWriterImpl writer, String fieldName, boolean isList,
      final VarBinaryHolder vb, final byte[] bytes) {
    ensure(bytes.length);
    workBuf.setBytes(0, bytes);
    vb.buffer = workBuf;
    vb.start = 0;
    vb.end = bytes.length;
    if (isList == false) {
      writer.binary(fieldName).write(vb);
    } else {
      writer.list.varBinary().write(vb);
    }
  }

  private void writeInt64(long readInt64, final MapOrListWriterImpl writer, String fieldName, boolean isList) {
    final BigIntHolder bh = new BigIntHolder();
    bh.value = readInt64;
    if (isList == false) {
      writer.bigInt(fieldName).write(bh);
    } else {
      writer.list.bigInt().write(bh);
    }
  }

  private void writeInt32(int readInt32, final MapOrListWriterImpl writer, String fieldName, boolean isList) {
    final IntHolder ih = new IntHolder();
    ih.value = readInt32;
    if (isList == false) {
      writer.integer(fieldName).write(ih);
    } else {
      writer.list.integer().write(ih);
    }
  }

    private void writeDecimal128(BigDecimal readBigDecimal, final MapOrListWriterImpl writer, String fieldName, boolean isList) {
        if (isList) {
            writer.list.varDecimal().writeVarDecimal(readBigDecimal);
        } else {
            writer.varDecimal(fieldName, readBigDecimal.precision(), readBigDecimal.scale()).writeVarDecimal(readBigDecimal);
        }
    }


    public void ensureAtLeastOneField(ComplexWriter writer) {
    if (!atLeastOneWrite) {
      // if we had no columns, create one empty one so we can return some data
      // for count purposes.
      SchemaPath sp = columns.get(0);
      PathSegment root = sp.getRootSegment();
      BaseWriter.MapWriter fieldWriter = writer.rootAsMap();
      while (root.getChild() != null && !root.getChild().isArray()) {
        fieldWriter = fieldWriter.map(root.getNameSegment().getPath());
        root = root.getChild();
      }
      fieldWriter.integer(root.getNameSegment().getPath());
    }
  }

  private void ensure(final int length) {
    workBuf = workBuf.reallocIfNeeded(length);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("BsonRecordReader[");
    if (reader != null) {
      sb.append("Name=")
          .append(reader.getCurrentName())
          .append(", Type=")
          .append(reader.getCurrentBsonType());
    }
    sb.append(']');
    return sb.toString();
  }
}
