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

/**
 *
 */

import org.apache.ranger.entity.XXUgsyncAuditInfo;
import org.apache.ranger.view.VXUgsyncAuditInfo;

public abstract class XUgsyncAuditInfoServiceBase<T extends XXUgsyncAuditInfo, V extends VXUgsyncAuditInfo>
		extends AbstractBaseResourceService<T, V> {
	public static final String NAME = "XUgsyncAuditInfo";

	public XUgsyncAuditInfoServiceBase() {

	}

	@Override
	protected T mapViewToEntityBean(V vObj, T mObj, int OPERATION_CONTEXT) {
		mObj.setEventTime(vObj.getEventTime());
		mObj.setUserName(vObj.getUserName());
		mObj.setSyncSource(vObj.getSyncSource());
		mObj.setNoOfNewGroups(vObj.getNoOfNewGroups());
		mObj.setNoOfNewUsers(vObj.getNoOfNewUsers());
		mObj.setNoOfModifiedGroups(vObj.getNoOfModifiedGroups());
		mObj.setNoOfModifiedUsers(vObj.getNoOfModifiedUsers());
		return mObj;
	}

	@Override
	protected V mapEntityToViewBean(V vObj, T mObj) {
		vObj.setEventTime( mObj.getEventTime());
		vObj.setUserName( mObj.getUserName());
		vObj.setSyncSource( mObj.getSyncSource());
		vObj.setNoOfNewGroups(mObj.getNoOfNewGroups());
		vObj.setNoOfNewUsers(mObj.getNoOfNewUsers());
		vObj.setNoOfModifiedGroups(mObj.getNoOfModifiedGroups());
		vObj.setNoOfModifiedUsers(mObj.getNoOfModifiedUsers());
		return vObj;
	}

    @Override
    protected void validateForCreate(VXUgsyncAuditInfo viewBaseBean) {
        // TODO Auto-generated method stub

    }

    @Override
    protected void validateForUpdate(VXUgsyncAuditInfo viewBaseBean, XXUgsyncAuditInfo t) {
        // TODO Auto-generated method stub

    }
}
