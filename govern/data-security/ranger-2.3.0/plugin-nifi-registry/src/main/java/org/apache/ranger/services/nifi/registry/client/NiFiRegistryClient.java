/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ranger.services.nifi.registry.client;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.client.urlconnection.HTTPSProperties;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.plugin.client.BaseClient;
import org.apache.ranger.plugin.service.ResourceLookupContext;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.ws.rs.core.Response;
import java.security.cert.Certificate;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * Client to communicate with NiFi Registry and retrieve available resources.
 */
public class NiFiRegistryClient {

    private static final Logger LOG = LoggerFactory.getLogger(NiFiRegistryClient.class);

    static final String SUCCESS_MSG = "ConnectionTest Successful";
    static final String FAILURE_MSG = "Unable to retrieve any resources using given parameters. ";

    private final String url;
    private final SSLContext sslContext;
    private final HostnameVerifier hostnameVerifier;
    private final ObjectMapper mapper = new ObjectMapper();

    public NiFiRegistryClient(final String url, final SSLContext sslContext) {
        this.url = url;
        this.sslContext = sslContext;
        this.hostnameVerifier = new NiFiRegistryHostnameVerifier();
    }

    public HashMap<String, Object> connectionTest() {
        String errMsg = "";
        boolean connectivityStatus;
        HashMap<String, Object> responseData = new HashMap<>();

        try {
            final WebResource resource = getWebResource();
            final ClientResponse response = getResponse(resource, "application/json");

            if (LOG.isDebugEnabled()) {
                LOG.debug("Got response from NiFi with status code " + response.getStatus());
            }

            if (Response.Status.OK.getStatusCode() == response.getStatus()) {
                connectivityStatus = true;
            } else {
                connectivityStatus = false;
                errMsg = "Status Code = " + response.getStatus();
            }

        } catch (Exception e) {
            LOG.error("Connection to NiFi failed due to " + e.getMessage(), e);
            connectivityStatus = false;
            errMsg = e.getMessage();
        }

        if (connectivityStatus) {
            BaseClient.generateResponseDataMap(connectivityStatus, SUCCESS_MSG, SUCCESS_MSG, null, null, responseData);
        } else {
            BaseClient.generateResponseDataMap(connectivityStatus, FAILURE_MSG, FAILURE_MSG + errMsg, null, null, responseData);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Response Data - " + responseData);
        }

        return responseData;
    }

    public List<String> getResources(ResourceLookupContext context) throws Exception {
        final WebResource resource = getWebResource();
        final ClientResponse response = getResponse(resource, "application/json");

        if (Response.Status.OK.getStatusCode() != response.getStatus()) {
            String errorMsg = IOUtils.toString(response.getEntityInputStream());
            throw new Exception("Unable to retrieve resources from NiFi Registry due to: " + errorMsg);
        }

        JsonNode rootNode = mapper.readTree(response.getEntityInputStream());
        if (rootNode == null) {
            throw new Exception("Unable to retrieve resources from NiFi Registry");
        }

        List<String> identifiers = rootNode.findValuesAsText("identifier");

        final String userInput = context.getUserInput();
        if (StringUtils.isBlank(userInput)) {
            return identifiers;
        } else {
            List<String> filteredIdentifiers = new ArrayList<>();

            for (String identifier : identifiers) {
                if (identifier.contains(userInput)) {
                    filteredIdentifiers.add(identifier);
                }
            }

            return filteredIdentifiers;
        }
    }

    protected WebResource getWebResource() {
        final ClientConfig config = new DefaultClientConfig();
        if (sslContext != null) {
            config.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES,
                    new HTTPSProperties(hostnameVerifier, sslContext));
        }

        final Client client = Client.create(config);
        return client.resource(url);
    }

    protected ClientResponse getResponse(WebResource resource, String accept) {
        return resource.accept(accept).get(ClientResponse.class);
    }

    public String getUrl() {
        return url;
    }

    public SSLContext getSslContext() {
        return sslContext;
    }

    public HostnameVerifier getHostnameVerifier() {
        return hostnameVerifier;
    }

    /**
     * Custom hostname verifier that checks subject alternative names against the hostname of the URI.
     */
    private static class NiFiRegistryHostnameVerifier implements HostnameVerifier {

        @Override
        public boolean verify(final String hostname, final SSLSession ssls) {
            try {
                for (final Certificate peerCertificate : ssls.getPeerCertificates()) {
                    if (peerCertificate instanceof X509Certificate) {
                        final X509Certificate x509Cert = (X509Certificate) peerCertificate;
                        final List<String> subjectAltNames = getSubjectAlternativeNames(x509Cert);
                        if (subjectAltNames.contains(hostname.toLowerCase())) {
                            return true;
                        }
                    }
                }
            } catch (final SSLPeerUnverifiedException | CertificateParsingException ex) {
                LOG.warn("Hostname Verification encountered exception verifying hostname due to: " + ex, ex);
            }

            return false;
        }

        private List<String> getSubjectAlternativeNames(final X509Certificate certificate) throws CertificateParsingException {
            final List<String> result = new ArrayList<>();
            final Collection<List<?>> altNames = certificate.getSubjectAlternativeNames();
            if (altNames == null) {
                return result;
            }

			for (final List<?> generalName : altNames) {
                /**
                 * generalName has the name type as the first element a String or byte array for the second element. We return any general names that are String types.
                 *
                 * We don't inspect the numeric name type because some certificates incorrectly put IPs and DNS names under the wrong name types.
                 */
				if (generalName.size() > 1) {
					final Object value = generalName.get(1);
					if (value instanceof String) {
						result.add(((String) value).toLowerCase());
					}
				}

            }
            return result;
        }
    }

}
