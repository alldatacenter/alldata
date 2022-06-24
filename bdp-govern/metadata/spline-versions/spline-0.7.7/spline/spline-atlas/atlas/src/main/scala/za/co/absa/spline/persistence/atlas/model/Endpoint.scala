/*
 * Copyright 2017 ABSA Group Limited
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

package za.co.absa.spline.persistence.atlas.model

import org.apache.atlas.AtlasClient
import org.apache.atlas.v1.model.instance.Referenceable

/**
  * The object represents an enumeration of endpoint directions.
  */
object EndpointDirection extends Enumeration {
  type EndpointDirection = Value
  val input, output = Value
}

/**
  * The object represents an enumeration of endpoint types.
  */
object EndpointType extends Enumeration {
  type EndpointType = Value
  val file = Value
}

/**
  * The class represents a file endpoint.
  * @param path A relative path to the file
  * @param uri An absolute path to the file including cluster name, etc.
  */
class FileEndpoint(val path: String, uri: String) extends Referenceable(
  SparkDataTypes.FileEndpoint,
  new java.util.HashMap[String, Object]{
    put(AtlasClient.NAME, path)
    put(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, uri)
    put("path", path)
  }
)
