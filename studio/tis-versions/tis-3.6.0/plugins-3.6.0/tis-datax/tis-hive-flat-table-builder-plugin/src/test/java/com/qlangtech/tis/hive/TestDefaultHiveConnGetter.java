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

package com.qlangtech.tis.hive;

import com.qlangtech.tis.config.hive.meta.HiveTable;
import com.qlangtech.tis.config.hive.meta.IHiveMetaStore;
import com.qlangtech.tis.plugin.common.PluginDesc;
import junit.framework.TestCase;
import org.junit.Assert;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-07-09 16:01
 **/
public class TestDefaultHiveConnGetter extends TestCase {
    public void testDescGenerate() {

        PluginDesc.testDescGenerate(DefaultHiveConnGetter.class, "DefaultHiveConnGetter_desc.json");
    }


    public void testTableGetter() throws Exception {
        DefaultHiveConnGetter connGetter = new DefaultHiveConnGetter();
        connGetter.dbName = "default";
        connGetter.metaStoreUrls = "thrift://192.168.28.201:9083";

        try (IHiveMetaStore metaStore = connGetter.createMetaStoreClient()) {
            HiveTable table = metaStore.getTable(connGetter.dbName, "instancedetail");
            Assert.assertNotNull(table);
        }

    }
}
