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
package org.apache.drill.exec.store.parquet;

import static org.apache.parquet.column.Encoding.PLAIN;
import static org.apache.parquet.column.Encoding.RLE;

import java.util.HashMap;

import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.store.ByteArrayUtil;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.bytes.BytesInput;
import org.apache.parquet.bytes.DirectByteBufferAllocator;
import org.apache.parquet.column.ColumnDescriptor;
import org.apache.parquet.column.values.rle.RunLengthBitPackingHybridValuesWriter;
import org.apache.parquet.hadoop.ParquetFileWriter;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.MessageTypeParser;

public class TestFileGenerator {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestFileGenerator.class);

  // 10 mb per page
  static int bytesPerPage = 1024 * 1024 * 1;
  // { 00000001, 00000010, 00000100, 00001000, 00010000, ... }
  static byte[] bitFields = { 1, 2, 4, 8, 16, 32, 64, -128 };
  static final byte allBitsTrue = -1;
  static final byte allBitsFalse = 0;
  static final byte[] varLen1 = { 50, 51, 52, 53, 54, 55, 56 };
  static final byte[] varLen2 = { 15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1 };
  static final byte[] varLen3 = { 100, 99, 98 };

  static final Object[] intVals = { -200, 100, Integer.MAX_VALUE };
  static final Object[] longVals = { -5000l, 5000l, Long.MAX_VALUE };
  static final Object[] floatVals = { 1.74f, Float.MAX_VALUE, Float.MIN_VALUE };
  static final Object[] doubleVals = { 100.45d, Double.MAX_VALUE, Double.MIN_VALUE, };
  static final Object[] boolVals = { false, false, true };
  static final Object[] binVals = { varLen1, varLen2, varLen3 };
  static final Object[] bin2Vals = { varLen3, varLen2, varLen1 };

  // TODO - figure out what this should be set at, it should be based on the max nesting level
  public static final int MAX_EXPECTED_BIT_WIDTH_FOR_DEFINITION_LEVELS = 16;

  static void populateDrill_418_fields(ParquetTestProperties props) {

    props.fields.put("cust_key", new FieldInfo("int32", "integer", 32, intVals, TypeProtos.MinorType.INT, props));
    props.fields.put("nation_key", new FieldInfo("int32", "integer", 32, intVals, TypeProtos.MinorType.INT, props));
    props.fields.put("acctbal", new FieldInfo("int32", "integer", 32, intVals, TypeProtos.MinorType.INT, props));
    props.fields.put("name", new FieldInfo("int32", "integer", 32, intVals, TypeProtos.MinorType.INT, props));
    props.fields.put("address", new FieldInfo("int32", "integer", 32, intVals, TypeProtos.MinorType.INT, props));
    props.fields.put("phone", new FieldInfo("int32", "integer", 32, intVals, TypeProtos.MinorType.INT, props));
    props.fields.put("mktsegment", new FieldInfo("int32", "integer", 32, intVals, TypeProtos.MinorType.INT, props));
    props.fields.put("comment_col", new FieldInfo("int32", "integer", 32, intVals, TypeProtos.MinorType.INT, props));
  }

  static void populateFieldInfoMap(ParquetTestProperties props) {
    props.fields.put("integer", new FieldInfo("int32", "integer", 32, intVals, TypeProtos.MinorType.INT, props));
    props.fields.put("bigInt", new FieldInfo("int64", "bigInt", 64, longVals, TypeProtos.MinorType.BIGINT, props));
    props.fields.put("f", new FieldInfo("float", "f", 32, floatVals, TypeProtos.MinorType.FLOAT4, props));
    props.fields.put("d", new FieldInfo("double", "d", 64, doubleVals, TypeProtos.MinorType.FLOAT8, props));
    props.fields.put("b", new FieldInfo("boolean", "b", 1, boolVals, TypeProtos.MinorType.BIT, props));
    props.fields.put("bin", new FieldInfo("binary", "bin", -1, binVals, TypeProtos.MinorType.VARBINARY, props));
    props.fields.put("bin2", new FieldInfo("binary", "bin2", -1, bin2Vals, TypeProtos.MinorType.VARBINARY, props));
  }

  static void populatePigTPCHCustomerFields(ParquetTestProperties props) {
    // all of the data in the fieldInfo constructors doesn't matter because the file is generated outside the test
    props.fields.put("C_CUSTKEY", new FieldInfo("int32", "integer", 32, intVals, TypeProtos.MinorType.INT, props));
    props.fields.put("C_NATIONKEY", new FieldInfo("int64", "bigInt", 64, longVals, TypeProtos.MinorType.BIGINT, props));
    props.fields.put("C_ACCTBAL", new FieldInfo("float", "f", 32, floatVals, TypeProtos.MinorType.FLOAT4, props));
    props.fields.put("C_NAME", new FieldInfo("double", "d", 64, doubleVals, TypeProtos.MinorType.FLOAT8, props));
    props.fields.put("C_ADDRESS", new FieldInfo("boolean", "b", 1, boolVals, TypeProtos.MinorType.BIT, props));
    props.fields.put("C_PHONE", new FieldInfo("binary", "bin", -1, binVals, TypeProtos.MinorType.VARBINARY, props));
    props.fields.put("C_MKTSEGMENT", new FieldInfo("binary", "bin2", -1, bin2Vals, TypeProtos.MinorType.VARBINARY, props));
    props.fields.put("C_COMMENT", new FieldInfo("binary", "bin2", -1, bin2Vals, TypeProtos.MinorType.VARBINARY, props));
  }

  static void populatePigTPCHSupplierFields(ParquetTestProperties props) {
    // all of the data in the fieldInfo constructors doesn't matter because the file is generated outside the test
    props.fields.put("S_SUPPKEY", new FieldInfo("int32", "integer", 32, intVals, TypeProtos.MinorType.INT, props));
    props.fields.put("S_NATIONKEY", new FieldInfo("int64", "bigInt", 64, longVals, TypeProtos.MinorType.BIGINT, props));
    props.fields.put("S_ACCTBAL", new FieldInfo("float", "f", 32, floatVals, TypeProtos.MinorType.FLOAT4, props));
    props.fields.put("S_NAME", new FieldInfo("double", "d", 64, doubleVals, TypeProtos.MinorType.FLOAT8, props));
    props.fields.put("S_ADDRESS", new FieldInfo("boolean", "b", 1, boolVals, TypeProtos.MinorType.BIT, props));
    props.fields.put("S_PHONE", new FieldInfo("binary", "bin", -1, binVals, TypeProtos.MinorType.VARBINARY, props));
    props.fields.put("S_COMMENT", new FieldInfo("binary", "bin2", -1, bin2Vals, TypeProtos.MinorType.VARBINARY, props));
  }

  public static void generateParquetFile(String filename, ParquetTestProperties props) throws Exception {

    int currentBooleanByte = 0;
    WrapAroundCounter booleanBitCounter = new WrapAroundCounter(7);

    Configuration configuration = new Configuration();
    configuration.set(FileSystem.FS_DEFAULT_NAME_KEY, "file:///");
    //"message m { required int32 integer; required int64 integer64; required boolean b; required float f; required double d;}"

    FileSystem fs = FileSystem.get(configuration);
    Path path = new Path(filename);
    if (fs.exists(path)) {
      fs.delete(path, false);
    }


    String messageSchema = "message m {";
    for (FieldInfo fieldInfo : props.fields.values()) {
      messageSchema += " required " + fieldInfo.parquetType + " " + fieldInfo.name + ";";
    }
    // remove the last semicolon, java really needs a join method for strings...
    // TODO - nvm apparently it requires a semicolon after every field decl, might want to file a bug
    //messageSchema = messageSchema.substring(schemaType, messageSchema.length() - 1);
    messageSchema += "}";

    MessageType schema = MessageTypeParser.parseMessageType(messageSchema);

    CompressionCodecName codec = CompressionCodecName.UNCOMPRESSED;
    ParquetFileWriter w = new ParquetFileWriter(configuration, schema, path);
    w.start();
    HashMap<String, Integer> columnValuesWritten = new HashMap<>();
    int valsWritten;
    for (int k = 0; k < props.numberRowGroups; k++) {
      w.startBlock(props.recordsPerRowGroup);
      currentBooleanByte = 0;
      booleanBitCounter.reset();

      for (FieldInfo fieldInfo : props.fields.values()) {

        if ( ! columnValuesWritten.containsKey(fieldInfo.name)) {
          columnValuesWritten.put(fieldInfo.name, 0);
          valsWritten = 0;
        } else {
          valsWritten = columnValuesWritten.get(fieldInfo.name);
        }

        String[] path1 = {fieldInfo.name};
        ColumnDescriptor c1 = schema.getColumnDescription(path1);

        w.startColumn(c1, props.recordsPerRowGroup, codec);
        final int valsPerPage = (int) Math.ceil(props.recordsPerRowGroup / (float) fieldInfo.numberOfPages);
        final int PAGE_SIZE = 1024 * 1024; // 1 MB
        byte[] bytes;
        RunLengthBitPackingHybridValuesWriter defLevels = new RunLengthBitPackingHybridValuesWriter(
            MAX_EXPECTED_BIT_WIDTH_FOR_DEFINITION_LEVELS,
            valsPerPage,
            PAGE_SIZE,
            new DirectByteBufferAllocator());
        RunLengthBitPackingHybridValuesWriter repLevels = new RunLengthBitPackingHybridValuesWriter(
            MAX_EXPECTED_BIT_WIDTH_FOR_DEFINITION_LEVELS,
            valsPerPage,
            PAGE_SIZE,
            new DirectByteBufferAllocator());
        // for variable length binary fields
        int bytesNeededToEncodeLength = 4;
        if (fieldInfo.bitLength > 0) {
          bytes = new byte[(int) Math.ceil(valsPerPage * fieldInfo.bitLength / 8.0)];
        } else {
          // the twelve at the end is to account for storing a 4 byte length with each value
          int totalValLength = ((byte[]) fieldInfo.values[0]).length + ((byte[]) fieldInfo.values[1]).length + ((byte[]) fieldInfo.values[2]).length + 3 * bytesNeededToEncodeLength;
          // used for the case where there is a number of values in this row group that is not divisible by 3
          int leftOverBytes = 0;
          if ( valsPerPage % 3 > 0 ) {
            leftOverBytes += ((byte[])fieldInfo.values[1]).length + bytesNeededToEncodeLength;
          }
          if ( valsPerPage % 3 > 1 ) {
            leftOverBytes += ((byte[])fieldInfo.values[2]).length + bytesNeededToEncodeLength;
          }
          bytes = new byte[valsPerPage / 3 * totalValLength + leftOverBytes];
        }
        int bytesPerPage = (int) (valsPerPage * (fieldInfo.bitLength / 8.0));
        int bytesWritten = 0;
        for (int z = 0; z < fieldInfo.numberOfPages; z++, bytesWritten = 0) {
          for (int i = 0; i < valsPerPage; i++) {
            repLevels.writeInteger(0);
            defLevels.writeInteger(1);
            if (fieldInfo.values[0] instanceof Boolean) {

              bytes[currentBooleanByte] |= bitFields[booleanBitCounter.val]
                  & ((boolean) fieldInfo.values[valsWritten % 3] ? allBitsTrue : allBitsFalse);
              booleanBitCounter.increment();
              if (booleanBitCounter.val == 0) {
                currentBooleanByte++;
              }
              valsWritten++;
              if (currentBooleanByte > bytesPerPage) {
                break;
              }
            } else {
              if (fieldInfo.values[valsWritten % 3] instanceof byte[]) {
                System.arraycopy(ByteArrayUtil.toByta(((byte[])fieldInfo.values[valsWritten % 3]).length),
                    0, bytes, bytesWritten, bytesNeededToEncodeLength);
                System.arraycopy(fieldInfo.values[valsWritten % 3],
                    0, bytes, bytesWritten + bytesNeededToEncodeLength, ((byte[])fieldInfo.values[valsWritten % 3]).length);
                bytesWritten += ((byte[])fieldInfo.values[valsWritten % 3]).length + bytesNeededToEncodeLength;
              } else{
                System.arraycopy( ByteArrayUtil.toByta(fieldInfo.values[valsWritten % 3]),
                    0, bytes, i * (fieldInfo.bitLength / 8), fieldInfo.bitLength / 8);
              }
              valsWritten++;
            }

          }
          byte[] fullPage = new byte[2 * 4 * valsPerPage + bytes.length];
          byte[] repLevelBytes = repLevels.getBytes().toByteArray();
          byte[] defLevelBytes = defLevels.getBytes().toByteArray();
          System.arraycopy(bytes, 0, fullPage, 0, bytes.length);
          System.arraycopy(repLevelBytes, 0, fullPage, bytes.length, repLevelBytes.length);
          System.arraycopy(defLevelBytes, 0, fullPage, bytes.length + repLevelBytes.length, defLevelBytes.length);
          w.writeDataPage( (props.recordsPerRowGroup / fieldInfo.numberOfPages), fullPage.length, BytesInput.from(fullPage), RLE, RLE, PLAIN);
          currentBooleanByte = 0;
        }
        w.endColumn();
        columnValuesWritten.remove(fieldInfo.name);
        columnValuesWritten.put(fieldInfo.name, valsWritten);
      }

      w.endBlock();
    }
    w.end(new HashMap<String, String>());
    logger.debug("Finished generating parquet file {}", path.getName());
  }

}
