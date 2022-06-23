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

import org.apache.spark.sql.catalyst.expressions.Literal
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.OneInstancePerTest
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import za.co.absa.spline.harvester.IdGenerator
import za.co.absa.spline.model.dt

import java.util.UUID

class LiteralConverterSpec extends AnyFlatSpec with OneInstancePerTest with MockitoSugar with Matchers {

  behavior of "LiteralConverter.convert()"

  behavior of "Converting Spark Literals"

  private val dtConverterMock = mock[DataTypeConverter]
  private val idGeneratorMock = mock[IdGenerator[Any, String]]

  private val converter = new LiteralConverter(idGeneratorMock, new DataConverter, dtConverterMock)

  when(idGeneratorMock.nextId(any())).thenReturn("some_id")

  it should "support array of struct literals" in {
    val testLiteral = Literal.create(Array(
      Tuple2("a1", "b1"),
      Tuple2("a2", "b2")
    ))

    val dummyType = dt.Simple(UUID.randomUUID(), "dummy", nullable = false)
    when(dtConverterMock convert testLiteral.dataType -> false) thenReturn dummyType

    val literal = converter.convert(testLiteral)

    literal.id shouldEqual "some_id"
    literal.value shouldEqual Seq(Seq("a1", "b1"), Seq("a2", "b2"))
    literal.dataType shouldEqual Some(dummyType.id)
    literal.extra.get should contain("_typeHint" -> "expr.Literal")
  }

  it should "support array of struct of array of struct literals" in {
    val testLiteral = Literal.create(Array(
      Tuple2("row1", Array(
        Tuple3("a1", Some(true), Map("b1" -> 100)),
        Tuple3("c1", None, Map("d1" -> 200, "e1" -> 300))
      )),
      Tuple2("row2", Array(
        Tuple3("a2", Some(false), Map("b2" -> 400)),
        Tuple3("c2", None, Map("d2" -> 500, "e2" -> 600, "f2" -> 700))
      ))
    ))

    val dummyType = dt.Simple(UUID.randomUUID(), "dummy", nullable = false)
    when(dtConverterMock convert testLiteral.dataType -> false) thenReturn dummyType

    val literal = converter.convert(testLiteral)

    literal.id shouldEqual "some_id"
    literal.dataType shouldEqual Some(dummyType.id)
    literal.value should be {
      Seq(
        Seq("row1", Seq(
          Seq("a1", true, Map("b1" -> 100)),
          Seq("c1", null, Map("d1" -> 200, "e1" -> 300))
        )),
        Seq("row2", Seq(
          Seq("a2", false, Map("b2" -> 400)),
          Seq("c2", null, Map("d2" -> 500, "e2" -> 600, "f2" -> 700))
        ))
      )
    }
  }

}
