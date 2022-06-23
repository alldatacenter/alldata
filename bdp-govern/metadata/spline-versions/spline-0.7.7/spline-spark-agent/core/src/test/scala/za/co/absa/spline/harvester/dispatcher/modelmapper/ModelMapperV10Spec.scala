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

package za.co.absa.spline.harvester.dispatcher.modelmapper

import org.mockito.Mockito._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import za.co.absa.spline.producer.dto.v1_0
import za.co.absa.spline.producer.model._

import java.util.UUID

class ModelMapperV10Spec
  extends AnyFlatSpec
    with Matchers
    with MockitoSugar {

  private val mapper = ModelMapperV10

  behavior of "toDTO()"

  it should "convert ExecutionPlan entity to DTO ver 1.0" in {

    val dummyDataType = new Object()

    val planEntity = ExecutionPlan(
      id = Some(UUID.fromString("00000000-0000-0000-0000-000000000000")),
      name = Some("Foo Plan"),
      discriminator = None,
      labels = Some(Map("lbl1" -> Seq("a", "b"))),
      operations = Operations(
        write = WriteOperation(
          outputSource = "aaa",
          append = true,
          id = "op-0",
          name = Some("Write Operation"),
          childIds = Seq("op-1"),
          params = Some(Map("param1" -> 42)),
          extra = Some(Map("extra1" -> 42))
        ),
        reads = Some(Seq(ReadOperation(
          inputSources = Seq("bbb"),
          id = "op-2",
          name = Some("Read Operation"),
          output = Some(Seq("attr-1", "attr-2")),
          params = Some(Map("param2" -> 42)),
          extra = Some(Map("extra2" -> 42))
        ))),
        other = Some(Seq(DataOperation(
          id = "op-1",
          name = Some("Data Operation"),
          childIds = Some(Seq("op-2")),
          output = Some(Seq("attr-3")),
          params = Some(Map("param3" -> 42)),
          extra = Some(Map("extra3" -> 42))
        )))
      ),
      attributes = Some(Seq(
        Attribute(
          id = "attr-1",
          dataType = Some(dummyDataType),
          childRefs = None,
          extra = None,
          name = "A"
        ),
        Attribute(
          id = "attr-2",
          dataType = Some(dummyDataType),
          childRefs = None,
          extra = None,
          name = "B"
        ),
        Attribute(
          id = "attr-3",
          dataType = Some(dummyDataType),
          childRefs = None,
          extra = None,
          name = "C"
        )
      )),
      expressions = Some(Expressions(
        functions = Some(Seq(
          FunctionalExpression(
            id = "e1",
            dataType = Some(dummyDataType),
            childRefs = Some(Seq(AttrOrExprRef(Some("a1"), None))),
            extra = Some(Map("extra_e1" -> 777)),
            name = "Expr1",
            params = None
          )
        )),
        constants = Some(Seq(
          Literal(
            id = "c1",
            dataType = None,
            extra = None,
            value = "forty two"
          )
        ))
      )),
      systemInfo = NameAndVersion("xxx", "777"),
      agentInfo = Some(NameAndVersion("yyy", "777")),
      extraInfo = Some(Map("extra42" -> 42))
    )

    val planDTO = v1_0.ExecutionPlan(
      id = Some(UUID.fromString("00000000-0000-0000-0000-000000000000")),
      operations = v1_0.Operations(
        write = v1_0.WriteOperation(
          outputSource = "aaa",
          append = true,
          id = 0,
          childIds = Seq(1),
          params = Some(Map("param1" -> 42)),
          extra = Some(Map(
            "extra1" -> 42,
            "name" -> "Write Operation"
          )),
          schema = None
        ),
        reads = Some(Seq(v1_0.ReadOperation(
          inputSources = Seq("bbb"),
          id = 2,
          childIds = Nil,
          schema = Some(Seq("attr-1", "attr-2")),
          params = Some(Map("param2" -> 42)),
          extra = Some(Map(
            "extra2" -> 42,
            "name" -> "Read Operation"
          ))
        ))),
        other = Some(Seq(v1_0.DataOperation(
          id = 1,
          childIds = Some(Seq(2)),
          schema = Some(Seq("attr-3")),
          params = Some(Map("param3" -> 42)),
          extra = Some(Map(
            "extra3" -> 42,
            "name" -> "Data Operation"
          ))
        )))
      ),
      systemInfo = v1_0.SystemInfo("xxx", "777"),
      agentInfo = Some(v1_0.AgentInfo("yyy", "777")),
      extraInfo = Some(Map(
        "extra42" -> 42,
        "appName" -> "Foo Plan",
        "attributes" -> Seq(
          Map("id" -> "attr-1", "name" -> "A", "dataTypeId" -> Some(dummyDataType)),
          Map("id" -> "attr-2", "name" -> "B", "dataTypeId" -> Some(dummyDataType)),
          Map("id" -> "attr-3", "name" -> "C", "dataTypeId" -> Some(dummyDataType))
        )))
    )

    mapper.toDTO(planEntity) shouldEqual Some(planDTO)
  }

  it should "convert ExecutionEvent entity to DTO ver 1.0" in {
    val eventEntity = ExecutionEvent(
      planId = UUID.fromString("00000000-0000-0000-0000-000000000000"),
      discriminator = Some("foo"),
      labels = Some(Map("lbl1" -> Seq("a", "b"))),
      timestamp = 123456789,
      durationNs = Some(555),
      error = None,
      extra = Some(Map("extra1" -> 42))
    )

    val eventDTO = v1_0.ExecutionEvent(
      planId = UUID.fromString("00000000-0000-0000-0000-000000000000"),
      timestamp = 123456789,
      error = None,
      extra = Some(Map(
        "extra1" -> 42,
        "durationNs" -> 555
      ))
    )

    mapper.toDTO(eventEntity) shouldEqual Some(eventDTO)
  }

  it should "skip failed events" in {
    val mockEvent = mock[ExecutionEvent]
    when(mockEvent.error) thenReturn Some("dummy error")

    mapper.toDTO(mockEvent) shouldEqual None
  }
}
