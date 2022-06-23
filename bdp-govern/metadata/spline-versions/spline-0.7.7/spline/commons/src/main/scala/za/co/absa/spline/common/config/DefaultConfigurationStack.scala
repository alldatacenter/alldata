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

package za.co.absa.spline.common.config

import org.apache.commons.configuration._
import za.co.absa.commons.config.UpperSnakeCaseEnvironmentConfiguration
import za.co.absa.spline.common.config.DefaultConfigurationStack.jndiConfigurationIfAvailable

import java.util
import javax.naming.InitialContext
import scala.util.Try

abstract class DefaultConfigurationStack extends CompositeConfiguration(util.Arrays.asList(
  jndiConfigurationIfAvailable.toSeq ++ Seq(
    new SystemConfiguration,
    new UpperSnakeCaseEnvironmentConfiguration,
    new EnvironmentConfiguration
  ): _*))

object DefaultConfigurationStack {
  def jndiConfigurationIfAvailable: Option[JNDIConfiguration] = Try({
    new InitialContext().getEnvironment
    new JNDIConfiguration("java:comp/env")
  }).toOption
}
