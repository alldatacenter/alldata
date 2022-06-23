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

package za.co.absa.spline.gateway.rest

import org.springframework.web.WebApplicationInitializer
import org.springframework.web.context.ContextLoaderListener
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext
import za.co.absa.spline.common.webmvc.AppInitializerUtils.{registerFilter, registerRESTDispatcher, registerRootDispatcher}
import za.co.absa.spline.common.webmvc.cors.PermissiveCorsFilter
import za.co.absa.spline.common.webmvc.diagnostics.{DiagnosticsRESTConfig, RootWebContextConfig}
import za.co.absa.spline.consumer.rest.ConsumerRESTConfig
import za.co.absa.spline.consumer.service.ConsumerServicesConfig
import za.co.absa.spline.gateway.rest.filter.GzipFilter
import za.co.absa.spline.persistence.ArangoRepoConfig
import za.co.absa.spline.producer.rest.ProducerRESTConfig
import za.co.absa.spline.producer.service.ProducerServicesConfig

import javax.servlet.ServletContext

object AppInitializer extends WebApplicationInitializer {
  override def onStartup(container: ServletContext): Unit = {
    container
      .addListener(new ContextLoaderListener(new AnnotationConfigWebApplicationContext {
        setAllowBeanDefinitionOverriding(false)
        register(
          classOf[ConsumerServicesConfig],
          classOf[ProducerServicesConfig],
          classOf[ArangoRepoConfig])
      }))

    registerFilter[PermissiveCorsFilter](container, "CORSFilter", "/*")
    registerFilter[GzipFilter](container, "GzipFilter", "/*")

    registerRESTDispatcher[ConsumerRESTConfig](container, "consumer")
    registerRESTDispatcher[ProducerRESTConfig](container, "producer")
    registerRESTDispatcher[DiagnosticsRESTConfig](container, "about")

    registerRootDispatcher[RootWebContextConfig](container)
  }
}
