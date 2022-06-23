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

object HttpConstants {

  object SplineHeaders {
    private val Prefix = "ABSA-Spline"

    val ApiVersion = s"$Prefix-API-Version"
    val ApiLTSVersion = s"$Prefix-API-LTS-Version"
    val AcceptRequestEncoding = s"$Prefix-Accept-Request-Encoding"
  }

  object Encoding {
    val GZIP = "gzip"
  }

}
