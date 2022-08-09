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

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar

class OpenLineageUriMapperSpec
  extends AnyFlatSpec
    with Matchers
    with MockitoSugar {

  private val mapper = OpenLineageUriMapper

  behavior of "uriToNamespaceAndName(...)"

  it should "convert file uri" in {
    val(namespace, name) = mapper.uriToNamespaceAndName("file://localhost/absolute/path/to/file.csv")
    namespace should be("file://localhost")
    name should be("/absolute/path/to/file.csv")
  }

  it should "convert file uri without host" in {
    val(namespace, name) = mapper.uriToNamespaceAndName("file:///absolute/path/to/file.csv")
    namespace should be("file")
    name should be("/absolute/path/to/file.csv")
  }

  it should "convert jdbc postgresql uri" in {
    val(namespace, name) = mapper.uriToNamespaceAndName("jdbc:postgresql://localhost:5433/postgres:bar1")
    namespace should be("postgresql://localhost:5433")
    name should be("/postgres:bar1")
  }

  it should "convert postgresql uri" in {
    val(namespace, name) = mapper.uriToNamespaceAndName("postgresql://localhost:5433/postgres:bar1")
    namespace should be("postgresql://localhost:5433")
    name should be("/postgres:bar1")
  }

}
