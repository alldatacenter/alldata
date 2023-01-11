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

package org.apache.ranger.elasticsearch;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthSchemeProvider;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.config.Lookup;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.impl.auth.SPNegoSchemeFactory;
import org.apache.ranger.audit.destination.ElasticSearchAuditDestination;
import org.apache.ranger.audit.provider.MiscUtil;
import org.apache.ranger.authorization.credutils.CredentialsProviderUtil;
import org.apache.ranger.authorization.credutils.kerberos.KerberosCredentialsProvider;
import org.apache.ranger.common.PropertiesUtil;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosTicket;

import java.io.File;
import java.security.PrivilegedActionException;
import java.util.Date;
import java.util.Locale;

import static org.apache.ranger.audit.destination.ElasticSearchAuditDestination.*;

/**
 * This class initializes the ElasticSearch client
 *
 */
@Component
public class ElasticSearchMgr {

	private static final Logger logger = LoggerFactory.getLogger(ElasticSearchMgr.class);
	public String index;
	Subject subject;
	String user;
	String password;

	synchronized void connect() {
		if (client == null) {
			synchronized (ElasticSearchAuditDestination.class) {
				if (client == null) {

					String urls = PropertiesUtil.getProperty(CONFIG_PREFIX + "." + CONFIG_URLS);
					String protocol = PropertiesUtil.getProperty(CONFIG_PREFIX + "." + CONFIG_PROTOCOL, "http");
					user = PropertiesUtil.getProperty(CONFIG_PREFIX + "." + CONFIG_USER, "");
					password = PropertiesUtil.getProperty(CONFIG_PREFIX + "." + CONFIG_PWRD, "");
					int port = Integer.parseInt(PropertiesUtil.getProperty(CONFIG_PREFIX + "." + CONFIG_PORT));
					this.index = PropertiesUtil.getProperty(CONFIG_PREFIX + "." + CONFIG_INDEX, "ranger_audits");
					String parameterString = String.format(Locale.ROOT,"User:%s, %s://%s:%s/%s", user, protocol, urls, port, index);
					logger.info("Initializing ElasticSearch " + parameterString);
					if (urls != null) {
						urls = urls.trim();
					}
					if (StringUtils.isBlank(urls) || "NONE".equalsIgnoreCase(urls.trim())) {
						logger.info(String.format("Clearing URI config value: %s", urls));
						urls = null;
					}

					try {
						if (StringUtils.isNotBlank(user) && StringUtils.isNotBlank(password) && password.contains("keytab") && new File(password).exists()) {
							subject = CredentialsProviderUtil.login(user, password);
						}
						RestClientBuilder restClientBuilder =
								getRestClientBuilder(urls, protocol, user, password, port);
						client = new RestHighLevelClient(restClientBuilder);
					} catch (Throwable t) {
						logger.error("Can't connect to ElasticSearch: " + parameterString, t);
					}
				}
			}
		}
	}

	public static RestClientBuilder getRestClientBuilder(String urls, String protocol, String user, String password, int port) {
		RestClientBuilder restClientBuilder = RestClient.builder(
				MiscUtil.toArray(urls, ",").stream()
						.map(x -> new HttpHost(x, port, protocol))
						.<HttpHost>toArray(i -> new HttpHost[i])
		);
		if (StringUtils.isNotBlank(user) && StringUtils.isNotBlank(password) && !user.equalsIgnoreCase("NONE") && !password.equalsIgnoreCase("NONE")) {
			if (password.contains("keytab") && new File(password).exists()) {
				final KerberosCredentialsProvider credentialsProvider =
						CredentialsProviderUtil.getKerberosCredentials(user, password);
				Lookup<AuthSchemeProvider> authSchemeRegistry = RegistryBuilder.<AuthSchemeProvider>create()
						.register(AuthSchemes.SPNEGO, new SPNegoSchemeFactory()).build();
				restClientBuilder.setHttpClientConfigCallback(clientBuilder -> {
					clientBuilder.setDefaultCredentialsProvider(credentialsProvider);
					clientBuilder.setDefaultAuthSchemeRegistry(authSchemeRegistry);
					return clientBuilder;
				});
			} else {
				final CredentialsProvider credentialsProvider =
						CredentialsProviderUtil.getBasicCredentials(user, password);
				restClientBuilder.setHttpClientConfigCallback(clientBuilder ->
						clientBuilder.setDefaultCredentialsProvider(credentialsProvider));
			}
		} else {
			logger.error("ElasticSearch Credentials not provided!!");
			final CredentialsProvider credentialsProvider = null;
			restClientBuilder.setHttpClientConfigCallback(clientBuilder ->
					clientBuilder.setDefaultCredentialsProvider(credentialsProvider));
		}
		return restClientBuilder;
	}

	RestHighLevelClient client = null;
	public RestHighLevelClient getClient() {
		if (client != null && subject != null) {
			KerberosTicket ticket = CredentialsProviderUtil.getTGT(subject);
			try {
				if (new Date().getTime() > ticket.getEndTime().getTime()){
					client = null;
					CredentialsProviderUtil.ticketExpireTime80 = 0;
					connect();
				} else if (CredentialsProviderUtil.ticketWillExpire(ticket)) {
					subject = CredentialsProviderUtil.login(user, password);
				}
			} catch (PrivilegedActionException e) {
				logger.error("PrivilegedActionException:", e);
				throw new RuntimeException(e);
			}
			return client;
		} else {
			connect();
		}
		return client;
	}

}
