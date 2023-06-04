/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.manager.client.cli.util;

import org.apache.inlong.manager.client.api.ClientConfiguration;
import org.apache.inlong.manager.client.api.impl.InlongClientImpl;
import org.apache.inlong.manager.client.api.inner.client.ClientFactory;
import org.apache.inlong.manager.common.auth.DefaultAuthentication;
import org.apache.inlong.manager.common.exceptions.BusinessException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * The utils for manager client.
 */
public class ClientUtils {

    private static final String CONFIG_FILE = "application.properties";
    public static ClientFactory clientFactory;
    private static ClientConfiguration configuration;
    private static String serviceUrl;

    /**
     * Get an inlong client instance.
     */
    public static InlongClientImpl getClient() {
        initClientConfiguration();
        return new InlongClientImpl(serviceUrl, configuration);
    }

    public static void initClientFactory() {
        clientFactory = org.apache.inlong.manager.client.api.util.ClientUtils.getClientFactory(
                getClient().getConfiguration());
    }

    /**
     * Get the file content.
     */
    public static String readFile(File file) {
        if (!file.exists() || file.length() == 0) {
            throw new BusinessException("File does not exist or is empty.");
        }
        StringBuilder stringBuilder = new StringBuilder();
        try (Reader reader = new InputStreamReader(Files.newInputStream(file.toPath()))) {
            int ch;
            while ((ch = reader.read()) != -1) {
                stringBuilder.append((char) ch);
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return stringBuilder.toString();
    }

    private static void initClientConfiguration() {
        Properties properties = new Properties();
        String path = Thread.currentThread().getContextClassLoader().getResource("").getPath() + CONFIG_FILE;
        try (InputStream inputStream = new BufferedInputStream(Files.newInputStream(Paths.get(path)))) {
            properties.load(inputStream);
            serviceUrl = properties.getProperty("server.host") + ":" + properties.getProperty("server.port");
            String user = properties.getProperty("default.admin.user");
            String password = properties.getProperty("default.admin.password");

            configuration = new ClientConfiguration();
            configuration.setAuthentication(new DefaultAuthentication(user, password));
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
