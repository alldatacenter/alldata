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
package com.qlangtech.tis;

import com.google.common.collect.Lists;
import com.qlangtech.tis.component.GlobalComponent;
import com.qlangtech.tis.config.ParamsConfig;
import com.qlangtech.tis.datax.impl.DataxReader;
import com.qlangtech.tis.datax.impl.DataxWriter;
import com.qlangtech.tis.extension.*;
import com.qlangtech.tis.extension.impl.ClassicPluginStrategy;
import com.qlangtech.tis.extension.impl.ExtensionRefreshException;
import com.qlangtech.tis.extension.impl.XmlFile;
import com.qlangtech.tis.extension.init.InitMilestone;
import com.qlangtech.tis.extension.init.InitReactorRunner;
import com.qlangtech.tis.extension.init.InitStrategy;
import com.qlangtech.tis.extension.model.UpdateCenter;
import com.qlangtech.tis.extension.util.VersionNumber;
import com.qlangtech.tis.fullbuild.IFullBuildContext;
import com.qlangtech.tis.install.InstallState;
import com.qlangtech.tis.manage.IAppSource;
import com.qlangtech.tis.manage.common.Config;
import com.qlangtech.tis.offline.DbScope;
import com.qlangtech.tis.plugin.*;
import com.qlangtech.tis.plugin.ds.*;
import com.qlangtech.tis.util.*;
import org.apache.commons.lang.StringUtils;
import org.jvnet.hudson.reactor.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.qlangtech.tis.extension.init.InitMilestone.PLUGINS_PREPARED;

