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

package com.qlangtech.tis.plugins.incr.flink.chunjun.sink;

import com.qlangtech.tis.extension.impl.StubSuFormGetterContext;
import com.qlangtech.tis.plugin.common.PluginDesc;
import com.qlangtech.tis.test.TISEasyMock;
import org.junit.Test;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-10-19 11:08
 **/
public class TestUniqueKeySetter implements TISEasyMock {

    @Test
    public void testDescriptorGenerate() {
        StubSuFormGetterContext suFormGetterContext = new StubSuFormGetterContext(this);
        this.replay();
        suFormGetterContext.setSuFormGetterContext();
        PluginDesc.testDescGenerate(UniqueKeySetter.class, "unique-key-setter-descriptor.json");
        this.verifyAll();
    }
}
