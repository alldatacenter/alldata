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

package za.co.absa.spline.harvester.dispatcher.modelmapper

import java.net.URI

object OpenLineageUriMapper {

  def uriToNamespaceAndName(uriString: String): (String, String) = {
    val uri = URI.create(preProcessUri(uriString))
    val namespace = Seq(Option(uri.getScheme), Option(uri.getAuthority)).flatten.mkString("://")
    val name = Option(uri.getPath).getOrElse("")

    (namespace, name)
  }

  private def preProcessUri(uri: String): String =
    uri.stripPrefix("jdbc:")

}
