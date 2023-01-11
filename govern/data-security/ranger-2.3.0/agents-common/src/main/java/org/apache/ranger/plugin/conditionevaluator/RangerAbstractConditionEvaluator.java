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
package org.apache.ranger.plugin.conditionevaluator;

import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItemCondition;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerPolicyConditionDef;

public abstract class RangerAbstractConditionEvaluator implements RangerConditionEvaluator {
	protected RangerServiceDef serviceDef;
	protected RangerPolicyConditionDef  conditionDef;
	protected RangerPolicyItemCondition condition;

	@Override
	public void setServiceDef(RangerServiceDef serviceDef) {
		this.serviceDef = serviceDef;
	}

	@Override
	public void setConditionDef(RangerPolicyConditionDef conditionDef) {
		this.conditionDef = conditionDef;
	}

	@Override
	public void setPolicyItemCondition(RangerPolicyItemCondition condition) {
		this.condition = condition;
	}

	@Override
	public void init() {
	}

	public RangerPolicyItemCondition getPolicyItemCondition() { return condition; }

}
