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

package za.co.absa.spline.persistence.api.composition

import java.util.UUID

/**
  * Represents a persisted source data (e.g. file)
  *
  * @param path        file location
  * @param datasetsIds IDs of associated dataset(s) that was read/written from/to the given data source
  */
case class MetaDataSource(path: String, datasetsIds: Seq[UUID])

/**
  * Represents a persisted source data (e.g. file).
  * Same as [[MetaDataSource]] but with type
  *
  * @param `type`      source type
  * @param path        file location
  * @param datasetsIds ID of an associated dataset that was read/written from/to the given data source
  */
case class TypedMetaDataSource(`type`: String, path: String, datasetsIds: Seq[UUID])
