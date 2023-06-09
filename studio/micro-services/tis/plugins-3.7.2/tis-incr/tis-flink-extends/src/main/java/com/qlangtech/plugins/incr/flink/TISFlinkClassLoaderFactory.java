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

import com.google.common.collect.Sets;
import com.qlangtech.tis.TIS;
import com.qlangtech.tis.coredefine.module.action.TargetResName;
import com.qlangtech.tis.extension.ITPIArtifact;
import com.qlangtech.tis.extension.PluginManager;
import com.qlangtech.tis.extension.UberClassLoader;
import com.qlangtech.tis.extension.impl.ClassicPluginStrategy;
import com.qlangtech.tis.manage.common.Config;
import com.qlangtech.tis.maven.plugins.tpi.PluginClassifier;
import com.qlangtech.tis.plugin.PluginAndCfgsSnapshot;
import com.qlangtech.tis.plugin.incr.TISSinkFactory;
import com.qlangtech.tis.realtime.BasicFlinkSourceHandle;
import com.qlangtech.tis.util.PluginMeta;
import org.apache.commons.io.FileUtils;
import org.apache.flink.runtime.execution.librarycache.BlobLibraryCacheManager;
import org.apache.flink.runtime.execution.librarycache.ClassLoaderFactoryBuilder;
import org.apache.flink.runtime.execution.librarycache.FlinkUserCodeClassLoaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.apache.flink.util.FlinkUserCodeClassLoader.NOOP_EXCEPTION_HANDLER;


public class TISFlinkClassLoaderFactory implements ClassLoaderFactoryBuilder {

    public static final String SKIP_CLASSLOADER_FACTORY_CREATION = "skip_classloader_factory_creation";

    private static final Logger logger = LoggerFactory.getLogger(TISFlinkClassLoaderFactory.class);

