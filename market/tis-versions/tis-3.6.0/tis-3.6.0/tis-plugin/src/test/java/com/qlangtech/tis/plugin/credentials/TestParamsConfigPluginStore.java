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

package com.qlangtech.tis.plugin.credentials;

import com.google.common.collect.Lists;
import com.qlangtech.tis.config.ParamsConfig;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.manage.common.CenterResource;
import com.qlangtech.tis.manage.common.HttpUtils;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.Validator;
import com.qlangtech.tis.util.PluginMeta;
import com.qlangtech.tis.util.UploadPluginMeta;
import junit.framework.TestCase;

import java.util.List;
import java.util.Optional;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-12-08 11:09
 **/
public class TestParamsConfigPluginStore extends TestCase {

    @Override
    protected void setUp() throws Exception {
        CenterResource.setNotFetchFromCenterRepository();
        HttpUtils.addMockGlobalParametersConfig();
    }

    private static final String keyTest1 = "test1";

    public void testWriteAndGet() {

        UploadPluginMeta pluginMeta = UploadPluginMeta.parse(
                ParamsConfig.CONTEXT_PARAMS_CFG + ":" + UploadPluginMeta.KEY_TARGET_PLUGIN_DESC + "_" + keyTest1);
        UploadPluginMeta.TargetDesc targetDesc = pluginMeta.getTargetDesc();

        assertEquals(keyTest1, targetDesc.matchTargetPluginDescName);
        ParamsConfigPluginStore paramsCfgPluginStore = new ParamsConfigPluginStore(pluginMeta);

        List<Descriptor.ParseDescribable<ParamsConfig>> dlist = Lists.newArrayList();
        Test1ParamsConfig cfg1 = new Test1ParamsConfig();
        cfg1.name = "id1";
        Descriptor.ParseDescribable<ParamsConfig> pluginDesc1 = new Descriptor.ParseDescribable<>(cfg1);
        pluginDesc1.extraPluginMetas.add(new PluginMeta("test1meta", "1.0.0", Optional.empty()));
        dlist.add(pluginDesc1);

        Test2ParamsConfig cfg2 = new Test2ParamsConfig();
        cfg2.name = "id1";
        Descriptor.ParseDescribable<ParamsConfig> pluginDesc2 = new Descriptor.ParseDescribable<>(cfg2);
        pluginDesc2.extraPluginMetas.add(new PluginMeta("test2meta", "1.0.0", Optional.empty()));
        dlist.add(pluginDesc2);

        paramsCfgPluginStore.setPlugins(null, Optional.empty(), dlist);

        List<ParamsConfig> plugins = paramsCfgPluginStore.getPlugins();
        assertNotNull("plugins can not be null", plugins);
        assertEquals(1, plugins.size());
        assertEquals(cfg1.name, plugins.get(0).identityValue());

        paramsCfgPluginStore = new ParamsConfigPluginStore(UploadPluginMeta.parse(ParamsConfig.CONTEXT_PARAMS_CFG));
        plugins = paramsCfgPluginStore.getPlugins();
        assertNotNull("plugins can not be null", plugins);
        assertTrue(plugins.size() > 1);


        cfg2 = new Test2ParamsConfig();
        cfg2.name = "id1";
        pluginDesc2 = new Descriptor.ParseDescribable<>(cfg2);
        pluginDesc2.extraPluginMetas.add(new PluginMeta("test2meta", "1.0.0", Optional.empty()));
        dlist.add(pluginDesc2);

        // 保存应该是要出错的，因为cfg2存在Id重复的问题
        paramsCfgPluginStore.setPlugins(null, Optional.empty(), dlist);
    }

    public static class Test1ParamsConfig extends ParamsConfig {

        @FormField(identity = true, ordinal = 0, validate = {Validator.require, Validator.identity})
        public String name;

        @Override
        public <INSTANCE> INSTANCE createConfigInstance() {
            return null;
        }

        @Override
        public String identityValue() {
            return this.name;
        }

        @TISExtension()
        public static class DefaultDescriptor extends Descriptor<ParamsConfig> {
            @Override
            public String getDisplayName() {
                return keyTest1;
            }
        }
    }

    public static class Test2ParamsConfig extends ParamsConfig {
        @FormField(identity = true, ordinal = 0, validate = {Validator.require, Validator.identity})
        public String name;

        @Override
        public <INSTANCE> INSTANCE createConfigInstance() {
            return null;
        }

        @Override
        public String identityValue() {
            return this.name;
        }

        @TISExtension()
        public static class DefaultDescriptor extends Descriptor<ParamsConfig> {
            @Override
            public String getDisplayName() {
                return "test2";
            }
        }
    }
}
