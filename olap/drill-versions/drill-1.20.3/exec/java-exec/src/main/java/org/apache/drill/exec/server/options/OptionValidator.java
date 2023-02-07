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
package org.apache.drill.exec.server.options;

import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.server.options.OptionValue.Kind;

/**
 * Validates the values provided to Drill options.
 */
public abstract class OptionValidator {
  // Stored here as well as in the option static class to allow insertion of option optionName into
  // the error messages produced by the validator
  private final String optionName;
  private final OptionDescription description;

  /** By default, if admin option value is not specified, it would be set to false.*/
  public OptionValidator(String optionName, OptionDescription description) {
    this.optionName = optionName;
    this.description = description;
  }

  /**
   * Gets the name of the option for this validator.
   *
   * @return the option name
   */
  public String getOptionName() {
    return optionName;
  }

  /**
   * Get the option description (long and short)
   * @return the description
   */
  public OptionDescription getOptionDescription() {
    return description;
  }

  /**
   * This function returns true if and only if this validator is meant for a short-lived option.
   *
   * NOTE: By default, options are not short-lived. So, if a derived class is meant for a short-lived option,
   * that class must do two things:
   * (1) override this method to return true, and
   * (2) return the number of queries for which the option is valid through {@link #getTtl}.
   * E.g. {@link org.apache.drill.exec.testing.ExecutionControls.ControlsOptionValidator}
   *
   * @return if this validator is for a short-lived option
   */
  public boolean isShortLived() {
    return false;
  }

  /**
   * If an option is short-lived, this method returns the number of queries for which the option is valid.
   * Please read the note at {@link #isShortLived}
   *
   * @return number of queries for which the option should be valid
   */
  public int getTtl() {
    if (!isShortLived()) {
      throw new UnsupportedOperationException("This option is not short-lived.");
    }
    return 0;
  }

  /**
   * Validates the option value.
   *
   * @param value the value to validate
   * @param manager the manager for accessing validation dependencies (options)
   * @throws UserException message to describe error with value, including range or list of expected values
   */
  public abstract void validate(OptionValue value, OptionMetaData optionMetaData, OptionSet manager);

  /**
   * Gets the kind of this option value for this validator.
   *
   * @return kind of this option value
   */
  public abstract Kind getKind();

  public String getConfigProperty() {
    return ExecConstants.bootDefaultFor(getOptionName());
  }

  public static class OptionDescription {
    private String description;
    private String shortDescription;

    /**
     * Constructor for option's description
     * @param description verbose format
     */
    public OptionDescription(String description) {
      this(description, null);
    }

    /**
     * Constructor for option's description
     * @param description verbose format
     * @param shortDescription short format
     */
    public OptionDescription(String description, String shortDescription) {
      this.description = description;
      this.shortDescription = shortDescription;
    }

    /**
     * Get verbose description
     * @return verbose description
     */
    public String getDescription() {
      return description;
    }

    /**
     * Get short description (typically for system tables)
     * @return short description
     */
    public String getShortDescription() {
      return shortDescription;
    }

    /**
     * Check for explicit short description
     * @return true if a short description explicitly exists
     */
    public boolean hasShortDescription() {
      return (shortDescription != null);
    }

  }
}
