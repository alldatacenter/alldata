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

package org.apache.ranger.common;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.authorization.utils.StringUtil;
import org.apache.ranger.biz.ServiceDBStore;
import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.entity.XXGroup;
import org.apache.ranger.entity.XXUser;
import org.apache.ranger.plugin.model.RangerBaseModelObject;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItemAccess;
import org.apache.ranger.plugin.model.RangerService;
import org.apache.ranger.plugin.store.EmbeddedServiceDefsUtil;
import org.apache.ranger.plugin.util.GrantRevokeRequest;
import org.apache.ranger.plugin.util.RangerServiceNotFoundException;
import org.apache.ranger.plugin.util.SearchFilter;
import org.apache.ranger.view.VXAsset;
import org.apache.ranger.view.VXAuditMap;
import org.apache.ranger.view.VXDataObject;
import org.apache.ranger.view.VXPermMap;
import org.apache.ranger.view.VXPermObj;
import org.apache.ranger.view.VXPolicy;
import org.apache.ranger.view.VXPolicyList;
import org.apache.ranger.view.VXRepository;
import org.apache.ranger.view.VXRepositoryList;
import org.apache.ranger.view.VXResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ServiceUtil {
	static final Logger LOG = LoggerFactory.getLogger(ServiceUtil.class);
	private static final String REGEX_PREFIX_STR 	 = "regex:";
	private static final int REGEX_PREFIX_STR_LENGTH = REGEX_PREFIX_STR.length();

	static Map<String, Integer> mapServiceTypeToAssetType = new HashMap<String, Integer>();
	static Map<String, Integer> mapAccessTypeToPermType   = new HashMap<String, Integer>();
	static String version;
	
	@Autowired
	JSONUtil jsonUtil;

	@Autowired
	RangerDaoManager xaDaoMgr;

	@Autowired
	RESTErrorUtil restErrorUtil;

	@Autowired
	ServiceDBStore svcStore;

	static {
		mapServiceTypeToAssetType.put(EmbeddedServiceDefsUtil.EMBEDDED_SERVICEDEF_HDFS_NAME,  Integer.valueOf(RangerCommonEnums.ASSET_HDFS));
		mapServiceTypeToAssetType.put(EmbeddedServiceDefsUtil.EMBEDDED_SERVICEDEF_HBASE_NAME, Integer.valueOf(RangerCommonEnums.ASSET_HBASE));
		mapServiceTypeToAssetType.put(EmbeddedServiceDefsUtil.EMBEDDED_SERVICEDEF_HIVE_NAME,  Integer.valueOf(RangerCommonEnums.ASSET_HIVE));
		mapServiceTypeToAssetType.put(EmbeddedServiceDefsUtil.EMBEDDED_SERVICEDEF_KNOX_NAME,  Integer.valueOf(RangerCommonEnums.ASSET_KNOX));
		mapServiceTypeToAssetType.put(EmbeddedServiceDefsUtil.EMBEDDED_SERVICEDEF_STORM_NAME, Integer.valueOf(RangerCommonEnums.ASSET_STORM));

		mapAccessTypeToPermType.put("Unknown", 0);
		mapAccessTypeToPermType.put("Reset", 1);
		mapAccessTypeToPermType.put("read", 2);
		mapAccessTypeToPermType.put("write", 3);
		mapAccessTypeToPermType.put("create", 4);
		mapAccessTypeToPermType.put("delete", 5);
		mapAccessTypeToPermType.put("admin", 6);
		mapAccessTypeToPermType.put("Obfuscate", 7);
		mapAccessTypeToPermType.put("Mask", 8);
		mapAccessTypeToPermType.put("execute", 9);
		mapAccessTypeToPermType.put("select", 10);
		mapAccessTypeToPermType.put("update", 11);
		mapAccessTypeToPermType.put("drop", 12);
		mapAccessTypeToPermType.put("alter", 13);
		mapAccessTypeToPermType.put("index", 14);
		mapAccessTypeToPermType.put("lock", 15);
		mapAccessTypeToPermType.put("all", 16);
		mapAccessTypeToPermType.put("allow", 17);
		mapAccessTypeToPermType.put("submitTopology", 18);
		mapAccessTypeToPermType.put("fileUpload", 19);
		mapAccessTypeToPermType.put("getNimbusConf", 20);
		mapAccessTypeToPermType.put("getClusterInfo", 21);
		mapAccessTypeToPermType.put("fileDownload", 22);
		mapAccessTypeToPermType.put("killTopology", 23);
		mapAccessTypeToPermType.put("rebalance", 24);
		mapAccessTypeToPermType.put("activate", 25);
		mapAccessTypeToPermType.put("deactivate", 26);
		mapAccessTypeToPermType.put("getTopologyConf", 27);
		mapAccessTypeToPermType.put("getTopology", 28);
		mapAccessTypeToPermType.put("getUserTopology", 29);
		mapAccessTypeToPermType.put("getTopologyInfo", 30);
		mapAccessTypeToPermType.put("uploadNewCredentials", 31);
		mapAccessTypeToPermType.put("repladmin", 32);
		mapAccessTypeToPermType.put("serviceadmin", 33);
		mapAccessTypeToPermType.put("tempudfadmin", 34);
		mapAccessTypeToPermType.put("idempotent_write", 35);
		mapAccessTypeToPermType.put("describe_configs", 36);
		mapAccessTypeToPermType.put("alter_configs", 37);
		mapAccessTypeToPermType.put("cluster_action", 38);

		version = "0";
	}

	public RangerService getServiceByName(@PathParam("name") String name) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> ServiceUtil.getServiceByName(" + name + ")");
		}

		RangerService ret = null;

		try {
			ret = svcStore.getServiceByName(name);
		} catch(WebApplicationException excp) {
			throw excp;
		} catch(Throwable excp) {
			LOG.error("getServiceByName(" + name + ") failed", excp);

			throw restErrorUtil.createRESTException(excp.getMessage());
		}

		if(ret == null) {
			throw restErrorUtil.createRESTException(HttpServletResponse.SC_NOT_FOUND, RangerServiceNotFoundException.buildExceptionMsg(name), true);
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== ServiceUtil.getServiceByName(" + name + "): " + ret);
		}

		return ret;
	}

	public RangerService toRangerService(VXAsset asset) {
		if(asset == null) {
			return null;
		}

		RangerService ret = new RangerService();

		dataObjectToRangerObject(asset, ret);

		ret.setType(toServiceType(asset.getAssetType()));
		ret.setName(asset.getName());
		ret.setDescription(asset.getDescription());
		ret.setIsEnabled(asset.getActiveStatus() == RangerCommonEnums.STATUS_ENABLED);
		ret.setConfigs(jsonUtil.jsonToMap(asset.getConfig()));

		return ret;
	}

	public VXAsset toVXAsset(RangerService service) {
		if(service == null || toAssetType(service.getType()) == null) {
			return null;
		}

		VXAsset ret = new VXAsset();
	
		rangerObjectToDataObject(service, ret);

		ret.setAssetType(toAssetType(service.getType()));
		ret.setName(service.getName());
		ret.setDescription(service.getDescription());
		ret.setActiveStatus(service.getIsEnabled() ? RangerCommonEnums.STATUS_ENABLED : RangerCommonEnums.STATUS_DISABLED);
		ret.setConfig(jsonUtil.readMapToString(service.getConfigs()));

		return ret;
	}

	public VXRepository toVXRepository(RangerService service){
		if(service == null || toAssetType(service.getType()) == null) {
			return null;
		}

		VXRepository  ret = new VXRepository();

		rangerObjectToDataObject(service,ret);

		ret.setRepositoryType(service.getType());
		ret.setName(service.getName());
		ret.setDescription(service.getDescription());
		ret.setIsActive(service.getIsEnabled());
		ret.setConfig(jsonUtil.readMapToString(service.getConfigs()));
		ret.setVersion(Long.toString(service.getVersion()));
		
		return ret;
	}
	
	public RangerPolicy toRangerPolicy(VXResource resource, RangerService service) {
		if(resource == null) {
			return null;
		}

		RangerPolicy ret = new RangerPolicy();

		dataObjectToRangerObject(resource, ret);

		if(service != null) {
			ret.setService(service.getName());
		} else {
			ret.setService(resource.getAssetName());
		}

		ret.setName(StringUtils.trim(resource.getPolicyName()));
		ret.setDescription(resource.getDescription());
		ret.setIsEnabled(resource.getResourceStatus() == RangerCommonEnums.STATUS_ENABLED);
		ret.setIsAuditEnabled(resource.getAuditList() != null && !resource.getAuditList().isEmpty());

		Boolean isPathRecursive  = resource.getIsRecursive() == RangerCommonEnums.BOOL_TRUE;
		Boolean isTableExcludes  = resource.getTableType() == RangerCommonEnums.POLICY_EXCLUSION;
		Boolean isColumnExcludes = resource.getColumnType() == RangerCommonEnums.POLICY_EXCLUSION;

		toRangerResourceList(resource.getName(), "path", Boolean.FALSE, isPathRecursive, ret.getResources());
		toRangerResourceList(resource.getTables(), "table", isTableExcludes, Boolean.FALSE, ret.getResources());
		toRangerResourceList(resource.getColumnFamilies(), "column-family", Boolean.FALSE, Boolean.FALSE, ret.getResources());
		toRangerResourceList(resource.getColumns(), "column", isColumnExcludes, Boolean.FALSE, ret.getResources());
		toRangerResourceList(resource.getDatabases(), "database", Boolean.FALSE, Boolean.FALSE, ret.getResources());
		toRangerResourceList(resource.getUdfs(), "udf", Boolean.FALSE, Boolean.FALSE, ret.getResources());
		toRangerResourceList(resource.getTopologies(), "topology", Boolean.FALSE, Boolean.FALSE, ret.getResources());
		toRangerResourceList(resource.getServices(), "service", Boolean.FALSE, Boolean.FALSE, ret.getResources());
		toRangerResourceList(resource.getHiveServices(), "hiveservice", Boolean.FALSE, Boolean.FALSE, ret.getResources());

		HashMap<String, List<VXPermMap>> sortedPermMap = new HashMap<String, List<VXPermMap>>();
		
		// re-group the list with permGroup as the key
		if (resource.getPermMapList() != null) {
			for(VXPermMap permMap : resource.getPermMapList()) {
				String          permGrp    = permMap.getPermGroup();
				List<VXPermMap> sortedList = sortedPermMap.get(permGrp);

				if(sortedList == null) {
					sortedList = new ArrayList<VXPermMap>();
					sortedPermMap.put(permGrp, sortedList);
				}

				sortedList.add(permMap);
			}
		}

		Integer assetType = getAssetType(service,ret.getService());

		for (Entry<String, List<VXPermMap>> entry : sortedPermMap.entrySet()) {
			List<String>                 userList   = new ArrayList<String>();
			List<String>                 groupList  = new ArrayList<String>();
			List<RangerPolicyItemAccess> accessList = new ArrayList<RangerPolicyItemAccess>();
			String                       ipAddress  = null;

			RangerPolicy.RangerPolicyItem policyItem = new RangerPolicy.RangerPolicyItem();

			for(VXPermMap permMap : entry.getValue()) {
				if(permMap.getPermFor() == AppConstants.XA_PERM_FOR_USER) {
					String userName = getUserName(permMap);

					if (! userList.contains(userName)) {
						userList.add(userName);
					}
				} else if(permMap.getPermFor() == AppConstants.XA_PERM_FOR_GROUP) {
					String groupName = getGroupName(permMap);

					if (! groupList.contains(groupName)) {
						groupList.add(groupName);
					}					
				}

				String accessType = toAccessType(permMap.getPermType());
				
				if(StringUtils.equalsIgnoreCase(accessType, "Admin")) {
					policyItem.setDelegateAdmin(Boolean.TRUE);
					if ( assetType != null && assetType == RangerCommonEnums.ASSET_HBASE) {
						accessList.add(new RangerPolicyItemAccess(accessType));
					}
				} else {
					accessList.add(new RangerPolicyItemAccess(accessType));
				}

				ipAddress = permMap.getIpAddress();
			}
			
			policyItem.setUsers(userList);
			policyItem.setGroups(groupList);
			policyItem.setAccesses(accessList);
			
			if(ipAddress != null && !ipAddress.isEmpty()) {
				RangerPolicy.RangerPolicyItemCondition ipCondition = new RangerPolicy.RangerPolicyItemCondition("ipaddress", Collections.singletonList(ipAddress));

				policyItem.getConditions().add(ipCondition);
			}
			
			ret.getPolicyItems().add(policyItem);
		}

		return ret;
	}

	public VXResource toVXResource(RangerPolicy policy, RangerService service) {
		if(policy == null || service == null || toAssetType(service.getType()) == null) {
			return null;
		}

		VXResource ret = new VXResource();

		rangerObjectToDataObject(policy, ret);

		ret.setAssetName(policy.getService());
		ret.setAssetId(service.getId());
		ret.setAssetType(toAssetType(service.getType()));
		ret.setPolicyName(policy.getName());
		ret.setDescription(policy.getDescription());
		ret.setGuid(policy.getGuid());
		ret.setResourceStatus(policy.getIsEnabled() ? RangerCommonEnums.STATUS_ENABLED : RangerCommonEnums.STATUS_DISABLED);

		List<VXAuditMap> auditList = null;
		if(policy.getIsAuditEnabled()) {
			VXAuditMap auditMap = new VXAuditMap();

			auditMap.setResourceId(policy.getId());
			auditMap.setAuditType(AppConstants.XA_AUDIT_TYPE_ALL);

			auditList = new ArrayList<VXAuditMap>();
			auditList.add(auditMap);
		}
		ret.setAuditList(auditList);

		for(Map.Entry<String, RangerPolicy.RangerPolicyResource> e : policy.getResources().entrySet()) {
			RangerPolicy.RangerPolicyResource res       = e.getValue();
			String                            resType   = e.getKey();
			String                            resString = getResourceString(res.getValues());

			if("path".equalsIgnoreCase(resType)) {
				ret.setName(resString);
				ret.setIsRecursive(Boolean.TRUE.equals(res.getIsRecursive()) ? RangerCommonEnums.BOOL_TRUE : RangerCommonEnums.BOOL_FALSE);
			} else if("table".equalsIgnoreCase(resType)) {
				ret.setTables(resString);
				ret.setTableType(Boolean.TRUE.equals(res.getIsExcludes()) ? RangerCommonEnums.POLICY_EXCLUSION : RangerCommonEnums.POLICY_INCLUSION);
			} else if("column-family".equalsIgnoreCase(resType)) {
				ret.setColumnFamilies(resString);
			} else if("column".equalsIgnoreCase(resType)) {
				ret.setColumns(resString);
				ret.setColumnType(Boolean.TRUE.equals(res.getIsExcludes()) ? RangerCommonEnums.POLICY_EXCLUSION : RangerCommonEnums.POLICY_INCLUSION);
			} else if("database".equalsIgnoreCase(resType)) {
				ret.setDatabases(resString);
			} else if("udf".equalsIgnoreCase(resType)) {
				ret.setUdfs(resString);
			} else if("topology".equalsIgnoreCase(resType)) {
				ret.setTopologies(resString);
			} else if("service".equalsIgnoreCase(resType)) {
				ret.setServices(resString);
			} else if(resType.equalsIgnoreCase("hiveservice")) {
				ret.setHiveServices(resString);
			}
		}
		updateResourceName(ret);

		List<VXPermMap> permMapList = getVXPermMapList(policy);
		
		ret.setPermMapList(permMapList);

		return ret;
	}

	public VXAsset publicObjecttoVXAsset(VXRepository vXRepository) {
		VXAsset ret = new VXAsset();
		publicDataObjectTovXDataObject(vXRepository,ret);

		Integer assetType = toAssetType(vXRepository.getRepositoryType());

		ret.setAssetType(assetType == null ? -1 : assetType.intValue());
		ret.setName(vXRepository.getName());
		ret.setDescription(vXRepository.getDescription());
		ret.setActiveStatus(vXRepository.getIsActive() ? RangerCommonEnums.STATUS_ENABLED : RangerCommonEnums.STATUS_DISABLED);
		ret.setConfig(vXRepository.getConfig());
		return ret;
	}
	
	public VXRepository  vXAssetToPublicObject(VXAsset asset) {
		VXRepository ret = new VXRepository();
		vXDataObjectToPublicDataObject(ret,asset);

		ret.setRepositoryType(toServiceType(asset.getAssetType()));
		ret.setName(asset.getName());
		ret.setDescription(asset.getDescription());
		ret.setIsActive(asset.getActiveStatus() == RangerCommonEnums.STATUS_ENABLED ? true : false);
		ret.setConfig(asset.getConfig());
		ret.setVersion(version);
		
		return ret;
	}

	private Map<String, RangerPolicy.RangerPolicyResource> toRangerResourceList(String resourceString, String resourceType, Boolean isExcludes, Boolean isRecursive, Map<String, RangerPolicy.RangerPolicyResource> resources) {
		Map<String, RangerPolicy.RangerPolicyResource> ret = resources == null ? new HashMap<String, RangerPolicy.RangerPolicyResource>() : resources;

		if(StringUtils.isNotBlank(resourceString)) {
			RangerPolicy.RangerPolicyResource resource = ret.get(resourceType);

			if(resource == null) {
				resource = new RangerPolicy.RangerPolicyResource();
				resource.setIsExcludes(isExcludes);
				resource.setIsRecursive(isRecursive);

				ret.put(resourceType, resource);
			}

			Collections.addAll(resource.getValues(), resourceString.split(","));
		}

		return ret;
	}

	private static String toServiceType(int assetType) {
		String ret = null;

		for(Map.Entry<String, Integer> e : mapServiceTypeToAssetType.entrySet()) {
			if(e.getValue().intValue() == assetType) {
				ret = e.getKey();

				break;
			}
		}

		return ret;
	}

	private static Integer toAssetType(String serviceType) {
		Integer ret = null;

		if(serviceType != null) {
			ret = mapServiceTypeToAssetType.get(serviceType.toLowerCase());
		}

		return ret;
	}

	public static String toAccessType(int permType) {
		String ret = null;

		for(Map.Entry<String, Integer> e : mapAccessTypeToPermType.entrySet()) {
			if(e.getValue().intValue() == permType) {
				ret = e.getKey();

				break;
			}
		}

		return ret;
	}

	private static Integer toPermType(String accessType) {
		Integer ret = null;

		for(Map.Entry<String, Integer> e : mapAccessTypeToPermType.entrySet()) {
			if(e.getKey().equalsIgnoreCase(accessType)) {
				ret = e.getValue();

				break;
			}
		}

		return ret == null ? 0 : ret;
	}
	
	private RangerBaseModelObject dataObjectToRangerObject(VXDataObject dataObject,RangerBaseModelObject rangerObject) {
		RangerBaseModelObject ret = rangerObject;

		ret.setId(dataObject.getId());
		ret.setCreateTime(dataObject.getCreateDate());
		ret.setUpdateTime(dataObject.getUpdateDate());
		ret.setCreatedBy(dataObject.getOwner());
		ret.setUpdatedBy(dataObject.getUpdatedBy());

		return ret;
	}

	private VXDataObject rangerObjectToDataObject(RangerBaseModelObject rangerObject, VXDataObject dataObject) {
		VXDataObject ret = dataObject;

		ret.setId(rangerObject.getId());
		ret.setCreateDate(rangerObject.getCreateTime());
		ret.setUpdateDate(rangerObject.getUpdateTime());
		ret.setOwner(rangerObject.getCreatedBy());
		ret.setUpdatedBy(rangerObject.getUpdatedBy());

		return ret;
	}
	
	private String toVxPolicyIncExc(int policyIncExc) {
		String ret = "";
		
		switch(policyIncExc)  {
		case 0:
			ret = "Inclusion";
			break;
		case 1:
			ret = "Exclusion";
			break;
		}
		return ret;
	}	

	private void updateResourceName(VXPolicy policy) {
		if(policy == null || toAssetType(policy.getRepositoryType()) == null) {
			return;
		}

		String resourceName = getResourceName(toAssetType(policy.getRepositoryType()),
											  policy.getResourceName(),
											  policy.getTables(),
											  policy.getColumnFamilies(),
											  policy.getColumns(),
											  policy.getDatabases(),
											  policy.getTopologies(),
											  policy.getServices());

		policy.setResourceName(resourceName);
	}

	private void updateResourceName(VXResource resource) {
		if(resource == null) {
			return;
		}

		String resourceName = getResourceName(resource.getAssetType(),
											  resource.getName(),
											  resource.getTables(),
											  resource.getColumnFamilies(),
											  resource.getColumns(),
											  resource.getDatabases(),
											  resource.getTopologies(),
											  resource.getServices());

		resource.setName(resourceName);
	}

	private String getResourceName(int assetType, String paths, String tables, String columnFamilies, String columns, String databases, String topologies, String services) {
		StringBuilder sb = new StringBuilder();

		switch(assetType) {
			case RangerCommonEnums.ASSET_HDFS:
				paths = emptyIfNull(paths);

				sb.append(paths);
			break;

			case RangerCommonEnums.ASSET_HBASE:
			{
				tables         = emptyIfNull(tables);
				columnFamilies = emptyIfNull(columnFamilies);
				columns        = emptyIfNull(columns);

				for(String column : columns.split(",")) {
					for(String columnFamily : columnFamilies.split(",")) {
						for(String table : tables.split(",")) {
							if(sb.length() > 0) {
								sb.append(",");
							}

							sb.append("/").append(table).append("/").append(columnFamily).append("/").append(column);
						}
					}
				}
			}
			break;

			case RangerCommonEnums.ASSET_HIVE:
			{
				databases = emptyIfNull(databases);
				tables    = emptyIfNull(tables);
				columns   = emptyIfNull(columns);

				for(String column : columns.split(",")) {
					for(String table : tables.split(",")) {
						for(String database : databases.split(",")) {
							if(sb.length() > 0) {
								sb.append(",");
							}

							sb.append("/").append(database).append("/").append(table).append("/").append(column);
						}
					}
				}
			}
			break;

			case RangerCommonEnums.ASSET_KNOX:
			{
				topologies = emptyIfNull(topologies);
				services   = emptyIfNull(services);

				for(String service : services.split(",")) {
					for(String topology : topologies.split(",")) {
						if(sb.length() > 0) {
							sb.append(",");
						}

						sb.append("/").append(topology).append("/").append(service);
					}
				}
			}
			break;

			case RangerCommonEnums.ASSET_STORM:
				topologies = emptyIfNull(topologies);

				sb.append(topologies);
			break;
		}

		return sb.toString();
	}

	private String emptyIfNull(String str) {
		return str == null ? "" : str;
	}
	
	private String getResourceString(List<String> values) {
		String ret = null;

		if(values != null) {
			for(String value : values) {
				if(ret == null) {
					ret = value;
				} else if(value != null) {
					ret += ("," + value);
				}
			}
		}

		return ret;
	}

	private String getUserName(VXPermMap permMap) {
		String userName = permMap.getUserName();

		if(userName == null || userName.isEmpty()) {
			Long userId = permMap.getUserId();

			if(userId != null) {
				XXUser xxUser = xaDaoMgr.getXXUser().getById(userId);

				if(xxUser != null) {
					userName = xxUser.getName();
				}
			}
		}

		return userName;
	}

	private String getGroupName(VXPermMap permMap) {
		String groupName = permMap.getGroupName();

		if(groupName == null || groupName.isEmpty()) {
			Long groupId = permMap.getGroupId();

			if(groupId != null) {
				XXGroup xxGroup = xaDaoMgr.getXXGroup().getById(groupId);

				if(xxGroup != null) {
					groupName = xxGroup.getName();
				}
			}
		}
		
		return groupName;
		
	}

	private Long getUserId(String userName) {
		Long userId = null;

		if(userName != null) {
			XXUser xxUser = xaDaoMgr.getXXUser().findByUserName(userName);
	
			if(xxUser != null) {
				userId = xxUser.getId();
			}
		}

		return userId;
	}

	private Long getGroupId(String groupName) {
		Long groupId = null;

		if(groupName != null) {
			XXGroup xxGroup = xaDaoMgr.getXXGroup().findByGroupName(groupName);

			if(xxGroup != null) {
				groupId = xxGroup.getId();
			}
		}

		return groupId;
	}
	
	public SearchCriteria getMappedSearchParams(HttpServletRequest request,
			SearchCriteria searchCriteria) {

		Object typeObj = searchCriteria.getParamValue("type");
		Object statusObj = searchCriteria.getParamValue("status");

		ArrayList<Integer> statusList = new ArrayList<Integer>();
		if (statusObj == null) {
			statusList.add(RangerCommonEnums.STATUS_DISABLED);
			statusList.add(RangerCommonEnums.STATUS_ENABLED);
		} else {
			Boolean status = restErrorUtil.parseBoolean(
					request.getParameter("status"), "Invalid value for "
							+ "status", MessageEnums.INVALID_INPUT_DATA, null,
					"status");
			int statusEnum = (status == null || status == false) ? AppConstants.STATUS_DISABLED
					: AppConstants.STATUS_ENABLED;
			statusList.add(statusEnum);
		}
		searchCriteria.addParam("status", statusList);

		if (typeObj != null) {
			String type = typeObj.toString();
			int typeEnum = AppConstants.getEnumFor_AssetType(type);
			searchCriteria.addParam("type", typeEnum);
		}
		return searchCriteria;
	}
	
	
	public VXRepositoryList rangerServiceListToPublicObjectList(List<RangerService> serviceList) {

		List<VXRepository> repoList = new ArrayList<VXRepository>();
		for (RangerService service  : serviceList) {
			VXRepository vXRepo = toVXRepository(service);

			if(vXRepo != null) {
				repoList.add(vXRepo);
			}
		}
		VXRepositoryList vXRepositoryList = new VXRepositoryList(repoList);
		return vXRepositoryList;
	}
	
		
	private VXDataObject vXDataObjectToPublicDataObject(VXDataObject publicDataObject, VXDataObject vXdataObject) {
		
		VXDataObject ret = publicDataObject;
		
		ret.setId(vXdataObject.getId());
		ret.setCreateDate(vXdataObject.getCreateDate());
		ret.setUpdateDate(vXdataObject.getUpdateDate());
		ret.setOwner(vXdataObject.getOwner());
		ret.setUpdatedBy(vXdataObject.getUpdatedBy());

		return ret;
	}
	
	protected VXDataObject publicDataObjectTovXDataObject(VXDataObject publicDataObject,VXDataObject vXDataObject) {
		
		VXDataObject ret = vXDataObject;
		
		ret.setId(publicDataObject.getId());
		ret.setCreateDate(publicDataObject.getCreateDate());
		ret.setUpdateDate(publicDataObject.getUpdateDate());
		ret.setOwner(publicDataObject.getOwner());
		ret.setUpdatedBy(publicDataObject.getUpdatedBy());

		return ret;
	}
	
	
	public VXPolicy toVXPolicy(RangerPolicy policy, RangerService service) {
		if(policy == null || service == null || toAssetType(service.getType()) == null) {
			return null;
		}

		VXPolicy ret = new VXPolicy();

		rangerObjectToDataObject(policy, ret);

		ret.setPolicyName(StringUtils.trim(policy.getName()));
		ret.setDescription(policy.getDescription());
		ret.setRepositoryName(policy.getService());
		ret.setIsEnabled(policy.getIsEnabled() ? true : false);
		ret.setRepositoryType(service.getType());
		ret.setIsAuditEnabled(policy.getIsAuditEnabled());
		if (policy.getVersion() != null ) {
			ret.setVersion(policy.getVersion().toString());
		} else {
			ret.setVersion(version);
		}
		
		for(Map.Entry<String, RangerPolicy.RangerPolicyResource> e : policy.getResources().entrySet()) {
			RangerPolicy.RangerPolicyResource res       = e.getValue();
			String                            resType   = e.getKey();
			String                            resString = getResourceString(res.getValues());

			if("path".equalsIgnoreCase(resType)) {
				ret.setResourceName(resString);
				ret.setIsRecursive(Boolean.TRUE.equals(res.getIsRecursive()) ? true : false);
			} else if("table".equalsIgnoreCase(resType)) {
				ret.setTables(resString);
				ret.setTableType(Boolean.TRUE.equals(res.getIsExcludes()) ? toVxPolicyIncExc(RangerCommonEnums.POLICY_EXCLUSION):toVxPolicyIncExc(RangerCommonEnums.POLICY_INCLUSION));
			} else if("column-family".equalsIgnoreCase(resType)) {
				ret.setColumnFamilies(resString);
			} else if("column".equalsIgnoreCase(resType)) {
				ret.setColumns(resString);
				ret.setColumnType(Boolean.TRUE.equals(res.getIsExcludes()) ? toVxPolicyIncExc(RangerCommonEnums.POLICY_EXCLUSION):toVxPolicyIncExc(RangerCommonEnums.POLICY_INCLUSION));
			} else if("database".equalsIgnoreCase(resType)) {
				ret.setDatabases(resString);
			} else if("udf".equalsIgnoreCase(resType)) {
				ret.setUdfs(resString);
			} else if("topology".equalsIgnoreCase(resType)) {
				ret.setTopologies(resString);
			} else if("service".equalsIgnoreCase(resType)) {
				ret.setServices(resString);
			} else if(resType.equalsIgnoreCase("hiveservice")) {
				ret.setHiveServices(resString);
			}
		}
		updateResourceName(ret);
			
		List<VXPermMap> vXPermMapList = getVXPermMapList(policy);
			
		List<VXPermObj> vXPermObjList = mapPermMapToPermObj(vXPermMapList);
		
		ret.setPermMapList(vXPermObjList);
		
		return ret;
 	}


	public List<VXPermMap> getVXPermMapList(RangerPolicy policy) {
		
		List<VXPermMap> permMapList = new ArrayList<VXPermMap>();
	
		int permGroup = 0;
		for(RangerPolicy.RangerPolicyItem policyItem : policy.getPolicyItems()) {
			String ipAddress = null;

            for (RangerPolicy.RangerPolicyItemCondition condition : policyItem.getConditions()) {
                if ("ipaddress".equalsIgnoreCase(condition.getType())) {
                    List<String> values = condition.getValues();
                    if (CollectionUtils.isNotEmpty(values)) {
                        // TODO changes this to properly deal with collection for now just returning 1st item
                        ipAddress = values.get(0);
                    }
                }

                if (ipAddress != null && !ipAddress.isEmpty()) {
                    break; // only 1 IP-address per permMap
                }
            }

			for(String userName : policyItem.getUsers()) {
				for(RangerPolicyItemAccess access : policyItem.getAccesses()) {
					if(! access.getIsAllowed()) {
						continue;
					}
	
					VXPermMap permMap = new VXPermMap();
	
					permMap.setPermFor(AppConstants.XA_PERM_FOR_USER);
					permMap.setPermGroup(Integer.valueOf(permGroup).toString());
					permMap.setUserName(userName);
					permMap.setUserId(getUserId(userName));
					permMap.setPermType(toPermType(access.getType()));
					permMap.setIpAddress(ipAddress);
	
					permMapList.add(permMap);
				}
				
				if(policyItem.getDelegateAdmin()) {
					VXPermMap permMap = new VXPermMap();
	
					permMap.setPermFor(AppConstants.XA_PERM_FOR_USER);
					permMap.setPermGroup(Integer.valueOf(permGroup).toString());
					permMap.setUserName(userName);
					permMap.setUserId(getUserId(userName));
					permMap.setPermType(toPermType("Admin"));
					permMap.setIpAddress(ipAddress);
	
					permMapList.add(permMap);
				}
			}
			permGroup++;
	
			for(String groupName : policyItem.getGroups()) {
				for(RangerPolicyItemAccess access : policyItem.getAccesses()) {
					if(! access.getIsAllowed()) {
						continue;
					}
	
					VXPermMap permMap = new VXPermMap();
	
					permMap.setPermFor(AppConstants.XA_PERM_FOR_GROUP);
					permMap.setPermGroup(Integer.valueOf(permGroup).toString());
					permMap.setGroupName(groupName);
					permMap.setGroupId(getGroupId(groupName));
					permMap.setPermType(toPermType(access.getType()));
					permMap.setIpAddress(ipAddress);
	
					permMapList.add(permMap);
				}
				
				if(policyItem.getDelegateAdmin()) {
					VXPermMap permMap = new VXPermMap();
	
					permMap.setPermFor(AppConstants.XA_PERM_FOR_GROUP);
					permMap.setPermGroup(Integer.valueOf(permGroup).toString());
					permMap.setGroupName(groupName);
					permMap.setGroupId(getGroupId(groupName));
					permMap.setPermType(toPermType("Admin"));
					permMap.setIpAddress(ipAddress);
	
					permMapList.add(permMap);
				}
			}
			permGroup++;
		}
		return permMapList;
    }
	
	
	public List<VXPermObj> mapPermMapToPermObj(List<VXPermMap> permMapList) {

		List<VXPermObj> permObjList = new ArrayList<VXPermObj>();
		HashMap<String, List<VXPermMap>> sortedPemMap = new HashMap<String, List<VXPermMap>>();

		if (permMapList != null) {
			for (VXPermMap vXPermMap : permMapList) {

				String permGrp = vXPermMap.getPermGroup();
				List<VXPermMap> sortedList = sortedPemMap.get(permGrp);
				if (sortedList == null) {
					sortedList = new ArrayList<VXPermMap>();
					sortedPemMap.put(permGrp, sortedList);
				}
				sortedList.add(vXPermMap);
			}
		}

		for (Entry<String, List<VXPermMap>> entry : sortedPemMap.entrySet()) {
			VXPermObj vXPermObj = new VXPermObj();
			List<String> userList = new ArrayList<String>();
			List<String> groupList = new ArrayList<String>();
			List<String> permList = new ArrayList<String>();
			String ipAddress = "";

			List<VXPermMap> permListForGrp = entry.getValue();

			for (VXPermMap permMap : permListForGrp) {
				if (permMap.getPermFor() == AppConstants.XA_PERM_FOR_USER) {
					if (!userList.contains(permMap.getUserName())) {
						userList.add(permMap.getUserName());
					}
				} else if (permMap.getPermFor() == AppConstants.XA_PERM_FOR_GROUP) {
					if (!groupList.contains(permMap.getGroupName())) {
						groupList.add(permMap.getGroupName());
					}					
				}
				String perm = AppConstants.getLabelFor_XAPermType(permMap
						.getPermType());
				if (!permList.contains(perm)) {
					permList.add(perm);
				}
				ipAddress = permMap.getIpAddress();
			}
			vXPermObj.setUserList(userList);
			vXPermObj.setGroupList(groupList);
			vXPermObj.setPermList(permList);
			vXPermObj.setIpAddress(ipAddress);

			permObjList.add(vXPermObj);
		}
		return permObjList;
	}
	
	
	public RangerPolicy toRangerPolicy(VXPolicy vXPolicy, RangerService service ) {
		if(vXPolicy == null || service == null || toAssetType(service.getType()) == null) {
			return null;
		}

		RangerPolicy ret = new RangerPolicy();

		ret = (RangerPolicy) dataObjectToRangerObject(vXPolicy, ret);

		ret.setService(service.getName());
		ret.setName(StringUtils.trim(vXPolicy.getPolicyName()));
		ret.setDescription(vXPolicy.getDescription());
		ret.setIsEnabled(vXPolicy.getIsEnabled() == true);
		ret.setIsAuditEnabled(vXPolicy.getIsAuditEnabled());

		Integer assetType = toAssetType(service.getType());
		
		Boolean isRecursive  =  Boolean.FALSE;
		if (assetType == RangerCommonEnums.ASSET_HDFS && vXPolicy.getIsRecursive() != null) {
			isRecursive      = vXPolicy.getIsRecursive();
		}
		
		Boolean isTableExcludes =  Boolean.FALSE;
		if ( vXPolicy.getTableType() != null) {
			isTableExcludes  = vXPolicy.getTableType().equals(RangerCommonEnums.getLabelFor_PolicyType(RangerCommonEnums.POLICY_EXCLUSION));
		}
		
		Boolean isColumnExcludes =  Boolean.FALSE;
		if ( vXPolicy.getColumnType() != null) {
			isColumnExcludes = vXPolicy.getColumnType().equals(RangerCommonEnums.getLabelFor_PolicyType(RangerCommonEnums.POLICY_EXCLUSION));
		}
		
		if (assetType == RangerCommonEnums.ASSET_HDFS && vXPolicy.getResourceName() != null ) {
			toRangerResourceList(vXPolicy.getResourceName(), "path", Boolean.FALSE, isRecursive, ret.getResources());
		}
		
		if (vXPolicy.getTables() != null) {
			toRangerResourceList(vXPolicy.getTables(), "table", isTableExcludes, isRecursive, ret.getResources());
		}
		
		if (vXPolicy.getColumnFamilies() != null) {
			toRangerResourceList(vXPolicy.getColumnFamilies(), "column-family", Boolean.FALSE, isRecursive, ret.getResources());
		}
		
		if (vXPolicy.getColumns() != null) {
			toRangerResourceList(vXPolicy.getColumns(), "column", isColumnExcludes, isRecursive, ret.getResources());
		}
		
		if (vXPolicy.getDatabases() != null) {
			toRangerResourceList(vXPolicy.getDatabases(), "database", Boolean.FALSE, isRecursive, ret.getResources());
		}
		
		if (vXPolicy.getUdfs() != null) {
			toRangerResourceList(vXPolicy.getUdfs(), "udf", Boolean.FALSE, isRecursive, ret.getResources());
		}
		
		if (vXPolicy.getTopologies() != null) {
			toRangerResourceList(vXPolicy.getTopologies(), "topology", Boolean.FALSE, isRecursive, ret.getResources());
		}
		
		if (vXPolicy.getServices() != null) {
			toRangerResourceList(vXPolicy.getServices(), "service", Boolean.FALSE, isRecursive, ret.getResources());
		}

		if (vXPolicy.getHiveServices() != null) {
			toRangerResourceList(vXPolicy.getHiveServices(), "hiveservice", Boolean.FALSE, isRecursive, ret.getResources());
		}

		if ( vXPolicy.getPermMapList() != null) {
			List<VXPermObj> vXPermObjList = vXPolicy.getPermMapList();

			for(VXPermObj vXPermObj : vXPermObjList ) {
				List<String>                 userList   = new ArrayList<String>();
				List<String>                 groupList  = new ArrayList<String>();
				List<RangerPolicyItemAccess> accessList = new ArrayList<RangerPolicyItemAccess>();
				String                       ipAddress  = null;
				boolean 			    delegatedAdmin  = false;
				
				if (vXPermObj.getUserList() != null)  {
					for (String user : vXPermObj.getUserList() ) {
						if ( user.contains(getUserName(user))) {
						userList.add(user);
						}
					}
				 }
				
				if (vXPermObj.getGroupList() != null) {
					for (String group : vXPermObj.getGroupList()) {
						if ( group.contains(getGroupName(group))) {
							groupList.add(group);
						}
					}
				}
		
				if (vXPermObj.getPermList() != null) {
					for (String perm : vXPermObj.getPermList()) {
						if ( AppConstants.getEnumFor_XAPermType(perm) != 0 ) {
							if ("Admin".equalsIgnoreCase(perm)) {
								delegatedAdmin=true;
								if (assetType != RangerCommonEnums.ASSET_HBASE) {
									continue;
								}
							}
							accessList.add(new RangerPolicyItemAccess(perm));
						}
					}
				}
				
				if (vXPermObj.getIpAddress() != null ) {
					ipAddress =  vXPermObj.getIpAddress();
				}
				
				RangerPolicy.RangerPolicyItem policyItem = new RangerPolicy.RangerPolicyItem();
	
				policyItem.setUsers(userList);
				policyItem.setGroups(groupList);
				policyItem.setAccesses(accessList);
				
				if (delegatedAdmin) {
					policyItem.setDelegateAdmin(Boolean.TRUE);	
				} else {
					policyItem.setDelegateAdmin(Boolean.FALSE);	
				}
								
				if(ipAddress != null && !ipAddress.isEmpty()) {
					RangerPolicy.RangerPolicyItemCondition ipCondition = new RangerPolicy.RangerPolicyItemCondition("ipaddress", Collections.singletonList(ipAddress));
	
					policyItem.getConditions().add(ipCondition);
				}
				
				ret.getPolicyItems().add(policyItem);
			}
		}

		return ret;
	}
	
	private String getUserName(String userName) {
		if(userName == null || userName.isEmpty()) {
		
			XXUser xxUser = xaDaoMgr.getXXUser().findByUserName(userName);

			if(xxUser != null) {
				userName = xxUser.getName();
			}
		}
		return userName;
	}
	
	
	private String getGroupName(String groupName) {
	
		if(groupName == null || groupName.isEmpty()) {
				XXGroup xxGroup = xaDaoMgr.getXXGroup().findByGroupName(groupName);

				if(xxGroup != null) {
					groupName = xxGroup.getName();
				}
		}
		return groupName;
	}
	
	
	public VXPolicyList rangerPolicyListToPublic(List<RangerPolicy> rangerPolicyList,SearchFilter filter) {
		
		RangerService service       = null;
		List<VXPolicy> vXPolicyList = new ArrayList<VXPolicy>();

		VXPolicyList vXPolicyListObj = new VXPolicyList(new ArrayList<VXPolicy>());
		if(CollectionUtils.isNotEmpty(rangerPolicyList)) {
			int    totalCount = rangerPolicyList.size();
			int    startIndex = filter.getStartIndex();
			int    pageSize   = filter.getMaxRows();
			int    toIndex    = Math.min(startIndex + pageSize, totalCount);
			String sortType   = filter.getSortType();
			String sortBy     = filter.getSortBy();
			for(int i = startIndex; i < toIndex; i++) {
				RangerPolicy policy =rangerPolicyList.get(i);
				try {
					service = svcStore.getServiceByName(policy.getService());
				} catch(Exception excp) {
					throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
				}
				if(service  == null) {
					throw restErrorUtil.createRESTException(HttpServletResponse.SC_NOT_FOUND, RangerServiceNotFoundException.buildExceptionMsg(policy.getService()), true);
				}
				VXPolicy vXPolicy = toVXPolicy(policy,service);
				if(vXPolicy != null) {
					vXPolicyList.add(vXPolicy);
				}
			}
			vXPolicyListObj = new VXPolicyList(vXPolicyList);
			vXPolicyListObj.setPageSize(pageSize);
			vXPolicyListObj.setResultSize(vXPolicyList.size());
			vXPolicyListObj.setStartIndex(startIndex);
			vXPolicyListObj.setTotalCount(totalCount);
			vXPolicyListObj.setSortBy(sortBy);
			vXPolicyListObj.setSortType(sortType);
		}
		return vXPolicyListObj;
	}
	
	
	public GrantRevokeRequest toGrantRevokeRequest(VXPolicy vXPolicy) {
		String serviceType 	  = null;
		RangerService service = null;
		GrantRevokeRequest ret 	  = new GrantRevokeRequest();
		
		if ( vXPolicy != null) {
			String		  serviceName = vXPolicy.getRepositoryName();
			try {
				service = svcStore.getServiceByName(serviceName);
			} catch (Exception e) {
				  LOG.error( HttpServletResponse.SC_BAD_REQUEST + "No Service Found for ServiceName:" + serviceName );
				  throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, e.getMessage() + serviceName, true);
			}
			
			if ( service != null) {
				serviceType = service.getType();
			} else {
			  LOG.error( HttpServletResponse.SC_BAD_REQUEST + "No Service Found for ServiceName" + serviceName );
			  throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, "No Service Found for ServiceName" + serviceName, true);
			}
			
			if (vXPolicy.getGrantor() != null) {
				ret.setGrantor(vXPolicy.getGrantor());
			}
			ret.setEnableAudit(Boolean.TRUE);
			
			ret.setIsRecursive(Boolean.FALSE);
			
			ret.setReplaceExistingPermissions(toBooleanReplacePerm(vXPolicy.isReplacePerm()));
		
			Integer assetType = toAssetType(serviceType);
			
			if (assetType == RangerCommonEnums.ASSET_HIVE) {
				
				String database = StringUtils.isEmpty(vXPolicy.getDatabases()) ? "*" : vXPolicy.getDatabases();
				String table    = getTableOrUdf(vXPolicy);
				String column   = StringUtils.isEmpty(vXPolicy.getColumns()) ? "*" : vXPolicy.getColumns();
	
				Map<String, String> mapResource = new HashMap<String, String>();
				mapResource.put("database", database);
				mapResource.put("table", table);
				mapResource.put("column", column);
				ret.setResource(mapResource);
			}
			else if ( assetType == RangerCommonEnums.ASSET_HBASE) {
				
				String tableName = vXPolicy.getTables();
					   tableName = StringUtil.isEmpty(tableName) ? "*" : tableName;
					
				String colFamily = vXPolicy.getColumnFamilies();
					   colFamily = StringUtil.isEmpty(colFamily) ? "*": colFamily;
					
				String qualifier = vXPolicy.getColumns();
					   qualifier = StringUtil.isEmpty(qualifier) ? "*" : qualifier;

				Map<String, String> mapResource = new HashMap<String, String>();
				mapResource.put("table", tableName);
				mapResource.put("column-family", colFamily);
				mapResource.put("column", qualifier);
                                ret.setResource(mapResource);
				
			}
			
			List<VXPermObj> vXPermObjList = vXPolicy.getPermMapList();
			
			if (vXPermObjList != null) {
				for(VXPermObj vXPermObj : vXPermObjList ) {
					boolean delegatedAdmin  = false;
					
					if (vXPermObj.getUserList() != null ) {
						for (String user : vXPermObj.getUserList() ) {
							if ( user.contains(getUserName(user))) {
								ret.getUsers().add(user);
							}
						}
					}
					
					if (vXPermObj.getGroupList() != null) {
						for (String group : vXPermObj.getGroupList()) {
							if ( group.contains(getGroupName(group))) {
								ret.getGroups().add(group);
							}
						}
					}
			
					if(vXPermObj.getPermList() != null) {
						for (String perm : vXPermObj.getPermList()) {
							if ( AppConstants.getEnumFor_XAPermType(perm) != 0 ) {
								if ("Admin".equalsIgnoreCase(perm)) {
									delegatedAdmin=true;
									if (assetType!=null && assetType.intValue() != RangerCommonEnums.ASSET_HBASE) {
										continue;
									}
								}
								ret.getAccessTypes().add(perm);
							}
						}
					}
					
					if (delegatedAdmin) {
						ret.setDelegateAdmin(Boolean.TRUE);	
					} else {
						ret.setDelegateAdmin(Boolean.FALSE);	
					}
				 }
				
			  }
		}
		return ret;

	}
	
	private String getTableOrUdf(VXPolicy vXPolicy) {
		String ret   = null;
		String table = vXPolicy.getTables();
		String udf	 = vXPolicy.getUdfs();
		
		if (!StringUtils.isEmpty(table)) {
				ret = table;
		} else if (!StringUtils.isEmpty(udf)) {
				ret = udf;
		}
		return ret;
	}
	

	public boolean isValidateHttpsAuthentication( String serviceName, HttpServletRequest request) {		
		boolean isValidAuthentication=false;
		boolean httpEnabled = PropertiesUtil.getBooleanProperty("ranger.service.http.enabled",true);
		X509Certificate[] certchain = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");
		String ipAddress = request.getHeader("X-FORWARDED-FOR");
		if (ipAddress == null) {
			ipAddress = request.getRemoteAddr();
		}
		boolean isSecure = request.isSecure();

		if (serviceName == null || serviceName.isEmpty()) {			
			LOG.error("ServiceName not provided");
			throw restErrorUtil.createRESTException("Unauthorized access.",
					MessageEnums.OPER_NOT_ALLOWED_FOR_ENTITY);
		}

		RangerService service = null;
		try {
			service = svcStore.getServiceByName(serviceName);
		} catch (Exception e) {
			LOG.error("Requested Service not found. serviceName=" + serviceName);
			throw restErrorUtil.createRESTException("Service:" + serviceName + " not found",
					MessageEnums.DATA_NOT_FOUND);
		}
		if(service==null){
			LOG.error("Requested Service not found. serviceName=" + serviceName);
			throw restErrorUtil.createRESTException(HttpServletResponse.SC_NOT_FOUND, RangerServiceNotFoundException.buildExceptionMsg(serviceName),
					false);
		}
		if(!service.getIsEnabled()){
			LOG.error("Requested Service is disabled. serviceName=" + serviceName);
			throw restErrorUtil.createRESTException("Unauthorized access.",
					MessageEnums.OPER_NOT_ALLOWED_FOR_STATE);
		}		
		if (!httpEnabled) {
			if (!isSecure) {
				LOG.error("Unauthorized access. Only https is allowed. serviceName=" + serviceName);
				throw restErrorUtil.createRESTException("Unauthorized access -"
						+ " only https allowed",
						MessageEnums.OPER_NOT_ALLOWED_FOR_ENTITY);
			}
			if (certchain == null || certchain.length == 0) {
				LOG.error("Unauthorized access. Unable to get client certificate. serviceName=" + serviceName);
				throw restErrorUtil.createRESTException("Unauthorized access -"
						+ " unable to get client certificate",
						MessageEnums.OPER_NOT_ALLOWED_FOR_ENTITY);
			}

			// Check if common name is found in service config
			Map<String, String> configMap = service.getConfigs();
			String cnFromConfig = configMap.get("commonNameForCertificate");
			if (cnFromConfig == null || "".equals(cnFromConfig.trim())) {
				LOG.error("Unauthorized access. No common name for certificate set. Please check your service config");
				throw restErrorUtil.createRESTException("Unauthorized access. No common name for certificate set. Please check your service config",
						MessageEnums.OPER_NOT_ALLOWED_FOR_ENTITY);		
			}


			String cnFromConfigForTest = cnFromConfig;
			boolean isRegEx = cnFromConfig.toLowerCase().startsWith(REGEX_PREFIX_STR);
			if (isRegEx) {
				cnFromConfigForTest = cnFromConfig.substring(REGEX_PREFIX_STR_LENGTH);
			}

			// Perform SAN validation
			try {
			    Collection<List<?>> subjectAltNames = certchain[0].getSubjectAlternativeNames();
				if (subjectAltNames != null) {
					for (List<?> sanItem : subjectAltNames) {
						if (sanItem.size() == 2) {
							Integer sanType = (Integer) sanItem.get(0);
							String sanValue = (String) sanItem.get(1);
							if ( (sanType == 2 || sanType == 7) && (matchNames(sanValue, cnFromConfigForTest,isRegEx)) ) {
								if (LOG.isDebugEnabled()) LOG.debug("Client Cert verification successful, matched SAN:" + sanValue);
								isValidAuthentication=true;
								break;
							}
						}
					}
				}
			} catch (Throwable e) {
			    LOG.error("Unauthorized access. Error getting SAN from certificate", e);
				throw restErrorUtil.createRESTException("Unauthorized access - Error getting SAN from client certificate", MessageEnums.OPER_NOT_ALLOWED_FOR_ENTITY);						
			}

			// Perform common name validation only if SAN validation did not succeed
			if (!isValidAuthentication) {
				String commonName = null;
				if (certchain != null) {
					X509Certificate clientCert = certchain[0];
					String dn = clientCert.getSubjectX500Principal().getName();
					try {
						LdapName ln = new LdapName(dn);
						for (Rdn rdn : ln.getRdns()) {
							if ("CN".equalsIgnoreCase(rdn.getType())) {
								commonName = rdn.getValue() + "";
								break;
							}
						}
						if (commonName == null) {
							LOG.error("Unauthorized access. CName is null. serviceName=" + serviceName);
							throw restErrorUtil.createRESTException(
									"Unauthorized access - Unable to find Common Name from ["
											+ dn + "]",
											MessageEnums.OPER_NOT_ALLOWED_FOR_ENTITY);
						}
					} catch (InvalidNameException e) {
						LOG.error("Invalid Common Name. CName=" + commonName + ", serviceName=" + serviceName, e);
						throw restErrorUtil.createRESTException(
								"Unauthorized access - Invalid Common Name",
								MessageEnums.OPER_NOT_ALLOWED_FOR_ENTITY);
					}
				}		
				if (commonName != null) {
					if (matchNames(commonName, cnFromConfigForTest,isRegEx)) {
						if (LOG.isDebugEnabled()) LOG.debug("Client Cert verification successful, matched CN " + commonName + " with " + cnFromConfigForTest + ", wildcard match = " + isRegEx);
						isValidAuthentication=true;
					}

					if (!isValidAuthentication) {
						LOG.error("Unauthorized access. expected [" + cnFromConfigForTest + "], found ["
								+ commonName + "], serviceName=" + serviceName);
						throw restErrorUtil.createRESTException(
								"Unauthorized access. expected [" + cnFromConfigForTest
								+ "], found [" + commonName + "]",
								MessageEnums.OPER_NOT_ALLOWED_FOR_ENTITY);
					}
				}
			}
		} else {
			isValidAuthentication = true;
		}
		return isValidAuthentication;
	}

	public boolean isValidService(String serviceName, HttpServletRequest request){
		boolean isValid = true;
		if (serviceName == null || serviceName.isEmpty()) {
			LOG.error("ServiceName not provided");
			isValid = false;
			throw restErrorUtil.createRESTException("Unauthorized access.",
					MessageEnums.OPER_NOT_ALLOWED_FOR_ENTITY);
		}

		RangerService service = null;
		try {
			if(null != request.getAttribute("downloadPolicy") && StringUtils.equalsIgnoreCase(request.getAttribute("downloadPolicy").toString(), "secure")){
				service = svcStore.getServiceByNameForDP(serviceName);
			}else{
				service = svcStore.getServiceByName(serviceName);
			}
		} catch (Exception e) {
			isValid = false;
			LOG.error("Requested Service not found. serviceName=" + serviceName);
			throw restErrorUtil.createRESTException("Service:" + serviceName + " not found",
					MessageEnums.DATA_NOT_FOUND);
		}
		if(service==null){
			isValid = false;
			LOG.error("Requested Service not found. serviceName=" + serviceName);
			throw restErrorUtil.createRESTException(HttpServletResponse.SC_NOT_FOUND, RangerServiceNotFoundException.buildExceptionMsg(serviceName),
					false);
		}
		if(!service.getIsEnabled()){
			isValid = false;
			LOG.error("Requested Service is disabled. serviceName=" + serviceName);
			throw restErrorUtil.createRESTException("Unauthorized access.",
					MessageEnums.OPER_NOT_ALLOWED_FOR_STATE);
		}
		return isValid;
	}

   private boolean matchNames(String target, String source, boolean wildcardMatch) {
       boolean matched = false;
       if(target != null && source != null) {
           String names[] = (wildcardMatch ? new String[] { source } : source.split(","));
           for (String n:names) {

               if (wildcardMatch) {
                   if(LOG.isDebugEnabled()) LOG.debug("Wildcard Matching [" + target + "] with [" + n + "]");
            	   if (wildcardMatch(target,n)) {
            		   if(LOG.isDebugEnabled()) LOG.debug("Matched target:" + target + " with " + n);
            		   matched = true;
            		   break;
            	   }            	               	
               } else {
                   if(LOG.isDebugEnabled()) LOG.debug("Matching [" + target + "] with [" + n + "]");
            	   if (target.equalsIgnoreCase(n)) {
            		   if(LOG.isDebugEnabled()) LOG.debug("Matched target:" + target + " with " + n);
            		   matched = true;
            		   break;
            	   }            	
               }
           }
       } else {
    	   if(LOG.isDebugEnabled()) LOG.debug("source=[" + source + "],target=[" + target +"], returning false.");
       }
       return matched;
   }

   private boolean wildcardMatch(String target, String source) {
	   boolean matched = false;
	   if(target != null && source != null) {
		   try {
			   matched = target.matches(source);
		   } catch (Throwable e) {
			   LOG.error("Error doing wildcard match..", e);
		   }
	   } else {
    	   if(LOG.isDebugEnabled()) LOG.debug("source=[" + source + "],target=[" + target +"], returning false.");
       }
	   return matched;
   }

	
	private Boolean toBooleanReplacePerm(boolean isReplacePermission) {
		
		Boolean ret;
		
		if (isReplacePermission) {
			ret = Boolean.TRUE;
		} else {
			ret = Boolean.FALSE;
		}
		return ret;
	}

	private Integer getAssetType(RangerService service, String serviceName) {
		if(service == null || StringUtils.isEmpty(service.getType())) {
			try {
				service = svcStore.getServiceByName(serviceName);
			} catch (Exception e) {
				  LOG.info( HttpServletResponse.SC_BAD_REQUEST + "No Service Found for ServiceName:" + serviceName );
				  throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, e.getMessage() + serviceName, true);
			}
		}
		
		String serviceType = service != null ? service.getType() : null;

		Integer assetType = toAssetType(serviceType);
		
		return assetType;
	}

	public List<RangerPolicy> getMatchingPoliciesForResource(HttpServletRequest request,
			List<RangerPolicy> policyLists) {
		List<RangerPolicy> policies = new ArrayList<RangerPolicy>();
		final String serviceTypeForTag = EmbeddedServiceDefsUtil.EMBEDDED_SERVICEDEF_TAG_NAME;
		if (request != null) {
			String resource = request.getParameter(SearchFilter.POL_RESOURCE);
			String serviceType = request.getParameter(SearchFilter.SERVICE_TYPE);
			if (!StringUtil.isEmpty(resource) && !StringUtil.isEmpty(serviceType)) {
				List<String> resourceList = null;
				Map<String, RangerPolicy.RangerPolicyResource> rangerPolicyResourceMap = null;
				RangerPolicy.RangerPolicyResource rangerPolicyResource = null;
				for (RangerPolicy rangerPolicy : policyLists) {
					if (rangerPolicy != null) {
						if(serviceTypeForTag.equals(rangerPolicy.getServiceType())) {
							policies.add(rangerPolicy);
						}else {
							rangerPolicyResourceMap = rangerPolicy.getResources();
							if (rangerPolicyResourceMap != null) {
								if (rangerPolicyResourceMap.containsKey("path")) {
									rangerPolicyResource = rangerPolicyResourceMap.get("path");
									if (rangerPolicyResource != null) {
										resourceList = rangerPolicyResource.getValues();
										if (CollectionUtils.isNotEmpty(resourceList) && resourceList.contains(resource)) {
													policies.add(rangerPolicy);
										}
									}
								} else if (rangerPolicyResourceMap.containsKey("database")) {
									rangerPolicyResource = rangerPolicyResourceMap.get("database");
									if (rangerPolicyResource != null) {
										resourceList = rangerPolicyResource.getValues();
										if (CollectionUtils.isNotEmpty(resourceList) && resourceList.contains(resource)) {
													policies.add(rangerPolicy);
										}
									}
								}
							}
						}
					}
				}
				policyLists.clear();
				if (CollectionUtils.isNotEmpty(policies)) {
					policyLists.addAll(policies);
				}
			}
		}
		return policyLists;
	}
}
	
