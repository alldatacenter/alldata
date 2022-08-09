/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.plugin.classloader;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Enumeration;

/**
 * AtlasPluginClassLoader to use plugin classpath first, before component classpath.
 */
public final class AtlasPluginClassLoader extends URLClassLoader {
    private static final Logger LOG = LoggerFactory.getLogger(AtlasPluginClassLoader.class);

    private static volatile AtlasPluginClassLoader me = null;

    private final ThreadLocal<ClassLoader> preActivateClassLoader = new ThreadLocal<>();

    private final MyClassLoader componentClassLoader;

    private AtlasPluginClassLoader(String pluginType, Class<?> pluginClass) throws URISyntaxException {
        this(AtlasPluginClassLoaderUtil.getPluginImplLibPath(pluginType, pluginClass));
    }

    //visible for testing
    AtlasPluginClassLoader(String libraryPath) {
        super(AtlasPluginClassLoaderUtil.getFilesInDirectories(new String[]{libraryPath}), null);

        componentClassLoader = AccessController.doPrivileged(new PrivilegedAction<MyClassLoader>() {
            public MyClassLoader run() {
                return new MyClassLoader(Thread.currentThread().getContextClassLoader());
            }
        });
    }

    public static AtlasPluginClassLoader getInstance(final String pluginType, final Class<?> pluginClass) throws PrivilegedActionException {
        AtlasPluginClassLoader ret = me;
        if (ret == null) {
            synchronized (AtlasPluginClassLoader.class) {
                ret = me;
                if (ret == null) {
					me = AccessController.doPrivileged(new PrivilegedExceptionAction<AtlasPluginClassLoader>() {
					    public AtlasPluginClassLoader run() throws URISyntaxException {
					        return new AtlasPluginClassLoader(pluginType, pluginClass);
					    }
					});
                    ret = me;
                }
            }
        }
        return ret;
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        if (LOG.isTraceEnabled()) {
            LOG.trace("==> AtlasPluginClassLoader.findClass({})", name);
        }

        Class<?> ret = null;

        try {
            // first try to find the class in pluginClassloader
            if (LOG.isTraceEnabled()) {
                LOG.trace("AtlasPluginClassLoader.findClass({}): calling pluginClassLoader.findClass()", name);
            }

            ret = super.findClass(name);
        } catch (Throwable e) {
            // on failure to find in pluginClassLoader, try to find in componentClassLoader
            MyClassLoader savedClassLoader = getComponentClassLoader();

            if (savedClassLoader != null) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("AtlasPluginClassLoader.findClass({}): calling componentClassLoader.findClass()", name);
                }

                ret = savedClassLoader.findClass(name);
            }
        }

        if (LOG.isTraceEnabled()) {
            LOG.trace("<== AtlasPluginClassLoader.findClass({}): {}", name, ret);
        }

        return ret;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        if (LOG.isTraceEnabled()) {
            LOG.trace("==> AtlasPluginClassLoader.loadClass({})", name);
        }

        Class<?> ret = null;

        try {
            // first try to load the class from pluginClassloader
            if (LOG.isTraceEnabled()) {
                LOG.trace("AtlasPluginClassLoader.loadClass({}): calling pluginClassLoader.loadClass()", name);
            }

            ret = super.loadClass(name);
        } catch (Throwable e) {
            // on failure to load from pluginClassLoader, try to load from componentClassLoader
            MyClassLoader savedClassLoader = getComponentClassLoader();

            if (savedClassLoader != null) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("AtlasPluginClassLoader.loadClass({}): calling componentClassLoader.loadClass()", name);
                }

                ret = savedClassLoader.loadClass(name);
            }
        }

        if (LOG.isTraceEnabled()) {
            LOG.trace("<== AtlasPluginClassLoader.loadClass({}): {}", name, ret);
        }

        return ret;
    }

    @Override
    public URL findResource(String name) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasPluginClassLoader.findResource({}) ", name);
        }

        // first try to find the resource from pluginClassloader
        if (LOG.isDebugEnabled()) {
            LOG.debug("AtlasPluginClassLoader.findResource({}): calling pluginClassLoader.findResource()", name);
        }

        URL ret = super.findResource(name);

        if (ret == null) {
            MyClassLoader savedClassLoader = getComponentClassLoader();

            if (savedClassLoader != null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("AtlasPluginClassLoader.findResource({}): calling componentClassLoader.getResource()", name);
                }

                ret = savedClassLoader.getResource(name);
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasPluginClassLoader.findResource({}): {}", name, ret);
        }

        return ret;
    }

    @Override
    public Enumeration<URL> findResources(String name) throws IOException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasPluginClassLoader.findResources({})", name);
        }

        Enumeration<URL> ret = null;

        Enumeration<URL> resourcesInPluginClsLoader = findResourcesUsingPluginClassLoader(name);
        Enumeration<URL> resourcesInComponentClsLoader = findResourcesUsingComponentClassLoader(name);

        if (resourcesInPluginClsLoader != null && resourcesInComponentClsLoader != null) {
            ret = new MergeEnumeration(resourcesInPluginClsLoader, resourcesInComponentClsLoader);
        } else if (resourcesInPluginClsLoader != null) {
            ret = resourcesInPluginClsLoader;
        } else {
            ret = resourcesInComponentClsLoader;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasPluginClassLoader.findResources({}): {}", name, ret);
        }

        return ret;
    }

    public void activate() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasPluginClassLoader.activate()");
        }

        preActivateClassLoader.set(Thread.currentThread().getContextClassLoader());

        Thread.currentThread().setContextClassLoader(this);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasPluginClassLoader.activate()");
        }
    }

    public void deactivate() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasPluginClassLoader.deactivate()");
        }

        ClassLoader classLoader = preActivateClassLoader.get();

        if (classLoader != null) {
            preActivateClassLoader.remove();
        } else {
            MyClassLoader savedClassLoader = getComponentClassLoader();
            if (savedClassLoader != null && savedClassLoader.getParent() != null) {
                classLoader = savedClassLoader.getParent();
            }
        }

        if (classLoader != null) {
            Thread.currentThread().setContextClassLoader(classLoader);
        } else {
            LOG.warn("AtlasPluginClassLoader.deactivate() was not successful.Couldn't not get the saved "
                    + "componentClassLoader...");
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasPluginClassLoader.deactivate()");
        }
    }

    private MyClassLoader getComponentClassLoader() {
        return componentClassLoader;
    }

    private Enumeration<URL> findResourcesUsingPluginClassLoader(String name) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasPluginClassLoader.findResourcesUsingPluginClassLoader({})", name);
        }

        Enumeration<URL> ret = null;

        try {
            ret = super.findResources(name);
        } catch (Throwable excp) {
            // Ignore exceptions
            if (LOG.isDebugEnabled()) {
                LOG.debug("AtlasPluginClassLoader.findResourcesUsingPluginClassLoader({}): resource not found in plugin", name, excp);
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasPluginClassLoader.findResourcesUsingPluginClassLoader({}): {}", name, ret);
        }

        return ret;
    }

    private Enumeration<URL> findResourcesUsingComponentClassLoader(String name) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasPluginClassLoader.findResourcesUsingComponentClassLoader({})", name);
        }

        Enumeration<URL> ret = null;

        try {
            MyClassLoader savedClassLoader = getComponentClassLoader();

            if (savedClassLoader != null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("AtlasPluginClassLoader.findResourcesUsingComponentClassLoader({}): calling componentClassLoader.getResources()", name);
                }

                ret = savedClassLoader.getResources(name);
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("<== AtlasPluginClassLoader.findResourcesUsingComponentClassLoader({}): {}", name, ret);
            }
        } catch (Throwable t) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("AtlasPluginClassLoader.findResourcesUsingComponentClassLoader({}): class not found in componentClassLoader.", name, t);
            }
        }

        return ret;
    }

    static class MergeEnumeration implements Enumeration<URL> { //NOPMD
        private Enumeration<URL> e1 = null;
        private Enumeration<URL> e2 = null;

        public MergeEnumeration(Enumeration<URL> e1, Enumeration<URL> e2) {
            this.e1 = e1;
            this.e2 = e2;
        }

        @Override
        public boolean hasMoreElements() {
            return ((e1 != null && e1.hasMoreElements()) || (e2 != null && e2.hasMoreElements()));
        }

        @Override
        public URL nextElement() {
            URL ret = null;

            if (e1 != null && e1.hasMoreElements()) {
                ret = e1.nextElement();
            } else if (e2 != null && e2.hasMoreElements()) {
                ret = e2.nextElement();
            }

            return ret;
        }
    }

    static class MyClassLoader extends ClassLoader {
        public MyClassLoader(ClassLoader realClassLoader) {
            super(realClassLoader);
        }

        @Override
        public Class<?> findClass(String name) throws ClassNotFoundException { //NOPMD
            return super.findClass(name);
        }
    }
}
