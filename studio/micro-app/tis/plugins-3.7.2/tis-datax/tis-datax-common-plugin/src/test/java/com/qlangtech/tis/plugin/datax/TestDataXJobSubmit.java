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

import com.qlangtech.tis.TIS;
import com.qlangtech.tis.datax.DataXJobSubmit;
import com.qlangtech.tis.extension.ExtensionList;
import junit.framework.TestCase;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-04-27 21:48
 **/
public class TestDataXJobSubmit extends TestCase {
    public void testTwoInstanceGetter() {
        ExtensionList<DataXJobSubmit> extensionList = TIS.get().getExtensionList(DataXJobSubmit.class);
        assertNotNull(extensionList);
        assertEquals(2, extensionList.size());
    }
}
