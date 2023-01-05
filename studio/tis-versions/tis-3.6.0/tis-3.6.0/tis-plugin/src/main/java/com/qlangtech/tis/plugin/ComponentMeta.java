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
import com.qlangtech.tis.extension.impl.XmlFile;
import com.qlangtech.tis.util.PluginMeta;
import com.qlangtech.tis.util.RobustReflectionConverter;
import com.qlangtech.tis.util.XStream2PluginInfoReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * 组件元数据信息
 *
 * @author 百岁（baisui@qlangtech.com）
 * @create: 2020-04-24 16:24
 */
public class ComponentMeta {
    private static final Logger logger = LoggerFactory.getLogger(ComponentMeta.class);
    public final List<IRepositoryResource> resources;

    /**
     * key 为文件名，val：最后更新时间 20开头的
     *
     * @return
     */
    public static Map<String, Long> getGlobalPluginStoreLastModifyTimestamp(ComponentMeta meta) {
        return meta.resources.stream().collect(Collectors.toMap((r) -> {
            return r.getTargetFile().relativePath;
        }, (r) -> {
            return r.getWriteLastModifyTimeStamp();
        }));
    }


    public ComponentMeta(List<IRepositoryResource> resources) {
        this.resources = resources;
    }

    public ComponentMeta(IRepositoryResource resource) {
        this(Collections.singletonList(resource));
    }

    public void addResource(IRepositoryResource rr) {
        this.resources.add(rr);
    }

    /**
     * 下载配置文件
     */
    public void downloaConfig() {
        resources.forEach((r) -> {
            r.copyConfigFromRemote();
        });
    }

    /**
     * 取得元数据信息
     *
     * @return
     */
    public Set<PluginMeta> loadPluginMeta() {

        return loadPluginMeta(() -> {
            List<File> cfgs = Lists.newArrayList();
            for (IRepositoryResource res : this.resources) {
                File targetFile = res.getTargetFile().getFile();
                if (!targetFile.exists()) {
                    continue;
                }
                cfgs.add(targetFile);
            }
            return cfgs;
        });
    }


    public static Set<PluginMeta> loadPluginMeta(Callable<List<File>> xstreamFilesProvider) {

        try {
            synchronized (RobustReflectionConverter.usedPluginInfo) {
                RobustReflectionConverter.usedPluginInfo.remove();
                XStream2PluginInfoReader reader = new XStream2PluginInfoReader(XmlFile.DEFAULT_DRIVER);

                List<File> cfgs = xstreamFilesProvider.call();
                for (File targetFile : cfgs) {
                    XmlFile xmlFile = new XmlFile(reader, targetFile);
                    xmlFile.read();
                }
                return RobustReflectionConverter.usedPluginInfo.get().getMetas();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * 同步插件
     */
    public void synchronizePluginsFromRemoteRepository() {
        try {
            this.downloaConfig();
            this.synchronizePluginsPackageFromRemote();
        } finally {
            TIS.permitInitialize = true;
        }
        if (TIS.initialized) {
            throw new IllegalStateException("make sure TIS plugin have not be initialized");
        }
    }


    /**
     * 同步插件包
     *
     * @return 本地被更新的插件包
     */
    public List<PluginMeta> synchronizePluginsPackageFromRemote() {
        List<PluginMeta> updateTpiPkgs = Lists.newArrayList();
        Set<PluginMeta> pluginMetas = loadPluginMeta();
        try {
            for (PluginMeta m : pluginMetas) {
                List<File> pluginFileCollector = Lists.newArrayList();
                if (m.copyFromRemote(pluginFileCollector)) {
                    // 本地包已经被更新
                    updateTpiPkgs.add(m);
                    if (TIS.permitInitialize) {
                        for (File f : pluginFileCollector) {
                            // 动态安装插件
                            TIS.get().getPluginManager().dynamicLoad(f, true, null);
                        }
                    }
                }
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        logger.info("download plugin from remote repository:"
                + updateTpiPkgs.stream().map((m) -> m.toString()).collect(Collectors.joining(",")));
        return updateTpiPkgs;
    }
}
