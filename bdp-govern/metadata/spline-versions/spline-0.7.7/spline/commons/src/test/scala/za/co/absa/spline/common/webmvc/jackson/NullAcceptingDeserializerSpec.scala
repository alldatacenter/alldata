/*
 * Copyright 2021 ABSA Group Limited
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

package za.co.absa.spline.common.webmvc.jackson

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.{ObjectMapper, PropertyNamingStrategies}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.twitter.finatra.jackson.FinatraInternalModules
import com.twitter.finatra.jackson.caseclass.exceptions.CaseClassMappingException
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar

class NullAcceptingDeserializerSpec extends AnyFlatSpec with Matchers with MockitoSugar {

  import za.co.absa.spline.common.webmvc.jackson.NullAcceptingDeserializerSpec._

  behavior of "standard finatra deserializer"

  it should "throw on null by default" in {
    a[CaseClassMappingException] should be thrownBy {
      objectMapper.readValue("""{ "str" : null }""", classOf[Foo])
    }
  }

  behavior of "NullAcceptingDeserializer used via annotation"

  it should "work normally when value is present" in {
    val value = objectMapper.readValue("""{ "str" : "abc" }""", classOf[Bar])
    value.str shouldBe "abc"
  }

  it should "accept null values" in {
    val value = objectMapper.readValue("""{ "str" : null }""", classOf[Bar])
    value.str shouldBe null
  }

  it should "be able to deserialize complex types" in {
    val json = """{ "value" : { "a" : 42, "b" : "abc", "c" : [1,2,3] }}}"""
    val value = objectMapper.readValue(json, classOf[Baz])
    value.value shouldBe Map("a" -> 42, "b" -> "abc", "c" -> Seq(1,2,3))
  }

}

object NullAcceptingDeserializerSpec {

  private val objectMapper: ObjectMapper =
    JsonMapper.builder.build()
      .registerModule(DefaultScalaModule)
      .setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE)
      .registerModule(FinatraInternalModules.caseClassModule)

  private case class Foo (
    str: String
  )

  private case class Bar (
    @JsonDeserialize(using = classOf[NullAcceptingDeserializer])
    str: String
  )

  private case class Baz (
    @JsonDeserialize(using = classOf[NullAcceptingDeserializer])
    value: Any
  )
}
