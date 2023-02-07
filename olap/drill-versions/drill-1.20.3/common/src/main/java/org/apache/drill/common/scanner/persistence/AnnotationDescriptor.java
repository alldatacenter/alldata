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

import static java.lang.String.format;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableMap;

/**
 * a class annotation
 */
public final class AnnotationDescriptor {
  private final String annotationType;
  private final List<AttributeDescriptor> attributes;
  private final Map<String, AttributeDescriptor> attributeMap;

  @JsonCreator public AnnotationDescriptor(
      @JsonProperty("annotationType") String annotationType,
      @JsonProperty("attributes") List<AttributeDescriptor> attributes) {
    this.annotationType = annotationType;
    this.attributes = Collections.unmodifiableList(attributes);
    ImmutableMap.Builder<String, AttributeDescriptor> mapBuilder = ImmutableMap.builder();
    for (AttributeDescriptor att : attributes) {
      mapBuilder.put(att.getName(), att);
    }
    this.attributeMap = mapBuilder.build();
  }

  /**
   * @return the class name of the annotation
   */
  public String getAnnotationType() {
    return annotationType;
  }

  public List<AttributeDescriptor> getAttributes() {
    return attributes;
  }

  public boolean hasAttribute(String attributeName) {
    return attributeMap.containsKey(attributeName);
  }

  public List<String> getValues(String attributeName) {
    AttributeDescriptor desc = attributeMap.get(attributeName);
    return desc == null ? Collections.<String>emptyList() : desc.getValues();
  }

  public String getSingleValue(String attributeName, String defaultValue) {
    List<String> values = getValues(attributeName);
    if (values.size() > 1) {
      throw new IllegalStateException(String.format(
          "Expected a single value for the attribute named %s but found %d (%s)",
          attributeName, values.size(), values.toString()));
    }
    return values.isEmpty() ? defaultValue : values.get(0);
  }

  public String getSingleValue(String attributeName) {
    String val = getSingleValue(attributeName, null);
    if (val == null) {
      throw new IllegalStateException(format("Attribute %s not found in %s", attributeName, attributes));
    }
    return val;
  }

  @Override
  public String toString() {
    return "Annotation[type=" + annotationType + ", attributes=" + attributes + "]";
  }

  static Map<String, AnnotationDescriptor> buildAnnotationsMap(List<AnnotationDescriptor> annotations) {
    ImmutableMap.Builder<String, AnnotationDescriptor> annMapBuilder = ImmutableMap.builder();
    for (AnnotationDescriptor ann : annotations) {
      annMapBuilder.put(ann.getAnnotationType(), ann);
    }
    return annMapBuilder.build();
  }

  @SuppressWarnings("unchecked")
  private <T> T proxy(Class<T> interfc, InvocationHandler ih) {
    if (!interfc.isInterface()) {
      throw new IllegalArgumentException("only proxying interfaces: " + interfc);
    }
    return (T)Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[]{ interfc }, ih);
  }

  public <T> T getProxy(Class<T> clazz) {
    return proxy(clazz, new InvocationHandler() {
      @Override
      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (args != null && args.length > 0) {
          // just property methods
          throw new UnsupportedOperationException(method + " " + Arrays.toString(args));
        }
        String attributeName = method.getName();
        if (!hasAttribute(attributeName)) {
          return method.getDefaultValue();
        }
        Class<?> returnType = method.getReturnType();
        if (returnType.isArray()) {
          List<String> values = getValues(attributeName);
          Class<?> componentType = returnType.getComponentType();
          Object[] result = (Object[])Array.newInstance(componentType, values.size());
          for (int i = 0; i < result.length; i++) {
            String value = values.get(i);
            result[i] = convertValue(componentType, value);
          }
          return result;
        } else {
          String value = getSingleValue(attributeName, null);
          return convertValue(returnType, value);
        }
      }

      private <U> Object convertValue(Class<U> c, String value) {
        if (c.equals(String.class)) {
          return value;
        } else if (c.isEnum()) {
          @SuppressWarnings("unchecked")
          Enum<?> enumValue = Enum.valueOf(c.asSubclass(Enum.class), value);
          return enumValue;
        } else if (c.equals(boolean.class)) {
          return Boolean.valueOf(value);
        } else if (c.equals(int.class)) {
          return Integer.valueOf(value);
        }
        throw new UnsupportedOperationException(c.toString());
      }
    });
  }
}