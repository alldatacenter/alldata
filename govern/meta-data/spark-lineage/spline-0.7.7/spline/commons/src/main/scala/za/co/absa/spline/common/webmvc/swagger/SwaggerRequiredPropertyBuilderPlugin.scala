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

package za.co.absa.spline.common.webmvc.swagger

import io.swagger.annotations.ApiModelProperty
import org.springframework.core.annotation.Order
import springfox.documentation.schema.Annotations._
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spi.schema.ModelPropertyBuilderPlugin
import springfox.documentation.spi.schema.contexts.ModelPropertyContext
import springfox.documentation.swagger.common.SwaggerPluginSupport.pluginDoesApply
import springfox.documentation.swagger.schema.ApiModelProperties._
import za.co.absa.commons.reflect.ReflectionUtils

import scala.language.implicitConversions

@Order(-2147482647) // == springfox.documentation.swagger.common.SwaggerPluginSupport.SWAGGER_PLUGIN_ORDER + 1
class SwaggerRequiredPropertyBuilderPlugin extends ModelPropertyBuilderPlugin {

  import SwaggerRequiredPropertyBuilderPlugin._

  override def supports(delimiter: DocumentationType): Boolean = pluginDoesApply(delimiter)

  override def apply(context: ModelPropertyContext): Unit = {
    def maybeRequiredByPropertyAnnotation =
      context.getAnnotatedElement.
        flatMap(findApiModePropertyAnnotation).
        map(_.required())

    def maybeRequiredByBeanAnnotation =
      context.getBeanPropertyDefinition.
        flatMap(findPropertyAnnotation(_, classOf[ApiModelProperty])).
        map(_.required())

    def maybeRequiredByType =
      context.getBeanPropertyDefinition.map(d =>
        !classOf[Option[_]].isAssignableFrom(d.getRawPrimaryType) &&
          ReflectionUtils.caseClassCtorArgDefaultValue(d.getAccessor.getDeclaringClass, d.getName).isEmpty
      )

    context.getBuilder.required(
      maybeRequiredByPropertyAnnotation.
        orElse(maybeRequiredByBeanAnnotation).
        orElse(maybeRequiredByType).
        getOrElse(true): Boolean)
  }
}

object SwaggerRequiredPropertyBuilderPlugin {
  implicit def googleOptionalToScalaOption[T](gopt: com.google.common.base.Optional[T]): Option[T] =
    if (gopt.isPresent) Some(gopt.get) else None
}
