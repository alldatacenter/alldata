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

import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.Project;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.*;

/**
 * classLoader which use first /WEB-INF/lib/*.jar and /WEB-INF/classes before core classLoader
 * <b>you must use the pluginFirstClassLoader true in the maven-hpi-plugin</b>
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/09/25
 * @since 1.371
 */
public class PluginFirstClassLoader extends AntClassLoader implements Closeable {

    private List<URL> urls = new ArrayList<URL>();
    private final Vector pathComponents;

    public PluginFirstClassLoader() {
        super(null, false);
        try {
            Field $pathComponents = AntClassLoader.class.getDeclaredField("pathComponents");
            $pathComponents.setAccessible(true);
            pathComponents = (Vector) $pathComponents.get(this);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new Error(e);
        }
    }

    public void addPathFiles(Collection<File> paths) throws IOException {
        for (File f : paths) {
            urls.add(f.toURI().toURL());
            addPathFile(f);
        }
    }

    /**
     * @return List of jar used by the plugin /WEB-INF/lib/*.jar and classes directory /WEB-INF/classes
     */
    public List<URL> getURLs() {
        return urls;
    }

//    @Override
//    public void close()  {
//        cleanup();
//    }

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
    protected Enumeration findResources(String arg0, boolean arg1) throws IOException {
        Enumeration enu = super.findResources(arg0, arg1);
        return enu;
    }

    @Override
    protected Enumeration<URL> findResources(String name) throws IOException {
        Enumeration enu = super.findResources(name);
        return enu;
    }

    @Override
    public URL getResource(String arg0) {
        URL url = super.getResource(arg0);
        return url;
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        InputStream is = super.getResourceAsStream(name);
        return is;
    }
}
