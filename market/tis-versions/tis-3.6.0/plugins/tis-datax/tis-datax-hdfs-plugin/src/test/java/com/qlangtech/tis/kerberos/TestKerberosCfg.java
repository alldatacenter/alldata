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

package com.qlangtech.tis.kerberos;

import com.alibaba.citrus.turbine.Context;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.qlangtech.tis.config.ParamsConfig;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.impl.IOUtils;
import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.plugin.IPluginStore;
import com.qlangtech.tis.plugin.SetPluginsResult;
import com.qlangtech.tis.runtime.module.misc.IControlMsgHandler;
import com.qlangtech.tis.runtime.module.misc.impl.DefaultFieldErrorHandler;
import com.qlangtech.tis.util.AttrValMap;
import com.qlangtech.tis.util.IPluginContext;
import com.qlangtech.tis.util.UploadPluginMeta;
import org.apache.commons.io.FileUtils;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Optional;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-06-02 10:29
 **/
public class TestKerberosCfg {

    @Rule
    public TemporaryFolder folder = TemporaryFolder.builder().build();

    static final String cfgContent = "dafasd23asdfasdfadf\nppppddddieeekkkd";

    @Test
    public void testJsonSubmit() throws Exception {

        final String cfgFileName = "xxxx.cfg";

        CompositContext fieldErrorHandler = EasyMock.createMock("fieldErrorHandler", CompositContext.class);
        // IControlMsgHandler controlMsgHandler = EasyMock.createMock("controlMsgHandler", IControlMsgHandler.class);
        Context context = EasyMock.createMock("context", Context.class);

        EasyMock.expect(context.get(DefaultFieldErrorHandler.KEY_VALIDATE_PLUGIN_INDEX)).andReturn(0).anyTimes();
        EasyMock.expect(context.get(DefaultFieldErrorHandler.KEY_VALIDATE_ITEM_INDEX)).andReturn(0).anyTimes();

        JSONObject jsonObject = IOUtils.loadResourceFromClasspath(TestKerberosCfg.class
                , "kerberos_form.json", true, (input) -> {
                    return JSON.parseObject(org.apache.commons.io.IOUtils.toString(input, TisUTF8.get()));
                });


        final File tmpFile = folder.newFile("00000008.tmp");
        FileUtils.write(tmpFile, cfgContent, Charset.defaultCharset(), false);
        jsonObject.getJSONObject("vals").getJSONObject("keytabPath")
                .put(Descriptor.KEY_primaryVal, tmpFile.getAbsolutePath() + ";" + cfgFileName);

        UploadPluginMeta pmeta = UploadPluginMeta.parse("params-cfg:require,append_true,targetItemDesc_kerberos");


        EasyMock.replay(fieldErrorHandler, context);
        AttrValMap attrValMap = AttrValMap.parseDescribableMap( //fieldErrorHandler,
                Optional.empty(), jsonObject);
        Assert.assertNotNull(attrValMap);

        Descriptor.PluginValidateResult validate = attrValMap.validate(fieldErrorHandler, context, false);
        Assert.assertNotNull(validate);

        Assert.assertTrue(validate.isValid());

        Assert.assertNotNull(attrValMap.descriptor);
        validate.setDescriptor(attrValMap.descriptor);

        KerberosCfg kerberosCfg = validate.newInstance(fieldErrorHandler);
        validateKerberosProps(kerberosCfg);

        IPluginStore<ParamsConfig> pluginStore
                = ParamsConfig.getTargetPluginStore(pmeta.getTargetDesc().matchTargetPluginDescName);
        FileUtils.deleteQuietly(pluginStore.getTargetFile().getFile());


        SetPluginsResult saveResult = pluginStore.setPlugins(fieldErrorHandler, Optional.of(context)
                , Collections.singletonList(new Descriptor.ParseDescribable<>(kerberosCfg)));

        Assert.assertNotNull(saveResult);
        Assert.assertTrue(saveResult.cfgChanged);
        Assert.assertTrue(saveResult.success);

        pluginStore.cleanPlugins();

        // 清空之后重新加载
        kerberosCfg = (KerberosCfg) pluginStore.getPlugin();
        validateKerberosProps(kerberosCfg);

        /**==================================
         * 开始更新测试
         ===================================*/
        JSONObject updateJson = IOUtils.loadResourceFromClasspath(TestKerberosCfg.class
                , "kerberos_form_update.json", true, (input) -> {
                    return JSON.parseObject(org.apache.commons.io.IOUtils.toString(input, TisUTF8.get()));
                });
        attrValMap = AttrValMap.parseDescribableMap(Optional.empty(), updateJson);
        Assert.assertNotNull(attrValMap);

        validate = attrValMap.validate(fieldErrorHandler, context, false);
        Assert.assertNotNull(validate);

        Assert.assertTrue(validate.isValid());
        validate.setDescriptor(attrValMap.descriptor);
        kerberosCfg = validate.newInstance(fieldErrorHandler);

        // validateKerberosProps(kerberosCfg);

        saveResult = pluginStore.setPlugins(fieldErrorHandler, Optional.of(context)
                , Collections.singletonList(new Descriptor.ParseDescribable<>(kerberosCfg)));

        Assert.assertNotNull(saveResult);
        Assert.assertFalse(saveResult.cfgChanged);
        Assert.assertTrue(saveResult.success);

        // pluginStore.cleanPlugins();
        kerberosCfg = (KerberosCfg) pluginStore.getPlugin();
        validateKerberosProps(kerberosCfg);

        pluginStore.cleanPlugins();
        kerberosCfg = (KerberosCfg) pluginStore.getPlugin();
        validateKerberosProps(kerberosCfg);

        EasyMock.verify(fieldErrorHandler, context);

    }

    protected void validateKerberosProps(KerberosCfg kerberosCfg) throws Exception {
        Assert.assertNotNull(kerberosCfg);

        Assert.assertEquals("principal@taobao", kerberosCfg.principal);
        Assert.assertEquals("xxxx.cfg", kerberosCfg.keytabPath);
        Assert.assertEquals("kerberos_name", kerberosCfg.name);

        File keyTabPath = kerberosCfg.getKeyTabPath();
        Assert.assertEquals(cfgContent, FileUtils.readFileToString(keyTabPath, TisUTF8.get()));
    }


    private interface CompositContext extends IPluginContext, IControlMsgHandler {

    }
}
