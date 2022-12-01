/*
 * Copyright 2019 ABSA Group Limited
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

package za.co.absa.spline.harvester.dispatcher

import za.co.absa.commons.NamedEntity
import za.co.absa.spline.agent.AgentConfig.ConfProperty
import za.co.absa.spline.harvester.exception.SplineInitializationException
import za.co.absa.spline.producer.model.{ExecutionEvent, ExecutionPlan}

/**
 * <p>
 * This trait deals with the captured lineage information.
 * When the lineage data is ready the instance of this trait is called to publish the result.
 * Which implementation is used depends on the configuration.
 * <br>
 * See [[ConfProperty.RootLineageDispatcher]]
 * </p>
 * <br>
 * <p>
 * A custom lineage dispatcher can be registered via the configuration properties.
 * First you define name and then class for that dispatcher same way as it's done in spline.default.properties
 * for the default http dispatcher.
 *
 * <br>
 * See: [[za.co.absa.spline.harvester.conf.StandardSplineConfigurationStack]]
 * </p>
 * <br>
 * <p>
 * When registering a class by config properties, the constructor can have access to the configuration object via constructor injection:
 * {{{
 *    @throws[SplineInitializationException]
 *    def constr(conf: org.apache.commons.configuration.Configuration)
 * }}}
 * </p>
 *
 *
 */
@throws[SplineInitializationException]
trait LineageDispatcher extends NamedEntity {
  def send(plan: ExecutionPlan): Unit
  def send(event: ExecutionEvent): Unit
}
