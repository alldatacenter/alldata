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
package org.apache.atlas;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.google.common.annotations.VisibleForTesting;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.client.urlconnection.URLConnectionClientHandler;
import com.sun.jersey.multipart.BodyPart;
import com.sun.jersey.multipart.FormDataBodyPart;
import com.sun.jersey.multipart.FormDataMultiPart;
import com.sun.jersey.multipart.MultiPart;
import com.sun.jersey.multipart.file.FileDataBodyPart;
import com.sun.jersey.multipart.file.StreamDataBodyPart;
import com.sun.jersey.multipart.impl.MultiPartWriter;
import org.apache.atlas.model.impexp.AtlasExportRequest;
import org.apache.atlas.model.impexp.AtlasImportRequest;
import org.apache.atlas.model.impexp.AtlasImportResult;
import org.apache.atlas.model.impexp.AtlasServer;
import org.apache.atlas.model.metrics.AtlasMetrics;
import org.apache.atlas.security.SecureClientUtils;
import org.apache.atlas.type.AtlasType;
import org.apache.atlas.utils.AtlasJson;
import org.apache.atlas.utils.AuthenticationUtil;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.security.UserGroupInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.apache.atlas.security.SecurityProperties.TLS_ENABLED;

public abstract class AtlasBaseClient {
    public static final String BASE_URI = "api/atlas/";
    public static final String TYPES = "types";
    public static final String ADMIN_VERSION = "admin/version";
    public static final String ADMIN_STATUS = "admin/status";
    public static final String ADMIN_METRICS = "admin/metrics";
    public static final String ADMIN_IMPORT = "admin/import";
    public static final String ADMIN_EXPORT = "admin/export";
    public static final String ADMIN_SERVER_TEMPLATE = "%sadmin/server/%s";

    public static final String QUERY = "query";
    public static final String LIMIT = "limit";
    public static final String OFFSET = "offset";
    public static final String STATUS = "Status";

    public static final API API_STATUS  = new API(BASE_URI + ADMIN_STATUS, HttpMethod.GET, Response.Status.OK);;
    public static final API API_VERSION = new API(BASE_URI + ADMIN_VERSION, HttpMethod.GET, Response.Status.OK);;
    public static final API API_METRICS = new API(BASE_URI + ADMIN_METRICS, HttpMethod.GET, Response.Status.OK);;

    static final        String JSON_MEDIA_TYPE                       = MediaType.APPLICATION_JSON + "; charset=UTF-8";
    static final        String UNKNOWN_STATUS                        = "Unknown status";
    static final        String ATLAS_CLIENT_HA_RETRIES_KEY           = "atlas.client.ha.retries";
    // Setting the default value based on testing failovers while client code like quickstart is running.
    static final        int    DEFAULT_NUM_RETRIES                   = 4;
    static final        String ATLAS_CLIENT_HA_SLEEP_INTERVAL_MS_KEY = "atlas.client.ha.sleep.interval.ms";
    // Setting the default value based on testing failovers while client code like quickstart is running.
    // With number of retries, this gives a total time of about 20s for the server to start.
    static final int DEFAULT_SLEEP_BETWEEN_RETRIES_MS = 5000;
    private static final Logger LOG = LoggerFactory.getLogger(AtlasBaseClient.class);
    private static final API IMPORT = new API(BASE_URI + ADMIN_IMPORT, HttpMethod.POST, Response.Status.OK, MediaType.MULTIPART_FORM_DATA, MediaType.APPLICATION_JSON);
    private static final API EXPORT = new API(BASE_URI + ADMIN_EXPORT, HttpMethod.POST, Response.Status.OK, MediaType.APPLICATION_JSON, MediaType.APPLICATION_OCTET_STREAM);
    private static final String IMPORT_REQUEST_PARAMTER = "request";
    private static final String IMPORT_DATA_PARAMETER = "data";

    protected WebResource service;
    protected Configuration configuration;
    private String basicAuthUser;
    private String basicAuthPassword;
    private AtlasClientContext atlasClientContext;
    private boolean retryEnabled = false;
    private Cookie cookie = null;

