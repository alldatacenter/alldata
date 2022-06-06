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
 * The {@link SwaggerOverwriteNestedAPI} is used to overwrite default values of
 * {@code NestedApiRecord} when {@link org.apache.ambari.swagger.AmbariSwaggerReader}
 * processes nested API classes for Swagger annotations.
 *
 * It can be useful to overcome the limitations of multi-nested service endpoints or endpoints without Path
 * parameters.
 *
 * Parent API and its properties are not validated.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SwaggerOverwriteNestedAPI {

    /**
     * @return class name of parent object
     */
    Class<?> parentApi();

    /**
     * @return parent API path, usually top-level API path starting with slash.
     */
    String parentApiPath();

    /**
     * @return path annotation value of the method in parent class.
     */
    String parentMethodPath();

    /**
     * @return array of Strings to provide path parameters. Only string types are supported as of now.
     */
    String[] pathParameters();
}
