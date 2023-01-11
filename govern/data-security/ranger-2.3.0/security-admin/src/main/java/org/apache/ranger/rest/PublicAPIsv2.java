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

import org.apache.commons.lang.StringUtils;
import org.apache.ranger.admin.client.datatype.RESTResponse;
import org.apache.ranger.biz.SecurityZoneDBStore;
import org.apache.ranger.common.RESTErrorUtil;
import org.apache.ranger.common.annotation.RangerAnnotationJSMgrName;
import org.apache.ranger.plugin.model.RangerPluginInfo;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerRole;
import org.apache.ranger.plugin.model.RangerSecurityZone;
import org.apache.ranger.plugin.model.RangerSecurityZoneHeaderInfo;
import org.apache.ranger.plugin.model.RangerService;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.model.RangerServiceHeaderInfo;
import org.apache.ranger.plugin.model.RangerServiceTags;
import org.apache.ranger.plugin.util.GrantRevokeRoleRequest;
import org.apache.ranger.plugin.util.SearchFilter;
import org.apache.ranger.plugin.util.ServiceTags;
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
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;

import java.util.ArrayList;
import java.util.List;

@Path("public/v2")
@Component
@Scope("request")
@RangerAnnotationJSMgrName("PublicMgr")
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class PublicAPIsv2 {
	private static final Logger logger = LoggerFactory.getLogger(PublicAPIsv2.class);

	@Autowired
	ServiceREST serviceREST;

	@Autowired
	TagREST tagREST;

	@Autowired
	SecurityZoneREST securityZoneRest;

	@Autowired
	RoleREST roleREST;

	@Autowired
	RESTErrorUtil restErrorUtil;

    @Autowired
    SecurityZoneDBStore securityZoneStore;

	/*
	 * SecurityZone Creation API
	 */
	@POST
	@Path("/api/zones")
	public RangerSecurityZone createSecurityZone(RangerSecurityZone securityZone) {
		return securityZoneRest.createSecurityZone(securityZone);
	}

	/*
	 * SecurityZone Manipulation API
	 */
	@PUT
	@Path("/api/zones/{id}")
	public RangerSecurityZone updateSecurityZone(@PathParam("id") Long zoneId, RangerSecurityZone securityZone) {
		return securityZoneRest.updateSecurityZone(zoneId, securityZone);
	}

	@DELETE
	@Path("/api/zones/name/{name}")
	public void deleteSecurityZone(@PathParam("name") String zoneName) {
		securityZoneRest.deleteSecurityZone(zoneName);
	}

	 @DELETE
	 @Path("/api/zones/{id}")
	 public void deleteSecurityZone(@PathParam("id") Long zoneId) {
		 securityZoneRest.deleteSecurityZone(zoneId);
	 }

	/*
	 *  API's to Access SecurityZones
	 */
	@GET
	@Path("/api/zones/name/{name}")
	public RangerSecurityZone getSecurityZone(@PathParam("name") String zoneName) {
		return securityZoneRest.getSecurityZone(zoneName);
	}

	@GET
	@Path("/api/zones/{id}")
	public RangerSecurityZone getSecurityZone(@PathParam("id") Long id) {
		return securityZoneRest.getSecurityZone(id);
	}

	@GET
    @Path("/api/zones")
    public List<RangerSecurityZone> getAllZones(@Context HttpServletRequest request){
		return securityZoneRest.getAllZones(request).getSecurityZones();
	}

    /**
     * Get {@link List} of security zone header info.
     * This API is authorized to every authenticated user.
     * @return {@link List} of {@link RangerSecurityZoneHeaderInfo} if present.
     */
    @GET
    @Path("/api/zone-headers")
    public List<RangerSecurityZoneHeaderInfo> getSecurityZoneHeaderInfoList() {
        if (logger.isDebugEnabled()) {
            logger.debug("==> PublicAPIsv2.getSecurityZoneHeaderInfoList()");
        }

        List<RangerSecurityZoneHeaderInfo> ret;
        try {
            ret = securityZoneStore.getSecurityZoneHeaderInfoList();
        } catch (WebApplicationException excp) {
            throw excp;
        } catch (Throwable excp) {
            logger.error("PublicAPIsv2.getSecurityZoneHeaderInfoList() failed", excp);
            throw restErrorUtil.createRESTException(excp.getMessage());
        }

        if (logger.isDebugEnabled()) {
            logger.debug("<== PublicAPIsv2.getSecurityZoneHeaderInfoList():" + ret);
        }
        return ret;
    }

    /**
     * Get service header info {@link List} for given zone.
     * This API is authorized to every authenticated user.
     * @param zoneId
     * @return {@link List} of {@link RangerServiceHeaderInfo} for given zone if present.
     */
    @GET
    @Path("/api/zones/{zoneId}/service-headers")
    public List<RangerServiceHeaderInfo> getServiceHeaderInfoListByZoneId(@PathParam("zoneId") Long zoneId) {
        if (logger.isDebugEnabled()) {
            logger.debug("==> PublicAPIsv2.getServiceHeaderInfoListByZoneId({})" + zoneId);
        }

        List<RangerServiceHeaderInfo> ret;
        try {
            ret = securityZoneStore.getServiceHeaderInfoListByZoneId(zoneId);
        } catch (WebApplicationException excp) {
            throw excp;
        } catch (Throwable excp) {
            logger.error("PublicAPIsv2.getServiceHeaderInfoListByZoneId() failed", excp);
            throw restErrorUtil.createRESTException(excp.getMessage());
        }

        if (logger.isDebugEnabled()) {
            logger.debug("<== PublicAPIsv2.getServiceHeaderInfoListByZoneId():" + ret);
        }
        return ret;
    }

	/*
	* ServiceDef Manipulation APIs
	 */

	@GET
	@Path("/api/servicedef/{id}")
	@Produces({ "application/json", "application/xml" })
	public RangerServiceDef getServiceDef(@PathParam("id") Long id) {
		return serviceREST.getServiceDef(id);
	}

	@GET
	@Path("/api/servicedef/name/{name}")
	@Produces({ "application/json", "application/xml" })
	public RangerServiceDef getServiceDefByName(@PathParam("name") String name) {
		return serviceREST.getServiceDefByName(name);
	}

	@GET
	@Path("/api/servicedef/")
	@Produces({ "application/json", "application/xml" })
	public List<RangerServiceDef> searchServiceDefs(@Context HttpServletRequest request) {
		return serviceREST.getServiceDefs(request).getServiceDefs();
	}

	@POST
	@Path("/api/servicedef/")
	@PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
	@Produces({ "application/json", "application/xml" })
	public RangerServiceDef createServiceDef(RangerServiceDef serviceDef) {
		return serviceREST.createServiceDef(serviceDef);
	}

	@PUT
	@Path("/api/servicedef/{id}")
	@PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
	@Produces({ "application/json", "application/xml" })
	public RangerServiceDef updateServiceDef(RangerServiceDef serviceDef, @PathParam("id") Long id) {
		// if serviceDef.id is specified, it should be same as param 'id'
		if(serviceDef.getId() == null) {
			serviceDef.setId(id);
		} else if(!serviceDef.getId().equals(id)) {
			throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST , "serviceDef id mismatch", true);
		}

		return serviceREST.updateServiceDef(serviceDef);
	}


	@PUT
	@Path("/api/servicedef/name/{name}")
	@PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
	@Produces({ "application/json", "application/xml" })
	public RangerServiceDef updateServiceDefByName(RangerServiceDef serviceDef,
	                                     @PathParam("name") String name) {
		// serviceDef.name is immutable
		// if serviceDef.name is specified, it should be same as the param 'name'
		if(serviceDef.getName() == null) {
			serviceDef.setName(name);
		} else if(!serviceDef.getName().equals(name)) {
			throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST , "serviceDef name mismatch", true);
		}

		// ignore serviceDef.id - if specified. Retrieve using the given name and use id from the retrieved object
		RangerServiceDef existingServiceDef = getServiceDefByName(name);
		serviceDef.setId(existingServiceDef.getId());
		if(StringUtils.isEmpty(serviceDef.getGuid())) {
			serviceDef.setGuid(existingServiceDef.getGuid());
		}

		return serviceREST.updateServiceDef(serviceDef);
	}

	/*
	* Should add this back when guid is used for search and delete operations as well
	@PUT
	@Path("/api/servicedef/guid/{guid}")
	@PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
	@Produces({ "application/json", "application/xml" })
	public RangerServiceDef updateServiceDefByGuid(RangerServiceDef serviceDef,
	                                               @PathParam("guid") String guid) {
		// ignore serviceDef.id - if specified. Retrieve using the given guid and use id from the retrieved object
		RangerServiceDef existingServiceDef = getServiceDefByGuid(guid);
		serviceDef.setId(existingServiceDef.getId());
		if(StringUtils.isEmpty(serviceDef.getGuid())) {
			serviceDef.setGuid(existingServiceDef.getGuid());
		}

		return serviceREST.updateServiceDef(serviceDef);
	}
	*/


	@DELETE
	@Path("/api/servicedef/{id}")
	@PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
	public void deleteServiceDef(@PathParam("id") Long id, @Context HttpServletRequest request) {
		serviceREST.deleteServiceDef(id, request);
	}

	@DELETE
	@Path("/api/servicedef/name/{name}")
	@PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
	public void deleteServiceDefByName(@PathParam("name") String name, @Context HttpServletRequest request) {
		RangerServiceDef serviceDef = serviceREST.getServiceDefByName(name);
		serviceREST.deleteServiceDef(serviceDef.getId(), request);
	}

	/*
	* Service Manipulation APIs
	 */

	@GET
	@Path("/api/service/{id}")
	@Produces({ "application/json", "application/xml" })
	@PreAuthorize("@rangerPreAuthSecurityHandler.isAPISpnegoAccessible()")
	public RangerService getService(@PathParam("id") Long id) {
		return serviceREST.getService(id);
	}

	@GET
	@Path("/api/service/name/{name}")
	@Produces({ "application/json", "application/xml" })
	@PreAuthorize("@rangerPreAuthSecurityHandler.isAPISpnegoAccessible()")
	public RangerService getServiceByName(@PathParam("name") String name) {
		return serviceREST.getServiceByName(name);
	}

	@GET
	@Path("/api/service/")
	@Produces({ "application/json", "application/xml" })
	@PreAuthorize("@rangerPreAuthSecurityHandler.isAPISpnegoAccessible()")
	public List<RangerService> searchServices(@Context HttpServletRequest request) {
		return serviceREST.getServices(request).getServices();
	}

	@POST
	@Path("/api/service/")
	@PreAuthorize("@rangerPreAuthSecurityHandler.isAPISpnegoAccessible()")
	@Produces({ "application/json", "application/xml" })
	public RangerService createService(RangerService service) {
		return serviceREST.createService(service);
	}

	@PUT
	@Path("/api/service/{id}")
	@PreAuthorize("@rangerPreAuthSecurityHandler.isAPISpnegoAccessible()")
	@Produces({ "application/json", "application/xml" })
	public RangerService updateService(RangerService service, @PathParam("id") Long id,
                                       @Context HttpServletRequest request) {
		// if service.id is specified, it should be same as the param 'id'
		if(service.getId() == null) {
			service.setId(id);
		} else if(!service.getId().equals(id)) {
			throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST , "service id mismatch", true);
		}

		return serviceREST.updateService(service, request);
	}


	@PUT
	@Path("/api/service/name/{name}")
	@PreAuthorize("@rangerPreAuthSecurityHandler.isAPISpnegoAccessible()")
	@Produces({ "application/json", "application/xml" })
	public RangerService updateServiceByName(RangerService service,
                                             @PathParam("name") String name,
                                             @Context HttpServletRequest request) {
		// ignore service.id - if specified. Retrieve using the given name and use id from the retrieved object
		RangerService existingService = getServiceByName(name);
		service.setId(existingService.getId());
		if(StringUtils.isEmpty(service.getGuid())) {
			service.setGuid(existingService.getGuid());
		}
		if (StringUtils.isEmpty(service.getName())) {
			service.setName(existingService.getName());
		}

		return serviceREST.updateService(service, request);
	}

	/*
	 * Should add this back when guid is used for search and delete operations as well
	@PUT
	@Path("/api/service/guid/{guid}")
	@PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
	@Produces({ "application/json", "application/xml" })
	public RangerService updateServiceByGuid(RangerService service,
	                                               @PathParam("guid") String guid) {
		// ignore service.id - if specified. Retrieve using the given guid and use id from the retrieved object
		RangerService existingService = getServiceByGuid(guid);
		service.setId(existingService.getId());
		if(StringUtils.isEmpty(service.getGuid())) {
			service.setGuid(existingService.getGuid());
		}

		return serviceREST.updateService(service);
	}
	*/

	@DELETE
	@Path("/api/service/{id}")
	@PreAuthorize("@rangerPreAuthSecurityHandler.isAPISpnegoAccessible()")
	public void deleteService(@PathParam("id") Long id) {
		serviceREST.deleteService(id);
	}

	@DELETE
	@Path("/api/service/name/{name}")
	@PreAuthorize("@rangerPreAuthSecurityHandler.isAPISpnegoAccessible()")
	public void deleteServiceByName(@PathParam("name") String name) {
		RangerService service = serviceREST.getServiceByName(name);
		serviceREST.deleteService(service.getId());
	}

	/*
	* Policy Manipulation APIs
	 */

	@GET
	@Path("/api/policy/{id}")
	@Produces({ "application/json", "application/xml" })
	public RangerPolicy getPolicy(@PathParam("id") Long id) {
		return serviceREST.getPolicy(id);
	}

	@GET
	@Path("/api/policy/")
	@Produces({ "application/json", "application/xml" })
	public List<RangerPolicy> getPolicies(@Context HttpServletRequest request) {

		List<RangerPolicy> ret  = new ArrayList<RangerPolicy>();

		if(logger.isDebugEnabled()) {
			logger.debug("==> PublicAPIsv2.getPolicies()");
		}

		ret = serviceREST.getPolicies(request).getPolicies();

		if(logger.isDebugEnabled()) {
			logger.debug("<== PublicAPIsv2.getPolicies(Request: " + request.getQueryString() + " Result Size: "  + ret.size() );
		}

		return ret;
	}

	@GET
	@Path("/api/service/{servicename}/policy/{policyname}")
	@Produces({ "application/json", "application/xml" })
	public RangerPolicy getPolicyByName(@PathParam("servicename") String serviceName,
	                                    @PathParam("policyname") String policyName,
	                                    @Context HttpServletRequest request) {
		if(logger.isDebugEnabled()) {
			logger.debug("==> PublicAPIsv2.getPolicyByName(" + serviceName + "," + policyName + ")");
		}

		SearchFilter filter = new SearchFilter();
		filter.setParam(SearchFilter.SERVICE_NAME, serviceName);
		filter.setParam(SearchFilter.POLICY_NAME, policyName);
		List<RangerPolicy> policies = serviceREST.getPolicies(filter);

		if (policies.size() != 1) {
			throw restErrorUtil.createRESTException(HttpServletResponse.SC_NOT_FOUND, "Not found", true);
		}
		RangerPolicy policy = policies.get(0);

		if(logger.isDebugEnabled()) {
			logger.debug("<== PublicAPIsv2.getPolicyByName(" + serviceName + "," + policyName + ")" + policy);
		}
		return policy;
	}

	@GET
	@Path("/api/service/{servicename}/policy/")
	@Produces({ "application/json", "application/xml" })
	public List<RangerPolicy> searchPolicies(@PathParam("servicename") String serviceName,
	                                         @Context HttpServletRequest request) {
		return serviceREST.getServicePoliciesByName(serviceName, request).getPolicies();
	}

	@GET
	@Path("/api/policies/{serviceDefName}/for-resource/")
	@Produces({ "application/json", "application/xml" })
	public List<RangerPolicy> getPoliciesForResource(@PathParam("serviceDefName") String serviceDefName,
													 @DefaultValue("") @QueryParam("serviceName") String serviceName,
													 @Context HttpServletRequest request) {
		return serviceREST.getPoliciesForResource(serviceDefName, serviceName, request);
	}

	@GET
	@Path("/api/policy/guid/{guid}")
	@Produces({ "application/json", "application/xml" })
	public RangerPolicy getPolicyByGUIDAndServiceNameAndZoneName(@PathParam("guid") String guid,
																 @DefaultValue("") @QueryParam("serviceName") String serviceName,
																 @DefaultValue("") @QueryParam("ZoneName") String zoneName) {
		return serviceREST.getPolicyByGUIDAndServiceNameAndZoneName(guid, serviceName, zoneName);
	}

	@POST
	@Path("/api/policy/")
	@Produces({ "application/json", "application/xml" })
	public RangerPolicy createPolicy(RangerPolicy policy , @Context HttpServletRequest request) {
		return serviceREST.createPolicy(policy, request);
	}

	@POST
	@Path("/api/policy/apply/")
	@Produces({ "application/json", "application/xml" })
	public RangerPolicy applyPolicy(RangerPolicy policy, @Context HttpServletRequest request) { // new API
		return serviceREST.applyPolicy(policy, request);
	}

	@PUT
	@Path("/api/policy/{id}")
	@Produces({ "application/json", "application/xml" })
	public RangerPolicy updatePolicy(RangerPolicy policy, @PathParam("id") Long id) {
		// if policy.id is specified, it should be same as the param 'id'
		if(policy.getId() == null) {
			policy.setId(id);
		} else if(!policy.getId().equals(id)) {
			throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST , "policyID mismatch", true);
		}

		return serviceREST.updatePolicy(policy);
	}

	@PUT
	@Path("/api/service/{servicename}/policy/{policyname}")
	@Produces({ "application/json", "application/xml" })
	public RangerPolicy updatePolicyByName(RangerPolicy policy,
	                                               @PathParam("servicename") String serviceName,
	                                               @PathParam("policyname") String policyName,
	                                               @Context HttpServletRequest request) {
		if (policy.getService() == null || !policy.getService().equals(serviceName)) {
			throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST , "service name mismatch", true);
		}
		RangerPolicy oldPolicy = getPolicyByName(serviceName, policyName, request);

		// ignore policy.id - if specified. Retrieve using the given serviceName+policyName and use id from the retrieved object
		policy.setId(oldPolicy.getId());
		if(StringUtils.isEmpty(policy.getGuid())) {
			policy.setGuid(oldPolicy.getGuid());
		}
		if(StringUtils.isEmpty(policy.getName())) {
			policy.setName(StringUtils.trim(oldPolicy.getName()));
		}

		return serviceREST.updatePolicy(policy);
	}


	/* Should add this back when guid is used for search and delete operations as well
	@PUT
	@Path("/api/policy/guid/{guid}")
	@Produces({ "application/json", "application/xml" })
	public RangerPolicy updatePolicyByGuid(RangerPolicy policy,
	                                               @PathParam("guid") String guid) {
		// ignore policy.guid - if specified. Retrieve using the given guid and use id from the retrieved object
		RangerPolicy existingPolicy = getPolicyByGuid(name);
		policy.setId(existingPolicy.getId());
		if(StringUtils.isEmpty(policy.getGuid())) {
			policy.setGuid(existingPolicy.getGuid());
		}

		return serviceREST.updatePolicy(policy);
	}
	*/


	@DELETE
	@Path("/api/policy/{id}")
	public void deletePolicy(@PathParam("id") Long id) {
		serviceREST.deletePolicy(id);
	}

	@DELETE
	@Path("/api/policy")
	public void deletePolicyByName(@QueryParam("servicename") String serviceName,
	                               @QueryParam("policyname") String policyName,
	                               @Context HttpServletRequest request) {
		if(logger.isDebugEnabled()) {
			logger.debug("==> PublicAPIsv2.deletePolicyByName(" + serviceName + "," + policyName + ")");
		}

		if (serviceName == null || policyName == null) {
			throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST , "Invalid service name or policy name", true);
		}
		RangerPolicy policy = getPolicyByName(serviceName, policyName, request);
		serviceREST.deletePolicy(policy.getId());
		if(logger.isDebugEnabled()) {
			logger.debug("<== PublicAPIsv2.deletePolicyByName(" + serviceName + "," + policyName + ")");
		}
	}

	@DELETE
	@Path("/api/policy/guid/{guid}")
	@Produces({ "application/json", "application/xml" })
	public void deletePolicyByGUIDAndServiceNameAndZoneName(@PathParam("guid") String guid,
												 @DefaultValue("") @QueryParam("serviceName") String serviceName,
												 @DefaultValue("") @QueryParam("zoneName") String zoneName) {
		serviceREST.deletePolicyByGUIDAndServiceNameAndZoneName(guid, serviceName, zoneName);
	}

	@PUT
	@Path("/api/service/{serviceName}/tags")
	@Produces({ "application/json", "application/xml" })
	@PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
	public void importServiceTags(@PathParam("serviceName") String serviceName, RangerServiceTags svcTags) {
		if (logger.isDebugEnabled()) {
			logger.debug("==> PublicAPIsv2.importServiceTags()");
		}

		ServiceTags serviceTags = RangerServiceTags.toServiceTags(svcTags);

		// overwrite serviceName with the one given in url
		serviceTags.setServiceName(serviceName);

		tagREST.importServiceTags(serviceTags);

		if (logger.isDebugEnabled()) {
			logger.debug("<== PublicAPIsv2.importServiceTags()");
		}
	}

	@GET
	@Path("/api/service/{serviceName}/tags")
	@Produces({ "application/json", "application/xml" })
	@PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
	public RangerServiceTags getServiceTags(@PathParam("serviceName") String serviceName, @Context HttpServletRequest request) {
		if (logger.isDebugEnabled()) {
			logger.debug("==> PublicAPIsv2.getServiceTags()");
		}

		Long              lastKnownVersion   = -1L;
		Long              lastActivationTime = 0L;
		String            pluginId           = null;
		Boolean           supportsTagDeltas  = false;
		String            pluginCapabilities = "";
		ServiceTags       tags               = tagREST.getServiceTagsIfUpdated(serviceName, lastKnownVersion, lastActivationTime, pluginId, supportsTagDeltas, pluginCapabilities, request);
		RangerServiceTags ret                = RangerServiceTags.toRangerServiceTags(tags);

		if (logger.isDebugEnabled()) {
			logger.debug("<== PublicAPIsv2.getServiceTags()");
		}

		return ret;
	}


	@GET
	@Path("/api/plugins/info")
	public List<RangerPluginInfo> getPluginsInfo(@Context HttpServletRequest request) {
		if (logger.isDebugEnabled()) {
			logger.debug("==> PublicAPIsv2.getPluginsInfo()");
		}

		List<RangerPluginInfo> ret = serviceREST.getPluginsInfo(request).getPluginInfoList();

		if (logger.isDebugEnabled()) {
			logger.debug("<== PublicAPIsv2.getPluginsInfo()");
		}
		return ret;
	}

	@DELETE
	@Path("/api/server/policydeltas")
	@PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
	public void deletePolicyDeltas(@DefaultValue("7") @QueryParam("days") Integer olderThan, @Context HttpServletRequest request) {
		if (logger.isDebugEnabled()) {
			logger.debug("==> PublicAPIsv2.deletePolicyDeltas(" + olderThan + ")");
		}

		serviceREST.deletePolicyDeltas(olderThan, request);

		if (logger.isDebugEnabled()) {
			logger.debug("<== PublicAPIsv2.deletePolicyDeltas(" + olderThan + ")");
		}
	}

	@DELETE
	@Path("/api/server/tagdeltas")
	@PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
	public void deleteTagDeltas(@DefaultValue("7") @QueryParam("days") Integer olderThan, @Context HttpServletRequest request) {
		if (logger.isDebugEnabled()) {
			logger.debug("==> PublicAPIsv2.deleteTagDeltas(" + olderThan + ")");
		}

		tagREST.deleteTagDeltas(olderThan, request);

		if (logger.isDebugEnabled()) {
			logger.debug("<== PublicAPIsv2.deleteTagDeltas(" + olderThan + ")");
		}
	}

	@DELETE
	@Path("/api/server/purgepolicies/{serviceName}")
	@PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
	public void purgeEmptyPolicies(@PathParam("serviceName") String serviceName, @Context HttpServletRequest request) {
		if (logger.isDebugEnabled()) {
			logger.debug("==> PublicAPIsv2.purgeEmptyPolicies(" + serviceName + ")");
		}

		if (serviceName == null) {
			throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST , "Invalid service name", true);
		}

		serviceREST.purgeEmptyPolicies(serviceName, request);

		if (logger.isDebugEnabled()) {
			logger.debug("<== PublicAPIsv2.purgeEmptyPolicies(" + serviceName + ")");
		}
	}

	/*
	 * Role Creation API
	 */

	@POST
	@Path("/api/roles")
	@Produces({ "application/json", "application/xml" })
	public RangerRole createRole(@QueryParam("serviceName") String serviceName, RangerRole role
			, @DefaultValue("false") @QueryParam("createNonExistUserGroup") Boolean createNonExistUserGroup
			, @Context HttpServletRequest request) {
		logger.info("==> PublicAPIsv2.createRole");
		RangerRole ret;
		ret = roleREST.createRole(serviceName, role, createNonExistUserGroup);
		logger.info("<== PublicAPIsv2.createRole" + ret.getName());
		return ret;
	}

	/*
	 * Role Manipulation API
	 */
	@PUT
	@Path("/api/roles/{id}")
	@Produces({ "application/json", "application/xml" })
	public RangerRole updateRole(@PathParam("id") Long roleId, RangerRole role
			, @DefaultValue("false") @QueryParam("createNonExistUserGroup") Boolean createNonExistUserGroup
			, @Context HttpServletRequest request) {
		return roleREST.updateRole(roleId, role, createNonExistUserGroup);
	}

	@DELETE
	@Path("/api/roles/name/{name}")
	public void deleteRole(@QueryParam("serviceName") String serviceName, @QueryParam("execUser") String userName, @PathParam("name") String roleName, @Context HttpServletRequest request) {
		roleREST.deleteRole(serviceName, userName, roleName);
	}

	@DELETE
	@Path("/api/roles/{id}")
	public void deleteRole(@PathParam("id") Long roleId, @Context HttpServletRequest request) {
		roleREST.deleteRole(roleId);
	}

	/*
	 *  APIs to Access Roles
	 */
	@GET
	@Path("/api/roles/name/{name}")
	@Produces({ "application/json", "application/xml" })
	public RangerRole getRole(@QueryParam("serviceName") String serviceName, @QueryParam("execUser") String userName, @PathParam("name") String roleName, @Context HttpServletRequest request) {
		return roleREST.getRole(serviceName, userName, roleName);
	}

	@GET
	@Path("/api/roles/{id}")
	@Produces({ "application/json", "application/xml" })
	public RangerRole getRole(@PathParam("id") Long id, @Context HttpServletRequest request) {
		return roleREST.getRole(id);
	}

	@GET
	@Path("/api/roles")
	@Produces({ "application/json", "application/xml" })
	public List<RangerRole> getAllRoles(@Context HttpServletRequest request) {
		return roleREST.getAllRoles(request).getSecurityRoles();
	}

	@GET
	@Path("/api/roles/names")
	@Produces({ "application/json", "application/xml" })
	public List<String> getAllRoleNames(@QueryParam("serviceName") String serviceName, @QueryParam("execUser") String userName, @Context HttpServletRequest request){
		return roleREST.getAllRoleNames(serviceName, userName, request);
	}

	@GET
	@Path("/api/roles/user/{user}")
	@Produces({ "application/json", "application/xml" })
	public List<String> getUserRoles(@PathParam("user") String userName, @Context HttpServletRequest request){
		return roleREST.getUserRoles(userName, request);
	}

	/*
    	This API is used to add users and groups with/without GRANT privileges to this Role. It follows add-or-update semantics
 	 */
	@PUT
	@Path("/api/roles/{id}/addUsersAndGroups")
	public RangerRole addUsersAndGroups(@PathParam("id") Long roleId, List<String> users, List<String> groups, Boolean isAdmin, @Context HttpServletRequest request) {
		return roleREST.addUsersAndGroups(roleId, users, groups, isAdmin);
	}

	/*
        This API is used to remove users and groups, without regard to their GRANT privilege, from this Role.
     */
	@PUT
	@Path("/api/roles/{id}/removeUsersAndGroups")
	public RangerRole removeUsersAndGroups(@PathParam("id") Long roleId, List<String> users, List<String> groups, @Context HttpServletRequest request) {
		return roleREST.removeUsersAndGroups(roleId, users, groups);
	}

	/*
        This API is used to remove GRANT privilege from listed users and groups.
     */
	@PUT
	@Path("/api/roles/{id}/removeAdminFromUsersAndGroups")
	public RangerRole removeAdminFromUsersAndGroups(@PathParam("id") Long roleId, List<String> users, List<String> groups, @Context HttpServletRequest request) {
		return roleREST.removeAdminFromUsersAndGroups(roleId, users, groups);
	}

	/*
    	This API is used to add users and roles with/without GRANT privileges to this Role. It follows add-or-update semantics
 	 */
	@PUT
	@Path("/api/roles/grant/{serviceName}")
	@Consumes({ "application/json", "application/xml" })
	@Produces({ "application/json", "application/xml" })
	public RESTResponse grantRole(@PathParam("serviceName") String serviceName, GrantRevokeRoleRequest grantRoleRequest, @Context HttpServletRequest request) {
		if(logger.isDebugEnabled()) {
			logger.debug("==> PublicAPIsv2.grantRoleUsersAndRoles(" + grantRoleRequest.toString() + ")");
		}
		return roleREST.grantRole(serviceName, grantRoleRequest, request);
	}

	/*
        This API is used to remove users and groups, without regard to their GRANT privilege, from this Role.
     */
	@PUT
	@Path("/api/roles/revoke/{serviceName}")
	@Consumes({ "application/json", "application/xml" })
	@Produces({ "application/json", "application/xml" })
	public RESTResponse revokeRoleUsersAndRoles(@PathParam("serviceName") String serviceName, GrantRevokeRoleRequest revokeRoleRequest, @Context HttpServletRequest request) {
		return roleREST.revokeRole(serviceName, revokeRoleRequest, request);
	}
}
