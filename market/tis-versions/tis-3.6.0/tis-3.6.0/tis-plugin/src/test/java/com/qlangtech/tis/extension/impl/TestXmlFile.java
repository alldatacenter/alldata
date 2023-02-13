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
package com.qlangtech.tis.extension.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.qlangtech.tis.plugin.ComponentMeta;
import com.qlangtech.tis.plugin.IRepositoryResource;
import com.qlangtech.tis.util.PluginMeta;
import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.List;
import java.util.Set;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @create: 2020-04-26 14:13
 */
public class TestXmlFile extends TestCase {

//    public void testArrayPropSave() throws Exception {
//        File testFile = new File("/tmp/test_file.xml");
//        XmlFile xmlFile = new XmlFile(testFile);
//        TestObject tstObject = new TestObject();
//        tstObject.setCols(Collections.singletonList("hello"));
//        xmlFile.write(tstObject, Collections.emptySet());
//    }

    /**
     * 测试序列化
     */
    public void testMashall() throws Exception {
        File testFile = new File("/tmp/test_file.xml");
        XmlFile xmlFile = new XmlFile(testFile);
        List<TestBean> plugins = Lists.newArrayList();
        plugins.add(new TestBean("baisui"));
        plugins.add(new TestBean("dabao"));
        plugins.add(new TestBean("xiaobao"));
        Set<PluginMeta> pluginsMeta = Sets.newHashSet();
        pluginsMeta.addAll(PluginMeta.parse("test1@1.1"));
        pluginsMeta.addAll(PluginMeta.parse("mock2@1.2"));
        xmlFile.write(plugins, pluginsMeta);
        List<IRepositoryResource> resources = Lists.newArrayList();
        resources.add(new FileRepositoryResource(testFile));
        ComponentMeta componentMeta = new ComponentMeta(resources);
        Set<PluginMeta> pluginMetaSet = componentMeta.loadPluginMeta();
        assertEquals(2, pluginMetaSet.size());
        for (PluginMeta pm : pluginsMeta) {
            assertTrue(pm.toString(), pluginMetaSet.contains(pm));
        }
        FileUtils.deleteQuietly(testFile);
    }

    private static class FileRepositoryResource implements IRepositoryResource {

        public long getWriteLastModifyTimeStamp() {
            return 0;
        }

        private final File f;

        public FileRepositoryResource(File f) {
            this.f = f;
        }

        @Override
        public void copyConfigFromRemote() {
            throw new UnsupportedOperationException();
        }

        @Override
        public XmlFile getTargetFile() {
            return new XmlFile(f);
        }
    }

    public static class TestBean {

        private final String name;

        public TestBean(String name) {
            this.name = name;
        }
    }
}
