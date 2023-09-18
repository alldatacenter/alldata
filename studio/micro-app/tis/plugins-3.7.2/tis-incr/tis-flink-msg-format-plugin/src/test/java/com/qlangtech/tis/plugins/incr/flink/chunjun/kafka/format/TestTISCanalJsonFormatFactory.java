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

package com.qlangtech.tis.plugins.incr.flink.chunjun.kafka.format;

import com.alibaba.fastjson.JSONObject;
import com.qlangtech.tis.trigger.util.JsonUtil;
import com.qlangtech.tis.util.DescriptorsJSON;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-04-15 13:53
 **/
public class TestTISCanalJsonFormatFactory {

    @Test
    public void testDescriptorsJSONGenerate() {
        TISCanalJsonFormatFactory canalJsonFormatFactory = new TISCanalJsonFormatFactory();
        DescriptorsJSON descJson = new DescriptorsJSON(canalJsonFormatFactory.getDescriptor());
        JSONObject descriptorsJSON = descJson.getDescriptorsJSON();
        JsonUtil.assertJSONEqual(TISCanalJsonFormatFactory.class, "canal-json-format-factory.json"
                , descriptorsJSON, (m, e, a) -> {
                    Assert.assertEquals(m, e, a);
                });
    }

}
