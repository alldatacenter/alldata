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

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.security.SecureClientLogin;
import org.apache.ranger.common.PropertiesUtil;
import org.apache.ranger.common.TimedExecutor;
import org.apache.ranger.db.XXGroupUserDao;
import org.apache.ranger.entity.XXGroupUser;
import org.apache.ranger.plugin.client.HadoopConfigHolder;
import org.apache.ranger.plugin.client.HadoopException;
import org.apache.ranger.plugin.model.RangerSecurityZone;
import org.apache.ranger.plugin.model.RangerService;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.service.RangerBaseService;
import org.apache.ranger.plugin.service.ResourceLookupContext;
import org.apache.ranger.plugin.store.EmbeddedServiceDefsUtil;
import org.apache.ranger.plugin.store.ServiceStore;
import org.apache.ranger.service.RangerServiceService;
import org.apache.ranger.services.tag.RangerServiceTag;
import org.apache.ranger.view.VXMessage;
import org.apache.ranger.view.VXResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
public class ServiceMgr {

	private static final Logger LOG = LoggerFactory.getLogger(ServiceMgr.class);
	
	private static final String LOOKUP_PRINCIPAL = "ranger.lookup.kerberos.principal";
	private static final String LOOKUP_KEYTAB = "ranger.lookup.kerberos.keytab";
    private static final String ADMIN_USER_PRINCIPAL = "ranger.admin.kerberos.principal";
    private static final String ADMIN_USER_KEYTAB = "ranger.admin.kerberos.keytab";
	private static final String AUTHENTICATION_TYPE = "hadoop.security.authentication";
	private static final String KERBEROS_TYPE = "kerberos";
	static final String NAME_RULES = "hadoop.security.auth_to_local";
	static final String HOST_NAME = "ranger.service.host";
	
	@Autowired
	RangerServiceService rangerSvcService;
	
	@Autowired
	ServiceDBStore svcDBStore;
	
	@Autowired
	TagDBStore tagStore;

	@Autowired
	TimedExecutor timedExecutor;

	@Autowired
	RangerBizUtil rangerBizUtil;

	@Autowired
	SecurityZoneDBStore zoneStore;

	@Autowired
	XXGroupUserDao groupUserDao;

	public List<String> lookupResource(String serviceName, ResourceLookupContext context, ServiceStore svcStore) throws Exception {
		List<String> 	  ret = null;
		RangerService service = svcDBStore.getServiceByName(serviceName);
		
		String authType = PropertiesUtil.getProperty(AUTHENTICATION_TYPE);
		String lookupPrincipal = SecureClientLogin.getPrincipal(PropertiesUtil.getProperty(LOOKUP_PRINCIPAL), PropertiesUtil.getProperty(HOST_NAME));
		String lookupKeytab = PropertiesUtil.getProperty(LOOKUP_KEYTAB);
		String nameRules = PropertiesUtil.getProperty(NAME_RULES);
		String rangerPrincipal = SecureClientLogin.getPrincipal(PropertiesUtil.getProperty(ADMIN_USER_PRINCIPAL), PropertiesUtil.getProperty(HOST_NAME));
		String rangerkeytab = PropertiesUtil.getProperty(ADMIN_USER_KEYTAB);
		
		if(!StringUtils.isEmpty(authType) && KERBEROS_TYPE.equalsIgnoreCase(authType.trim()) && SecureClientLogin.isKerberosCredentialExists(lookupPrincipal, lookupKeytab)){
			if(service != null && service.getConfigs() != null){
				service.getConfigs().put(HadoopConfigHolder.RANGER_LOOKUP_PRINCIPAL, lookupPrincipal);
				service.getConfigs().put(HadoopConfigHolder.RANGER_LOOKUP_KEYTAB, lookupKeytab);
				service.getConfigs().put(HadoopConfigHolder.RANGER_NAME_RULES, nameRules);
				service.getConfigs().put(HadoopConfigHolder.RANGER_AUTH_TYPE, authType);				
			}
		}
		if(!StringUtils.isEmpty(authType) && KERBEROS_TYPE.equalsIgnoreCase(authType.trim()) && SecureClientLogin.isKerberosCredentialExists(rangerPrincipal, rangerkeytab)){
			if(service != null && service.getConfigs() != null){
				service.getConfigs().put(HadoopConfigHolder.RANGER_PRINCIPAL, rangerPrincipal);
				service.getConfigs().put(HadoopConfigHolder.RANGER_KEYTAB, rangerkeytab);
				service.getConfigs().put(HadoopConfigHolder.RANGER_NAME_RULES, nameRules);
				service.getConfigs().put(HadoopConfigHolder.RANGER_AUTH_TYPE, authType);				
			}
		}
		
		Map<String, String> newConfigs = rangerSvcService.getConfigsWithDecryptedPassword(service);
		service.setConfigs(newConfigs);
		
		RangerBaseService svc = getRangerServiceByService(service, svcStore);
		
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> ServiceMgr.lookupResource for Service: (" + svc + "Context: " + context + ")");
		}

