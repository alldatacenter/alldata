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

import java.io.Serializable;
import java.util.List;

/**
 * Interface for validating Excel cell values
 */
public interface ExcelCellValidator<T> extends Serializable {

    /**
     * Returns the data validation constraint for the cell
     */
    List<String> constraint();

    /**
     * Validates the cell value
     * @param value the cell value to validate
     * @return true if the value is valid, false otherwise
     */
    boolean validate(T value);

    /**
     * Returns the error message to display if the cell value is invalid
     */
    String getInvalidTip();
}
