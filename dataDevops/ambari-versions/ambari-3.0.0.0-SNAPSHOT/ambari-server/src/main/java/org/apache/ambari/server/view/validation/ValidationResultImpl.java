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

package org.apache.ambari.server.view.validation;

import org.apache.ambari.view.validation.ValidationResult;

/**
 * Simple validation result implementation.
 */
public class ValidationResultImpl implements ValidationResult{
  /**
   * Indicates whether or not the result is valid.
   */
  private final boolean valid;

  /**
   * Detail message for the result.
   */
  private final String detail;


  // ----- Constructors ------------------------------------------------------

  /**
   * Construct a validation result.
   *
   * @param valid   indicates whether or not the result is valid
   * @param detail  detail message for the result
   */
  public ValidationResultImpl(boolean valid, String detail) {
    this.valid  = valid;
    this.detail = detail;
  }


  // ----- ValidationResult --------------------------------------------------

  @Override
  public boolean isValid() {
    return valid;
  }

  @Override
  public String getDetail() {
    return detail;
  }


  // ----- helper methods ----------------------------------------------------

  /**
   * Factory method to create a validation result from an existing result (accounts for null).
   *
   * @param result  the validation result; may be null
   *
   * @return a new validation result
   */
  public static ValidationResult create(ValidationResult result) {
    if (result == null) {
      result = SUCCESS;
    }
    return new ValidationResultImpl(result.isValid(), result.getDetail());
  }
}
