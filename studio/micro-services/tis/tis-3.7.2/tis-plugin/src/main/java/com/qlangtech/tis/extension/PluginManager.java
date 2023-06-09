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
package com.qlangtech.tis.extension;

import com.qlangtech.tis.TIS;
import com.qlangtech.tis.extension.impl.ClassicPluginStrategy;
import com.qlangtech.tis.extension.impl.ExtensionRefreshException;
import com.qlangtech.tis.extension.impl.MissingDependencyException;
import com.qlangtech.tis.extension.impl.PluginManifest;
import com.qlangtech.tis.extension.init.InitMilestone;
import com.qlangtech.tis.extension.init.InitReactorRunner;
import com.qlangtech.tis.extension.init.InitStrategy;
import com.qlangtech.tis.extension.model.UpdateCenter;
import com.qlangtech.tis.extension.util.CyclicGraphDetector;
import com.qlangtech.tis.manage.common.CenterResource;
import com.qlangtech.tis.maven.plugins.tpi.PluginClassifier;
import com.qlangtech.tis.plugin.PluginAndCfgsSnapshot;
import com.qlangtech.tis.util.InitializerFinder;
import com.qlangtech.tis.util.Util;
import com.qlangtech.tis.util.YesNoMaybe;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.jvnet.hudson.reactor.Executable;
import org.jvnet.hudson.reactor.Reactor;
import org.jvnet.hudson.reactor.TaskBuilder;
import org.jvnet.hudson.reactor.TaskGraphBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import static com.qlangtech.tis.extension.init.InitMilestone.*;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class PluginManager {

    public static final String PACAKGE_CLASSIFIER = "classifier";
    public static final String PACAKGE_TPI_EXTENSION = "." + PluginClassifier.PACAKGE_TPI_EXTENSION_NAME;

    private static final Logger logger = LoggerFactory.getLogger(PluginManager.class);

    private static final Logger LOGGER = logger;

    private final PluginStrategy strategy;

    private final File workDir;

    public final File rootDir;

    /**
     * Once plugin is uploaded, this flag becomes true.
     * This is used to report a message that Jenkins needs to be restarted
     * for new plugins to take effect.
     */
    public volatile boolean pluginUploaded = false;

    // private static final Logger logger = LoggerFactory.getLogger(TIS.class.getName());
    public final PluginManager.PluginInstanceStore pluginInstanceStore = new PluginManager.PluginInstanceStore();

    public final UberClassLoader uberClassLoader = new UberClassLoader(this);

    public File getWorkDir() {
        return workDir;
    }

    boolean pluginListed = false;

    /**
     * All discovered plugins.
     */
    protected final List<PluginWrapper> plugins = new ArrayList<PluginWrapper>() {
        @Override
        public boolean add(PluginWrapper pluginWrapper) {
            return super.add(pluginWrapper);
        }
    };

    /**
     * All active plugins, topologically sorted so that when X depends on Y, Y appears in the list before X does.
     */
    public final List<PluginWrapper> activePlugins = new CopyOnWriteArrayList<PluginWrapper>();

    public Map<String, PluginWrapper> getActivePluginsMap() {
        return activePlugins.stream().collect(Collectors.toMap((p) -> p.getShortName(), (p) -> p));
    }

    protected final List<FailedPlugin> failedPlugins = new ArrayList<FailedPlugin>();

    public String getFaildPluginsDesc() {
        if (CollectionUtils.isEmpty(failedPlugins)) {
            return "FailedPlugins is Empty";
        }
        return failedPlugins.stream().map((f) -> "plugin:" + f.name + ",cause:"
                + ExceptionUtils.getMessage(f.cause)).collect(Collectors.joining(","));
    }

    public PluginManager(File rootDir) {
        try {
            if (!rootDir.exists()) {
                FileUtils.forceMkdir(rootDir);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.workDir = rootDir;
        this.rootDir = rootDir;
        this.strategy = this.createPluginStrategy();
    }

    protected PluginStrategy createPluginStrategy() {
        // default and fallback
        return new ClassicPluginStrategy(this);
    }

    public PluginStrategy getPluginStrategy() {
        return strategy;
    }

    /**
     * Try the dynamicLoad, removeExisting to attempt to dynamic load disabled plugins
     */
    public void dynamicLoad(File arc, boolean removeExisting
            , PluginAndCfgsSnapshot.PluginWrapperList batch) throws IOException, InterruptedException, RestartRequiredException {
        // try (ACLContext context = ACL.as2(ACL.SYSTEM2)) {
        LOGGER.info("Attempting to dynamic load {}", arc);
        PluginManifest manifest = PluginManifest.create(arc);
        if (manifest == null) {
            LOGGER.warn("can not find tpi:" + arc.getAbsolutePath());
            return;
        }
//        PluginWrapper p = null;
//        String sn;
//        try {
//            sn = strategy.getShortName(arc);
//        } catch (AbstractMethodError x) {
//            LOGGER.info("JENKINS-12753 fix not active: {}", x.getMessage());
//            p = strategy.createPluginWrapper(arc);
//            sn = p.getShortName();
//        }
        ITPIArtifactMatch install = ITPIArtifact.create(
                manifest.computeShortName(StringUtils.EMPTY), manifest.parseClassifier());
        this.dynamicLoad(install, arc, removeExisting, batch);
    }


    public void dynamicLoad(ITPIArtifactMatch art, File arc, boolean removeExisting
            , PluginAndCfgsSnapshot.PluginWrapperList batch)
            throws IOException, InterruptedException, RestartRequiredException {
        // try (ACLContext context = ACL.as2(ACL.SYSTEM2)) {


        PluginWrapper p = null;

        PluginWrapper pw = getPlugin(art);
        if (pw != null) {
            if (removeExisting) { // try to load disabled plugins
                for (Iterator<PluginWrapper> i = plugins.iterator(); i.hasNext(); ) {
                    pw = i.next();
                    if (art.getIdentityName().equals(pw.getShortName())) {
                        i.remove();
                        break;
                    }
                }
                PluginWrapper aplugin = null;
                for (Iterator<PluginWrapper> i = activePlugins.iterator(); i.hasNext(); ) {
                    pw = i.next();
                    if (art.getIdentityName().equals(pw.getShortName())) {
                        aplugin = pw;
                        break;
                    }
                }
                if (aplugin != null) {
                    activePlugins.remove(aplugin);
                }
            } else {
                throw new RestartRequiredException("PluginIsAlreadyInstalled_RestartRequired:" + (art.getIdentityName()));
            }
        }
        if (p == null) {
            p = strategy.createPluginWrapper(arc);
        }
        if (p.supportsDynamicLoad() == YesNoMaybe.NO) {
            throw new RestartRequiredException("PluginDoesntSupportDynamicLoad_RestartRequired:" + (art.getIdentityName()));
        }
        // there's no need to do cyclic dependency check, because we are deploying one at a time,
        // so existing plugins can't be depending on this newly deployed one.

        plugins.add(p);
        if (p.isActive()) {
            activePlugins.add(p);
        }
        synchronized (((UberClassLoader) uberClassLoader).loaded) {
            ((UberClassLoader) uberClassLoader).loaded.clear();
        }

        // TODO antimodular; perhaps should have a PluginListener to complement ExtensionListListener?
        //  CustomClassFilter.Contributed.load();

        try {
            p.resolvePluginDependencies();
            strategy.load(p);

            if (batch != null) {
                batch.add(p);
            } else {
                start(new PluginAndCfgsSnapshot.PluginWrapperList(p));
            }

        } catch (Exception e) {
            failedPlugins.add(new FailedPlugin(art.getIdentityName(), e));
            activePlugins.remove(p);
            plugins.remove(p);
            throw new IOException("Failed to install " + art.getIdentityName() + " plugin", e);
        }

        LOGGER.info("Plugin {}:{} dynamically {}", p.getShortName(), p.getVersion()
                , batch != null ? "loaded but not yet started" : "installed");
        //}
    }


    public void start(PluginAndCfgsSnapshot.PluginWrapperList plugins) throws Exception {
        Map<String, PluginWrapper> pluginsByName = plugins.getPluginsByName(); //plugins.stream().collect(Collectors.toMap(PluginWrapper::getShortName, p -> p));

        // recalculate dependencies of plugins optionally depending the newly deployed ones.
        for (PluginWrapper depender : this.plugins) {
            if (plugins.contains(depender)) {
                // skip itself.
                continue;
            }
            for (PluginWrapper.Dependency d : depender.getOptionalDependencies()) {
                PluginWrapper dependee = pluginsByName.get(d.shortName);
                if (dependee != null) {
                    // this plugin depends on the newly loaded one!
                    // recalculate dependencies!
                    getPluginStrategy().updateDependency(depender, dependee);
                    break;
                }
            }
        }

        // Redo who depends on who.
        resolveDependentPlugins();

        try {
            TIS.get().refreshExtensions();
        } catch (ExtensionRefreshException e) {
            throw new IOException("Failed to refresh extensions after installing some plugins", e);
        }
        for (PluginWrapper p : plugins.getPlugins()) {
            //TODO:According to the postInitialize() documentation, one may expect that
            //p.getPluginOrFail() NPE will continue the initialization. Keeping the original behavior ATM
            p.getPluginOrFail().postInitialize();
        }

        // run initializers in the added plugins
        Reactor r = new Reactor(InitMilestone.ordering());
        Set<ClassLoader> loaders = plugins.getLoaders(); //plugins.stream().map(p -> p.classLoader).collect(Collectors.toSet());
        r.addAll(new InitializerFinder(uberClassLoader) {
            @Override
            protected boolean filter(Method e) {
                return !loaders.contains(e.getDeclaringClass().getClassLoader()) || super.filter(e);
            }
        }.discoverTasks(r));

        new InitReactorRunner().run(r);
    }


    public synchronized void resolveDependentPlugins() {
        for (PluginWrapper plugin : plugins) {
            // Set of optional dependents plugins of plugin
            Set<String> optionalDependents = new HashSet<>();
            Set<String> dependents = new HashSet<>();
            for (PluginWrapper possibleDependent : plugins) {
                // No need to check if plugin is dependent of itself
                if (possibleDependent.getShortName().equals(plugin.getShortName())) {
                    continue;
                }

                // The plugin could have just been deleted. If so, it doesn't
                // count as a dependent.
                if (possibleDependent.isDeleted()) {
                    continue;
                }
                List<PluginWrapper.Dependency> dependencies = possibleDependent.getDependencies();
                for (PluginWrapper.Dependency dependency : dependencies) {
                    if (dependency.shortName.equals(plugin.getShortName())) {
                        dependents.add(possibleDependent.getShortName());

                        // If, in addition, the dependency is optional, add to the optionalDependents list
                        if (dependency.optional) {
                            optionalDependents.add(possibleDependent.getShortName());
                        }

                        // already know possibleDependent depends on plugin, no need to continue with the rest of
                        // dependencies. We continue with the next possibleDependent
                        break;
                    }
                }
            }
            plugin.setDependents(dependents);
            plugin.setOptionalDependents(optionalDependents);
        }
    }


    public TaskBuilder initTasks(final InitStrategy initStrategy, TIS tis) {
        TaskBuilder builder;
        if (!pluginListed) {
            builder = new TaskGraphBuilder() {

                List<File> archives;

                Collection<String> bundledPlugins = Collections.emptyList();

                {
                    // Handle loadBundledPlugins = add("Loading bundled plugins", new Executable() {
                    // public void run(Reactor session) throws Exception {
                    // bundledPlugins = loadBundledPlugins();
                    // }
                    // });
                    Handle listUpPlugins = add("Listing up plugins", new Executable() {

                        public void run(Reactor session) throws Exception {
                            archives = initStrategy.listPluginArchives(PluginManager.this);
                            if (CollectionUtils.isEmpty(archives)) {
                                throw new IllegalStateException("have not found any plugin in :" + PluginManager.this.rootDir.getAbsolutePath());
                            }
                        }
                    });
                    requires(listUpPlugins).attains(PLUGINS_LISTED).add("Preparing plugins", new Executable() {

                        public void run(Reactor session) throws Exception {
                            // once we've listed plugins, we can fill in the reactor with plugin-specific initialization tasks
                            TaskGraphBuilder g = new TaskGraphBuilder();
                            final Map<String, File> inspectedShortNames = new HashMap<String, File>();
                            for (final File arc : archives) {
                                g.followedBy().notFatal().attains(PLUGINS_LISTED).add("Inspecting plugin " + arc, new Executable() {

                                    public void run(Reactor session1) throws Exception {
                                        try {
                                            PluginWrapper p = strategy.createPluginWrapper(arc);
                                            if (isDuplicate(p))
                                                return;
                                            p.isBundled = containsHpiJpi(bundledPlugins, arc.getName());
                                            plugins.add(p);
                                        } catch (IOException e) {
                                            failedPlugins.add(new FailedPlugin(arc.getName(), e));
                                            throw e;
                                        }
                                    }

                                    /**
                                     * Inspects duplication. this happens when you run hpi:run on a bundled plugin,
                                     * as well as putting numbered jpi files, like "cobertura-1.0.jpi" and "cobertura-1.1.jpi"
                                     */
                                    private boolean isDuplicate(PluginWrapper p) {
                                        String shortName = p.getShortName();
                                        if (inspectedShortNames.containsKey(shortName)) {
                                            LOGGER.info("Ignoring " + arc + " because " + inspectedShortNames.get(shortName) + " is already loaded");
                                            return true;
                                        }
                                        inspectedShortNames.put(shortName, arc);
                                        return false;
                                    }
                                });
                            }
                            g.followedBy().attains(PLUGINS_LISTED).add("Checking cyclic dependencies", new Executable() {

                                /**
                                 * Makes sure there's no cycle in dependencies.
                                 */
                                public void run(Reactor reactor) throws Exception {
                                    try {
                                        CyclicGraphDetector<PluginWrapper> cgd = new CyclicGraphDetector<PluginWrapper>() {

                                            @Override
                                            protected List<PluginWrapper> getEdges(PluginWrapper p) {
                                                List<PluginWrapper> next = new ArrayList<PluginWrapper>();
                                                addTo(p, p.getDependencies(), next);
                                                addTo(p, p.getOptionalDependencies(), next);
                                                return next;
                                            }

                                            private void addTo(PluginWrapper plugin, List<PluginWrapper.Dependency> dependencies, List<PluginWrapper> r) {

                                                ITPIArtifact.matchDependency(PluginManager.this, dependencies, plugin, (p) -> {
                                                    r.add(p.getLeft());
                                                });

//                                                Optional<PluginClassifier> classifier = plugin.getClassifier();
//                                                ITPIArtifactMatch match = ITPIArtifact.match(classifier);
//                                                for (PluginWrapper.Dependency d : dependencies) {
//                                                    match.setIdentityName(d.shortName);
//                                                    PluginWrapper p = getPlugin(match);
//                                                    if (p != null) {
//                                                        r.add(p);
//                                                    }
//                                                }
                                            }

                                            @Override
                                            protected void reactOnCycle(PluginWrapper q, List<PluginWrapper> cycle) throws CyclicGraphDetector.CycleDetectedException {
                                                LOGGER.info("found cycle in plugin dependencies: (root=" + q + ", deactivating all involved) " + Util.join(cycle, " -> "));
                                                for (PluginWrapper pluginWrapper : cycle) {
                                                    pluginWrapper.setHasCycleDependency(true);
                                                    failedPlugins.add(new FailedPlugin(pluginWrapper.getShortName(), new CycleDetectedException(cycle)));
                                                }
                                            }
                                        };
                                        cgd.run(getPlugins());
                                        // obtain topologically sorted list and overwrite the list
                                        ListIterator<PluginWrapper> litr = getPlugins().listIterator();
                                        for (PluginWrapper p : cgd.getSorted()) {
                                            litr.next();
                                            litr.set(p);
                                            if (p.isActive()) {
                                                activePlugins.add(p);
                                            }
                                        }
                                    } catch (CyclicGraphDetector.CycleDetectedException e) {
                                        // disable all plugins since classloading from them can lead to StackOverflow
                                        stop();
                                        // let Hudson fail
                                        throw e;
                                    }
                                }
                            });
                            // Let's see for a while until we open this functionality up to plugins
                            // g.followedBy().attains(PLUGINS_LISTED).add("Load compatibility rules", new Executable() {
                            // public void run(Reactor reactor) throws Exception {
                            // compatibilityTransformer.loadRules(uberClassLoader);
                            // }
                            // });
                            session.addAll(g.discoverTasks(session));
                            // technically speaking this is still too early, as at this point tasks are merely scheduled, not necessarily executed.
                            pluginListed = true;
                        }
                    });
                }
            };
        } else {
            builder = TaskBuilder.EMPTY_BUILDER;
        }
        // lists up initialization tasks about loading plugins.
        return TaskBuilder.union(builder, new TaskGraphBuilder() {

            {
                requires(PLUGINS_LISTED).attains(PLUGINS_PREPARED).add("Loading plugins", new Executable() {

                    /**
                     * Once the plugins are listed, schedule their initialization.
                     */
                    public void run(Reactor session) throws Exception {
                        // Jenkins.getInstance().lookup.set(PluginInstanceStore.class, new PluginInstanceStore());
                        TaskGraphBuilder g = new TaskGraphBuilder();
                        // schedule execution of loading plugins
                        for (final PluginWrapper pluginWrapper : activePlugins.toArray(new PluginWrapper[activePlugins.size()])) {
                            g.followedBy().notFatal().attains(PLUGINS_PREPARED).add("Loading plugin " + pluginWrapper.getShortName(), new Executable() {

                                public void run(Reactor session) throws Exception {
                                    try {
                                        pluginWrapper.resolvePluginDependencies();
                                        strategy.load(pluginWrapper);
                                    } catch (MissingDependencyException e) {
                                        failedPlugins.add(new FailedPlugin(pluginWrapper.getShortName(), e));
                                        activePlugins.remove(pluginWrapper);
                                        plugins.remove(pluginWrapper);
                                        LOGGER.error("Failed to install {}: {}", pluginWrapper.getShortName(), e.getMessage());
                                        return;
                                    } catch (IOException e) {
                                        failedPlugins.add(new FailedPlugin(pluginWrapper.getShortName(), e));
                                        activePlugins.remove(pluginWrapper);
                                        plugins.remove(pluginWrapper);
                                        throw e;
                                    }
                                }
                            });
                        }
                        // schedule execution of initializing plugins
                        for (final PluginWrapper p : activePlugins.toArray(new PluginWrapper[activePlugins.size()])) {
                            g.followedBy().notFatal().attains(PLUGINS_STARTED).add("Initializing plugin " + p.getShortName(), new Executable() {

                                public void run(Reactor session) throws Exception {
                                    if (!activePlugins.contains(p)) {
                                        return;
                                    }
                                    try {
                                        p.getPlugin().postInitialize();
                                    } catch (Exception e) {
                                        failedPlugins.add(new FailedPlugin(p.getShortName(), e));
                                        activePlugins.remove(p);
                                        plugins.remove(p);
                                        throw e;
                                    }
                                }
                            });
                        }

                        if (CenterResource.notFetchFromCenterRepository()) {
                            g.followedBy().notFatal().attains(PLUGINS_STARTED).add("Load updateCenter", (reactor) -> {
                                UpdateCenter updateCenter = tis.getUpdateCenter();
                                updateCenter.load();
                                updateCenter.updateAllSites();
                            });
                        }


                        session.addAll(g.discoverTasks(session));
                    }
                });
                // All plugins are loaded. Now we can figure out who depends on who.
                requires(PLUGINS_PREPARED).attains(COMPLETED).add("Resolving Dependant Plugins Graph", new Executable() {

                    @Override
                    public void run(Reactor reactor) throws Exception {
                        resolveDependantPlugins();
                    }
                });
            }
        });
    }

    public synchronized void resolveDependantPlugins() {
        for (PluginWrapper plugin : plugins) {
            Set<String> dependants = new HashSet<>();
            for (PluginWrapper possibleDependant : plugins) {
                // count as a dependant.
                if (possibleDependant.isDeleted()) {
                    continue;
                }
                List<PluginWrapper.Dependency> dependencies = possibleDependant.getDependencies();
                for (PluginWrapper.Dependency dependency : dependencies) {
                    if (dependency.shortName.equals(plugin.getShortName())) {
                        dependants.add(possibleDependant.getShortName());
                    }
                }
            }
            plugin.setDependents(dependants);
        }
    }

    /**
     * Orderly terminates all the plugins.
     */
    public void stop() {
        for (PluginWrapper p : activePlugins) {
            p.stop();
            p.releaseClassLoader();
        }
        activePlugins.clear();
        // Work around a bug in commons-logging.
        // See http://www.szegedi.org/articles/memleak.html
        //LogFactory.release(uberClassLoader);
    }

    protected Collection<String> loadBundledPlugins() throws Exception {
        try {
            return loadPluginsFromWar("/WEB-INF/plugins", null);
        } finally {
            loadDetachedPlugins();
        }
    }

    /**
     * Stores {@link Plugin} instances.
     */
    static final class PluginInstanceStore {

        final Map<PluginWrapper, Plugin> store = new Hashtable<PluginWrapper, Plugin>();
    }

    protected void loadDetachedPlugins() {
    }

    protected Set<String> loadPluginsFromWar(String fromPath, FilenameFilter filter) {
        Set<String> names = new HashSet();
        return names;
    }

    /**
     * Return the {@link PluginWrapper} that loaded the given class 'c'.
     *
     * @since 1.402.
     */
    public PluginWrapper whichPlugin(Class c) {
        PluginWrapper oneAndOnly = null;
        ClassLoader cl = c.getClassLoader();
        for (PluginWrapper p : activePlugins) {
            if (p.classLoader == cl) {
                if (oneAndOnly != null)
                    // ambigious
                    return null;
                oneAndOnly = p;
            }
        }
        return oneAndOnly;
    }

    /*
     * contains operation that considers xxx.hpi and xxx.jpi as equal
     * this is necessary since the bundled plugins are still called *.hpi
     */
    private boolean containsHpiJpi(Collection<String> bundledPlugins, String name) {
        return bundledPlugins.contains(name.replaceAll("\\.hpi", PACAKGE_TPI_EXTENSION)) || bundledPlugins.contains(name.replaceAll("\\" + PACAKGE_TPI_EXTENSION, ".hpi"));
    }

    /**
     * Get the plugin instance with the given short name.
     *
     * @param tpi the short name of the plugin
     * @return The plugin singleton or <code>null</code> if a plugin with the given short name does not exist.
     */
    public PluginWrapper getPlugin(ITPIArtifactMatch tpi) {
        for (PluginWrapper p : getPlugins()) {
            if (ITPIArtifact.isEquals(p, tpi)) {
                return p;
            }
        }
        return null;
    }

    public List<PluginWrapper> getPlugins() {
        List<PluginWrapper> out = new ArrayList<PluginWrapper>(plugins.size());
        out.addAll(plugins);
        return out;
    }

    /**
     * Remembers why a plugin failed to deploy.
     */
    public static final class FailedPlugin {

        public final String name;

        public final Exception cause;

        public FailedPlugin(String name, Exception cause) {
            this.name = name;
            this.cause = cause;
        }

        public String getExceptionString() {
            return ExceptionUtils.getFullStackTrace(cause);
        }
    }

    // !SystemProperties.getBoolean(PluginManager.class.getName()+".noFastLookup");
    public static final boolean FAST_LOOKUP = true;

}
