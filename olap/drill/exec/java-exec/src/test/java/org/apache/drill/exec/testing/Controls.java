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

import java.util.List;

import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

import static org.apache.drill.exec.testing.ExecutionControls.EMPTY_CONTROLS;

public class Controls {

  /**
   * Returns a builder that can be used to add injections.
   *
   * @return a builder instance
   */
  public static Builder newBuilder() {
    return new Builder();
  }

  /**
   * Constructor. To disallow building controls without the builder.
   */
  private Controls() {
  }

  /**
   * A builder to create a controls string, a JSON that holds a list of injections that are to be injected in code for
   * testing purposes. This string is passed through the
   * {@link org.apache.drill.exec.ExecConstants#DRILLBIT_CONTROL_INJECTIONS} session option.
   * <p/>
   * The builder class can be reused; it is safe to call build() multiple times to build multiple controls strings in
   * series. Each new controls string contains all the injections added to the builder before it.
   */
  public static class Builder {

    private final List<String> injections = Lists.newArrayList();

    public Builder() {
    }

    /**
     * Adds an exception injection to the controls builder with the given parameters.
     *
     * @param siteClass      class where the exception should be thrown
     * @param desc           descriptor for the exception site in the site class
     * @param exceptionClass class of the exception to throw
     * @param nSkip          number of times to skip before firing
     * @param nFire          number of times to fire the exception
     * @return this builder
     */
    public Builder addException(final Class<?> siteClass, final String desc,
                                final Class<? extends Throwable> exceptionClass, final int nSkip,
                                final int nFire) {
      injections.add(ControlsInjectionUtil.createException(siteClass, desc, nSkip, nFire, exceptionClass));
      return this;
    }

    /**
     * Adds an exception injection to the controls builder with the given parameters. The injection is not skipped, and
     * the exception is thrown when execution reaches the site.
     *
     * @param siteClass      class where the exception should be thrown
     * @param desc           descriptor for the exception site in the site class
     * @param exceptionClass class of the exception to throw
     * @return this builder
     */
    public Builder addException(final Class<?> siteClass, final String desc,
                                final Class<? extends Throwable> exceptionClass) {
      return addException(siteClass, desc, exceptionClass, 0, 1);
    }

    /**
     * Adds an exception injection (for the specified drillbit) to the controls builder with the given parameters.
     *
     * @param siteClass      class where the exception should be thrown
     * @param desc           descriptor for the exception site in the site class
     * @param exceptionClass class of the exception to throw
     * @param endpoint       the endpoint of the drillbit on which to inject
     * @param nSkip          number of times to skip before firing
     * @param nFire          number of times to fire the exception
     * @return this builder
     */
    public Builder addExceptionOnBit(final Class<?> siteClass, final String desc,
                                     final Class<? extends Throwable> exceptionClass,
                                     final DrillbitEndpoint endpoint, final int nSkip,
                                     final int nFire) {
      injections.add(ControlsInjectionUtil.createExceptionOnBit(siteClass, desc, nSkip, nFire, exceptionClass,
        endpoint));
      return this;
    }

    /**
     * Adds an exception injection (for the specified drillbit) to the controls builder with the given parameters. The
     * injection is not skipped, and the exception is thrown when execution reaches the site on the specified drillbit.
     *
     * @param siteClass      class where the exception should be thrown
     * @param desc           descriptor for the exception site in the site class
     * @param exceptionClass class of the exception to throw
     * @param endpoint       endpoint of the drillbit on which to inject
     * @return this builder
     */
    public Builder addExceptionOnBit(final Class<?> siteClass, final String desc,
                                     final Class<? extends Throwable> exceptionClass,
                                     final DrillbitEndpoint endpoint) {
      return addExceptionOnBit(siteClass, desc, exceptionClass, endpoint, 0, 1);
    }

