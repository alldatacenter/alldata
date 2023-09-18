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

import com.qlangtech.tis.manage.common.Option;
import com.qlangtech.tis.plugin.ds.ReflectSchemaFieldType;
import junit.framework.TestCase;

import java.util.List;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2021-02-06 13:38
 */
public class TestGroovyShellEvaluate extends TestCase {

    public void testEval() {
        List<Option> fieldTyps = GroovyShellEvaluate.eval("com.qlangtech.tis.plugin.ds.ReflectSchemaFieldType.all()");
        assertNotNull(fieldTyps);
        List<Option> allOpts = ReflectSchemaFieldType.all();
        assertEquals(allOpts.size(), fieldTyps.size());
        int index = 0;
        Option actualOpt = null;
        for (Option o : allOpts) {
            actualOpt = fieldTyps.get(index++);
            assertEquals("index:" + index, o.getName() + "_" + o.getValue()
                    , actualOpt.getName() + "_" + actualOpt.getValue());
        }
    }
}
