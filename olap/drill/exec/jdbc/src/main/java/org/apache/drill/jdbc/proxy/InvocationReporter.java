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
package org.apache.drill.jdbc.proxy;

import java.lang.reflect.Method;


/**
 * Reporter of method-invocation events.
 */
interface InvocationReporter
{

  /**
   * Reports a proxy setup message.
   */
  void setupMessage( String message );

  /**
   * Reports an invocation of a proxied method.
   *
   * @param  targetObject
   *         the target of the method invocation (the object on which the method
   *         was called)
   * @param  targetType
   *         the declared target type of the method (the interface whose method
   *         was called; not the actual type of {@code targetObject})
   * @param  method
   *         the method that was called
   * @param  arguments
   *         the arguments that were passed
   *         (represented as for {@link Method#invoke})
   */
  void methodCalled( Object targetObject, Class<?> targetType, Method method,
                     Object[] arguments );

  /**
   * Reports the return from an invocation of a proxied method.
   *
   * @param  targetObject  same as for {@link #methodCalled} for the call
   * @param  targetType    same as for {@link #methodCalled} for the call
   * @param  method        same as for {@link #methodCalled} for the call
   * @param  arguments     same as for {@link #methodCalled} for the call
   *
   * @param result         the value returned from the method
   *                       (represented as from {@link Method#invoke})
   *
   */
  void methodReturned( Object targetObject, Class<?> targetType, Method method,
                       Object[] arguments, Object result );

  /**
   * Reports the throwing of an exception from an invocation of a proxied method.
   *
   * @param  targetObject  same as for {@link #methodCalled} for the call
   * @param  targetType    same as for {@link #methodCalled} for the call
   * @param  method        same as for {@link #methodCalled} for the call
   * @param  arguments     same as for {@link #methodCalled} for the call
   *
   * @param  exception     the exception thrown by the method.
   */
  void methodThrew( Object targetObject, Class<?> targetType, Method method,
                    Object[] arguments, Throwable exception  );

} // interface InvocationReporter
