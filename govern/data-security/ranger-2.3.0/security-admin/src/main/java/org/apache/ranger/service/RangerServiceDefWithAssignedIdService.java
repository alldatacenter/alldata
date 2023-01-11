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

import org.apache.ranger.entity.XXServiceDefWithAssignedId;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.springframework.stereotype.Service;

@Service
public class RangerServiceDefWithAssignedIdService extends RangerServiceDefServiceBase<XXServiceDefWithAssignedId, RangerServiceDef> {

	@Override
	protected XXServiceDefWithAssignedId mapViewToEntityBean(RangerServiceDef vObj, XXServiceDefWithAssignedId xObj,
			int OPERATION_CONTEXT) {
		return super.mapViewToEntityBean(vObj, xObj, OPERATION_CONTEXT);
	}

	@Override
	protected RangerServiceDef mapEntityToViewBean(RangerServiceDef vObj, XXServiceDefWithAssignedId xObj) {
		return super.mapEntityToViewBean(vObj, xObj);
	}

	@Override
	protected void validateForCreate(RangerServiceDef vObj) {

	}

	@Override
	protected void validateForUpdate(RangerServiceDef vServiceDef, XXServiceDefWithAssignedId xServiceDef) {

	}

	public RangerServiceDef getPopulatedViewObject(XXServiceDefWithAssignedId xServiceDef) {
		return this.populateViewBean(xServiceDef);
	}

}
