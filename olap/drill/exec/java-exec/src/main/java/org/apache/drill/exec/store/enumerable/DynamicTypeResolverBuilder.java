/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.store.enumerable;

import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.jsontype.impl.StdTypeResolverBuilder;
import org.reflections.Reflections;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class DynamicTypeResolverBuilder extends StdTypeResolverBuilder {

  @Override
  public TypeDeserializer buildTypeDeserializer(DeserializationConfig config,
    JavaType baseType, Collection<NamedType> subtypes) {

    Reflections reflections = new Reflections("org.apache.drill.exec.store");
    @SuppressWarnings("unchecked")
    Class<Object> rawClass = (Class<Object>) baseType.getRawClass();
    List<NamedType> dynamicSubtypes = reflections.getSubTypesOf(rawClass).stream()
      .map(NamedType::new)
      .collect(Collectors.toList());
    dynamicSubtypes.addAll(subtypes);

    return super.buildTypeDeserializer(config, baseType, dynamicSubtypes);
  }
}
