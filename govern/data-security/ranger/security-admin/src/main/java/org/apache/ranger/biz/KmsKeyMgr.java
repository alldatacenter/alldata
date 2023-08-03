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

package org.apache.ranger.biz;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MediaType;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.PredicateUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.security.ProviderUtils;
import org.apache.hadoop.security.SecureClientLogin;
import org.apache.hadoop.security.authentication.util.KerberosName;
import org.apache.ranger.common.ContextUtil;
import org.apache.ranger.common.MessageEnums;
import org.apache.ranger.plugin.util.PasswordUtils;
import org.apache.ranger.common.PropertiesUtil;
import org.apache.ranger.common.RESTErrorUtil;
import org.apache.ranger.common.RangerConfigUtil;
import org.apache.ranger.common.SortField;
import org.apache.ranger.common.StringUtil;
import org.apache.ranger.db.RangerDaoManagerBase;
import org.apache.ranger.entity.XXService;
import org.apache.ranger.entity.XXServiceConfigMap;
import org.apache.ranger.plugin.model.RangerService;
import org.apache.ranger.plugin.util.KeySearchFilter;
import org.apache.ranger.view.VXKmsKey;
import org.apache.ranger.view.VXKmsKeyList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

@Component
public class KmsKeyMgr {

	private static final Logger logger = LoggerFactory.getLogger(KmsKeyMgr.class);
	
	private static final String KMS_KEY_LIST_URI  		= "v1/keys/names";				//GET
	private static final String KMS_ADD_KEY_URI  		= "v1/keys";					//POST
	private static final String KMS_ROLL_KEY_URI 		= "v1/key/${alias}";			//POST
	private static final String KMS_DELETE_KEY_URI 		= "v1/key/${alias}";			//DELETE
	private static final String KMS_KEY_METADATA_URI 	= "v1/key/${alias}/_metadata";  //GET
	private static final String KMS_URL_CONFIG 			= "provider";
	private static final String KMS_PASSWORD 			= "password";
	private static final String KMS_USERNAME 			= "username";
	private static Map<String, String> providerList = new HashMap<String, String>();
	private static int nextProvider = 0;
	static final String NAME_RULES = "hadoop.security.auth_to_local";
	static final String RANGER_AUTH_TYPE = "hadoop.security.authentication";	
	private static final String KERBEROS_TYPE = "kerberos";
    private static final String ADMIN_USER_PRINCIPAL = "ranger.admin.kerberos.principal";
    private static final String ADMIN_USER_KEYTAB = "ranger.admin.kerberos.keytab";
    static final String HOST_NAME = "ranger.service.host";

	@Autowired
	ServiceDBStore svcStore;	
	
	@Autowired
	RESTErrorUtil restErrorUtil;
	
	@Autowired
	RangerConfigUtil configUtil;
	
	@Autowired
	RangerDaoManagerBase rangerDaoManagerBase;

        @Autowired
        RangerBizUtil rangerBizUtil;

