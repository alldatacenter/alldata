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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ranger.plugin.errors;

import com.google.common.collect.ImmutableSet;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by alal on 7/30/15.
 */
public class TestValidationErrorCode {

    @Test
    public void testUserMessage() throws Exception {
        ValidationErrorCode errorCode = ValidationErrorCode.SERVICE_VALIDATION_ERR_UNSUPPORTED_ACTION;
        String aParameter = "FOO";
        String expected = errorCode._template.replace("{0}", aParameter);
        Assert.assertEquals(expected, errorCode.getMessage(aParameter));
    }

    /**
     * tests if template has any trivial template variable problems, e.g. if template has {3} in it then it
     * better also have {0}, {1} and {2} in it else MessageFormat output might be unexpected.
     *
     * This check is far from perfect.  It may give false alarms if the message itself contains strings of the form {1}
     * which have been escaped using single quotes.  If that happens we would have to make this test smarter.
     */
    @Test
    public void testTemplates() {

        // we check up to 5 substitution variables.  If there are more than 5 then you probably have a different set of problems
        Set<ValidationErrorCode> may = ImmutableSet.copyOf(ValidationErrorCode.values());

        // set of enums that must not hvae any subsequent placeholders in it
        Set<ValidationErrorCode> mustNot = new HashSet<>();

        for (int i = 0; i < 5; i++) {
            String token = String.format("{%d", i);
            // check which ones should not have anymore substition varabile placehoders in them, {0}, {1}, etc.
            for (ValidationErrorCode anEnum : may) {
                if (!anEnum._template.contains(token)) {
                    // if template does not have {1} then it surely must not have {2}, {3}, etc.
                    mustNot.add(anEnum);
                }
            }
            // check for incorrectly numbers substition variable placeholders
            for (ValidationErrorCode anEnum : mustNot) {
                Assert.assertFalse(anEnum.toString() + ": contains " + token + ". Check for wongly numberd substition variable placeholders.",
                        anEnum._template.contains(token));
            }
        }
    }

    /**
     * Test if the values assigned to the validation error code are unique or not.
     */
    @Test
    public void testValidationErrorCodesUnique() {
        Set<Integer> errorCodes = new HashSet<>();
        for (ValidationErrorCode anEnum : ValidationErrorCode.values()) {
            int errorCode = anEnum.getErrorCode();
            // errorCode that we see must not have been seen so far.
            Assert.assertFalse("ValidationErrorCode: error code [" + errorCode + "] used multiple times!", errorCodes.contains(errorCode));
            errorCodes.add(errorCode);
        }
    }
}