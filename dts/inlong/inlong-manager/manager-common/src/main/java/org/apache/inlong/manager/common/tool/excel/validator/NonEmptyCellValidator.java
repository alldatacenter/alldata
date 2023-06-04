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

package org.apache.inlong.manager.common.tool.excel.validator;

import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;

/**
 * This class is used to validate the field name in the Excel file.
 */
public class NonEmptyCellValidator implements ExcelCellValidator<String> {

    public NonEmptyCellValidator() {
        // do nothing
    }

    /**
     * Get the constraint of the validator.
     */
    @Override
    public List<String> constraint() {
        return Collections.emptyList();
    }

    @Override
    public boolean validate(String value) {
        return StringUtils.isNotBlank(value);
    }

    @Override
    public String getInvalidTip() {
        return "Value can not be empty!";
    }
}
