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
package org.apache.ambari.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The {@link Markdown} annotation is used to add information when creating <a
 * href=https://en.wikipedia.org/wiki/Markdown>Markdown</a> content.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.FIELD, ElementType.METHOD })
public @interface Markdown {
  /**
   * A description to add for this element when generating Markdown.
   *
   * @return
   */
  String description();

  /**
   * An optional list of example values.
   *
   * @return
   */
  String[] examples() default {};

  /**
   * A way of expressing a relationship.
   */
  String relatedTo() default "";

  /**
   * If {@code true}, indicates that the annotated content is for internal-use only.
   *
   * @return {@code true} for internal-only content.
   */
  boolean internal() default false;
}
