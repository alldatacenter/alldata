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

package com.qlangtech.tis.plugin.common;

import com.qlangtech.tis.extension.Describable;
import com.qlangtech.tis.trigger.util.JsonUtil;
import com.qlangtech.tis.util.DescriptorsJSON;
import org.junit.Assert;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-06-24 09:24
 **/
public class PluginDesc {


    public static <TT extends Describable> void testDescGenerate(Class<TT> clazz, String assertFileName) {
        try {
            TT plugin = clazz.newInstance();
            // DescriptorsJSON descJson = new DescriptorsJSON(plugin.getDescriptor());
            JsonUtil.assertJSONEqual(clazz, assertFileName
                    , DescriptorsJSON.desc(plugin.getDescriptor()), (m, e, a) -> {
                        Assert.assertEquals(m, e, a);
                    });
            //return plugin;
        } catch (Exception e) {
            throw new RuntimeException(assertFileName, e);
        }
    }
}
