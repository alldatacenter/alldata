/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.sql.execution.columnar

import java.nio.ByteBuffer

import scala.collection.mutable

import org.apache.spark.network.util.JavaUtils
import org.apache.spark.sql.catalyst.expressions.codegen.{CodeAndComment, CodeFormatter, CodeGenerator}
import org.apache.spark.sql.catalyst.expressions.codegen.GenerateUnsafeProjection.newCodeGenContext
import org.apache.spark.sql.types._
import org.slf4j.LoggerFactory

class RssColumnarBatchCodeGenBuild {

  private val logger = LoggerFactory.getLogger(classOf[RssColumnarBatchCodeGenBuild])

  def create(schema: StructType, batchSize: Int): RssBatchBuilder = {
    val ctx = newCodeGenContext()
    val codes = genCode(schema, batchSize)
    val codeBody =
      s"""
         |
         |public java.lang.Object generate(Object[] references) {
         |  return new SpecificRssColumnarBatchBuilder(references);
         |}
         |
         |class SpecificRssColumnarBatchBuilder extends ${classOf[RssBatchBuilder].getName} {
         |
         |  private Object[] references;
         |  int rowCnt = 0;
         |  ${codes._1}
         |
         |  public SpecificRssColumnarBatchBuilder(Object[] references) {
         |    this.references = references;
         |  }
         |
         |  public void newBuilders() throws Exception {
         |    rowCnt = 0;
         |    ${codes._2}
         |  }
         |
         |  public byte[] buildColumnBytes() throws Exception {
         |    int offset = 0;
         |    byte[] giantBuffer = new byte[totalSize];
         |    byte[] rowCntBytes = int2ByteArray(rowCnt);
         |    System.arraycopy(rowCntBytes, 0, giantBuffer, offset, rowCntBytes.length);
         |    offset += 4;
         |    ${codes._3}
         |    return giantBuffer;
         |  }
         |
         |  public void writeRow(InternalRow row) throws Exception {
         |    ${codes._4}
         |    rowCnt += 1;
         |  }
         |
         |  public int getRowCnt() {
         |    return rowCnt;
         |  }
         |}
       """.stripMargin

    val code = CodeFormatter.stripOverlappingComments(
      new CodeAndComment(codeBody, ctx.getPlaceHolderToComments()))

    logger.debug(s"\n${CodeFormatter.format(code)}")

    val (clazz, _) = CodeGenerator.compile(code)

    clazz.generate(ctx.references.toArray).asInstanceOf[RssBatchBuilder]
  }

