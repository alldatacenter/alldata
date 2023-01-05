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

package com.qlangtech.tis.util;

import com.qlangtech.tis.common.utils.Assert;
import junit.framework.TestCase;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-07-05 15:51
 **/
public class TestPluginMeta extends TestCase {
    public void testParse() {
        String plugin1Name = "test-plugin";
        String plugin2Name = "test2-plugin";
        String metaAttribute = plugin1Name + "@3.1.2!hdfs_2.1.3," + plugin2Name + "@3.2.2!hdfs_3.1.3";
        Map<String, PluginMeta> metas
                = PluginMeta.parse(metaAttribute).stream().collect(Collectors.toMap((m) -> m.getPluginName(), (m) -> m));
        assertEquals(2, metas.size());

        PluginMeta pluginMeta = metas.get(plugin1Name);
        assertNotNull(pluginMeta);

        Assert.assertEquals(plugin1Name, pluginMeta.getPluginName());
        Assert.assertEquals("3.1.2", String.valueOf(pluginMeta.ver));
        Assert.assertTrue(pluginMeta.classifier.isPresent());
        Assert.assertEquals("hdfs_2.1.3", pluginMeta.classifier.get().getClassifier());


        pluginMeta = metas.get(plugin2Name);
        assertNotNull(pluginMeta);

        Assert.assertEquals(plugin2Name, pluginMeta.getPluginName());
        Assert.assertEquals("3.2.2", String.valueOf(pluginMeta.ver));
        Assert.assertTrue(pluginMeta.classifier.isPresent());
        Assert.assertEquals("hdfs_3.1.3", pluginMeta.classifier.get().getClassifier());
        //=======================================================

        metaAttribute = plugin1Name + "@3.1.2!hdfs_2.1.3," + plugin2Name + "@3.2.2";
        metas
                = PluginMeta.parse(metaAttribute).stream().collect(Collectors.toMap((m) -> m.getPluginName(), (m) -> m));
        assertEquals(2, metas.size());
        pluginMeta = metas.get(plugin2Name);
        assertNotNull(pluginMeta);

        Assert.assertEquals(plugin2Name, pluginMeta.getPluginName());
        Assert.assertEquals("3.2.2", String.valueOf(pluginMeta.ver));
        Assert.assertFalse(pluginMeta.classifier.isPresent());
        // Assert.assertEquals("hdfs_3.1.3", pluginMeta.classifier.get().getClassifier());
        long lastModify = 1656920168722l;
        String flinkCdc = "tis-flink-cdc-mysql-plugin";
        metaAttribute = flinkCdc + "@3.5.0@" + lastModify;
        metas
                = PluginMeta.parse(metaAttribute).stream().collect(Collectors.toMap((m) -> m.getPluginName(), (m) -> m));
        assertEquals(1, metas.size());

        pluginMeta = metas.get(flinkCdc);
        Assert.assertNotNull(pluginMeta);
        Assert.assertEquals("3.5.0", String.valueOf(pluginMeta.ver));
        Assert.assertFalse(pluginMeta.isLastModifyTimeStampNull());


        Assert.assertEquals(lastModify, pluginMeta.getLastModifyTimeStamp());
    }
}
