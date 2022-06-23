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

package za.co.absa.spline.common.webmvc.jackson

import java.util

import com.fasterxml.jackson.databind.ObjectMapper
import org.mockito.Mockito
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter

class ObjectMapperBeanPostProcessorSpec extends AnyFlatSpec with Matchers with MockitoSugar {

  "post process function" should "be called on every ObjectMapper instance in the message converters" in {
    val postProcessSpy = mock[ObjectMapper => Unit]
    val mapperDummy1 = mock[ObjectMapper]
    val mapperDummy2 = mock[ObjectMapper]

    new ObjectMapperBeanPostProcessor(postProcessSpy)
      .postProcessBeforeInitialization(
        new RequestMappingHandlerAdapter {
          getMessageConverters.addAll(util.Arrays.asList(
            mock[HttpMessageConverter[_]],
            new MappingJackson2HttpMessageConverter {
              setObjectMapper(mapperDummy1)
            },
            new MappingJackson2HttpMessageConverter {
              setObjectMapper(mapperDummy2)
            }
          ))
        },
        "testPostProcessor")

    Mockito.verify(postProcessSpy).apply(mapperDummy1)
    Mockito.verify(postProcessSpy).apply(mapperDummy2)
  }

}
