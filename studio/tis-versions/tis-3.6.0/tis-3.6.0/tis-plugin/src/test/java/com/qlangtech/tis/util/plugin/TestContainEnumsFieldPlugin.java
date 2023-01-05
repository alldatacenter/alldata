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

package com.qlangtech.tis.util.plugin;

import com.qlangtech.tis.trigger.util.JsonUtil;
import com.qlangtech.tis.util.DescriptorsJSON;
import com.qlangtech.tis.util.plugin.impl.EnumProp2;
import junit.framework.TestCase;

/**
 * plugin 中包括 Describle 类型的枚举，运行时需要对其进行过滤
 *
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-09-16 08:27
 * @see // PluginExtraProps.KEY_ENUM_FILTER
 **/
public class TestContainEnumsFieldPlugin extends TestCase {


    public void testEnumsFields() {
        ContainEnumsFieldPlugin enumsFieldContainPlugin = new ContainEnumsFieldPlugin();
        DescriptorsJSON descJson = new DescriptorsJSON(enumsFieldContainPlugin.getDescriptor());

        JsonUtil.assertJSONEqual(ContainEnumsFieldPlugin.class, "enums-field-contain-descriptor-enum-prop1.json"
                , descJson.getDescriptorsJSON(), (m, e, a) -> {
                    assertEquals(m, e, a);
                });

        ContainEnumsFieldPlugin.acceptEnumProp = EnumProp2.KEY;
        JsonUtil.assertJSONEqual(ContainEnumsFieldPlugin.class, "enums-field-contain-descriptor-enum-prop2.json"
                , descJson.getDescriptorsJSON(), (m, e, a) -> {
                    assertEquals(m, e, a);
                });
    }
}
