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
package org.apache.drill.exec.testing;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Injection for a single exception. Specifies how many times to inject it, and how many times to skip
 * injecting it before the first injection. This class is used internally for tracking injected
 * exceptions; injected exceptions are specified via the
 * {@link org.apache.drill.exec.ExecConstants#DRILLBIT_CONTROL_INJECTIONS} session option.
 */
@JsonAutoDetect(fieldVisibility = Visibility.ANY)
public class ExceptionInjection extends Injection {

  private final Class<? extends Throwable> exceptionClass;

  @JsonCreator // ensures instances are created only through JSON
  private ExceptionInjection(@JsonProperty("address") final String address,
                             @JsonProperty("port") final int port,
                             @JsonProperty("siteClass") final String siteClass,
                             @JsonProperty("desc") final String desc,
                             @JsonProperty("nSkip") final int nSkip,
                             @JsonProperty("nFire") final int nFire,
                             @JsonProperty("exceptionClass") String classString) throws InjectionConfigurationException {
    super(address, port, siteClass, desc, nSkip, nFire, 0L);
    final Class<?> clazz;
    try {
      clazz = Class.forName(classString);
    } catch (ClassNotFoundException e) {
      throw new InjectionConfigurationException("Injected exceptionClass not found.", e);
    }

    if (!Throwable.class.isAssignableFrom(clazz)) {
      throw new InjectionConfigurationException("Injected exceptionClass is not a Throwable.");
    }

    @SuppressWarnings("unchecked")
    final Class<? extends Throwable> exceptionClazz = (Class<? extends Throwable>) clazz;
    this.exceptionClass = exceptionClazz;
  }

  /**
   * Constructs the exception to throw, if it is time to throw it.
   *
   * @return the exception to throw, or null if it isn't time to throw it
   */
  private Throwable constructException() {
    if (! injectNow()) {
      return null;
    }

    // if we get here, we should throw the specified exception
    final Constructor<?> constructor;
    try {
      constructor = exceptionClass.getConstructor(String.class);
    } catch (NoSuchMethodException e) {
      // this should not throw; validated already.
      throw new RuntimeException("No constructor found that takes a single String argument.");
    }

    final Throwable throwable;
    try {
      throwable = (Throwable) constructor.newInstance(getDesc());
    } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
      // this should not throw; validated already.
      throw new IllegalStateException("Couldn't construct exception instance.", e);
    }

    return throwable;
  }

  /**
   * Throw the unchecked exception specified by this injection.
   *
   * @throws IllegalStateException if it's time to throw, and the injection specified a checked exception
   */
  public void throwUnchecked() {
    final Throwable throwable = constructException();
    if (throwable == null) {
      return;
    }

    if (throwable instanceof RuntimeException) {
      final RuntimeException e = (RuntimeException) throwable;
      throw e;
    }
    if (throwable instanceof Error) {
      final Error e = (Error) throwable;
      throw e;
    }

    throw new IllegalStateException("Throwable was not an unchecked exception.");
  }

  /**
   * Throw the checked exception specified by this injection.
   *
   * @param exceptionClass the class of the exception to throw
   * @throws T                     if it is time to throw the exception
   * @throws IllegalStateException if it is time to throw the exception, and the exception's class
   *                               is incompatible with the class specified by the injection
   */
  public <T extends Throwable> void throwChecked(final Class<T> exceptionClass) throws T {
    final Throwable throwable = constructException();
    if (throwable == null) {
      return;
    }

    if (exceptionClass.isAssignableFrom(throwable.getClass())) {
      final T exception = exceptionClass.cast(throwable);
      throw exception;
    }

    throw new IllegalStateException("Constructed Throwable(" + throwable.getClass().getName()
      + ") is incompatible with exceptionClass(" + exceptionClass.getName() + ")");
  }
}
