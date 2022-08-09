/*
 * Copyright 2020 ABSA Group Limited
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

package za.co.absa.spline.harvester.postprocessing

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import za.co.absa.spline.harvester.HarvestingContext
import za.co.absa.spline.producer.model.WriteOperation

class PostProcessorSpec extends AnyFlatSpec with Matchers with MockitoSugar {

  class UriAppendingPostProcessingFilterMock(str: String) extends AbstractPostProcessingFilter("Mock") {
    override def processWriteOperation(op: WriteOperation, ctx: HarvestingContext): WriteOperation =
      op.copy(outputSource = op.outputSource + str)
  }

  private val wop = WriteOperation("foo", append = false, "42", None, Seq.empty, None, None)

  it should "apply one filter" in {

    val filter = new UriAppendingPostProcessingFilterMock("@")
    val pp = new PostProcessor(Seq(filter), mock[HarvestingContext])

    val filteredOp = pp.process(wop)

    filteredOp.outputSource shouldEqual "foo@"
  }

  it should "apply filter chain in correct order" in {

    val filters = Seq(
      new UriAppendingPostProcessingFilterMock("@"),
      new UriAppendingPostProcessingFilterMock("#"),
      new UriAppendingPostProcessingFilterMock("%")
    )

    val pp = new PostProcessor(filters, mock[HarvestingContext])

    val filteredOp = pp.process(wop)

    filteredOp.outputSource shouldEqual "foo@#%"
  }

}
