/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.qlangtech.tis.plugin.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface FormField {
    /**
     * 替换IdentityName接口的功能，说明一个插件中有一个field的值能代表该插件的唯一ID，不过一个插件中只能有一个field标记为identity=true
     */
    boolean identity() default false;

    //  String dftVal() default StringUtils.EMPTY;

    // 表单中的顺序
    int ordinal() default 0;

    //

    /**
     * 插件中有些参数是比较少设置的属于高级设置，不用的时候可以选择隐藏
     *
     * @return
     */
    boolean advance() default false;

    Validator[] validate() default {};

    FormFieldType type() default FormFieldType.INPUTTEXT;


}
