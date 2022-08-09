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

import org.apache.commons.configuration.MapConfiguration

import java.util

trait MapLikeConfigurationAdapter {
  this: ReadOnlyConfiguration =>

  private val mapConf = new MapConfiguration(propertiesMap)

  protected def propertiesMap: util.Map[String, _ <: AnyRef]

  override def getProperty(key: String): AnyRef = mapConf.getProperty(key)

  override def getKeys: util.Iterator[_] = mapConf.getKeys()

  override def containsKey(key: String): Boolean = mapConf.containsKey(key)

  override def isEmpty: Boolean = mapConf.isEmpty

}
