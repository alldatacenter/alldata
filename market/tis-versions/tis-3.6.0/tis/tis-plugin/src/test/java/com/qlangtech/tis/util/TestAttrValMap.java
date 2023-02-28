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

package com.qlangtech.tis.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.impl.IOUtils;
import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.runtime.module.misc.IControlMsgHandler;
import com.qlangtech.tis.util.plugin.TestPluginImpl;
import junit.framework.TestCase;
import org.easymock.EasyMock;

import java.util.Optional;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-06-22 10:27
 **/
public class TestAttrValMap extends TestCase {
    public void testCreateDescribable() {

        IControlMsgHandler fieldErrorHandler = EasyMock.createMock("fieldErrorHandler", IControlMsgHandler.class);
        IPluginContext pluginContext = EasyMock.createMock("pluginContext", IPluginContext.class);

        JSONObject jsonObject = IOUtils.loadResourceFromClasspath(TestPluginImpl.class
                , "testPluginImpl-post-content.json", true, (input) -> {
                    return JSON.parseObject(org.apache.commons.io.IOUtils.toString(input, TisUTF8.get()));
                });

        EasyMock.replay(fieldErrorHandler, pluginContext);
        AttrValMap attrValMap = AttrValMap.parseDescribableMap( Optional.empty(), jsonObject);

        Descriptor.ParseDescribable describable = attrValMap.createDescribable(pluginContext);
        assertNotNull(describable);
        TestPluginImpl testPlugin = (TestPluginImpl) describable.getInstance();
        assertNotNull(testPlugin);
        // 没有设置值，所以值对象应该为空，不能为0
        assertTrue("testPlugin.connectionsPerHost must be null", testPlugin.connectionsPerHost == null);
        assertEquals(12, (int) testPlugin.maxPendingPerConnection);

        EasyMock.verify(fieldErrorHandler, pluginContext);
    }
}
