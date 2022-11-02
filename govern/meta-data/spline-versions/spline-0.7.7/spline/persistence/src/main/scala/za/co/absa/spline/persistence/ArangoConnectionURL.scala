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

import org.apache.commons.lang3.StringUtils.trimToNull
import za.co.absa.commons.lang.OptionImplicits.StringWrapper
import za.co.absa.spline.persistence.ArangoConnectionURL.ArangoSecureDbScheme

import java.net.MalformedURLException
import scala.util.matching.Regex

case class ArangoConnectionURL(scheme: String, user: Option[String], password: Option[String], hosts: Seq[(String, Int)], dbName: String) {

  import za.co.absa.commons.lang.OptionImplicits._

  require(user.isDefined || password.isEmpty, "user cannot be blank if password is specified")

  def asString: String = {
    val userInfo = trimToNull(Seq(user, password.map(_ => "*****")).flatten.mkString(":"))
    val commaSeparatedHostsString = hosts.map { case (host, port) => s"$host:$port" }.mkString(",")

    new StringBuilder()
      .append(s"$scheme://")
      .having(userInfo.nonBlankOption)(_ append _ append "@")
      .append(commaSeparatedHostsString)
      .append(s"/$dbName")
      .result()
  }

  def isSecure: Boolean = scheme == ArangoSecureDbScheme
}

object ArangoConnectionURL {

  private[persistence] val ArangoDbScheme = "arangodb"
  private[persistence] val ArangoSecureDbScheme = "arangodbs"
  private val DefaultPort = 8529

  private val ArangoConnectionUrlRegex = {
    val scheme = s"^($ArangoDbScheme|$ArangoSecureDbScheme)"
    val user = "([^@:]+)"
    val password = "(.+)"
    val dbName = "(\\S+)"
    val hostList = {
      val hostWithPort = "[^@:]+(?::\\d+)?"
      s"($hostWithPort(?:,$hostWithPort)*)"
    }
    new Regex(s"$scheme://(?:$user(?::$password)?@)?$hostList/$dbName")
  }

  val HumanReadableFormat = s"$ArangoDbScheme|$ArangoSecureDbScheme://[user[:password]@]host[:port]/database"

  def apply(url: String): ArangoConnectionURL = try {
    val ArangoConnectionUrlRegex(scheme, user, password, commaSeparatedHostWithPortList, dbName) = url

    val hosts: Array[(String, Int)] = commaSeparatedHostWithPortList
      .split(",")
      .map(hostPortString => {
        val Array(host, port) = hostPortString.split(":").padTo(2, DefaultPort.toString)
        (host, port.toInt)
      })
    ArangoConnectionURL(
      scheme = scheme,
      user = user.nonBlankOption,
      password = password.nonBlankOption,
      hosts = hosts,
      dbName = dbName
    )
  } catch {
    case e: scala.MatchError => throw new MalformedURLException(e.getMessage)
  }
}