    @Override
    public BlobLibraryCacheManager.ClassLoaderFactory buildClientLoaderFactory(
            FlinkUserCodeClassLoaders.ResolveOrder classLoaderResolveOrder
            , String[] alwaysParentFirstPatterns
            , @Nullable Consumer<Throwable> exceptionHander, boolean checkClassLoaderLeak) {
        this.makeDataDirUseable();
        TIS tis = TIS.get();
        PluginManager pluginManager = tis.getPluginManager();


        return (libraryURLs) -> {
            logger.info("create Flink app classloader:{},resolveOrder:{}"
                    , Arrays.stream(libraryURLs).map((url) -> String.valueOf(url)).collect(Collectors.joining(","))
                    , classLoaderResolveOrder);
            try {
                //
                PluginMeta flinkPluginMeta = null;
                TargetResName tisAppName = null;
                PluginAndCfgsSnapshot cfg = null;
                for (URL cp : libraryURLs) {
                    // 从对应的资源中将对应的plugin的目录解析出来，放到data目录下去
                    cfg = PluginAndCfgsSnapshot.getRepositoryCfgsSnapshot(cp.toString(), cp.openStream(), false);
                    tisAppName = cfg.getAppName();

                    flinkPluginMeta = new PluginMeta(TISSinkFactory.KEY_PLUGIN_TPI_CHILD_PATH + tisAppName.getName()
                            , Config.getMetaProps().getVersion(), Optional.of(PluginClassifier.MATCH_ALL_CLASSIFIER));
                    break;
                }
                Objects.requireNonNull(cfg, "cfg can not be null");
                if (tisAppName == null) {
                    throw new IllegalStateException("param tisAppName can not be empty");
                }
                if (flinkPluginMeta == null || !flinkPluginMeta.getPluginPackageFile().exists()) {
                    throw new IllegalStateException("appPluginDir can not be empty,path:"
                            + flinkPluginMeta.getPluginPackageFile().getAbsolutePath());
                }
                final String shotName = TISSinkFactory.KEY_PLUGIN_TPI_CHILD_PATH + tisAppName.getName();
                ClassicPluginStrategy.removeByClassNameInFinders(BasicFlinkSourceHandle.class);

                pluginManager.dynamicLoad(ITPIArtifact.create(shotName, flinkPluginMeta.classifier)
                        , flinkPluginMeta.getPluginPackageFile(), true, null);

                return FlinkUserCodeClassLoaders.create(
                        classLoaderResolveOrder,
                        libraryURLs,
                        new UberClassLoader(pluginManager, cfg.getPluginNames()),
                        alwaysParentFirstPatterns,
                        NOOP_EXCEPTION_HANDLER,
                        checkClassLoaderLeak);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        };
    }

    private void makeDataDirUseable() {
        try {

            FileUtils.forceMkdir(PluginAndCfgsSnapshot.getPluginRootDir());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // 本地缓存
    private PluginAndCfgsSnapshot localCache = null;

    @Override
    public BlobLibraryCacheManager.ClassLoaderFactory buildServerLoaderFactory(
            FlinkUserCodeClassLoaders.ResolveOrder classLoaderResolveOrder
            , String[] alwaysParentFirstPatterns, @Nullable Consumer<Throwable> exceptionHander, boolean checkClassLoaderLeak) {

        if (Boolean.getBoolean(SKIP_CLASSLOADER_FACTORY_CREATION)) {

            return (urls) -> FlinkUserCodeClassLoaders.create(classLoaderResolveOrder,
                    urls,
                    TIS.get().getPluginManager().uberClassLoader,
                    //TISFlinkClassLoaderFactory.class.getClassLoader(),
                    alwaysParentFirstPatterns,
                    NOOP_EXCEPTION_HANDLER,
                    checkClassLoaderLeak);
        }
        this.makeDataDirUseable();
        logger.info("buildServerLoader classLoaderResolveOrder:{}", classLoaderResolveOrder);
        return new BlobLibraryCacheManager.DefaultClassLoaderFactory(classLoaderResolveOrder
                , alwaysParentFirstPatterns, exceptionHander, checkClassLoaderLeak) {
            @Override
            public URLClassLoader createClassLoader(URL[] libraryURLs) {
                try {
                    PluginAndCfgsSnapshot cfgSnapshot = null;//= getTisAppName();
                    File nodeExcludeLock = new File(Config.getDataDir(), "initial.lock");
                    FileUtils.touch(nodeExcludeLock);
                    RandomAccessFile raf = new RandomAccessFile(nodeExcludeLock, "rw");
                    try (FileChannel channel = raf.getChannel()) {
                        // 服务器节点级别通过文件来排他
                        try (FileLock fileLock = channel.tryLock()) {
                            PluginAndCfgsSnapshot localSnaphsot = null;
                            try {
                                TIS.permitInitialize = false;
                                for (URL url : libraryURLs) {
                                    cfgSnapshot = PluginAndCfgsSnapshot.getRepositoryCfgsSnapshot(url.toString(), url.openStream());
                                    break;
                                }
                                Objects.requireNonNull(cfgSnapshot, "cfgSnapshot can not be null,libraryURLs size:" + libraryURLs.length);
                                //  boolean tisInitialized = TIS.initialized;
                                // PluginAndCfgsSnapshot cfgSnapshot = getTisAppName();
                                logger.info("start createClassLoader of app:" + cfgSnapshot.getAppName().getName());

                                // TIS.clean();
                                // 这里只需要类不需要配置文件了
                                PluginMeta flinkPluginMeta
                                        = new PluginMeta(TISSinkFactory.KEY_PLUGIN_TPI_CHILD_PATH + cfgSnapshot.getAppName().getName()
                                        , Config.getMetaProps().getVersion(), Optional.empty());
                                // 服务端不需要配置文件，只需要能够加载到类就行了
                                localSnaphsot = PluginAndCfgsSnapshot.getWorkerPluginAndCfgsSnapshot(cfgSnapshot.getAppName(), Sets.newHashSet(flinkPluginMeta));
                            } finally {
                                TIS.permitInitialize = true;
                            }
                            cfgSnapshot.synchronizTpisAndConfs(localSnaphsot, Optional.ofNullable(localCache));


                            localCache = cfgSnapshot;

                        }
                    }


                    final Set<String> relativePluginNames = cfgSnapshot.getPluginNames();
                    logger.info("relativePluginNames:{}", relativePluginNames.stream().collect(Collectors.joining(",")));
                    return new TISChildFirstClassLoader(new UberClassLoader(TIS.get().getPluginManager(), relativePluginNames)
                            , libraryURLs, this.getParentClassLoader()
                            , this.alwaysParentFirstPatterns, this.classLoadingExceptionHandler);
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }


//    public static void synchronizeIncrPluginsFromRemoteRepository(String appName) {
//
//        if (CenterResource.notFetchFromCenterRepository()) {
//            return;
//        }
//
//        TIS.permitInitialize = false;
//        try {
//            if (StringUtils.isBlank(appName)) {
//                throw new IllegalArgumentException("param appName can not be null");
//            }
//
//            List<IRepositoryResource> keyedPluginStores = Lists.newArrayList();
//
//            keyedPluginStores.add(new KeyedPluginStore(new KeyedPluginStore.AppKey(null, false, appName, MQListenerFactory.class)));
//            keyedPluginStores.add(new KeyedPluginStore(new KeyedPluginStore.AppKey(null, false, appName, IncrStreamFactory.class)));
//            keyedPluginStores.add(new KeyedPluginStore(new KeyedPluginStore.AppKey(null, false, appName, TISSinkFactory.class)));
//            ComponentMeta dataxComponentMeta = new ComponentMeta(keyedPluginStores);
//            dataxComponentMeta.synchronizePluginsFromRemoteRepository();
//
//
//        } finally {
//            TIS.permitInitialize = true;
//        }
//    }


}
