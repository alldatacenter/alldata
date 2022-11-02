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

package za.co.absa.spline.common.webmvc

import org.springframework.web.context.support.AnnotationConfigWebApplicationContext
import org.springframework.web.servlet.DispatcherServlet

import java.util
import javax.servlet.DispatcherType.{ASYNC, REQUEST}
import javax.servlet.{Filter, ServletContext}
import scala.reflect.ClassTag

object AppInitializerUtils {

  def registerRootDispatcher[A: ClassTag](container: ServletContext): Unit = {
    val restConfigClassTag = implicitly[ClassTag[A]]
    val dispatcher = container
      .addServlet("RootDispatcher", new DispatcherServlet(new AnnotationConfigWebApplicationContext {
        setAllowBeanDefinitionOverriding(false)
        register(restConfigClassTag.runtimeClass)
      }))
    dispatcher.addMapping("/*")
    dispatcher.setAsyncSupported(true)
  }

  def registerRESTDispatcher[A: ClassTag](container: ServletContext, name: String): Unit = {
    val restConfigClassTag = implicitly[ClassTag[A]]
    val webContext = new AnnotationConfigWebApplicationContext {
      setAllowBeanDefinitionOverriding(false)
      register(restConfigClassTag.runtimeClass)
    }
    val dispatcher = container.addServlet(s"${name}Dispatcher", new DispatcherServlet(webContext))
    dispatcher.setLoadOnStartup(1)
    dispatcher.addMapping(s"/$name/*")
    dispatcher.setAsyncSupported(true)
  }

  def registerFilter[A <: Filter : ClassTag](container: ServletContext, name: String, valuemapping: String): Unit = {
    val filterClassTag = implicitly[ClassTag[A]]
    val filterClass: Class[A] = filterClassTag.runtimeClass.asInstanceOf[Class[A]]
    val registration = container.addFilter(name, filterClass)
    registration.addMappingForUrlPatterns(util.EnumSet.of(REQUEST), false, valuemapping)
    registration.setAsyncSupported(true)
  }

}