    private SecureClientUtils clientUtils;

    protected AtlasBaseClient() {
    }

    protected AtlasBaseClient(String[] baseUrl, String[] basicAuthUserNamePassword) {
        if (basicAuthUserNamePassword != null) {
            if (basicAuthUserNamePassword.length > 0) {
                this.basicAuthUser = basicAuthUserNamePassword[0];
            }
            if (basicAuthUserNamePassword.length > 1) {
                this.basicAuthPassword = basicAuthUserNamePassword[1];
            }
        }

        initializeState(baseUrl, null, null);
    }

    protected AtlasBaseClient(String... baseUrls) throws AtlasException {
        this(getCurrentUGI(), baseUrls);
    }

    protected AtlasBaseClient(UserGroupInformation ugi, String[] baseUrls) {
        this(ugi, ugi.getShortUserName(), baseUrls);
    }

    protected AtlasBaseClient(UserGroupInformation ugi, String doAsUser, String[] baseUrls) {
        initializeState(baseUrls, ugi, doAsUser);
    }

    protected AtlasBaseClient(String[] baseUrls, Cookie cookie) {
        this.cookie = cookie;
        initializeState(baseUrls, null, null);
    }

    @VisibleForTesting
    protected AtlasBaseClient(WebResource service, Configuration configuration) {
        this.service = service;
        this.configuration = configuration;
    }

    @VisibleForTesting
    protected AtlasBaseClient(Configuration configuration, String[] baseUrl, String[] basicAuthUserNamePassword) {
        if (basicAuthUserNamePassword != null) {
            if (basicAuthUserNamePassword.length > 0) {
                this.basicAuthUser = basicAuthUserNamePassword[0];
            }
            if (basicAuthUserNamePassword.length > 1) {
                this.basicAuthPassword = basicAuthUserNamePassword[1];
            }
        }

        initializeState(configuration, baseUrl, null, null);
    }

    protected static UserGroupInformation getCurrentUGI() throws AtlasException {
        try {
            return UserGroupInformation.getCurrentUser();
        } catch (IOException e) {
            throw new AtlasException(e);
        }
    }

    public void setCookie(Cookie cookie) {
        this.cookie = cookie;
    }

    public boolean isServerReady() throws AtlasServiceException {
        WebResource resource   = getResource(API_VERSION.getNormalizedPath());
        try {
            callAPIWithResource(API_VERSION, resource, null, ObjectNode.class);
            return true;
        } catch (ClientHandlerException che) {
            return false;
        } catch (AtlasServiceException ase) {
            if (ase.getStatus() != null && ase.getStatus().equals(ClientResponse.Status.SERVICE_UNAVAILABLE)) {
                LOG.warn("Received SERVICE_UNAVAILABLE, server is not yet ready");
                return false;
            }
            throw ase;
        }
    }

    /**
     * Return status of the service instance the client is pointing to.
     *
     * @return One of the values in ServiceState.ServiceStateValue or {@link #UNKNOWN_STATUS} if
     * there is a JSON parse exception
     * @throws AtlasServiceException if there is a HTTP error.
     */
    public String getAdminStatus() throws AtlasServiceException {
        String      result    = AtlasBaseClient.UNKNOWN_STATUS;
        WebResource resource  = getResource(service, API_STATUS.getNormalizedPath());
        ObjectNode  response  = callAPIWithResource(API_STATUS, resource, null, ObjectNode.class);

        if (response.has(STATUS)) {
            result = response.get(STATUS).asText();
        }

        return result;
    }

    /**
     * @return Return metrics of the service instance the client is pointing to
     * @throws AtlasServiceException
     */
    public AtlasMetrics getAtlasMetrics() throws AtlasServiceException {
        return callAPI(API_METRICS, AtlasMetrics.class, null);
    }