		if(svc != null) {
			if (StringUtils.equals(svc.getServiceDef().getName(), EmbeddedServiceDefsUtil.EMBEDDED_SERVICEDEF_TAG_NAME)) {
				ret = svc.lookupResource(context);
			} else {
				LookupCallable callable = new LookupCallable(svc, context);
				long time = getTimeoutValueForLookupInMilliSeconds(svc);
				ret = timedExecutor.timedTask(callable, time, TimeUnit.MILLISECONDS);
			}
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("==> ServiceMgr.lookupResource for Response: (" + ret + ")");
		}

		return ret;
	}
	
	public VXResponse validateConfig(RangerService service, ServiceStore svcStore) throws Exception {
		VXResponse        ret = new VXResponse();
		String authType = PropertiesUtil.getProperty(AUTHENTICATION_TYPE);
		String lookupPrincipal = SecureClientLogin.getPrincipal(PropertiesUtil.getProperty(LOOKUP_PRINCIPAL), PropertiesUtil.getProperty(HOST_NAME));
		String lookupKeytab = PropertiesUtil.getProperty(LOOKUP_KEYTAB);
		String nameRules = PropertiesUtil.getProperty(NAME_RULES);
		String rangerPrincipal = SecureClientLogin.getPrincipal(PropertiesUtil.getProperty(ADMIN_USER_PRINCIPAL), PropertiesUtil.getProperty(HOST_NAME));
		String rangerkeytab = PropertiesUtil.getProperty(ADMIN_USER_KEYTAB);
		
		if(!StringUtils.isEmpty(authType) && KERBEROS_TYPE.equalsIgnoreCase(authType.trim()) && SecureClientLogin.isKerberosCredentialExists(lookupPrincipal, lookupKeytab)){
			if(service != null && service.getConfigs() != null){
				service.getConfigs().put(HadoopConfigHolder.RANGER_LOOKUP_PRINCIPAL, lookupPrincipal);
				service.getConfigs().put(HadoopConfigHolder.RANGER_LOOKUP_KEYTAB, lookupKeytab);
				service.getConfigs().put(HadoopConfigHolder.RANGER_NAME_RULES, nameRules);
				service.getConfigs().put(HadoopConfigHolder.RANGER_AUTH_TYPE, authType);
			}
		}
		if(!StringUtils.isEmpty(authType) && KERBEROS_TYPE.equalsIgnoreCase(authType.trim()) && SecureClientLogin.isKerberosCredentialExists(rangerPrincipal, rangerkeytab)){
			if(service != null && service.getConfigs() != null){
				service.getConfigs().put(HadoopConfigHolder.RANGER_PRINCIPAL, rangerPrincipal);
				service.getConfigs().put(HadoopConfigHolder.RANGER_KEYTAB, rangerkeytab);
				service.getConfigs().put(HadoopConfigHolder.RANGER_NAME_RULES, nameRules);
				service.getConfigs().put(HadoopConfigHolder.RANGER_AUTH_TYPE, authType);				
			}
		}
		RangerBaseService svc=null;
		if(service!=null){
			Map<String, String> newConfigs = rangerSvcService.getConfigsWithDecryptedPassword(service);
			service.setConfigs(newConfigs);
			svc = getRangerServiceByService(service, svcStore);
		}
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> ServiceMgr.validateConfig for Service: (" + svc + ")");
		}

		if(svc != null) {
			try {
				// Timeout value use during validate config is 10 times that used during lookup
				long time = getTimeoutValueForValidateConfigInMilliSeconds(svc);
				ValidateCallable callable = new ValidateCallable(svc);
				Map<String, Object> responseData = timedExecutor.timedTask(callable, time, TimeUnit.MILLISECONDS);

				ret = generateResponseForTestConn(responseData, "");
			} catch (Exception e) {
				String msg = "Unable to connect repository with given config for " + svc.getServiceName();
						
				HashMap<String, Object> respData = new HashMap<String, Object>();
				if (e instanceof HadoopException) {
					respData = ((HadoopException) e).getResponseData();
				}
				ret = generateResponseForTestConn(respData, msg);
				LOG.error("==> ServiceMgr.validateConfig Error:" + e);
			}
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("==> ServiceMgr.validateConfig for Response: (" + ret + ")");
		}

		return ret;
	}
	
	public boolean isZoneAdmin(String zoneName) {
		boolean isZoneAdmin = false;
		RangerSecurityZone securityZone = null;
		try {
			securityZone = zoneStore.getSecurityZoneByName(zoneName);
		} catch (Exception e) {
			LOG.error(
					"Unexpected error when fetching security zone with name:["
							+ zoneName + "] from database", e);
		}

		if (securityZone != null) {
			String userId = rangerBizUtil.getCurrentUserLoginId();

			List<XXGroupUser> groupUsers = groupUserDao
					.findByUserId(rangerBizUtil.getXUserId());
			List<String> loggedInUsersGroups = new ArrayList<>();
			for (XXGroupUser groupUser : groupUsers) {
				loggedInUsersGroups.add(groupUser.getName());
			}
			for (String loggedInUsersGroup : loggedInUsersGroups) {
				if (securityZone != null
						&& securityZone.getAdminUserGroups() != null
						&& securityZone.getAdminUserGroups().contains(
								loggedInUsersGroup)) {
					isZoneAdmin = true;
					break;
				}
			}
			if ((securityZone != null && securityZone.getAdminUsers() != null && securityZone
					.getAdminUsers().contains(userId))) {
				isZoneAdmin = true;
			}
		}

		return isZoneAdmin;
	}

	public boolean isZoneAuditor(String zoneName) {
		boolean isZoneAuditor = false;
		RangerSecurityZone securityZone = null;
		try {
			securityZone = zoneStore.getSecurityZoneByName(zoneName);
		} catch (Exception e) {
			LOG.error(
					"Unexpected error when fetching security zone with name:["
							+ zoneName + "] from database", e);
		}

		if (securityZone != null) {
			String userId = rangerBizUtil.getCurrentUserLoginId();

			List<XXGroupUser> groupUsers = groupUserDao
					.findByUserId(rangerBizUtil.getXUserId());
			List<String> loggedInUsersGroups = new ArrayList<>();
			for (XXGroupUser groupUser : groupUsers) {
				loggedInUsersGroups.add(groupUser.getName());
			}
			for (String loggedInUsersGroup : loggedInUsersGroups) {
				if (securityZone != null
						&& securityZone.getAuditUserGroups() != null
						&& securityZone.getAuditUserGroups().contains(
								loggedInUsersGroup)) {
					isZoneAuditor = true;
					break;
				}
			}
			if ((securityZone != null && securityZone.getAuditUsers() != null && securityZone
					.getAuditUsers().contains(userId))) {
				isZoneAuditor = true;
			}
		}

		return isZoneAuditor;
	}

	public RangerBaseService getRangerServiceByName(String serviceName, ServiceStore svcStore) throws Exception {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> ServiceMgr.getRangerServiceByName(" + serviceName + ")");
		}

		RangerBaseService ret     = null;
		RangerService     service = svcStore == null ? null : svcStore.getServiceByName(serviceName);

		if(service != null) {
			ret = getRangerServiceByService(service, svcStore);
		} else {
			LOG.warn("ServiceMgr.getRangerServiceByName(" + serviceName + "): could not find the service");
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== ServiceMgr.getRangerServiceByName(" + serviceName + "): " + ret);
		}

		return ret;
	}

	public RangerBaseService getRangerServiceByService(RangerService service, ServiceStore svcStore) throws Exception{
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> ServiceMgr.getRangerServiceByService(" + service + ")");
		}

		RangerBaseService ret         = null;
		String	          serviceType = service == null ? null : service.getType();

		if(! StringUtils.isEmpty(serviceType)) {
			RangerServiceDef serviceDef = svcStore == null ? null : svcStore.getServiceDefByName(serviceType);

			if(serviceDef != null) {
				Class<RangerBaseService> cls = getClassForServiceType(serviceDef);

				if(cls != null) {
					ret = cls.newInstance();

					ret.init(serviceDef, service);

					if(ret instanceof RangerServiceTag) {
						((RangerServiceTag)ret).setTagStore(tagStore);
					}
				} else {
					LOG.warn("ServiceMgr.getRangerServiceByService(" + service + "): could not find service class '"
						 + serviceDef.getImplClass() + "' for the service type '" + serviceType + "'");
				}
			} else {
				LOG.warn("ServiceMgr.getRangerServiceByService(" + service + "): could not find the service-def for the service type '" + serviceType + "'");
			}
		} else {
			LOG.warn("ServiceMgr.getRangerServiceByService(" + service + "): could not find the service-type '" + serviceType + "'");
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== ServiceMgr.getRangerServiceByService(" + service + "): " + ret);
		}

		return ret;
	}

	private static Map<String, Class<RangerBaseService>> serviceTypeClassMap = new HashMap<String, Class<RangerBaseService>>();
	private static String RANGER_DEFAULT_SERVICE_NAME = "org.apache.ranger.plugin.service.RangerDefaultService";

	@SuppressWarnings("unchecked")
	private Class<RangerBaseService> getClassForServiceType(RangerServiceDef serviceDef) throws Exception {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> ServiceMgr.getClassForServiceType(" + serviceDef + ")");
		}

		Class<RangerBaseService> ret = null;

		if(serviceDef != null) {
			String serviceType = serviceDef.getName();

			ret = serviceTypeClassMap.get(serviceType);

			if(ret == null) {
				synchronized(serviceTypeClassMap) {
					ret = serviceTypeClassMap.get(serviceType);

					if(ret == null) {
						String clsName = serviceDef.getImplClass();

						if(LOG.isDebugEnabled()) {
							LOG.debug("ServiceMgr.getClassForServiceType(" + serviceType + "): service-class " + clsName + " not found in cache");
						}
						try {

							Class<?> cls;

							if (StringUtils.isEmpty(clsName)) {
								if (LOG.isDebugEnabled()) {
									LOG.debug("No service-class configured for service-type:[" + serviceType + "], using RangerDefaultService");
								}
								clsName = RANGER_DEFAULT_SERVICE_NAME;

								cls = Class.forName(clsName);
							} else {
								URL[] pluginFiles = getPluginFilesForServiceType(serviceType);

								URLClassLoader clsLoader = new URLClassLoader(pluginFiles, Thread.currentThread().getContextClassLoader());

								cls = Class.forName(clsName, true, clsLoader);
							}

							ret = (Class<RangerBaseService>) cls;

							serviceTypeClassMap.put(serviceType, ret);

							if (LOG.isDebugEnabled()) {
								LOG.debug("ServiceMgr.getClassForServiceType(" + serviceType + "): service-class " + clsName + " added to cache");
							}
						} catch (Exception excp) {
							LOG.warn("ServiceMgr.getClassForServiceType(" + serviceType + "): failed to find service-class '" + clsName + "'. Resource lookup will not be available", excp);
							//Let's propagate the error
							throw new Exception(serviceType + " failed to find service class " + clsName + ". Resource lookup will not be available. Please make sure plugin jar is in the correct place.");
						}
					} else {
						if(LOG.isDebugEnabled()) {
							LOG.debug("ServiceMgr.getClassForServiceType(" + serviceType + "): service-class " + ret.getCanonicalName() + " found in cache");
						}
					}
				}
			} else {
				if(LOG.isDebugEnabled()) {
					LOG.debug("ServiceMgr.getClassForServiceType(" + serviceType + "): service-class " + ret.getCanonicalName() + " found in cache");
				}
			}
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== ServiceMgr.getClassForServiceType(" + serviceDef + "): " + ret);
		}

		return ret;
	}

	private URL[] getPluginFilesForServiceType(String serviceType) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> ServiceMgr.getPluginFilesForServiceType(" + serviceType + ")");
		}

		List<URL> ret = new ArrayList<URL>();

		getFilesInDirectory("ranger-plugins/" + serviceType, ret);

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== ServiceMgr.getPluginFilesForServiceType(" + serviceType + "): " + ret.size() + " files");
		}

		return ret.toArray(new URL[] { });
	}

	private void getFilesInDirectory(String dirPath, List<URL> files) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> ServiceMgr.getFilesInDirectory(" + dirPath + ")");
		}

		URL pluginJarPath = getClass().getClassLoader().getResource(dirPath);

		if(pluginJarPath != null && "file".equals(pluginJarPath.getProtocol())) {
			try {
				File[] dirFiles = new File(pluginJarPath.toURI()).listFiles();

				if(dirFiles != null) {
					for(File dirFile : dirFiles) {
						try {
							URL jarPath = dirFile.toURI().toURL();

							LOG.warn("getFilesInDirectory('" + dirPath + "'): adding " + dirFile.getAbsolutePath());
	
							files.add(jarPath);
						} catch(Exception excp) {
							LOG.warn("getFilesInDirectory('" + dirPath + "'): failed to get URI for file " + dirFile.getAbsolutePath(), excp);
						}
					}
				}
			} catch(Exception excp) {
				LOG.warn("getFilesInDirectory('" + dirPath + "'): error", excp);
			}
		} else {
				LOG.warn("getFilesInDirectory('" + dirPath + "'): could not find directory in CLASSPATH");
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== ServiceMgr.getFilesInDirectory(" + dirPath + ")");
		}
	}

	private VXResponse generateResponseForTestConn(
			Map<String, Object> responseData, String msg) {
		VXResponse vXResponse = new VXResponse();

		Long objId = null;
		boolean connectivityStatus = false;
		int statusCode = VXResponse.STATUS_ERROR;
		String message = msg;
		String description = msg;
		String fieldName = null;

		if (responseData != null) {
			if (responseData.get("objectId") != null) {
				objId = Long.parseLong(responseData.get("objectId").toString());
			}
			if (responseData.get("connectivityStatus") != null) {
				connectivityStatus = Boolean.parseBoolean(responseData.get("connectivityStatus").toString());
			}
			if (connectivityStatus) {
				statusCode = VXResponse.STATUS_SUCCESS;
			}
			if (responseData.get("message") != null) {
				message = responseData.get("message").toString();
			}
			if (responseData.get("description") != null) {
				description = responseData.get("description").toString();
			}
			if (responseData.get("fieldName") != null) {
				fieldName = responseData.get("fieldName").toString();
			}
		}

		VXMessage vXMsg = new VXMessage();
		List<VXMessage> vXMsgList = new ArrayList<VXMessage>();
		vXMsg.setFieldName(fieldName);
		vXMsg.setMessage(message);
		vXMsg.setObjectId(objId);
		vXMsgList.add(vXMsg);

		vXResponse.setMessageList(vXMsgList);
		vXResponse.setMsgDesc(description);
		vXResponse.setStatusCode(statusCode);
		return vXResponse;
	}
	
	static final long _DefaultTimeoutValue_Lookp = 1000; // 1 s
	static final long _DefaultTimeoutValue_ValidateConfig = 10000; // 10 s

	long getTimeoutValueForLookupInMilliSeconds(RangerBaseService svc) {
		return getTimeoutValueInMilliSeconds("resource.lookup", svc, _DefaultTimeoutValue_Lookp);
	}
	
	long getTimeoutValueForValidateConfigInMilliSeconds(RangerBaseService svc) {
		return getTimeoutValueInMilliSeconds("validate.config", svc, _DefaultTimeoutValue_ValidateConfig);
	}
	
	long getTimeoutValueInMilliSeconds(final String type, RangerBaseService svc, long defaultValue) {
		if (LOG.isDebugEnabled()) {
			LOG.debug(String.format("==> ServiceMgr.getTimeoutValueInMilliSeconds (%s, %s)", type, svc));
		}
		String propertyName = type + ".timeout.value.in.ms"; // type == "lookup" || type == "validate-config"

		Long result = null;
		Map<String, String> config = svc.getConfigs();
		if (config != null && config.containsKey(propertyName)) {
			result = parseLong(config.get(propertyName));
		}
		if (result != null) {
			LOG.debug("Found override in service config!");
		} else {
			String[] keys = new String[] {
					"ranger.service." + svc.getServiceName() + "." + propertyName,
					"ranger.servicetype." + svc.getServiceType() + "." + propertyName,
					"ranger." + propertyName
			};
			for (String key : keys) {
				String value = PropertiesUtil.getProperty(key);
				if (value != null) {
					result = parseLong(value);
					if (result != null) {
						if (LOG.isDebugEnabled()) {
							LOG.debug("Using the value[" + value + "] found in property[" + key + "]");
						}
						break;
					}
				}
			}
		}
		if (result == null) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("No overrides found in service config of properties file.  Using supplied default of[" + defaultValue + "]!");
			}
			result = defaultValue;
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug(String.format("<== ServiceMgr.getTimeoutValueInMilliSeconds (%s, %s): %s", type, svc, result));
		}
		return result;
	}
	
	Long parseLong(String str) {
		try {
			return Long.valueOf(str);
		} catch (NumberFormatException e) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("ServiceMgr.parseLong: could not parse [" + str + "] as Long! Returning null");
			}
			return null;
		}
	}
	
	abstract static class TimedCallable<T> implements Callable<T> {

		final RangerBaseService svc;
		final Date creation; // NOTE: This would be different from when the callable was actually offered to the executor

		public TimedCallable(RangerBaseService svc) {
			this.svc = svc;
			this.creation = new Date();
		}

		@Override
		public T call() throws Exception {
			Date start = null;
			if (LOG.isDebugEnabled()) {
				start = new Date();
				LOG.debug("==> TimedCallable: " + toString());
			}

			ClassLoader clsLoader = Thread.currentThread().getContextClassLoader();
			try {
				Thread.currentThread().setContextClassLoader(svc.getClass().getClassLoader());
				return actualCall();
			} catch (Exception e) {
				LOG.error("TimedCallable.call: Error:" + e);
				throw e;
			} finally {
				Thread.currentThread().setContextClassLoader(clsLoader);
				if (LOG.isDebugEnabled()) {
					Date finish = new Date();
					long waitTime = start.getTime() - creation.getTime();
					long executionTime = finish.getTime() - start.getTime();
					LOG.debug(String.format("<== TimedCallable: %s: wait time[%d ms], execution time [%d ms]", toString(), waitTime, executionTime));
				}
			}
		}

		abstract T actualCall() throws Exception;
	}

	static class LookupCallable extends TimedCallable<List<String>> {

		final ResourceLookupContext context;

		public LookupCallable(final RangerBaseService svc, final ResourceLookupContext context) {
			super(svc);
			this.context = context;
		}

		@Override
		public String toString() {
			return String.format("lookup resource[%s] for service[%s], ", context.toString(), svc.getServiceName());
		}

		@Override
		public List<String> actualCall() throws Exception {
			List<String> ret = svc.lookupResource(context);
			return ret;
		}
	}

	static class ValidateCallable extends TimedCallable<Map<String, Object>> {

		public ValidateCallable(RangerBaseService svc) {
			super(svc);
		}

		@Override
		public String toString() {
			return String.format("validate config for service[%s]", svc.getServiceName());
		}

		@Override
		public Map<String, Object> actualCall() throws Exception {
			return svc.validateConfig();
		}
	}
}

