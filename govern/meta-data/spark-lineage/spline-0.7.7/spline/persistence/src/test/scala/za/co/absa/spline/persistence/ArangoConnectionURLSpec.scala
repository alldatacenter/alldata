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

package za.co.absa.spline.persistence

import ArangoConnectionURL.{ArangoDbScheme, ArangoSecureDbScheme}

import java.net.MalformedURLException

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ArangoConnectionURLSpec extends AnyFlatSpec with Matchers {

  behavior of "URL parser"

  it should "parse ArangoDB connection URL without port number" in {
    val url = ArangoConnectionURL("arangodb://my.host.com/foo-bar_db")
    url.hosts shouldEqual Seq("my.host.com" -> 8529)
    url.dbName shouldEqual "foo-bar_db"
  }

  it should "parse ArangoDB secure connection URL" in {
    val url = ArangoConnectionURL("arangodbs://my.host.com/foo-bar_db")
    url.scheme shouldEqual ArangoSecureDbScheme
  }

  it should "parse ArangoDB connection URL with port number" in {
    val url = ArangoConnectionURL("arangodb://my.host.com:1234/foo-bar_db")
    url.hosts shouldEqual Seq("my.host.com" -> 1234)
    url.dbName shouldEqual "foo-bar_db"
  }

  it should "parse 'MongoDB Connection String' style comma separated 'host:port' list" in {
    val url = ArangoConnectionURL("arangodb://user-123@my.host1:123,my.host2:456/foo-bar_db")
    url.hosts shouldEqual Seq("my.host1" -> 123, "my.host2" -> 456)
  }

  it should "parse ArangoDB connection URL with user and empty password" in {
    val url = ArangoConnectionURL("arangodb://user-123@my.host.com/foo-bar_db")
    url.hosts shouldEqual Seq("my.host.com" -> 8529)
    url.dbName shouldEqual "foo-bar_db"
    url.user shouldEqual Some("user-123")
  }

  it should "parse ArangoDB connection URL with user and password" in {
    val url = ArangoConnectionURL("arangodb://user-123:this:is@my_sup&r~5a$$w)rd@my.host.com/foo-bar_db")
    url.hosts shouldEqual Seq("my.host.com" -> 8529)
    url.dbName shouldEqual "foo-bar_db"
    url.user shouldEqual Some("user-123")
    url.password shouldEqual Some("this:is@my_sup&r~5a$$w)rd")
  }

  it should "fail on null or malformed connectionUrl" in {
    a[MalformedURLException] should be thrownBy ArangoConnectionURL(null)
    a[MalformedURLException] should be thrownBy ArangoConnectionURL("bull**it")
  }

  it should "fail on missing host, port or database name" in {
    a[MalformedURLException] should be thrownBy ArangoConnectionURL("arangodb://my.host.com:1234")
  }

  behavior of "asString()"

  it should "compose equivalent representation of the input" in {
    ArangoConnectionURL(ArangoDbScheme, None, None, Seq("ip1" -> 11, "ip2" -> 22), "test").asString shouldEqual "arangodb://ip1:11,ip2:22/test"
    ArangoConnectionURL(ArangoDbScheme, Some("alice"), None, Seq("host" -> 42), "test").asString shouldEqual "arangodb://alice@host:42/test"
  }

  it should "hide user password" in {
    ArangoConnectionURL(ArangoDbScheme, Some("bob"), Some("secret"), Seq("host" -> 42), "test").asString shouldEqual "arangodb://bob:*****@host:42/test"
  }

}