    public <T> T callAPI(API api, Class<T> responseType, Object requestObject, String... params)
            throws AtlasServiceException {
        return callAPIWithResource(api, getResource(api, params), requestObject, responseType);
    }

    public <T> T callAPI(API api, GenericType<T> responseType, Object requestObject, String... params)
            throws AtlasServiceException {
        return callAPIWithResource(api, getResource(api, params), requestObject, responseType);
    }

    public <T> T callAPI(API api, Class<T> responseType, Object requestBody,
                         MultivaluedMap<String, String> queryParams, String... params) throws AtlasServiceException {
        WebResource resource = getResource(api, queryParams, params);
        return callAPIWithResource(api, resource, requestBody, responseType);
    }

    public <T> T callAPI(API api, Class<T> responseType, MultivaluedMap<String, String> queryParams, String... params)
            throws AtlasServiceException {
        WebResource resource = getResource(api, queryParams, params);
        return callAPIWithResource(api, resource, null, responseType);
    }

    public <T> T callAPI(API api, GenericType<T> responseType, MultivaluedMap<String, String> queryParams, String... params)
            throws AtlasServiceException {
        WebResource resource = getResource(api, queryParams, params);
        return callAPIWithResource(api, resource, null, responseType);
    }

    public <T> T callAPI(API api, Class<T> responseType, MultivaluedMap<String, String> queryParams)
            throws AtlasServiceException {
        return callAPIWithResource(api, getResource(api, queryParams), null, responseType);
    }

    public <T> T callAPI(API api, Class<T> responseType, String queryParamKey, List<String> queryParamValues)
            throws AtlasServiceException {
        return callAPIWithResource(api, getResource(api, queryParamKey, queryParamValues), null, responseType);
    }

    @VisibleForTesting
    protected Client getClient(Configuration configuration, UserGroupInformation ugi, String doAsUser) {
        DefaultClientConfig config = new DefaultClientConfig();
        // Enable POJO mapping feature
        config.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
        config.getClasses().add(JacksonJaxbJsonProvider.class);
        config.getClasses().add(MultiPartWriter.class);

        int readTimeout = configuration.getInt("atlas.client.readTimeoutMSecs", 60000);
        int connectTimeout = configuration.getInt("atlas.client.connectTimeoutMSecs", 60000);
        if (configuration.getBoolean(TLS_ENABLED, false)) {
            // create an SSL properties configuration if one doesn't exist.  SSLFactory expects a file, so forced
            // to create a
            // configuration object, persist it, then subsequently pass in an empty configuration to SSLFactory
            try {
                SecureClientUtils.persistSSLClientConfiguration(configuration, System.getProperty("atlas.conf") );
            } catch (Exception e) {
                LOG.info("Error processing client configuration.", e);
            }
        }

        final URLConnectionClientHandler handler;
        clientUtils = new SecureClientUtils();

        boolean isKerberosEnabled = AuthenticationUtil.isKerberosAuthenticationEnabled(ugi);

        if (isKerberosEnabled) {
            handler = clientUtils.getClientConnectionHandler(config, configuration, doAsUser, ugi);
        } else {
            if (configuration.getBoolean(TLS_ENABLED, false)) {
                handler = clientUtils.getUrlConnectionClientHandler();
            } else {
                handler = new URLConnectionClientHandler();
            }
        }
        Client client = new Client(handler, config);
        client.setReadTimeout(readTimeout);
        client.setConnectTimeout(connectTimeout);
        return client;
    }

    public void close() {
        if (clientUtils != null) {
            clientUtils.destroyFactory();
        }
    }

    @VisibleForTesting
    protected String determineActiveServiceURL(String[] baseUrls, Client client) {
        if (baseUrls.length == 0) {
            throw new IllegalArgumentException("Base URLs cannot be null or empty");
        }
        final String baseUrl;
        AtlasServerEnsemble atlasServerEnsemble = new AtlasServerEnsemble(baseUrls);
        if (atlasServerEnsemble.hasSingleInstance()) {
            baseUrl = atlasServerEnsemble.firstURL();
            LOG.info("Client has only one service URL, will use that for all actions: {}", baseUrl);
        } else {
            try {
                baseUrl = selectActiveServerAddress(client, atlasServerEnsemble);
            } catch (AtlasServiceException e) {
                LOG.error("None of the passed URLs are active: {}", atlasServerEnsemble, e);
                throw new IllegalArgumentException("None of the passed URLs are active " + atlasServerEnsemble, e);
            }
        }
        return baseUrl;
    }

