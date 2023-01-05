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

package com.qlangtech.plugins.incr.flink;

import com.qlangtech.tis.TIS;
import com.qlangtech.tis.extension.PluginManager;
import com.qlangtech.tis.extension.impl.IOUtils;
import com.qlangtech.tis.manage.common.Config;
import com.qlangtech.tis.manage.common.HttpUtils;
import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.plugin.KeyedPluginStore;
import com.qlangtech.tis.solr.common.DOMUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.flink.runtime.execution.librarycache.BlobLibraryCacheManager;
import org.apache.flink.runtime.execution.librarycache.FlinkUserCodeClassLoaders;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.function.Consumer;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * 事先需要将hudi Stream Code打好包
 * com.qlangtech.tis.realtime.transfer.hudi.HudiSourceHandle
 *
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-04-06 14:58
 **/
public class TestHudiTISFlinkClassLoaderFactory {

    @ClassRule
    public static TemporaryFolder folder = new TemporaryFolder();
    private static File streamUberJar;

    @BeforeClass
    public static void beforeLaunch() throws Exception {
        File dataDir = folder.newFolder("dataDir");
        Config.setDataDir(dataDir.getAbsolutePath());

        File jarDir = folder.newFolder("jarDir");
        Manifest manifest = new Manifest();
        IOUtils.loadResourceFromClasspath(
                TestHudiTISFlinkClassLoaderFactory.class, "hudijar_manifest.mf", true, (stream) -> {
                    manifest.read(stream);
                    return null;
                });
        streamUberJar = new File(jarDir, "hudi.jar");
        try (JarOutputStream jaroutput = new JarOutputStream(
                FileUtils.openOutputStream(streamUberJar, false), manifest)) {
            jaroutput.flush();
        }

        HttpUtils.mockConnMaker = new HttpUtils.DefaultMockConnectionMaker();

        HttpUtils.addMockApply(PluginManager.PACAKGE_TPI_EXTENSION, new HttpUtils.IClasspathRes() {
            @Override
            public InputStream getResourceAsStream(URL url) {
                ///opt/data/tis/libs/plugins/
                try {
                    String path = URLDecoder.decode(StringUtils.substringAfter(url.getQuery(), "path="));
                    File tpi = new File(Config.DEFAULT_DATA_DIR, path);
                    return FileUtils.openInputStream(tpi);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        HttpUtils.addMockApply("." + DOMUtil.XML_RESERVED_PREFIX, new HttpUtils.IClasspathRes() {
            @Override
            public InputStream getResourceAsStream(URL url) {
                ///opt/data/tis/libs/plugins/
                try {
                    String path = URLDecoder.decode(StringUtils.substringAfter(url.getQuery(), "path="));
                    File xml = new File(Config.DEFAULT_DATA_DIR, path);
                    if (!xml.exists()) {
                        throw new IllegalStateException("cfg is not exist:" + xml.getAbsolutePath());
                    }
                    return FileUtils.openInputStream(xml);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });


        KeyedPluginStore.AppKey appKey = new KeyedPluginStore.AppKey(null, false, "hudi", null);
        String appPath = Config.SUB_DIR_CFG_REPO + File.separator + Config.KEY_TIS_PLUGIN_CONFIG + File.separator + appKey.getSubDirPath();
        HttpUtils.addMockApply(-1, new HttpUtils.MockMatchKey(URLEncoder.encode(appPath, TisUTF8.getName()), false, true), new HttpUtils.IClasspathRes() {
            @Override
            public InputStream getResourceAsStream(URL url) {
                try {
                    return new ByteArrayInputStream(IOUtils.writeZip(new File(Config.DEFAULT_DATA_DIR, appPath)));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });

    }

    @Test
    public void testBuildServerLoaderFactory() throws Exception {
        TISFlinkClassLoaderFactory loaderFactory = new TISFlinkClassLoaderFactory();

        String[] alwaysParentFirstPatterns = new String[]{};
        Consumer<Throwable> exceptionHander = (e) -> {
        };
        BlobLibraryCacheManager.ClassLoaderFactory classLoaderFactory
                = loaderFactory.buildServerLoaderFactory(
                FlinkUserCodeClassLoaders.ResolveOrder.CHILD_FIRST, alwaysParentFirstPatterns, exceptionHander, false);

        Assert.assertNotNull(streamUberJar);

        URL[] urls = new URL[]{streamUberJar.toURI().toURL()};

        URLClassLoader clazzLoader = classLoaderFactory.createClassLoader(urls);
        Assert.assertNotNull("clazzLoader can not be null", clazzLoader);

        Class<?> clazz = clazzLoader.loadClass("com.qlangtech.tis.realtime.transfer.hudi.HudiSourceHandle");
        Assert.assertNotNull(clazz);

        clazzLoader = classLoaderFactory.createClassLoader(urls);
        Assert.assertNotNull("clazzLoader can not be null", clazzLoader);
    }
}
