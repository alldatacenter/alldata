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

import com.alibaba.citrus.turbine.Context;
import com.qlangtech.tis.extension.Describable;
import com.qlangtech.tis.extension.IPropertyType;
import com.qlangtech.tis.extension.impl.BaseSubFormProperties;
import com.qlangtech.tis.runtime.module.misc.IControlMsgHandler;
import com.qlangtech.tis.util.impl.AttrVals;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * plugin form can have an sub form ,where build form needs multi step
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2021-04-10 19:25
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface SubForm {
    String FIELD_DES_CLASS = ("desClazz");

    // get describe form bean class
    Class<? extends Describable> desClazz();

    /**
     * 至少选一个
     *
     * @return
     */
    boolean atLeastOne() default true;

    /**
     * id list fetch method name which owned to describable plugin instance
     *
     * @return
     */
    String idListGetScript();

    interface ISubFormItemValidate {
        public boolean validateSubFormItems(IControlMsgHandler msgHandler, Context context, BaseSubFormProperties props
                , IPropertyType.SubFormFilter subFormFilter, AttrVals formData);
    }
}
