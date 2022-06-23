/*
 * Copyright 2017 ABSA Group Limited
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

package za.co.absa.spline.harvester.converter

import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.catalyst.expressions.codegen.{CodegenContext, ExprCode}
import org.apache.spark.sql.catalyst.expressions.{CaseWhen, Expression, Literal}
import org.apache.spark.sql.types.DataTypes.NullType
import org.apache.spark.sql.types._
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{Inside, OneInstancePerTest}
import org.scalatestplus.mockito.MockitoSugar
import za.co.absa.spline.harvester.IdGenerator
import za.co.absa.spline.model.dt
import za.co.absa.spline.producer.model.FunctionalExpression

import java.util.UUID

class ExpressionConverterSpec extends AnyFlatSpec with OneInstancePerTest with MockitoSugar with Matchers with Inside {

  import ExpressionConverterSpec._

  behavior of "ExpressionConverter.convert()"

  behavior of "Converting arbitrary Spark Expression"

  private val dtConverterMock = mock[DataTypeConverter]
  private val exprToRefConverterMock = mock[ExprToRefConverter]
  private val idGeneratorMock = mock[IdGenerator[Any, String]]
  private val converter = new ExpressionConverter(idGeneratorMock, dtConverterMock, exprToRefConverterMock)

  when(dtConverterMock convert NullType -> true) thenReturn nullDataType
  when(dtConverterMock convert StringType -> false) thenReturn stringDataType
  when(idGeneratorMock.nextId(any())) thenReturn "some_id"

  it should "support secondary constructor, but only capture params from the primary one" in {
    val expression = new Foo("this parameter should not be captured")

    inside(converter.convert(expression)) {
      case FunctionalExpression(id, Some(dataTypeId), children, extra, name, params) =>
        id shouldEqual "some_id"
        name shouldEqual "foo"
        dataTypeId shouldEqual nullDataType.id
        children should be(None)
        extra.get should contain("_typeHint" -> "expr.Generic")
        params.get shouldNot contain key "aParamOfSecondaryConstructor"
    }
  }

  it should "ignore properties with null or None value" in {
    val expression = Foo.empty

    inside(converter.convert(expression)) {
      case fe: FunctionalExpression =>
        val params = fe.params.get
        params shouldNot contain key "otherExpression"
        params shouldNot contain key "string"
        params shouldNot contain key "javaInteger"
        params shouldNot contain key "any"
        params shouldNot contain key "seq"
        params shouldNot contain key "option"
        params shouldNot contain key "optionWithDefault"
    }
  }

  it should "support java boxed primitives as well as scala primitives" in {
    val expression = Foo.empty.copy(javaInteger = 1, scalaInt = 2)

    inside(converter.convert(expression)) {
      case fe: FunctionalExpression =>
        fe.params.get should contain allOf(
          "javaInteger" -> 1,
          "scalaInt" -> 2
        )
    }
  }

  it should "support params with default values" in {
    val expression = Foo(
      returnType = NullType,
      nullable = true,
      children = Nil,
      otherExpression = null,
      javaInteger = null,
      string = null,
      scalaInt = -1,
      option = None,
      any = null,
      seq = null
    )

    inside(converter.convert(expression)) {
      case fe: FunctionalExpression =>
        fe.params.get should contain allOf(
          "optionWithDefault" -> "this is a default value",
          "scalaIntWithDefault" -> 42
        )
    }
  }

  it should "support options, maps, sequences and their combinations" in {
    val expression = Foo.empty.copy(option =
      Some(Map(
        1 -> 10,
        777 -> Nil,
        2 -> Seq(None, 20),
        3 -> Map(
          42 -> Some(Some(Some(Some(42)))),
          777 -> None))))

    inside(converter.convert(expression)) {
      case fe: FunctionalExpression =>
        fe.params.get should contain("option" -> Map("1" -> 10, "2" -> Seq(null, 20), "3" -> Map("42" -> 42)))
    }
  }

  it should "support objects" in {
    val expression = Foo.empty.copy(any = Bar)

    inside(converter.convert(expression)) {
      case fe: FunctionalExpression =>
        fe.params.get should contain("any" -> "Bar")
    }
  }

  it should "support expressions" in {
    val expression = Foo.empty.copy(any = CaseWhen(Seq(Literal(42) -> Literal("Moo")), Literal("Meh")))

    inside(converter.convert(expression)) {
      case fe: FunctionalExpression =>
        fe.params.get should contain("any" -> "CASE WHEN 42 THEN Moo ELSE Meh END")
    }
  }

  it should "convert unknown objects to string" in {
    val expression = Foo.empty.copy(
      any = Bar,
      option = Some(new {
        override def toString = "blah"
      }),
      seq = Seq(Map(Some(Bar) -> new {
        override def toString = "blah"
      }))
    )

    inside(converter.convert(expression)) {
      case fe: FunctionalExpression =>
        fe.params.get should contain allOf(
          "any" -> "Bar",
          "option" -> "blah",
          "seq" -> Seq(Map("Some(this is some Bar object)" -> "blah"))
        )
    }
  }

  it should "not duplicate known properties in params" in {
    val expression = Foo.empty.copy(
      returnType = NullType,
      nullable = true,
      children = Seq(Literal("this is a child")),
      otherExpression = Literal("this isn't a child")
    )

    inside(converter.convert(expression)) {
      case fe: FunctionalExpression =>
        val params = fe.params.get
        params shouldNot contain key "nullable"

        params shouldNot contain key "dataType"
        params should contain("returnType" -> "NullType")

        params shouldNot contain key "children"
        params should contain("otherExpression" -> "this isn't a child")
    }
  }

  it should "ignore properties of type Expression that are referred from 'children'" in {
    val aChild = Literal("this is a child")
    val expression = Foo.empty.copy(
      children = Seq(aChild),
      otherExpression = aChild
    )

    inside(converter.convert(expression)) {
      case fe: FunctionalExpression =>
        fe.params.get shouldNot contain key "otherExpression"
    }
  }
}

object ExpressionConverterSpec {

  private val nullDataType = dt.Simple(UUID.randomUUID(), "Null", nullable = true)
  private val stringDataType = dt.Simple(UUID.randomUUID(), "String", nullable = false)

  case class Foo(
    returnType: DataType,
    nullable: Boolean,
    children: Seq[Expression],
    otherExpression: Expression,
    string: String,
    javaInteger: Integer,
    scalaInt: Int,
    scalaIntWithDefault: Int = 42,
    option: Option[Any],
    optionWithDefault: Option[Any] = Some("this is a default value"),
    any: Any,
    seq: Any
  ) extends Expression {

    def this(aParamOfSecondaryConstructor: String) = this(NullType, true, Nil, null, null, null, -1, -1, null, None, null, null)

    val additionalPropertyNotFromConstructor = "this should not be captured"

    override def dataType: DataType = returnType

    override def eval(input: InternalRow): Any = ()

    override def doGenCode(ctx: CodegenContext, ev: ExprCode): ExprCode = null

    protected def withNewChildrenInternal(newChildren: IndexedSeq[Expression]): Expression = copy(children = newChildren)
  }

  object Foo {
    val empty = new Foo("does not matter")
  }

  object Bar {
    override def toString: String = "this is some Bar object"
  }

}
