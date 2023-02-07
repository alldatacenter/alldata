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
package org.apache.drill.exec.fn.hive;

import org.apache.hadoop.hive.common.type.HiveChar;
import org.apache.hadoop.hive.common.type.HiveDecimal;
import org.apache.hadoop.hive.common.type.HiveVarchar;
import org.apache.hadoop.hive.ql.exec.Description;
import org.apache.hadoop.hive.ql.exec.UDFArgumentException;
import org.apache.hadoop.hive.ql.exec.UDFArgumentLengthException;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.ql.udf.generic.GenericUDF;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector.Category;
import org.apache.hadoop.hive.serde2.objectinspector.PrimitiveObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.PrimitiveObjectInspector.PrimitiveCategory;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.BinaryObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.BooleanObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.ByteObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.DateObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.DoubleObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.FloatObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.HiveCharObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.HiveDecimalObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.HiveVarcharObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.IntObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.LongObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.PrimitiveObjectInspectorFactory;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.PrimitiveObjectInspectorUtils;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.ShortObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.StringObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.TimestampObjectInspector;

/**
 * Contains test Hive UDF that take a particular data type and return the same data type and value.
 * These test UDFs exercise the code paths in Drill ObjectInspectors and parsing the return value from Hive UDF
 * for all supported data types in Hive UDF.
 */
public class HiveTestUDFImpls {
  public static abstract class GenericUDFTestBase extends GenericUDF {
    protected final String udfName;
    protected final PrimitiveCategory inputType;
    protected final PrimitiveCategory outputType;
    protected ObjectInspector argumentOI;

    public GenericUDFTestBase(String udfName, PrimitiveCategory type) {
      this(udfName, type, type);
    }

    public GenericUDFTestBase(String udfName, PrimitiveCategory inputType, PrimitiveCategory outputType) {
      this.udfName = udfName;
      this.inputType = inputType;
      this.outputType = outputType;
    }

    @Override
    public ObjectInspector initialize(ObjectInspector[] arguments) throws UDFArgumentException {
      if (arguments.length != 1) {
        throw new UDFArgumentLengthException(String.format("%s needs 1 argument, got %d", udfName, arguments.length));
      }

      if (arguments[0].getCategory() != Category.PRIMITIVE ||
          ((PrimitiveObjectInspector) arguments[0]).getPrimitiveCategory() != inputType) {
        String actual = arguments[0].getCategory() + (arguments[0].getCategory() == Category.PRIMITIVE ?
            "[" + ((PrimitiveObjectInspector) arguments[0]).getPrimitiveCategory() + "]" : "");
        throw new UDFArgumentException(
            String.format("%s only takes primitive type %s, got %s", udfName, inputType, actual));
      }
      argumentOI = arguments[0];
      return PrimitiveObjectInspectorFactory.getPrimitiveJavaObjectInspector(outputType);
    }

    @Override
    public String getDisplayString(String[] children) {
      StringBuilder sb = new StringBuilder();
      sb.append(udfName);
      sb.append("(");
      if (children.length > 0) {
        sb.append(children[0]);
        for (int i = 1; i < children.length; i++) {
          sb.append(",");
          sb.append(children[i]);
        }
      }
      sb.append(")");
      return sb.toString();
    }

    @Override
    public Object evaluate(DeferredObject[] arguments) throws HiveException {
      if (arguments[0] == null || arguments[0].get() == null) {
        return null;
      }

      Object input = arguments[0].get();
      switch(inputType) {
        case BOOLEAN:
          return ((BooleanObjectInspector)argumentOI).get(input) ? Boolean.TRUE : Boolean.FALSE;
        case BYTE:
          return Byte.valueOf(((ByteObjectInspector)argumentOI).get(input));
        case SHORT:
          return Short.valueOf(((ShortObjectInspector)argumentOI).get(input));
        case INT:
          return Integer.valueOf(((IntObjectInspector)argumentOI).get(input));
        case LONG:
          return Long.valueOf(((LongObjectInspector)argumentOI).get(input));
        case FLOAT:
          return Float.valueOf(((FloatObjectInspector)argumentOI).get(input));
        case DOUBLE:
          return Double.valueOf(((DoubleObjectInspector)argumentOI).get(input));
        case STRING:
          return PrimitiveObjectInspectorUtils.getString(input, (StringObjectInspector)argumentOI);
        case BINARY:
          return PrimitiveObjectInspectorUtils.getBinary(input, (BinaryObjectInspector) argumentOI).getBytes();
        case VARCHAR:
          if (outputType == PrimitiveCategory.CHAR) {
            HiveVarchar hiveVarchar = PrimitiveObjectInspectorUtils.getHiveVarchar(input, (HiveVarcharObjectInspector) argumentOI);
            return new HiveChar(hiveVarchar.getValue(), HiveChar.MAX_CHAR_LENGTH);
          } else {
            return PrimitiveObjectInspectorUtils.getHiveVarchar(input, (HiveVarcharObjectInspector)argumentOI);
          }
        case CHAR:
          return PrimitiveObjectInspectorUtils.getHiveChar(input, (HiveCharObjectInspector) argumentOI);
        case DATE:
          return PrimitiveObjectInspectorUtils.getDate(input, (DateObjectInspector) argumentOI);
        case TIMESTAMP:
          return PrimitiveObjectInspectorUtils.getTimestamp(input, (TimestampObjectInspector) argumentOI);
        case DECIMAL:
          // return type is a HiveVarchar
          HiveDecimal decimalValue =
              PrimitiveObjectInspectorUtils.getHiveDecimal(input, (HiveDecimalObjectInspector) argumentOI);
          return new HiveVarchar(decimalValue.toString(), HiveVarchar.MAX_VARCHAR_LENGTH);
      }

      throw new UnsupportedOperationException(String.format("Unexpected input type '%s' in Test UDF", inputType));
    }
  }

