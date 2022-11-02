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

import org.apache.commons.configuration.PropertiesConfiguration
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import za.co.absa.spline.harvester.HarvestingContext
import za.co.absa.spline.producer.model.{ReadOperation, WriteOperation}

class DataSourcePasswordReplacingFilterSpec extends AnyFlatSpec with Matchers with MockitoSugar {

  private val defaultProperties =
    new PropertiesConfiguration(getClass.getResource("/spline.default.properties"))
      .subset("spline.postProcessingFilter.dsPasswordReplace")

  private val dummyContext = mock[HarvestingContext]

  it should "mask secret in URL query parameter as `password=*****`" in { //NOSONAR
    val originalURL = "" +
      "jdbc:sqlserver://database.windows.net:1433" +
      ";user=sample" +
      ";password=12345" + //NOSONAR
      ";PASSPHRASE=" + //NOSONAR -- empty password is also a password
      ";encrypt=true" +
      ";trustServerCertificate=false" +
      ";hostNameInCertificate=*.database.windows.net" +
      ";loginTimeout=30:"

    val sanitizedURL = "" +
      "jdbc:sqlserver://database.windows.net:1433" +
      ";user=sample" +
      ";password=*****" + //NOSONAR <-- PASSWORD SHOULD BE SANITIZED
      ";PASSPHRASE=*****" + //NOSONAR <-- PASSWORD SHOULD BE SANITIZED
      ";encrypt=true" +
      ";trustServerCertificate=false" +
      ";hostNameInCertificate=*.database.windows.net" +
      ";loginTimeout=30:"

    val rop = ReadOperation(Seq(originalURL), "", None, None, None, None)
    val wop = WriteOperation(originalURL, append = false, "", None, Nil, None, None)

    val filter = new DataSourcePasswordReplacingFilter(defaultProperties)

    filter.processReadOperation(rop, dummyContext).inputSources shouldEqual Seq(sanitizedURL)
    filter.processWriteOperation(wop, dummyContext).outputSource shouldEqual sanitizedURL
  }

  it should "mask secret in URL userinfo as `user:*****@host`" in {
    val rop = ReadOperation(Seq("mongodb://bob:super_secret@mongodb.host.example.org:27017?authSource=admin"), "", None, None, None, None) //NOSONAR
    val wop = WriteOperation("mongodb://bob:@mongodb.host.example.org:27017?authSource=admin", append = false, "", None, Nil, None, None) //NOSONAR

    val filter = new DataSourcePasswordReplacingFilter(defaultProperties)

    filter.processReadOperation(rop, dummyContext).inputSources shouldEqual Seq("mongodb://bob:*****@mongodb.host.example.org:27017?authSource=admin") //NOSONAR
    filter.processWriteOperation(wop, dummyContext).outputSource shouldEqual "mongodb://bob:*****@mongodb.host.example.org:27017?authSource=admin" //NOSONAR
  }

  it should "mask secrets in 'params'" in {
    val originalParams = Map(
      "a" -> 42,
      "b" -> Seq(null),
      "c" -> None,
      "m" -> Map(
        "url" -> "jdbc:postgresql://bob:secret@someHost:somePort/someDB",
        "dbtable" -> "someTable",
        "user" -> "someUser",
        "PASSPHRASE" -> "somePassword" // NOSONAR
      ),
      "url" -> "jdbc:postgresql://bob:secret@someHost:somePort/someDB",
      "dbtable" -> "someTable",
      "user" -> "someUser",
      "password" -> "somePassword" // NOSONAR
    )

    val sanitizedParams = Map(
      "a" -> 42,
      "b" -> Seq(null),
      "c" -> None,
      "url" -> "jdbc:postgresql://bob:*****@someHost:somePort/someDB",
      "m" -> Map(
        "url" -> "jdbc:postgresql://bob:*****@someHost:somePort/someDB",
        "dbtable" -> "someTable",
        "user" -> "someUser",
        "PASSPHRASE" -> "*****" // NOSONAR
      ),
      "url" -> "jdbc:postgresql://bob:*****@someHost:somePort/someDB",
      "dbtable" -> "someTable",
      "user" -> "someUser",
      "password" -> "*****" // NOSONAR
    )

    val rop = ReadOperation(Nil, "", None, None, params = Some(originalParams), None)
    val wop = WriteOperation("", append = false, "", None, Nil, params = Some(originalParams), None)

    val filter = new DataSourcePasswordReplacingFilter(defaultProperties)

    filter.processReadOperation(rop, dummyContext).params shouldEqual Some(sanitizedParams)
    filter.processWriteOperation(wop, dummyContext).params shouldEqual Some(sanitizedParams)
  }

}
