/*
 * Copyright 2019 ABSA Group Limited
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

import com.fasterxml.classmate.TypeResolver
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.{Bean, Configuration}
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import springfox.documentation.builders.{PathSelectors, RequestHandlerSelectors}
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket
import springfox.documentation.swagger2.annotations.EnableSwagger2

@Configuration
@EnableSwagger2
class SwaggerConfig @Autowired()(val typeResolver: TypeResolver)
  extends WebMvcConfigurer
    with SwaggerScalaTypesRules
    with SwaggerUISupport {

  @Bean def api: Docket =
    new Docket(DocumentationType.SWAGGER_2).
      select.
      apis(RequestHandlerSelectors.any).
      paths(PathSelectors.any).
      build

  @Bean def rpbPlugin = new SwaggerRequiredPropertyBuilderPlugin
}
