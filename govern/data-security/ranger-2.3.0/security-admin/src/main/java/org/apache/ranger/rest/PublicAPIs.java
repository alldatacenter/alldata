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

import org.apache.ranger.common.*;
import org.apache.ranger.common.annotation.RangerAnnotationClassName;
import org.apache.ranger.common.annotation.RangerAnnotationJSMgrName;
import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.entity.XXPolicy;
import org.apache.ranger.entity.XXService;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerService;
import org.apache.ranger.plugin.util.SearchFilter;
import org.apache.ranger.service.RangerPolicyService;
import org.apache.ranger.service.XAssetService;
import org.apache.ranger.view.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;

import java.util.List;

@Path("public")
@Component
@Scope("request")
@RangerAnnotationJSMgrName("PublicMgr")
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class PublicAPIs {
	private static final Logger logger = LoggerFactory.getLogger(PublicAPIs.class);

	@Autowired
	RangerSearchUtil searchUtil;

	@Autowired
	XAssetService xAssetService;

	@Autowired
	RangerPolicyService policyService;

	@Autowired
	StringUtil stringUtil;

	@Autowired
	ServiceUtil serviceUtil;
	
	@Autowired
	ServiceREST serviceREST;

	@Autowired
	RangerDaoManager daoMgr;

	@Autowired
	RESTErrorUtil restErrorUtil;
	
	@GET
	@Path("/api/repository/{id}")
	@Produces({ "application/json", "application/xml" })
	public VXRepository getRepository(@PathParam("id") Long id) {
		if(logger.isDebugEnabled()) {
			logger.debug("==> PublicAPIs.getRepository(" + id + ")");
		}
		
		RangerService service = serviceREST.getService(id);
		
		VXRepository ret = serviceUtil.toVXRepository(service);

		if(logger.isDebugEnabled()) {
			logger.debug("<= PublicAPIs.getRepository(" + id + ")");
		}
		return ret;
	}
	
	
	@POST
	@Path("/api/repository/")
	@Produces({ "application/json", "application/xml" })
	public VXRepository createRepository(VXRepository vXRepository) {
		if(logger.isDebugEnabled()) {
			logger.debug("==> PublicAPIs.createRepository(" + vXRepository + ")");
		}
		
		VXAsset vXAsset  = serviceUtil.publicObjecttoVXAsset(vXRepository);
		
		RangerService service = serviceUtil.toRangerService(vXAsset);

		RangerService createdService = serviceREST.createService(service);
		
		VXAsset retvXAsset = serviceUtil.toVXAsset(createdService);
		
		VXRepository ret = serviceUtil.vXAssetToPublicObject(retvXAsset);
		
		if(logger.isDebugEnabled()) {
			logger.debug("<== PublicAPIs.createRepository(" + ret + ")");
		}
		
		return ret;
	}

	
	@PUT
	@Path("/api/repository/{id}")
	@Produces({ "application/json", "application/xml" })
	public VXRepository updateRepository(VXRepository vXRepository,
			@PathParam("id") Long id) {
		
		if(logger.isDebugEnabled()) {
			logger.debug("==> PublicAPIs.updateRepository(" + id + ")");
		}
		
		XXService existing = daoMgr.getXXService().getById(id);
		if(existing == null) {
			throw restErrorUtil.createRESTException("Repository not found for Id: " + id, MessageEnums.DATA_NOT_FOUND);
		}

		vXRepository.setId(id);
		
		VXAsset vXAsset  = serviceUtil.publicObjecttoVXAsset(vXRepository);

		RangerService service = serviceUtil.toRangerService(vXAsset);
		service.setVersion(existing.getVersion());

		RangerService updatedService = serviceREST.updateService(service, null);
		
		VXAsset retvXAsset = serviceUtil.toVXAsset(updatedService);
		
		VXRepository ret = serviceUtil.vXAssetToPublicObject(retvXAsset);
		
		if(logger.isDebugEnabled()) {
			logger.debug("<== PublicAPIs.updateRepository(" + ret + ")");
		}
		
		return ret;
	}

	
	@DELETE
	@Path("/api/repository/{id}")
	@PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
	@RangerAnnotationClassName(class_name = VXAsset.class)
	public void deleteRepository(@PathParam("id") Long id,
			@Context HttpServletRequest request) {

		if(logger.isDebugEnabled()) {
			logger.debug("==> PublicAPIs.deleteRepository(" + id + ")");
		}
		
		serviceREST.deleteService(id);
		
		if(logger.isDebugEnabled()) {
			logger.debug("<== PublicAPIs.deleteRepository(" + id + ")");
		}
	}
	
	@GET
	@Path("/api/repository/")
	@Produces({ "application/json", "application/xml" })
	public VXRepositoryList searchRepositories(
			@Context HttpServletRequest request) {
	
		if(logger.isDebugEnabled()) {
			logger.debug("==> PublicAPIs.searchRepositories()");
		}

		SearchFilter filter = searchUtil.getSearchFilterFromLegacyRequestForRepositorySearch(request, xAssetService.sortFields);

		List<RangerService> serviceList = serviceREST.getServices(filter);

		VXRepositoryList ret = null;

		if (serviceList != null) {
			ret = serviceUtil.rangerServiceListToPublicObjectList(serviceList);
		}

		if(logger.isDebugEnabled()) {
			logger.debug("<== PublicAPIs.searchRepositories(): count=" + (ret == null ? 0 : ret.getListSize()));
		}

		return ret;
	}


	@GET
	@Path("/api/repository/count")
	@Produces({ "application/json", "application/xml" })
	public VXLong countRepositories(@Context HttpServletRequest request) {

		if(logger.isDebugEnabled()) {
			logger.debug("==> PublicAPIs.countRepositories()");
		}

		VXRepositoryList repositories = searchRepositories(request);

		VXLong ret = new VXLong();
		ret.setValue(repositories == null ? 0 : repositories.getResultSize());

        if(logger.isDebugEnabled()) {
			logger.debug("<== PublicAPIs.countRepositories(): count=" + ret);
		}

        return ret;
	}	
	

	
	@GET
	@Path("/api/policy/{id}")
	@Produces({ "application/json", "application/xml" })
	public VXPolicy getPolicy(@PathParam("id") Long id) {
		
		if(logger.isDebugEnabled()) {
			logger.debug("==> PublicAPIs.getPolicy() " + id);
		}
		
		RangerPolicy  policy  = null;
		RangerService service = null;

		policy = serviceREST.getPolicy(id);
		
		if(policy != null) {
			service = serviceREST.getServiceByName(policy.getService());
		}

		VXPolicy ret = serviceUtil.toVXPolicy(policy, service);
	
		if(logger.isDebugEnabled()) {
			logger.debug("<== PublicAPIs.getPolicy()" + ret);
		}
		
		return ret;
	}
	

	@POST
	@Path("/api/policy")
	@Produces({ "application/json", "application/xml" })
	public VXPolicy createPolicy(VXPolicy vXPolicy) {
		
		if(logger.isDebugEnabled()) {
			logger.debug("==> PublicAPIs.createPolicy()");
		}

		if(vXPolicy == null) {
		    throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, "Policy object is null in create policy api", false);
		}
		RangerService service = serviceREST.getServiceByName(vXPolicy.getRepositoryName());
		RangerPolicy  policy  = serviceUtil.toRangerPolicy(vXPolicy,service);

		VXPolicy ret = null;
		if(policy != null) {
			if(logger.isDebugEnabled()) {
				logger.debug("RANGERPOLICY: " + policy.toString());
			}
		
			RangerPolicy  createdPolicy = serviceREST.createPolicy(policy,null);

			ret = serviceUtil.toVXPolicy(createdPolicy, service);
		}
		
		if(logger.isDebugEnabled()) {
			logger.debug("<== PublicAPIs.createPolicy(" + policy + "): " + ret);
		}
	
		return ret;
	}

	@PUT
	@Path("/api/policy/{id}")
	@Produces({ "application/json", "application/xml" })
	public VXPolicy updatePolicy(VXPolicy vXPolicy, @PathParam("id") Long id) {
		
		if(logger.isDebugEnabled()) {
			logger.debug("==> PublicAPIs.updatePolicy(): "  + vXPolicy );
		}

		if(vXPolicy == null) {
			throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, "Policy object is null in update policy api", false);
		}

		XXPolicy existing = daoMgr.getXXPolicy().getById(id);
		if(existing == null) {
			throw restErrorUtil.createRESTException("Policy not found for Id: " + id, MessageEnums.DATA_NOT_FOUND);
		}

		vXPolicy.setId(id);
		
		RangerService service = serviceREST.getServiceByName(vXPolicy.getRepositoryName());
		RangerPolicy  policy  = serviceUtil.toRangerPolicy(vXPolicy,service);

		VXPolicy ret = null;
		if(policy != null) {
			policy.setVersion(existing.getVersion());

			RangerPolicy  updatedPolicy = serviceREST.updatePolicy(policy);

			ret = serviceUtil.toVXPolicy(updatedPolicy, service);
		}

		if(logger.isDebugEnabled()) {
			logger.debug("<== PublicAPIs.updatePolicy(" + policy + "): " + ret);
		}
	
		return ret;
	}

	@DELETE
	@Path("/api/policy/{id}")
	@PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
	@RangerAnnotationClassName(class_name = VXResource.class)
	public void deletePolicy(@PathParam("id") Long id,
			@Context HttpServletRequest request) {
		
		if(logger.isDebugEnabled()) {
			logger.debug("==> PublicAPIs.deletePolicy(): "  + id );
		}
		
		serviceREST.deletePolicy(id);
		
		if(logger.isDebugEnabled()) {
			logger.debug("<== PublicAPIs.deletePolicy(): "  + id );
		}
	}

	@GET
	@Path("/api/policy")
	@Produces({ "application/json", "application/xml" })
	public VXPolicyList searchPolicies(@Context HttpServletRequest request) {
		
		if(logger.isDebugEnabled()) {
			logger.debug("==> PublicAPIs.searchPolicies(): ");
		}

		SearchFilter filter = searchUtil.getSearchFilterFromLegacyRequest(request, policyService.sortFields);
		// get all policies from the store; pick the page to return after applying filter
		int savedStartIndex = filter.getStartIndex();
		int savedMaxRows    = filter.getMaxRows();

		filter.setStartIndex(0);
		filter.setMaxRows(Integer.MAX_VALUE);

		List<RangerPolicy> rangerPolicyList = serviceREST.getPolicies(filter);

		filter.setStartIndex(savedStartIndex);
		filter.setMaxRows(savedMaxRows);

		VXPolicyList vXPolicyList = null;
		if (rangerPolicyList != null) {
			vXPolicyList = serviceUtil.rangerPolicyListToPublic(rangerPolicyList,filter);
		}
		if(logger.isDebugEnabled()) {
			logger.debug("<== PublicAPIs.searchPolicies(): "  + vXPolicyList );
		}
		return vXPolicyList;
	}

	@GET
	@Path("/api/policy/count")
	@Produces({ "application/xml", "application/json" })
	public VXLong countPolicies(@Context HttpServletRequest request) {
		
		if(logger.isDebugEnabled()) {
			logger.debug("==> PublicAPIs.countPolicies(): ");
		}

		VXPolicyList policies = searchPolicies(request);

		VXLong vXlong = new VXLong();
		vXlong.setValue(policies == null ? 0 : policies.getResultSize());

		if(logger.isDebugEnabled()) {
			logger.debug("<== PublicAPIs.countPolicies(): "  + request );
		}

		return vXlong;
	}

}