  @Description(name = "testHiveUDFBOOLEAN", value = "_FUNC_(BOOLEAN) - Tests boolean data as input and output")
  public static class GenericUDFTestBOOLEAN extends GenericUDFTestBase {
    public GenericUDFTestBOOLEAN() {
      super("testHiveUDFBOOLEAN", PrimitiveCategory.BOOLEAN);
    }
  }

  // TODO(DRILL-2470) - re-enable the test case for this function in TestSampleHiveUDFs
  @Description(name = "testHiveUDFBYTE", value = "_FUNC_(BYTE) - Tests byte data as input and output")
  public static class GenericUDFTestBYTE extends GenericUDFTestBase {
    public GenericUDFTestBYTE() {
      super("testHiveUDFBYTE", PrimitiveCategory.BYTE);
    }
  }

  // TODO(DRILL-2470) - re-enable the test case for this function in TestSampleHiveUDFs
  @Description(name = "testHiveUDFSHORT", value = "_FUNC_(SHORT) - Tests short data as input and output")
  public static class GenericUDFTestSHORT extends GenericUDFTestBase {
    public GenericUDFTestSHORT() {
      super("testHiveUDFSHORT", PrimitiveCategory.SHORT);
    }
  }

  @Description(name = "testHiveUDFINT", value = "_FUNC_(INT) - Tests int data as input and output")
  public static class GenericUDFTestINT extends GenericUDFTestBase {
    public GenericUDFTestINT() {
      super("testHiveUDFINT", PrimitiveCategory.INT);
    }
  }

  @Description(name = "testHiveUDFLONG", value = "_FUNC_(LONG) - Tests long data as input and output")
  public static class GenericUDFTestLONG extends GenericUDFTestBase {
    public GenericUDFTestLONG() {
      super("testHiveUDFLONG", PrimitiveCategory.LONG);
    }
  }

  @Description(name = "testHiveUDFFLOAT", value = "_FUNC_(FLOAT) - Tests float data as input and output")
  public static class GenericUDFTestFLOAT extends GenericUDFTestBase {
    public GenericUDFTestFLOAT() {
      super("testHiveUDFFLOAT", PrimitiveCategory.FLOAT);
    }
  }

  @Description(name = "testHiveUDFDOUBLE", value = "_FUNC_(DOUBLE) - Tests double data as input and output")
  public static class GenericUDFTestDOUBLE extends GenericUDFTestBase {
    public GenericUDFTestDOUBLE() {
      super("testHiveUDFDOUBLE", PrimitiveCategory.DOUBLE);
    }
  }

  @Description(name = "testHiveUDFVARCHAR", value = "_FUNC_(VARCHAR) - Tests varchar data as input and output")
  public static class GenericUDFTestVARCHAR extends GenericUDFTestBase {
    public GenericUDFTestVARCHAR() {
      super("testHiveUDFVARCHAR", PrimitiveCategory.VARCHAR);
    }
  }

  @Description(name = "testHiveUDFCHAR", value = "_FUNC_(VARCHAR) - Tests varchar data as input and char data as output")
  public static class GenericUDFTestCHAR extends GenericUDFTestBase {
    public GenericUDFTestCHAR() {
      super("testHiveUDFCHAR", PrimitiveCategory.VARCHAR, PrimitiveCategory.CHAR);
    }
  }

  @Description(name = "testHiveUDFSTRING", value = "_FUNC_(STRING) - Tests string data as input and output")
  public static class GenericUDFTestSTRING extends GenericUDFTestBase {
    public GenericUDFTestSTRING() {
      super("testHiveUDFSTRING", PrimitiveCategory.STRING);
    }
  }

  @Description(name = "testHiveUDFBINARY", value = "_FUNC_(BINARY) - Tests binary data as input and output")
  public static class GenericUDFTestBINARY extends GenericUDFTestBase {
    public GenericUDFTestBINARY() {
      super("testHiveUDFBINARY", PrimitiveCategory.BINARY);
    }
  }

  @Description(name = "testHiveUDFTIMESTAMP", value = "_FUNC_(TIMESTAMP) - Tests timestamp data as input and output")
  public static class GenericUDFTestTIMESTAMP extends GenericUDFTestBase {
    public GenericUDFTestTIMESTAMP() {
      super("testHiveUDFTIMESTAMP", PrimitiveCategory.TIMESTAMP);
    }
  }

  @Description(name = "testHiveUDFDATE", value = "_FUNC_(DATE) - Tests date data as input and output")
  public static class GenericUDFTestDATE extends GenericUDFTestBase {
    public GenericUDFTestDATE() {
      super("testHiveUDFDATE", PrimitiveCategory.DATE);
    }
  }

  @Description(name = "testHiveUDFDECIMAL", value = "_FUNC_(DECIMAL) - Tests decimal data as input and output")
  public static class GenericUDFTestDECIMAL extends GenericUDFTestBase {
    public GenericUDFTestDECIMAL() {
      super("testHiveUDFDECIMAL", PrimitiveCategory.DECIMAL, PrimitiveCategory.VARCHAR);
    }
  }
}