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

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.IndexedColors;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Style {

    IndexedColors bgColor() default IndexedColors.BLACK;

    IndexedColors allBorderColor() default IndexedColors.BLACK;

    BorderStyle allBorderStyle() default BorderStyle.NONE;

    IndexedColors bottomBorderColor() default IndexedColors.BLACK;

    IndexedColors topBorderColor() default IndexedColors.BLACK;

    IndexedColors leftBorderColor() default IndexedColors.BLACK;

    IndexedColors rightBorderColor() default IndexedColors.BLACK;

    BorderStyle bottomBorderStyle() default BorderStyle.NONE;

    BorderStyle topBorderStyle() default BorderStyle.NONE;

    BorderStyle leftBorderStyle() default BorderStyle.NONE;

    BorderStyle rightBorderStyle() default BorderStyle.NONE;

    int width() default 5000;
}