	@SuppressWarnings("unchecked")
	public VXKmsKeyList searchKeys(HttpServletRequest request, String repoName) throws Exception {
		String providers[] = null;
		try {
			providers = getKMSURL(repoName);
		} catch (Exception e) {
			logger.error("getKey(" + repoName + ") failed", e);
		}
		List<VXKmsKey> vXKeys = new ArrayList<VXKmsKey>();
		VXKmsKeyList vxKmsKeyList = new VXKmsKeyList();
		List<String> keys = null;
		String connProvider = null;
		boolean isKerberos=false;
		try {
			isKerberos = checkKerberos();
		} catch (Exception e1) {
			logger.error("checkKerberos(" + repoName + ") failed", e1);
		}
		if(providers!=null){
			for (int i = 0; i < providers.length; i++) {
				Client c = getClient();
				String currentUserLoginId = StringUtil.getUTFEncodedString(ContextUtil.getCurrentUserLoginId());
				String keyLists = KMS_KEY_LIST_URI.replaceAll(
						Pattern.quote("${userName}"), currentUserLoginId);
				connProvider = providers[i];
				String uri = providers[i]
						+ (providers[i].endsWith("/") ? keyLists : ("/" + keyLists));
				if(!isKerberos){
					uri = uri.concat("?user.name="+currentUserLoginId);
				}else{
					uri = uri.concat("?doAs="+currentUserLoginId);
				}

				final WebResource r = c.resource(uri);
				try {
					String response = null;
					if(!isKerberos){
						response = r.accept(MediaType.APPLICATION_JSON_TYPE).type(MediaType.APPLICATION_JSON_TYPE).get(String.class);
					}else{
						Subject sub = getSubjectForKerberos(repoName);
						response = Subject.doAs(sub, new PrivilegedAction<String>() {
							@Override
							public String run() {
								return r.accept(MediaType.APPLICATION_JSON_TYPE).type(MediaType.APPLICATION_JSON_TYPE).get(String.class);
							}
						});
					}
					Gson gson = new GsonBuilder().create();
					logger.debug(" Search Key RESPONSE: [" + response + "]");
					keys = gson.fromJson(response, List.class);
					Collections.sort(keys);
					VXKmsKeyList vxKmsKeyList2 = new VXKmsKeyList();
					List<VXKmsKey> vXKeys2 = new ArrayList<VXKmsKey>();
					for (String name : keys) {
						VXKmsKey key = new VXKmsKey();
						key.setName(name);
						vXKeys2.add(key);
					}
					vxKmsKeyList2.setVXKeys(vXKeys2);
					vxKmsKeyList = getFilteredKeyList(request, vxKmsKeyList2);
					break;
				} catch (Exception e) {
					if (e instanceof UniformInterfaceException || i == providers.length - 1)
						throw e;
					else
						continue;
				}
			}
		}
		//details
		if (vxKmsKeyList != null && vxKmsKeyList.getVXKeys() != null && !vxKmsKeyList.getVXKeys().isEmpty()) {
			List<VXKmsKey> lstKMSKey = vxKmsKeyList.getVXKeys();
			int startIndex=restErrorUtil.parseInt(
					request.getParameter("startIndex"), 0,
					"Invalid value for parameter startIndex",
					MessageEnums.INVALID_INPUT_DATA, null, "startIndex");
			startIndex = startIndex < 0 ? 0 : startIndex;
			
			int pageSize=restErrorUtil.parseInt(
					request.getParameter("pageSize"), 0,
					"Invalid value for parameter pageSize",
					MessageEnums.INVALID_INPUT_DATA, null, "pageSize");
			pageSize = pageSize < 0 ? 0 : pageSize;
			
			vxKmsKeyList.setResultSize(lstKMSKey.size());
			vxKmsKeyList.setTotalCount(lstKMSKey.size());
			if((startIndex+pageSize) <= lstKMSKey.size()){
				lstKMSKey = lstKMSKey.subList(startIndex, (startIndex+pageSize));}
			else{
				startIndex = startIndex >= lstKMSKey.size() ? 0 : startIndex;
				lstKMSKey = lstKMSKey.subList(startIndex, lstKMSKey.size());
			}
			if(CollectionUtils.isNotEmpty(lstKMSKey)){
				for (VXKmsKey kmsKey : lstKMSKey) {
					if(kmsKey!=null){
						VXKmsKey key = getKeyFromUri(connProvider, kmsKey.getName(), isKerberos, repoName);
						vXKeys.add(key);
					}
				}
			}
			vxKmsKeyList.setStartIndex(startIndex);
			vxKmsKeyList.setPageSize(pageSize);
		}
		if(vxKmsKeyList!=null){
			vxKmsKeyList.setVXKeys(vXKeys);
		}
		return vxKmsKeyList;
	}

