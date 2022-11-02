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

package za.co.absa.spline.common.webmvc.jackson

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter

import scala.collection.JavaConverters._

class ObjectMapperBeanPostProcessor(postProcess: ObjectMapper => Unit) extends BeanPostProcessor {

  override def postProcessBeforeInitialization(bean: AnyRef, beanName: String): AnyRef = {
    bean match {
      case adapter: RequestMappingHandlerAdapter =>
        adapter
          .getMessageConverters
          .asScala
          .collect({ case hmc: MappingJackson2HttpMessageConverter => hmc.getObjectMapper })
          .foreach(postProcess(_))
      case _ =>
    }
    bean
  }
}

