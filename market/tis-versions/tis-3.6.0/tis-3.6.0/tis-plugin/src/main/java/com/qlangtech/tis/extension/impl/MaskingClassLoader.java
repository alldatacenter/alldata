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

import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * {@link ClassLoader} that masks a specified set of classes
 * from its parent class loader.
 * <p>
 * This code is used to create an isolated environment.
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class MaskingClassLoader extends ClassLoader {

    /**
     * Prefix of the packages that should be hidden.
     */
    private final List<String> masksClasses = new ArrayList<String>();

    private final List<String> masksResources = new ArrayList<String>();

    public MaskingClassLoader(ClassLoader parent, String... masks) {
        this(parent, Arrays.asList(masks));
    }

    public MaskingClassLoader(ClassLoader parent, Collection<String> masks) {
        super(parent);
        this.masksClasses.addAll(masks);
        /**
         * The name of a resource is a '/'-separated path name
         */
        for (String mask : masks) {
            masksResources.add(mask.replace(".", "/"));
        }
    }

    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
//        for (String mask : masksClasses) {
//            if (name.startsWith(mask))
//                throw new ClassNotFoundException();
//        }
        if (isMasked(name)) {
            return null;
        }
        return super.loadClass(name, resolve);
    }

    @Override
    public synchronized URL getResource(String name) {
        if (isMasked(name)){
            return null;
        }
        return super.getResource(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        if (isMasked(name)){
            return Collections.emptyEnumeration();
        }
        return super.getResources(name);
    }

    public synchronized void add(String prefix) {
        masksClasses.add(prefix);
        if (prefix != null) {
            masksResources.add(prefix.replace(".", "/"));
        }
    }

    private boolean isMasked(String name) {
        for (String mask : masksResources) {
            if (name.startsWith(mask))
                return true;
        }
        return false;
    }
}
