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

package com.qlangtech.tis.extension.util;

import com.google.common.collect.Lists;
import com.qlangtech.tis.manage.common.Option;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-06-08 13:22
 **/
public class OverwriteProps {

    public static List<Option> ENUM_BOOLEAN
            = Lists.newArrayList(new Option("是", true), new Option("否", false));

    public static OverwriteProps createBooleanEnums() {
        OverwriteProps opts = new OverwriteProps();
        opts.opts = Optional.of(ENUM_BOOLEAN);
        opts.dftValConvert = (val) -> {
            if (val instanceof String) {
                return Boolean.valueOf((String) val);
            }
            return val;
        };
        return opts;
    }

    public Object processDftVal(Object dftVal) {
        return dftVal != null ? dftValConvert.apply(dftVal) : (this.dftVal != null ? this.dftVal : null);
    }

    public Optional<String> appendHelper = Optional.empty();
    private Object dftVal;
    public Optional<List<Option>> opts = Optional.empty();
    public Function<Object, Object> dftValConvert = (val) -> val;

    public OverwriteProps setAppendHelper(String appendHelper) {
        this.appendHelper = Optional.of(appendHelper);
        return this;
    }

    public OverwriteProps setDftVal(Object dftVal) {
        this.dftVal = dftVal;
        return this;
    }
}
