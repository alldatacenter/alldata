/*
 * Copyright 2022 ABSA Group Limited
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

package za.co.absa.spline.harvester.dispatcher.openlineage

import org.apache.commons.configuration.Configuration
import za.co.absa.commons.config.ConfigurationImplicits.{ConfigurationOptionalWrapper, ConfigurationRequiredWrapper}
import za.co.absa.commons.version.Version
import za.co.absa.spline.harvester.dispatcher.openlineage.HttpOpenLineageDispatcherConfig._

import scala.concurrent.duration.{Duration, DurationInt, DurationLong}

object HttpOpenLineageDispatcherConfig {
  val apiUrlProperty = "api.url"
  val ConnectionTimeoutMsKey = "timeout.connection"
  val ReadTimeoutMsKey = "timeout.read"
  val ApiVersion = "apiVersion"
  val Namespace = "namespace"


  val DefaultConnectionTimeout: Duration = 1.second
  val DefaultReadTimeout: Duration = 20.second
  val DefaultNamespace = "default"

  def apply(c: Configuration) = new HttpOpenLineageDispatcherConfig(c)
}

class HttpOpenLineageDispatcherConfig(config: Configuration) {
  val apiUrl: String = config.getRequiredString(apiUrlProperty)

  val connTimeout: Duration = config
    .getOptionalLong(ConnectionTimeoutMsKey)
    .map(_.millis)
    .getOrElse(DefaultConnectionTimeout)

  val readTimeout: Duration = config
    .getOptionalLong(ReadTimeoutMsKey)
    .map(_.millis)
    .getOrElse(DefaultReadTimeout)

  val apiVersion: Version = Version.asSimple(config.getRequiredString(ApiVersion))

  val namespace: String = config
    .getOptionalString(Namespace)
    .getOrElse(DefaultNamespace)
}