//import com.qlangtech.tis.offline.IndexBuilderTriggerFactory;
//import com.qlangtech.tis.offline.TableDumpFactory;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class TIS {

    /**
     * The version number before it is "computed" (by a call to computeVersion()).
     *
     * @since 2.0
     */
    public static final String UNCOMPUTED_VERSION = "?";

    /**
     * Version number of this Jenkins.
     */
    public static String VERSION = UNCOMPUTED_VERSION;

    private static final Logger logger = LoggerFactory.getLogger(TIS.class);
    public static final String DB_GROUP_NAME = "db";

    public static final String KEY_ALT_SYSTEM_PROP_TIS_PLUGIN_ROOT = "plugin_dir_root";
    public static final String KEY_TIS_PLUGIN_ROOT = "plugins";
    public static final String KEY_ACTION_CLEAN_TIS = "cleanTis";

    // public static final String KEY_TIS_INCR_COMPONENT_CONFIG_FILE = "incr_config.xml";
    public static final String KEY_TIE_GLOBAL_COMPONENT_CONFIG_FILE = "global_config.xml";

    private final transient UpdateCenter updateCenter = UpdateCenter.createUpdateCenter(null);


    /**
     * The Jenkins instance startup type i.e. NEW, UPGRADE etc
     */
    private transient String installStateName;
    private InstallState installState;


    /**
     * All {@link DescriptorExtensionList} keyed by their {@link DescriptorExtensionList}.
     */
    private static final transient Memoizer<Class<? extends Describable>, IPluginStore> globalPluginStore
            = new Memoizer<Class<? extends Describable>, IPluginStore>() {

        public PluginStore compute(Class<? extends Describable> key) {
            return new PluginStore(key);
        }
    };

    private static final transient Memoizer<KeyedPluginStore.Key, KeyedPluginStore> collectionPluginStore
            = new Memoizer<KeyedPluginStore.Key, KeyedPluginStore>() {
        public KeyedPluginStore compute(KeyedPluginStore.Key key) {
            return new KeyedPluginStore(key);
        }
    };


    public static final transient Memoizer<KeyedPluginStore.AppKey, KeyedPluginStore<IAppSource>> appSourcePluginStore
            = new Memoizer<KeyedPluginStore.AppKey, KeyedPluginStore<IAppSource>>() {
        @Override
        public KeyedPluginStore<IAppSource> compute(KeyedPluginStore.AppKey key) {
            return new KeyedPluginStore(key);
        }
    };

    public static final transient Memoizer<DataXReaderAppKey, KeyedPluginStore<DataxReader>> dataXReaderPluginStore
            = new Memoizer<DataXReaderAppKey, KeyedPluginStore<DataxReader>>() {
        @Override
        public KeyedPluginStore<DataxReader> compute(DataXReaderAppKey key) {
            return new KeyedPluginStore(key, key.pluginCreateCallback);
        }
    };

    public static final transient Memoizer<KeyedPluginStore.AppKey, KeyedPluginStore<DataxWriter>> dataXWriterPluginStore
            = new Memoizer<KeyedPluginStore.AppKey, KeyedPluginStore<DataxWriter>>() {
        @Override
        public KeyedPluginStore<DataxWriter> compute(KeyedPluginStore.AppKey key) {
            return new KeyedPluginStore(key);
        }
    };

    public static final transient Memoizer<DataxReader.SubFieldFormAppKey<? extends Describable>, KeyedPluginStore<? extends Describable>>
            dataXReaderSubFormPluginStore
            = new Memoizer<DataxReader.SubFieldFormAppKey<? extends Describable>, KeyedPluginStore<? extends Describable>>() {
        @Override
        public KeyedPluginStore<? extends Describable> compute(DataxReader.SubFieldFormAppKey<? extends Describable> key) {
            return new KeyedPluginStore(key);
        }
    };


    public static class DataXReaderAppKey extends KeyedPluginStore.AppKey<DataxReader> {
        public final PluginStore.IPluginProcessCallback<DataxReader> pluginCreateCallback;
        private final String appname;
        private final boolean isDB;

        public DataXReaderAppKey(IPluginContext pluginContext, StoreResourceType resType, String appname
                , PluginStore.IPluginProcessCallback<DataxReader> pluginCreateCallback) {
            super(pluginContext, resType, appname, DataxReader.class);
            if (pluginCreateCallback == null) {
                throw new IllegalStateException("param pluginCreateCallback can not be null");
            }
            this.pluginCreateCallback = pluginCreateCallback;
            this.appname = appname;
            this.isDB = (StoreResourceType.DataBase == resType);
        }

        public DataXReaderAppKey(IPluginContext pluginContext, boolean isDB, String appname
                , PluginStore.IPluginProcessCallback<DataxReader> pluginCreateCallback) {
            this(pluginContext, StoreResourceType.parse(isDB), appname, pluginCreateCallback);
        }

        public boolean isSameAppName(String appname, boolean isDB) {
            return this.appname.equals(appname) && (this.isDB == isDB);
        }
    }


    private static final transient Memoizer<DSKey, DataSourceFactoryPluginStore> databasePluginStore
            = new Memoizer<DSKey, DataSourceFactoryPluginStore>() {
        @Override
        public DataSourceFactoryPluginStore compute(DSKey key) {
            if (key.isFacadeType()) {
                // shall not maintance record in DB
                return new DataSourceFactoryPluginStore(key, false);
//                {
//                    @Override
//                    public void saveTable(String tableName) throws Exception {
//                        throw new UnsupportedOperationException("tableName:" + tableName);
//                    }
//                };
            } else {
                return new DataSourceFactoryPluginStore(key, true);
            }
        }
    };


    /**
     * Parses {@link #VERSION} into {@link 'VersionNumber'}, or null if it's not parseable as a version number
     * (such as when Jenkins is run with {@code mvn jetty:run})
     */
    public static VersionNumber getVersion() {
        return toVersion(VERSION);
    }


    public UpdateCenter getUpdateCenter() {
        return this.updateCenter;
    }

    /**
     * Parses a version string into {@link VersionNumber}, or null if it's not parseable as a version number
     * (such as when Jenkins is run with {@code mvn jetty:run})
     */
    private static VersionNumber toVersion(String versionString) {
        if (versionString == null) {
            return null;
        }

        try {
            return new VersionNumber(versionString);
        } catch (NumberFormatException e) {
            try {
                // for non-released version of Jenkins, this looks like "1.345 (private-foobar), so try to approximate.
                int idx = versionString.indexOf(' ');
                if (idx > 0) {
                    return new VersionNumber(versionString.substring(0, idx));
                }
            } catch (NumberFormatException ignored) {
                // fall through
            }

            // totally unparseable
            return null;
        } catch (IllegalArgumentException e) {
            // totally unparseable
            return null;
        }
    }

    /**
     * Refresh {@link ExtensionList}s by adding all the newly discovered extensions.
     * <p>
     * Exposed only for {@link 'PluginManager#dynamicLoad(File)'}.
     */
    public void refreshExtensions() throws ExtensionRefreshException {


        List<ExtensionFinder> finders = ClassicPluginStrategy.finders; // getExtensionList(ExtensionFinder.class);
//        for (ExtensionFinder ef : finders) {
//            if (!ef.isRefreshable()) {
//                throw new ExtensionRefreshException(ef + " doesn't support refresh");
//            }
//        }

        List<ExtensionComponentSet> fragments = new ArrayList<>();
        for (ExtensionFinder ef : finders) {
            fragments.add(ef.refresh());
        }
        ExtensionComponentSet delta = ExtensionComponentSet.union(fragments).filtered();

        // if we find a new ExtensionFinder, we need it to list up all the extension points as well
//        List<ExtensionComponent<ExtensionFinder>> newFinders = new ArrayList<>(delta.find(ExtensionFinder.class));
//        while (!newFinders.isEmpty()) {
//            ExtensionFinder f = newFinders.remove(newFinders.size() - 1).getInstance();
//
//            ExtensionComponentSet ecs = ExtensionComponentSet.allOf(f).filtered();
//            newFinders.addAll(ecs.find(ExtensionFinder.class));
//            delta = ExtensionComponentSet.union(delta, ecs);
//        }

        for (ExtensionList el : extensionLists.values()) {
            el.refresh(delta);
        }
        for (ExtensionList el : descriptorLists.values()) {
            el.refresh(delta);
        }

//        // TODO: we need some generalization here so that extension points can be notified when a refresh happens?
//        for (ExtensionComponent<RootAction> ea : delta.find(RootAction.class)) {
//            Action a = ea.getInstance();
//            if (!actions.contains(a)) actions.add(a);
//        }
    }

    public static IDataSourceFactoryPluginStoreGetter dsFactoryPluginStoreGetter;

    public static <DS extends DataSourceFactory> DS getDataBasePlugin(PostedDSProp dsProp) {
        return getDataBasePlugin(dsProp, true);
    }

    public static <DS extends DataSourceFactory> DS getDataBasePlugin(PostedDSProp dsProp, boolean validateNull) {
        DataSourceFactoryPluginStore pluginStore = getDataSourceFactoryPluginStore(dsProp);
        DS ds = (DS) pluginStore.getPlugin();
        if (validateNull) {
            Objects.requireNonNull(ds, dsProp.toString() + " relevant plugin can not be null ");
        }
        return ds;
    }

    public static DataSourceFactoryPluginStore getDataSourceFactoryPluginStore(PostedDSProp dsProp) {
        DataSourceFactoryPluginStore pluginStore = null;
        if (dsFactoryPluginStoreGetter != null) {
            pluginStore = dsFactoryPluginStoreGetter.getPluginStore(dsProp);
        } else {
            DSKey key = new DSKey(DB_GROUP_NAME, dsProp, DataSourceFactory.class);
            pluginStore = databasePluginStore.get(key);
        }
        return pluginStore;
    }

    public interface IDataSourceFactoryPluginStoreGetter {
        DataSourceFactoryPluginStore getPluginStore(PostedDSProp dsProp);
    }

    public static void deleteDB(String dbName, DbScope dbScope) {
        try {
            DataSourceFactoryPluginStore dsPluginStore = getDataSourceFactoryPluginStore(new PostedDSProp(DBIdentity.parseId(dbName), dbScope));
            dsPluginStore.deleteDB();
            databasePluginStore.clear(dsPluginStore.getDSKey());
        } catch (Exception e) {
            throw new RuntimeException(dbName, e);
        }
    }

    public static <T extends Describable> IPluginStore<T> getPluginStore(String collection, Class<T> key) {
//        PluginStore<T> pluginStore = collectionPluginStore.get(new KeyedPluginStore.Key(IFullBuildContext.NAME_APP_DIR, collection, key));
//        if (pluginStore == null) {
//            // 如果和collection自身绑定的plugin没有找到，就尝试找全局plugin
//            return getPluginStore(key);
//        } else {
//            return pluginStore;
//        }
        return getPluginStore(IFullBuildContext.NAME_APP_DIR, collection, key);
    }

    /**
     * Get the index relevant plugin configuration
     *
     * @param collection
     * @param key
     * @param <T>
     * @return
     */
    public static <T extends Describable> IPluginStore<T> getPluginStore(String groupName, String collection, Class<T> key) {
        IPluginStore<T> pluginStore = collectionPluginStore.get(new KeyedPluginStore.Key(groupName, collection, key));
        if (pluginStore == null) {
            // 如果和collection自身绑定的plugin没有找到，就尝试找全局plugin
            return getPluginStore(key);
        } else {
            return pluginStore;
        }
    }

    public static <T extends Describable> IPluginStore<T> getPluginStore(Class<T> key) {
        return globalPluginStore.get(key);
    }

    public final transient Memoizer<Class, ExtensionList> extensionLists = new Memoizer<Class, ExtensionList>() {

        public ExtensionList compute(Class key) {
            return ExtensionList.create(TIS.this, key);
        }
    };

    /**
     * All {@link DescriptorExtensionList} keyed by their {@link DescriptorExtensionList}.
     */
    public final transient Memoizer<Class, DescriptorExtensionList> descriptorLists = new Memoizer<Class, DescriptorExtensionList>() {

        public DescriptorExtensionList compute(Class key) {
            return DescriptorExtensionList.createDescriptorList(TIS.this, key);
        }
    };

    public final transient PluginManager pluginManager;

    public static final File pluginCfgRoot = new File(Config.getMetaCfgDir(), Config.KEY_TIS_PLUGIN_CONFIG);

    public static final File pluginDirRoot;

    static {
        String pluginRootDir = System.getProperty(KEY_ALT_SYSTEM_PROP_TIS_PLUGIN_ROOT);
        pluginDirRoot = StringUtils.isEmpty(pluginRootDir)
                ? new File(Config.getLibDir(), KEY_TIS_PLUGIN_ROOT)
                : new File(pluginRootDir);
    }

    private static TIS tis;

    public static void clean() {
        if (tis != null) {
            tis.cleanExtensionCache();
            tis = null;
        }
        cleanPluginStore();
        initialized = false;
    }

    public void cleanExtensionCache() {
        this.extensionLists.clear();
        this.descriptorLists.clear();
    }

    public static void cleanPluginStore() {
        globalPluginStore.clear();
        collectionPluginStore.clear();
        databasePluginStore.clear();
        appSourcePluginStore.clear();
        dataXReaderPluginStore.clear();
        dataXWriterPluginStore.clear();
        dataXReaderSubFormPluginStore.clear();
    }

    public static int cleanPluginStore(Class<? extends Describable> descClass) {
        MemoizerStoreProc[]
                stores = new MemoizerStoreProc[]{
                new MemoizerStoreProc(globalPluginStore) {
                    @Override
                    protected Class<Describable> getPluginClass(Map.Entry<?, ?> next) {
                        return (Class<Describable>) next.getKey();
                    }
                }
                , new MemoizerStoreProc(collectionPluginStore)
                , new MemoizerStoreProc(databasePluginStore)
                , new MemoizerStoreProc(appSourcePluginStore)
                , new MemoizerStoreProc(dataXReaderPluginStore)
                , new MemoizerStoreProc(dataXWriterPluginStore)
                , new MemoizerStoreProc(dataXReaderSubFormPluginStore)};
        int removeCount = 0;
        for (MemoizerStoreProc store : stores) {
            removeCount += store.clean(descClass);
        }
        return removeCount;
    }

    static class MemoizerStoreProc {
        final Memoizer<?, ?> store;

        public MemoizerStoreProc(Memoizer<?, ?> store) {
            this.store = store;
        }

        public final int clean(Class<? extends Describable> pluginClass) {
            Set<? extends Map.Entry<?, ?>> entries = store.getEntries();
            Iterator<? extends Map.Entry<?, ?>> iterator = entries.iterator();
            int removeCount = 0;
            while (iterator.hasNext()) {
                Class<Describable> key = getPluginClass(iterator.next());
                if (pluginClass == key) {
                    iterator.remove();
                    removeCount++;
                }
            }
            return removeCount;
        }

        protected Class<Describable> getPluginClass(Map.Entry<?, ?> next) {
            KeyedPluginStore.Key key = (KeyedPluginStore.Key) next.getKey();
            return key.pluginClass;
            //   return (Class<Describable>) next.getKey();
        }
    }


    // 插件运行系统是否已经初始化
    public static boolean initialized = false;

    // 允许初始化，防止在非console组件中初始化过程中，插件还没有下载好，TIS已经完成初始化
    public static boolean permitInitialize = true;

    private TIS() {
        final long start = System.currentTimeMillis();
        try {
            this.pluginManager = new PluginManager(pluginDirRoot);
            final InitStrategy is = InitStrategy.get(Thread.currentThread().getContextClassLoader());
            executeReactor(// loading and preparing plugins
                    is, // load jobs
                    pluginManager.initTasks(is, TIS.this), // forced ordering among key milestones
                    loadTasks(), InitMilestone.ordering());
            logger.info("tis plugin have been initialized,consume:{}ms.", System.currentTimeMillis() - start);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            initialized = true;
        }
    }

    private synchronized TaskBuilder loadTasks() {
        TaskGraphBuilder g = new TaskGraphBuilder();
        return g;
    }

    public static TIS get() {
        if (permitInitialize && tis == null) {
            synchronized (TIS.class) {
                if (permitInitialize && tis == null) {
                    tis = new TIS();
                }
            }
        }
        return tis;
    }

    public PluginManager getPluginManager() {
        return this.pluginManager;
    }

    public Descriptor getDescriptorOrDir(Class<? extends Describable> type) {
        Descriptor d = getDescriptor(type);
        if (d == null)
            throw new AssertionError(type + " is missing its descriptor");
        return d;
    }

    /**
     * Executes a reactor.
     *
     * @param is If non-null, this can be consulted for ignoring some tasks. Only used during the initialization of Jenkins.
     */
    private void executeReactor(final InitStrategy is, TaskBuilder... builders) throws IOException, InterruptedException, ReactorException {
        Reactor reactor = new Reactor(builders) {

            /**
             * Sets the thread name to the task for better diagnostics.
             */
            @Override
            protected void runTask(Task task) throws Exception {
                if (is != null && is.skipInitTask(task))
                    return;
                String taskName = task.getDisplayName();
                Thread t = Thread.currentThread();
                String name = t.getName();
                if (taskName != null)
                    t.setName(taskName);
                try {
                    super.runTask(task);
                } finally {
                    t.setName(name);
                    // SecurityContextHolder.clearContext();
                }
            }
        };
        new InitReactorRunner() {

            @Override
            protected void onInitMilestoneAttained(InitMilestone milestone) {
                // initLevel = milestone;
                if (milestone == PLUGINS_PREPARED) {
                    // set up Guice to enable injection as early as possible
                    // before this milestone, ExtensionList.ensureLoaded() won't actually try to locate instances
                    // ExtensionList.lookup(ExtensionFinder.class).getComponents();
                }
            }
        }.run(reactor);
    }

    // public File getRootDir() {
    // return this.root;
    // }

    /**
     * Exposes {@link Descriptor} by its name to URL.
     * <p>
     * After doing all the {@code getXXX(shortClassName)} methods, I finally realized that
     * this just doesn't scale.
     *
     * @param id Either {@link Descriptor#getId()} (recommended) or the short name of a {@link Describable} subtype (for compatibility)
     * @throws IllegalArgumentException if a short name was passed which matches multiple IDs (fail fast)
     */
    // too late to fix
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Descriptor getDescriptor(String id) {
        // legacy descriptors that are reigstered manually doesn't show up in getExtensionList, so check them explicitly.
        Iterable<Descriptor> descriptors = getExtensionList(Descriptor.class);
        for (Descriptor d : descriptors) {
            if (d.getId().equals(id)) {
                return d;
            }
        }
        Descriptor candidate = null;
        for (Descriptor d : descriptors) {
            String name = d.getId();
            if (name.substring(name.lastIndexOf('.') + 1).equals(id)) {
                if (candidate == null) {
                    candidate = d;
                } else {
                    throw new IllegalArgumentException(id + " is ambiguous; matches both " + name + " and " + candidate.getId());
                }
            }
        }
        return candidate;
    }

