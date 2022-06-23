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

import org.apache.hadoop.conf.Configuration

class HadoopConfigurationTest extends ReadOnlyConfigurationTest {
  HadoopConfigurationTest.init()

  override protected val givenConf = new HadoopConfiguration(new Configuration())
  override protected val emptyConf = new HadoopConfiguration(new Configuration(false))
}

object HadoopConfigurationTest {
  Configuration.addDefaultResource(
    s"${this.getClass.getPackage.getName.replaceAllLiterally(".", "/")}/hdfs-test-conf.xml")

  def init(): Unit = {}
}
