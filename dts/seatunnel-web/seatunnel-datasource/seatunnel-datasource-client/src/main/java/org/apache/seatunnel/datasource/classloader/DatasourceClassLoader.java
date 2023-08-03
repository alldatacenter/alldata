/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seatunnel.datasource.classloader;

import org.apache.seatunnel.common.utils.ExceptionUtils;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

@Slf4j
public class DatasourceClassLoader extends URLClassLoader {

    private final ClassLoader parentClassLoader;

    public DatasourceClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, null);
        this.parentClassLoader = parent;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        log.info("load class for name : " + name);
        try {
            for (String alwaysParentFirstPattern :
                    DatasourceLoadConfig.DEFAULT_PARENT_FIRST_PATTERNS) {
                if (name.startsWith(alwaysParentFirstPattern)) {
                    return parentClassLoader.loadClass(name);
                }
            }
            return findClass(name);
        } catch (ClassNotFoundException e) {
            log.info("load class from parentClassLoader : " + name);
            try {

                return parentClassLoader.loadClass(name);
            } catch (ClassNotFoundException superE) {
                log.error(
                        "parentClassLoader load class is error : " + ExceptionUtils.getMessage(e));
                throw new ClassNotFoundException(
                        "parentClassLoader Failed to load class: " + name, superE);
            }
        }
    }

    @Override
    public URL getResource(String name) {
        log.info("getResource : " + name);
        // first, try and find it via the URLClassloader
        URL urlClassLoaderResource = findResource(name);
        if (urlClassLoaderResource != null) {
            return urlClassLoaderResource;
        }

        // delegate to super
        return parentClassLoader.getResource(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        // first get resources from URLClassloader
        log.info("getResources : " + name);
        Enumeration<URL> urlClassLoaderResources = findResources(name);
        final List<URL> result = new ArrayList<>();

        while (urlClassLoaderResources.hasMoreElements()) {
            result.add(urlClassLoaderResources.nextElement());
        }

        // get parent urls
        if (parentClassLoader != null) {
            Enumeration<URL> parentResources = parentClassLoader.getResources(name);
            while (parentResources.hasMoreElements()) {
                result.add(parentResources.nextElement());
            }
        }

        return new Enumeration<URL>() {
            final Iterator<URL> iter = result.iterator();

            public boolean hasMoreElements() {
                return iter.hasNext();
            }

            public URL nextElement() {
                return iter.next();
            }
        };
    }
}
