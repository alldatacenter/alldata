/**
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

package org.apache.ambari.view.validation;

import org.apache.ambari.view.ViewInstanceDefinition;

/**
 * Interface for custom view validation.  The validator is used validate a view instance and
 * its properties.
 */
public interface Validator {
  /**
   * Validate the given view instance definition.  Return {@code null} to indicate that
   * no validation was performed.
   *
   * @param definition  the view instance definition
   * @param mode        the validation mode
   *
   * @return the instance validation result; may be {@code null}
   */
  public ValidationResult validateInstance(ViewInstanceDefinition definition, ValidationContext mode);

  /**
   * Validate a property of the given view instance definition.  Return {@code null} to indicate that
   * no validation was performed.
   *
   * @param property    the property name
   * @param definition  the view instance definition
   * @param mode        the validation modes
   *
   * @return the instance validation result; may be {@code null}
   */
  public ValidationResult validateProperty(String property, ViewInstanceDefinition definition, ValidationContext mode);

  /**
   * The context in which a view instance validation check is performed.
   */
  public enum ValidationContext {
    PRE_CREATE,  // validation prior to creation
    PRE_UPDATE,  // validation prior to update
    EXISTING     // validation of an existing view instance
  }
}