	public VXKmsKey rolloverKey(String provider, VXKmsKey vXKey) throws Exception{
		String providers[] = null;
                rangerBizUtil.blockAuditorRoleUser();
		try {
			providers = getKMSURL(provider);
		} catch (Exception e) {
			logger.error("rolloverKey(" + provider + ", " + vXKey.getName() + ") failed", e);
		}
		VXKmsKey ret = null;
		boolean isKerberos=false;
		try {
			isKerberos = checkKerberos();
		} catch (Exception e1) {
			logger.error("checkKerberos(" + provider + ") failed", e1);
		}
		if(providers!=null){
			for (int i = 0; i < providers.length; i++) {
				Client c = getClient();
				String rollRest = KMS_ROLL_KEY_URI.replaceAll(Pattern.quote("${alias}"), vXKey.getName());
				String currentUserLoginId = StringUtil.getUTFEncodedString(ContextUtil.getCurrentUserLoginId());
				String uri = providers[i] + (providers[i].endsWith("/") ? rollRest : ("/" + rollRest));
				if(!isKerberos){
					uri = uri.concat("?user.name="+currentUserLoginId);
				}else{
					uri = uri.concat("?doAs="+currentUserLoginId);
				}
				final WebResource r = c.resource(uri);
				Gson gson = new GsonBuilder().create();
				final String jsonString = gson.toJson(vXKey);
				try {
					String response = null;
					if(!isKerberos){
					 response = r.accept(MediaType.APPLICATION_JSON_TYPE).type(MediaType.APPLICATION_JSON_TYPE).post(String.class, jsonString);}
					else{
						Subject sub = getSubjectForKerberos(provider);
			            response = Subject.doAs(sub, new PrivilegedAction<String>() {
							@Override
							public String run() {
		                        return r.accept(MediaType.APPLICATION_JSON_TYPE).type(MediaType.APPLICATION_JSON_TYPE).post(String.class, jsonString);
							}
						});
		            }
					logger.debug("Roll RESPONSE: [" + response + "]");
					ret = gson.fromJson(response, VXKmsKey.class);
					break;
				} catch (Exception e) {
					if (e instanceof UniformInterfaceException || i == providers.length - 1)
						throw e;
					else
						continue;
				}
			}
		}
		return ret;
	}
	
	public void deleteKey(String provider, String name) throws Exception{
		String providers[] = null;
                rangerBizUtil.blockAuditorRoleUser();
		try {
			providers = getKMSURL(provider);
		} catch (Exception e) {
			logger.error("deleteKey(" + provider + ", " + name + ") failed", e);
		}
		boolean isKerberos=false;
		try {
			isKerberos = checkKerberos();
		} catch (Exception e1) {
			logger.error("checkKerberos(" + provider + ") failed", e1);
		}
		if(providers!=null){
			for (int i = 0; i < providers.length; i++) {
				Client c = getClient();
				String deleteRest = KMS_DELETE_KEY_URI.replaceAll(Pattern.quote("${alias}"), name);
				String currentUserLoginId = StringUtil.getUTFEncodedString(ContextUtil.getCurrentUserLoginId());
				String uri = providers[i] + (providers[i].endsWith("/") ? deleteRest : ("/" + deleteRest));
				if(!isKerberos){
						uri = uri.concat("?user.name="+currentUserLoginId);
				}else{
					uri = uri.concat("?doAs="+currentUserLoginId);
				}
				final WebResource r = c.resource(uri);
				try {
					String response = null;
					if(!isKerberos){
						response = r.delete(String.class);
					}else{
						Subject sub = getSubjectForKerberos(provider);
						response = Subject.doAs(sub, new PrivilegedAction<String>() {
							@Override
							public String run() {
								return r.delete(String.class);
							}
						});
					}
					logger.debug("delete RESPONSE: [" + response + "]");
					break;
				} catch (Exception e) {
					if (e instanceof UniformInterfaceException || i == providers.length - 1)
						throw e;
					else
						continue;
				}
			}
		}
	}

