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

package za.co.absa.spline.persistence.model

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class DataSourceSpec extends AnyFlatSpec with Matchers {

  behavior of "DataSource.getName"

  it should "return last part of string between slashes" in {
    DataSource.getName("foo") shouldEqual "foo"
    DataSource.getName("/foo") shouldEqual "foo"
    DataSource.getName("foo/") shouldEqual "foo"
    DataSource.getName("/foo//") shouldEqual "foo"
    DataSource.getName("a://b/c/d/foo") shouldEqual "foo"
  }

  it should "return empty string when input string only contains slashes" in {
    DataSource.getName("///") shouldEqual ""
  }

  it should "return empty string when input string is empty" in {
    DataSource.getName("") shouldEqual ""
  }

}
