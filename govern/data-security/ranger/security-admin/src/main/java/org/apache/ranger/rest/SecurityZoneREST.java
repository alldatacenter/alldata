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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;

import org.apache.ranger.biz.RangerBizUtil;
import org.apache.ranger.biz.SecurityZoneDBStore;
import org.apache.ranger.biz.ServiceDBStore;
import org.apache.ranger.biz.ServiceMgr;
import org.apache.ranger.common.MessageEnums;
import org.apache.ranger.common.RESTErrorUtil;
import org.apache.ranger.common.RangerConstants;
import org.apache.ranger.common.RangerSearchUtil;
import org.apache.ranger.common.RangerValidatorFactory;
import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.entity.XXService;
import org.apache.ranger.entity.XXServiceDef;
import org.apache.ranger.plugin.model.RangerSecurityZone;
import org.apache.ranger.plugin.model.validation.RangerSecurityZoneValidator;
import org.apache.ranger.plugin.model.validation.RangerValidator;
import org.apache.ranger.plugin.util.SearchFilter;
import org.apache.ranger.service.RangerSecurityZoneServiceService;
import org.apache.ranger.plugin.model.RangerSecurityZone.RangerSecurityZoneService;
import org.apache.ranger.view.RangerSecurityZoneList;
import org.apache.ranger.plugin.store.EmbeddedServiceDefsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Sets;

