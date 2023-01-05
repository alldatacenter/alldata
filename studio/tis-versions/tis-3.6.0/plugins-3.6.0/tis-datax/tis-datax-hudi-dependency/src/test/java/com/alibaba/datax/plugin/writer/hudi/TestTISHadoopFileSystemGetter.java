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

package com.alibaba.datax.plugin.writer.hudi;

import com.qlangtech.tis.coredefine.module.action.TargetResName;
import com.qlangtech.tis.manage.common.CenterResource;
import com.qlangtech.tis.plugin.PluginAndCfgsSnapshot;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;
import org.apache.hudi.common.fs.FSUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Optional;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-05-10 13:06
 **/
public class TestTISHadoopFileSystemGetter {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();


    @Test
    public void testExtraHadoopFileSystemGetter() throws Exception {

        TargetResName resName = new TargetResName("hudi");
        File manifestJar = folder.newFile("manifest.jar");
        CenterResource.setNotFetchFromCenterRepository();
        PluginAndCfgsSnapshot.createManifestCfgAttrs2File(manifestJar, resName, -1, Optional.of((meta) -> {
            return !resName.equalWithName(meta.getPluginName()) && !(resName.getName().indexOf("hudi") > -1);
        }));

        URLClassLoader classLoader = new URLClassLoader(
                new URL[]{manifestJar.toURL()}, Thread.currentThread().getContextClassLoader());
        Thread.currentThread().setContextClassLoader(classLoader);

        CenterResource.setFetchFromCenterRepository(false);
        Configuration cfg = new Configuration();
        FileSystem fs = FSUtils.getFs("/", cfg);
        Assert.assertNotNull("fs can not be null", fs);

        RemoteIterator<LocatedFileStatus> list = fs.listFiles(new Path("/"), false);
        while (list.hasNext()) {
            System.out.println(list.next());
        }
    }
}