	public VXKmsKey createKey(String provider, VXKmsKey vXKey) throws Exception{
		String providers[] = null;
                rangerBizUtil.blockAuditorRoleUser();
		try {
			providers = getKMSURL(provider);
		} catch (Exception e) {
			logger.error("createKey(" + provider + ", " + vXKey.getName()
					+ ") failed", e);
		}
		VXKmsKey ret = null;
		boolean isKerberos=false;
		try {
			isKerberos = checkKerberos();
		} catch (Exception e1) {
			logger.error("checkKerberos(" + provider + ") failed", e1);
		}
		if(providers!=null){
			for (int i = 0; i < providers.length; i++) {
				Client c = getClient();
				String currentUserLoginId = StringUtil.getUTFEncodedString(ContextUtil.getCurrentUserLoginId());
				String uri = providers[i] + (providers[i].endsWith("/") ? KMS_ADD_KEY_URI : ("/" + KMS_ADD_KEY_URI));
				if(!isKerberos){
					uri = uri.concat("?user.name="+currentUserLoginId);
				}else{
					uri = uri.concat("?doAs="+currentUserLoginId);
				}
				final WebResource r = c.resource(uri);
				Gson gson = new GsonBuilder().create();
				final String jsonString = gson.toJson(vXKey);
				try {
					String response = null;
					if(!isKerberos){
						response = r.accept(MediaType.APPLICATION_JSON_TYPE).type(MediaType.APPLICATION_JSON_TYPE).post(String.class, jsonString);
					}else{
							Subject sub = getSubjectForKerberos(provider);
							response = Subject.doAs(sub, new PrivilegedAction<String>() {
								@Override
								public String run() {
									return r.accept(MediaType.APPLICATION_JSON_TYPE).type(MediaType.APPLICATION_JSON_TYPE).post(String.class, jsonString);
								}
							});
					}
					logger.debug("Create RESPONSE: [" + response + "]");
					ret = gson.fromJson(response, VXKmsKey.class);
					return ret;
				} catch (Exception e) {
					if (e instanceof UniformInterfaceException || i == providers.length - 1)
						throw e;
					else
						continue;
				}
			}
		}
		return ret;	
	}
	
	public VXKmsKey getKey(String provider, String name) throws Exception{
		String providers[] = null;
		try {
			providers = getKMSURL(provider);
		} catch (Exception e) {
			logger.error("getKey(" + provider + ", " + name + ") failed", e);
		}
		boolean isKerberos=false;
		try {
			isKerberos = checkKerberos();
		} catch (Exception e1) {
			logger.error("checkKerberos(" + provider + ") failed", e1);
		}
		if(providers!=null){
			for (int i = 0; i < providers.length; i++) {
				Client c = getClient();
				String keyRest = KMS_KEY_METADATA_URI.replaceAll(Pattern.quote("${alias}"), name);
				String currentUserLoginId = StringUtil.getUTFEncodedString(ContextUtil.getCurrentUserLoginId());
				String uri = providers[i] + (providers[i].endsWith("/") ? keyRest : ("/" + keyRest));
				if(!isKerberos){
						uri = uri.concat("?user.name="+currentUserLoginId);
				}else{
					uri = uri.concat("?doAs="+currentUserLoginId);
				}
				final WebResource r = c.resource(uri);
				try {
					String response = null;
					if(!isKerberos){
						response = r.accept(MediaType.APPLICATION_JSON_TYPE).type(MediaType.APPLICATION_JSON_TYPE).get(String.class);
					}else{
						Subject sub = getSubjectForKerberos(provider);
						response = Subject.doAs(sub, new PrivilegedAction<String>() {
							@Override
							public String run() {
								return r.accept(MediaType.APPLICATION_JSON_TYPE).type(MediaType.APPLICATION_JSON_TYPE).get(String.class);
							}
						});
					}
					Gson gson = new GsonBuilder().create();
					logger.debug("RESPONSE: [" + response + "]");
					VXKmsKey key = gson.fromJson(response, VXKmsKey.class);
					return key;
				} catch (Exception e) {
					if (e instanceof UniformInterfaceException || i == providers.length - 1)
						throw e;
					else
						continue;
				}
			}
		}
		return null;
	}

