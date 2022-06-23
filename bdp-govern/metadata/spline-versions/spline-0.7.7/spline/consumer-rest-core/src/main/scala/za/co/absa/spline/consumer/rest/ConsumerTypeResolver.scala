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
package za.co.absa.spline.consumer.rest

import com.fasterxml.jackson.annotation.JsonTypeInfo.{As, Id}
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTypeResolverBuilder
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator
import com.fasterxml.jackson.databind.{JavaType, ObjectMapper}
import za.co.absa.spline.consumer.service.model.{DataType, LineageOverviewNode}

class ConsumerTypeResolver extends DefaultTypeResolverBuilder(
  ObjectMapper.DefaultTyping.OBJECT_AND_NON_CONCRETE,
  LaissezFaireSubTypeValidator.instance) {

  init(Id.NAME, null)
  inclusion(As.PROPERTY)
  typeProperty("_type")

  override def useForType(t: JavaType): Boolean = {
    t.isTypeOrSubTypeOf(classOf[LineageOverviewNode]) || t.isTypeOrSubTypeOf(classOf[DataType])
  }
}
