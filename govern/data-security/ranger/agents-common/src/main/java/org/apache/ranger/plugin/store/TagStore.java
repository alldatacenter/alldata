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

package org.apache.ranger.plugin.store;

import org.apache.ranger.plugin.model.*;
import org.apache.ranger.plugin.util.SearchFilter;
import org.apache.ranger.plugin.util.ServiceTags;

import java.util.List;

/**
 * Interface to backing store for the top-level TAG model objects
 */

public interface TagStore {
    void init() throws Exception;

    void setServiceStore(ServiceStore svcStore);

    ServiceStore getServiceStore();

    RangerTagDef createTagDef(RangerTagDef tagDef) throws Exception;

    RangerTagDef updateTagDef(RangerTagDef TagDef) throws Exception;

    void deleteTagDefByName(String name) throws Exception;

	void deleteTagDef(Long id) throws Exception;

    RangerTagDef getTagDef(Long id) throws Exception;

    RangerTagDef getTagDefByGuid(String guid) throws Exception;

	RangerTagDef getTagDefByName(String name) throws Exception;

    List<RangerTagDef> getTagDefs(SearchFilter filter) throws Exception;

    PList<RangerTagDef> getPaginatedTagDefs(SearchFilter filter) throws Exception;

    List<String> getTagTypes() throws Exception;


    RangerTag createTag(RangerTag tag) throws Exception;

    RangerTag updateTag(RangerTag tag) throws Exception;

    void deleteTag(Long id) throws Exception;

    RangerTag getTag(Long id) throws Exception;

    RangerTag getTagByGuid(String guid) throws Exception;

    List<Long> getTagIdsForResourceId(Long resourceId) throws Exception;

    List<RangerTag> getTagsByType(String name) throws Exception;

    List<RangerTag> getTagsForResourceId(Long resourceId) throws Exception;

    List<RangerTag> getTagsForResourceGuid(String resourceGuid) throws Exception;

    List<RangerTag> getTags(SearchFilter filter) throws Exception;

    PList<RangerTag> getPaginatedTags(SearchFilter filter) throws Exception;


    RangerServiceResource createServiceResource(RangerServiceResource resource) throws Exception;

    RangerServiceResource updateServiceResource(RangerServiceResource resource) throws Exception;

    void refreshServiceResource(Long resourceId) throws Exception;

    void deleteServiceResource(Long id) throws Exception;

    void deleteServiceResourceByGuid(String guid) throws Exception;

    RangerServiceResource getServiceResource(Long id) throws Exception;

    RangerServiceResource getServiceResourceByGuid(String guid) throws Exception;

    List<RangerServiceResource> getServiceResourcesByService(String serviceName) throws Exception;

    List<String> getServiceResourceGuidsByService(String serviceName) throws Exception;

    RangerServiceResource getServiceResourceByServiceAndResourceSignature(String serviceName, String resourceSignature) throws Exception;

    List<RangerServiceResource> getServiceResources(SearchFilter filter) throws Exception;

    PList<RangerServiceResource> getPaginatedServiceResources(SearchFilter filter) throws Exception;


    RangerTagResourceMap createTagResourceMap(RangerTagResourceMap tagResourceMap) throws Exception;

    void deleteTagResourceMap(Long id) throws Exception;

    RangerTagResourceMap getTagResourceMap(Long id) throws Exception;

    RangerTagResourceMap getTagResourceMapByGuid(String guid) throws Exception;

    List<RangerTagResourceMap> getTagResourceMapsForTagId(Long tagId) throws Exception;

    List<RangerTagResourceMap> getTagResourceMapsForTagGuid(String tagGuid) throws Exception;

    List<RangerTagResourceMap> getTagResourceMapsForResourceId(Long resourceId) throws Exception;

    List<RangerTagResourceMap> getTagResourceMapsForResourceGuid(String resourceGuid) throws Exception;

    RangerTagResourceMap getTagResourceMapForTagAndResourceId(Long tagId, Long resourceId) throws Exception;

    RangerTagResourceMap getTagResourceMapForTagAndResourceGuid(String tagGuid, String resourceGuid) throws Exception;

    List<RangerTagResourceMap> getTagResourceMaps(SearchFilter filter) throws Exception;

    PList<RangerTagResourceMap> getPaginatedTagResourceMaps(SearchFilter filter) throws Exception;


    ServiceTags getServiceTagsIfUpdated(String serviceName, Long lastKnownVersion, boolean needsBackwardCompatibility) throws Exception;
    ServiceTags getServiceTags(String serviceName, Long lastKnownVersion) throws Exception;
    ServiceTags getServiceTagsDelta(String serviceName, Long lastKnownVersion) throws Exception;


    Long getTagVersion(String serviceName);

    void deleteAllTagObjectsForService(String serviceName) throws Exception;

    boolean isInPlaceTagUpdateSupported();

}
