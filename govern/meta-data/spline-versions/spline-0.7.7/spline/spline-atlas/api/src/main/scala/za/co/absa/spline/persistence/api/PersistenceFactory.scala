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

package za.co.absa.spline.persistence.api

import org.apache.commons.configuration.Configuration
import org.slf4s.Logging

/**
  * The abstract class represents a factory of persistence readers and writers for all main data lineage entities.
  *
  * @param configuration A source of settings
  */
abstract class PersistenceFactory(protected val configuration: Configuration) extends Logging {

  /**
    * The method creates a writer to the persistence layer for the [[DataLineage DataLineage]] entity.
    *
    * @return A writer to the persistence layer for the [[DataLineage DataLineage]] entity
    */
  def createDataLineageWriter: DataLineageWriter

  /**
    * The method creates a reader from the persistence layer for the [[DataLineage DataLineage]] entity.
    *
    * @return An optional reader from the persistence layer for the [[DataLineage DataLineage]] entity
    */
  def createDataLineageReader: Option[DataLineageReader]
}
