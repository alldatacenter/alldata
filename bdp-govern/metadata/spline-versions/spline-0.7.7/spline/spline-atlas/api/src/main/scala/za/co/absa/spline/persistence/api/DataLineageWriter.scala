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

import za.co.absa.spline.persistence.api.composition.DataLineage

import scala.concurrent.{ExecutionContext, Future}

/**
  * The trait represents a writer to a persistence layer for the [[DataLineage]] entity.
  */
trait DataLineageWriter {

  /**
    * The method stores a particular data lineage to the persistence layer.
    *
    * @param lineage A data lineage that will be stored
    */
  def store(lineage: DataLineage)(implicit ec: ExecutionContext) : Future[Unit]
}
