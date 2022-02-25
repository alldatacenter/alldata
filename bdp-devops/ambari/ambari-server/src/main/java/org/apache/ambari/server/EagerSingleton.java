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

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.google.inject.Module;
import com.google.inject.ScopeAnnotation;
import com.google.inject.Singleton;
import com.google.inject.binder.AnnotatedBindingBuilder;

/**
 * The {@link EagerSingleton} annotation is used to mark a {@link Singleton}
 * class as being needed to be eagerly registered by a {@link Module}.
 * <p/>
 * Classes marked with this annotation should also be {@link Singleton}. They
 * will be discovered on the classpath and then
 * {@link AnnotatedBindingBuilder#asEagerSingleton()} will be invoked on them.
 */
@Target({ ElementType.TYPE })
@Retention(RUNTIME)
@ScopeAnnotation
public @interface EagerSingleton {
}
