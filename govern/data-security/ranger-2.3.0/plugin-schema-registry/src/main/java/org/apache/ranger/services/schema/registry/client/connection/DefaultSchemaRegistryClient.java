/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ranger.services.schema.registry.client.connection;

import com.hortonworks.registries.auth.Login;
import com.hortonworks.registries.schemaregistry.client.LoadBalancedFailoverUrlSelector;
import com.hortonworks.registries.schemaregistry.client.UrlSelector;
import org.apache.ranger.services.schema.registry.client.connection.util.SecurityUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.hortonworks.registries.schemaregistry.client.SchemaRegistryClient.Configuration;

import static com.hortonworks.registries.schemaregistry.client.SchemaRegistryClient.Configuration.DEFAULT_CONNECTION_TIMEOUT;
import static com.hortonworks.registries.schemaregistry.client.SchemaRegistryClient.Configuration.DEFAULT_READ_TIMEOUT;


public class DefaultSchemaRegistryClient implements ISchemaRegistryClient {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultSchemaRegistryClient.class);

    private static final String SCHEMA_REGISTRY_PATH = "/api/v1/schemaregistry";
    private static final String SCHEMAS_PATH = SCHEMA_REGISTRY_PATH + "/schemas/";
    private static final String SCHEMA_REGISTRY_VERSION_PATH = SCHEMA_REGISTRY_PATH + "/version";
    private static final String SSL_ALGORITHM = "TLS";
    private final javax.ws.rs.client.Client client;
    private final Login login;
    private final UrlSelector urlSelector;
    private final Map<String, SchemaRegistryTargets> urlWithTargets;
    private final Configuration configuration;

    public DefaultSchemaRegistryClient(Map<String, ?> conf) {
        configuration = new Configuration(conf);
        login = SecurityUtils.initializeSecurityContext(conf);
        ClientConfig config = createClientConfig(conf);
        final boolean SSLEnabled = SecurityUtils.isHttpsConnection(conf);
        ClientBuilder clientBuilder = JerseyClientBuilder.newBuilder()
                .withConfig(config)
                .property(ClientProperties.FOLLOW_REDIRECTS, Boolean.TRUE);
        if (SSLEnabled) {
            SSLContext ctx;
            try {
                ctx = SecurityUtils.createSSLContext(conf, SSL_ALGORITHM);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            clientBuilder.sslContext(ctx);
        }
        client = clientBuilder.build();

        // get list of urls and create given or default UrlSelector.
        urlSelector = createUrlSelector();
        urlWithTargets = new ConcurrentHashMap<>();
    }

    private ClientConfig createClientConfig(Map<String, ?> conf) {
        ClientConfig config = new ClientConfig();
        config.property(ClientProperties.CONNECT_TIMEOUT, DEFAULT_CONNECTION_TIMEOUT);
        config.property(ClientProperties.READ_TIMEOUT, DEFAULT_READ_TIMEOUT);
        config.property(ClientProperties.FOLLOW_REDIRECTS, true);
        for (Map.Entry<String, ?> entry : conf.entrySet()) {
            config.property(entry.getKey(), entry.getValue());
        }
        return config;
    }

    private UrlSelector createUrlSelector() {
        UrlSelector urlSelector = null;
        String rootCatalogURL = configuration.getValue(Configuration.SCHEMA_REGISTRY_URL.name());
        String urlSelectorClass = configuration.getValue(Configuration.URL_SELECTOR_CLASS.name());
        if (urlSelectorClass == null) {
            urlSelector = new LoadBalancedFailoverUrlSelector(rootCatalogURL);
        } else {
            try {
                urlSelector = (UrlSelector) Class.forName(urlSelectorClass)
                        .getConstructor(String.class)
                        .newInstance(rootCatalogURL);
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | NoSuchMethodException
                    | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
        urlSelector.init(configuration.getConfig());

        return urlSelector;
    }

    private static class SchemaRegistryTargets {
        private final WebTarget schemaRegistryVersion;
        private final WebTarget schemasTarget;

        SchemaRegistryTargets(WebTarget rootResource) {
            schemaRegistryVersion = rootResource.path(SCHEMA_REGISTRY_VERSION_PATH);
            schemasTarget = rootResource.path(SCHEMAS_PATH);
        }
    }

    private SchemaRegistryTargets currentSchemaRegistryTargets() {
        String url = urlSelector.select();
        urlWithTargets.computeIfAbsent(url, s -> new SchemaRegistryTargets(client.target(s)));
        return urlWithTargets.get(url);
    }

    private static String encode(String schemaName) {
        try {
            return URLEncoder.encode(schemaName, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<String> getSchemaGroups() {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> DefaultSchemaRegistryClient.getSchemaGroups()");
        }

        ArrayList<String> res = new ArrayList<>();
        WebTarget webResource = currentSchemaRegistryTargets().schemasTarget;
        try {
            Response response = login.doAction(() ->
                    webResource.request(MediaType.APPLICATION_JSON_TYPE).get(Response.class));

            if(LOG.isDebugEnabled()) {
                LOG.debug("DefaultSchemaRegistryClient.getSchemaGroups(): response statusCode = " + response.getStatus());
            }

            JSONArray mDataList = new JSONObject(response.readEntity(String.class)).getJSONArray("entities");
            int len = mDataList.length();
            for(int i = 0; i < len; i++) {
                JSONObject entity = mDataList.getJSONObject(i);
                JSONObject schemaMetadata = (JSONObject)entity.get("schemaMetadata");
                String group = (String) schemaMetadata.get("schemaGroup");
                res.add(group);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== DefaultSchemaRegistryClient.getSchemaGroups(): "
                    + res.size()
                    + " schemaGroups found");
        }

        return res;
    }

    @Override
    public List<String> getSchemaNames(List<String> schemaGroups) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> DefaultSchemaRegistryClient.getSchemaNames( " + schemaGroups + " )");
        }

        ArrayList<String> res = new ArrayList<>();
        WebTarget webTarget = currentSchemaRegistryTargets().schemasTarget;
        try {
            Response response = login.doAction(() ->
                    webTarget.request(MediaType.APPLICATION_JSON_TYPE).get(Response.class));

            if(LOG.isDebugEnabled()) {
                LOG.debug("DefaultSchemaRegistryClient.getSchemaNames(): response statusCode = " + response.getStatus());
            }

            JSONArray mDataList = new JSONObject(response.readEntity(String.class)).getJSONArray("entities");
            int len = mDataList.length();
            for(int i = 0; i < len; i++) {
                JSONObject entity = mDataList.getJSONObject(i);
                JSONObject schemaMetadata = (JSONObject)entity.get("schemaMetadata");
                String group = (String) schemaMetadata.get("schemaGroup");
                for(String schemaGroup:  schemaGroups) {
                    if(group.matches(schemaGroup)) {
                        String name = (String) schemaMetadata.get("name");
                        res.add(name);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== DefaultSchemaRegistryClient.getSchemaNames( " + schemaGroups + " ): "
                    + res.size()
                    + " schemaNames found");
        }

        return res;
    }

    @Override
    public List<String> getSchemaBranches(String schemaMetadataName) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> DefaultSchemaRegistryClient.getSchemaBranches( " + schemaMetadataName + " )");
        }

        ArrayList<String> res = new ArrayList<>();
        WebTarget target = currentSchemaRegistryTargets().schemasTarget.path(encode(schemaMetadataName) + "/branches");
        try {
            Response response = login.doAction(() ->
                    target.request(MediaType.APPLICATION_JSON_TYPE).get(Response.class));

            if(LOG.isDebugEnabled()) {
                LOG.debug("DefaultSchemaRegistryClient.getSchemaBranches(): response statusCode = " + response.getStatus());
            }

            JSONArray mDataList = new JSONObject(response.readEntity(String.class)).getJSONArray("entities");
            int len = mDataList.length();
            for(int i = 0; i < len; i++) {
                JSONObject entity = mDataList.getJSONObject(i);
                JSONObject branchInfo = entity;
                String smName = (String) branchInfo.get("schemaMetadataName");
                if (smName.matches(schemaMetadataName)) {
                    String bName = (String) branchInfo.get("name");
                    res.add(bName);
                }

            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== DefaultSchemaRegistryClient.getSchemaBranches( " + schemaMetadataName + " ): "
                    + res.size()
                    + " branches found.");
        }

        return res;
    }

    @Override
    public void checkConnection() throws Exception {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> DefaultSchemaRegistryClient.checkConnection(): trying to connect to the SR server... ");
        }

        WebTarget webTarget = currentSchemaRegistryTargets().schemaRegistryVersion;
        Response responce = login.doAction(() ->
                webTarget.request(MediaType.APPLICATION_JSON_TYPE).get(Response.class));
        if(LOG.isDebugEnabled()) {
            LOG.debug("DefaultSchemaRegistryClient.checkConnection(): response statusCode = " + responce.getStatus());
        }
        if(responce.getStatus() != Response.Status.OK.getStatusCode()) {
            LOG.error("DefaultSchemaRegistryClient.checkConnection(): Connection failed. Response StatusCode = "
                    + responce.getStatus());
            throw new Exception("Connection failed. StatusCode = " + responce.getStatus());
        }

        String respStr = responce.readEntity(String.class);
        if (!(respStr.contains("version") && respStr.contains("revision"))) {
            LOG.error("DefaultSchemaRegistryClient.checkConnection(): Connection failed. Bad response body.");
            throw new Exception("Connection failed. Bad response body.");
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== DefaultSchemaRegistryClient.checkConnection(): connection test successfull ");
        }
    }

}
