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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.qlangtech.tis.common.utils.Assert;
import com.qlangtech.tis.extension.DefaultPlugin;
import com.qlangtech.tis.trigger.util.JsonUtil;
import junit.framework.TestCase;

import java.util.Map;
import java.util.Optional;

/**
 *
 */
public class TestPluginExtraProps extends TestCase {
    public void testLode() throws Exception {
        Optional<PluginExtraProps> ep = PluginExtraProps.load(TestPluginExtraProps.class);
        assertNotNull(ep);
        assertTrue(ep.isPresent());
        PluginExtraProps extraProps = ep.get();
        PluginExtraProps.Props prop = extraProps.getProp("dbName");
        assertNotNull(prop);
        assertNotNull("数据库名", prop.getLable());

        prop = extraProps.getProp("userName");
        assertNotNull(prop);
        assertNotNull("用户名", prop.getLable());
        assertTrue("isAsynHelp must be true", prop.isAsynHelp());


        PluginExtraProps.Props encode = extraProps.getProp("encode");
        JSONObject props = encode.getProps();
        JSONObject creator = props.getJSONObject("creator");
        assertNotNull(creator);
        assertEquals("部门管理", creator.getString("label"));
        assertEquals("/base/departmentlist", creator.getString("routerLink"));
    }

//    public void testCreatorWithError() throws Exception {
//
//        try {
//            Optional<PluginExtraProps> ep = PluginExtraProps.load(WithCreatorError.class);
//            fail("must have faild");
//        } catch (Exception e) {
//            assertEquals("propKey:dbName,package:com.qlangtech.tis.extension.util,propKey:WithCreatorError.json", e.getMessage());
//        }
//    }

    public void testCreatorWithMerge() throws Exception {
        Optional<PluginExtraProps> ep = PluginExtraProps.load(WithCreatorErrorOk.class);
        assertTrue(ep.isPresent());
        PluginExtraProps extraProps = ep.get();
        for (Map.Entry<String, PluginExtraProps.Props> e : extraProps.entrySet()) {
            System.out.println("key:" + e.getKey());
            System.out.println("value:" + JsonUtil.toString(e.getValue()));
            System.out.println("==============================================");
        }

        PluginExtraProps.Props dbName = extraProps.getProp("dbName");
        assertNotNull(dbName);

        JSONObject props = dbName.getProps();
        JSONObject creator = props.getJSONObject(PluginExtraProps.KEY_CREATOR);
        assertNotNull(creator);

        assertEquals("/base/departmentlist", creator.getString(PluginExtraProps.KEY_ROUTER_LINK));
        assertEquals("部门管理", creator.getString(PluginExtraProps.KEY_LABEL));

        JSONArray plugins = creator.getJSONArray("plugin");
        assertEquals(1, plugins.size());
        JSONObject pmeta = plugins.getJSONObject(0);
        assertNotNull(pmeta);

        JsonUtil.assertJSONEqual(TestPluginExtraProps.class, "pluginMeta.json", creator, (m, e, a) -> {
            assertEquals(m, e, a);
        });

//        {
//            "hetero": "params-cfg",
//                "descName": "DataX-global",
//                "extraParam": "append_true"
//        }

    }

    public void testAddFieldDescriptor() {

        DefaultPlugin plugin = new DefaultPlugin();

        Optional<PluginExtraProps> extraProps
                = PluginExtraProps.load(Optional.of(plugin.getDescriptor()), DefaultPlugin.class);

        Assert.assertTrue(extraProps.isPresent());

        PluginExtraProps ep = extraProps.get();
        PluginExtraProps.Props nameProp = ep.getProp("name");
        Assert.assertNotNull(nameProp);

        Assert.assertTrue("isAsynHelp must be true", nameProp.isAsynHelp());
        Assert.assertEquals(DefaultPlugin.FILED_NAME_DESCRIPTION, nameProp.getAsynHelp());
        Assert.assertEquals(DefaultPlugin.DFT_NAME_VALUE, nameProp.getDftVal());





    }

    public void testAddFieldDescriptorWithNotMatchFieldName() {
        try {
            DefaultPlugin plugin = new DefaultPlugin();
            DefaultPlugin.DefaultDescriptor desc = (DefaultPlugin.DefaultDescriptor) plugin.getDescriptor();
            desc.addFieldDescriptor("xxx", DefaultPlugin.DFT_NAME_VALUE, DefaultPlugin.FILED_NAME_DESCRIPTION);
            PluginExtraProps.load(Optional.of(desc), DefaultPlugin.class);
            Assert.fail("must be faild");
        } catch (Exception e) {
            Assert.assertEquals("prop key:xxx relevant prop must exist , exist props keys:name,cols", e.getMessage());
        }
    }
}
