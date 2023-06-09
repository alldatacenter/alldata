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

import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.IPropertyType;
import junit.framework.TestCase;
import org.easymock.EasyMock;

import java.util.List;
import java.util.Optional;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @create: 2020-07-20 13:21
 */
public class TestUploadPluginMeta extends TestCase {


    public void testUncacheAttribute() {
        String groovyScript = "com.qlangtech.tis.plugin.datax.DataXMongodbWriter.getDftColumn()";
        UploadPluginMeta parseResult = UploadPluginMeta.parse(groovyScript + ":uncache_true");
        assertTrue(parseResult.getBoolean(UploadPluginMeta.KEY_UNCACHE));
        assertEquals(groovyScript, parseResult.getName());
    }

    public void testDataXReaderWithExecId() {
        String rawContent = "dataxReader:require,dataxName_mysql_mysql,execId_fe4e79f1-9f05-6976-7604-db6d1c3ad391";
        UploadPluginMeta parseResult = UploadPluginMeta.parse(rawContent);
        assertNotNull(parseResult);
        assertTrue(parseResult.isRequired());
        assertEquals("dataxReader", parseResult.getName());
        assertEquals("mysql_mysql", parseResult.getExtraParam("dataxName"));
        assertEquals("fe4e79f1-9f05-6976-7604-db6d1c3ad391", parseResult.getExtraParam("execId"));
    }

    public void testPluginMetaParse() {
        //  String pluginName = "dsname_yuqing_zj2_bak";
        String pluginName = "test_plugin";
        String[] plugins = new String[]{pluginName + ":require"};
        List<UploadPluginMeta> pluginMetas = UploadPluginMeta.parse(plugins);
        assertEquals(1, pluginMetas.size());
        UploadPluginMeta meta = pluginMetas.get(0);
        assertNotNull(meta);
        assertEquals(pluginName, meta.getName());
        assertTrue(meta.isRequired());
        // ===============================================
        plugins = new String[]{pluginName};
        pluginMetas = UploadPluginMeta.parse(plugins);
        assertEquals(1, pluginMetas.size());
        meta = pluginMetas.get(0);
        assertNotNull(meta);
        assertEquals(pluginName, meta.getName());
        assertFalse(meta.isRequired());
        // ==============================================
        plugins = new String[]{pluginName + ":xxxx"};
        try {
            pluginMetas = UploadPluginMeta.parse(plugins);
            fail("shall be faild,but not");
        } catch (Exception e) {
        }
        String dsnameKey = "dsname";
        final String dbName = "yuqing_zj2_bak";
        plugins = new String[]{pluginName + ":" + dsnameKey + "_" + dbName + ",require"};

        pluginMetas = UploadPluginMeta.parse(plugins);

        assertEquals(1, pluginMetas.size());
        meta = pluginMetas.get(0);
        assertNotNull(meta);
        assertEquals(pluginName, meta.getName());
        assertTrue(meta.isRequired());
        assertEquals(dbName, meta.getExtraParam(dsnameKey));


        //=======================================================================
        final String targetDescriptor = "MySQLDataxReader";
        final String targetDescriptorImpl = "com.qlangtech.tis.plugin.datax.DataxMySQLReader";
        final String subFieldName = "subFieldName";

        // dataxReader:require,targetDescriptorImpl_com.qlangtech.tis.plugin.datax.DataxMySQLReader,targetDescriptorName_MySQL,subFormFieldName_selectedTabs,dataxName_hudi,maxReaderTableCount_9999

        plugins = new String[]{pluginName + ":" + UploadPluginMeta.PLUGIN_META_TARGET_DESCRIPTOR_NAME
                + "_" + targetDescriptor
                + "," + UploadPluginMeta.PLUGIN_META_TARGET_DESCRIPTOR_IMPLEMENTION + "_" + targetDescriptorImpl
                + "," + IPropertyType.SubFormFilter.PLUGIN_META_SUB_FORM_FIELD + "_" + subFieldName + ",require"};

        pluginMetas = UploadPluginMeta.parse(plugins);

        assertEquals(1, pluginMetas.size());
        meta = pluginMetas.get(0);
        assertTrue(meta.isRequired());
        Optional<IPropertyType.SubFormFilter> subFormFilter = meta.getSubFormFilter();
        assertTrue(subFormFilter.isPresent());
        IPropertyType.SubFormFilter filter = subFormFilter.get();

        assertEquals(targetDescriptor, filter.targetDesc.descDisplayName);
        assertEquals(targetDescriptorImpl, filter.targetDesc.impl);
        assertEquals(subFieldName, filter.subFieldName);
        Descriptor descriptor = EasyMock.createMock("descriptor", Descriptor.class);
        EasyMock.expect(descriptor.getDisplayName()).andReturn(targetDescriptor);
        EasyMock.expect(descriptor.getDisplayName()).andReturn("dddd");

        EasyMock.replay(descriptor);
        assertTrue(filter.match(descriptor));
        assertFalse(filter.match(descriptor));
        EasyMock.verify(descriptor);

    }
}
