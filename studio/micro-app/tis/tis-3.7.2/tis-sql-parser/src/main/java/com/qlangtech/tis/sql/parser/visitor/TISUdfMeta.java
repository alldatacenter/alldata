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
package com.qlangtech.tis.sql.parser.visitor;

import java.lang.reflect.Method;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2019年6月2日
 */
public class TISUdfMeta {

    // 是否是聚合函数
    private final boolean aggregateFunc;

    // 数字类型函数计算，比如sum,avg之类的
    private final boolean numeric;

    private Method method;

    public TISUdfMeta(boolean aggregateFunc, Method method) {
        this(aggregateFunc, false, method);
    }

    public TISUdfMeta(boolean aggregateFunc, boolean numeric, Method method) {
        super();
        this.aggregateFunc = aggregateFunc;
        this.numeric = numeric;
        this.method = method;
    }

    public boolean isAggregateFunc() {
        return this.aggregateFunc;
    }

    public boolean isNumeric() {
        return this.numeric;
    }
}
