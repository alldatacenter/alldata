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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.biz.AssetMgr;
import org.apache.ranger.biz.RangerBizUtil;
import org.apache.ranger.biz.ServiceDBStore;
import org.apache.ranger.biz.TagDBStore;
import org.apache.ranger.common.RESTErrorUtil;
import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.entity.XXService;
import org.apache.ranger.entity.XXServiceDef;
import org.apache.ranger.plugin.model.RangerPluginInfo;
import org.apache.ranger.plugin.model.RangerService;
import org.apache.ranger.plugin.model.RangerServiceResource;
import org.apache.ranger.plugin.model.RangerTag;
import org.apache.ranger.plugin.model.RangerTagResourceMap;
import org.apache.ranger.plugin.model.RangerTagDef;
import org.apache.ranger.plugin.store.EmbeddedServiceDefsUtil;
import org.apache.ranger.plugin.store.TagStore;
import org.apache.ranger.plugin.store.TagValidator;
import org.apache.ranger.plugin.util.RangerRESTUtils;
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

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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

import java.util.List;

@Path(TagRESTConstants.TAGDEF_NAME_AND_VERSION)
@Component
@Scope("request")
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class TagREST {

    private static final Logger LOG = LoggerFactory.getLogger(TagREST.class);
    public static final String Allowed_User_List_For_Tag_Download = "tag.download.auth.users";

	@Autowired
	RESTErrorUtil restErrorUtil;

	@Autowired
	ServiceDBStore svcStore;

	@Autowired
	TagDBStore tagStore;
	
	@Autowired
	RangerDaoManager daoManager;
	
	@Autowired
	RangerBizUtil bizUtil;

    @Autowired
    AssetMgr assetMgr;

    TagValidator validator;

    public TagREST() {
	}

	@PostConstruct
	public void initStore() {
		validator = new TagValidator();

        tagStore.setServiceStore(svcStore);
        validator.setTagStore(tagStore);
	}

    TagStore getTagStore() {
        return tagStore;
    }

    @POST
    @Path(TagRESTConstants.TAGDEFS_RESOURCE)
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public RangerTagDef createTagDef(RangerTagDef tagDef, @DefaultValue("true") @QueryParam("updateIfExists") boolean updateIfExists) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.createTagDef(" + tagDef + ", " + updateIfExists + ")");
        }

        RangerTagDef ret;

        try {
            RangerTagDef exist = validator.preCreateTagDef(tagDef, updateIfExists);
            if (exist == null) {
                ret = tagStore.createTagDef(tagDef);
            } else if (updateIfExists) {
                ret = updateTagDef(exist.getId(), exist);
            } else {
                throw new Exception("tag-definition with Id " + exist.getId() + " already exists");
            }
        } catch(Exception excp) {
            LOG.error("createTagDef(" + tagDef + ") failed", excp);

            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.createTagDef(" + tagDef + ", " + updateIfExists + "): " + ret);
        }

        return ret;
    }


    @PUT
    @Path(TagRESTConstants.TAGDEF_RESOURCE + "{id}")
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public RangerTagDef updateTagDef(@PathParam("id") Long id, RangerTagDef tagDef) {

        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.updateTagDef(" + id + ")");
        }
        if (tagDef.getId() == null) {
            tagDef.setId(id);
        } else if (!tagDef.getId().equals(id)) {
            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST , "tag name mismatch", true);
        }

        RangerTagDef ret;

        try {
            ret = tagStore.updateTagDef(tagDef);
        } catch (Exception excp) {
            LOG.error("updateTagDef(" + id + ") failed", excp);
            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }
        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.updateTagDef(" + id + ")");
        }

        return ret;
    }

    @DELETE
    @Path(TagRESTConstants.TAGDEF_RESOURCE + "{id}")
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public void deleteTagDef(@PathParam("id") Long id) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.deleteTagDef(" + id + ")");
        }

        try {
            tagStore.deleteTagDef(id);
        } catch(Exception excp) {
            LOG.error("deleteTagDef(" + id + ") failed", excp);

            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.deleteTagDef(" + id + ")");
        }
    }

    @DELETE
    @Path(TagRESTConstants.TAGDEF_RESOURCE + "guid/{guid}")
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public void deleteTagDefByGuid(@PathParam("guid") String guid) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.deleteTagDefByGuid(" + guid + ")");
        }

        try {
            RangerTagDef exist = tagStore.getTagDefByGuid(guid);
            if(exist!=null){
				tagStore.deleteTagDef(exist.getId());
			}
        } catch(Exception excp) {
            LOG.error("deleteTagDef(" + guid + ") failed", excp);

            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.deleteTagDefByGuid(" + guid + ")");
        }
    }

    @GET
    @Path(TagRESTConstants.TAGDEF_RESOURCE + "{id}")
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public RangerTagDef getTagDef(@PathParam("id") Long id) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.getTagDef(" + id + ")");
        }

        RangerTagDef ret;

        try {
            ret = tagStore.getTagDef(id);
        } catch(Exception excp) {
            LOG.error("getTagDef(" + id + ") failed", excp);

            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }

        if(ret == null) {
            throw restErrorUtil.createRESTException(HttpServletResponse.SC_NOT_FOUND, "Not found", true);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.getTagDef(" + id + "): " + ret);
        }

        return ret;
    }

    @GET
    @Path(TagRESTConstants.TAGDEF_RESOURCE + "guid/{guid}")
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public RangerTagDef getTagDefByGuid(@PathParam("guid") String guid) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.getTagDefByGuid(" + guid + ")");
        }

        RangerTagDef ret;

        try {
            ret = tagStore.getTagDefByGuid(guid);
        } catch(Exception excp) {
            LOG.error("getTagDefByGuid(" + guid + ") failed", excp);

            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }

        if(ret == null) {
            throw restErrorUtil.createRESTException(HttpServletResponse.SC_NOT_FOUND, "Not found", true);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.getTagDefByGuid(" + guid + "): " + ret);
        }

        return ret;
    }

    @GET
    @Path(TagRESTConstants.TAGDEF_RESOURCE + "name/{name}")
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public RangerTagDef getTagDefByName(@PathParam("name") String name) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.getTagDefByName(" + name + ")");
        }

        RangerTagDef ret;

        try {
            ret = tagStore.getTagDefByName(name);
        } catch(Exception excp) {
            LOG.error("getTagDefByName(" + name + ") failed", excp);

            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }

        if(ret == null) {
            throw restErrorUtil.createRESTException(HttpServletResponse.SC_NOT_FOUND, "Not found", true);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.getTagDefByName(" + name + "): " + ret);
        }

        return ret;
    }

    @GET
    @Path(TagRESTConstants.TAGDEFS_RESOURCE)
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public List<RangerTagDef> getAllTagDefs() {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.getAllTagDefs()");
        }

        List<RangerTagDef> ret;

        try {
            ret = tagStore.getTagDefs(new SearchFilter());
        } catch(Exception excp) {
            LOG.error("getAllTagDefs() failed", excp);

            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }

        if(ret == null) {
            throw restErrorUtil.createRESTException(HttpServletResponse.SC_NOT_FOUND, "Not found", true);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.getAllTagDefs()");
        }

        return ret;
    }

    @GET
    @Path(TagRESTConstants.TAGTYPES_RESOURCE)
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public List<String> getTagTypes() {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.getTagTypes()");
        }

        List<String> ret = null;

        try {
            ret = tagStore.getTagTypes();
        } catch(Exception excp) {
            LOG.error("getTagTypes() failed", excp);

            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.getTagTypes(): count=" + (ret != null ? ret.size() : 0));
        }

        return ret;
    }


    @POST
    @Path(TagRESTConstants.TAGS_RESOURCE)
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public RangerTag createTag(RangerTag tag, @DefaultValue("true") @QueryParam("updateIfExists") boolean updateIfExists) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.createTag(" + tag + ", " + updateIfExists + ")");
        }

        RangerTag ret;

        try {
            RangerTag exist = validator.preCreateTag(tag);
            if (exist == null) {
                ret = tagStore.createTag(tag);
            } else if (updateIfExists) {
                ret = updateTag(exist.getId(), tag);
            } else {
                throw new Exception("tag with Id " + exist.getId() + " already exists");
            }
        } catch(Exception excp) {
            LOG.error("createTag(" + tag + ") failed", excp);

            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.createTag(" + tag + ", " + updateIfExists + "): " + ret);
        }

        return ret;
    }

    @PUT
    @Path(TagRESTConstants.TAG_RESOURCE + "{id}")
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public RangerTag updateTag(@PathParam("id") Long id, RangerTag tag) {

        RangerTag ret;

        try {
            validator.preUpdateTag(id, tag);
            ret = tagStore.updateTag(tag);
        } catch (Exception excp) {
            LOG.error("updateTag(" + id + ") failed", excp);
            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }
        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.updateTag(" + id + "): " + ret);
        }

        return ret;
    }

    @PUT
    @Path(TagRESTConstants.TAG_RESOURCE + "guid/{guid}")
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public RangerTag updateTagByGuid(@PathParam("guid") String guid, RangerTag tag) {

        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.updateTagByGuid(" + guid + ")");
        }

        RangerTag ret;

        try {
            validator.preUpdateTagByGuid(guid, tag);
            ret = tagStore.updateTag(tag);
        } catch (Exception excp) {
            LOG.error("updateTagByGuid(" + guid + ") failed", excp);
            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }
        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.updateTagByGuid(" + guid + "): " + ret);
        }

        return ret;
    }

    @DELETE
    @Path(TagRESTConstants.TAG_RESOURCE + "{id}")
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public void deleteTag(@PathParam("id") Long id) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.deleteTag(" + id +")");
        }

        try {
            validator.preDeleteTag(id);
            tagStore.deleteTag(id);
        } catch(Exception excp) {
            LOG.error("deleteTag(" + id + ") failed", excp);
            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.deleteTag(" + id + ")");
        }
    }

    @DELETE
    @Path(TagRESTConstants.TAG_RESOURCE + "guid/{guid}")
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public void deleteTagByGuid(@PathParam("guid") String guid) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.deleteTagByGuid(" + guid + ")");
        }

        try {
            RangerTag exist = validator.preDeleteTagByGuid(guid);
            tagStore.deleteTag(exist.getId());
        } catch(Exception excp) {
            LOG.error("deleteTagByGuid(" + guid + ") failed", excp);

            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.deleteTagByGuid(" + guid + ")");
        }
    }

    @GET
    @Path(TagRESTConstants.TAG_RESOURCE + "{id}")
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public RangerTag getTag(@PathParam("id") Long id) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.getTag(" + id + ")");
        }
        RangerTag ret;

        try {
            ret = tagStore.getTag(id);
        } catch(Exception excp) {
            LOG.error("getTag(" + id + ") failed", excp);

            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.getTag(" + id + "): " + ret);
        }

        return ret;
    }

    @GET
    @Path(TagRESTConstants.TAG_RESOURCE + "guid/{guid}")
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public RangerTag getTagByGuid(@PathParam("guid") String guid) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.getTagByGuid(" + guid + ")");
        }
        RangerTag ret;

        try {
            ret = tagStore.getTagByGuid(guid);
        } catch(Exception excp) {
            LOG.error("getTagByGuid(" + guid + ") failed", excp);

            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.getTagByGuid(" + guid + "): " + ret);
        }

        return ret;
    }

    @GET
    @Path(TagRESTConstants.TAGS_RESOURCE + "type/{type}")
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public List<RangerTag> getTagsByType(@PathParam("type") String type) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.getTagsByType(" + type + ")");
        }
        List<RangerTag> ret;

        try {
            ret = tagStore.getTagsByType(type);
        } catch(Exception excp) {
            LOG.error("getTagsByType(" + type + ") failed", excp);

            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.getTagsByType(" + type + "): " + ret);
        }

        return ret;
    }

    @GET
    @Path(TagRESTConstants.TAGS_RESOURCE)
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public List<RangerTag> getAllTags() {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.getAllTags()");
        }

        List<RangerTag> ret;

        try {
            ret = tagStore.getTags(new SearchFilter());
        } catch(Exception excp) {
            LOG.error("getAllTags() failed", excp);

            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }

        if (CollectionUtils.isEmpty(ret)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("getAllTags() - No tags found");
            }
        }
        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.getAllTags(): " + ret);
        }

        return ret;
    }

    @POST
    @Path(TagRESTConstants.RESOURCES_RESOURCE)
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public RangerServiceResource createServiceResource(RangerServiceResource resource, @DefaultValue("true") @QueryParam("updateIfExists") boolean updateIfExists) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.createServiceResource(" + resource + ", " + updateIfExists + ")");
        }

        RangerServiceResource ret;

        try {
            RangerServiceResource exist = validator.preCreateServiceResource(resource);
            if (exist == null) {
                ret = tagStore.createServiceResource(resource);
            } else if (updateIfExists) {
                ret = updateServiceResource(exist.getId(), resource);
            } else {
                throw new Exception("resource with Id " + exist.getId() + " already exists");
            }
        } catch(Exception excp) {
            LOG.error("createServiceResource(" + resource + ") failed", excp);

            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.createServiceResource(" + resource + ", " + updateIfExists + "): " + ret);
        }

        return ret;
    }

    @PUT
    @Path(TagRESTConstants.RESOURCE_RESOURCE + "{id}")
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public RangerServiceResource updateServiceResource(@PathParam("id") Long id, RangerServiceResource resource) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.updateServiceResource(" + id + ")");
        }
        RangerServiceResource ret;

        try {
            validator.preUpdateServiceResource(id, resource);
            ret = tagStore.updateServiceResource(resource);
        } catch(Exception excp) {
            LOG.error("updateServiceResource(" + resource + ") failed", excp);

            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.updateServiceResource(" + id + "): " + ret);
        }
        return ret;
    }

    @PUT
    @Path(TagRESTConstants.RESOURCE_RESOURCE + "guid/{guid}")
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public RangerServiceResource updateServiceResourceByGuid(@PathParam("guid") String guid, RangerServiceResource resource) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.updateServiceResourceByGuid(" + guid + ", " + resource + ")");
        }
        RangerServiceResource ret;
        try {
            validator.preUpdateServiceResourceByGuid(guid, resource);
            ret = tagStore.updateServiceResource(resource);
        } catch(Exception excp) {
            LOG.error("updateServiceResourceByGuid(" + guid + ", " + resource + ") failed", excp);

            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }
        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.updateServiceResourceByGuid(" + guid + ", " + resource + "): " + ret);
        }
        return ret;
    }

    @DELETE
    @Path(TagRESTConstants.RESOURCE_RESOURCE + "{id}")
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public void deleteServiceResource(@PathParam("id") Long id) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.deleteServiceResource(" + id + ")");
        }
        try {
            validator.preDeleteServiceResource(id);
            tagStore.deleteServiceResource(id);
        } catch (Exception excp) {
            LOG.error("deleteServiceResource() failed", excp);

            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.deleteServiceResource(" + id + ")");
        }
    }

    @DELETE
    @Path(TagRESTConstants.RESOURCE_RESOURCE + "guid/{guid}")
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public void deleteServiceResourceByGuid(@PathParam("guid") String guid, @DefaultValue("false") @QueryParam("deleteReferences") boolean deleteReferences) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.deleteServiceResourceByGuid(" + guid + ", " + deleteReferences + ")");
        }

        try {
            RangerServiceResource exist = validator.preDeleteServiceResourceByGuid(guid, deleteReferences);
            if (deleteReferences) {
                List<RangerTagResourceMap> tagResourceMaps = tagStore.getTagResourceMapsForResourceGuid(exist.getGuid());
                if (CollectionUtils.isNotEmpty(tagResourceMaps)) {
                    for (RangerTagResourceMap tagResourceMap : tagResourceMaps) {
                        deleteTagResourceMap(tagResourceMap.getId());
                    }
                }
            }
            tagStore.deleteServiceResource(exist.getId());
        } catch(Exception excp) {
            LOG.error("deleteServiceResourceByGuid(" + guid + ", " + deleteReferences + ") failed", excp);

            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.deleteServiceResourceByGuid(" + guid + ", " + deleteReferences + ")");
        }
    }

    @GET
    @Path(TagRESTConstants.RESOURCE_RESOURCE + "{id}")
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public RangerServiceResource getServiceResource(@PathParam("id") Long id) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.getServiceResource(" + id + ")");
        }
        RangerServiceResource ret;
        try {
            ret = tagStore.getServiceResource(id);
        } catch(Exception excp) {
            LOG.error("getServiceResource(" + id + ") failed", excp);

            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }
        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.getServiceResource(" + id + "): " + ret);
        }
        return ret;
    }

    @GET
    @Path(TagRESTConstants.RESOURCE_RESOURCE + "guid/{guid}")
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public RangerServiceResource getServiceResourceByGuid(@PathParam("guid") String guid) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.getServiceResourceByGuid(" + guid + ")");
        }
        RangerServiceResource ret;
        try {
            ret = tagStore.getServiceResourceByGuid(guid);
        } catch(Exception excp) {
            LOG.error("getServiceResourceByGuid(" + guid + ") failed", excp);

            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }
        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.getServiceResourceByGuid(" + guid + "): " + ret);
        }
        return ret;
    }

    @GET
    @Path(TagRESTConstants.RESOURCES_RESOURCE + "service/{serviceName}")
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public List<RangerServiceResource> getServiceResourcesByService(@PathParam("serviceName") String serviceName) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.getServiceResourcesByService(" + serviceName + ")");
        }

        List<RangerServiceResource> ret = null;

        try {
            ret = tagStore.getServiceResourcesByService(serviceName);
        } catch(Exception excp) {
            LOG.error("getServiceResourcesByService(" + serviceName + ") failed", excp);

            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }

        if (CollectionUtils.isEmpty(ret)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("getServiceResourcesByService(" + serviceName + ") - No service-resources found");
            }
        }
        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.getServiceResourcesByService(" + serviceName + "): count=" + (ret == null ? 0 : ret.size()));
        }

        return ret;
    }

    @GET
    @Path(TagRESTConstants.RESOURCE_RESOURCE + "service/{serviceName}/signature/{resourceSignature}")
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public RangerServiceResource getServiceResourceByServiceAndResourceSignature(@PathParam("serviceName") String serviceName,
                                                                       @PathParam("resourceSignature") String resourceSignature) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.getServiceResourceByServiceAndResourceSignature(" + serviceName + ", " + resourceSignature + ")");
        }

        RangerServiceResource ret = null;

        try {
            ret = tagStore.getServiceResourceByServiceAndResourceSignature(serviceName, resourceSignature);
        } catch(Exception excp) {
            LOG.error("getServiceResourceByServiceAndResourceSignature(" + serviceName + ", " + resourceSignature + ") failed", excp);

            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.getServiceResourceByServiceAndResourceSignature(" + serviceName + ", " + resourceSignature + "): " + ret);
        }

        return ret;
    }

    @GET
    @Path(TagRESTConstants.RESOURCES_RESOURCE)
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public List<RangerServiceResource> getAllServiceResources() {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.getAllServiceResources()");
        }

        List<RangerServiceResource> ret;

        try {
            ret = tagStore.getServiceResources(new SearchFilter());
        } catch(Exception excp) {
            LOG.error("getAllServiceResources() failed", excp);

            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.getAllServiceResources(): count=" + (ret == null ? 0 : ret.size()));
        }

        return ret;
    }

    @POST
    @Path(TagRESTConstants.TAGRESOURCEMAPS_RESOURCE)
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public RangerTagResourceMap createTagResourceMap(@QueryParam("tag-guid") String tagGuid, @QueryParam("resource-guid") String resourceGuid,
                                                     @DefaultValue("false") @QueryParam("lenient") boolean lenient) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.createTagResourceMap(" + tagGuid + ", " +  resourceGuid + ", " + lenient + ")");
        }

        RangerTagResourceMap tagResourceMap;

        try {
            tagResourceMap = tagStore.getTagResourceMapForTagAndResourceGuid(tagGuid, resourceGuid);
            if (tagResourceMap == null) {
                tagResourceMap = validator.preCreateTagResourceMap(tagGuid, resourceGuid);

                tagResourceMap = tagStore.createTagResourceMap(tagResourceMap);
            } else if (!lenient) {
                throw new Exception("tagResourceMap with tag-guid=" + tagGuid + " and resource-guid=" + resourceGuid + " already exists");
            }
        } catch(Exception excp) {
            LOG.error("createTagResourceMap(" + tagGuid + ", " +  resourceGuid + ", " + lenient + ") failed", excp);

            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.createTagResourceMap(" + tagGuid + ", " + resourceGuid + ", " + lenient + ")");
        }

        return tagResourceMap;
    }

    @DELETE
    @Path(TagRESTConstants.TAGRESOURCEMAP_RESOURCE + "{id}")
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public void deleteTagResourceMap(@PathParam("id") Long id) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.deleteTagResourceMap(" + id + ")");
        }
        try {
            validator.preDeleteTagResourceMap(id);
            tagStore.deleteTagResourceMap(id);
        } catch (Exception excp) {
            LOG.error("deleteTagResourceMap() failed", excp);

            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.deleteTagResourceMap(" + id + ")");
        }
    }

    @DELETE
    @Path(TagRESTConstants.TAGRESOURCEMAP_RESOURCE + "guid/{guid}")
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public void deleteTagResourceMapByGuid(@PathParam("guid") String guid) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.deleteTagResourceMapByGuid(" + guid + ")");
        }

        try {
            RangerTagResourceMap exist = validator.preDeleteTagResourceMapByGuid(guid);
            tagStore.deleteServiceResource(exist.getId());
        } catch(Exception excp) {
            LOG.error("deleteTagResourceMapByGuid(" + guid + ") failed", excp);

            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.deleteTagResourceMapByGuid(" + guid + ")");
        }
    }

    @DELETE
    @Path(TagRESTConstants.TAGRESOURCEMAPS_RESOURCE)
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public void deleteTagResourceMap(@QueryParam("tag-guid") String tagGuid, @QueryParam("resource-guid") String resourceGuid) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.deleteTagResourceMap(" + tagGuid + ", " + resourceGuid + ")");
        }

        try {
            RangerTagResourceMap exist = validator.preDeleteTagResourceMap(tagGuid, resourceGuid);
            tagStore.deleteTagResourceMap(exist.getId());
        } catch(Exception excp) {
            LOG.error("deleteTagResourceMap(" + tagGuid + ", " +  resourceGuid + ") failed", excp);

            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.deleteTagResourceMap(" + tagGuid + ", " + resourceGuid + ")");
        }
    }

    @GET
    @Path(TagRESTConstants.TAGRESOURCEMAP_RESOURCE + "{id}")
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public RangerTagResourceMap getTagResourceMap(@PathParam("id") Long id) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.getTagResourceMap(" + id + ")");
        }
        RangerTagResourceMap ret;

        try {
            ret = tagStore.getTagResourceMap(id);
        } catch(Exception excp) {
            LOG.error("getTagResourceMap(" + id + ") failed", excp);

            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.getTagResourceMap(" + id + "): " + ret);
        }

        return ret;
    }

    @GET
    @Path(TagRESTConstants.TAGRESOURCEMAP_RESOURCE + "guid/{guid}")
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public RangerTagResourceMap getTagResourceMapByGuid(@PathParam("guid") String guid) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.getTagResourceMapByGuid(" + guid + ")");
        }
        RangerTagResourceMap ret;

        try {
            ret = tagStore.getTagResourceMapByGuid(guid);
        } catch(Exception excp) {
            LOG.error("getTagResourceMapByGuid(" + guid + ") failed", excp);

            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.getTagResourceMapByGuid(" + guid + "): " + ret);
        }

        return ret;
    }

    @GET
    @Path(TagRESTConstants.TAGRESOURCEMAP_RESOURCE + "tag-resource-guid")
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public RangerTagResourceMap getTagResourceMap(@QueryParam("tagGuid") String tagGuid, @QueryParam("resourceGuid") String resourceGuid) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.getTagResourceMap(" + tagGuid + ", " + resourceGuid + ")");
        }

        RangerTagResourceMap ret = null;

        try {
            ret = tagStore.getTagResourceMapForTagAndResourceGuid(tagGuid, resourceGuid);
        } catch(Exception excp) {
            LOG.error("getTagResourceMap(" + tagGuid + ", " +  resourceGuid + ") failed", excp);

            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.getTagResourceMap(" + tagGuid + ", " + resourceGuid + ")");
        }

        return ret;
    }

    @GET
    @Path(TagRESTConstants.TAGRESOURCEMAPS_RESOURCE)
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public List<RangerTagResourceMap> getAllTagResourceMaps() {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.getAllTagResourceMaps()");
        }

        List<RangerTagResourceMap> ret;

        try {
            ret = tagStore.getTagResourceMaps(new SearchFilter());
        } catch(Exception excp) {
            LOG.error("getAllTagResourceMaps() failed", excp);

            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }

        if (CollectionUtils.isEmpty(ret)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("getAllTagResourceMaps() - No tag-resource-maps found");
            }
        }
        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.getAllTagResourceMaps(): " + ret);
        }

        return ret;
    }

    // This API is used by tag-sync to upload tag-objects

    @PUT
    @Path(TagRESTConstants.IMPORT_SERVICETAGS_RESOURCE)
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public void importServiceTags(ServiceTags serviceTags) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.importServiceTags()");
        }

        try {

            ServiceTagsProcessor serviceTagsProcessor = new ServiceTagsProcessor(tagStore);
            serviceTagsProcessor.process(serviceTags);

        } catch (Exception excp) {
            LOG.error("importServiceTags() failed", excp);

            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);

        }
        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.importServiceTags()");
        }
    }

    // This API is typically used by plug-in to get selected tagged resources from RangerAdmin

    @GET
    @Path(TagRESTConstants.TAGS_DOWNLOAD + "{serviceName}")
    @Produces({ "application/json", "application/xml" })
    public ServiceTags getServiceTagsIfUpdated(@PathParam("serviceName") String serviceName,
                                                   @QueryParam(TagRESTConstants.LAST_KNOWN_TAG_VERSION_PARAM) Long lastKnownVersion,
                                               @DefaultValue("0") @QueryParam(TagRESTConstants.LAST_ACTIVATION_TIME) Long lastActivationTime, @QueryParam("pluginId") String pluginId,
                                               @DefaultValue("false") @QueryParam(RangerRESTUtils.REST_PARAM_SUPPORTS_TAG_DELTAS) Boolean supportsTagDeltas,
                                               @DefaultValue("") @QueryParam(RangerRESTUtils.REST_PARAM_CAPABILITIES) String pluginCapabilities,
                                               @Context HttpServletRequest request) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.getServiceTagsIfUpdated(" + serviceName + ", " + lastKnownVersion + ", " + lastActivationTime + ", " + pluginId + ", " + supportsTagDeltas + ")");
        }

		ServiceTags ret      = null;
		int         httpCode = HttpServletResponse.SC_OK;
		String      logMsg   = null;
        Long downloadedVersion = null;
        String clusterName = null;
		if (request != null) {
			clusterName = !StringUtils.isEmpty(request.getParameter(SearchFilter.CLUSTER_NAME)) ? request.getParameter(SearchFilter.CLUSTER_NAME) : "";
		}

        try {
            bizUtil.failUnauthenticatedIfNotAllowed();

            ret = tagStore.getServiceTagsIfUpdated(serviceName, lastKnownVersion, !supportsTagDeltas);

            if (ret == null) {
                downloadedVersion = lastKnownVersion;
                httpCode = HttpServletResponse.SC_NOT_MODIFIED;
                logMsg = "No change since last update";
            } else {
                downloadedVersion = ret.getTagVersion();
                httpCode = HttpServletResponse.SC_OK;
                logMsg = "Returning " + (ret.getTags() != null ? ret.getTags().size() : 0) + " tags. Tag version=" + ret.getTagVersion();
            }
        } catch (WebApplicationException webException) {
            httpCode = webException.getResponse().getStatus();
            logMsg = webException.getResponse().getEntity().toString();
        } catch(Exception excp) {
			httpCode = HttpServletResponse.SC_BAD_REQUEST;
			logMsg   = excp.getMessage();
        } finally {
            assetMgr.createPluginInfo(serviceName, pluginId, request, RangerPluginInfo.ENTITY_TYPE_TAGS, downloadedVersion, lastKnownVersion, lastActivationTime, httpCode, clusterName, pluginCapabilities);
        }

        if(httpCode != HttpServletResponse.SC_OK) {
            boolean logError = httpCode != HttpServletResponse.SC_NOT_MODIFIED;
            throw restErrorUtil.createRESTException(httpCode, logMsg, logError);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.getServiceTagsIfUpdated(" + serviceName + ", " + lastKnownVersion + ", " + lastActivationTime + ", " + pluginId + ", " + supportsTagDeltas + ")");
        }

        return ret;
    }

    @GET
    @Path(TagRESTConstants.TAGS_SECURE_DOWNLOAD + "{serviceName}")
    @Produces({ "application/json", "application/xml" })
    public ServiceTags getSecureServiceTagsIfUpdated(@PathParam("serviceName") String serviceName,
                                                   @QueryParam(TagRESTConstants.LAST_KNOWN_TAG_VERSION_PARAM) Long lastKnownVersion,
                                                     @DefaultValue("0") @QueryParam(TagRESTConstants.LAST_ACTIVATION_TIME) Long lastActivationTime, @QueryParam("pluginId") String pluginId,
                                                     @DefaultValue("false") @QueryParam(RangerRESTUtils.REST_PARAM_SUPPORTS_TAG_DELTAS) Boolean supportsTagDeltas,
                                                     @DefaultValue("") @QueryParam(RangerRESTUtils.REST_PARAM_CAPABILITIES) String pluginCapabilities,
                                                     @Context HttpServletRequest request) {

        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.getSecureServiceTagsIfUpdated(" + serviceName + ", " + lastKnownVersion + ", " + lastActivationTime + ", " + pluginId + ", " + supportsTagDeltas + ")");
        }

		ServiceTags ret      = null;
		int         httpCode = HttpServletResponse.SC_OK;
		String      logMsg   = null;
		boolean isAllowed = false;
		boolean isAdmin = bizUtil.isAdmin();
		boolean isKeyAdmin = bizUtil.isKeyAdmin();
        Long downloadedVersion = null;
        String clusterName = null;
		if (request != null) {
			clusterName = !StringUtils.isEmpty(request.getParameter(SearchFilter.CLUSTER_NAME)) ? request.getParameter(SearchFilter.CLUSTER_NAME) : "";
		}

        try {
        	XXService xService = daoManager.getXXService().findByName(serviceName);
        	if (xService == null) {
                LOG.error("Requested Service not found. serviceName=" + serviceName);
                throw restErrorUtil.createRESTException(HttpServletResponse.SC_NOT_FOUND, "Service:" + serviceName + " not found",
                        false);
            }
        	XXServiceDef xServiceDef = daoManager.getXXServiceDef().getById(xService.getType());
        	RangerService rangerService = svcStore.getServiceByName(serviceName);
        	
        	if (StringUtils.equals(xServiceDef.getImplclassname(), EmbeddedServiceDefsUtil.KMS_IMPL_CLASS_NAME)) {
        		if (isKeyAdmin) {
        			isAllowed = true;
        		}else {
        			isAllowed = bizUtil.isUserAllowed(rangerService, Allowed_User_List_For_Tag_Download);
        		}
        	}else{
        		if (isAdmin) {
        			isAllowed = true;
        		}else{
        			isAllowed = bizUtil.isUserAllowed(rangerService, Allowed_User_List_For_Tag_Download);
        		}
        	}
        	if (isAllowed) {
	            ret = tagStore.getServiceTagsIfUpdated(serviceName, lastKnownVersion, !supportsTagDeltas);

				if(ret == null) {
                    downloadedVersion = lastKnownVersion;
					httpCode = HttpServletResponse.SC_NOT_MODIFIED;
					logMsg   = "No change since last update";
				} else {
                    downloadedVersion = ret.getTagVersion();
					httpCode = HttpServletResponse.SC_OK;
					logMsg   = "Returning " + (ret.getTags() != null ? ret.getTags().size() : 0) + " tags. Tag version=" + ret.getTagVersion();
				}
			}else{
				LOG.error("getSecureServiceTagsIfUpdated(" + serviceName + ", " + lastKnownVersion + ", " + lastActivationTime + ") failed as User doesn't have permission to download tags");
				httpCode = HttpServletResponse.SC_FORBIDDEN; // assert user is authenticated.
				logMsg = "User doesn't have permission to download tags";
			}
        } catch (WebApplicationException webException) {
            httpCode = webException.getResponse().getStatus();
            logMsg = webException.getResponse().getEntity().toString();
        } catch (Exception excp) {
			httpCode = HttpServletResponse.SC_BAD_REQUEST;
			logMsg   = excp.getMessage();
        }  finally {
            assetMgr.createPluginInfo(serviceName, pluginId, request, RangerPluginInfo.ENTITY_TYPE_TAGS, downloadedVersion, lastKnownVersion, lastActivationTime, httpCode, clusterName, pluginCapabilities);
        }

        if(httpCode != HttpServletResponse.SC_OK) {
            boolean logError = httpCode != HttpServletResponse.SC_NOT_MODIFIED;
            throw restErrorUtil.createRESTException(httpCode, logMsg, logError);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.getSecureServiceTagsIfUpdated(" + serviceName + ", " + lastKnownVersion + ", " + lastActivationTime + ", " + pluginId + ", " + supportsTagDeltas + ")");
        }

        return ret;
    }

    @DELETE
    @Path("/server/tagdeltas")
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public void deleteTagDeltas(@DefaultValue("3") @QueryParam("days") Integer olderThan, @Context HttpServletRequest request) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> ServiceREST.deleteTagDeltas(" + olderThan + ")");
        }

        svcStore.resetTagUpdateLog(olderThan, ServiceTags.TagsChangeType.INVALIDATE_TAG_DELTAS);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== ServiceREST.deleteTagDeltas(" + olderThan + ")");
        }
    }

}
