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

package org.apache.ranger.plugin.model.validation;

import java.util.*;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.plugin.errors.ValidationErrorCode;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerPolicy.RangerDataMaskPolicyItem;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItem;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItemAccess;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyResource;
import org.apache.ranger.plugin.model.RangerPolicy.RangerRowFilterPolicyItem;
import org.apache.ranger.plugin.model.RangerPolicyResourceSignature;
import org.apache.ranger.plugin.model.RangerSecurityZone;
import org.apache.ranger.plugin.model.RangerService;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerAccessTypeDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerResourceDef;
import org.apache.ranger.plugin.model.RangerValiditySchedule;
import org.apache.ranger.plugin.store.ServiceStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RangerPolicyValidator extends RangerValidator {

	private static final Logger LOG = LoggerFactory.getLogger(RangerPolicyValidator.class);

	public RangerPolicyValidator(ServiceStore store) {
		super(store);
	}

	public void validate(RangerPolicy policy, Action action, boolean isAdmin) throws Exception {
		if(LOG.isDebugEnabled()) {
			LOG.debug(String.format("==> RangerPolicyValidator.validate(%s, %s, %s)", policy, action, isAdmin));
		}

		List<ValidationFailureDetails> failures = new ArrayList<>();
		boolean valid = isValid(policy, action, isAdmin, failures);
		String message = "";
		try {
			if (!valid) {
				message = serializeFailures(failures);
				throw new Exception(message);
			}
		} finally {
			if(LOG.isDebugEnabled()) {
				LOG.debug(String.format("<== RangerPolicyValidator.validate(%s, %s, %s): %s, reason[%s]", policy, action, isAdmin, valid, message));
			}
		}
	}

	@Override
	boolean isValid(Long id, Action action, List<ValidationFailureDetails> failures) {
		if(LOG.isDebugEnabled()) {
			LOG.debug(String.format("==> RangerPolicyValidator.isValid(%s, %s, %s)", id, action, failures));
		}

		boolean valid = true;
		if (action != Action.DELETE) {
			ValidationErrorCode error = ValidationErrorCode.POLICY_VALIDATION_ERR_UNSUPPORTED_ACTION;
			failures.add(new ValidationFailureDetailsBuilder()
				.isAnInternalError()
				.becauseOf(error.getMessage())
				.errorCode(error.getErrorCode())
				.build());
			valid = false;
		} else if (id == null) {
			ValidationErrorCode error = ValidationErrorCode.POLICY_VALIDATION_ERR_MISSING_FIELD;
			failures.add(new ValidationFailureDetailsBuilder()
					.becauseOf("policy id was null/missing")
					.field("id")
					.isMissing()
					.errorCode(error.getErrorCode())
					.becauseOf(error.getMessage("id"))
					.build());
			valid = false;
		} else if (policyExists(id)) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("No policy found for id[" + id + "]! ok!");
			}
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug(String.format("<== RangerPolicyValidator.isValid(%s, %s, %s): %s", id, action, failures, valid));
		}
		return valid;
	}

	boolean isValid(RangerPolicy policy, Action action, boolean isAdmin, List<ValidationFailureDetails> failures) {
		if(LOG.isDebugEnabled()) {
			LOG.debug(String.format("==> RangerPolicyValidator.isValid(%s, %s, %s, %s)", policy, action, isAdmin, failures));
		}

		if (!(action == Action.CREATE || action == Action.UPDATE)) {
			throw new IllegalArgumentException("isValid(RangerPolicy, ...) is only supported for create/update");
		}
		boolean valid = true;
		if (policy == null) {
			ValidationErrorCode error = ValidationErrorCode.POLICY_VALIDATION_ERR_NULL_POLICY_OBJECT;
			failures.add(new ValidationFailureDetailsBuilder()
				.field("policy")
				.isMissing()
				.becauseOf(error.getMessage())
				.errorCode(error.getErrorCode())
				.build());
			valid = false;
		} else {
			Integer priority = policy.getPolicyPriority();
			if (priority != null) {
				if (priority < RangerPolicy.POLICY_PRIORITY_NORMAL || priority > RangerPolicy.POLICY_PRIORITY_OVERRIDE) {
					ValidationErrorCode error = ValidationErrorCode.POLICY_VALIDATION_ERR_POLICY_INVALID_PRIORITY;
					failures.add(new ValidationFailureDetailsBuilder()
							.field("policyPriority")
							.isSemanticallyIncorrect()
							.becauseOf(error.getMessage("out of range"))
							.errorCode(error.getErrorCode())
							.build());
					valid = false;
				}
			}
			Long id = policy.getId();
			RangerPolicy existingPolicy = null;

			if (action == Action.UPDATE) { // id is ignored for CREATE
				if (id == null) {
					ValidationErrorCode error = ValidationErrorCode.POLICY_VALIDATION_ERR_MISSING_FIELD;
					failures.add(new ValidationFailureDetailsBuilder()
						.field("id")
						.isMissing()
						.becauseOf(error.getMessage("id"))
						.errorCode(error.getErrorCode())
						.build());
					valid = false;
				}

				existingPolicy = getPolicy(id);

				if (existingPolicy == null) {
					ValidationErrorCode error = ValidationErrorCode.POLICY_VALIDATION_ERR_INVALID_POLICY_ID;
					failures.add(new ValidationFailureDetailsBuilder()
						.field("id")
						.isSemanticallyIncorrect()
						.becauseOf(error.getMessage(id))
						.errorCode(error.getErrorCode())
						.build());
					valid = false;
				}
			}
			String policyName  = policy.getName();
			String serviceName = policy.getService();
			String policyServicetype = policy.getServiceType();
			String zoneName    = policy.getZoneName();

			RangerService service = null;
			RangerSecurityZone zone = null;
			boolean serviceNameValid = false;
			if (StringUtils.isBlank(serviceName)) {
				ValidationErrorCode error = ValidationErrorCode.POLICY_VALIDATION_ERR_MISSING_FIELD;
				failures.add(new ValidationFailureDetailsBuilder()
						.field("service name")
						.isMissing()
						.becauseOf(error.getMessage("service name"))
						.errorCode(error.getErrorCode())
						.build());
				valid = false;
			} else {
				service = getService(serviceName);
				if (service == null) {
					ValidationErrorCode error = ValidationErrorCode.POLICY_VALIDATION_ERR_INVALID_SERVICE_NAME;
					failures.add(new ValidationFailureDetailsBuilder()
							.field("service name")
							.isSemanticallyIncorrect()
							.becauseOf(error.getMessage(serviceName))
							.errorCode(error.getErrorCode())
							.build());
					valid = false;
				} else {
					serviceNameValid = true;

					String serviceType = service.getType();

					if (StringUtils.isNotEmpty(serviceType) && StringUtils.isNotEmpty(policyServicetype)) {
						if (!serviceType.equalsIgnoreCase(policyServicetype)) {
							ValidationErrorCode error = ValidationErrorCode.POLICY_VALIDATION_ERR_INVALID_SERVICE_TYPE;

							failures.add(new ValidationFailureDetailsBuilder()
									.field("service type")
									.isSemanticallyIncorrect()
									.becauseOf(error.getMessage(policyServicetype,serviceName))
									.errorCode(error.getErrorCode())
									.build());
							valid = false;
						}
					}
				}
			}

			if (StringUtils.isNotEmpty(zoneName)) {
				zone = getSecurityZone(zoneName);
				if (zone == null) {
					ValidationErrorCode error = ValidationErrorCode.POLICY_VALIDATION_ERR_NONEXISTANT_ZONE_NAME;
					failures.add(new ValidationFailureDetailsBuilder()
							.field("zoneName")
							.isSemanticallyIncorrect()
							.becauseOf(error.getMessage(id, zoneName))
							.errorCode(error.getErrorCode())
							.build());
					valid = false;
				}
				List<String> tagSvcList = zone.getTagServices();
				Set<String> svcNameSet = zone.getServices().keySet();
				if(!svcNameSet.contains(serviceName) && !tagSvcList.contains(serviceName)){
					ValidationErrorCode error = ValidationErrorCode.POLICY_VALIDATION_ERR_SERVICE_NOT_ASSOCIATED_TO_ZONE;
					failures.add(new ValidationFailureDetailsBuilder()
							.field("zoneName")
							.isSemanticallyIncorrect()
							.becauseOf(error.getMessage(serviceName, zoneName))
							.errorCode(error.getErrorCode())
							.build());
					valid = false;
				}
			}

			if (StringUtils.isBlank(policyName)) {
				ValidationErrorCode error = ValidationErrorCode.POLICY_VALIDATION_ERR_MISSING_FIELD;
				failures.add(new ValidationFailureDetailsBuilder()
					.field("name")
					.isMissing()
					.becauseOf(error.getMessage("name"))
					.errorCode(error.getErrorCode())
					.build());
				valid = false;
			} else {
				if (service != null && (StringUtils.isEmpty(zoneName) || zone != null)) {
					Long zoneId = zone != null ? zone.getId() : RangerSecurityZone.RANGER_UNZONED_SECURITY_ZONE_ID;
					Long policyId = getPolicyId(service.getId(), policyName, zoneId);

					if (policyId != null) {
						if (action == Action.CREATE) {
							ValidationErrorCode error = ValidationErrorCode.POLICY_VALIDATION_ERR_POLICY_NAME_CONFLICT;
							failures.add(new ValidationFailureDetailsBuilder()
									.field("policy name")
									.isSemanticallyIncorrect()
									.becauseOf(error.getMessage(policyId, serviceName))
									.errorCode(error.getErrorCode())
									.build());
							valid = false;
						} else if (!policyId.equals(id)) { // action == UPDATE
							ValidationErrorCode error = ValidationErrorCode.POLICY_VALIDATION_ERR_POLICY_NAME_CONFLICT;
							failures.add(new ValidationFailureDetailsBuilder()
									.field("id/name")
									.isSemanticallyIncorrect()
									.becauseOf(error.getMessage(policyId, serviceName))
									.errorCode(error.getErrorCode())
									.build());
							valid = false;
						}
					}
				}
			}

			if(existingPolicy != null) {
				if (!StringUtils.equalsIgnoreCase(existingPolicy.getService(), policy.getService())) {
					ValidationErrorCode error = ValidationErrorCode.POLICY_VALIDATION_ERR_POLICY_UPDATE_MOVE_SERVICE_NOT_ALLOWED;
					failures.add(new ValidationFailureDetailsBuilder()
							.field("service name")
							.isSemanticallyIncorrect()
							.becauseOf(error.getMessage(policy.getId(), existingPolicy.getService(), policy.getService()))
							.errorCode(error.getErrorCode())
							.build());
					valid = false;
				}

				int existingPolicyType = existingPolicy.getPolicyType() == null ? RangerPolicy.POLICY_TYPE_ACCESS : existingPolicy.getPolicyType();
				int policyType         = policy.getPolicyType() == null ? RangerPolicy.POLICY_TYPE_ACCESS : policy.getPolicyType();

				if (existingPolicyType != policyType) {
					ValidationErrorCode error = ValidationErrorCode.POLICY_VALIDATION_ERR_POLICY_TYPE_CHANGE_NOT_ALLOWED;
					failures.add(new ValidationFailureDetailsBuilder()
							.field("policy type")
							.isSemanticallyIncorrect()
							.becauseOf(error.getMessage(policy.getId(), existingPolicyType, policyType))
							.errorCode(error.getErrorCode())
							.build());
					valid = false;
				}

				String existingZoneName = existingPolicy.getZoneName();

				if (StringUtils.isNotEmpty(zoneName) || StringUtils.isNotEmpty(existingZoneName)) {
					if (!StringUtils.equals(existingZoneName, zoneName)) {
						ValidationErrorCode error = ValidationErrorCode.POLICY_VALIDATION_ERR_UPDATE_ZONE_NAME_NOT_ALLOWED;
						failures.add(new ValidationFailureDetailsBuilder()
								.field("zoneName")
								.isSemanticallyIncorrect()
								.becauseOf(error.getMessage(existingZoneName, zoneName))
								.errorCode(error.getErrorCode())
								.build());
						valid = false;
					}
				}
			}

			boolean          isAuditEnabled   = getIsAuditEnabled(policy);
			String           serviceDefName   = null;
			RangerServiceDef serviceDef       = null;
			int              policyItemsCount = 0;

			int policyType=policy.getPolicyType() == null ? RangerPolicy.POLICY_TYPE_ACCESS : policy.getPolicyType();
			switch (policyType) {
			case RangerPolicy.POLICY_TYPE_DATAMASK:
				if (CollectionUtils.isNotEmpty(policy.getDataMaskPolicyItems())) {
					policyItemsCount += policy.getDataMaskPolicyItems().size();
				}
				break;
			case RangerPolicy.POLICY_TYPE_ROWFILTER:
				if (CollectionUtils.isNotEmpty(policy.getRowFilterPolicyItems())) {
					policyItemsCount += policy.getRowFilterPolicyItems().size();
				}
				break;
			default:
				if (CollectionUtils.isNotEmpty(policy.getPolicyItems())){
					policyItemsCount += policy.getPolicyItems().size();
				}
				if(CollectionUtils.isNotEmpty(policy.getDenyPolicyItems())) {
					policyItemsCount += policy.getDenyPolicyItems().size();
				}
				break;
			}

			if (policyItemsCount == 0 && !isAuditEnabled) {
				ValidationErrorCode error = ValidationErrorCode.POLICY_VALIDATION_ERR_MISSING_POLICY_ITEMS;
				failures.add(new ValidationFailureDetailsBuilder()
					.field("policy items")
					.isMissing()
					.becauseOf(error.getMessage())
					.errorCode(error.getErrorCode())
					.build());
				valid = false;
			} else if (service != null) {
				serviceDefName = service.getType();
				serviceDef     = getServiceDef(serviceDefName);
				if (serviceDef == null) {
					ValidationErrorCode error = ValidationErrorCode.POLICY_VALIDATION_ERR_MISSING_SERVICE_DEF;
					failures.add(new ValidationFailureDetailsBuilder()
						.field("policy service def")
						.isAnInternalError()
						.becauseOf(error.getMessage(serviceDefName, serviceName))
						.errorCode(error.getErrorCode())
						.build());
					valid = false;
				} else {
					if (Boolean.TRUE.equals(policy.getIsDenyAllElse())) {
						if (CollectionUtils.isNotEmpty(policy.getDenyPolicyItems()) || CollectionUtils.isNotEmpty(policy.getDenyExceptions())) {
							ValidationErrorCode error = ValidationErrorCode.POLICY_VALIDATION_ERR_UNSUPPORTED_POLICY_ITEM_TYPE;
							failures.add(new ValidationFailureDetailsBuilder()
									.field("policy items")
									.becauseOf(error.getMessage())
									.errorCode(error.getErrorCode())
									.build());
							valid = false;
						}
					}
					valid = isValidPolicyItems(policy.getPolicyItems(), failures, serviceDef) && valid;
					valid = isValidPolicyItems(policy.getDenyPolicyItems(), failures, serviceDef) && valid;
					valid = isValidPolicyItems(policy.getAllowExceptions(), failures, serviceDef) && valid;
					valid = isValidPolicyItems(policy.getDenyExceptions(), failures, serviceDef) && valid;
				}
			}

			if (serviceNameValid) { // resource checks can't be done meaningfully otherwise
				valid = isValidValiditySchedule(policy, failures, action) && valid;
				valid = isValidResources(policy, failures, action, isAdmin, serviceDef) && valid;
				valid = isValidAccessTypeDef(policy, failures, action, isAdmin, serviceDef) && valid;
			}
		}
		
		if(LOG.isDebugEnabled()) {
			LOG.debug(String.format("<== RangerPolicyValidator.isValid(%s, %s, %s, %s): %s", policy, action, isAdmin, failures, valid));
		}
		return valid;
	}
	
	boolean isValidAccessTypeDef(RangerPolicy policy, final List<ValidationFailureDetails> failures, Action action,boolean isAdmin, final RangerServiceDef serviceDef) {
		boolean valid = true;
		if(LOG.isDebugEnabled()) {
			LOG.debug(String.format("==> RangerPolicyValidator.isValidAccessTypeDef(%s, %s, %s,%s,%s)", policy, failures, action,isAdmin,serviceDef));
		}
		int policyType=policy.getPolicyType() == null ? RangerPolicy.POLICY_TYPE_ACCESS : policy.getPolicyType();
		//row filter policy
		if (policyType==RangerPolicy.POLICY_TYPE_ROWFILTER){
			List<String> rowFilterAccessTypeDefNames=new ArrayList<String>();
			if(serviceDef!=null && serviceDef.getRowFilterDef()!=null){
				if(!CollectionUtils.isEmpty(serviceDef.getRowFilterDef().getAccessTypes())){
					for(RangerAccessTypeDef rangerAccessTypeDef:serviceDef.getRowFilterDef().getAccessTypes()){
						rowFilterAccessTypeDefNames.add(rangerAccessTypeDef.getName().toLowerCase());
					}
				}
			}

			if(!CollectionUtils.isEmpty(policy.getRowFilterPolicyItems())){
				for(RangerRowFilterPolicyItem rangerRowFilterPolicyItem:policy.getRowFilterPolicyItems()){
					if(!CollectionUtils.isEmpty(rangerRowFilterPolicyItem.getAccesses())){
						for(RangerPolicyItemAccess rangerPolicyItemAccess : rangerRowFilterPolicyItem.getAccesses()){
							if(!rowFilterAccessTypeDefNames.contains(rangerPolicyItemAccess.getType().toLowerCase())){
								ValidationErrorCode error = ValidationErrorCode.POLICY_VALIDATION_ERR_POLICY_ITEM_ACCESS_TYPE_INVALID;
								failures.add(new ValidationFailureDetailsBuilder()
									.field("row filter policy item access type")
									.isSemanticallyIncorrect()
									.becauseOf(error.getMessage(rangerPolicyItemAccess.getType(), rowFilterAccessTypeDefNames))
									.errorCode(error.getErrorCode())
									.build());
								valid = false;
							}
						}
					}
				}
			}
		}
		//data mask policy
		if (policyType==RangerPolicy.POLICY_TYPE_DATAMASK){
			List<String> dataMaskAccessTypeDefNames=new ArrayList<String>();
			if(serviceDef!=null && serviceDef.getDataMaskDef()!=null){
				if(!CollectionUtils.isEmpty(serviceDef.getDataMaskDef().getAccessTypes())){
					for(RangerAccessTypeDef rangerAccessTypeDef:serviceDef.getDataMaskDef().getAccessTypes()){
						dataMaskAccessTypeDefNames.add(rangerAccessTypeDef.getName().toLowerCase());
					}
				}
			}

			if(!CollectionUtils.isEmpty(policy.getDataMaskPolicyItems())){
				for(RangerDataMaskPolicyItem rangerDataMaskPolicyItem:policy.getDataMaskPolicyItems()){
					if(!CollectionUtils.isEmpty(rangerDataMaskPolicyItem.getAccesses())){
						for(RangerPolicyItemAccess rangerPolicyItemAccess : rangerDataMaskPolicyItem.getAccesses()){
							if(!dataMaskAccessTypeDefNames.contains(rangerPolicyItemAccess.getType().toLowerCase())){
								ValidationErrorCode error = ValidationErrorCode.POLICY_VALIDATION_ERR_POLICY_ITEM_ACCESS_TYPE_INVALID;
								failures.add(new ValidationFailureDetailsBuilder()
									.field("data masking policy item access type")
									.isSemanticallyIncorrect()
									.becauseOf(error.getMessage(rangerPolicyItemAccess.getType(), dataMaskAccessTypeDefNames))
									.errorCode(error.getErrorCode())
									.build());
								valid = false;
							}
						}
					}
				}
			}
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug(String.format("<== RangerPolicyValidator.isValidAccessTypeDef(%s, %s, %s,%s,%s)", policy, failures, action,isAdmin,serviceDef));
		}
		return valid;
	}

	boolean isValidResources(RangerPolicy policy, final List<ValidationFailureDetails> failures, Action action,
			boolean isAdmin, final RangerServiceDef serviceDef) {
		
		if(LOG.isDebugEnabled()) {
			LOG.debug(String.format("==> RangerPolicyValidator.isValidResources(%s, %s, %s, %s, %s)", policy, failures, action, isAdmin, serviceDef));
		}
		
		boolean valid = true;
		Map<String, RangerPolicyResource> resourceMap = policy.getResources();
		if (resourceMap != null) { // following checks can't be done meaningfully otherwise
			valid = isPolicyResourceUnique(policy, failures, action) && valid;
			if (serviceDef != null) { // following checks can't be done meaningfully otherwise
				valid = isValidResourceNames(policy, failures, serviceDef) && valid;
				valid = isValidResourceValues(resourceMap, failures, serviceDef) && valid;
				valid = isValidResourceFlags(resourceMap, failures, serviceDef.getResources(), serviceDef.getName(), policy.getName(), isAdmin) && valid;
			}
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug(String.format("<== RangerPolicyValidator.isValidResources(%s, %s, %s, %s, %s): %s", policy, failures, action, isAdmin, serviceDef, valid));
		}
		return valid;
	}

	boolean isValidValiditySchedule(RangerPolicy policy, final List<ValidationFailureDetails> failures, Action action) {

		boolean valid = true;
		if (LOG.isDebugEnabled()) {
			LOG.debug(String.format("==> RangerPolicyValidator.isValidValiditySchedule(%s, %s, %s)", policy, failures, action));
		}
		List<RangerValiditySchedule> validitySchedules = policy.getValiditySchedules();
		List<RangerValiditySchedule> normalizedValiditySchedules = null;

		for (RangerValiditySchedule entry : validitySchedules) {
			RangerValidityScheduleValidator validator = new RangerValidityScheduleValidator(entry);

			RangerValiditySchedule normalizedValiditySchedule = validator.validate(failures);
			if (normalizedValiditySchedule == null) {
				valid = false;
				if (LOG.isDebugEnabled()) {
					LOG.debug("Invalid Validity-Schedule:[" + entry +"]");
				}
			} else {
				if (normalizedValiditySchedules == null) {
					normalizedValiditySchedules = new ArrayList<>();
				}
				normalizedValiditySchedules.add(normalizedValiditySchedule);
			}
		}
		if (valid && CollectionUtils.isNotEmpty(normalizedValiditySchedules)) {
			policy.setValiditySchedules(normalizedValiditySchedules);
		}
		if (LOG.isDebugEnabled()) {
			LOG.debug(String.format("<== RangerPolicyValidator.isValidValiditySchedule(%s, %s, %s): %s", policy, failures, action, valid));
		}
		return valid;
	}

	boolean isPolicyResourceUnique(RangerPolicy policy, final List<ValidationFailureDetails> failures, Action action) {
		
		if(LOG.isDebugEnabled()) {
			LOG.debug(String.format("==> RangerPolicyValidator.isPolicyResourceUnique(%s, %s, %s)", policy, failures, action));
		}

		boolean valid = true;

		RangerPolicyResourceSignature policySignature = _factory.createPolicyResourceSignature(policy);
		String signature = policySignature.getSignature();
		List<RangerPolicy> policies = getPoliciesForResourceSignature(policy.getService(), signature);
		if (CollectionUtils.isNotEmpty(policies)) {
			ValidationErrorCode error = ValidationErrorCode.POLICY_VALIDATION_ERR_DUPLICATE_POLICY_RESOURCE;
			RangerPolicy matchedPolicy = policies.iterator().next();
			// there shouldn't be a matching policy for create.  During update only match should be to itself
			if (action == Action.CREATE || (action == Action.UPDATE && !matchedPolicy.getId().equals(policy.getId()))) {
				failures.add(new ValidationFailureDetailsBuilder()
						.field("resources")
						.isSemanticallyIncorrect()
						.becauseOf(error.getMessage(matchedPolicy.getName(), policy.getService()))
						.errorCode(error.getErrorCode())
						.build());
				valid = false;
			}
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug(String.format("<== RangerPolicyValidator.isPolicyResourceUnique(%s, %s, %s): %s", policy, failures, action, valid));
		}
		return valid;
	}
	
	boolean isValidResourceNames(final RangerPolicy policy, final List<ValidationFailureDetails> failures, final RangerServiceDef serviceDef) {
		
		if(LOG.isDebugEnabled()) {
			LOG.debug(String.format("==> RangerPolicyValidator.isValidResourceNames(%s, %s, %s)", policy, failures, serviceDef));
		}

		boolean valid = true;
		convertPolicyResourceNamesToLower(policy);
		Set<String> policyResources = policy.getResources().keySet();

		RangerServiceDefHelper defHelper = new RangerServiceDefHelper(serviceDef);
		Set<List<RangerResourceDef>> hierarchies = defHelper.getResourceHierarchies(policy.getPolicyType()); // this can be empty but not null!
		if (hierarchies.isEmpty()) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("RangerPolicyValidator.isValidResourceNames: serviceDef does not have any resource hierarchies, possibly due to invalid service def!!");
			}
			ValidationErrorCode error = ValidationErrorCode.POLICY_VALIDATION_ERR_INVALID_RESOURCE_NO_COMPATIBLE_HIERARCHY;
			failures.add(new ValidationFailureDetailsBuilder()
					.field("service def resource hierarchies")
					.subField("incompatible")
					.isSemanticallyIncorrect()
					.becauseOf(error.getMessage(serviceDef.getName(), " does not have any resource hierarchies"))
					.errorCode(error.getErrorCode())
					.build());
			valid = false;
		} else {
			/*
			 * A policy is for a single hierarchy however, it doesn't specify which one.  So we have to guess which hierarchy(s) it possibly be for.  First, see if the policy could be for
			 * any of the known hierarchies?  A candidate hierarchy is one whose resource levels are a superset of those in the policy.
			 * Why?  What we want to catch at this stage is policies that straddles multiple hierarchies, e.g. db, udf and column for a hive policy.
			 * This has the side effect of catch spurious levels specified on the policy, e.g. having a level "blah" on a hive policy.
			 */
			Set<List<RangerResourceDef>> candidateHierarchies = filterHierarchies_hierarchyHasAllPolicyResources(policyResources, hierarchies, defHelper);
			if (candidateHierarchies.isEmpty()) {
				if (LOG.isDebugEnabled()) {
					LOG.debug(String.format("No compatible resource hierarchies found: resource[%s], service-def[%s], valid-resource-hierarchies[%s]",
							policyResources.toString(), serviceDef.getName(), toStringHierarchies_all(hierarchies, defHelper)));
				}
				ValidationErrorCode error;
				if (hierarchies.size() == 1) { // we can give a simpler message for single hierarchy service-defs which is the majority of cases
					error = ValidationErrorCode.POLICY_VALIDATION_ERR_INVALID_RESOURCE_NO_COMPATIBLE_HIERARCHY_SINGLE;
				} else {
					error = ValidationErrorCode.POLICY_VALIDATION_ERR_INVALID_RESOURCE_NO_COMPATIBLE_HIERARCHY;
				}
				failures.add(new ValidationFailureDetailsBuilder()
					.field("policy resources")
					.subField("incompatible")
					.isSemanticallyIncorrect()
					.becauseOf(error.getMessage(serviceDef.getName(), toStringHierarchies_all(hierarchies, defHelper)))
					.errorCode(error.getErrorCode())
					.build());
				valid = false;
			} else {
				if (LOG.isDebugEnabled()) {
					LOG.debug("isValidResourceNames: Found [" + candidateHierarchies.size() + "] compatible hierarchies: " + toStringHierarchies_all(candidateHierarchies, defHelper));
				}
				/*
				 * Among the candidate hierarchies there should be at least one for which policy specifies all of the mandatory resources.  Note that there could be multiple
				 * hierarchies that meet that criteria, e.g. a hive policy that specified only DB.  It is not clear if it belongs to DB->UDF or DB->TBL->COL hierarchy.
				 * However, if both UDF and TBL were required then we can detect that policy does not specify mandatory levels for any of the candidate hierarchies.
				 */
				Set<List<RangerResourceDef>> validHierarchies = filterHierarchies_mandatoryResourcesSpecifiedInPolicy(policyResources, candidateHierarchies, defHelper);
				if (validHierarchies.isEmpty()) {
					ValidationErrorCode error;
					if (candidateHierarchies.size() == 1) { // we can provide better message if there is a single candidate hierarchy
						error = ValidationErrorCode.POLICY_VALIDATION_ERR_INVALID_RESOURCE_MISSING_MANDATORY_SINGLE;
					} else {
						error = ValidationErrorCode.POLICY_VALIDATION_ERR_INVALID_RESOURCE_MISSING_MANDATORY;
					}
					failures.add(new ValidationFailureDetailsBuilder()
						.field("policy resources")
						.subField("missing mandatory")
						.isSemanticallyIncorrect()
						.becauseOf(error.getMessage(serviceDef.getName(), toStringHierarchies_mandatory(candidateHierarchies, defHelper)))
						.errorCode(error.getErrorCode())
						.build());
					valid = false;
				} else {
					if (LOG.isDebugEnabled()) {
						LOG.debug("isValidResourceNames: Found hierarchies with all mandatory fields specified: " + toStringHierarchies_mandatory(validHierarchies, defHelper));
					}
				}
			}
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug(String.format("<== RangerPolicyValidator.isValidResourceNames(%s, %s, %s): %s", policy, failures, serviceDef, valid));
		}
		return valid;
	}
	
	/**
	 * String representation of mandatory resources of all the hierarchies suitable of showing to user.  Mandatory resources within a hierarchy are not ordered per the hierarchy.
	 * @param hierarchies
	 * @param defHelper
	 * @return
	 */
	String toStringHierarchies_mandatory(Set<List<RangerResourceDef>> hierarchies, RangerServiceDefHelper defHelper) {

		// helper function skipping sanity checks of getting null arguments passed
		StringBuilder builder = new StringBuilder();
		for (List<RangerResourceDef> aHierarchy : hierarchies) {
			builder.append(defHelper.getMandatoryResourceNames(aHierarchy));
			builder.append(" ");
		}
		return builder.toString();
	}
	
	/**
	 * String representation of all resources of all hierarchies.  Resources within a hierarchy are ordered per the hierarchy.
	 * @param hierarchies
	 * @param defHelper
	 * @return
	 */
	String toStringHierarchies_all(Set<List<RangerResourceDef>> hierarchies, RangerServiceDefHelper defHelper) {

		// helper function skipping sanity checks of getting null arguments passed
		StringBuilder builder = new StringBuilder();
		for (List<RangerResourceDef> aHierarchy : hierarchies) {
			builder.append(defHelper.getAllResourceNamesOrdered(aHierarchy));
			builder.append(" ");
		}
		return builder.toString();
	}
	/**
	 * Returns the subset of all hierarchies that are a superset of the policy's resources.
	 * @param policyResources
	 * @param hierarchies
	 * @return
	 */
	Set<List<RangerResourceDef>> filterHierarchies_hierarchyHasAllPolicyResources(Set<String> policyResources, Set<List<RangerResourceDef>> hierarchies, RangerServiceDefHelper defHelper) {
		
		// helper function skipping sanity checks of getting null arguments passed
		Set<List<RangerResourceDef>> result = new HashSet<List<RangerResourceDef>>(hierarchies.size());
		for (List<RangerResourceDef> aHierarchy : hierarchies) {
			if (defHelper.hierarchyHasAllResources(aHierarchy, policyResources)) {
				result.add(aHierarchy);
			}
		}
		return result;
	}
	
	/**
	 * Returns the subset of hierarchies all of whose mandatory resources were found in policy's resource set.  candidate hierarchies are expected to have passed
	 * <code>filterHierarchies_hierarchyHasAllPolicyResources</code> check first.
	 * @param policyResources
	 * @param hierarchies
	 * @param defHelper
	 * @return
	 */
	Set<List<RangerResourceDef>> filterHierarchies_mandatoryResourcesSpecifiedInPolicy(Set<String> policyResources, Set<List<RangerResourceDef>> hierarchies, RangerServiceDefHelper defHelper) {
		
		// helper function skipping sanity checks of getting null arguments passed
		Set<List<RangerResourceDef>> result = new HashSet<List<RangerResourceDef>>(hierarchies.size());
		for (List<RangerResourceDef> aHierarchy : hierarchies) {
			Set<String> mandatoryResources = defHelper.getMandatoryResourceNames(aHierarchy);
			if (policyResources.containsAll(mandatoryResources)) {
				result.add(aHierarchy);
			}
		}
		return result;
	}
	
	boolean isValidResourceFlags(final Map<String, RangerPolicyResource> inputPolicyResources, final List<ValidationFailureDetails> failures,
			final List<RangerResourceDef> resourceDefs, final String serviceDefName, final String policyName, boolean isAdmin) {
		if(LOG.isDebugEnabled()) {
			LOG.debug(String.format("==> RangerPolicyValidator.isValidResourceFlags(%s, %s, %s, %s, %s, %s)", inputPolicyResources, failures, resourceDefs, serviceDefName, policyName, isAdmin));
		}

		boolean valid = true;
		if (resourceDefs == null) {
			LOG.debug("isValidResourceFlags: service Def is null");
		} else {
			Map<String, RangerPolicyResource> policyResources = getPolicyResourceWithLowerCaseKeys(inputPolicyResources);
			for (RangerResourceDef resourceDef : resourceDefs) {
				if (resourceDef == null) {
					ValidationErrorCode error = ValidationErrorCode.POLICY_VALIDATION_ERR_NULL_RESOURCE_DEF;
					failures.add(new ValidationFailureDetailsBuilder()
						.field("resource-def")
						.isAnInternalError()
						.becauseOf(error.getMessage(serviceDefName))
						.errorCode(error.getErrorCode())
						.build());
					valid = false;
				} else if (StringUtils.isBlank(resourceDef.getName())) {
					ValidationErrorCode error = ValidationErrorCode.POLICY_VALIDATION_ERR_MISSING_RESOURCE_DEF_NAME;
					failures.add(new ValidationFailureDetailsBuilder()
						.field("resource-def-name")
						.isAnInternalError()
						.becauseOf(error.getMessage(serviceDefName))
						.errorCode(error.getErrorCode())
						.build());
					valid = false;
				} else {
					String resourceName = resourceDef.getName().toLowerCase();
					RangerPolicyResource policyResource = policyResources.get(resourceName);
					if (policyResource == null) {
						if (LOG.isDebugEnabled()) {
							LOG.debug("a policy-resource object for resource[" + resourceName + "] on policy [" + policyName + "] was null");
						}
					} else {
						boolean excludesSupported = Boolean.TRUE.equals(resourceDef.getExcludesSupported()); // could be null
						boolean policyResourceIsExcludes = Boolean.TRUE.equals(policyResource.getIsExcludes()); // could be null
						if (policyResourceIsExcludes && !excludesSupported) {
							ValidationErrorCode error = ValidationErrorCode.POLICY_VALIDATION_ERR_EXCLUDES_NOT_SUPPORTED;
							failures.add(new ValidationFailureDetailsBuilder()
								.field("isExcludes")
								.subField(resourceName)
								.isSemanticallyIncorrect()
								.becauseOf(error.getMessage(resourceName))
								.errorCode(error.getErrorCode())
								.build());
							valid = false;
						}
						if (policyResourceIsExcludes && !isAdmin) {
							ValidationErrorCode error = ValidationErrorCode.POLICY_VALIDATION_ERR_EXCLUDES_REQUIRES_ADMIN;
							failures.add(new ValidationFailureDetailsBuilder()
								.field("isExcludes")
								.subField("isAdmin")
								.isSemanticallyIncorrect()
								.becauseOf(error.getMessage())
								.errorCode(error.getErrorCode())
								.build());
							valid = false;
						}
						boolean recursiveSupported = Boolean.TRUE.equals(resourceDef.getRecursiveSupported());
						boolean policyIsRecursive = Boolean.TRUE.equals(policyResource.getIsRecursive());
						if (policyIsRecursive && !recursiveSupported) {
							ValidationErrorCode error = ValidationErrorCode.POLICY_VALIDATION_ERR_RECURSIVE_NOT_SUPPORTED;
							failures.add(new ValidationFailureDetailsBuilder()
								.field("isRecursive")
								.subField(resourceName)
								.isSemanticallyIncorrect()
								.becauseOf(error.getMessage(resourceName))
								.errorCode(error.getErrorCode())
								.build());
							valid = false;
						}
					}
				}
			}
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug(String.format("<== RangerPolicyValidator.isValidResourceFlags(%s, %s, %s, %s, %s, %s): %s", inputPolicyResources, failures, resourceDefs, serviceDefName, policyName, isAdmin, valid));
		}
		return valid;
	}

	boolean isValidResourceValues(Map<String, RangerPolicyResource> resourceMap, List<ValidationFailureDetails> failures, RangerServiceDef serviceDef) {
		if(LOG.isDebugEnabled()) {
			LOG.debug(String.format("==> RangerPolicyValidator.isValidResourceValues(%s, %s, %s)", resourceMap, failures, serviceDef));
		}

		boolean valid = true;
		Map<String, String> validationRegExMap = getValidationRegExes(serviceDef);
		for (Map.Entry<String, RangerPolicyResource> entry : resourceMap.entrySet()) {
			String name = entry.getKey();
			RangerPolicyResource policyResource = entry.getValue();
			if(policyResource != null) {
				if(CollectionUtils.isNotEmpty(policyResource.getValues())) {
					Set<String> resources = new HashSet<>(policyResource.getValues());
					for (String aValue : resources) {
						if (StringUtils.isBlank(aValue)) {
							policyResource.getValues().remove(aValue);
						}
					}
				}

				if(CollectionUtils.isEmpty(policyResource.getValues())){
					ValidationErrorCode error = ValidationErrorCode.POLICY_VALIDATION_ERR_MISSING_RESOURCE_LIST;
					if(LOG.isDebugEnabled()) {
						LOG.debug(String.format("Resource list was empty or contains null: value[%s], resource-name[%s], service-def-name[%s]", policyResource.getValues(), name, serviceDef.getName()));
					}
					failures.add(new ValidationFailureDetailsBuilder()
						.field("resource-values")
						.subField(name)
						.isMissing()
						.becauseOf(error.getMessage(name))
						.errorCode(error.getErrorCode())
						.build());
					valid=false;
				}

				if (validationRegExMap.containsKey(name) && CollectionUtils.isNotEmpty(policyResource.getValues())) {
					String regEx = validationRegExMap.get(name);
					for (String aValue : policyResource.getValues()) {
						if (!aValue.matches(regEx)) {
							if (LOG.isDebugEnabled()) {
								LOG.debug(String.format("Resource failed regex check: value[%s], resource-name[%s], regEx[%s], service-def-name[%s]", aValue, name, regEx, serviceDef.getName()));
							}
							ValidationErrorCode error = ValidationErrorCode.POLICY_VALIDATION_ERR_INVALID_RESOURCE_VALUE_REGEX;
							failures.add(new ValidationFailureDetailsBuilder()
								.field("resource-values")
								.subField(name)
								.isSemanticallyIncorrect()
								.becauseOf(error.getMessage(aValue, name))
								.errorCode(error.getErrorCode())
								.build());
							valid = false;
						}
					}
				}
			}
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug(String.format("<== RangerPolicyValidator.isValidResourceValues(%s, %s, %s): %s", resourceMap, failures, serviceDef, valid));
		}
		return valid;
	}

	boolean isValidPolicyItems(List<RangerPolicyItem> policyItems, List<ValidationFailureDetails> failures, RangerServiceDef serviceDef) {
		if(LOG.isDebugEnabled()) {
			LOG.debug(String.format("==> RangerPolicyValidator.isValid(%s, %s, %s)", policyItems, failures, serviceDef));
		}

		boolean valid = true;
		if (CollectionUtils.isEmpty(policyItems)) {
			LOG.debug("policy items collection was null/empty");
		} else {
			for (RangerPolicyItem policyItem : policyItems) {
				if (policyItem == null) {
					ValidationErrorCode error = ValidationErrorCode.POLICY_VALIDATION_ERR_NULL_POLICY_ITEM;
					failures.add(new ValidationFailureDetailsBuilder()
						.field("policy item")
						.isMissing()
						.becauseOf(error.getMessage())
						.errorCode(error.getErrorCode())
						.build());
					valid = false;
				} else {
					// we want to go through all elements even though one may be bad so all failures are captured
					valid = isValidPolicyItem(policyItem, failures, serviceDef) && valid;
				}
			}
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug(String.format("<== RangerPolicyValidator.isValid(%s, %s, %s): %s", policyItems, failures, serviceDef, valid));
		}
		return valid;
	}

	boolean isValidPolicyItem(RangerPolicyItem policyItem, List<ValidationFailureDetails> failures, RangerServiceDef serviceDef) {
		if(LOG.isDebugEnabled()) {
			LOG.debug(String.format("==> RangerPolicyValidator.isValid(%s, %s, %s)", policyItem, failures, serviceDef));
		}
		
		boolean valid = true;
		if (policyItem == null) {
			LOG.debug("policy item was null!");
		} else {
			// access items collection can't be empty (unless delegated admin is true) and should be otherwise valid
			if (CollectionUtils.isEmpty(policyItem.getAccesses())) {
				if (!Boolean.TRUE.equals(policyItem.getDelegateAdmin())) {
					ValidationErrorCode error = ValidationErrorCode.POLICY_VALIDATION_ERR_MISSING_FIELD;
					failures.add(new ValidationFailureDetailsBuilder()
						.field("policy item accesses")
						.isMissing()
						.becauseOf(error.getMessage("policy item accesses"))
						.errorCode(error.getErrorCode())
						.build());
					valid = false;
				} else {
					LOG.debug("policy item collection was null but delegated admin is true. Ok");
				}
			} else {
				valid = isValidItemAccesses(policyItem.getAccesses(), failures, serviceDef) && valid;
			}
			// both users and user-groups collections can't be empty
			if (CollectionUtils.isEmpty(policyItem.getUsers()) && CollectionUtils.isEmpty(policyItem.getGroups()) && CollectionUtils.isEmpty(policyItem.getRoles())) {
				ValidationErrorCode error = ValidationErrorCode.POLICY_VALIDATION_ERR_MISSING_USER_AND_GROUPS;
				failures.add(new ValidationFailureDetailsBuilder()
					.field("policy item users/user-groups/roles")
					.isMissing()
					.becauseOf(error.getMessage())
					.errorCode(error.getErrorCode())
					.build());
				valid = false;
			}
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug(String.format("<== RangerPolicyValidator.isValid(%s, %s, %s): %s", policyItem, failures, serviceDef, valid));
		}
		return valid;
	}

	boolean isValidItemAccesses(List<RangerPolicyItemAccess> accesses, List<ValidationFailureDetails> failures, RangerServiceDef serviceDef) {
		if(LOG.isDebugEnabled()) {
			LOG.debug(String.format("==> RangerPolicyValidator.isValid(%s, %s, %s)", accesses, failures, serviceDef));
		}
		
		boolean valid = true;
		if (CollectionUtils.isEmpty(accesses)) {
			LOG.debug("policy item accesses collection was null/empty!");
		} else {
			Set<String> accessTypes = getAccessTypes(serviceDef);
			for (RangerPolicyItemAccess access : accesses) {
				if (access == null) {
					ValidationErrorCode error = ValidationErrorCode.POLICY_VALIDATION_ERR_NULL_POLICY_ITEM_ACCESS;
					failures.add(new ValidationFailureDetailsBuilder()
						.field("policy item access")
						.isMissing()
						.becauseOf(error.getMessage())
						.errorCode(error.getErrorCode())
						.build());
					valid = false;
				} else {
					// we want to go through all elements even though one may be bad so all failures are captured
					valid = isValidPolicyItemAccess(access, failures, accessTypes) && valid;
				}
			}
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug(String.format("<== RangerPolicyValidator.isValid(%s, %s, %s): %b", accesses, failures, serviceDef, valid));
		}
		return valid;
	}

	boolean isValidPolicyItemAccess(RangerPolicyItemAccess access, List<ValidationFailureDetails> failures, Set<String> accessTypes) {
		if(LOG.isDebugEnabled()) {
			LOG.debug(String.format("==> RangerPolicyValidator.isValidPolicyItemAccess(%s, %s, %s)", access, failures, accessTypes));
		}
		
		boolean valid = true;
		if (CollectionUtils.isEmpty(accessTypes)) { // caller should firewall this argument!
			LOG.debug("isValidPolicyItemAccess: accessTypes was null!");
		} else if (access == null) {
			LOG.debug("isValidPolicyItemAccess: policy item access was null!");
		} else {
			String accessType = access.getType();
			if (StringUtils.isBlank(accessType)) {
				ValidationErrorCode error = ValidationErrorCode.POLICY_VALIDATION_ERR_MISSING_FIELD;
				failures.add(new ValidationFailureDetailsBuilder()
					.field("policy item access type")
					.isMissing()
					.becauseOf(error.getMessage("policy item access type"))
					.errorCode(error.getErrorCode())
					.build());
				valid = false;
			} else {
				String matchedAccessType = getMatchedAccessType(accessType, accessTypes);
				if (StringUtils.isEmpty(matchedAccessType)) {
					ValidationErrorCode error = ValidationErrorCode.POLICY_VALIDATION_ERR_POLICY_ITEM_ACCESS_TYPE_INVALID;
					failures.add(new ValidationFailureDetailsBuilder()
							.field("policy item access type")
							.isSemanticallyIncorrect()
							.becauseOf(error.getMessage(accessType, accessTypes))
							.errorCode(error.getErrorCode())
							.build());
					valid = false;
				} else {
					access.setType(matchedAccessType);
				}
			}
			Boolean isAllowed = access.getIsAllowed();
			// it can be null (which is treated as allowed) but not false
			if (isAllowed != null && isAllowed == false) {
				ValidationErrorCode error = ValidationErrorCode.POLICY_VALIDATION_ERR_POLICY_ITEM_ACCESS_TYPE_DENY;
				failures.add(new ValidationFailureDetailsBuilder()
					.field("policy item access type allowed")
					.isSemanticallyIncorrect()
					.becauseOf(error.getMessage())
					.errorCode(error.getErrorCode())
					.build());
				valid = false;
			}
		}
		
		if(LOG.isDebugEnabled()) {
			LOG.debug(String.format("<== RangerPolicyValidator.isValidPolicyItemAccess(%s, %s, %s): %s", access, failures, accessTypes, valid));
		}
		return valid;
	}

	String getMatchedAccessType(String accessType, Set<String> validAccessTypes) {
		String ret = null;
		for (String validType : validAccessTypes) {
			if (StringUtils.equalsIgnoreCase(accessType, validType)) {
				ret = validType;
				break;
			}
		}
		return ret;
	}
}
