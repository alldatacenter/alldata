/*
 * Copyright 2021 ABSA Group Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package za.co.absa.spline.producer.modelmapper.v1.spark

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import za.co.absa.spline.producer.model.v1_1.{AttrOrExprRef, FunctionalExpression, Literal}
import za.co.absa.spline.producer.modelmapper.v1.FieldNamesV1
import za.co.absa.spline.producer.modelmapper.v1.TypesV1.ExprDef

class SparkSplineExpressionConverterSpec extends AnyFlatSpec with Matchers {

  behavior of "convert()"

  it should "convert Literal" in {
    val converter = new SparkSplineExpressionConverter(null)

    val lit = converter.convert(Map(
      "_typeHint" -> "expr.Literal",
      "dataTypeId" -> "d32cfc1f-e90f-4ece-93f5-15193534c855",
      "value" -> "foo"
    ))

    lit should be(a[Literal])
    lit.asInstanceOf[Literal].id should not be empty
    lit.asInstanceOf[Literal].value should be(a[String])
    lit.asInstanceOf[Literal].value should equal("foo")
    lit.asInstanceOf[Literal].dataType should equal(Some("d32cfc1f-e90f-4ece-93f5-15193534c855"))
    lit.asInstanceOf[Literal].extra should equal(Map("_typeHint" -> "expr.Literal"))
  }

  it should "support missing Literal.value" in {
    val converter = new SparkSplineExpressionConverter(null)

    val lit = converter.convert(Map("_typeHint" -> "expr.Literal"))

    lit should be(a[Literal])
    assert(lit.asInstanceOf[Literal].value == null)
    assert(lit.asInstanceOf[Literal].dataType.isEmpty)
  }

  it should "convert Binary expression" in {
    val converter = new SparkSplineExpressionConverter(new AttributeRefConverter {
      override def isAttrRef(obj: Any): Boolean = false

      override def convert(arg: ExprDef): AttrOrExprRef = null
    })

    val binExpr = converter.convert(Map(
      "_typeHint" -> "expr.Binary",
      "dataTypeId" -> "d32cfc1f-e90f-4ece-93f5-15193534c855",
      "symbol" -> "+",
      "children" -> Seq(
        Map("_typeHint" -> "expr.Literal"),
        Map("_typeHint" -> "expr.Literal"),
      )
    ))

    binExpr should be(a[FunctionalExpression])
    binExpr.asInstanceOf[FunctionalExpression].id should not be empty
    binExpr.asInstanceOf[FunctionalExpression].childRefs should have length 2
    binExpr.asInstanceOf[FunctionalExpression].name should equal("+")
    binExpr.asInstanceOf[FunctionalExpression].dataType should equal(Some("d32cfc1f-e90f-4ece-93f5-15193534c855"))
    binExpr.asInstanceOf[FunctionalExpression].params should be(empty)
    binExpr.asInstanceOf[FunctionalExpression].extra should equal(Map(
      "_typeHint" -> "expr.Binary",
      "symbol" -> "+"
    ))
  }

  it should "convert Alias expression" in {
    val converter = new SparkSplineExpressionConverter(new AttributeRefConverter {
      override def isAttrRef(obj: Any): Boolean = false

      override def convert(arg: ExprDef): AttrOrExprRef = null
    })

    val aliasExpr = converter.convert(Map(
      "_typeHint" -> "expr.Alias",
      "dataTypeId" -> "d32cfc1f-e90f-4ece-93f5-15193534c855",
      "alias" -> "foo",
      "child" -> Map("_typeHint" -> "expr.Literal"),
    ))

    aliasExpr should be(a[FunctionalExpression])
    aliasExpr.asInstanceOf[FunctionalExpression].id should not be empty
    aliasExpr.asInstanceOf[FunctionalExpression].childRefs should have length 1
    aliasExpr.asInstanceOf[FunctionalExpression].name should equal("alias")
    aliasExpr.asInstanceOf[FunctionalExpression].dataType should equal(Some("d32cfc1f-e90f-4ece-93f5-15193534c855"))
    aliasExpr.asInstanceOf[FunctionalExpression].params should equal(Map(
      "name" -> "foo"
    ))
    aliasExpr.asInstanceOf[FunctionalExpression].extra should equal(Map(
      "_typeHint" -> "expr.Alias"
    ))
  }

  it should "convert UDF expression" in {
    val converter = new SparkSplineExpressionConverter(new AttributeRefConverter {
      override def isAttrRef(obj: Any): Boolean = false

      override def convert(arg: ExprDef): AttrOrExprRef = null
    })

    val udfExpr = converter.convert(Map(
      "_typeHint" -> "expr.UDF",
      "dataTypeId" -> "d32cfc1f-e90f-4ece-93f5-15193534c855",
      "name" -> "foo"
    ))

    udfExpr should be(a[FunctionalExpression])
    udfExpr.asInstanceOf[FunctionalExpression].id should not be empty
    udfExpr.asInstanceOf[FunctionalExpression].childRefs should be(empty)
    udfExpr.asInstanceOf[FunctionalExpression].name should equal("foo")
    udfExpr.asInstanceOf[FunctionalExpression].dataType should equal(Some("d32cfc1f-e90f-4ece-93f5-15193534c855"))
    udfExpr.asInstanceOf[FunctionalExpression].params should be(empty)
    udfExpr.asInstanceOf[FunctionalExpression].extra should equal(Map(
      "_typeHint" -> "expr.UDF"
    ))
  }

  it should "convert Generic expression" in {
    val converter = new SparkSplineExpressionConverter(new AttributeRefConverter {
      override def isAttrRef(obj: Any): Boolean = false

      override def convert(arg: ExprDef): AttrOrExprRef = null
    })

    val genExpr = converter.convert(Map(
      "_typeHint" -> "expr.Generic",
      "dataTypeId" -> "d32cfc1f-e90f-4ece-93f5-15193534c855",
      "name" -> "foo",
      "exprType" -> "bar",
      "params" -> Map("a" -> 111, "b" -> 222)
    ))

    genExpr should be(a[FunctionalExpression])
    genExpr.asInstanceOf[FunctionalExpression].id should not be empty
    genExpr.asInstanceOf[FunctionalExpression].childRefs should be(empty)
    genExpr.asInstanceOf[FunctionalExpression].name should equal("foo")
    genExpr.asInstanceOf[FunctionalExpression].dataType should equal(Some("d32cfc1f-e90f-4ece-93f5-15193534c855"))
    genExpr.asInstanceOf[FunctionalExpression].params should equal(Map(
      "a" -> 111,
      "b" -> 222
    ))
    genExpr.asInstanceOf[FunctionalExpression].extra should equal(Map(
      "_typeHint" -> "expr.Generic",
      "simpleClassName" -> "bar",
    ))
  }
}
