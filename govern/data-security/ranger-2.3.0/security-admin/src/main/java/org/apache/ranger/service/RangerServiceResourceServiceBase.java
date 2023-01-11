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

package org.apache.ranger.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.ranger.common.GUIDUtil;
import org.apache.ranger.common.MessageEnums;
import org.apache.ranger.entity.XXResourceDef;
import org.apache.ranger.entity.XXService;
import org.apache.ranger.entity.XXServiceResource;
import org.apache.ranger.entity.XXServiceResourceElement;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyResource;
import org.apache.ranger.plugin.model.RangerServiceResource;
import org.apache.ranger.plugin.store.PList;
import org.apache.ranger.plugin.util.SearchFilter;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class RangerServiceResourceServiceBase<T extends XXServiceResource, V extends RangerServiceResource> extends RangerBaseModelService<T, V> {

	@Autowired
	GUIDUtil guidUtil;

	@Override
	protected T mapViewToEntityBean(V vObj, T xObj, int operationContext) {
		String guid = (StringUtils.isEmpty(vObj.getGuid())) ? guidUtil.genGUID() : vObj.getGuid();

		xObj.setGuid(guid);
		xObj.setVersion(vObj.getVersion());
		xObj.setIsEnabled(vObj.getIsEnabled());
		xObj.setResourceSignature(vObj.getResourceSignature());

		XXService xService = daoMgr.getXXService().findByName(vObj.getServiceName());
		if (xService == null) {
			throw restErrorUtil.createRESTException("Error Populating XXServiceResource. No Service found with name: " + vObj.getServiceName(), MessageEnums.INVALID_INPUT_DATA);
		}

		xObj.setServiceId(xService.getId());

		return xObj;
	}

	@Override
	protected V mapEntityToViewBean(V vObj, T xObj) {
		vObj.setGuid(xObj.getGuid());
		vObj.setVersion(xObj.getVersion());
		vObj.setIsEnabled(xObj.getIsEnabled());
		vObj.setResourceSignature(xObj.getResourceSignature());

		XXService xService = daoMgr.getXXService().getById(xObj.getServiceId());

		vObj.setServiceName(xService.getName());

		Map<String, RangerPolicy.RangerPolicyResource> resourceElements = getServiceResourceElements(xObj);

		vObj.setResourceElements(resourceElements);

		return vObj;
	}

	Map<String, RangerPolicyResource> getServiceResourceElements(T xObj) {
        List<XXServiceResourceElement> resElementList = daoMgr.getXXServiceResourceElement().findByResourceId(xObj.getId());
        Map<String, RangerPolicy.RangerPolicyResource> resourceElements = new HashMap<String, RangerPolicy.RangerPolicyResource>();

        for (XXServiceResourceElement resElement : resElementList) {
            List<String> resValueMapList = daoMgr.getXXServiceResourceElementValue().findValuesByResElementId(resElement.getId());

            XXResourceDef xResDef = daoMgr.getXXResourceDef().getById(resElement.getResDefId());

            RangerPolicyResource policyRes = new RangerPolicyResource();
            policyRes.setIsExcludes(resElement.getIsExcludes());
            policyRes.setIsRecursive(resElement.getIsRecursive());
            policyRes.setValues(resValueMapList);

            resourceElements.put(xResDef.getName(), policyRes);
        }
        return resourceElements;
    }

	public PList<V> searchServiceResources(SearchFilter searchFilter) {
		PList<V> retList = new PList<V>();
		List<V> resourceList = new ArrayList<V>();

		List<T> xResourceList = searchRangerObjects(searchFilter, searchFields, sortFields, retList);

		for (T xResource : xResourceList) {
			V taggedRes = populateViewBean(xResource);
			resourceList.add(taggedRes);
		}
		retList.setList(resourceList);
		return retList;
	}

}
