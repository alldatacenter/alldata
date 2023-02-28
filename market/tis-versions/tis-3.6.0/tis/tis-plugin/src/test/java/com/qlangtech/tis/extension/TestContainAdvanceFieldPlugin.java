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
import com.qlangtech.tis.common.utils.Assert;
import com.qlangtech.tis.extension.impl.PropertyType;
import com.qlangtech.tis.trigger.util.JsonUtil;
import com.qlangtech.tis.util.DescriptorsJSON;
import junit.framework.TestCase;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-06-09 10:19
 **/
public class TestContainAdvanceFieldPlugin extends TestCase {

    public void testGetDesc() {
        ContainAdvanceFieldPlugin plugin = new ContainAdvanceFieldPlugin();
        Descriptor<ContainAdvanceFieldPlugin> descriptor = plugin.getDescriptor();

        PropertyType propVal = (PropertyType) descriptor.getPropertyType("propVal");
        Assert.assertNotNull("propVal can not be null", propVal);

        Assert.assertTrue(propVal.formField.advance());

        JSONObject desc = DescriptorsJSON.desc(descriptor);
        Assert.assertNotNull(desc);

        System.out.println(JsonUtil.toString(desc));
        JSONObject descProps = desc.getJSONObject(ContainAdvanceFieldPlugin.class.getName());
        Boolean advance = descProps.getJSONArray("attrs").getJSONObject(1).getBoolean("advance");
        Assert.assertNotNull(advance);
        Assert.assertTrue("advance must be true", advance);

        Boolean containAdvance = descProps.getBoolean("containAdvance");
        Assert.assertNotNull(containAdvance);
        Assert.assertTrue(containAdvance);
    }
}
