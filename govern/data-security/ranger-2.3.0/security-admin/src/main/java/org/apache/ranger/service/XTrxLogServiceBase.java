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

import java.util.ArrayList;
import java.util.List;

import org.apache.ranger.common.SearchCriteria;
import org.apache.ranger.entity.XXTrxLog;
import org.apache.ranger.view.VXTrxLog;
import org.apache.ranger.view.VXTrxLogList;

public abstract class XTrxLogServiceBase<T extends XXTrxLog, V extends VXTrxLog>
		extends AbstractBaseResourceService<T, V> {
	public static final String NAME = "XTrxLog";

	public XTrxLogServiceBase() {

	}

	@Override
	protected T mapViewToEntityBean(V vObj, T mObj, int OPERATION_CONTEXT) {
		mObj.setObjectClassType( vObj.getObjectClassType());
		mObj.setObjectId( vObj.getObjectId());
		mObj.setParentObjectId( vObj.getParentObjectId());
		mObj.setParentObjectClassType( vObj.getParentObjectClassType());
		mObj.setParentObjectName( vObj.getParentObjectName());
		mObj.setObjectName( vObj.getObjectName());
		mObj.setAttributeName( vObj.getAttributeName());
		mObj.setPreviousValue( vObj.getPreviousValue());
		mObj.setNewValue( vObj.getNewValue());
		mObj.setTransactionId( vObj.getTransactionId());
		mObj.setAction( vObj.getAction());
		mObj.setSessionId( vObj.getSessionId());
		mObj.setRequestId( vObj.getRequestId());
		mObj.setSessionType( vObj.getSessionType());
		return mObj;
	}

	@Override
	protected V mapEntityToViewBean(V vObj, T mObj) {
		vObj.setObjectClassType( mObj.getObjectClassType());
		vObj.setObjectId( mObj.getObjectId());
		vObj.setParentObjectId( mObj.getParentObjectId());
		vObj.setParentObjectClassType( mObj.getParentObjectClassType());
		vObj.setParentObjectName( mObj.getParentObjectName());
		vObj.setObjectName( mObj.getObjectName());
		vObj.setAttributeName( mObj.getAttributeName());
		vObj.setPreviousValue( mObj.getPreviousValue());
		vObj.setNewValue( mObj.getNewValue());
		vObj.setTransactionId( mObj.getTransactionId());
		vObj.setAction( mObj.getAction());
		vObj.setSessionId( mObj.getSessionId());
		vObj.setRequestId( mObj.getRequestId());
		vObj.setSessionType( mObj.getSessionType());
		return vObj;
	}

	/**
	 * @param searchCriteria
	 * @return
	 */
	public VXTrxLogList searchXTrxLogs(SearchCriteria searchCriteria) {
		VXTrxLogList returnList = new VXTrxLogList();
		List<VXTrxLog> xTrxLogList = new ArrayList<VXTrxLog>();

		List<T> resultList = searchResources(searchCriteria,
				searchFields, sortFields, returnList);

		// Iterate over the result list and create the return list
		for (T gjXTrxLog : resultList) {
			VXTrxLog vXTrxLog = populateViewBean(gjXTrxLog);
			xTrxLogList.add(vXTrxLog);
		}

		returnList.setVXTrxLogs(xTrxLogList);
		return returnList;
	}

}
