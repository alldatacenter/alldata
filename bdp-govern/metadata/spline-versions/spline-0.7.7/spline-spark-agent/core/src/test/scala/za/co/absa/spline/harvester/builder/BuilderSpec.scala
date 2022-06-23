/*
 * Copyright 2019 ABSA Group Limited
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

package za.co.absa.spline.harvester.builder

import org.apache.spark.sql.catalyst.plans.logical.LogicalPlan
import org.mockito.AdditionalAnswers.returnsFirstArg
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import za.co.absa.spline.harvester.{IdGenerator, IdGeneratorsBundle}
import za.co.absa.spline.harvester.builder.read.{ReadCommand, ReadNodeBuilder}
import za.co.absa.spline.harvester.converter.{DataConverter, DataTypeConverter}
import za.co.absa.spline.harvester.postprocessing.PostProcessor
import za.co.absa.spline.producer.model.ReadOperation

class BuilderSpec extends AnyFlatSpec with Matchers with MockitoSugar {

  it should "not force lowercase on keys of the params Map when" in {
    val postProcessorMock = mock[PostProcessor]
    val logicalPlanStub = mock[LogicalPlan]
    val dataTypeConverterMock = mock[DataTypeConverter]
    val dataConverterMock = mock[DataConverter]

    val idGeneratorsMock = mock[IdGeneratorsBundle]
    val operationIdGeneratorMock = mock[IdGenerator[Any, String]]

    when(logicalPlanStub.output) thenReturn Seq.empty
    when(postProcessorMock.process(any[ReadOperation]())) thenAnswer returnsFirstArg()
    when(idGeneratorsMock.operationIdGenerator) thenReturn operationIdGeneratorMock

    val command = ReadCommand(
      SourceIdentifier(Some("CSV"), "whaateverpath"),
      logicalPlanStub,
      Map("caseSensitiveKey" -> "blabla")
    )

    val readNode =
      new ReadNodeBuilder(command)(idGeneratorsMock, dataTypeConverterMock, dataConverterMock, postProcessorMock)
        .build()

    readNode.params.get.keySet should contain("caseSensitiveKey")
    readNode.extra.get.keySet should contain("sourceType")
    readNode.params.get.keySet shouldNot contain("casesensitivekey")
    readNode.extra.get.keySet shouldNot contain("sourcetype")
  }

}
