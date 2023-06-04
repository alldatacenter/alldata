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

package org.apache.inlong.manager.common.tool.excel.annotation;

import org.apache.inlong.manager.common.tool.excel.ExcelCellDataTransfer;
import org.apache.inlong.manager.common.tool.excel.validator.ExcelCellValidator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for Excel field
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ExcelField {

    /**
     * Name of the field in Excel
     */
    String name();

    /**
     * Data transfer method from Excel to Object
     */
    ExcelCellDataTransfer x2oTransfer() default ExcelCellDataTransfer.NONE;
    /**
     * Validator for the field
     */
    Class<? extends ExcelCellValidator> validator() default ExcelCellValidator.class;

    Font font() default @Font;

    Style style() default @Style;

    Font headerFont() default @Font;

    Style headerStyle() default @Style;
}