//    /**
//     *
//     * @param name
//     * @return
//     */
//    public Descriptor getDescriptorByDisplayName(String name) {
//        Iterable<Descriptor> descriptors = getExtensionList(Descriptor.class);
//        for (Descriptor d : descriptors) {
//            d.clazz
//            if (d.getDisplayName().equals(name)) {
//                return d;
//            }
//        }
//        return null;
//    }

    /**
     * Gets the {@link Descriptor} that corresponds to the given {@link Describable} type.
     * <p>
     * If you have an instance of {@code type} and call {@link Describable#getDescriptor()},
     * you'll get the same instance that this method returns.
     */
    public Descriptor getDescriptor(Class<? extends Describable> type) {
        if (java.lang.reflect.Modifier.isAbstract(type.getModifiers())) {
            throw new IllegalArgumentException("class can not be abstract:" + type.getName());
        }
        for (Descriptor d : getExtensionList(Descriptor.class)) {
            if (d.clazz == type)
                return d;
        }
        return null;
    }

    /**
     * Gets the {@link Descriptor} instance in the current Jenkins by its type.
     */
    public <T extends Descriptor> T getDescriptorByType(Class<T> type) {
        for (Descriptor d : getExtensionList(Descriptor.class))
            if (d.getClass() == type)
                return type.cast(d);
        return null;
    }

    public <T> ExtensionList<T> getExtensionList(Class<T> extensionType) {
        return extensionLists.get(extensionType);
    }

    /**
     * Returns {@link ExtensionList} that retains the discovered {@link Descriptor} instances for the given
     * kind of {@link Describable}.
     *
     * @return Can be an empty list but never null.
     */
    @SuppressWarnings({"unchecked"})
    public <T extends Describable<T>, D extends Descriptor<T>> DescriptorExtensionList<T, D> getDescriptorList(Class<T> type) {
        return descriptorLists.get(type);
    }

    private GlobalComponent globalComponent;

    public GlobalComponent loadGlobalComponent() {
        if (globalComponent == null) {
            try {
                File globalConfig = getGlobalConfigFile();
                if (!globalConfig.exists()) {
                    // 不存在的话
                    return new GlobalComponent();
                }
                globalComponent = (GlobalComponent) (new XmlFile(globalConfig).read());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return globalComponent;
    }

    public void saveComponent(GlobalComponent gloablComponent) {
        try {
            File gloabl = getGlobalConfigFile();
            (new XmlFile(gloabl)).write(gloablComponent, Collections.emptySet());
            this.globalComponent = null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 取得增量模块需要用到的plugin名称
     *
     * @param incrPluginConfigSet 增量相关的插件配置集合
     * @return
     */
    public static Set<PluginMeta> loadIncrComponentUsedPlugin(String collection, List<File> incrPluginConfigSet, boolean clearThreadholder) {
        try {
            synchronized (RobustReflectionConverter.usedPluginInfo) {
                if (clearThreadholder) {
                    RobustReflectionConverter.usedPluginInfo.remove();
                }
                for (File incrConfig : incrPluginConfigSet) {
                    if (!incrConfig.exists()) {
                        throw new IllegalStateException("file not exist,path:" + incrConfig.getAbsolutePath());
                    }
                    XmlFile xmlFile = new XmlFile(new XStream2PluginInfoReader(XmlFile.DEFAULT_DRIVER), incrConfig);
                    xmlFile.read();
                }
                return RobustReflectionConverter.usedPluginInfo.get().getMetas();
            }
        } catch (IOException e) {
            throw new RuntimeException("collection:" + collection + " relevant configs:"
                    + incrPluginConfigSet.stream().map((f) -> f.getAbsolutePath()).collect(Collectors.joining(",")), e);
        }
    }

    public static ComponentMeta getDumpAndIndexBuilderComponent(List<IRepositoryResource> resources) {
        checkNotInitialized();
        permitInitialize = false;
        resources.add(getPluginStore(ParamsConfig.class));
//        resources.add(getPluginStore(TableDumpFactory.class));
//        resources.add(getPluginStore(IndexBuilderTriggerFactory.class));
        return new ComponentMeta(resources);
    }

    /**
     * 取得solrcore 启动相关的插件资源
     *
     * @param resources
     * @return
     */
    public static ComponentMeta getCoreComponent(List<IRepositoryResource> resources) {
        checkNotInitialized();
        permitInitialize = false;
        // resources.add(getPluginStore(IndexBuilderTriggerFactory.class));
        return new ComponentMeta(resources);
    }

    public static ComponentMeta getDumpAndIndexBuilderComponent(IRepositoryResource... extractRes) {

        List<IRepositoryResource> resources = Lists.newArrayList();
        for (IRepositoryResource r : extractRes) {
            resources.add(r);
        }
        return getDumpAndIndexBuilderComponent(resources);
    }

    private static void checkNotInitialized() {
        if (initialized) {
            throw new IllegalStateException("TIS plugins has initialized");
        }
    }

    public static ComponentMeta getAssembleComponent() {
        checkNotInitialized();
        permitInitialize = false;

        List<IRepositoryResource> resources = Lists.newArrayList();
        // resources.add(TIS.getPluginStore(HeteroEnum.INDEX_BUILD_CONTAINER.extensionPoint));
        // resources.add(TIS.getPluginStore(FlatTableBuilder.class));
        // resources.add(TIS.getPluginStore(TableDumpFactory.class));
        resources.add(TIS.getPluginStore(ParamsConfig.class));
        return new ComponentMeta(resources);
    }

    private File getGlobalConfigFile() {
        return new File(pluginCfgRoot, "global" + File.separator + KEY_TIE_GLOBAL_COMPONENT_CONFIG_FILE);
    }

    /**
     * Update the current install state. This will invoke state.initializeState()
     * when the state has been transitioned.
     */
    public void setInstallState(InstallState newState) {
        String prior = installStateName;
        installStateName = newState.name();
        logger.info("Install state transitioning from: {} to : {}", prior, installStateName);
        if (!installStateName.equals(prior)) {
            // getSetupWizard().onInstallStateUpdate(newState);
            newState.initializeState();
        }
    }

    public InstallState getInstallState() {
        if (installState != null) {
            installStateName = installState.name();
            installState = null;
        }
        InstallState is = installStateName != null ? InstallState.valueOf(installStateName) : InstallState.UNKNOWN;
        return is != null ? is : InstallState.UNKNOWN;
    }
}
