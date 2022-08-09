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

package za.co.absa.spline.harvester.dispatcher.httpdispatcher

import za.co.absa.commons.version.Version
import za.co.absa.spline.harvester.dispatcher.ProducerApiVersion.SupportedApiRange

class ProducerApiCompatibilityManager private(serverApiVersions: Seq[Version], serverApiLTSVersions: Seq[Version]) {

  def newerServerApiVersion: Option[Version] =
    serverApiVersions
      .sorted
      .lastOption
      .find(SupportedApiRange.Max.<)

  def highestCompatibleApiVersion: Option[Version] =
    serverApiVersions
      .sorted
      .reverse
      .find(v => SupportedApiRange.Min <= v && v <= SupportedApiRange.Max)

  def deprecatedApiVersion: Option[Version] =
    serverApiLTSVersions
      .sorted
      .headOption
      .collect { case v if v > SupportedApiRange.Max => SupportedApiRange.Max }
}

object ProducerApiCompatibilityManager {
  def apply(serverApiVersions: Seq[Version], serverApiLTSVersions: Seq[Version]): ProducerApiCompatibilityManager =
    new ProducerApiCompatibilityManager(serverApiVersions, serverApiLTSVersions)
}
