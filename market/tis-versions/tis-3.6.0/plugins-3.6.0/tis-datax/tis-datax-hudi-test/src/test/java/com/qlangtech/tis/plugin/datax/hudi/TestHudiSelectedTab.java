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

package com.qlangtech.tis.plugin.datax.hudi;

import com.alibaba.citrus.turbine.Context;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.IPropertyType;
import com.qlangtech.tis.extension.impl.IOUtils;
import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.runtime.module.misc.IControlMsgHandler;
import com.qlangtech.tis.runtime.module.misc.impl.DefaultFieldErrorHandler;
import com.qlangtech.tis.trigger.util.JsonUtil;
import com.qlangtech.tis.util.AttrValMap;
import com.qlangtech.tis.util.DescriptorsJSON;
import com.qlangtech.tis.util.IPluginContext;
import com.qlangtech.tis.util.UploadPluginMeta;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

import java.util.Optional;
import java.util.Stack;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-03-14 11:46
 **/
public class TestHudiSelectedTab {


    @Test
    public void testDescGenerate() {
        HudiSelectedTab hudiTab = new HudiSelectedTab();
        DescriptorsJSON descJson = new DescriptorsJSON(hudiTab.getDescriptor());

        JsonUtil.assertJSONEqual(HudiSelectedTab.class, "hudi-selected-tab-descriptor.json"
                , descJson.getDescriptorsJSON(), (m, e, a) -> {
                    Assert.assertEquals(m, e, a);
                });
    }

    /**
     * recordField 中选择了 base_id 但是在 col中没有选择该列 <br/>
     * ，需要在com.qlangtech.tis.plugin.datax.hudi.HudiSelectedTab.validateAll()中进行校验过程中检验出来
     */
    @Test
    public void testValidateForm() {
        IControlMsgHandler fieldErrorHandler = EasyMock.createMock("fieldErrorHandler", IControlMsgHandler.class);
        IPluginContext pluginContext = EasyMock.createMock("pluginContext", IPluginContext.class);
        Context context = EasyMock.createMock("context", Context.class);

        fieldErrorHandler.addFieldError(context, "cols", "需要选择recordField");

        EasyMock.expect(context.get(DefaultFieldErrorHandler.KEY_VALIDATE_PLUGIN_INDEX))
                .andReturn(new Integer(0)).anyTimes();
        EasyMock.expect(context.get(DefaultFieldErrorHandler.KEY_VALIDATE_ITEM_INDEX))
                .andReturn(new Integer(0)).anyTimes();
        EasyMock.expect(context.get(DefaultFieldErrorHandler.KEY_VALIDATE_FIELDS_STACK))
                .andReturn(new Stack<DefaultFieldErrorHandler.FieldIndex>()).anyTimes();


        JSONObject jsonObject = IOUtils.loadResourceFromClasspath(HudiSelectedTab.class
                , "HudiSelectedTab-post-content.json", true, (input) -> {
                    return JSON.parseObject(org.apache.commons.io.IOUtils.toString(input, TisUTF8.get()));
                });


        UploadPluginMeta pmeta = UploadPluginMeta.parse(
                "dataxReader:require,targetDescriptorName_MySQL,targetDescriptorImpl_DataxMySQLReader,subFormFieldName_selectedTabs,dataxName_hudi");

        Optional<IPropertyType.SubFormFilter> subFormFilter = pmeta.getSubFormFilter();
        Assert.assertTrue(subFormFilter.isPresent());

        EasyMock.replay(fieldErrorHandler, pluginContext, context);
        AttrValMap attrValMap = AttrValMap.parseDescribableMap(subFormFilter, jsonObject);

        Descriptor.PluginValidateResult validate = attrValMap.validate(fieldErrorHandler, context, false);

        Assert.assertFalse("must contain error in form ", validate.isValid());

//        Descriptor.ParseDescribable describable = attrValMap.createDescribable(pluginContext);
//        Assert.assertNotNull(describable);
//        HudiSelectedTab testPlugin = (HudiSelectedTab) describable.instance;
//        Assert.assertNotNull(testPlugin);
        // 没有设置值，所以值对象应该为空，不能为0
//        Assert.assertTrue("testPlugin.connectionsPerHost must be null", testPlugin.connectionsPerHost == null);
//        Assert.assertEquals(12, (int) testPlugin.maxPendingPerConnection);

        EasyMock.verify(fieldErrorHandler, pluginContext, context);
    }
}
