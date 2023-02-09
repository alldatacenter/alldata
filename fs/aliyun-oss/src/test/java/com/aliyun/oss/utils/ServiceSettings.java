/*
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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.aliyun.oss.utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ServiceSettings {

    private static final String SETTINGS_FILE_NAME =
            System.getProperty("user.home") +
            System.getProperty("file.separator") +
            "aliyun-oss-test.properties";
    private static final Log log = LogFactory.getLog(ServiceSettings.class);
    
    private Properties properties = new Properties();


    public ServiceSettings() {
    }

    public String getOSSEndpoint() {
        return properties.getProperty("oss.endpoint");
    }

    public void setOSSEndpoint(String ossEndpoint) {
        properties.setProperty("oss.endpoint", ossEndpoint);
    }

    public String getOSSAccessKeyId() {
        return properties.getProperty("oss.accesskeyid");
    }

    public void setOSSAccessKeyId(String ossAccessKeyId) {
        properties.setProperty("oss.accesskeyid", ossAccessKeyId);
    }

    public String getOSSAccessKeySecret() {
        return properties.getProperty("oss.accesskeysecret");
    }

    public void setOSSAccessKeySecret(String ossAccessKeySecret) {
        properties.setProperty("oss.accesskeysecret", ossAccessKeySecret);
    }

    public String getProxyHost() {
        return properties.getProperty("proxy.host");
    }

    public void setProxyHost(String proxyHost) {
        properties.setProperty("proxy.host", proxyHost);
    }

    public int getProxyPort() {
        if (properties.getProperty("proxy.port") != null) {
           return Integer.parseInt(properties.getProperty("proxy.port"));
        }
        else {
            return 0;
        }
    }

    public void setProxyPort(int proxyPort) {
        properties.setProperty("proxy.port", String.valueOf(proxyPort));
    }

    /**
     * Load settings from the configuration file.
     * <p>
     * The configuration format:
     * oss.endpoint=
     * oss.accesskeyid=
     * oss.accesskeysecret=
     * proxy.host=
     * proxy.port=
     * </p>
     * @return
     */
    public static ServiceSettings load() {
        ServiceSettings ss = new ServiceSettings();

        InputStream is = null;
        try {
            is = new FileInputStream(SETTINGS_FILE_NAME);
            Properties pr = new Properties();
            pr.load(is);

            ss.properties = pr;

        } catch (FileNotFoundException e) {
            log.warn("The settings file '" + SETTINGS_FILE_NAME + "' does not exist.");
        } catch (IOException e) {
            log.warn("Failed to load the settings from the file: " + SETTINGS_FILE_NAME);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) { }
            }
        }

        return ss;
    }
}
