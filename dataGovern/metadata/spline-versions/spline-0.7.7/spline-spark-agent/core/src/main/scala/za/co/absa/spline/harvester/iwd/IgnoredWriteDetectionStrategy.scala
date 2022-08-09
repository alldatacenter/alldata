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

package za.co.absa.spline.harvester.iwd

import za.co.absa.commons.NamedEntity
import za.co.absa.spline.harvester.LineageHarvester.Metrics

/**
 * <p>
 * This trait deals with wirtes in ignore mode. Some destination doesn't provide enough info to decide
 * whether write actually happened and than something must be chosen as default.
 *
 * This class allows you to implement your own strategy. The mechanism is the same as for
 * [[za.co.absa.spline.harvester.dispatcher.LineageDispatcher]]
 * </p>
 * <br>
 * <p>
 * By default [[za.co.absa.spline.harvester.iwd.DefaultIgnoredWriteDetectionStrategy]] is used.
 * </p>
 *
 */
trait IgnoredWriteDetectionStrategy extends NamedEntity {
  def wasWriteIgnored(writeMetrics: Metrics): Boolean
}
