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

/**
 * Immutable set of options accessible by name or validator.
 */
public interface OptionSet {

  /**
   * Gets the option value for the given option name.
   *
   * This interface also provides convenient methods to get typed option values:
   * {@link #getOption(TypeValidators.BooleanValidator validator)},
   * {@link #getOption(TypeValidators.DoubleValidator validator)},
   * {@link #getOption(TypeValidators.LongValidator validator)}, and
   * {@link #getOption(TypeValidators.StringValidator validator)}.
   *
   * @param name option name
   * @return the option value, null if the option does not exist
   */
  OptionValue getOption(String name);

  /**
   * Gets the default value for the specified option.
   * @param optionName The option to retrieve the default value for.
   * @return The default value for the option.
   */
  OptionValue getDefault(String optionName);

  /**
   * Gets the boolean value (from the option value) for the given boolean validator.
   *
   * @param validator the boolean validator
   * @return the boolean value
   */
  boolean getOption(TypeValidators.BooleanValidator validator);

  /**
   * Gets the double value (from the option value) for the given double validator.
   *
   * @param validator the double validator
   * @return the double value
   */
  double getOption(TypeValidators.DoubleValidator validator);

  /**
   * Gets the long value (from the option value) for the given long validator.
   *
   * @param validator the long validator
   * @return the long value
   */
  long getOption(TypeValidators.LongValidator validator);

  /**
   * Gets the string value (from the option value) for the given string validator.
   *
   * @param validator the string validator
   * @return the string value
   */
  String getOption(TypeValidators.StringValidator validator);

  /**
   * Return the value of a Boolean option.
   *
   * @param name option name
   * @return the Boolean value
   * @throws IllegalArgumentException if the option is undefined or
   * is not of the correct data type
   */
  boolean getBoolean(String name);

  /**
   * Return the value of a long option as an int
   *
   * @param name option name
   * @return the long value
   * @throws IllegalArgumentException if the option is undefined or
   * is not of the correct data type
   */
  int getInt(String name);

  /**
   * Return the value of a long option.
   *
   * @param name option name
   * @return the long value
   * @throws IllegalArgumentException if the option is undefined or
   * is not of the correct data type
   */
  long getLong(String name);

  /**
   * Return the value of a double option.
   *
   * @param name option name
   * @return the double value
   * @throws IllegalArgumentException if the option is undefined or
   * is not of the correct data type
   */
  double getDouble(String name);

  /**
   * Return the value of a String option.
   *
   * @param name option name
   * @return the String value
   * @throws IllegalArgumentException if the option is undefined or
   * is not of the correct data type
   */
  String getString(String name);
}
