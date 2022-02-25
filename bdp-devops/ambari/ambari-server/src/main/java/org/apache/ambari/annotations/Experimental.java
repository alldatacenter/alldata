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
 * The {@code Experimental} annotation is used to mark an area of code which
 * contains untested functionality. This allows areas of code
 * which may not be completed to be quickly located.
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR,
    ElementType.ANNOTATION_TYPE, ElementType.PACKAGE, ElementType.FIELD,
    ElementType.LOCAL_VARIABLE })
public @interface Experimental {

  /**
   * The logical feature set that an experimental area of code belongs to.
   *
   * @return
   */
  ExperimentalFeature feature();

  /**
   * Any notes to why the annotation is used or any other action that may be useful.
   */
  String comment() default "";
}
