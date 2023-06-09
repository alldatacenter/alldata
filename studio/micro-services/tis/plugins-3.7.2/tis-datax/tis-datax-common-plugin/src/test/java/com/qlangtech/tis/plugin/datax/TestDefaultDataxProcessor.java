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

package com.qlangtech.tis.plugin.datax;

import com.google.common.collect.Lists;
import com.qlangtech.tis.datax.TableAlias;
import com.qlangtech.tis.datax.TableAliasMapper;
import com.qlangtech.tis.extension.impl.XmlFile;
import com.qlangtech.tis.manage.IAppSource;
import com.qlangtech.tis.plugin.KeyedPluginStore;
import com.qlangtech.tis.plugin.StoreResourceType;
import com.qlangtech.tis.plugin.common.PluginDesc;
import com.qlangtech.tis.plugin.test.BasicTest;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.RandomStringUtils;

import java.io.IOException;
import java.util.List;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-05-03 23:15
 **/
public class TestDefaultDataxProcessor extends BasicTest {

    public void testDescGenerate() {

        PluginDesc.testDescGenerate(DefaultDataxProcessor.class, "default-datax-processor-descriptor.json");
    }

    public void testSaveProcess() {
        final String appName = "test" + RandomStringUtils.randomAlphanumeric(2);

        try {
            DefaultDataxProcessor dataxProcessor = new DefaultDataxProcessor();
            TableAlias tabAlias = new TableAlias();
            tabAlias.setFrom("customer_order_relation");
            tabAlias.setTo("customer_order_relation1");
            List<TableAlias> tableMaps = Lists.newArrayList(tabAlias);
            dataxProcessor.setTableMaps(tableMaps);

            dataxProcessor.globalCfg = "datax-global-config";
            dataxProcessor.dptId = "356";
            dataxProcessor.recept = "小明";

            IAppSource.save(null, appName, dataxProcessor);

            DefaultDataxProcessor loadDataxProcessor = IAppSource.load(appName);
            assertNotNull("loadDataxProcessor can not be null", loadDataxProcessor);
            assertEquals(dataxProcessor.globalCfg, loadDataxProcessor.globalCfg);
            assertEquals(dataxProcessor.dptId, loadDataxProcessor.dptId);
            assertEquals(dataxProcessor.recept, loadDataxProcessor.recept);

            TableAliasMapper tabAlias1 = loadDataxProcessor.getTabAlias(null);
            assertEquals(1, tabAlias1.size());

            tabAlias1.forEach((key, val) -> {
                assertEquals(tabAlias.getFrom(), key);

                assertEquals(tabAlias.getFrom(), val.getFrom());
                assertEquals(tabAlias.getTo(), val.getTo());
            });

//            for (Map.Entry<String, TableAlias> entry : tabAlias1.entrySet()) {
//
//            }
        } finally {
            try {
                KeyedPluginStore.AppKey appKey = new KeyedPluginStore.AppKey(null, StoreResourceType.parse(false), appName, IAppSource.class);
                XmlFile storeFile = appKey.getSotreFile();
                FileUtils.forceDelete(storeFile.getFile().getParentFile());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
