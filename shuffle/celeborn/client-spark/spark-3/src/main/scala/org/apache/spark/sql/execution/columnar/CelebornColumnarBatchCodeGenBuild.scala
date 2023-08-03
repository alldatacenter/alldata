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

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

import scala.collection.mutable

import org.apache.spark.network.util.JavaUtils
import org.apache.spark.sql.catalyst.expressions.codegen.{CodeAndComment, CodeFormatter, CodeGenerator}
import org.apache.spark.sql.catalyst.expressions.codegen.GenerateUnsafeProjection.newCodeGenContext
import org.apache.spark.sql.types._
import org.slf4j.LoggerFactory

class CelebornColumnarBatchCodeGenBuild {

  private val logger = LoggerFactory.getLogger(classOf[CelebornColumnarBatchCodeGenBuild])

  def create(schema: StructType, batchSize: Int): CelebornBatchBuilder = {
    val ctx = newCodeGenContext()
    val codes = genCode(schema, batchSize)
    val codeBody =
      s"""
         |
         |public java.lang.Object generate(Object[] references) {
         |  return new SpecificCelebornColumnarBatchBuilder(references);
         |}
         |
         |class SpecificCelebornColumnarBatchBuilder extends ${classOf[CelebornBatchBuilder].getName} {
         |
         |  private Object[] references;
         |  int rowCnt = 0;
         |  ${codes._1}
         |
         |  public SpecificCelebornColumnarBatchBuilder(Object[] references) {
         |    this.references = references;
         |  }
         |
         |  public void newBuilders() throws Exception {
         |    rowCnt = 0;
         |    ${codes._2}
         |  }
         |
         |  public byte[] buildColumnBytes() throws Exception {
         |    ${classOf[ByteArrayOutputStream].getName} giantBuffer = new ${classOf[
        ByteArrayOutputStream].getName}();
         |    byte[] rowCntBytes = int2ByteArray(rowCnt);
         |    giantBuffer.write(rowCntBytes);
         |    ${codes._3}
         |    return giantBuffer.toByteArray();
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

    clazz.generate(ctx.references.toArray).asInstanceOf[CelebornBatchBuilder]
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
               |  ${classOf[CelebornNullColumnBuilder].getName} b$index;
          """.stripMargin)
          buildCode.append(
            s"""
               |  b$index = new ${classOf[CelebornNullColumnBuilder].getName}();
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
               |  ${classOf[CelebornByteCodeGenColumnBuilder].getName} b$index;
          """.stripMargin)
          buildCode.append(
            s"""
               |  b$index = new ${classOf[CelebornByteCodeGenColumnBuilder].getName}();
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
               |  ${classOf[CelebornBooleanCodeGenColumnBuilder].getName} b$index;
          """.stripMargin)
          buildCode.append(
            s"""
               |  b$index  = new ${classOf[CelebornBooleanCodeGenColumnBuilder].getName}();
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
               |  ${classOf[CelebornShortCodeGenColumnBuilder].getName} b$index;
          """.stripMargin)
          buildCode.append(
            s"""
               |  b$index = new ${classOf[CelebornShortCodeGenColumnBuilder].getName}();
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
               |  ${classOf[CelebornIntCodeGenColumnBuilder].getName} b$index;
          """.stripMargin)
          buildCode.append(
            s"""
               |  b$index = new ${classOf[CelebornIntCodeGenColumnBuilder].getName}();
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
               |  ${classOf[CelebornLongCodeGenColumnBuilder].getName} b$index;
          """.stripMargin)
          buildCode.append(
            s"""
               |  b$index = new ${classOf[CelebornLongCodeGenColumnBuilder].getName}();
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
               |  ${classOf[CelebornFloatCodeGenColumnBuilder].getName} b$index;
          """.stripMargin)
          buildCode.append(
            s"""
               |  b$index = new ${classOf[CelebornFloatCodeGenColumnBuilder].getName}();
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
               |  ${classOf[CelebornDoubleCodeGenColumnBuilder].getName} b$index;
          """.stripMargin)
          buildCode.append(
            s"""
               |  b$index = new ${classOf[CelebornDoubleCodeGenColumnBuilder].getName}();
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
               |  ${classOf[CelebornStringCodeGenColumnBuilder].getName} b$index;
          """.stripMargin)
          buildCode.append(
            s"""
               |  b$index = new ${classOf[CelebornStringCodeGenColumnBuilder].getName}();
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
               |  ${classOf[CelebornCompactMiniDecimalCodeGenColumnBuilder].getName} b$index;
          """.stripMargin)
          buildCode.append(
            s"""
               |  b$index =
               |  new ${classOf[CelebornCompactMiniDecimalCodeGenColumnBuilder].getName}(
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
               |  ${classOf[CelebornCompactDecimalCodeGenColumnBuilder].getName} b$index;
          """.stripMargin)
          buildCode.append(
            s"""
               |  b$index =
               |  new ${classOf[CelebornCompactDecimalCodeGenColumnBuilder].getName}
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
               |  ${classOf[CelebornDecimalCodeGenColumnBuilder].getName} b$index;
          """.stripMargin)
          buildCode.append(
            s"""
               |  b$index = new ${classOf[CelebornDecimalCodeGenColumnBuilder].getName}
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
       |  giantBuffer.write(columnBuilderBytes$index);
       |  giantBuffer.write(bytes$index);
          """.stripMargin
  }
}
