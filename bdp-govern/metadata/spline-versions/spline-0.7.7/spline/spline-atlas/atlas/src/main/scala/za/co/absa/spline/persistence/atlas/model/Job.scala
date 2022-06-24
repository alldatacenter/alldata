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
import org.apache.atlas.v1.model.instance.{Id, Referenceable}

import scala.collection.JavaConverters._

/**
  * The class represents a computation logic of a specific job of a Spark application.
  * @param id An identifier
  * @param name A Spark application name
  * @param qualifiedName An uniquie identifier
  * @param operations A sequence of operations defining computation logic of the job
  * @param datasets A sequence of intermediate results with the job
  * @param inputDatasets Input data
  * @param outputDatasets Output data
  * @param inputEndpoints Endpoints where the job is sourcing data from
  * @param outputEndpoints Endpoint where results of the job will be stored
  */
class Job(
  id: String,
  name: String,
  qualifiedName: String,
  operations: Seq[Id],
  datasets: Seq[Id],
  inputDatasets: Seq[Id],
  outputDatasets: Seq[Id],
  inputEndpoints: Seq[Id],
  outputEndpoints: Seq[Id]

)extends Referenceable(
    SparkDataTypes.Job,
    new java.util.HashMap[String, Object]{
      put("id" ,id)
      put(AtlasClient.NAME, name)
      put(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, qualifiedName)
      put("operations", operations.asJava)
      put("datasets", datasets.asJava)
      put("inputDatasets", inputDatasets.asJava)
      put("outputDatasets", outputDatasets.asJava)
      put("inputs", inputEndpoints.asJava)
      put("outputs", outputEndpoints.asJava)
    }
  )