	public VXKmsKey getKeyFromUri(String provider, String name, boolean isKerberos, String repoName) throws Exception {
		Client c = getClient();
		String keyRest = KMS_KEY_METADATA_URI.replaceAll(Pattern.quote("${alias}"), name);
		String currentUserLoginId = StringUtil.getUTFEncodedString(ContextUtil.getCurrentUserLoginId());
		String uri = provider + (provider.endsWith("/") ? keyRest : ("/" + keyRest));
		if(!isKerberos){
			uri = uri.concat("?user.name="+currentUserLoginId);
		}else{
			uri = uri.concat("?doAs="+currentUserLoginId);
		}
		final WebResource r = c.resource(uri);
		String response = null;
		if(!isKerberos){
			response = r.accept(MediaType.APPLICATION_JSON_TYPE).type(MediaType.APPLICATION_JSON_TYPE).get(String.class);
		}else{
			Subject sub = getSubjectForKerberos(repoName);
			response = Subject.doAs(sub, new PrivilegedAction<String>() {
				@Override
				public String run() {
					return r.accept(MediaType.APPLICATION_JSON_TYPE).type(MediaType.APPLICATION_JSON_TYPE).get(String.class);
				}				
			});
		}
		Gson gson = new GsonBuilder().create();
		logger.debug("RESPONSE: [" + response + "]");
		VXKmsKey key = gson.fromJson(response, VXKmsKey.class);
		return key;
	}
	
	private String[] getKMSURL(String name) throws Exception{
		String providers[] = null;
		RangerService rangerService = null;
		try {
			rangerService = svcStore.getServiceByName(name);
			if(rangerService!=null){
				String kmsUrl = rangerService.getConfigs().get(KMS_URL_CONFIG);
				String dbKmsUrl = kmsUrl;
				if(providerList.containsKey(kmsUrl)){
					kmsUrl = providerList.get(kmsUrl);
				}else{
					providerList.put(kmsUrl, kmsUrl);
				}
				providers = createProvider(dbKmsUrl,kmsUrl);
			}else{
				throw new Exception("Service " + name + " not found");
			}
		} catch (Exception excp) {
			logger.error("getServiceByName(" + name + ") failed", excp);
			throw new Exception("getServiceByName(" + name + ") failed", excp);
		}
		if (providers == null) {
			throw new Exception("Providers for service " + name + " not found");
		}
		return providers;
	}
	
	private String[] createProvider(String dbKmsUrl, String uri) throws IOException,URISyntaxException {		
		URI providerUri = new URI(uri);
		URL origUrl = new URL(extractKMSPath(providerUri).toString());
		String authority = origUrl.getAuthority();
		// 	check for ';' which delimits the backup hosts
		if (Strings.isNullOrEmpty(authority)) {
			throw new IOException("No valid authority in kms uri [" + origUrl+ "]");
		}
		// 	Check if port is present in authority
		// 	In the current scheme, all hosts have to run on the same port
		int port = -1;
		String hostsPart = authority;
		if (authority.contains(":")) {
			String[] t = authority.split(":");
			try {
				port = Integer.parseInt(t[1]);
			} catch (Exception e) {
				throw new IOException("Could not parse port in kms uri ["
				+ origUrl + "]");
			}
			hostsPart = t[0];
		}
		return createProvider(dbKmsUrl, providerUri, origUrl, port, hostsPart);
	}

	private static Path extractKMSPath(URI uri) throws MalformedURLException,IOException {
		return ProviderUtils.unnestUri(uri);
	}

