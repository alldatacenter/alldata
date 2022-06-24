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

package za.co.absa.spline.persistence.api.composition

import java.util.UUID

import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{AsyncFunSpec, Matchers}
import za.co.absa.spline.model.PersistedDatasetDescriptor
import za.co.absa.spline.persistence.api.DataLineageReader

import scala.concurrent.{ExecutionContext, Future}

class ParallelCompositeDataLineageReaderSpec() extends AsyncFunSpec with MockitoSugar with Matchers {

  override implicit def executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  describe("getDatasetDescriptor()") {
    it("first result should be returned") {
      val reader1 = mock[DataLineageReader]
      when(reader1.getDatasetDescriptor(any[UUID])(any())).thenReturn(Future {
        Thread.sleep(10000)
        PersistedDatasetDescriptor(UUID.randomUUID(), "1", "x", "file://test", 1)
      })

      val reader2 = mock[DataLineageReader]
      when(reader2.getDatasetDescriptor(any[UUID])(any())).thenReturn(Future {
        PersistedDatasetDescriptor(UUID.randomUUID(), "2", "x", "file://test", 1)
      })

      val compositeDataLineageReader = new ParallelCompositeDataLineageReader(Seq(reader1, reader2))
      compositeDataLineageReader.getDatasetDescriptor(UUID.randomUUID())
          .map(_.appId shouldEqual "2")
    }
  }
}
