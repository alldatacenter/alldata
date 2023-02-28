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

import com.qlangtech.tis.TIS;
import com.qlangtech.tis.common.utils.Assert;
import com.qlangtech.tis.extension.UberClassLoader;
import junit.framework.TestCase;

import java.io.File;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-07-14 14:09
 **/
public class TestPluginManifest extends TestCase {


    public void testGetExplodePluginManifest() throws Exception {
        UberClassLoader uberClassLoader = TIS.get().getPluginManager().uberClassLoader;
        // tis-datax-hudi-plugin
        Class<?> clazz = uberClassLoader.findClass("com.alibaba.datax.plugin.writer.hudi.HudiConfig");
        Assert.assertNotNull("clazz can not be null", clazz);

        PluginManifest.ExplodePluginManifest pluginManifest = PluginManifest.create(clazz);

        File pluginLibDir = pluginManifest.getPluginLibDir();
        Assert.assertTrue("pluginLibDir:" + pluginLibDir.getAbsolutePath(), pluginLibDir.exists());
    }
}
