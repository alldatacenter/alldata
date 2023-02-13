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

package com.qlangtech.tis.async.message.client.consumer.impl;

import com.qlangtech.tis.TIS;
import com.qlangtech.tis.async.message.client.consumer.IMQListener;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.plugin.datax.IncrSelectedTabExtend;
import com.qlangtech.tis.plugin.incr.IIncrSelectedTabExtendFactory;

import java.util.Optional;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-07-31 09:22
 **/
public class TestMQListenerFactory extends MQListenerFactory {
    @Override
    public IMQListener create() {
        return null;
    }

    @TISExtension
    public static class DefaultDesc extends BaseDescriptor implements IIncrSelectedTabExtendFactory {
        @Override
        public EndType getEndType() {
            return EndType.MySQL;
        }

        @Override
        public PluginVender getVender() {
            return PluginVender.TIS;
        }

        @Override
        public Descriptor<IncrSelectedTabExtend> getSelectedTableExtendDescriptor() {
            return TIS.get().getDescriptor(TestIncrSourceSelectedTabExtend.class);
        }

        @Override
        public Optional<EndType> getTargetType() {
            return Optional.empty();
        }
    }
}
