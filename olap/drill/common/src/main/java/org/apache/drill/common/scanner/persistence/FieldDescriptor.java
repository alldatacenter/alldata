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
package org.apache.drill.common.scanner.persistence;

import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;

import org.apache.drill.common.exceptions.DrillRuntimeException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * a class fields
 */
public final class FieldDescriptor {
  private final String name;
  private final String descriptor;
  private final List<AnnotationDescriptor> annotations;
  private final Map<String, AnnotationDescriptor> annotationMap;

  @JsonCreator public FieldDescriptor(
      @JsonProperty("name") String name,
      @JsonProperty("descriptor") String descriptor,
      @JsonProperty("annotations") List<AnnotationDescriptor> annotations) {
    this.name = name;
    this.descriptor = descriptor;
    this.annotations = annotations;
    this.annotationMap = AnnotationDescriptor.buildAnnotationsMap(annotations);
    // validate the descriptor
    getType();
  }

  public String getName() {
    return name;
  }

  /**
   * @return the descriptor of the type of the field as defined in the bytecode
   */
  public String getDescriptor() {
    return descriptor;
  }

  public List<AnnotationDescriptor> getAnnotations() {
    return annotations;
  }

  public AnnotationDescriptor getAnnotation(Class<?> clazz) {
    return annotationMap.get(clazz.getName());
  }

  public <T> T getAnnotationProxy(Class<T> clazz) {
    final AnnotationDescriptor annotationDescriptor = getAnnotation(clazz);
    if (annotationDescriptor == null) {
      return null;
    }
    return annotationDescriptor.getProxy(clazz);
  }

  @JsonIgnore
  public TypeDescriptor getType() {
      int arrayDim = 0;
      char c;
      while ((c = descriptor.charAt(arrayDim)) == '[') {
          ++arrayDim;
      }
      if (c == 'L') {
        int lastIndex = descriptor.length() - 1;
        if (descriptor.charAt(lastIndex) != ';') {
          throw new DrillRuntimeException("Illegal descriptor: " + descriptor);
        }
        String className = descriptor.substring(arrayDim + 1, lastIndex).replace('/', '.');
        return TypeDescriptor.forClass(className, arrayDim);
      } else {
        return TypeDescriptor.forPrimitive(c, arrayDim);
      }
  }

  @JsonIgnore
  public Class<?> getFieldClass() {
    TypeDescriptor type = getType();
    Class<?> elementClass = type.getType();
    if (type.isArray()) {
      return Array.newInstance(elementClass, new int[type.getArrayDim()]).getClass();
    } else {
      return elementClass;
    }
  }

  @Override
  public String toString() {
    return "Field[name=" + name + ", descriptor=" + descriptor + ", annotations=" + annotations + "]";
  }
}