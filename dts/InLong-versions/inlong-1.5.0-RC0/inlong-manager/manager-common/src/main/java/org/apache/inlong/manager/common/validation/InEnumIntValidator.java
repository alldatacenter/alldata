/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.manager.common.validation;

import com.google.common.base.Joiner;
import org.apache.inlong.manager.common.enums.IntListValuable;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Collections;
import java.util.List;

/**
 * Check whether the incoming Integer type parameter is in the corresponding enum value
 */
public class InEnumIntValidator implements ConstraintValidator<InEnumInt, Integer> {

    private List<Integer> values;

    @Override
    public void initialize(InEnumInt annotation) {
        IntListValuable[] values = annotation.value().getEnumConstants();
        if (values.length == 0) {
            this.values = Collections.emptyList();
        } else {
            this.values = values[0].valueList();
        }
    }

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        if (value == null || values.contains(value)) {
            return true;
        }

        // disable default msg
        context.disableDefaultConstraintViolation();
        // Add the error message again
        context.buildConstraintViolationWithTemplate(
                context.getDefaultConstraintMessageTemplate()
                        .replace("{value}", Joiner.on(",").join(values)))
                .addConstraintViolation();
        return false;
    }

}