    protected Configuration getClientProperties() {
        try {
            if (configuration == null) {
                configuration = ApplicationProperties.get();
            }
        } catch (AtlasException e) {
            LOG.error("Exception while loading configuration.", e);
        }
        return configuration;
    }

    protected WebResource getResource(String path, String... pathParams) {
        return getResource(service, path, pathParams);
    }

    protected <T> T callAPIWithResource(API api, WebResource resource, Object requestObject, Class<T> responseType) throws AtlasServiceException {
        GenericType<T> genericType = null;
        if (responseType != null) {
            genericType = new GenericType<>(responseType);
        }
        return callAPIWithResource(api, resource, requestObject, genericType);
    }

    protected <T> T callAPIWithResource(API api, WebResource resource, Object requestObject, GenericType<T> responseType) throws AtlasServiceException {
        ClientResponse clientResponse = null;
        int i = 0;
        do {
            if (LOG.isDebugEnabled()) {
                LOG.debug("------------------------------------------------------");
                LOG.debug("Call         : {} {}", api.getMethod(), api.getNormalizedPath());
                LOG.debug("Content-type : {} ", api.getConsumes());
                LOG.debug("Accept       : {} ", api.getProduces());
                if (requestObject != null) {
                    LOG.debug("Request      : {}", requestObject);
                }
            }

            WebResource.Builder requestBuilder = resource.getRequestBuilder();

            // Set content headers
            requestBuilder
                    .accept(api.getProduces())
                    .type(api.getConsumes())
                    .header("Expect", "100-continue");

            // Set cookie if present
            if (cookie != null) {
                requestBuilder.cookie(cookie);
            }

            clientResponse = requestBuilder.method(api.getMethod(), ClientResponse.class, requestObject);

            LOG.debug("HTTP Status  : {}", clientResponse.getStatus());

            if (!LOG.isDebugEnabled()) {
                LOG.info("method={} path={} contentType={} accept={} status={}", api.getMethod(),
                    api.getNormalizedPath(), api.getConsumes(), api.getProduces(), clientResponse.getStatus());
            }

            if (clientResponse.getStatus() == api.getExpectedStatus().getStatusCode()) {
                if (responseType == null) {
                    return null;
                }
                try {
                    if(api.getProduces().equals(MediaType.APPLICATION_OCTET_STREAM)) {
                        return (T) clientResponse.getEntityInputStream();
                    } else if (responseType.getRawClass().equals(ObjectNode.class)) {
                        String stringEntity = clientResponse.getEntity(String.class);
                        try {
                            JsonNode jsonObject = AtlasJson.parseToV1JsonNode(stringEntity);
                            LOG.debug("Response     : {}", jsonObject);
                            LOG.debug("------------------------------------------------------");
                            return (T) jsonObject;
                        } catch (IOException e) {
                            throw new AtlasServiceException(api, e);
                        }
                    } else {
                        T entity = clientResponse.getEntity(responseType);
                        LOG.debug("Response     : {}", entity);
                        LOG.debug("------------------------------------------------------");
                        return entity;
                    }
                } catch (ClientHandlerException e) {
                    throw new AtlasServiceException(api, e);
                }
            } else if (clientResponse.getStatus() != ClientResponse.Status.SERVICE_UNAVAILABLE.getStatusCode()) {
                break;
            } else {
                LOG.error("Got a service unavailable when calling: {}, will retry..", resource);
                sleepBetweenRetries();
            }

            i++;
        } while (i < getNumberOfRetries());

        throw new AtlasServiceException(api, clientResponse);
    }

