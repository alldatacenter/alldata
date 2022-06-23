/*
 * Copyright 2021 ABSA Group Limited
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

package za.co.absa.spline.harvester.conf

import org.apache.commons.configuration.Configuration
import za.co.absa.commons.HierarchicalObjectFactory.ClassName
import za.co.absa.spline.agent.AgentConfig.ConfProperty._
import za.co.absa.spline.harvester.conf.Spline05ConfigurationAdapter._
import za.co.absa.spline.harvester.dispatcher.httpdispatcher.HttpLineageDispatcherConfig._
import za.co.absa.spline.harvester.iwd.DefaultIgnoredWriteDetectionStrategy._

import java.util
import scala.collection.JavaConverters.asJavaIterableConverter

class Spline05ConfigurationAdapter(configuration: Configuration) extends ReadOnlyConfiguration {

  private val defaultValues = for {
    (deprecatedKey, substitutionMap) <- Substitutions
    if configuration.containsKey(deprecatedKey)
    entry <- substitutionMap
  } yield entry

  private def scalaKeys = KeyMap.keys.filter(containsKey) ++ defaultValues.keys

  override def isEmpty: Boolean = scalaKeys.isEmpty

  override def containsKey(key: String): Boolean = KeyMap
    .get(key)
    .map(spline05key => configuration.containsKey(spline05key))
    .getOrElse(defaultValues.contains(key))

  override def getProperty(key: String): AnyRef = KeyMap
    .get(key)
    .map(spline05key => configuration.getProperty(spline05key))
    .orElse(defaultValues.get(key))
    .orNull

  override def getKeys: util.Iterator[String] = scalaKeys.asJava.iterator()

}

object Spline05ConfigurationAdapter {
  private val DeprecatedDispatcherClassName = "spline.lineage_dispatcher.className"
  private val SubstitutingDispatcherNameValue = "http"
  private val SubstitutingDispatcherPrefix = s"$RootLineageDispatcher.$SubstitutingDispatcherNameValue"

  private val DeprecatedIWDStrategyClassName = "spline.iwd_strategy.className"
  private val SubstitutingIWDStrategyNameValue = "default"
  private val SubstitutingIWDStrategyPrefix = s"$IgnoreWriteDetectionStrategy.$SubstitutingIWDStrategyNameValue"

  private val KeyMap = Map(
    s"$SubstitutingIWDStrategyPrefix.$ClassName" -> DeprecatedIWDStrategyClassName,
    s"$SubstitutingIWDStrategyPrefix.$OnMissingMetricsKey" -> "spline.iwd_strategy.default.on_missing_metrics",

    s"$SubstitutingDispatcherPrefix.$ClassName" -> DeprecatedDispatcherClassName,
    s"$SubstitutingDispatcherPrefix.$ProducerUrlProperty" -> "spline.producer.url",
    s"$SubstitutingDispatcherPrefix.$ConnectionTimeoutMsKey" -> "spline.timeout.connection",
    s"$SubstitutingDispatcherPrefix.$ReadTimeoutMsKey" -> "spline.timeout.read"
  )

  private val Substitutions = Map(
    DeprecatedDispatcherClassName -> Map(RootLineageDispatcher -> SubstitutingDispatcherNameValue),
    DeprecatedIWDStrategyClassName -> Map(IgnoreWriteDetectionStrategy -> SubstitutingIWDStrategyNameValue)
  )

}