    /**
     * Adds a pause injection to the controls builder with the given parameters.
     *
     * @param siteClass class where the pause should happen
     * @param desc      descriptor for the pause site in the site class
     * @param nSkip     number of times to skip before firing
     * @return this builder
     */
    public Builder addPause(final Class<?> siteClass, final String desc, final int nSkip) {
      injections.add(ControlsInjectionUtil.createPause(siteClass, desc, nSkip));
      return this;
    }

    /**
     * Adds a time-bound pause injection to the controls builder with the given parameters.
     *
     * @param siteClass class where the pause should happen
     * @param desc      descriptor for the pause site in the site class
     * @param nSkip     number of times to skip before firing
     * @param msPause     duration of the pause in millisec
     * @return this builder
     */
    public Builder addTimedPause(final Class<?> siteClass, final String desc, final int nSkip, final long msPause) {
      injections.add(ControlsInjectionUtil.createTimedPause(siteClass, desc, nSkip, msPause));
      return this;
    }

    /**
     * Adds a pause injection to the controls builder with the given parameters. The pause is not skipped i.e. the pause
     * happens when execution reaches the site.
     *
     * @param siteClass class where the pause should happen
     * @param desc      descriptor for the pause site in the site class
     * @return this builder
     */
    public Builder addPause(final Class<?> siteClass, final String desc) {
      return addPause(siteClass, desc, 0);
    }

    /**
     * Adds a pause injection (for the specified drillbit) to the controls builder with the given parameters.
     *
     * @param siteClass class where the pause should happen
     * @param desc      descriptor for the pause site in the site class
     * @param nSkip     number of times to skip before firing
     * @return this builder
     */
    public Builder addPauseOnBit(final Class<?> siteClass, final String desc,
                                 final DrillbitEndpoint endpoint, final int nSkip) {
      injections.add(ControlsInjectionUtil.createPauseOnBit(siteClass, desc, nSkip, endpoint));
      return this;
    }

    /**
     * Adds a time-bound pause injection (for the specified drillbit) to the controls builder with the given parameters.
     *
     * @param siteClass class where the pause should happen
     * @param desc      descriptor for the pause site in the site class
     * @param nSkip     number of times to skip before firing
     * @param msPause     duration of the pause in millisec
     * @return this builder
     */
    public Builder addTimedPauseOnBit(final Class<?> siteClass, final String desc,
                                 final DrillbitEndpoint endpoint, final int nSkip, final long msPause) {
      injections.add(ControlsInjectionUtil.createTimedPauseOnBit(siteClass, desc, nSkip, endpoint, msPause));
      return this;
    }

    /**
     * Adds a pause injection (for the specified drillbit) to the controls builder with the given parameters. The pause
     * is not skipped i.e. the pause happens when execution reaches the site.
     *
     * @param siteClass class where the pause should happen
     * @param desc      descriptor for the pause site in the site class
     * @return this builder
     */
    public Builder addPauseOnBit(final Class<?> siteClass, final String desc,
                                 final DrillbitEndpoint endpoint) {
      return addPauseOnBit(siteClass, desc, endpoint, 0);
    }

    /**
     * Adds a count down latch to the controls builder with the given parameters.
     *
     * @param siteClass class where the latch should be injected
     * @param desc      descriptor for the latch in the site class
     * @return this builder
     */
    public Builder addLatch(final Class<?> siteClass, final String desc) {
      injections.add(ControlsInjectionUtil.createLatch(siteClass, desc));
      return this;
    }

    /**
     * Builds the controls string.
     *
     * @return a validated controls string with the added injections
     * @throws java.lang.AssertionError if controls cannot be validated using
     *                                  {@link org.apache.drill.exec.testing.ExecutionControls#controlsOptionMapper}
     */
    public String build() {
      if (injections.size() == 0) {
        return EMPTY_CONTROLS;
      }

      final StringBuilder builder = new StringBuilder("{ \"injections\" : [");
      for (final String injection : injections) {
        builder.append(injection)
          .append(",");
      }
      builder.setLength(builder.length() - 1); // remove the extra ","
      builder.append("]}");
      final String controls = builder.toString();
      ControlsInjectionUtil.validateControlsString(controls);
      return controls;
    }
  }
}