	private String[] createProvider(String dbkmsUrl, URI providerUri, URL origUrl, int port,
			String hostsPart) throws IOException {
		String[] hosts = hostsPart.split(";");
		String[] providers = new String[hosts.length];
		if (hosts.length == 1) {
			providers[0] = origUrl.toString();
		} else {
			String providerNext=providerUri.getScheme()+"://"+origUrl.getProtocol()+"@";
			for(int i=nextProvider; i<hosts.length; i++){
				providerNext = providerNext+hosts[i];
				if(i!=(hosts.length-1)){
					providerNext = providerNext+";";
				}
			}
			for(int i=0; i<nextProvider && i<hosts.length; i++){
				providerNext = providerNext+";"+hosts[i];
			}
			if(nextProvider != hosts.length-1){
				nextProvider = nextProvider+1;
			}else{
				nextProvider = 0;
			}
			providerNext = providerNext +":"+port+origUrl.getPath();
			providerList.put(dbkmsUrl, providerNext);
			for (int i = 0; i < hosts.length; i++) {
				try {
					String url = origUrl.getProtocol()+"://"+hosts[i]+":"+port+origUrl.getPath();
					providers[i] = new URI(url).toString();
				} catch (URISyntaxException e) {
					throw new IOException("Could not Prase KMS URL..", e);
				}
			}
		}
		return providers;
	}
	
	private Subject getSubjectForKerberos(String provider) throws Exception {
		String userName = getKMSUserName(provider);
		String password = getKMSPassword(provider);
		String nameRules = PropertiesUtil.getProperty(NAME_RULES);
		if (StringUtils.isEmpty(nameRules)) {
			KerberosName.setRules("DEFAULT");
			nameRules = "DEFAULT";
		} else {
			KerberosName.setRules(nameRules);
		}
		Subject sub = new Subject();
		String rangerPrincipal = SecureClientLogin.getPrincipal(PropertiesUtil.getProperty(ADMIN_USER_PRINCIPAL), PropertiesUtil.getProperty(HOST_NAME));
		if (checkKerberos()) {
			if (SecureClientLogin.isKerberosCredentialExists(rangerPrincipal, PropertiesUtil.getProperty(ADMIN_USER_KEYTAB))) {
				sub = SecureClientLogin.loginUserFromKeytab(rangerPrincipal, PropertiesUtil.getProperty(ADMIN_USER_KEYTAB), nameRules);
			} else {
				sub = SecureClientLogin.loginUserWithPassword(userName, password);
			}
		} else {
			sub = SecureClientLogin.login(userName);
		}
		return sub;
	}

	private String getKMSPassword(String srvName) throws Exception {
		XXService rangerService = rangerDaoManagerBase.getXXService().findByName(srvName);
		XXServiceConfigMap xxConfigMap = rangerDaoManagerBase.getXXServiceConfigMap().findByServiceAndConfigKey(rangerService.getId(), KMS_PASSWORD);
		String encryptedPwd = xxConfigMap.getConfigvalue();
		String pwd = PasswordUtils.decryptPassword(encryptedPwd);
		return pwd;
	}

	private String getKMSUserName(String srvName) throws Exception {
		RangerService rangerService = null;
		rangerService = svcStore.getServiceByName(srvName);
		return rangerService.getConfigs().get(KMS_USERNAME);
	}

	private boolean checkKerberos() throws Exception {
		if(KERBEROS_TYPE.equalsIgnoreCase(PropertiesUtil.getProperty(RANGER_AUTH_TYPE, "simple"))){
			return true;
		}else{
			return false;
		}
	}

	private synchronized Client getClient() {
		Client ret = null;
		ClientConfig cc = new DefaultClientConfig();
		cc.getProperties().put(ClientConfig.PROPERTY_FOLLOW_REDIRECTS, true);
		ret = Client.create(cc);	
		return ret;
	}	
	
	public VXKmsKeyList getFilteredKeyList(HttpServletRequest request, VXKmsKeyList vXKmsKeyList){
		List<SortField> sortFields = new ArrayList<SortField>();
		sortFields.add(new SortField(KeySearchFilter.KEY_NAME, KeySearchFilter.KEY_NAME));

		KeySearchFilter filter = getKeySearchFilter(request, sortFields);
		
		Predicate pred = getPredicate(filter);
		
		if(pred != null) {
			CollectionUtils.filter(vXKmsKeyList.getVXKeys(), pred);
		}
		return vXKmsKeyList;
	}
	
