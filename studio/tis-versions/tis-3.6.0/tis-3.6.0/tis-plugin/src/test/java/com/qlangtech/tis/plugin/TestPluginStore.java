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
package com.qlangtech.tis.plugin;

import com.google.common.collect.Lists;
import com.qlangtech.tis.TIS;
import com.qlangtech.tis.common.utils.Assert;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.UberClassLoader;
import com.qlangtech.tis.manage.common.CenterResource;
import com.qlangtech.tis.manage.common.Config;
import com.qlangtech.tis.manage.common.HttpUtils;
import com.qlangtech.tis.maven.plugins.tpi.PluginClassifier;
import com.qlangtech.tis.util.PluginMeta;
import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @create: 2020-05-03 09:04
 */
public class TestPluginStore extends TestCase {

    private static final String VALUE_PROP_1 = "prop1-1";

    private static final String VALUE_PROP_2 = "prop2-1";

    @Override
    protected void setUp() throws Exception {
        CenterResource.setNotFetchFromCenterRepository();
        HttpUtils.addMockGlobalParametersConfig();
    }



    public void testTableDumpFactory() {

        // assertFalse(TIS.initialized);
        PluginStore<TestPlugin> pstore = new PluginStore<>(TestPlugin.class);

        File storeFile = pstore.getTargetFile().getFile();
        FileUtils.deleteQuietly(storeFile);
        List<Descriptor.ParseDescribable<TestPlugin>> dlist = createTestPlugin();
        SetPluginsResult updateResult = pstore.setPlugins(null, Optional.empty(), dlist);

        Assert.assertTrue(updateResult.success);
        Assert.assertTrue("cfgChanged must have change", updateResult.cfgChanged);
        Assert.assertTrue(updateResult.lastModifyTimeStamp > 0);


        File targetFile = pstore.getTargetFile().getFile();
        Assert.assertTrue("getLastModifyTimeStampFile must be exist", pstore.getLastModifyTimeStampFile().exists());
        Assert.assertTrue(targetFile.exists());
        pstore.cleanPlugins();
        List<TestPlugin> plugins = pstore.getPlugins();
        assertEquals(1, plugins.size());
        TestPlugin plugin = pstore.getPlugin();
        assertNotNull(plugin);
        assertEquals(VALUE_PROP_1, plugin.prop1);
        assertEquals(VALUE_PROP_2, plugin.prop2);


        ComponentMeta componentMeta = new ComponentMeta(pstore);
        Set<PluginMeta> pluginMetas = componentMeta.loadPluginMeta();
        Assert.assertTrue("pluginMetas.size() > 0", pluginMetas.size() == 1);
        Optional<PluginMeta> f = pluginMetas.stream().findFirst();
        PluginMeta meta = f.get();
        assertEquals(testMeta.getPluginName(), meta.getPluginName());
        assertEquals(testMeta.ver, meta.ver);
        Optional<PluginClassifier> classifier = meta.classifier;
        Assert.assertTrue(classifier.isPresent());
        assertEquals(classifier_hdfs, classifier.get().getClassifier());
    }

    final static String classifier_hdfs = "hdfs_2.1.3";
    private static final PluginMeta testMeta = new PluginMeta("testmeta", "1.0.0", Optional.of( PluginClassifier.create(classifier_hdfs)));

    public static List<Descriptor.ParseDescribable<TestPlugin>> createTestPlugin() {
        TestPlugin p = new TestPlugin();
        p.prop1 = VALUE_PROP_1;
        p.prop2 = VALUE_PROP_2;
        List<Descriptor.ParseDescribable<TestPlugin>> dlist = Lists.newArrayList();
        Descriptor.ParseDescribable parseDescribable = new Descriptor.ParseDescribable(p);

        parseDescribable.extraPluginMetas.add(testMeta);
        dlist.add(parseDescribable);
        return dlist;
    }
}
