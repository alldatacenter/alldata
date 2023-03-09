/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/

package org.apache.griffin.core.util;

import static org.apache.griffin.core.util.PropertiesUtil.getConf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.FileNotFoundException;
import java.util.Properties;

import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

public class PropertiesUtilTest {

    @Test
    public void testGetPropertiesForSuccess() {
        String path = "/quartz.properties";
        Properties properties = PropertiesUtil.getProperties(path,
                new ClassPathResource(path));
        assertEquals(properties
                .get("org.quartz.jobStore.isClustered"), "true");
    }

    @Test
    public void testGetPropertiesForFailureWithWrongPath() {
        String path = ".././quartz.properties";
        Properties properties = PropertiesUtil.getProperties(path,
                new ClassPathResource(path));
        assertEquals(properties, null);
    }

    @Test
    public void testGetConfWithLocation() throws FileNotFoundException {
        String name = "sparkJob.properties";
        String defaultPath = "/" + name;
        String location = "src/test/resources";
        Properties properties = getConf(name, defaultPath, location);
        assertNotNull(properties);
    }

    @Test
    public void testGetConfWithLocationEmpty() throws FileNotFoundException {
        String name = "sparkJob.properties";
        String defaultPath = "/" + name;
        String location = "src/main";
        Properties properties = getConf(name, defaultPath, location);
        assertNotNull(properties);
    }

    @Test
    public void testGetConfWithNoLocation() throws FileNotFoundException {
        String name = "sparkJob.properties";
        String defaultPath = "/" + name;
        Properties properties = getConf(name, defaultPath, null);
        assertNotNull(properties);
    }

}
