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

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.apache.drill.shaded.guava.com.google.common.annotations.VisibleForTesting;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.exec.server.options.OptionManager;
import org.apache.drill.exec.server.options.OptionMetaData;
import org.apache.drill.exec.server.options.OptionSet;
import org.apache.drill.exec.server.options.OptionValue;
import org.apache.drill.exec.server.options.TypeValidators.TypeValidator;
import org.apache.drill.exec.testing.InjectionSite.InjectionSiteKeyDeserializer;
import org.apache.drill.exec.util.AssertionUtil;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Tracks the simulated controls that will be injected for testing purposes.
 */
public final class ExecutionControls {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ExecutionControls.class);

  // used to map JSON specified injections to POJOs
  public static final ObjectMapper controlsOptionMapper = new ObjectMapper();

  static {
    controlsOptionMapper.addMixInAnnotations(Injection.class, InjectionMixIn.class);
  }

  // Jackson MixIn: an annotated class that is used only by Jackson's ObjectMapper to allow a list of injections to
  // hold various types of injections
  @JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type")
  @JsonSubTypes({
    @Type(value = ExceptionInjection.class, name = "exception"),
    @Type(value = CountDownLatchInjectionImpl.class, name = "latch"),
    @Type(value = PauseInjection.class, name = "pause")})
  public static abstract class InjectionMixIn {
  }

  /**
   * The JSON specified for the {@link org.apache.drill.exec.ExecConstants#DRILLBIT_CONTROL_INJECTIONS}
   * option is validated using this class. Controls are short-lived options.
   */
  public static class ControlsOptionValidator extends TypeValidator {

    private final int ttl; // the number of queries for which this option is valid

    /**
     * Constructor for controls option validator.
     *  @param name the name of the validator
     * @param ttl  the number of queries for which this option should be valid
     * @param description Description of the option
     */
    public ControlsOptionValidator(final String name, final int ttl, OptionDescription description) {
      super(name, OptionValue.Kind.STRING, description);
      assert ttl > 0;
      this.ttl = ttl;
    }

    @Override
    public int getTtl() {
      return ttl;
    }

    @Override
    public boolean isShortLived() {
      return true;
    }

    @Override
    public void validate(final OptionValue v, final OptionMetaData metaData, final OptionSet manager) {
      final String jsonString = v.string_val;
      try {
        validateControlsString(jsonString);
      } catch (final IOException e) {
        throw UserException.validationError()
            .message(String.format("Invalid controls option string (%s) due to %s.", jsonString, e.getMessage()))
            .build(logger);
      }
    }
  }

  /**
   * POJO used to parse JSON-specified controls.
   */
  private static class Controls {
    public Collection<? extends Injection> injections;
  }

  public static void validateControlsString(final String jsonString) throws IOException {
    controlsOptionMapper.readValue(jsonString, Controls.class);
  }

  /**
   * The default value for controls.
   */
  public static final String EMPTY_CONTROLS = "{\"injections\" : []}";

  /**
   * Caches the currently specified controls.
   */
  @JsonDeserialize(keyUsing = InjectionSiteKeyDeserializer.class)
  private final Map<InjectionSite, Injection> controls = new HashMap<>();

  private final DrillbitEndpoint endpoint; // the current endpoint

  @VisibleForTesting
  public ExecutionControls(final OptionManager options) {
    this(options, null);
  }

  public ExecutionControls(final OptionManager options, final DrillbitEndpoint endpoint) {
    this.endpoint = endpoint;

    if (!AssertionUtil.isAssertionsEnabled()) {
      return;
    }

    final OptionValue optionValue = options.getOption(ExecConstants.DRILLBIT_CONTROL_INJECTIONS);
    if (optionValue == null) {
      return;
    }

    final String opString = optionValue.string_val;
    final Controls controls;
    try {
      controls = controlsOptionMapper.readValue(opString, Controls.class);
    } catch (final IOException e) {
      // This never happens. opString must have been validated.
      logger.warn("Could not parse injections. Injections must have been validated before this point.");
      throw new DrillRuntimeException("Could not parse injections.", e);
    }
    if (controls.injections == null) {
      return;
    }

    logger.debug("Adding control injections: \n{}", opString);
    for (final Injection injection : controls.injections) {
      this.controls.put(new InjectionSite(injection.getSiteClass(), injection.getDesc()), injection);
    }
  }

  /**
   * Look for an exception injection matching the given injector, site descriptor, and endpoint.
   *
   * @param injector the injector, which indicates a class
   * @param desc     the injection site description
   * @return the exception injection, if there is one for the injector, site and endpoint; null otherwise
   */
  public ExceptionInjection lookupExceptionInjection(final ExecutionControlsInjector injector, final String desc) {
    final Injection injection = lookupInjection(injector, desc);
    return injection != null ? (ExceptionInjection) injection : null;
  }

  /**
   * Look for an pause injection matching the given injector, site descriptor, and endpoint.
   *
   * @param injector the injector, which indicates a class
   * @param desc     the injection site description
   * @return the pause injection, if there is one for the injector, site and endpoint; null otherwise
   */
  public PauseInjection lookupPauseInjection(final ExecutionControlsInjector injector, final String desc) {
    final Injection injection = lookupInjection(injector, desc);
    return injection != null ? (PauseInjection) injection : null;
  }

  /**
   * Look for a count down latch injection matching the given injector, site descriptor, and endpoint.
   *
   * @param injector the injector, which indicates a class
   * @param desc     the injection site description
   * @return the count down latch injection, if there is one for the injector, site and endpoint;
   * otherwise, a latch that does nothing
   */
  public CountDownLatchInjection lookupCountDownLatchInjection(final ExecutionControlsInjector injector,
                                                               final String desc) {
    final Injection injection = lookupInjection(injector, desc);
    return injection != null ? (CountDownLatchInjection) injection : NoOpControlsInjector.LATCH;
  }

  private Injection lookupInjection(final ExecutionControlsInjector injector, final String desc) {
    if (controls.isEmpty()) {
      return null;
    }

    // lookup the request
    final InjectionSite site = new InjectionSite(injector.getSiteClass(), desc);
    final Injection injection = controls.get(site);
    if (injection == null) {
      return null;
    }
    // return only if injection was meant for this drillbit
    return injection.isValidForBit(endpoint) ? injection : null;
  }

  /**
   * This method resumes all pauses within the current context (QueryContext or FragmentContext).
   */
  public void unpauseAll() {
    for (final Injection injection : controls.values()) {
      if (injection instanceof PauseInjection) {
        ((PauseInjection) injection).unpause();
      }
    }
  }
}
