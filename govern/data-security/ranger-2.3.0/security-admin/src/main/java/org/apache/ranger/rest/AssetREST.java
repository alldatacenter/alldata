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

 package org.apache.ranger.rest;

import java.io.File;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.Encoded;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.apache.ranger.admin.client.datatype.RESTResponse;
import org.apache.ranger.biz.AssetMgr;
import org.apache.ranger.biz.RangerBizUtil;
import org.apache.ranger.common.PropertiesUtil;
import org.apache.ranger.common.RESTErrorUtil;
import org.apache.ranger.common.RangerSearchUtil;
import org.apache.ranger.common.SearchCriteria;
import org.apache.ranger.common.ServiceUtil;
import org.apache.ranger.common.StringUtil;
import org.apache.ranger.common.annotation.RangerAnnotationClassName;
import org.apache.ranger.common.annotation.RangerAnnotationJSMgrName;
import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.entity.XXServiceDef;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerService;
import org.apache.ranger.plugin.store.EmbeddedServiceDefsUtil;
import org.apache.ranger.plugin.util.GrantRevokeRequest;
import org.apache.ranger.plugin.util.SearchFilter;
import org.apache.ranger.plugin.util.ServicePolicies;
import org.apache.ranger.security.context.RangerAPIList;
import org.apache.ranger.service.XAccessAuditService;
import org.apache.ranger.service.XAssetService;
import org.apache.ranger.service.XCredentialStoreService;
import org.apache.ranger.service.XPolicyExportAuditService;
import org.apache.ranger.service.XPolicyService;
import org.apache.ranger.service.XResourceService;
import org.apache.ranger.service.XTrxLogService;
import org.apache.ranger.view.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Path("assets")
@Component
@Scope("request")
@RangerAnnotationJSMgrName("AssetMgr")
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class AssetREST {
	private static final Logger logger = LoggerFactory.getLogger(AssetREST.class);

	@Autowired
	RangerSearchUtil searchUtil;

	@Autowired
	AssetMgr assetMgr;

	@Autowired
	XAssetService xAssetService;

	@Autowired
	XResourceService xResourceService;
	
	@Autowired
	XPolicyService xPolicyService;

	@Autowired
	XCredentialStoreService xCredentialStoreService;

	@Autowired
	RESTErrorUtil restErrorUtil;
	
	@Autowired
	XPolicyExportAuditService xPolicyExportAudits;
	
	@Autowired
	XTrxLogService xTrxLogService;
	
	@Autowired
	RangerBizUtil msBizUtil;

	@Autowired
	XAccessAuditService xAccessAuditService;

	@Autowired
	ServiceUtil serviceUtil;

	@Autowired
	ServiceREST serviceREST;

	@Autowired
	RangerDaoManager daoManager;
	
	@GET
	@Path("/assets/{id}")
	@Produces({ "application/json", "application/xml" })
	@PreAuthorize("@rangerPreAuthSecurityHandler.isAPIAccessible(\"" + RangerAPIList.GET_X_ASSET + "\")")
	public VXAsset getXAsset(@PathParam("id") Long id) {
		if(logger.isDebugEnabled()) {
			logger.debug("==> AssetREST.getXAsset(" + id + ")");
		}

		RangerService service = serviceREST.getService(id);

		VXAsset ret = serviceUtil.toVXAsset(service);

		if(logger.isDebugEnabled()) {
			logger.debug("<== AssetREST.getXAsset(" + id + "): " + ret);
		}

		return ret;
	}

	@POST
	@Path("/assets")
	@Produces({ "application/json", "application/xml" })
	@PreAuthorize("@rangerPreAuthSecurityHandler.isAPIAccessible(\"" + RangerAPIList.CREATE_X_ASSET + "\")")
	public VXAsset createXAsset(VXAsset vXAsset) {
		if(logger.isDebugEnabled()) {
			logger.debug("==> AssetREST.createXAsset(" + vXAsset + ")");
		}

		RangerService service = serviceUtil.toRangerService(vXAsset);

		RangerService createdService = serviceREST.createService(service);
		
		VXAsset ret = serviceUtil.toVXAsset(createdService);

		if(logger.isDebugEnabled()) {
			logger.debug("<== AssetREST.createXAsset(" + vXAsset + "): " + ret);
		}

		return ret;
	}

	@PUT
	@Path("/assets/{id}")
	@Produces({ "application/json", "application/xml" })
	@PreAuthorize("@rangerPreAuthSecurityHandler.isAPIAccessible(\"" + RangerAPIList.UPDATE_X_ASSET + "\")")
	public VXAsset updateXAsset(VXAsset vXAsset) {
		if(logger.isDebugEnabled()) {
			logger.debug("==> AssetREST.updateXAsset(" + vXAsset + ")");
		}

		RangerService service = serviceUtil.toRangerService(vXAsset);

		RangerService updatedService = serviceREST.updateService(service, null);
		
		VXAsset ret = serviceUtil.toVXAsset(updatedService);

		if(logger.isDebugEnabled()) {
			logger.debug("<== AssetREST.updateXAsset(" + vXAsset + "): " + ret);
		}

		return ret;
	}

	@DELETE
	@Path("/assets/{id}")
	@RangerAnnotationClassName(class_name = VXAsset.class)
	@PreAuthorize("@rangerPreAuthSecurityHandler.isAPIAccessible(\"" + RangerAPIList.DELETE_X_ASSET + "\")")
	public void deleteXAsset(@PathParam("id") Long id,
			@Context HttpServletRequest request) {
		if(logger.isDebugEnabled()) {
			logger.debug("==> AssetREST.deleteXAsset(" + id + ")");
		}

		serviceREST.deleteService(id);

		if(logger.isDebugEnabled()) {
			logger.debug("<== AssetREST.deleteXAsset(" + id + ")");
		}
	}

	@POST
	@Path("/assets/testConfig")
	@Produces({ "application/json", "application/xml" })
	@PreAuthorize("@rangerPreAuthSecurityHandler.isAPIAccessible(\"" + RangerAPIList.TEST_CONFIG + "\")")
	public VXResponse configTest(VXAsset vXAsset) {
		if(logger.isDebugEnabled()) {
			logger.debug("==> AssetREST.configTest(" + vXAsset + ")");
		}

		RangerService service = serviceUtil.toRangerService(vXAsset);

		VXResponse ret = serviceREST.validateConfig(service);

		if(logger.isDebugEnabled()) {
			logger.debug("<== AssetREST.testConfig(" + vXAsset + "): " + ret);
		}

		return ret;
	}

	@GET
	@Path("/assets")
	@Produces({ "application/json", "application/xml" })
	@PreAuthorize("@rangerPreAuthSecurityHandler.isAPIAccessible(\"" + RangerAPIList.SEARCH_X_ASSETS + "\")")
	public VXAssetList searchXAssets(@Context HttpServletRequest request) {
		if(logger.isDebugEnabled()) {
			logger.debug("==> AssetREST.searchXAssets()");
		}

		VXAssetList ret = new VXAssetList();

		SearchFilter filter = searchUtil.getSearchFilterFromLegacyRequestForRepositorySearch(request, xAssetService.sortFields);

		List<RangerService> services = serviceREST.getServices(filter);

		if(services != null) {
			List<VXAsset> assets = new ArrayList<VXAsset>();

			for(RangerService service : services) {
				VXAsset asset = serviceUtil.toVXAsset(service);
				
				if(asset != null) {
					assets.add(asset);
				}
			}

			ret.setVXAssets(assets);
		}

		if(logger.isDebugEnabled()) {
			logger.debug("<== AssetREST.searchXAssets(): count=" +  ret.getListSize());
		}

		return ret;
	}

	@GET
	@Path("/assets/count")
	@Produces({ "application/json", "application/xml" })
	@PreAuthorize("@rangerPreAuthSecurityHandler.isAPIAccessible(\"" + RangerAPIList.COUNT_X_ASSETS + "\")")
	public VXLong countXAssets(@Context HttpServletRequest request) {
		if(logger.isDebugEnabled()) {
			logger.debug("==> AssetREST.countXAssets()");
		}

		VXLong ret = new VXLong();

		ret.setValue(searchXAssets(request).getListSize());

		if(logger.isDebugEnabled()) {
			logger.debug("<== AssetREST.countXAssets(): " + ret);
		}

		return ret;
	}

	@GET
	@Path("/resources/{id}")
	@Produces({ "application/json", "application/xml" })
	public VXResource getXResource(@PathParam("id") Long id) {
		if(logger.isDebugEnabled()) {
			logger.debug("==> AssetREST.getXResource(" + id + ")");
		}

		RangerPolicy  policy  = null;
		RangerService service = null;

		policy = serviceREST.getPolicy(id);
		
		if(policy != null) {
			service = serviceREST.getServiceByName(policy.getService());
		}

		VXResource ret = serviceUtil.toVXResource(policy, service);

		if(logger.isDebugEnabled()) {
			logger.debug("<== AssetREST.getXResource(" + id + "): " + ret);
		}

		return ret;
	}

	@POST
	@Path("/resources")
	@Produces({ "application/json", "application/xml" })
	public VXResource createXResource(VXResource vXResource) {
		if(logger.isDebugEnabled()) {
			logger.debug("==> AssetREST.createXResource(" + vXResource + ")");
		}

		RangerService service = serviceREST.getService(vXResource.getAssetId());
		RangerPolicy  policy  = serviceUtil.toRangerPolicy(vXResource, service);

		RangerPolicy createdPolicy = serviceREST.createPolicy(policy, null);

		VXResource ret = serviceUtil.toVXResource(createdPolicy, service);

		if(logger.isDebugEnabled()) {
			logger.debug("<== AssetREST.createXResource(" + vXResource + "): " + ret);
		}

		return ret;
	}
	
	@PUT
	@Path("/resources/{id}")
	@Produces({ "application/json", "application/xml" })
	public VXResource updateXResource(VXResource vXResource) {
		if(logger.isDebugEnabled()) {
			logger.debug("==> AssetREST.updateXResource(" + vXResource + ")");
		}

		RangerService service = serviceREST.getService(vXResource.getAssetId());
		RangerPolicy  policy  = serviceUtil.toRangerPolicy(vXResource, service);

		RangerPolicy updatedPolicy = serviceREST.updatePolicy(policy);
		
		VXResource ret = serviceUtil.toVXResource(updatedPolicy, service);

		if(logger.isDebugEnabled()) {
			logger.debug("<== AssetREST.updateXResource(" + vXResource + "): " + ret);
		}

		return ret;
	}

	@DELETE
	@Path("/resources/{id}")
	@PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
	@RangerAnnotationClassName(class_name = VXResource.class)
	public void deleteXResource(@PathParam("id") Long id,
			@Context HttpServletRequest request) {
		if(logger.isDebugEnabled()) {
			logger.debug("==> AssetREST.deleteXResource(" + id + ")");
		}

		serviceREST.deletePolicy(id);

		if(logger.isDebugEnabled()) {
			logger.debug("<== AssetREST.deleteXResource(" + id + ")");
		}
	}

	@GET
	@Path("/resources")
	@Produces({ "application/json", "application/xml" })
	public VXResourceList searchXResources(@Context HttpServletRequest request) {
		if(logger.isDebugEnabled()) {
			logger.debug("==> AssetREST.searchXResources()");
		}

		VXResourceList ret = new VXResourceList();

		SearchFilter filter = searchUtil.getSearchFilterFromLegacyRequest(request, xResourceService.sortFields);

		List<RangerPolicy> policies = serviceREST.getPolicies(filter);

		if(policies != null) {
			List<VXResource> resources = new ArrayList<VXResource>();

			for(RangerPolicy policy : policies) {
				RangerService service = serviceREST.getServiceByName(policy.getService());

				VXResource resource = serviceUtil.toVXResource(policy, service);

				if(resource != null) {
					resources.add(resource);
				}
			}

			ret.setVXResources(resources);
		}

		if(logger.isDebugEnabled()) {
			logger.debug("<== AssetREST.searchXResources(): count=" + ret.getResultSize());
		}

		return ret;
	}

	@GET
	@Path("/resources/count")
	@Produces({ "application/json", "application/xml" })
	public VXLong countXResources(@Context HttpServletRequest request) {
		if(logger.isDebugEnabled()) {
			logger.debug("==> AssetREST.countXResources()");
		}

		VXLong ret = new VXLong();

		ret.setValue(searchXResources(request).getListSize());

		if(logger.isDebugEnabled()) {
			logger.debug("<== AssetREST.countXAssets(): " + ret);
		}

		return ret;
	}

	@GET
	@Path("/credstores/{id}")
	@Produces({ "application/json", "application/xml" })
	public VXCredentialStore getXCredentialStore(@PathParam("id") Long id) {
		return assetMgr.getXCredentialStore(id);
	}

	@POST
	@Path("/credstores")
	@Produces({ "application/json", "application/xml" })
	public VXCredentialStore createXCredentialStore(
			VXCredentialStore vXCredentialStore) {
		return assetMgr.createXCredentialStore(vXCredentialStore);
	}

	@PUT
	@Path("/credstores")
	@Produces({ "application/json", "application/xml" })
	public VXCredentialStore updateXCredentialStore(
			VXCredentialStore vXCredentialStore) {
		return assetMgr.updateXCredentialStore(vXCredentialStore);
	}

	@DELETE
	@Path("/credstores/{id}")
	@PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
	@RangerAnnotationClassName(class_name = VXCredentialStore.class)
	public void deleteXCredentialStore(@PathParam("id") Long id,
			@Context HttpServletRequest request) {
		boolean force = false;
		assetMgr.deleteXCredentialStore(id, force);
	}

	@GET
	@Path("/credstores")
	@Produces({ "application/json", "application/xml" })
	public VXCredentialStoreList searchXCredentialStores(
			@Context HttpServletRequest request) {
		SearchCriteria searchCriteria = searchUtil.extractCommonCriterias(
				request, xCredentialStoreService.sortFields);
		return assetMgr.searchXCredentialStores(searchCriteria);
	}

	@GET
	@Path("/credstores/count")
	@Produces({ "application/json", "application/xml" })
	public VXLong countXCredentialStores(@Context HttpServletRequest request) {
		SearchCriteria searchCriteria = searchUtil.extractCommonCriterias(
				request, xCredentialStoreService.sortFields);
		return assetMgr.getXCredentialStoreSearchCount(searchCriteria);
	}

	@GET
	@Path("/resource/{id}")
	public Response getXResourceFile(@Context HttpServletRequest request,
			@PathParam("id") Long id) {
		String fileType = searchUtil.extractString(request,
				new SearchCriteria(), "fileType", "File type",
				StringUtil.VALIDATION_TEXT);

		VXResource resource = getXResource(id);


		Response response=null;
		if(resource!=null && StringUtils.isNotEmpty(fileType)){
			File file = null;
			file=assetMgr.getXResourceFile(resource, fileType);
			if(file!=null){
				response=Response.ok(file, MediaType.APPLICATION_OCTET_STREAM).header("Content-Disposition","attachment;filename=" + file.getName()).build();
				file=null;
			}
		}
		return response;
	}

	@GET
	@Path("/policyList/{repository}")
	@Encoded
	public String getResourceJSON(@Context HttpServletRequest request,
			@PathParam("repository") String repository) {
		
		String            epoch       = request.getParameter("epoch");
		X509Certificate[] certchain   = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");
		String            ipAddress   = request.getHeader("X-FORWARDED-FOR");
		boolean           isSecure    = request.isSecure();
		String            policyCount = request.getParameter("policyCount");
		String            agentId     = request.getParameter("agentId");
		Long              lastKnowPolicyVersion = Long.valueOf(-1);
		String            capabilityVector = "0";

		if (ipAddress == null) {
			ipAddress = request.getRemoteAddr();
		}

		boolean httpEnabled = PropertiesUtil.getBooleanProperty("ranger.service.http.enabled",true);

		ServicePolicies servicePolicies = null;

		try {
			servicePolicies = serviceREST.getServicePoliciesIfUpdated(repository, lastKnowPolicyVersion, 0L, agentId, "", "", false, capabilityVector, request);
		} catch(Exception excp) {
			logger.error("failed to retrieve policies for repository " + repository, excp);
		}

		RangerService      service       = serviceUtil.getServiceByName(repository);
		List<RangerPolicy> policies      = servicePolicies != null ? servicePolicies.getPolicies() : null;
		long               policyUpdTime = (servicePolicies != null && servicePolicies.getPolicyUpdateTime() != null) ? servicePolicies.getPolicyUpdateTime().getTime() : 0l;
		VXAsset            vAsset        = serviceUtil.toVXAsset(service);
		List<VXResource>   vResourceList = new ArrayList<VXResource>();
		
		if(policies != null) {
			for(RangerPolicy policy : policies) {
				vResourceList.add(serviceUtil.toVXResource(policy, service));
			}
		}

		String file = assetMgr.getLatestRepoPolicy(vAsset, vResourceList, policyUpdTime,
				certchain, httpEnabled, epoch, ipAddress, isSecure, policyCount, agentId);
		
		return file;
	}

	@GET
	@Path("/exportAudit")
	@Produces({ "application/json", "application/xml" })
	@PreAuthorize("@rangerPreAuthSecurityHandler.isAPIAccessible(\"" + RangerAPIList.SEARCH_X_POLICY_EXPORT_AUDITS + "\")")
	public VXPolicyExportAuditList searchXPolicyExportAudits(
			@Context HttpServletRequest request) {

		SearchCriteria searchCriteria = searchUtil.extractCommonCriterias(
				request, xPolicyExportAudits.sortFields);
		searchUtil.extractString(request, searchCriteria, "agentId",
				"The XA agent id pulling the policies.",
				StringUtil.VALIDATION_TEXT);
		searchUtil.extractString(request, searchCriteria, "clientIP",
				"The XA agent ip pulling the policies.",
				StringUtil.VALIDATION_TEXT);		
		searchUtil.extractString(request, searchCriteria, "repositoryName",
				"Repository name for which export was done.",
				StringUtil.VALIDATION_TEXT);
		searchUtil.extractInt(request, searchCriteria, "httpRetCode",
				"HTTP response code for exported policy.");
		searchUtil.extractDate(request, searchCriteria, "startDate",
				"Start Date", null);
		searchUtil.extractDate(request, searchCriteria, "endDate",
                                "End Date", null);
		searchUtil.extractString(request, searchCriteria, "cluster",
				"Cluster Name", StringUtil.VALIDATION_TEXT);
		searchUtil.extractString(request, searchCriteria, "zoneName",
				"Zone Name", StringUtil.VALIDATION_TEXT);
		return assetMgr.searchXPolicyExportAudits(searchCriteria);
	}

	@GET
	@Path("/report")
	@Produces({ "application/json", "application/xml" })
	@PreAuthorize("@rangerPreAuthSecurityHandler.isAPIAccessible(\"" + RangerAPIList.GET_REPORT_LOGS + "\")")
	public VXTrxLogList getReportLogs(@Context HttpServletRequest request){

		SearchCriteria searchCriteria = searchUtil.extractCommonCriterias(
				request, xTrxLogService.sortFields);
                searchUtil.extractInt(request, searchCriteria, "objectClassType", "audit type.");
		searchUtil.extractString(request, searchCriteria, "attributeName",
				"Attribute Name", StringUtil.VALIDATION_TEXT);
		searchUtil.extractString(request, searchCriteria, "action",
				"CRUD Action Type", StringUtil.VALIDATION_TEXT);
		searchUtil.extractString(request, searchCriteria, "sessionId",
				"Session Id", StringUtil.VALIDATION_TEXT);
		searchUtil.extractString(request, searchCriteria, "owner",
				"Owner", StringUtil.VALIDATION_TEXT);
		searchUtil.extractDate(request, searchCriteria, "startDate", "Trasaction date since", "MM/dd/yyyy");
		searchUtil.extractDate(request, searchCriteria, "endDate", "Trasaction date till", "MM/dd/yyyy");
		return assetMgr.getReportLogs(searchCriteria);
	}
	
	@GET
	@Path("/report/{transactionId}")
	@Produces({ "application/json", "application/xml" })
	@PreAuthorize("@rangerPreAuthSecurityHandler.isAPIAccessible(\"" + RangerAPIList.GET_TRANSACTION_REPORT + "\")")
	public VXTrxLogList getTransactionReport(@Context HttpServletRequest request,
			@PathParam("transactionId") String transactionId){
		return assetMgr.getTransactionReport(transactionId);
	}
	
	@GET
	@Path("/accessAudit")
	@Produces({ "application/json", "application/xml" })
	@PreAuthorize("@rangerPreAuthSecurityHandler.isAPIAccessible(\"" + RangerAPIList.GET_ACCESS_LOGS + "\")")
	public VXAccessAuditList getAccessLogs(@Context HttpServletRequest request){
		SearchCriteria searchCriteria = searchUtil.extractCommonCriterias(
				request, xAccessAuditService.sortFields);
		searchUtil.extractString(request, searchCriteria, "accessType",
				"Access Type", StringUtil.VALIDATION_TEXT);
		searchUtil.extractString(request, searchCriteria, "aclEnforcer",
				"Access Enforcer", StringUtil.VALIDATION_TEXT);
		searchUtil.extractString(request, searchCriteria, "agentId",
				"Application", StringUtil.VALIDATION_TEXT);
		searchUtil.extractString(request, searchCriteria, "repoName",
				"Service Name", StringUtil.VALIDATION_TEXT);
		searchUtil.extractString(request, searchCriteria, "sessionId",
				"Session ID", StringUtil.VALIDATION_TEXT);
		searchUtil.extractStringList(request, searchCriteria, "requestUser",
			"Users", "requestUser", null, StringUtil.VALIDATION_TEXT);
		searchUtil.extractStringList(request, searchCriteria, "excludeUser",
			"Exclude Users", "-requestUser", null, StringUtil.VALIDATION_TEXT);
		searchUtil.extractString(request, searchCriteria, "requestData",
				"Request Data", StringUtil.VALIDATION_TEXT);
		searchUtil.extractString(request, searchCriteria, "resourcePath",
				"Resource Name", StringUtil.VALIDATION_TEXT);
		searchUtil.extractString(request, searchCriteria, "clientIP",
				"Client IP", StringUtil.VALIDATION_TEXT);
		searchUtil.extractString(request, searchCriteria, "resourceType",
				"Resource Type", StringUtil.VALIDATION_TEXT);
		searchUtil.extractString(request,searchCriteria,"excludeServiceUser",
				"Exclude Service User",StringUtil.VALIDATION_TEXT);

		searchUtil.extractInt(request, searchCriteria, "auditType", "Audit Type");
		searchUtil.extractInt(request, searchCriteria, "accessResult", "Result");
		searchUtil.extractInt(request, searchCriteria, "assetId", "Asset ID");
		searchUtil.extractLong(request, searchCriteria, "policyId", "Policy ID");
		searchUtil.extractInt(request, searchCriteria, "repoType", "Service Type");

		searchUtil.extractDate(request, searchCriteria, "startDate","Start Date", "MM/dd/yyyy");
		searchUtil.extractDate(request, searchCriteria, "endDate", "End Date", "MM/dd/yyyy");

		searchUtil.extractString(request, searchCriteria, "tags", "tags", null);
		searchUtil.extractString(request, searchCriteria, "cluster", "Cluster Name", StringUtil.VALIDATION_TEXT);
		searchUtil.extractStringList(request, searchCriteria, "zoneName", "Zone Name List", "zoneName", null, null);

		searchUtil.extractString(request, searchCriteria, "agentHost", "Agent Host Name", StringUtil.VALIDATION_TEXT);

		searchUtil.extractString(request, searchCriteria, "eventId", "Event Id", null);

		boolean isKeyAdmin = msBizUtil.isKeyAdmin();
		boolean isAuditKeyAdmin = msBizUtil.isAuditKeyAdmin();
		XXServiceDef xxServiceDef = daoManager.getXXServiceDef().findByName(EmbeddedServiceDefsUtil.EMBEDDED_SERVICEDEF_KMS_NAME);
		if(isKeyAdmin && xxServiceDef != null || isAuditKeyAdmin && xxServiceDef != null){
			searchCriteria.getParamList().put("repoType", xxServiceDef.getId());
		}
		else if (xxServiceDef != null) {
			searchCriteria.getParamList().put("-repoType", xxServiceDef.getId());
		}
		return assetMgr.getAccessLogs(searchCriteria);
	}
	
	@POST
	@Path("/resources/grant")
	@Produces({ "application/json", "application/xml" })
	public VXPolicy grantPermission(@Context HttpServletRequest request,VXPolicy vXPolicy) {
		
		RESTResponse ret = null;
		
		if(logger.isDebugEnabled()) {
			logger.debug("==> AssetREST.grantPermission(" + vXPolicy + ")");
		}
		
		if ( vXPolicy != null) {
			String		  serviceName = vXPolicy.getRepositoryName();
			GrantRevokeRequest grantRevokeRequest = serviceUtil.toGrantRevokeRequest(vXPolicy);
			try {
				ret = serviceREST.grantAccess(serviceName, grantRevokeRequest, request);
			} catch(WebApplicationException excp) {
				throw excp;
			} catch (Throwable e) {
				  logger.error( HttpServletResponse.SC_BAD_REQUEST + "Grant Access Failed for the request " + vXPolicy, e);
				  throw restErrorUtil.createRESTException("Grant Access Failed for the request: " + vXPolicy + ". " + e.getMessage());
			}
		} else {
			 logger.error( HttpServletResponse.SC_BAD_REQUEST + "Bad Request parameter");
			 throw restErrorUtil.createRESTException("Bad Request parameter");
		}
		
		if(logger.isDebugEnabled()) {
			logger.debug("<== AssetREST.grantPermission(" + ret + ")");
		}
		
		// TO DO Current Grant REST doesn't return a policy so returning a null value. Has to be replace with VXpolicy.
		return vXPolicy;
	}
	
	@POST
	@Path("/resources/revoke")
	@Produces({ "application/json", "application/xml" })
	public VXPolicy revokePermission(@Context HttpServletRequest request,VXPolicy vXPolicy) {
		
		RESTResponse ret = null;
		
		if(logger.isDebugEnabled()) {
			logger.debug("==> AssetREST.revokePermission(" + vXPolicy + ")");
		}
		
		if ( vXPolicy != null) {
			String		  serviceName = vXPolicy.getRepositoryName();
			GrantRevokeRequest grantRevokeRequest = serviceUtil.toGrantRevokeRequest(vXPolicy);
			try {
				 ret = serviceREST.revokeAccess(serviceName, grantRevokeRequest, request);
			} catch(WebApplicationException excp) {
				throw excp;
			} catch (Throwable e) {
				  logger.error( HttpServletResponse.SC_BAD_REQUEST + "Revoke Access Failed for the request " + vXPolicy, e);
				  throw restErrorUtil.createRESTException("Revoke Access Failed for the request: " + vXPolicy + ". " + e.getMessage());
			}
		} else {
			 logger.error( HttpServletResponse.SC_BAD_REQUEST + "Bad Request parameter");
			 throw restErrorUtil.createRESTException("Bad Request parameter");
		}
		
		if(logger.isDebugEnabled()) {
			logger.debug("<== AssetREST.revokePermission(" + ret + ")");
		}
		return vXPolicy;
	}

	@GET
	@Path("/ugsyncAudits")
	@Produces({ "application/json", "application/xml" })
	@PreAuthorize("@rangerPreAuthSecurityHandler.isAPIAccessible(\"" + RangerAPIList.GET_UGSYNC_AUDITS + "\")")
	public VXUgsyncAuditInfoList getUgsyncAudits(@Context HttpServletRequest request){

		SearchCriteria searchCriteria = searchUtil.extractCommonCriterias(
				request, xAccessAuditService.sortFields);
		searchUtil.extractString(request, searchCriteria, "userName",
				"User Name", StringUtil.VALIDATION_TEXT);
		searchUtil.extractString(request, searchCriteria, "sessionId",
				"Session Id", StringUtil.VALIDATION_TEXT);
		searchUtil.extractString(request, searchCriteria, "syncSource",
				"Sync Source", StringUtil.VALIDATION_TEXT);
		searchUtil.extractString(request, searchCriteria, "syncSourceInfo",
				"Sync Source Info", StringUtil.VALIDATION_TEXT);
		searchUtil.extractLong(request, searchCriteria, "noOfUsers", "No of Users");
		searchUtil.extractLong(request, searchCriteria, "noOfGroups", "No of Groups");

		searchUtil.extractDate(request, searchCriteria, "startDate",
				"Start Date", "MM/dd/yyyy");
		searchUtil.extractDate(request, searchCriteria, "endDate", "End Date",
				"MM/dd/yyyy");
		return assetMgr.getUgsyncAudits(searchCriteria);
	}

	@GET
	@Path("/ugsyncAudits/{syncSource}")
	@Encoded
	@Produces({ "application/json", "application/xml" })
	@PreAuthorize("@rangerPreAuthSecurityHandler.isAPIAccessible(\"" + RangerAPIList.GET_UGSYNC_AUDITS_BY_SYNCSOURCE + "\")")
	public VXUgsyncAuditInfoList getUgsyncAuditsBySyncSource(@PathParam("syncSource") String syncSource){
		VXUgsyncAuditInfoList vxUgsyncAuditInfoList = new VXUgsyncAuditInfoList();
		vxUgsyncAuditInfoList = assetMgr.getUgsyncAuditsBySyncSource(syncSource);
		return vxUgsyncAuditInfoList;
	}
}
