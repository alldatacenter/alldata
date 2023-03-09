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

import static org.apache.griffin.core.util.FileUtil.getFilePath;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;

public class PropertiesUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(
        PropertiesUtil.class);

    public static Properties getProperties(String path, Resource resource) {
        PropertiesFactoryBean propFactoryBean = new PropertiesFactoryBean();
        Properties properties = null;
        try {
            propFactoryBean.setLocation(resource);
            propFactoryBean.afterPropertiesSet();
            properties = propFactoryBean.getObject();
            LOGGER.info("Read properties successfully from {}.", path);
        } catch (IOException e) {
            LOGGER.error("Get properties from {} failed. {}", path, e);
        }
        return properties;
    }

    /**
     * @param name        properties name like quartz.properties
     * @param defaultPath properties classpath like /quartz.properties
     * @param location    custom properties path
     * @return Properties
     * @throws FileNotFoundException location setting is wrong that there is no
     *                               target file.
     */
    public static Properties getConf(String name, String defaultPath,
                                     String location)
        throws FileNotFoundException {
        String path = getConfPath(name, location);
        Resource resource;
        if (path == null) {
            resource = new ClassPathResource(defaultPath);
            path = defaultPath;
        } else {
            resource = new InputStreamResource(new FileInputStream(path));
        }
        return getProperties(path, resource);
    }

    public static String getConfPath(String name, String location) {
        return getFilePath(name, location);
    }


}
