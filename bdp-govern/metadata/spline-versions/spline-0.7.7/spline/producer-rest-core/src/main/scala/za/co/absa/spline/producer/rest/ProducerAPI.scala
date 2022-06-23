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

package za.co.absa.spline.producer.rest

import za.co.absa.commons.version.Version
import za.co.absa.commons.version.Version._

object ProducerAPI {
  val CurrentVersion: Version = ver"1.1"
  val DeprecatedVersions: Seq[Version] = Seq(ver"1" /*, ...*/)
  val LTSVersions: Seq[Version] = Seq(CurrentVersion /*, ...*/)
  val SupportedVersions: Seq[Version] = LTSVersions ++ DeprecatedVersions

  final val MimeTypeV1_1 = "application/vnd.absa.spline.producer.v1.1+json"
}
