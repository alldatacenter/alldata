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

import java.util.Map;

import org.apache.ambari.view.validation.ValidationResult;

import com.google.gson.Gson;

/**
 * View instance validation result.  This result includes the validation results
 * for the associated view instance and its properties.
 */
public class InstanceValidationResultImpl extends ValidationResultImpl {

  /**
   * Static Gson instance.
   */
  private static final Gson GSON = new Gson();

  /**
   * The validation results for the associated instance's properties
   */
  private final Map<String, ValidationResult> propertyResults;


  // ----- Constructors ------------------------------------------------------

  /**
   * Construct an instance validation result.
   *
   * @param instanceResult   the results of the instance validation
   * @param propertyResults  the results of the property validations
   */
  public InstanceValidationResultImpl(ValidationResult instanceResult, Map<String, ValidationResult> propertyResults) {
    super(isValid(instanceResult, propertyResults), getDetail(instanceResult, propertyResults));
    this.propertyResults = propertyResults;
  }


  // ----- InstanceValidationResultImpl --------------------------------------

  /**
   * Get the validation results for the properties of the associated instance.
   *
   * @return the property validation results
   */
  public Map<String, ValidationResult> getPropertyResults() {
    return propertyResults;
  }

  /**
   * Return this result as a JSON string.
   *
   * @return this result in JSON format
   */
  public String toJson() {
    return GSON.toJson(this);
  }


  // ----- helper methods ----------------------------------------------------

  // determine whether or not the given instance and property results are valid
  private static boolean isValid(ValidationResult instanceResult, Map<String, ValidationResult> propertyResults) {
    boolean instanceValid = instanceResult.isValid();
    if (instanceValid) {
      for (Map.Entry<String, ValidationResult> entry : propertyResults.entrySet()) {
        ValidationResult propertyResult = entry.getValue();
        if (propertyResult != null && !propertyResult.isValid()) {
          return false;
        }
      }
    }
    return instanceValid;
  }

  // get a detail message from the given instance and property results
  private static String getDetail(ValidationResult instanceResult, Map<String, ValidationResult> propertyResults) {
    if (instanceResult.isValid()) {
      for (Map.Entry<String, ValidationResult> entry : propertyResults.entrySet()) {
        ValidationResult propertyResult = entry.getValue();
        if (propertyResult != null && !propertyResult.isValid()) {
          return "The instance has invalid properties.";
        }
      }
    }
    return instanceResult.getDetail();
  }
}
