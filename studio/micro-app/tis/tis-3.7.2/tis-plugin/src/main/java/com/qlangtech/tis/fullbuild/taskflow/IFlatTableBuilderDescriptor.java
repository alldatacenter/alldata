package com.qlangtech.tis.fullbuild.taskflow;

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

import com.google.common.collect.Lists;
import com.qlangtech.tis.plugin.IPluginTaggable;

import java.util.List;

/**
 * 标示是离线构建Descriptor
 *
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-01-24 18:25
 **/
public interface IFlatTableBuilderDescriptor extends IPluginTaggable {
    @Override
    default List<PluginTag> getTags() {
        return Lists.newArrayList(PluginTag.OfflineParser);
    }
}
