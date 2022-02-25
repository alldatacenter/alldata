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
package org.apache.ambari.server;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.ScopeAnnotation;
import com.google.inject.Singleton;

/**
 * The {@link AmbariService} annotation is used to register a class that
 * implements Guava's {@link Service} with the {@link ServiceManager}.
 * <p/>
 * Classes with this annotation are bound as singletons and automatically
 * injected with their members. There is not need to use {@link Singleton} or
 * {@link EagerSingleton}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
@ScopeAnnotation
public @interface AmbariService {
}
