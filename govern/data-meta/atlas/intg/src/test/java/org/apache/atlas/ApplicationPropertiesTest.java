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
package org.apache.atlas;

import java.io.InputStream;
import java.util.AbstractMap;

import org.apache.atlas.utils.AtlasConfigurationUtil;
import org.apache.commons.configuration.Configuration;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

/**
 * Unit test for {@link ApplicationProperties}
 *
 */
public class ApplicationPropertiesTest {

    @Test
    public void testGetFileAsInputStream() throws Exception {
        Configuration props = ApplicationProperties.get("test.properties");
        InputStream inStr = null;

        // configured file as class loader resource
        try {
            inStr = ApplicationProperties.getFileAsInputStream(props, "jaas.properties.file", null);
            assertNotNull(inStr);
        }
        finally {
            if (inStr != null) {
                inStr.close();
            }
        }

        // configured file from file system path
        props.setProperty("jaas.properties.file", "src/test/resources/atlas-jaas.properties");
        try {
            inStr = ApplicationProperties.getFileAsInputStream(props, "jaas.properties.file", null);
            assertNotNull(inStr);
        }
        finally {
            if (inStr != null) {
                inStr.close();
            }
        }

        // default file as class loader resource
        try {
            inStr = ApplicationProperties.getFileAsInputStream(props, "property.not.specified.in.config", "atlas-jaas.properties");
            assertNotNull(inStr);
        }
        finally {
            if (inStr != null) {
                inStr.close();
            }
        }

        // default file relative to working directory
        try {
            inStr = ApplicationProperties.getFileAsInputStream(props, "property.not.specified.in.config", "src/test/resources/atlas-jaas.properties");
            assertNotNull(inStr);
        }
        finally {
            if (inStr != null) {
                inStr.close();
            }
        }

        // default file relative to atlas configuration directory
        String originalConfDirSetting = System.setProperty(ApplicationProperties.ATLAS_CONFIGURATION_DIRECTORY_PROPERTY, "src/test/resources");
        try {
            inStr = ApplicationProperties.getFileAsInputStream(props, "property.not.specified.in.config", "atlas-jaas.properties");
            assertNotNull(inStr);
        }
        finally {
            if (inStr != null) {
                inStr.close();
            }
            if (originalConfDirSetting != null) {
                System.setProperty(ApplicationProperties.ATLAS_CONFIGURATION_DIRECTORY_PROPERTY, originalConfDirSetting);
            }
            else {
                System.clearProperty(ApplicationProperties.ATLAS_CONFIGURATION_DIRECTORY_PROPERTY);
            }
        }

        // non-existent property and no default file
        try {
            inStr = ApplicationProperties.getFileAsInputStream(props, "property.not.specified.in.config", null);
            fail("Expected " + AtlasException.class.getSimpleName() + " but none thrown");
        }
        catch (AtlasException e) {
            // good
        }
        finally {
            if (inStr != null) {
                inStr.close();
            }
        }

        // configured file not found in file system or classpath
        props.setProperty("jaas.properties.file", "does_not_exist.txt");
        try {
            inStr = ApplicationProperties.getFileAsInputStream(props, "jaas.properties.file", null);
            fail("Expected " + AtlasException.class.getSimpleName() + " but none thrown");
        }
        catch (AtlasException e) {
            // good
        }
        finally {
            if (inStr != null) {
                inStr.close();
            }
        }
    }

    @Test
    public void verifySetDefault() throws AtlasException {
        Configuration props = ApplicationProperties.get("test.properties");
        ApplicationProperties aProps = (ApplicationProperties) props;

        String defaultValue = "someValue";
        String someKey = "someKey";
        AbstractMap.SimpleEntry<String, String> defaultKV = new AbstractMap.SimpleEntry<>(someKey, defaultValue);
        aProps.setDefault(defaultKV, "newValue");

        assertNotEquals(props.getString(someKey), defaultValue);
        aProps.setDefault(defaultKV, "");
        assertEquals(props.getString(someKey), defaultValue);
    }

    @Test
    public void verifyGetLatesttString () throws AtlasException {
        String key="atlas.metadata.namespace", old_val = "nm-sp-1", new_val = "nm-sp-2";
        Configuration atlasConf = ApplicationProperties.get("test.properties");
        assertEquals(atlasConf.getString(key), old_val);
        assertEquals(AtlasConfigurationUtil.getRecentString(atlasConf, key, old_val), new_val);
        assertEquals(AtlasConfigurationUtil.getRecentString(atlasConf, "garbage", old_val), old_val);
    }
}