	private Predicate getPredicate(KeySearchFilter filter) {
		if(filter == null || filter.isEmpty()) {
			return null;
		}

		List<Predicate> predicates = new ArrayList<Predicate>();

		addPredicateForKeyName(filter.getParam(KeySearchFilter.KEY_NAME), predicates);
		
		Predicate ret = CollectionUtils.isEmpty(predicates) ? null : PredicateUtils.allPredicate(predicates);

		return ret;
	}
	
	private Predicate addPredicateForKeyName(final String name, List<Predicate> predicates) {
			if(StringUtils.isEmpty(name)) {
				return null;
			}

			Predicate ret = new Predicate() {
				@Override
				public boolean evaluate(Object object) {
					if(object == null) {
						return false;
					}

					boolean ret = false;

					if(object instanceof VXKmsKey) {
						VXKmsKey vXKmsKey = (VXKmsKey)object;
						if(StringUtils.isEmpty(vXKmsKey.getName())) {
							ret = true;
						}else{
							ret = vXKmsKey.getName().contains(name);
						}
					} else {
						ret = true;
					}

					return ret;
				}
			};

			if(predicates != null) {
				predicates.add(ret);
			}
				
			return ret;
	}
		
	private KeySearchFilter getKeySearchFilter(HttpServletRequest request, List<SortField> sortFields) {
		if (request == null) {
			return null;
		}
		KeySearchFilter ret = new KeySearchFilter();

		if (MapUtils.isEmpty(request.getParameterMap())) {
			ret.setParams(new HashMap<String, String>());
		}

		ret.setParam(KeySearchFilter.KEY_NAME, request.getParameter(KeySearchFilter.KEY_NAME));
		extractCommonCriteriasForFilter(request, ret, sortFields);
		return ret;
	}
	
	private KeySearchFilter extractCommonCriteriasForFilter(HttpServletRequest request, KeySearchFilter ret, List<SortField> sortFields) {
		int startIndex = restErrorUtil.parseInt(request.getParameter(KeySearchFilter.START_INDEX), 0,
				"Invalid value for parameter startIndex", MessageEnums.INVALID_INPUT_DATA, null,
				KeySearchFilter.START_INDEX);
		ret.setStartIndex(startIndex);

		int pageSize = restErrorUtil.parseInt(request.getParameter(KeySearchFilter.PAGE_SIZE),
				configUtil.getDefaultMaxRows(), "Invalid value for parameter pageSize",
				MessageEnums.INVALID_INPUT_DATA, null, KeySearchFilter.PAGE_SIZE);
		ret.setMaxRows(pageSize);

		ret.setGetCount(restErrorUtil.parseBoolean(request.getParameter("getCount"), true));
		String sortBy = restErrorUtil.validateString(request.getParameter(KeySearchFilter.SORT_BY),
				StringUtil.VALIDATION_ALPHA, "Invalid value for parameter sortBy", MessageEnums.INVALID_INPUT_DATA,
				null, KeySearchFilter.SORT_BY);
		boolean sortSet = false;
		if (!StringUtils.isEmpty(sortBy)) {
			for (SortField sortField : sortFields) {
				if (sortField.getParamName().equalsIgnoreCase(sortBy)) {
					ret.setSortBy(sortField.getParamName());
					String sortType = restErrorUtil.validateString(request.getParameter("sortType"),
							StringUtil.VALIDATION_ALPHA, "Invalid value for parameter sortType",
							MessageEnums.INVALID_INPUT_DATA, null, "sortType");
					ret.setSortType(sortType);
					sortSet = true;
					break;
				}
			}
		}

		if (!sortSet && !StringUtils.isEmpty(sortBy)) {
			logger.info("Invalid or unsupported sortBy field passed. sortBy=" + sortBy, new Throwable());
		}
		
		if(ret.getParams() == null) {
			ret.setParams(new HashMap<String, String>());
		}
		return ret;
	}
}
