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

package com.qlangtech.tis.extension;

import com.alibaba.fastjson.JSONObject;
import com.qlangtech.tis.extension.util.GroovyShellEvaluate;
import com.qlangtech.tis.trigger.util.JsonUtil;
import com.qlangtech.tis.util.DescriptorsJSON;
import junit.framework.TestCase;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-06-11 09:52
 **/
public class TestDescriptor extends TestCase {




    public void testGetPluginFormPropertyTypes() {

        GroovyShellEvaluate.eval("com.qlangtech.tis.extension.DefaultPlugin.getColsDefaultVal()");

        DefaultPlugin dftPlugin = new DefaultPlugin();
       // DescriptorsJSON descJson = new DescriptorsJSON(dftPlugin.getDescriptor());

        JSONObject desc = DescriptorsJSON.desc(dftPlugin.getDescriptor());
        //descJson.getDescriptorsJSON();

        JsonUtil.assertJSONEqual(DefaultPlugin.class, "default-plugin-descriptor-turn-1.json"
                , desc, (m, e, a) -> {
                    assertEquals(m, e, a);
                });


        JsonUtil.assertJSONEqual(DefaultPlugin.class, "default-plugin-descriptor-turn-2.json"
                , desc, (m, e, a) -> {
                    assertEquals(m, e, a);
                });
    }
}