    protected WebResource getResource(API api, String... pathParams) {
        return getResource(service, api, pathParams);
    }

    protected WebResource getResource(API api, MultivaluedMap<String, String> queryParams, String... pathParams) {
        WebResource resource = service.path(api.getNormalizedPath());
        resource = appendPathParams(resource, pathParams);
        resource = appendQueryParams(queryParams, resource);
        return resource;
    }

    protected WebResource getResource(API api, MultivaluedMap<String, String> queryParams) {
        return getResource(service, api, queryParams);
    }

    protected abstract API formatPathParameters(API api, String... params);

    void initializeState(String[] baseUrls, UserGroupInformation ugi, String doAsUser) {
        initializeState(getClientProperties(), baseUrls, ugi, doAsUser);
    }

    void initializeState(Configuration configuration, String[] baseUrls, UserGroupInformation ugi, String doAsUser) {
        this.configuration = configuration;
        Client client = getClient(configuration, ugi, doAsUser);

        if ((!AuthenticationUtil.isKerberosAuthenticationEnabled()) && basicAuthUser != null && basicAuthPassword != null) {
            final HTTPBasicAuthFilter authFilter = new HTTPBasicAuthFilter(basicAuthUser, basicAuthPassword);
            client.addFilter(authFilter);
        }

        String activeServiceUrl = determineActiveServiceURL(baseUrls, client);
        atlasClientContext = new AtlasClientContext(baseUrls, client, ugi, doAsUser);
        service = client.resource(UriBuilder.fromUri(activeServiceUrl).build());
    }

    void sleepBetweenRetries() {
        try {
            Thread.sleep(getSleepBetweenRetriesMs());
        } catch (InterruptedException e) {
            LOG.error("Interrupted from sleeping between retries.", e);
        }
    }

    int getNumberOfRetries() {
        return configuration.getInt(AtlasBaseClient.ATLAS_CLIENT_HA_RETRIES_KEY, AtlasBaseClient.DEFAULT_NUM_RETRIES);
    }

    public InputStream exportData(AtlasExportRequest request) throws AtlasServiceException {
        try {
            return (InputStream) callAPI(EXPORT, Object.class, request);
        } catch (AtlasServiceException e) {
            LOG.error("error in export API call", e);
            throw new AtlasServiceException(e);
        }
    }

