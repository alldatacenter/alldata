/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.spark.test.extensions;

import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.platform.commons.util.AnnotationUtils;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.ToStringBuilder;

import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Optional;

public class DefaultParameterContext implements ParameterContext {

  private final Parameter parameter;
  private final int index;
  private final Optional<Object> target;

  public DefaultParameterContext(Parameter parameter, int index, Optional<Object> target) {
    Preconditions.condition(index >= 0, "index must be greater than or equal to zero");
    this.parameter = Preconditions.notNull(parameter, "parameter must not be null");
    this.index = index;
    this.target = Preconditions.notNull(target, "target must not be null");
  }

  @Override
  public Parameter getParameter() {
    return this.parameter;
  }

  @Override
  public int getIndex() {
    return this.index;
  }

  @Override
  public Optional<Object> getTarget() {
    return this.target;
  }

  @Override
  public boolean isAnnotated(Class<? extends Annotation> annotationType) {
    return AnnotationUtils.isAnnotated(this.parameter, this.index, annotationType);
  }

  @Override
  public <A extends Annotation> Optional<A> findAnnotation(Class<A> annotationType) {
    return AnnotationUtils.findAnnotation(this.parameter, this.index, annotationType);
  }

  @Override
  public <A extends Annotation> List<A> findRepeatableAnnotations(Class<A> annotationType) {
    return AnnotationUtils.findRepeatableAnnotations(this.parameter, this.index, annotationType);
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .append("parameter", this.parameter)
        .append("index", this.index)
        .append("target", this.target)
        .toString();
  }
}
