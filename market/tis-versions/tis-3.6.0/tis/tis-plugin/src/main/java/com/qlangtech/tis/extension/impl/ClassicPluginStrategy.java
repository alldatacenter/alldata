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
import com.qlangtech.tis.TIS;
import com.qlangtech.tis.extension.*;
import com.qlangtech.tis.extension.util.AntClassLoader;
import com.qlangtech.tis.extension.util.ClassLoaderReflectionToolkit;
import com.qlangtech.tis.extension.util.CyclicGraphDetector;
import com.qlangtech.tis.util.Util;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class ClassicPluginStrategy implements PluginStrategy {
    public static final List<ExtensionFinder> finders = Collections.singletonList(new ExtensionFinder.Sezpoz());

    public static void removeByClassNameInFinders(Class<?> superType) {
        finders.forEach((finder) -> {
            finder.removeByType(superType);
        });
    }


    private PluginManager pluginManager;

    /**
     * All the plugins eventually delegate this classloader to load core, servlet APIs, and SE runtime.
     */
    private final MaskingClassLoader coreClassLoader = new MaskingClassLoader(getClass().getClassLoader());

    public ClassicPluginStrategy(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    @Override
    public String getShortName(File archive) throws IOException {
        Manifest manifest;
        if (PluginManifest.isLinked(archive)) {
            manifest = PluginManifest.loadLinkedManifest(archive);
        } else {
            try (JarInputStream jf = new JarInputStream(FileUtils.openInputStream(archive), false)) {
                manifest = jf.getManifest();
            }
//            JarFile jf = new JarFile(archive, false);
//            try {
//                manifest = jf.getManifest();
//            } finally {
//                jf.close();
//            }
        }
        Attributes attrs = manifest.getMainAttributes();
        if (attrs == null) {
            throw new IllegalStateException("archive:"
                    + archive.getAbsolutePath() + " relevant mainifest attrs can not be null");
        }
        return PluginManifest.parseShortName(attrs, archive.getName());
    }


    @Override
    public PluginWrapper createPluginWrapper(File archive) throws IOException {

        PluginManifest manifest = PluginManifest.create(pluginManager.getWorkDir(), archive);

//       // final Manifest manifest;
//        URL baseResourceURL = null;
//        File expandDir = null;
//        // if .hpi, this is the directory where war is expanded
//        boolean isLinked = isLinked(archive);
//        if (isLinked) {
//            manifest = loadLinkedManifest(archive);
//        } else {
//            if (archive.isDirectory()) {
//                // already expanded
//                expandDir = archive;
//            } else {
//                File f = pluginManager.getWorkDir();
//                expandDir = new File(f == null ? archive.getParentFile() : f, getBaseName(archive.getName()));
//                explode(archive, expandDir);
//            }
//            File manifestFile = new File(expandDir, PluginWrapper.MANIFEST_FILENAME);
//            if (!manifestFile.exists()) {
//                throw new IOException("Plugin installation failed. No manifest at " + manifestFile);
//            }
//            FileInputStream fin = new FileInputStream(manifestFile);
//            try {
//                manifest = new Manifest(fin);
//            } finally {
//                fin.close();
//            }
//        }
//        final Attributes atts = manifest.getMainAttributes();
//        // TODO: define a mechanism to hide classes
//        // String export = manifest.getMainAttributes().getValue("Export");
//        List<File> paths = new ArrayList<File>();
//        if (isLinked) {
//            parseClassPath(manifest, archive, paths, "Libraries", ",");
//            // backward compatibility
//            parseClassPath(manifest, archive, paths, "Class-Path", " +");
//            baseResourceURL = resolve(archive, atts.getValue("Resource-Path")).toURI().toURL();
//        } else {
//            File classes = new File(expandDir, "WEB-INF/classes");
//            if (classes.exists())
//                paths.add(classes);
//            File lib = new File(expandDir, "WEB-INF/lib");
//            File[] libs = lib.listFiles(JAR_FILTER);
//            if (libs != null)
//                paths.addAll(Arrays.asList(libs));
//            baseResourceURL = expandDir.toPath().toUri().toURL();
//        }
        File disableFile = new File(archive.getPath() + ".disabled");
        if (disableFile.exists()) {
            LOGGER.info("Plugin " + archive.getName() + " is disabled");
        }
        // compute dependencies
//        List<PluginWrapper.Dependency> dependencies = new ArrayList<PluginWrapper.Dependency>();
//        List<PluginWrapper.Dependency> optionalDependencies = new ArrayList<PluginWrapper.Dependency>();
        DependencyMeta dependencyMeta = manifest.getDependencyMeta();
        //  fix(atts, dependencyMeta.optionalDependencies);
        // Register global classpath mask. This is useful for hiding JavaEE APIs that you might see from the container,
        // such as database plugin for JPA support. The Mask-Classes attribute is insufficient because those classes
        // also need to be masked by all the other plugins that depend on the database plugin.
        String masked = manifest.getMasked();//atts.getValue("Global-Mask-Classes");
        if (masked != null) {
            for (String pkg : masked.trim().split("[ \t\r\n]+")) { coreClassLoader.add(pkg);}
        }
        ClassLoader dependencyLoader = new DependencyClassLoader(
                coreClassLoader, archive, manifest, Util.join(dependencyMeta.dependencies, dependencyMeta.optionalDependencies));
        dependencyLoader = getBaseClassLoader(manifest, dependencyLoader);
        return new PluginWrapper(pluginManager, archive, manifest, manifest.baseResourceURL
                , createClassLoader(manifest, dependencyLoader)
                , disableFile, dependencyMeta.dependencies, dependencyMeta.optionalDependencies);
    }

//    public static DependencyMeta getDependencyMeta(Attributes atts) {
//        DependencyMeta dependencyMeta = new DependencyMeta();
//        String v = atts.getValue(KEY_MANIFEST_DEPENDENCIES);
//        if (v != null) {
//            for (String s : v.split(",")) {
//                PluginWrapper.Dependency d = PluginWrapper.Dependency.parse(s);
//                if (d.optional) {
//                    dependencyMeta.optionalDependencies.add(d);
//                } else {
//                    dependencyMeta.dependencies.add(d);
//                }
//            }
//        }
//        return dependencyMeta;
//    }

    public static class DependencyMeta {
        public List<PluginWrapper.Dependency> dependencies = new ArrayList<PluginWrapper.Dependency>();
        List<PluginWrapper.Dependency> optionalDependencies = new ArrayList<PluginWrapper.Dependency>();
    }

    private static void fix(Attributes atts, List<PluginWrapper.Dependency> optionalDependencies) {
        String pluginName = atts.getValue("Short-Name");
        String jenkinsVersion = atts.getValue("Jenkins-Version");
        if (jenkinsVersion == null)
            jenkinsVersion = atts.getValue("Hudson-Version");
        // optionalDependencies.addAll(getImpliedDependencies(pluginName, jenkinsVersion));
    }

//    @Deprecated
//    protected ClassLoader createClassLoader(PluginManifest manifest, ClassLoader parent) throws IOException {
//        return createClassLoader(paths, parent, null);
//    }

    /**
     * Creates the classloader that can load all the specified jar files and delegate to the given parent.
     */
    protected ClassLoader createClassLoader(PluginManifest manifest, ClassLoader parent) throws IOException {
        List<File> paths = manifest.getLibs();
        // Attributes attrs = manifest.getAttrs();
        // if (attrs != null) {
        // String usePluginFirstClassLoader =;// attrs.getValue(KEY_MANIFEST_PLUGIN_FIRST_CLASSLOADER);
        if (manifest.getUsePluginFirstClassLoader()) {
            PluginFirstClassLoader classLoader = new PluginFirstClassLoader();
            classLoader.setParentFirst(false);
            classLoader.setParent(parent);
            classLoader.addPathFiles(paths);
            return classLoader;
        }
        // }
        AntClassLoader2 classLoader = new AntClassLoader2(parent);
        classLoader.addPathFiles(paths);
        return classLoader;
    }

    /**
     * Implicit dependencies that are known to be unnecessary and which must be cut out to prevent a dependency cycle among bundled plugins.
     */
    private static final Set<String> BREAK_CYCLES = new HashSet<String>(Arrays.asList("script-security/matrix-auth", "script-security/windows-slaves", "script-security/antisamy-markup-formatter", "script-security/matrix-project", "credentials/matrix-auth", "credentials/windows-slaves"));

    /**
     * Computes the classloader that takes the class masking into account.
     *
     * <p>
     * This mechanism allows plugins to have their own versions for libraries that core bundles.
     */
    private ClassLoader getBaseClassLoader(PluginManifest manifest, ClassLoader base) {
        String masked = manifest.getMaskedClasses();// atts.getValue("Mask-Classes");
        if (masked != null)
            base = new MaskingClassLoader(base, masked.trim().split("[ \t\r\n]+"));
        return base;
    }

    public void initializeComponents(PluginWrapper plugin) {
    }


    public <T> List<ExtensionComponent<T>> findComponents(final Class<T> type, TIS tis) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Scout-loading ExtensionList: " + type);
        }
        for (ExtensionFinder finder : finders) {

            finder.scout(type, tis);
        }
        List<ExtensionComponent<T>> r = Lists.newArrayList();
        for (ExtensionFinder finder : finders) {
            try {
                r.addAll(finder.find(type, tis));
            } catch (AbstractMethodError e) {
                // backward compatibility
                for (T t : finder.findExtensions(type, tis)) r.add(new ExtensionComponent<T>(t));
            }
        }
        return r;
    }

    public void load(PluginWrapper wrapper) throws IOException {
        // override the context classloader. This no longer makes sense,
        // but it is left for the backward compatibility
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(wrapper.classLoader);
        try {
            String className = wrapper.getPluginClass();
            if (className == null) {
                // use the default dummy instance
                wrapper.setPlugin(new Plugin.DummyImpl());
            } else {
                try {
                    Class<?> clazz = wrapper.classLoader.loadClass(className);
                    Object o = clazz.newInstance();
                    if (!(o instanceof Plugin)) {
                        throw new IOException(className + " doesn't extend from hudson.Plugin");
                    }
                    wrapper.setPlugin((Plugin) o);
                } catch (LinkageError | ClassNotFoundException e) {
                    throw new IOException("Unable to load " + className + " from " + wrapper.getShortName(), e);
                } catch (IllegalAccessException | InstantiationException e) {
                    throw new IOException("Unable to create instance of " + className + " from " + wrapper.getShortName(), e);
                }
            }
            // initialize plugin
            try {
                Plugin plugin = wrapper.getPlugin();
                // plugin.setServletContext(pluginManager.context);
                startPlugin(wrapper);
            } catch (Throwable t) {
                // gracefully handle any error in plugin.
                throw new IOException("Failed to initialize", t);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
    }

    public void startPlugin(PluginWrapper plugin) throws Exception {
        plugin.getPlugin().start();
    }

    @Override
    public void updateDependency(PluginWrapper depender, PluginWrapper dependee) {
        DependencyClassLoader classLoader = findAncestorDependencyClassLoader(depender.classLoader);
        if (classLoader != null) {
            classLoader.updateTransientDependencies();
            LOGGER.info("Updated dependency of {}", depender.getShortName());
        }
    }

    private DependencyClassLoader findAncestorDependencyClassLoader(ClassLoader classLoader) {
        for (; classLoader != null; classLoader = classLoader.getParent()) {
            if (classLoader instanceof DependencyClassLoader) {
                return (DependencyClassLoader) classLoader;
            }
            if (classLoader instanceof AntClassLoader) {
                // AntClassLoaders hold parents not only as AntClassLoader#getParent()
                // but also as AntClassLoader#getConfiguredParent()
                DependencyClassLoader ret = findAncestorDependencyClassLoader(((AntClassLoader) classLoader).getConfiguredParent());
                if (ret != null) {
                    return ret;
                }
            }
        }
        return null;
    }


    /**
     * Used to load classes from dependency plugins.
     */
    final class DependencyClassLoader extends ClassLoader {

        /**
         * This classloader is created for this plugin. Useful during debugging.
         */
        private final File _for;

        private List<PluginWrapper.Dependency> dependencies;
        private final PluginManifest manifest;

        /**
         * Topologically sorted list of transient dependencies.
         */
        private volatile List<PluginWrapper> transientDependencies;

        public DependencyClassLoader(ClassLoader parent, File archive, PluginManifest manifest
                , List<PluginWrapper.Dependency> dependencies) {
            super(parent);
            this._for = archive;
            this.dependencies = dependencies;
            this.manifest = manifest;
        }

        private void updateTransientDependencies() {
            // This will be recalculated at the next time.
            transientDependencies = null;
        }

        private List<PluginWrapper> getTransitiveDependencies() {
            if (transientDependencies == null) {
                CyclicGraphDetector<PluginWrapper> cgd = new CyclicGraphDetector<PluginWrapper>() {
                    @Override
                    protected List<PluginWrapper> getEdges(PluginWrapper pw) {
                        List<PluginWrapper> dep = new ArrayList<PluginWrapper>();
                        ITPIArtifact.matchDependency(pluginManager, pw.getDependencies(), pw, (pair) -> {
                            PluginWrapper p = pair.getLeft();
                            if (p.isActive()) {
                                dep.add(p);
                            }
                        });


//                        ITPIArtifactMatch match = ITPIArtifact.match(pw.getClassifier());
//
//                        for (PluginWrapper.Dependency d : pw.getDependencies()) {
//                            match.setIdentityName(d.shortName);
//                            PluginWrapper p = pluginManager.getPlugin(match);
//                            if (p != null && p.isActive()) {
//                                dep.add(p);
//                            }
//                        }
                        return dep;
                    }
                };
                // try {
                String requiredFrom = manifest.computeShortName(StringUtils.EMPTY);// PluginManifest.computeShortName(this.manifest, StringUtils.EMPTY);
                ITPIArtifact.matchDependency(pluginManager, dependencies, requiredFrom
                        , this.manifest.parseClassifier(), (pair) -> {
                            try {
                                PluginWrapper p = pair.getLeft();
                                if (p.isActive()) {
                                    cgd.run(Collections.singleton(p));
                                }
                            } catch (CyclicGraphDetector.CycleDetectedException e) {
                                throw new AssertionError(e);
                            }
                        });

//                    ITPIArtifactMatch match = ITPIArtifact.match(PluginWrapper.parseClassifier(this.manifest));
//                    for (PluginWrapper.Dependency d : dependencies) {
//                        match.setIdentityName(d.shortName);
//                        PluginWrapper p = pluginManager.getPlugin(match);
//                        if (p != null && p.isActive()) {
//                            cgd.run(Collections.singleton(p));
//                        }
//                    }
//                } catch (CyclicGraphDetector.CycleDetectedException e) {
//                    // such error should have been reported earlier
//                    throw new AssertionError(e);
//                }
                transientDependencies = cgd.getSorted();
            }
            return transientDependencies;
        }

        // public List<PluginWrapper> getDependencyPluginWrappers() {
        // List<PluginWrapper> r = new ArrayList<PluginWrapper>();
        // for (Dependency d : dependencies) {
        // PluginWrapper w = pluginManager.getPlugin(d.shortName);
        // if (w!=null)    r.add(w);
        // }
        // return r;
        // }
        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            if (PluginManager.FAST_LOOKUP) {
                for (PluginWrapper pw : getTransitiveDependencies()) {
                    try {
                        Class<?> c = ClassLoaderReflectionToolkit._findLoadedClass(pw.classLoader, name);
                        if (c != null)
                            return c;
                        return ClassLoaderReflectionToolkit._findClass(pw.classLoader, name);
                    } catch (ClassNotFoundException e) {
                        // not found. try next
                    }
                }
            } else {
//                for (PluginWrapper.Dependency dep : dependencies) {
//                    PluginWrapper p = pluginManager.getPlugin(dep);
//                    if (p != null)
//                        try {
//                            return p.classLoader.loadClass(name);
//                        } catch (ClassNotFoundException _) {
//                            // try next
//                        }
//                }
            }

            throw new ClassNotFoundException("by " + this._for.getName() + ",for:" + name);
        }

        @Override
        protected Enumeration<URL> findResources(String name) throws IOException {
            HashSet<URL> result = new HashSet<URL>();
            if (PluginManager.FAST_LOOKUP) {
                for (PluginWrapper pw : getTransitiveDependencies()) {
                    Enumeration<URL> urls = ClassLoaderReflectionToolkit._findResources(pw.classLoader, name);
                    while (urls != null && urls.hasMoreElements()) result.add(urls.nextElement());
                }
            } else {
//                for (PluginWrapper.Dependency dep : dependencies) {
//                    PluginWrapper p = pluginManager.getPlugin(dep);
//                    if (p != null) {
//                        Enumeration<URL> urls = p.classLoader.getResources(name);
//                        while (urls != null && urls.hasMoreElements()) result.add(urls.nextElement());
//                    }
//                }
            }
            return Collections.enumeration(result);
        }

        @Override
        protected URL findResource(String name) {
            if (PluginManager.FAST_LOOKUP) {
                for (PluginWrapper pw : getTransitiveDependencies()) {
                    URL url = ClassLoaderReflectionToolkit._findResource(pw.classLoader, name);
                    if (url != null)
                        return url;
                }
            } else {
//                for (PluginWrapper.Dependency dep : dependencies) {
//                    PluginWrapper p = pluginManager.getPlugin(dep);
//                    if (p != null) {
//                        URL url = p.classLoader.getResource(name);
//                        if (url != null)
//                            return url;
//                    }
//                }
            }
            return null;
        }
    }

    private final class AntClassLoader2 extends AntClassLoader implements Closeable {

        private final Vector pathComponents;

        private AntClassLoader2(ClassLoader parent) {
            super(parent, true);
            try {
                Field $pathComponents = AntClassLoader.class.getDeclaredField("pathComponents");
                $pathComponents.setAccessible(true);
                pathComponents = (Vector) $pathComponents.get(this);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new Error(e);
            }
        }

        public void addPathFiles(Collection<File> paths) throws IOException {
            for (File f : paths) addPathFile(f);
        }

        public void close() throws IOException {
            cleanup();
        }

        /**
         * As of 1.8.0, {@link AntClassLoader} doesn't implement {@link #findResource(String)}
         * in any meaningful way, which breaks fast lookup. Implement it properly.
         */
        @Override
        protected URL findResource(String name) {
            URL url = null;
            // try and load from this loader if the parent either didn't find
            // it or wasn't consulted.
            Enumeration e = pathComponents.elements();
            while (e.hasMoreElements() && url == null) {
                File pathComponent = (File) e.nextElement();
                url = getResourceURL(pathComponent, name);
                if (url != null) {
                    log("Resource " + name + " loaded from ant loader", Project.MSG_DEBUG);
                }
            }
            return url;
        }

        @Override
        protected Class defineClassFromData(File container, byte[] classData, String classname) throws IOException {
            // classData = pluginManager.getCompatibilityTransformer().transform(classname, classData, this);
            return super.defineClassFromData(container, classData, classname);
        }
    }

    // public static boolean useAntClassLoader = SystemProperties.getBoolean(ClassicPluginStrategy.class.getName() + ".useAntClassLoader");
    private static final Logger LOGGER = LoggerFactory.getLogger(ClassicPluginStrategy.class.getName());
    // public static boolean DISABLE_TRANSFORMER = SystemProperties.getBoolean(ClassicPluginStrategy.class.getName() + ".noBytecodeTransformer");
}
