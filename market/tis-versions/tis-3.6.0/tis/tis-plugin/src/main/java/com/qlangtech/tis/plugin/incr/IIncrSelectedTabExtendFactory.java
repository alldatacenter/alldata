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

package com.qlangtech.tis.plugin.incr;

import com.qlangtech.tis.async.message.client.consumer.impl.MQListenerFactory;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.plugin.datax.IncrSelectedTabExtend;

/**
 * 增量Source 或 Sink对表属性扩展
 *
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-08-10 17:12
 * @see MQListenerFactory
 * @see TISSinkFactory
 **/
public interface IIncrSelectedTabExtendFactory {

    String KEY_EXTEND_SELECTED_TAB_PROP = "extendSelectedTabProp";

    public Descriptor<IncrSelectedTabExtend> getSelectedTableExtendDescriptor();
}
