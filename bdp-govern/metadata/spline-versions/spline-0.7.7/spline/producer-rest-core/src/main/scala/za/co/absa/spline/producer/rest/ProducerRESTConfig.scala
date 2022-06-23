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

package za.co.absa.spline.producer.rest

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.twitter.finatra.jackson.FinatraInternalModules
import org.springframework.context.annotation.{Bean, ComponentScan, Configuration, EnableAspectJAutoProxy}
import org.springframework.web.method.support.HandlerMethodReturnValueHandler
import org.springframework.web.servlet.config.annotation.{EnableWebMvc, WebMvcConfigurer}
import za.co.absa.spline.common.webmvc.jackson.ObjectMapperBeanPostProcessor
import za.co.absa.spline.common.webmvc.{ScalaFutureMethodReturnValueHandler, UnitMethodReturnValueHandler}

import java.util

@EnableWebMvc
@EnableAspectJAutoProxy
@Configuration
@ComponentScan(basePackageClasses = Array(
  classOf[controller._package],
  classOf[controller.v1._package],
))
class ProducerRESTConfig extends WebMvcConfigurer {

  import scala.concurrent.ExecutionContext.Implicits.global

  override def addReturnValueHandlers(returnValueHandlers: util.List[HandlerMethodReturnValueHandler]): Unit = {
    returnValueHandlers.add(new UnitMethodReturnValueHandler)
    returnValueHandlers.add(new ScalaFutureMethodReturnValueHandler)
  }

  @Bean def jacksonConfigurer = new ObjectMapperBeanPostProcessor(_
    .registerModule(DefaultScalaModule)
    .setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE)
    .registerModule(FinatraInternalModules.caseClassModule))
}