    public void exportData(AtlasExportRequest request, String absolutePath) throws AtlasServiceException {
        OutputStream fileOutputStream = null;
        InputStream inputStream = exportData(request);
        try {
            fileOutputStream = new FileOutputStream(new File(absolutePath));
            byte[] buffer = new byte[8 * 1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, bytesRead);
            }

            IOUtils.closeQuietly(inputStream);
            IOUtils.closeQuietly(fileOutputStream);

        } catch (Exception e) {
            LOG.error("error writing to file", e);
            throw new AtlasServiceException(e);
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    LOG.error("error closing file", e);
                    throw new AtlasServiceException(e);
                }
            }
        }
    }

    public AtlasImportResult importData(AtlasImportRequest request, String absoluteFilePath) throws AtlasServiceException {
        return performImportData(getImportRequestBodyPart(request),
                            new FileDataBodyPart(IMPORT_DATA_PARAMETER, new File(absoluteFilePath)));
    }

    public AtlasImportResult importData(AtlasImportRequest request, InputStream stream) throws AtlasServiceException {
        return performImportData(getImportRequestBodyPart(request),
                                new StreamDataBodyPart(IMPORT_DATA_PARAMETER, stream));
    }

    private AtlasImportResult performImportData(BodyPart requestPart, BodyPart filePart) throws AtlasServiceException {
        MultiPart multipartEntity = new FormDataMultiPart()
                .bodyPart(requestPart)
                .bodyPart(filePart);

        return callAPI(IMPORT, AtlasImportResult.class, multipartEntity);
    }


    private FormDataBodyPart getImportRequestBodyPart(AtlasImportRequest request) {
        return new FormDataBodyPart(IMPORT_REQUEST_PARAMTER, AtlasType.toJson(request), MediaType.APPLICATION_JSON_TYPE);
    }

    public AtlasServer getServer(String serverName) throws AtlasServiceException {
        API api = new API(String.format(ADMIN_SERVER_TEMPLATE, BASE_URI, serverName), HttpMethod.GET, Response.Status.OK);
        return callAPI(api, AtlasServer.class, null);
    }

    boolean isRetryableException(ClientHandlerException che) {
        return che.getCause().getClass().equals(IOException.class)
                || che.getCause().getClass().equals(ConnectException.class);
    }

    void handleClientHandlerException(ClientHandlerException che) {
        if (isRetryableException(che)) {
            atlasClientContext.getClient().destroy();
            LOG.warn("Destroyed current context while handling ClientHandlerEception.");
            LOG.warn("Will retry and create new context.");
            sleepBetweenRetries();
            initializeState(atlasClientContext.getBaseUrls(), atlasClientContext.getUgi(),
                    atlasClientContext.getDoAsUser());
            return;
        }
        throw che;
    }

    @VisibleForTesting
    ObjectNode callAPIWithRetries(API api, Object requestObject, ResourceCreator resourceCreator)
            throws AtlasServiceException {
        for (int i = 0; i < getNumberOfRetries(); i++) {
            WebResource resource = resourceCreator.createResource();
            try {
                LOG.debug("Using resource {} for {} times", resource.getURI(), i + 1);
                return callAPIWithResource(api, resource, requestObject, ObjectNode.class);
            } catch (ClientHandlerException che) {
                if (i == (getNumberOfRetries() - 1)) {
                    throw che;
                }
                LOG.warn("Handled exception in calling api {}", api.getNormalizedPath(), che);
                LOG.warn("Exception's cause: {}", che.getCause().getClass());
                handleClientHandlerException(che);
            }
        }
        throw new AtlasServiceException(api, new RuntimeException("Could not get response after retries."));
    }

    @VisibleForTesting
    void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    @VisibleForTesting
    void setService(WebResource resource) {
        this.service = resource;
    }

    private String selectActiveServerAddress(Client client, AtlasServerEnsemble serverEnsemble)
            throws AtlasServiceException {
        List<String> serverInstances = serverEnsemble.getMembers();
        String activeServerAddress = null;
        for (String serverInstance : serverInstances) {
            LOG.info("Trying with address {}", serverInstance);
            activeServerAddress = getAddressIfActive(client, serverInstance);
            if (activeServerAddress != null) {
                LOG.info("Found service {} as active service.", serverInstance);
                break;
            }
        }
        if (activeServerAddress != null)
            return activeServerAddress;
        else
            throw new AtlasServiceException(API_STATUS, new RuntimeException("Could not find any active instance"));
    }

    private String getAddressIfActive(Client client, String serverInstance) {
        String activeServerAddress = null;
        for (int i = 0; i < getNumberOfRetries(); i++) {
            try {
                service = client.resource(UriBuilder.fromUri(serverInstance).build());
                String adminStatus = getAdminStatus();
                if (StringUtils.equals(adminStatus, "ACTIVE")) {
                    activeServerAddress = serverInstance;
                    break;
                } else {
                    LOG.info("attempt #{}: Service {} - is not active. status={}", (i + 1), serverInstance, adminStatus);
                }
            } catch (Exception e) {
                LOG.error("attempt #{}: Service {} - could not get status", (i + 1), serverInstance, e);
            }
            sleepBetweenRetries();
        }
        return activeServerAddress;
    }

    private WebResource getResource(WebResource service, String path, String... pathParams) {
        WebResource resource = service.path(path);
        resource = appendPathParams(resource, pathParams);
        return resource;
    }

    private int getSleepBetweenRetriesMs() {
        return configuration.getInt(AtlasBaseClient.ATLAS_CLIENT_HA_SLEEP_INTERVAL_MS_KEY, AtlasBaseClient.DEFAULT_SLEEP_BETWEEN_RETRIES_MS);
    }

    // Modify URL to include the path params
    private WebResource getResource(WebResource service, API api, String... pathParams) {
        WebResource resource = service.path(api.getNormalizedPath());
        resource = appendPathParams(resource, pathParams);
        return resource;
    }

    private WebResource getResource(API api, String queryParamKey, List<String> queryParamValues) {
        WebResource resource = service.path(api.getNormalizedPath());
        for (String queryParamValue : queryParamValues) {
            if (StringUtils.isNotBlank(queryParamKey) && StringUtils.isNotBlank(queryParamValue)) {
                resource = resource.queryParam(queryParamKey, queryParamValue);
            }
        }
        return resource;
    }

    private WebResource appendPathParams(WebResource resource, String[] pathParams) {
        if (pathParams != null) {
            for (String pathParam : pathParams) {
                resource = resource.path(pathParam);
            }
        }
        return resource;
    }

    // Modify URL to include the query params
    private WebResource getResource(WebResource service, API api, MultivaluedMap<String, String> queryParams) {
        WebResource resource = service.path(api.getNormalizedPath());
        resource = appendQueryParams(queryParams, resource);
        return resource;
    }

    private WebResource appendQueryParams(MultivaluedMap<String, String> queryParams, WebResource resource) {
        if (null != queryParams && !queryParams.isEmpty()) {
            for (Map.Entry<String, List<String>> entry : queryParams.entrySet()) {
                for (String value : entry.getValue()) {
                    if (StringUtils.isNotBlank(value)) {
                        resource = resource.queryParam(entry.getKey(), value);
                    }
                }
            }
        }
        return resource;
    }

    public static class API {
        private final String method;
        private final String path;
        private final String consumes;
        private final String produces;
        private final Response.Status status;

        private static final Logger LOG = LoggerFactory.getLogger(API.class);

        public API(String path, String method, Response.Status status) {
            this(path, method, status, JSON_MEDIA_TYPE, MediaType.APPLICATION_JSON);
        }

        public API(String path, String method, Response.Status status, String consumes, String produces) {
            this.path = path;
            this.method = method;
            this.status = status;
            this.consumes = consumes;
            this.produces = produces;
        }

        public String getMethod() {
            return method;
        }

        public String getPath() {
            return path;
        }

        public String getNormalizedPath() {
            // This method used to return Paths.get(path).normalize().toString(), but
            // the use of Paths.get(path) on Windows produces a path with Windows
            // path separators (i.e. back-slashes) which is not valid for a URI
            // and will result in an HTTP 404 status code.
            URI uri = null;
            String resultUri = null;

            try {
                uri = new URI(path);
                if (uri != null) {
                    URI normalizedUri = uri.normalize();
                    resultUri = normalizedUri.toString();
                }
            } catch (Exception e) {
                LOG.error("getNormalizedPath() caught exception for path={}", path, e);
                resultUri = null;
            }

            return resultUri;
        }

        public Response.Status getExpectedStatus() {
            return status;
        }

        public String getConsumes() {
            return consumes;
        }

        public String getProduces() {
            return produces;
        }
    }

    /**
     * A class to capture input state while creating the client.
     *
     * The information here will be reused when the client is re-initialized on switch-over
     * in case of High Availability.
     */
    private class AtlasClientContext {
        private String[] baseUrls;
        private Client client;
        private String doAsUser;
        private UserGroupInformation ugi;

        public AtlasClientContext(String[] baseUrls, Client client, UserGroupInformation ugi, String doAsUser) {
            this.baseUrls = baseUrls;
            this.client = client;
            this.ugi = ugi;
            this.doAsUser = doAsUser;
        }

        public Client getClient() {
            return client;
        }

        public String[] getBaseUrls() {
            return baseUrls;
        }

        public String getDoAsUser() {
            return doAsUser;
        }

        public UserGroupInformation getUgi() {
            return ugi;
        }
    }
}
