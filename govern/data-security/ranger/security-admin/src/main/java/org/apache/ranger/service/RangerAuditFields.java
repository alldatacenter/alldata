/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ranger.service;

import org.apache.ranger.common.ContextUtil;
import org.apache.ranger.common.DateUtil;
import org.apache.ranger.entity.XXDBBase;
import org.springframework.stereotype.Component;

@Component
public class RangerAuditFields<T extends XXDBBase> {

	public <T extends XXDBBase, PARENT extends XXDBBase> T populateAuditFields(T xObj, PARENT parentObj) {
		xObj.setCreateTime(parentObj.getCreateTime());
		xObj.setUpdateTime(parentObj.getUpdateTime());
		xObj.setAddedByUserId(parentObj.getAddedByUserId());
		xObj.setUpdatedByUserId(parentObj.getUpdatedByUserId());
		return xObj;
	}

	public <T extends XXDBBase> T populateAuditFieldsForCreate(T xObj) {
		xObj.setCreateTime(DateUtil.getUTCDate());
		xObj.setUpdateTime(DateUtil.getUTCDate());
		xObj.setAddedByUserId(ContextUtil.getCurrentUserId());
		xObj.setUpdatedByUserId(ContextUtil.getCurrentUserId());
		return xObj;
	}

}
