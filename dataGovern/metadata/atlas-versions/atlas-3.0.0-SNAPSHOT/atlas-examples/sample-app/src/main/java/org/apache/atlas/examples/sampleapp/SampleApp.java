/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.examples.sampleapp;

import org.apache.atlas.AtlasClientV2;
import org.apache.atlas.AtlasException;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.utils.AuthenticationUtil;

import java.util.Date;

public class SampleApp {
    private AtlasClientV2 client;

    SampleApp(String[] atlasServerUrls, String[] basicAuthUsernamePassword) {
        client = new AtlasClientV2(atlasServerUrls, basicAuthUsernamePassword);
    }

    SampleApp(String[] atlasServerUrls) throws AtlasException {
        client = new AtlasClientV2(atlasServerUrls);
    }

    public static void main(String[] args) throws Exception {
        SampleApp sampleApp = null;

        try {
            if (!AuthenticationUtil.isKerberosAuthenticationEnabled()) {
                String[] atlasServerUrls           = getServerUrl();
                String[] basicAuthUsernamePassword = getUserInput();

                sampleApp = new SampleApp(atlasServerUrls, basicAuthUsernamePassword);
            } else {
                String[] atlasServerUrls = getServerUrl();

                sampleApp = new SampleApp(atlasServerUrls);
            }

            // TypeDef Examples
            TypeDefExample typeDefExample = new TypeDefExample(sampleApp.getClient());

            typeDefExample.createTypeDefinitions();
            typeDefExample.printTypeDefinitions();

            // Entity Examples
            EntityExample entityExample = new EntityExample(sampleApp.getClient());

            entityExample.createEntities();

            AtlasEntity createdEntity = entityExample.getTableEntity();

            entityExample.getEntityByGuid(createdEntity.getGuid());

            // Lineage Examples
            sampleApp.lineageExample(createdEntity.getGuid());

            // Discovery/Search Examples
            sampleApp.discoveryExample(createdEntity);

            // Glossary Examples
            sampleApp.glossaryExample();

            entityExample.deleteEntities();
        } finally {
            if (sampleApp != null && sampleApp.getClient() != null) {
                sampleApp.getClient().close();
            }
        }
    }

    public static void log(String message) {
        System.out.println("[" + new Date() + "] " + message);
    }

    public AtlasClientV2 getClient() {
        return client;
    }

    private void lineageExample(String entityGuid) throws Exception {
        LineageExample lineageExample = new LineageExample(client);

        lineageExample.lineage(entityGuid);
    }

    private void discoveryExample(AtlasEntity entity) {
        DiscoveryExample discoveryExample = new DiscoveryExample(client);

        discoveryExample.testSearch();
        discoveryExample.quickSearch(entity.getTypeName());
        discoveryExample.basicSearch(entity.getTypeName(), SampleAppConstants.METRIC_TAG, (String)entity.getAttribute(SampleAppConstants.ATTR_NAME));
    }

    private void glossaryExample() throws Exception {
        GlossaryExample glossaryExample = new GlossaryExample(client);

        glossaryExample.createGlossary();
        glossaryExample.createGlossaryTerm();
        glossaryExample.getGlossaryDetail();
        glossaryExample.createGlossaryCategory();
        glossaryExample.deleteGlossary();
    }

    private static String[] getUserInput() {
        try {
            String  username = System.console().readLine("Enter username: ");
            char[]  pwChar   = System.console().readPassword("Enter password: ");
            String  password = (pwChar != null) ? new String(pwChar) : "";

            return new String[] { username, password };
        } catch (Exception e) {
            System.out.print("Error while reading user input");
            System.exit(1);
        }

        return null; // will not reach here
    }

    private static String[] getServerUrl() {
        try {
            String atlasServerUrl = System.console().readLine("Enter Atlas server URL: ");

            return new String[] { atlasServerUrl };
        } catch (Exception e) {
            System.out.print("Error while reading user input");
            System.exit(1);
        }

        return null; // will not reach here
    }
}