@Path("zones")
@Component
@Scope("request")
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class SecurityZoneREST {
    private static final Logger LOG = LoggerFactory.getLogger(SecurityZoneREST.class);
    private static final String STR_USER_NOT_AUTHORIZED_TO_ACCESS_ZONE = "User is not authorized to access zone(s).";

    @Autowired
    RESTErrorUtil restErrorUtil;

    @Autowired
    SecurityZoneDBStore securityZoneStore;

    @Autowired
    RangerSecurityZoneServiceService securityZoneService;

    @Autowired
    ServiceDBStore svcStore;

    @Autowired
	RangerSearchUtil searchUtil;

    @Autowired
    RangerValidatorFactory validatorFactory;
    
    @Autowired
    RangerBizUtil bizUtil;
    
	@Autowired
	ServiceREST serviceRest;

	@Autowired
	RangerDaoManager daoManager;

	@Autowired
	ServiceMgr serviceMgr;


    @POST
    @Path("/zones")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    public RangerSecurityZone createSecurityZone(RangerSecurityZone securityZone) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> createSecurityZone("+ securityZone + ")");
        }

        RangerSecurityZone ret;
        try {
            ensureAdminAccess(securityZone);
            removeEmptyEntries(securityZone);
            RangerSecurityZoneValidator validator = validatorFactory.getSecurityZoneValidator(svcStore, securityZoneStore);
            validator.validate(securityZone, RangerValidator.Action.CREATE);
            ret = securityZoneStore.createSecurityZone(securityZone);
        } catch(WebApplicationException excp) {
            throw excp;
        } catch(Throwable excp) {
            LOG.error("createSecurityZone(" + securityZone + ") failed", excp);

            throw restErrorUtil.createRESTException(excp.getMessage());
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("<== createSecurityZone("+ securityZone + "):" +  ret);
        }
        return ret;
    }

    @PUT
    @Path("/zones/{id}")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    public RangerSecurityZone updateSecurityZone(@PathParam("id") Long zoneId,
                                                 RangerSecurityZone securityZone) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> updateSecurityZone(id=" + zoneId +", " + securityZone + ")");
        }

        if (zoneId != null && zoneId.equals(RangerSecurityZone.RANGER_UNZONED_SECURITY_ZONE_ID)) {
            throw restErrorUtil.createRESTException("Cannot update unzoned zone");
        }

        ensureUserAllowOperationOnServiceForZone(securityZone);
        removeEmptyEntries(securityZone);
        if (securityZone.getId() != null && !zoneId.equals(securityZone.getId())) {
            throw restErrorUtil.createRESTException("zoneId mismatch!!");
        } else {
            securityZone.setId(zoneId);
        }
        RangerSecurityZone ret;
        try {
            RangerSecurityZoneValidator validator = validatorFactory.getSecurityZoneValidator(svcStore, securityZoneStore);
            validator.validate(securityZone, RangerValidator.Action.UPDATE);
            ret = securityZoneStore.updateSecurityZoneById(securityZone);
        } catch(WebApplicationException excp) {
            throw excp;
        } catch(Throwable excp) {
            LOG.error("updateSecurityZone(" + securityZone + ") failed", excp);

            throw restErrorUtil.createRESTException(excp.getMessage());
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("<== updateSecurityZone(id=" + zoneId +", " + securityZone + "):" + ret);
        }
        return ret;
    }

    @DELETE
    @Path("/zones/name/{name}")
    public void deleteSecurityZone(@PathParam("name") String zoneName) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> deleteSecurityZone(name=" + zoneName + ")");
        }
        try {
        	ensureAdminAccess();
            RangerSecurityZoneValidator validator = validatorFactory.getSecurityZoneValidator(svcStore, securityZoneStore);
            validator.validate(zoneName, RangerValidator.Action.DELETE);
            securityZoneStore.deleteSecurityZoneByName(zoneName);
        } catch(WebApplicationException excp) {
            throw excp;
        } catch(Throwable excp) {
            LOG.error("deleteSecurityZone(" + zoneName + ") failed", excp);

            throw restErrorUtil.createRESTException(excp.getMessage());
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("<== deleteSecurityZone(name=" + zoneName + ")");
        }
    }

    @DELETE
    @Path("/zones/{id}")
    public void deleteSecurityZone(@PathParam("id") Long zoneId) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> deleteSecurityZone(id=" + zoneId + ")");
        }
        if (zoneId != null && zoneId.equals(RangerSecurityZone.RANGER_UNZONED_SECURITY_ZONE_ID)) {
            throw restErrorUtil.createRESTException("Cannot delete unzoned zone");
        }
        try {
        	ensureAdminAccess();
            RangerSecurityZoneValidator validator = validatorFactory.getSecurityZoneValidator(svcStore, securityZoneStore);
            validator.validate(zoneId, RangerValidator.Action.DELETE);
            securityZoneStore.deleteSecurityZoneById(zoneId);
        } catch(WebApplicationException excp) {
            throw excp;
        } catch(Throwable excp) {
            LOG.error("deleteSecurityZone(" + zoneId + ") failed", excp);

            throw restErrorUtil.createRESTException(excp.getMessage());
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("<== deleteSecurityZone(id=" + zoneId + ")");
        }
    }

    @GET
    @Path("/zones/name/{name}")
    @Produces({ "application/json" })
    public RangerSecurityZone getSecurityZone(@PathParam("name") String zoneName) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> getSecurityZone(name=" + zoneName + ")");
        }

        if (!bizUtil.hasModuleAccess(RangerConstants.MODULE_SECURITY_ZONE)) {
            throw restErrorUtil.createRESTException(STR_USER_NOT_AUTHORIZED_TO_ACCESS_ZONE, MessageEnums.OPER_NO_PERMISSION);
        }

        RangerSecurityZone ret;
        try {
            ret = securityZoneStore.getSecurityZoneByName(zoneName);
        } catch(WebApplicationException excp) {
            throw excp;
        } catch(Throwable excp) {
            LOG.error("getSecurityZone(" + zoneName + ") failed", excp);

            throw restErrorUtil.createRESTException(excp.getMessage());
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== getSecurityZone(name=" + zoneName + "):" + ret);
        }
        return ret;
    }

    @GET
    @Path("/zones/{id}")
    @Produces({ "application/json" })
    public RangerSecurityZone getSecurityZone(@PathParam("id") Long id) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> getSecurityZone(id=" + id + ")");
        }

        if (!bizUtil.hasModuleAccess(RangerConstants.MODULE_SECURITY_ZONE)) {
            throw restErrorUtil.createRESTException(STR_USER_NOT_AUTHORIZED_TO_ACCESS_ZONE, MessageEnums.OPER_NO_PERMISSION);
        }

        if (id != null && id.equals(RangerSecurityZone.RANGER_UNZONED_SECURITY_ZONE_ID)) {
            throw restErrorUtil.createRESTException("Cannot access unzoned zone");
        }

        RangerSecurityZone ret;
        try {
            ret = securityZoneStore.getSecurityZone(id);
        } catch(WebApplicationException excp) {
            throw excp;
        } catch(Throwable excp) {
            LOG.error("getSecurityZone(" + id + ") failed", excp);

            throw restErrorUtil.createRESTException(excp.getMessage());
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== getSecurityZone(id=" + id + "):" + ret);
        }
        return ret;
    }

    @GET
    @Path("/zones")
    @Produces({ "application/json" })
    public RangerSecurityZoneList getAllZones(@Context HttpServletRequest request) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> getAllZones()");
        }

        if (!bizUtil.hasModuleAccess(RangerConstants.MODULE_SECURITY_ZONE)) {
            throw restErrorUtil.createRESTException(STR_USER_NOT_AUTHORIZED_TO_ACCESS_ZONE, MessageEnums.OPER_NO_PERMISSION);
        }

        RangerSecurityZoneList   ret    = new RangerSecurityZoneList();
        SearchFilter             filter = searchUtil.getSearchFilter(request, securityZoneService.sortFields);
        try {
            List<RangerSecurityZone> securityZones = securityZoneStore.getSecurityZones(filter);
            ret.setSecurityZoneList(securityZones);
            if (securityZones != null) {
                ret.setTotalCount(securityZones.size());
                ret.setSortBy(filter.getSortBy());
                ret.setSortType(filter.getSortType());
                ret.setResultSize(securityZones.size());
            }
        } catch (WebApplicationException excp) {
            throw excp;
        } catch (Throwable excp) {
            LOG.error("getSecurityZones() failed", excp);

            throw restErrorUtil.createRESTException(excp.getMessage());
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== getAllZones():" + ret);
        }
        return ret;
    }

	private void ensureAdminAccess(){
		if(!bizUtil.isAdmin()){
			String userName = bizUtil.getCurrentUserLoginId();
			throw restErrorUtil.createRESTException(HttpServletResponse.SC_FORBIDDEN, "Ranger Security Zone is not accessible for user '" + userName + "'.", true);
		}
	}
	
	private void ensureUserAllowOperationOnServiceForZone(
			RangerSecurityZone securityZone){
		if (!bizUtil.isAdmin()) {
			String userName = bizUtil.getCurrentUserLoginId();
			RangerSecurityZone existingSecurityZone = null;
			try {
				existingSecurityZone = svcStore
						.getSecurityZone(securityZone.getId());
			} catch (Exception ex) {
				LOG.error("Unable to get Security Zone with id : " + securityZone.getId(), ex);
				throw restErrorUtil.createRESTException(ex.getMessage());
			}
			if (existingSecurityZone != null) {
				/* Validation for non service related fields of security zone */
				
				
				if (!Objects.equals(securityZone.getName(), existingSecurityZone.getName())) {
					throwRestError("User : " + userName
							+ " is not allowed to edit zone name of zone : " + existingSecurityZone.getName());
				} else if (!Objects.equals(securityZone.getDescription(), existingSecurityZone.getDescription())) {
					throwRestError("User : " + userName
							+ " is not allowed to edit zone description of zone : " + existingSecurityZone.getName());
				}
				if (!serviceMgr.isZoneAdmin(existingSecurityZone.getName())) {
					if (!Objects.equals(securityZone.getAdminUserGroups(), existingSecurityZone.getAdminUserGroups())) {
						throwRestError("User : "
								+ userName
								+ " is not allowed to edit zone Admin User Group of zone : " + existingSecurityZone.getName());
					} else if (!Objects.equals(securityZone.getAdminUsers(), existingSecurityZone.getAdminUsers())) {
						throwRestError("User : " + userName
								+ " is not allowed to edit zone Admin User of zone : " + existingSecurityZone.getName());
					} else if (!Objects.equals(securityZone.getAuditUsers(), existingSecurityZone.getAuditUsers())) {
						throwRestError("User : " + userName
								+ " is not allowed to edit zone Audit User of zone : " + existingSecurityZone.getName());
					} else if (!Objects.equals(securityZone.getAuditUserGroups(), existingSecurityZone.getAuditUserGroups())) {
						throwRestError("User : "
								+ userName
								+ " is not allowed to edit zone Audit User Group of zone : " + existingSecurityZone.getName());
					}
				}
				
				/*
				 * Validation on tag service association / disassociation with
				 * security zone
				 * */
				
				List<String> dbTagServices = existingSecurityZone
						.getTagServices();
				List<String> uiTagServices = securityZone.getTagServices();
				List<String> addRmvTagSvc = new ArrayList<String>();
				if (!dbTagServices.equals(uiTagServices)) {
					for (String svc : dbTagServices) {
						if (!uiTagServices.contains(svc)) {
							addRmvTagSvc.add(svc);
						}
					}

					for (String svc : uiTagServices) {
						if (!dbTagServices.contains(svc)) {
							addRmvTagSvc.add(svc);
						}
					}
				}
				if (!addRmvTagSvc.isEmpty()) {
					for (String svc : addRmvTagSvc) {
						/*
						 * if user is neither svc admin nor admin then
						 * add/remove of svc in zone is not allowed
						 */
						if (!svcStore.isServiceAdminUser(svc, userName)) {
							throwRestError("User : "
									+ userName
									+ " is not allowed to add/remove tag service : "
									+ svc + " in Ranger Security zone : " + existingSecurityZone.getName());

						}
					}
				}
				
				
				/*
				 * Validation on service association / disassociation with
				 * security zone
				 */
				Set<String> existingRangerSecurityZoneService = existingSecurityZone
						.getServices().keySet();
				Set<String> newRangerSecurityZoneService = securityZone.getServices()
						.keySet();
				Set<String> diffServiceSet = new HashSet<>(Sets.difference(
							newRangerSecurityZoneService,
							existingRangerSecurityZoneService));
					diffServiceSet.addAll(Sets.difference(
							existingRangerSecurityZoneService,
							newRangerSecurityZoneService));

				if (diffServiceSet != null && diffServiceSet.size() > 0) {
					for (String svc : diffServiceSet) {
						/*
						 * if user is neither svc admin nor admin then
						 * add/remove of svc in zone is not allowed
						 */
						if (!svcStore.isServiceAdminUser(svc, userName)) {
							throwRestError("User : "
									+ userName
									+ " is not allowed to add/remove service : "
									+ svc + " in Ranger Security zone : " + existingSecurityZone.getName());

						}
					}
				}

				/* Validation for resources on existing svc in security zone */
				for (String svc : existingRangerSecurityZoneService) {
					RangerSecurityZoneService rangerSecurityZnSvcFromDB = existingSecurityZone
							.getServices().get(svc);

					RangerSecurityZoneService rangerSecurityZnSvcFromUI = securityZone
							.getServices().get(svc);

					if (rangerSecurityZnSvcFromUI != null) {
						if (!Objects.equals(rangerSecurityZnSvcFromDB.getResources(), rangerSecurityZnSvcFromUI.getResources())) {
							if (!svcStore.isServiceAdminUser(svc, userName)) {
								throwRestError("User : "
										+ userName
										+ " is not allowed to edit resource in service : "
										+ svc + " in Ranger Security zone : " + existingSecurityZone.getName());
							}
						}
					}

				}
			}

		}
	}
	
	private void throwRestError(String message){
		throw restErrorUtil.createRESTException(HttpServletResponse.SC_FORBIDDEN, message, true);
	}


	private void ensureAdminAccess(RangerSecurityZone securityZone) {
		if (!bizUtil.isAdmin()) {
			String userName = bizUtil.getCurrentUserLoginId();
			throw restErrorUtil.createRESTException(
					"Ranger Securtiy Zone is not accessible for user '" + userName + "'.",
					MessageEnums.OPER_NO_PERMISSION);
		}
		else {
			blockAdminFromKMSService(securityZone);
		}
	}

	private void blockAdminFromKMSService(RangerSecurityZone securityZone) {
		if(securityZone != null) {
			Map<String, RangerSecurityZoneService> serviceMap = securityZone.getServices();
			if (serviceMap != null) {
				for (String serviceName : serviceMap.keySet()) {
					XXService xService = daoManager.getXXService().findByName(serviceName);
					if (xService != null) {
						XXServiceDef xServiceDef = daoManager.getXXServiceDef().getById(xService.getType());
						if (EmbeddedServiceDefsUtil.KMS_IMPL_CLASS_NAME.equals(xServiceDef.getImplclassname())) {
							throw restErrorUtil.createRESTException(
									"KMS Services/Service-Defs are not accessible for Zone operations",
									MessageEnums.OPER_NOT_ALLOWED_FOR_ENTITY);
						}
					}
				}
			}
		}
	}

	private void removeEmptyEntries(RangerSecurityZone securityZone) {
                bizUtil.removeEmptyStrings(securityZone.getTagServices());
		bizUtil.removeEmptyStrings(securityZone.getAdminUsers());
		bizUtil.removeEmptyStrings(securityZone.getAdminUserGroups());
		bizUtil.removeEmptyStrings(securityZone.getAuditUsers());
		bizUtil.removeEmptyStrings(securityZone.getAuditUserGroups());
		Map<String, RangerSecurityZoneService> serviceResouceMap=securityZone.getServices();
		if(serviceResouceMap!=null) {
			Set<Map.Entry<String, RangerSecurityZoneService>> serviceResouceMapEntries = serviceResouceMap.entrySet();
			Iterator<Map.Entry<String, RangerSecurityZoneService>> iterator=serviceResouceMapEntries.iterator();
			while (iterator.hasNext()){
				Map.Entry<String, RangerSecurityZoneService> serviceResouceMapEntry = iterator.next();
				RangerSecurityZoneService rangerSecurityZoneService=serviceResouceMapEntry.getValue();
				List<HashMap<String, List<String>>> resources=rangerSecurityZoneService.getResources();
				if(resources!=null) {
					for (Map<String, List<String>> resource : resources) {
						if (resource!=null) {
							for (Map.Entry<String, List<String>> entry : resource.entrySet()) {
								List<String> resourceValues  = entry.getValue();
								bizUtil.removeEmptyStrings(resourceValues);
							}
						}
					}
				}
			}
		}
	}
}
