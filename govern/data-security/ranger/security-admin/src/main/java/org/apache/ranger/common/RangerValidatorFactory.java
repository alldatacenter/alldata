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

package org.apache.ranger.common;

import org.apache.ranger.plugin.model.validation.RangerPolicyValidator;
import org.apache.ranger.plugin.model.validation.RangerSecurityZoneValidator;
import org.apache.ranger.plugin.model.validation.RangerServiceDefValidator;
import org.apache.ranger.plugin.model.validation.RangerServiceValidator;
import org.apache.ranger.plugin.model.validation.RangerRoleValidator;
import org.apache.ranger.plugin.store.RoleStore;
import org.apache.ranger.plugin.store.SecurityZoneStore;
import org.apache.ranger.plugin.store.ServiceStore;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Service
@Scope("singleton")
public class RangerValidatorFactory {
	public RangerServiceValidator getServiceValidator(ServiceStore store) {
		return new RangerServiceValidator(store);
	}

	public RangerPolicyValidator getPolicyValidator(ServiceStore store) {
		return new RangerPolicyValidator(store);
	}

	public RangerServiceDefValidator getServiceDefValidator(ServiceStore store) {
		return new RangerServiceDefValidator(store);
	}

	public RangerSecurityZoneValidator getSecurityZoneValidator(ServiceStore store, SecurityZoneStore securityZoneStore) {
	    return new RangerSecurityZoneValidator(store, securityZoneStore);
    }

	public RangerRoleValidator getRangerRoleValidator(RoleStore roleStore) {
		return new RangerRoleValidator(roleStore);
	}
}
