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
package org.apache.ambari.view.property;

import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.ViewInstanceDefinition;
import org.apache.ambari.view.validation.Validator;
import org.apache.ambari.view.validation.ValidationResult;

import java.util.Map;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.URISyntaxException;

/**
 * The validator class for the PropertyValidator view.
 *
 */
public class MyValidator implements Validator {

    private static  final   String      PARAMETER_NAME_FREEFORM = "freeform";
    private static  final   String      PARAMETER_NAME_INTEGER = "integer";
    private static  final   String      PARAMETER_NAME_FIRST_VALUE = "first.value";
    private static  final   String      PARAMETER_NAME_SECOND_VALUE = "second.value";
    private static  final   String      PARAMETER_NAME_URL = "url";

    private static  final   int         SECOND_VALUE_MAX = 100;
    
    @Override
    public ValidationResult validateInstance(ViewInstanceDefinition definition, ValidationContext mode) {
        // get the instance props
        Map<String,String> props = definition.getPropertyMap();
        
        // do a check to confirm first is less than or equals to second    
        return  validateParameterFirstSecondValues(props);
    }

    @Override
    public ValidationResult validateProperty(String property, ViewInstanceDefinition definition, ValidationContext mode) {
        if (property.equals(PARAMETER_NAME_FREEFORM)) {
            // do nothing, this property can be free form
        } else if (property.equals(PARAMETER_NAME_URL)) {
            return  validateParameterURL(definition.getPropertyMap());
        } else if (property.equals(PARAMETER_NAME_INTEGER)) {
            return  validateParameterInteger(definition.getPropertyMap());
        } else if (property.equals(PARAMETER_NAME_FIRST_VALUE)) {
            return  validateParameterFirst(definition.getPropertyMap());
        } else if (property.equals(PARAMETER_NAME_SECOND_VALUE)) {
            return  validateParameterSecond(definition.getPropertyMap());
        }
        
        return ValidationResult.SUCCESS;
    }

    /**
     * Validates the parameter URL.
     *
     * @param   properties      the view instance parameters
     * @return  the validation result
     */
    private ValidationResult    validateParameterURL(Map<String,String> properties) {
        String urlProp = properties.get(PARAMETER_NAME_URL);

        URL u = null;
        try {
            u = new URL(urlProp);
        } catch (MalformedURLException e) {
            return  new MyValidationResult(false, "Must be valid URL");
        }

        try {
            u.toURI();
        } catch (URISyntaxException e) {
            return  new MyValidationResult(false, "Must be valid URL");
        }
    
        return  ValidationResult.SUCCESS;
    }

    /**
     * Validates the parameter Integer.
     *
     * @param   properties      the view instance parameters
     * @return  the validation result
     */
    private ValidationResult    validateParameterInteger(Map<String,String> properties) {
        String val = properties.get(PARAMETER_NAME_INTEGER);
        int intValue = -1;
        try {
            checkInteger(val);
        } catch (NumberFormatException nfe) {
            return  new MyValidationResult(false, "Must be an integer");
        }
        return  ValidationResult.SUCCESS;
    }
    
    /**
     * Validates the parameter first value.
     *
     * @param   properties      the view instance parameters
     * @return  the validation result
     */
    private ValidationResult    validateParameterFirst(Map<String,String> properties) {
        String val = properties.get(PARAMETER_NAME_FIRST_VALUE);
        int intValue = -1;
        try {
            checkInteger(val);
        } catch (NumberFormatException nfe) {
            return  new MyValidationResult(false, "Must be an integer");
        }
        return  ValidationResult.SUCCESS;
    }
    
    /**
     * Validates the parameter second value.
     *
     * @param   properties      the view instance parameters
     * @return  the validation result
     */
    private ValidationResult    validateParameterSecond(Map<String,String> properties) {
        String val = properties.get(PARAMETER_NAME_SECOND_VALUE);
        int intValue = -1;
        try {
            intValue = checkInteger(val);
        } catch (NumberFormatException nfe) {
            return  new MyValidationResult(false, "Must be an integer");
        }

        if (intValue > SECOND_VALUE_MAX)
            return  new MyValidationResult(false, "Must be less than "+SECOND_VALUE_MAX);

        return  ValidationResult.SUCCESS;
    }
    
    /**
     * Validates the parameter First and Second values.
     *
     * @param   properties      the view instance parameters
     * @return  the validation result
     */
    private ValidationResult    validateParameterFirstSecondValues(Map<String,String> properties) {
        // check that first is an int
        String firstValue = properties.get(PARAMETER_NAME_FIRST_VALUE);
        int firstIntValue = -1;
        try {
            firstIntValue = checkInteger(firstValue);
        } catch (NumberFormatException nfe) {
            return  new MyValidationResult(false, "Must be an integer");
        }
        
        // check that second is an int
        String secondValue = properties.get(PARAMETER_NAME_SECOND_VALUE);
        int secondIntValue = -1;
        try {
            secondIntValue = checkInteger(secondValue);
        } catch (NumberFormatException nfe) {
            return  new MyValidationResult(false, "Must be an integer");
        }

        // check that second >= first
        if (secondIntValue < firstIntValue)
            return  new MyValidationResult(false, "Second value must be greater or equal to first");
        
        return  ValidationResult.SUCCESS;
    }
   
    /**
     * Check integer.
     *
     * @param   value      the value
     * @return  the resulting integer
     * @throws  NumberFormatException if the value is not a valid integer
     */
    private static  int checkInteger(String value) {
        return  Integer.parseInt(value);
    }
 
    /**
     * Represents a validation result for this validator.
     *
     */
    private static  class    MyValidationResult implements ValidationResult {

        private boolean     valid;
        private String      details;
    
        /**
         * Constructor.
         *
         * @param   valid       if <code>true</code>, result is valid
         * @param   details     the details of the result
         *
         */
        public  MyValidationResult(boolean valid, String details) {
            this.valid = valid;
            this.details = details;
        }
        
        @Override
        public boolean isValid() {
            return  this.valid;
        }

        @Override
        public String getDetail() {
            return  this.details;
        }

    } // end inner MyValidationResult class
    
} // end MyValidator class