  /**
   * @return (code to define columnBuilder, code to instantiate columnBuilder,
   *         code to build column bytes, code to write row to columnBuilder)
   */
  def genCode(schema: StructType, batchSize: Int): (
      mutable.StringBuilder,
      mutable.StringBuilder,
      mutable.StringBuilder,
      mutable.StringBuilder) = {
    val initCode = new mutable.StringBuilder()
    val buildCode = new mutable.StringBuilder()
    val writeCode = new mutable.StringBuilder()
    val writeRowCode = new mutable.StringBuilder()
    for (index <- schema.indices) {
      schema.fields(index).dataType match {
        case NullType =>
          initCode.append(
            s"""
               |  ${classOf[RssNullColumnBuilder].getName} b$index;
          """.stripMargin)
          buildCode.append(
            s"""
               |  b$index = new ${classOf[RssNullColumnBuilder].getName}();
               |  builder.initialize($batchSize, "${schema.fields(index).name}", false);
          """.stripMargin)
          writeCode.append(genWriteCode(index))
          writeRowCode.append(
            s"""
               |  b$index.appendFrom(row, $index);
          """.stripMargin)
        case ByteType =>
          initCode.append(
            s"""
               |  ${classOf[RssByteCodeGenColumnBuilder].getName} b$index;
          """.stripMargin)
          buildCode.append(
            s"""
               |  b$index = new ${classOf[RssByteCodeGenColumnBuilder].getName}();
               |  b$index.initialize($batchSize, "${schema.fields(index).name}", false);
          """.stripMargin)
          writeCode.append(genWriteCode(index))
          writeRowCode.append(
            s"""
               |  b$index.appendFrom(row, $index);
          """.stripMargin)
        case BooleanType =>
          initCode.append(
            s"""
               |  ${classOf[RssBooleanCodeGenColumnBuilder].getName} b$index;
          """.stripMargin)
          buildCode.append(
            s"""
               |  b$index  = new ${classOf[RssBooleanCodeGenColumnBuilder].getName}();
               |  b$index.initialize($batchSize, "${schema.fields(index).name}", false);
          """.stripMargin)
          writeCode.append(genWriteCode(index))
          writeRowCode.append(
            s"""
               |  b$index.appendFrom(row, $index);
          """.stripMargin)
        case ShortType =>
          initCode.append(
            s"""
               |  ${classOf[RssShortCodeGenColumnBuilder].getName} b$index;
          """.stripMargin)
          buildCode.append(
            s"""
               |  b$index = new ${classOf[RssShortCodeGenColumnBuilder].getName}();
               |  b$index.initialize($batchSize, "${schema.fields(index).name}", false);
          """.stripMargin)
          writeCode.append(genWriteCode(index))
          writeRowCode.append(
            s"""
               |  b$index.appendFrom(row, $index);
          """.stripMargin)
        case IntegerType =>
          initCode.append(
            s"""
               |  ${classOf[RssIntCodeGenColumnBuilder].getName} b$index;
          """.stripMargin)
          buildCode.append(
            s"""
               |  b$index = new ${classOf[RssIntCodeGenColumnBuilder].getName}();
               |  b$index.initialize($batchSize, "${schema.fields(index).name}", false);
          """.stripMargin)
          writeCode.append(genWriteCode(index))
          writeRowCode.append(
            s"""
               |  b$index.appendFrom(row, $index);
          """.stripMargin)
        case LongType =>
          initCode.append(
            s"""
               |  ${classOf[RssLongCodeGenColumnBuilder].getName} b$index;
          """.stripMargin)
          buildCode.append(
            s"""
               |  b$index = new ${classOf[RssLongCodeGenColumnBuilder].getName}();
               |  b$index.initialize($batchSize, "${schema.fields(index).name}", false);
          """.stripMargin)
          writeCode.append(genWriteCode(index))
          writeRowCode.append(
            s"""
               |  b$index.appendFrom(row, $index);
          """.stripMargin)
        case FloatType =>
          initCode.append(
            s"""
               |  ${classOf[RssFloatCodeGenColumnBuilder].getName} b$index;
          """.stripMargin)
          buildCode.append(
            s"""
               |  b$index = new ${classOf[RssFloatCodeGenColumnBuilder].getName}();
               |  b$index.initialize($batchSize, "${schema.fields(index).name}", false);
          """.stripMargin)
          writeCode.append(genWriteCode(index))
          writeRowCode.append(
            s"""
               |  b$index.appendFrom(row, $index);
          """.stripMargin)
        case DoubleType =>
          initCode.append(
            s"""
               |  ${classOf[RssDoubleCodeGenColumnBuilder].getName} b$index;
          """.stripMargin)
          buildCode.append(
            s"""
               |  b$index = new ${classOf[RssDoubleCodeGenColumnBuilder].getName}();
               |  b$index.initialize($batchSize, "${schema.fields(index).name}", false);
          """.stripMargin)
          writeCode.append(genWriteCode(index))
          writeRowCode.append(
            s"""
               |  b$index.appendFrom(row, $index);
          """.stripMargin)
        case StringType =>
          initCode.append(
            s"""
               |  ${classOf[RssStringCodeGenColumnBuilder].getName} b$index;
          """.stripMargin)
          buildCode.append(
            s"""
               |  b$index = new ${classOf[RssStringCodeGenColumnBuilder].getName}();
               |  b$index.initialize($batchSize, "${schema.fields(index).name}", false);
          """.stripMargin)
          writeCode.append(genWriteCode(index))
          writeRowCode.append(
            s"""
               |  b$index.appendFrom(row, $index);
          """.stripMargin)
        case dt: DecimalType if dt.precision <= Decimal.MAX_INT_DIGITS =>
          initCode.append(
            s"""
               |  ${classOf[RssCompactMiniDecimalCodeGenColumnBuilder].getName} b$index;
          """.stripMargin)
          buildCode.append(
            s"""
               |  b$index =
               |  new ${classOf[RssCompactMiniDecimalCodeGenColumnBuilder].getName}(
               |  new ${classOf[DecimalType].getName}(${dt.precision}, ${dt.scale}));
               |  b$index.initialize($batchSize, "${schema.fields(index).name}", false);
          """.stripMargin)
          writeCode.append(genWriteCode(index))
          writeRowCode.append(
            s"""
               |  b$index.appendFrom(row, $index);
          """.stripMargin)
        case dt: DecimalType if dt.precision <= Decimal.MAX_LONG_DIGITS =>
          initCode.append(
            s"""
               |  ${classOf[RssCompactDecimalCodeGenColumnBuilder].getName} b$index;
          """.stripMargin)
          buildCode.append(
            s"""
               |  b$index =
               |  new ${classOf[RssCompactDecimalCodeGenColumnBuilder].getName}
               |  (new ${classOf[DecimalType].getName}(${dt.precision}, ${dt.scale}));
               |  b$index.initialize($batchSize, "${schema.fields(index).name}", false);
          """.stripMargin)
          writeCode.append(genWriteCode(index))
          writeRowCode.append(
            s"""
               |  b$index.appendFrom(row, $index);
          """.stripMargin)
        case dt: DecimalType =>
          initCode.append(
            s"""
               |  ${classOf[RssDecimalCodeGenColumnBuilder].getName} b$index;
          """.stripMargin)
          buildCode.append(
            s"""
               |  b$index = new ${classOf[RssDecimalCodeGenColumnBuilder].getName}
               |  (new ${classOf[DecimalType].getName}(${dt.precision}, ${dt.scale}));
               |  b$index.initialize($batchSize, "${schema.fields(index).name}", false);
          """.stripMargin)
          writeCode.append(genWriteCode(index))
          writeRowCode.append(
            s"""
               |  b$index.appendFrom(row, $index);
          """.stripMargin)
      }
    }
    (initCode, buildCode, writeCode, writeRowCode)
  }

  def genWriteCode(index: Int): String = {
    s"""
       |  ${classOf[ByteBuffer].getName} buffers$index = b$index.build();
       |  byte[] bytes$index = ${classOf[JavaUtils].getName}.bufferToArray(buffers$index);
       |  byte[] columnBuilderBytes$index = int2ByteArray(bytes$index.length);
       |  System.arraycopy(columnBuilderBytes$index, 0, giantBuffer, offset, columnBuilderBytes$index.length);
       |  offset += 4;
       |  System.arraycopy(bytes$index, 0, giantBuffer, offset, bytes$index.length);
       |  offset += bytes$index.length;
          """.stripMargin
  }
}
