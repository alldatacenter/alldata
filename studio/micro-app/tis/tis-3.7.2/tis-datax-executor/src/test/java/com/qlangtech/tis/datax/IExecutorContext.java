/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qlangtech.tis.datax;

import com.qlangtech.tis.plugin.StoreResourceType;
import com.qlangtech.tis.plugin.ds.DBIdentity;

import java.util.Collections;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-05-08 10:39
 **/
public interface IExecutorContext {
    String dataXName = "baisuitestTestcase";
    DataXJobInfo jobName = DataXJobInfo.create("customer_order_relation_1.json", DBIdentity.parseId(""), Collections.emptyList());

    StoreResourceType resType = StoreResourceType.DataApp;
}
