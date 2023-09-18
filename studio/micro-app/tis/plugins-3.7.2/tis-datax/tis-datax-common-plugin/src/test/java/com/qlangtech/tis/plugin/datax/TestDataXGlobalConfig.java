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

package com.qlangtech.tis.plugin.datax;

import com.alibaba.fastjson.JSON;
import com.qlangtech.tis.config.ParamsConfig;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.impl.IOUtils;
import com.qlangtech.tis.util.DescriptorsJSON;
import junit.framework.TestCase;

import java.util.Collections;
import java.util.List;

/**
 * @author: baisui 百岁
 * @create: 2021-04-21 10:37
 **/
public class TestDataXGlobalConfig extends TestCase {

    public void testDescriptJSONGenerate() {

        DataXGlobalConfig globalConfig = new DataXGlobalConfig();
        Descriptor<ParamsConfig> descriptor = globalConfig.getDescriptor();
        assertNotNull(descriptor);

        List<Descriptor<ParamsConfig>> singleton = Collections.singletonList(descriptor);
        DescriptorsJSON descriptorsJSON = new DescriptorsJSON(singleton);

        JSON.parseObject(IOUtils.loadResourceFromClasspath(TestDataXGlobalConfig.class, "dataXGlobalConfig-descriptor-assert.json"));

       // System.out.println(descriptorsJSON.getDescriptorsJSON().toJSONString());

        assertEquals(JSON.parseObject(IOUtils.loadResourceFromClasspath(TestDataXGlobalConfig.class, "dataXGlobalConfig-descriptor-assert.json")).toJSONString()
                , descriptorsJSON.getDescriptorsJSON().toJSONString());

    }
}
