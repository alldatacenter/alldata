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

package com.qlangtech.tis.plugin.solr.schema;

import com.qlangtech.tis.TIS;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.trigger.util.JsonUtil;
import com.qlangtech.tis.util.DescriptorsJSON;
import junit.framework.TestCase;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.util.List;

/**
 * @author: baisui 百岁
 * @create: 2021-02-06 10:41
 **/
public class TestJSONFieldTypeFactory extends TestCase {
    /**
     * 表单生成
     */
    public void testFormCreate() throws Exception {
        List<Descriptor<FieldTypeFactory>> descriptors = TIS.get().getDescriptorList(FieldTypeFactory.class);
        assertNotNull(descriptors);
        assertEquals(1, descriptors.size());
        DescriptorsJSON desc2Json = new DescriptorsJSON(descriptors);
        String assertFile = "fieldTypeFactory-descriptor-assert.json";
        try (InputStream reader = this.getClass().getResourceAsStream(assertFile)) {
            assertNotNull(reader);
            // FileUtils.write(new File(assertFile), JsonUtil.toString(desc2Json.getDescriptorsJSON()), TisUTF8.get(), false);
            assertEquals(IOUtils.toString(reader, TisUTF8.get()), JsonUtil.toString(desc2Json.getDescriptorsJSON()));
        }
    }
}
