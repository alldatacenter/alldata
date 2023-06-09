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
package com.qlangtech.tis.plugin;

import com.alibaba.citrus.turbine.Context;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.qlangtech.tis.TIS;
import com.qlangtech.tis.extension.Describable;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.impl.XmlFile;
import com.qlangtech.tis.manage.common.CenterResource;
import com.qlangtech.tis.manage.common.Config;
import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.order.center.IParamContext;
import com.qlangtech.tis.util.IPluginContext;
import com.qlangtech.tis.util.PluginMeta;
import com.qlangtech.tis.util.RobustReflectionConverter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 全局插件持久化存储
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class PluginStore<T extends Describable> implements IPluginStore<T> {
    private static final Logger logger = LoggerFactory.getLogger(PluginStore.class);
    private final transient Class<T> pluginClass;

    private List<T> plugins = Lists.newArrayList();
    private transient Set<PluginMeta> pluginMetas;
    // 在plugin 从xstream中反序列化之后再进行一下额外的处理
    private final transient IPluginProcessCallback<T>[] pluginCreateCallback;
    private transient final List<PluginsUpdateListener> pluginsUpdateListeners = Lists.newArrayList();

    private final transient XmlFile file;

    public PluginStore(Class<T> pluginClass, IPluginProcessCallback<T>... pluginCreateCallback) {
        this(pluginClass, Descriptor.getConfigFile(pluginClass.getName()), pluginCreateCallback);
    }

    public PluginStore(Class<T> pluginClass, XmlFile file, IPluginProcessCallback<T>... pluginCreateCallback) {
        this.pluginClass = pluginClass;
        this.file = file;
        this.pluginCreateCallback = pluginCreateCallback;
    }


    /**
     * 反序列化之后需要额外从其他地方加载属性到实例对象上
     *
     * @param <T>
     */
    public interface IPluginProcessCallback<T> {
        void afterDeserialize(T t);
    }

    public void cleanPlugins() {
        this.plugins.clear();
        this.loaded = false;
    }

    /**
     * 拷贝配置文件到本地
     */
    @Override
    public void copyConfigFromRemote() {
        CenterResource.copyFromRemote2Local(
                Config.KEY_TIS_PLUGIN_CONFIG + "/" + Descriptor.getPluginFileName(getSerializeFileName()), true);
    }

    /**
     * 目标文件
     *
     * @return
     */
    @Override
    public XmlFile getTargetFile() {
        return this.file;
    }

    @Override
    public List<T> getPlugins() {
        this.load();
        RobustReflectionConverter.PluginMetas metas = null;
        if (pluginMetas != null && !(metas = RobustReflectionConverter.usedPluginInfo.get()).isCacheable()) {
            metas.addAll(pluginMetas);
        }
        return plugins;
    }

    @Override
    public T find(String name) {
        return find(name, true);
    }

    @Override
    public T find(String name, boolean throwNotFoundErr) {
        List<T> plugins = this.getPlugins();
        if (!IdentityName.class.isAssignableFrom(this.pluginClass)) {
            throw new IllegalStateException(this.pluginClass + " can not find by name:" + name);
        }
        for (T item : plugins) {

            if (StringUtils.equals(name, ((IdentityName) item).identityValue())) {
                return item;
            }
        }
        if (throwNotFoundErr) {
            final String instanceName = this.pluginClass.getSimpleName();
            throw new IllegalStateException(instanceName + " has not be initialized,name:" + name + " can not find relevant '" + instanceName
                    + "' in ["
                    + plugins.stream().map((r) -> ((IdentityName) r).identityValue()).collect(Collectors.joining(",")) + "]");
        } else {
            return null;
        }
    }

    public List<Descriptor<T>> allDescriptor() {
        return TIS.get().getDescriptorList(this.pluginClass);
    }

    public T getPlugin() {
        if (this.getPlugins().size() > 1) {
            throw new IllegalStateException("plugin size can not much than 1");
        }
        for (T plugin : this.getPlugins()) {
            return plugin;
        }
        return null;
//        Optional<T> first = this.getPlugins().stream().findFirst();
//        if (!first.isPresent()) {
//            return null;
//        }
//        return first.get();
        // throw new IllegalStateException(" have not find any plugin,plugin size:" + this.getPlugins().size());
    }

    /**
     * 当本plugin还没有初始值的时候，可以从一个已经有的plugin把值拷贝过来<br>
     * 适用场景：全局设置了一个plugin的，collection绑定的plugin没有设置，当在设置collection绑定的plugin时候可以以全局plugin为模版，所以就有一个全局plugin向collection绑定的plugin拷贝属性的过程
     *
     * @param other
     */
    public synchronized void copyFrom(IPluginContext pluginContext, PluginStore<T> other) {
        if (this.getPlugin() != null) {
            throw new IllegalStateException("destination plugin store have saved ,can not copy from other");
        }
        if (other.getPlugin() == null) {
            throw new IllegalStateException("from plugin store have not initialized");
        }
        List<Descriptor.ParseDescribable<T>> dlist = Collections.singletonList(getDescribablesWithMeta(other, other.getPlugin()));
        this.setPlugins(pluginContext, Optional.empty(), dlist);
    }

    public static <TT extends Describable> Descriptor.ParseDescribable<TT> getDescribablesWithMeta(IPluginStore<TT> other, TT plugin) {
        Descriptor.ParseDescribable<TT> parseDescribable = new Descriptor.ParseDescribable<>(plugin);
        ComponentMeta cmetas = new ComponentMeta(other);
        parseDescribable.extraPluginMetas.addAll(cmetas.loadPluginMeta());
        return parseDescribable;
    }


    @Override
    public synchronized SetPluginsResult setPlugins(IPluginContext pluginContext, Optional<Context> context
            , List<Descriptor.ParseDescribable<T>> dlist) {
        // as almost the process is process file shall not care of process model whether update or add,bu some times have
        // extra process like db process ,shall pass a bool flag form client
        return this.setPlugins(pluginContext, context, dlist, false);
    }


    public void addPluginsUpdateListener(PluginsUpdateListener consumer) {
        Objects.requireNonNull(consumer, "param consumer can not be null");
        this.pluginsUpdateListeners.add(consumer);
    }

    public static abstract class PluginsUpdateListener implements Consumer<PluginStore<Describable>>, Recyclable {
        private final Recyclable recyclable;
        public final String identity;

        static final AtomicInteger ver = new AtomicInteger();

        public PluginsUpdateListener(String identity, Recyclable recyclable) {
            this.recyclable = recyclable;
            this.identity = identity + "@ver" + ver.incrementAndGet();
        }

//        @Override
//        public void accept(PluginStore<T> pluginStore) {
//            throw new UnsupportedOperationException();
//        }

        @Override
        public boolean isDirty() {
            return recyclable.isDirty();
        }
    }

    /**
     * save the plugin config
     *
     * @param pluginContext
     * @param context
     * @param dlist
     * @param update        whether the process is update or create
     * @return 文件更新之前和更新之后是否有变化
     */
    @Override
    public synchronized SetPluginsResult setPlugins(IPluginContext pluginContext, Optional<Context> context
            , List<Descriptor.ParseDescribable<T>> dlist, boolean update) {

        try {
            Set<PluginMeta> pluginsMeta = Sets.newHashSet();
            List<T> collect = dlist.stream().flatMap((r) -> {
                pluginsMeta.addAll(r.extraPluginMetas);
                if (!r.subFormFields) {
                    T instance = r.getInstance();
                    if (!this.pluginClass.isAssignableFrom(instance.getClass())) {
                        throw new IllegalStateException("plugin must be type of "
                                + this.pluginClass.getName() + ", but now is " + instance.getClass().getName());
                    }
                    for (IPluginProcessCallback<T> callback : pluginCreateCallback) {
                        callback.afterDeserialize(instance);
                    }

                }
                return (r.getSubFormInstances()).stream();
            }).collect(Collectors.toList());
            if (this.plugins != null) {
                this.plugins.forEach((plugin) -> {
                    if (plugin instanceof IPluginStore.RecyclableController) {
                        ((RecyclableController) plugin).signDirty();
                    }
                });
            }


            this.plugins = collect;
            boolean changed = this.file.write(this, pluginsMeta);
            long lastModifyTimestamp = -1;
            if (changed) {
                // 将代表组文件的更新时间戳更新
                lastModifyTimestamp = this.writeLastModifyTimeStamp();
                if (this.plugins != null) {
                    this.plugins.forEach((plugin) -> {
                        if (plugin instanceof AfterPluginSaved) {
                            ((AfterPluginSaved) plugin).afterSaved();
                        }
                    });
                }
            }

            if (CollectionUtils.isNotEmpty(pluginsUpdateListeners)) {
                Iterator<PluginsUpdateListener> it = pluginsUpdateListeners.iterator();
                PluginsUpdateListener next = null;
                while (it.hasNext()) {
                    next = it.next();
                    if (next.isDirty()) {
                        it.remove();
                        logger.info("dirty instance:" + next.identity + " will remove from watch listeners");
                        continue;
                    }
                    next.accept((PluginStore<Describable>) this);
                }
                logger.info("notify pluginsUpdateListeners size:" + pluginsUpdateListeners.size());
            }
            SetPluginsResult pluginsResult = new SetPluginsResult(true, changed);
            if (changed) {
                pluginsResult.lastModifyTimeStamp = lastModifyTimestamp;
            }
            return pluginsResult;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private long writeLastModifyTimeStamp() {
        File timestamp = getLastModifyTimeStampFile(this.file.getFile());
//        String millisecTimeStamp = IParamContext.getCurrentMillisecTimeStamp();
//        FileUtils.writeStringToFile(timestamp, millisecTimeStamp, TisUTF8.get());
//        return Long.parseLong(millisecTimeStamp);
        return writeLastModifyTimeStamp(timestamp);
    }

    protected long writeLastModifyTimeStamp(File timestamp) {
        // File timestamp = getLastModifyTimeStampFile(this.file.getFile());
        try {
            String millisecTimeStamp = IParamContext.getCurrentMillisecTimeStamp();
            FileUtils.writeStringToFile(timestamp, millisecTimeStamp, TisUTF8.get());
            return Long.parseLong(millisecTimeStamp);
        } catch (IOException e) {
            throw new RuntimeException(timestamp.getAbsolutePath(), e);
        }
    }

    public final long getWriteLastModifyTimeStamp() {
        try {
            File timestamp = getLastModifyTimeStampFile(this.file.getFile());
            if (!timestamp.exists()) {
                File cfg = this.file.getFile();
                if (!cfg.exists()) {
                    //  throw new IllegalArgumentException(timestamp.getName() + " is not exist,but cfg file also not exist:" + cfg.getAbsolutePath());
                    return -1;
                }
                return writeLastModifyTimeStamp();
            }
            return Long.parseLong(FileUtils.readFileToString(timestamp, TisUTF8.get()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public File getLastModifyTimeStampFile() {
        return getLastModifyTimeStampFile(this.file.getFile());
    }

    /**
     * 代表该配置组最后更新时间，用来和远程任务同步过程中，配置文件进行对比用
     *
     * @return
     */

    public static File getLastModifyTimeStampFile(File cfg) {
        return new File(cfg.getParentFile(), cfg.getName() + CenterResource.KEY_LAST_MODIFIED_EXTENDION);
    }

    protected String getSerializeFileName() {
        return pluginClass.getName();
    }

    private transient boolean loaded = false;

    private synchronized void load() {

        RobustReflectionConverter.PluginMetas metasCollector = RobustReflectionConverter.usedPluginInfo.get();
        if (metasCollector.isCacheable() && this.loaded) {
            return;
        }
        logger.debug("load:" + this.file.getFile().getAbsolutePath() + ",this.loaded:" + this.loaded);
        // MapBackedDataHolder dataHolder = new MapBackedDataHolder();
        Set<PluginMeta> pluginMetas = Sets.newHashSet();
        XmlFile.DefaultDataHolder dataHolder = new XmlFile.DefaultDataHolder(pluginMetas, this.file);
        try {
            // dataX 或者flink 启动过程中应该在启动的时候已经将资源文件同步了，这里就不需要再同步了
            //ComponentMeta componentMeta = new ComponentMeta(this);
            //componentMeta.downloaConfig();
            if (!file.exists()) {
                logger.warn("target xstream file is not exist:" + file.getFile().getAbsolutePath());
                return;
            }
            // 远程下载插件
            //List<XStream2.PluginMeta> pluginMetas = componentMeta.synchronizePluginsPackageFromRemote();
//            if (CollectionUtils.isNotEmpty(pluginMetas)) {
//                // 本地有插件包被更新了，需要更新一下pluginManager中已经加载了的插件了
//                // TODO 在运行时有插件被更新了，目前的做法只有靠重启了，将来再来实现运行是热更新插件
//            }

            file.unmarshal(this, dataHolder);
            if (plugins != null) {
                plugins.forEach((p) -> {
                    for (IPluginProcessCallback<T> callback : this.pluginCreateCallback) {
                        callback.afterDeserialize(p);
                    }
                });
            }
            this.loaded = true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ArrayList<Throwable> errors = (ArrayList<Throwable>) dataHolder.get("ReadError");
        if (CollectionUtils.isNotEmpty(errors)) {
            for (Throwable t : errors) {
                throw new RuntimeException(file.getFile().getAbsolutePath() + "\n" + TIS.get().getPluginManager().getFaildPluginsDesc(), t);
            }
        }
        this.pluginMetas = CollectionUtils.isEmpty(pluginMetas) ? null : pluginMetas;
    }
}
