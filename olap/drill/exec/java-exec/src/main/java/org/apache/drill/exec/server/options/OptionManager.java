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

import javax.validation.constraints.NotNull;

/**
 * Manager for Drill {@link OptionValue options}. Implementations must be case-insensitive to the name of an option.
 *
 * The options governed by an {@link OptionManager} fall into various categories. These categories are described below.
 *
 * <ul>
 *   <li>
 *     <b>Local:</b> Local options are options who have a value stored in this {@link OptionManager}. Whether an option is <b>Local</b> to an {@link OptionManager} or not should
 *      be irrelevant to the user.
 *   </li>
 *   <li>
 *     <b>Public:</b> Public options are options that are visible to end users in all the standard tables and rest endpoints.
 *   </li>
 *   <li>
 *     <b>Internal:</b> Internal options are options that are only visible to end users if they check special tables and rest endpoints that are not documented. These options
 *     are not intended to be modified by users and should only be modified by support during debugging. Internal options are also not gauranteed to be consistent accross
 *     patch, minor, or major releases.
 *   </li>
 * </ul>
 *
 */
public interface OptionManager extends OptionSet, Iterable<OptionValue> {

  /**
   * Sets a boolean option on the {@link OptionManager}.
   * @param name The name of the option.
   * @param value The value of the option.
   */
  void setLocalOption(String name, boolean value);

  /**
   * Sets a long option on the {@link OptionManager}.
   * @param name The name of the option.
   * @param value The value of the option.
   */
  void setLocalOption(String name, long value);

  /**
   * Sets a double option on the {@link OptionManager}.
   * @param name The name of the option.
   * @param value The value of the option.
   */
  void setLocalOption(String name, double value);

  /**
   * Sets a String option on the {@link OptionManager}.
   * @param name The name of the option.
   * @param value The value of the option.
   */
  void setLocalOption(String name, String value);

  /**
   * Sets an option on the {@link OptionManager}.
   * @param name The name of the option.
   * @param value The value of the option.
   */
  void setLocalOption(String name, Object value);

  /**
   * Sets an option of the specified {@link OptionValue.Kind} on the {@link OptionManager}.
   * @param kind The kind of the option.
   * @param name The name of the option.
   * @param value The value of the option.
   */
  void setLocalOption(OptionValue.Kind kind, String name, String value);

  /**
   * Deletes the option.
   *
   * If the option name is valid (exists in the set of validators produced by {@link SystemOptionManager#createDefaultOptionDefinitions()}),
   * but the option was not set within this manager, calling this method should be a no-op.
   *
   * @param name option name
   * @throws org.apache.drill.common.exceptions.UserException message to describe error with value
   */
  void deleteLocalOption(String name);

  /**
   * Deletes all options.
   *
   * If no options are set, calling this method should be no-op.
   *
   * @throws org.apache.drill.common.exceptions.UserException message to describe error with value
   */
  void deleteAllLocalOptions();

  /**
   * Get the option definition corresponding to the given option name.
   * @param name The name of the option to retrieve a validator for.
   * @return The option validator corresponding to the given option name.
   * @throws UserException - if the definition is not found
   */
  @NotNull
  OptionDefinition getOptionDefinition(String name);

  /**
   * Gets the list of options managed this manager.
   *
   * @return the list of options
   */
  OptionList getOptionList();

  /**
   * Returns all the internal options contained in this option manager.
   *
   * @return All the internal options contained in this option manager.
   */
  @NotNull
  OptionList getInternalOptionList();

  /**
   * Returns all the public options contained in this option manager.
   *
   * @return All the public options contained in this option manager.
   */
  @NotNull
  OptionList getPublicOptionList();
}
