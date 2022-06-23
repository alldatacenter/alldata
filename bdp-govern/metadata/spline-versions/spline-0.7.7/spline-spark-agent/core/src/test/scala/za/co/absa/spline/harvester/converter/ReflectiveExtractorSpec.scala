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

package za.co.absa.spline.harvester.converter

import org.apache.hadoop.classification.InterfaceAudience
import org.apache.hadoop.io.Text
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar

class ReflectiveExtractorSpec extends AnyFlatSpec with MockitoSugar with Matchers {

  import za.co.absa.spline.harvester.converter.ReflectiveExtractorSpec._


  behavior of "ReflectiveExtractor.extractProperties()"

  it should "extract properties of object" in {

    val map = ReflectiveExtractor.extractProperties(Foo(42, "answer"))

    map("x") shouldBe 42
    map("y") shouldBe "answer"
  }

  it should "not fail on Scala bug #12190" in {
    val objectWithCyclicAnnotation = new Text("")
    ReflectiveExtractor.extractProperties(objectWithCyclicAnnotation)
  }

  it should "extract properties of object with cyclic annotation" in {
    val map = ReflectiveExtractor.extractProperties(CyclicFoo(42, "answer"))

    map("x") shouldBe 42
    map("y") shouldBe "answer"
  }
}

object ReflectiveExtractorSpec {

  case class Foo(x: Int, y: String)

  @InterfaceAudience.Public
  case class CyclicFoo(x: Int, y: String)

}
