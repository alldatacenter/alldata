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

import org.testng.Assert;
import org.testng.annotations.Test;


public class AtlasPluginClassLoaderTest {

    @Test
    public void testClassLoader() throws Exception {
        String cls = "org.apache.atlas.service.Services";

        try {
            loadClass(cls, null);
            Assert.fail("Expected ClassNotFoundException");
        } catch (ClassNotFoundException e) {
            //expected
        }

        AtlasPluginClassLoader classLoader = new AtlasPluginClassLoader("../common/target");

        classLoader.activate();

        //org.apache.atlas.service.Services class should be loadable now
        //should also load org.apache.atlas.service.Service
        Class<?> servicesCls = loadClass(cls, null);
        loadClass("org.apache.atlas.service.Service", servicesCls.getClassLoader());

        //Fall back to current class loader should also work
        loadClass(AtlasPluginClassLoaderUtil.class.getName(), null);

        classLoader.deactivate();

        //After disable, class loading should fail again
        try {
            loadClass(cls, null);
            Assert.fail("Expected ClassNotFoundException");
        } catch (ClassNotFoundException e) {
            //expected
        }
    }

    private Class<?> loadClass(String cls, ClassLoader classLoader) throws ClassNotFoundException {
        if (classLoader == null) {
            classLoader = Thread.currentThread().getContextClassLoader();
        }
        return Class.forName(cls, true, classLoader);
    }
}
