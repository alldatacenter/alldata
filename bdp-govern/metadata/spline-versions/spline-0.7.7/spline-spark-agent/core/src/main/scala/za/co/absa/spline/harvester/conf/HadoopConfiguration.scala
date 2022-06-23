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

package za.co.absa.spline.harvester.conf

import org.apache.hadoop.conf.{Configuration => SparkHadoopConf}

import java.util
import java.util.function.Consumer


/**
 * {@link org.apache.hadoop.conf.Configuration} to {@link org.apache.commons.configuration.Configuration} adapter
 *
 * @param shc A source of Hadoop configuration
 */
class HadoopConfiguration(shc: SparkHadoopConf)
  extends ReadOnlyConfiguration
    with MapLikeConfigurationAdapter {

  override protected def propertiesMap: util.Map[String, _ <: AnyRef] = {
    val aMap = new util.HashMap[String, String]()
    shc.iterator().forEachRemaining(new Consumer[util.Map.Entry[String, String]] {
      override def accept(t: util.Map.Entry[String, String]): Unit = {
        aMap.put(t.getKey, t.getValue)
      }
    })
    aMap
  }
}
