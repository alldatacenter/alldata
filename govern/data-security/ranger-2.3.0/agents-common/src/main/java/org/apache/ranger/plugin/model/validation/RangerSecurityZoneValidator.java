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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.plugin.errors.ValidationErrorCode;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerSecurityZone;
import org.apache.ranger.plugin.model.RangerService;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.model.RangerSecurityZone.RangerSecurityZoneService;
import org.apache.ranger.plugin.policyengine.RangerAccessResourceImpl;
import org.apache.ranger.plugin.policyengine.RangerResourceTrie;
import org.apache.ranger.plugin.policyresourcematcher.RangerDefaultPolicyResourceMatcher;
import org.apache.ranger.plugin.policyresourcematcher.RangerPolicyResourceMatcher;
import org.apache.ranger.plugin.store.EmbeddedServiceDefsUtil;
import org.apache.ranger.plugin.store.SecurityZoneStore;
import org.apache.ranger.plugin.store.ServiceStore;
import org.apache.ranger.plugin.util.SearchFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RangerSecurityZoneValidator extends RangerValidator {
    private static final Logger LOG = LoggerFactory.getLogger(RangerSecurityZoneValidator.class);

    private final SecurityZoneStore securityZoneStore;

    public RangerSecurityZoneValidator(ServiceStore store, SecurityZoneStore securityZoneStore) {
        super(store);
        this.securityZoneStore = securityZoneStore;
    }

    public void validate(RangerSecurityZone securityZone, Action action) throws Exception {
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("==> RangerPolicyValidator.validate(%s, %s)", securityZone, action));
        }

        List<ValidationFailureDetails> failures = new ArrayList<>();

        boolean valid = isValid(securityZone, action, failures);

        String message;
        try {
            if (!valid) {
                message = serializeFailures(failures);
                throw new Exception(message);
            }

        } finally {
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("<== RangerPolicyValidator.validate(%s, %s)", securityZone, action));
            }
        }
    }

    @Override
    boolean isValid(String name, Action action, List<ValidationFailureDetails> failures) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("==> RangerPolicyValidator.isValid(%s, %s, %s)", name, action, failures));
        }

        boolean ret = true;

        if (action != Action.DELETE) {
            ValidationErrorCode error = ValidationErrorCode.SECURITY_ZONE_VALIDATION_ERR_UNSUPPORTED_ACTION;

            failures.add(new ValidationFailureDetailsBuilder().isAnInternalError().becauseOf(error.getMessage()).errorCode(error.getErrorCode()).build());
            ret = false;
        } else {
            if (StringUtils.isEmpty(name)) {
                ValidationErrorCode error = ValidationErrorCode.SECURITY_ZONE_VALIDATION_ERR_MISSING_FIELD;

                failures.add(new ValidationFailureDetailsBuilder().becauseOf("security zone name was null/missing").field("name").isMissing().errorCode(error.getErrorCode()).becauseOf(error.getMessage("name")).build());
                ret = false;
            } else {
                if (getSecurityZone(name) == null) {
                    ValidationErrorCode error = ValidationErrorCode.SECURITY_ZONE_VALIDATION_ERR_INVALID_ZONE_ID;

                    failures.add(new ValidationFailureDetailsBuilder().becauseOf("security zone does not exist").field("name").errorCode(error.getErrorCode()).becauseOf(error.getMessage(name)).build());
                    ret = false;
                }
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("<== RangerPolicyValidator.isValid(%s, %s, %s) : %s", name, action, failures, ret));
        }

        return ret;
    }

    @Override
    boolean isValid(Long id, Action action, List<ValidationFailureDetails> failures) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("==> RangerPolicyValidator.isValid(%s, %s, %s)", id, action, failures));
        }

        boolean ret = true;

        if (action != Action.DELETE) {
            ValidationErrorCode error = ValidationErrorCode.SECURITY_ZONE_VALIDATION_ERR_UNSUPPORTED_ACTION;

            failures.add(new ValidationFailureDetailsBuilder().isAnInternalError().becauseOf(error.getMessage()).errorCode(error.getErrorCode()).build());
            ret = false;
        } else if (id == null) {
            ValidationErrorCode error = ValidationErrorCode.SECURITY_ZONE_VALIDATION_ERR_MISSING_FIELD;

            failures.add(new ValidationFailureDetailsBuilder().becauseOf("security zone id was null/missing").field("id").isMissing().errorCode(error.getErrorCode()).becauseOf(error.getMessage("id")).build());
            ret = false;
        } else if (getSecurityZone(id) == null) {
                ValidationErrorCode error = ValidationErrorCode.SECURITY_ZONE_VALIDATION_ERR_INVALID_ZONE_ID;

                failures.add(new ValidationFailureDetailsBuilder().becauseOf("security zone id does not exist").field("id").errorCode(error.getErrorCode()).becauseOf(error.getMessage(id)).build());
                ret = false;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("<== RangerPolicyValidator.isValid(%s, %s, %s) : %s", id, action, failures, ret));
        }

        return ret;
    }

    boolean isValid(RangerSecurityZone securityZone, Action action, List<ValidationFailureDetails> failures) {
        if(LOG.isDebugEnabled()) {
            LOG.debug(String.format("==> RangerPolicyValidator.isValid(%s, %s, %s)", securityZone, action, failures));
        }

        if (!(action == Action.CREATE || action == Action.UPDATE)) {
            throw new IllegalArgumentException("isValid(RangerPolicy, ...) is only supported for create/update");
        }

        boolean ret = true;

        RangerSecurityZone existingZone;
        final String zoneName = securityZone.getName();
        if (StringUtils.isEmpty(StringUtils.trim(zoneName))) {
            ValidationErrorCode error = ValidationErrorCode.SECURITY_ZONE_VALIDATION_ERR_MISSING_FIELD;

            failures.add(new ValidationFailureDetailsBuilder().becauseOf("security zone name was null/missing").field("name").isMissing().errorCode(error.getErrorCode()).becauseOf(error.getMessage("name")).build());
            ret = false;
        }

        if (action == Action.CREATE) {
            securityZone.setId(-1L);
            existingZone = getSecurityZone(zoneName);
            if (existingZone != null) {
                ValidationErrorCode error = ValidationErrorCode.SECURITY_ZONE_VALIDATION_ERR_ZONE_NAME_CONFLICT;

                failures.add(new ValidationFailureDetailsBuilder().becauseOf("security zone name exists").field("name").errorCode(error.getErrorCode()).becauseOf(error.getMessage(existingZone.getId())).build());
                ret = false;
            }
        } else {
            Long zoneId  = securityZone.getId();
            existingZone = getSecurityZone(zoneId);

            if (existingZone == null) {
                ValidationErrorCode error = ValidationErrorCode.SECURITY_ZONE_VALIDATION_ERR_INVALID_ZONE_ID;

                failures.add(new ValidationFailureDetailsBuilder().becauseOf("security zone with id does not exist").field("id").errorCode(error.getErrorCode()).becauseOf(error.getMessage(zoneId)).build());
                ret = false;
            } else if (StringUtils.isNotEmpty(StringUtils.trim(zoneName)) && !StringUtils.equals(zoneName, existingZone.getName())) {
                existingZone = getSecurityZone(zoneName);

                if (existingZone != null) {
                    if (!StringUtils.equals(existingZone.getName(), zoneName)) {
                        ValidationErrorCode error = ValidationErrorCode.SECURITY_ZONE_VALIDATION_ERR_ZONE_NAME_CONFLICT;

                        failures.add(new ValidationFailureDetailsBuilder().becauseOf("security zone name").field("name").errorCode(error.getErrorCode()).becauseOf(error.getMessage(existingZone.getId())).build());
                        ret = false;
                    }
                }
            }
        }

        ret = ret && validateWithinSecurityZone(securityZone, action, failures);

        ret = ret && validateAgainstAllSecurityZones(securityZone, action, failures);

        if(LOG.isDebugEnabled()) {
            LOG.debug(String.format("<== RangerPolicyValidator.isValid(%s, %s, %s) : %s", securityZone, action, failures, ret));
        }

        return ret;
    }

    private boolean validateWithinSecurityZone(RangerSecurityZone securityZone, Action action, List<ValidationFailureDetails> failures) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("==> RangerPolicyValidator.validateWithinSecurityZone(%s, %s, %s)", securityZone, action, failures));
        }

        boolean ret = true;

        // Validate each service for existence, not being tag-service and each resource-spec for validity
        if (MapUtils.isNotEmpty(securityZone.getServices())) {
            for (Map.Entry<String, RangerSecurityZone.RangerSecurityZoneService> serviceSpecification : securityZone.getServices().entrySet()) {
                String                                       serviceName         = serviceSpecification.getKey();
                RangerSecurityZone.RangerSecurityZoneService securityZoneService = serviceSpecification.getValue();

                ret = ret && validateSecurityZoneService(serviceName, securityZoneService, failures);
            }
        } else {
            ValidationErrorCode error = ValidationErrorCode.SECURITY_ZONE_VALIDATION_ERR_MISSING_SERVICES;

            failures.add(new ValidationFailureDetailsBuilder().becauseOf("security zone services").isMissing().field("services").errorCode(error.getErrorCode()).becauseOf(error.getMessage(securityZone.getName())).build());
            ret = false;
        }
        // both admin users and user-groups collections can't be empty
        if (CollectionUtils.isEmpty(securityZone.getAdminUsers()) && CollectionUtils.isEmpty(securityZone.getAdminUserGroups())) {
            ValidationErrorCode error = ValidationErrorCode.SECURITY_ZONE_VALIDATION_ERR_MISSING_USER_AND_GROUPS;

            failures.add(new ValidationFailureDetailsBuilder().field("security zone admin users/user-groups").isMissing().becauseOf(error.getMessage()).errorCode(error.getErrorCode()).build());
            ret = false;
        }
        // both audit users and user-groups collections can't be empty
        if (CollectionUtils.isEmpty(securityZone.getAuditUsers()) && CollectionUtils.isEmpty(securityZone.getAuditUserGroups())) {
            ValidationErrorCode error = ValidationErrorCode.SECURITY_ZONE_VALIDATION_ERR_MISSING_USER_AND_GROUPS;

            failures.add(new ValidationFailureDetailsBuilder().field("security zone audit users/user-groups").isMissing().becauseOf(error.getMessage()).errorCode(error.getErrorCode()).build());
            ret = false;
        }

        if (securityZone.getServices() != null) {
			for (Map.Entry<String, RangerSecurityZoneService> serviceResourceMapEntry : securityZone.getServices()
					.entrySet()) {
				if (serviceResourceMapEntry.getValue().getResources() != null) {
					for (Map<String, List<String>> resource : serviceResourceMapEntry.getValue().getResources()) {
						if (resource != null) {
							for (Map.Entry<String, List<String>> entry : resource.entrySet()) {
								if (CollectionUtils.isEmpty(entry.getValue())) {
									ValidationErrorCode error = ValidationErrorCode.SECURITY_ZONE_VALIDATION_ERR_MISSING_RESOURCES;
									failures.add(new ValidationFailureDetailsBuilder().field("security zone resources")
											.subField("resources").isMissing()
											.becauseOf(error.getMessage(serviceResourceMapEntry.getKey()))
											.errorCode(error.getErrorCode()).build());
									ret = false;
								}
							}
						}
					}
				}
			}
		}
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("<== RangerPolicyValidator.validateWithinSecurityZone(%s, %s, %s) : %s", securityZone, action, failures, ret));
        }
        return ret;
    }

    private boolean validateAgainstAllSecurityZones(RangerSecurityZone securityZone, Action action, List<ValidationFailureDetails> failures) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("==> RangerPolicyValidator.validateAgainstAllSecurityZones(%s, %s, %s)", securityZone, action, failures));
        }

        boolean ret = true;

        final String zoneName;

        if (securityZone.getId() != -1L) {
            RangerSecurityZone existingZone = getSecurityZone(securityZone.getId());
            zoneName = existingZone.getName();
        } else {
            zoneName = securityZone.getName();
        }

        for (Map.Entry<String, RangerSecurityZone.RangerSecurityZoneService> entry:  securityZone.getServices().entrySet()) {
            String                                       serviceName      = entry.getKey();
            RangerSecurityZone.RangerSecurityZoneService serviceResources = entry.getValue();

            if (CollectionUtils.isNotEmpty(serviceResources.getResources())) {
                SearchFilter             filter = new SearchFilter();
                List<RangerSecurityZone> zones  = null;

                filter.setParam(SearchFilter.SERVICE_NAME, serviceName);
                filter.setParam(SearchFilter.ZONE_NAME, zoneName);

                try {
                    zones = securityZoneStore.getSecurityZones(filter);
                } catch (Exception excp) {
                    LOG.error("Failed to get Security-Zones", excp);
                    ValidationErrorCode error = ValidationErrorCode.SECURITY_ZONE_VALIDATION_ERR_INTERNAL_ERROR;

                    failures.add(new ValidationFailureDetailsBuilder().becauseOf(error.getMessage(excp.getMessage())).errorCode(error.getErrorCode()).build());
                    ret = false;
                }

                if (CollectionUtils.isNotEmpty(zones)) {
                    RangerService    service    = getService(serviceName);
                    RangerServiceDef serviceDef = service != null ? getServiceDef(service.getType()) : null;

                    if (serviceDef == null) {
                        ValidationErrorCode error = ValidationErrorCode.SECURITY_ZONE_VALIDATION_ERR_INTERNAL_ERROR;

                        failures.add(new ValidationFailureDetailsBuilder().becauseOf(error.getMessage(serviceName)).errorCode(error.getErrorCode()).build());
                        ret = false;

                    } else {
                        zones.add(securityZone);
                        ret = ret && validateZoneServiceInAllZones(zones, serviceName, serviceDef, failures);
                    }
                }
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("<== RangerPolicyValidator.validateAgainstAllSecurityZones(%s, %s, %s) : %s", securityZone, action, failures, ret));
        }

        return ret;
    }

    private boolean validateZoneServiceInAllZones(List<RangerSecurityZone> zones, String serviceName, RangerServiceDef serviceDef, List<ValidationFailureDetails> failures) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("==> RangerPolicyValidator.validateZoneServiceInAllZones(%s, %s, %s, %s)", zones, serviceName, serviceDef, failures));
        }

        boolean ret = true;

        // For each zone, get list-of-resources corresponding to serviceName.
        //    For each list-of-resources:
        //       get one resource (this is a map of <String, List<String>>); convert it into map of <String, RangerPolicyResource>. excludes is always false, recursive true only for HDFS
        //       build a subclass of RangerPolicyResourceEvaluator with id of zone, zoneName as a member, and RangerDefaultResourceMatcher as matcher.
        //       add this to list-of-evaluators

        Map<String, List<RangerZoneResourceMatcher>> matchersForResourceDef = new HashMap<>();

        for (RangerSecurityZone zone : zones) {
            List<HashMap<String, List<String>>> resources = zone.getServices().get(serviceName).getResources();

            for (Map<String, List<String>> resource : resources) {
                Map<String, RangerPolicy.RangerPolicyResource> policyResources = new HashMap<>();

                for (Map.Entry<String, List<String>> entry : resource.entrySet()) {
                    String       resourceDefName = entry.getKey();
                    List<String> resourceValues  = entry.getValue();

                    RangerPolicy.RangerPolicyResource policyResource = new RangerPolicy.RangerPolicyResource();

                    policyResource.setIsExcludes(false);
                    policyResource.setIsRecursive(EmbeddedServiceDefsUtil.isRecursiveEnabled(serviceDef, resourceDefName));
                    policyResource.setValues(resourceValues);
                    policyResources.put(resourceDefName, policyResource);

                    if (matchersForResourceDef.get(resourceDefName) == null) {
                        matchersForResourceDef.put(resourceDefName, new ArrayList<>());
                    }
                }

                RangerZoneResourceMatcher matcher = new RangerZoneResourceMatcher(zone.getName(), policyResources, serviceDef);

                for (String resourceDefName : resource.keySet()) {
                    matchersForResourceDef.get(resourceDefName).add(matcher);
                }
            }
        }

        // Build a map of trie with list-of-evaluators with one entry corresponds to one resource-def if it exists in the list-of-resources

        Map<String, RangerResourceTrie<RangerZoneResourceMatcher>> trieMap = new HashMap<>();
        List<RangerServiceDef.RangerResourceDef> resourceDefs = serviceDef.getResources();

        for (Map.Entry<String, List<RangerZoneResourceMatcher>> entry : matchersForResourceDef.entrySet()) {
            String                             resourceDefName = entry.getKey();
            List<RangerZoneResourceMatcher>    matchers        = entry.getValue();
            RangerServiceDef.RangerResourceDef resourceDef     = null;

            for (RangerServiceDef.RangerResourceDef element : resourceDefs) {
                if (StringUtils.equals(element.getName(), resourceDefName)) {
                    resourceDef = element;
                    break;
                }
            }

            trieMap.put(entry.getKey(), new RangerResourceTrie<>(resourceDef, matchers));
        }

        // For each zone, get list-of-resources corresponding to serviceName
        //    For each list-of-resources:
        //       get one resource; for each level in the resource, run it through map of trie and get possible evaluators.
        //       check each evaluator to see if the resource-match actually happens. If yes then add the zone-evaluator to matching evaluators.
        //       flag error if there are more than one matching evaluators with different zone-ids.
        //

        RangerServiceDefHelper serviceDefHelper = new RangerServiceDefHelper(serviceDef, true);

        for (RangerSecurityZone zone : zones) {
            List<HashMap<String, List<String>>> resources = zone.getServices().get(serviceName).getResources();

            for (Map<String, List<String>> resource : resources) {

                Set<RangerZoneResourceMatcher>       smallestList     = null;

                List<String> resourceKeys = serviceDefHelper.getOrderedResourceNames(resource.keySet());

                for (String resourceDefName : resourceKeys) {
                    List<String> resourceValues = resource.get(resourceDefName);

                    RangerResourceTrie<RangerZoneResourceMatcher> trie = trieMap.get(resourceDefName);

                    Set<RangerZoneResourceMatcher> zoneMatchersForResource = trie.getEvaluatorsForResource(resourceValues);
                    Set<RangerZoneResourceMatcher> inheritedZoneMatchers = trie.getInheritedEvaluators();

                    if (LOG.isDebugEnabled()) {
                        LOG.debug("ResourceDefName:[" + resourceDefName + "], values:[" + resourceValues + "], matched-zones:[" + zoneMatchersForResource + "], inherited-zones:[" + inheritedZoneMatchers + "]");
                    }

                    if (smallestList != null) {
                        if (CollectionUtils.isEmpty(inheritedZoneMatchers) && CollectionUtils.isEmpty(zoneMatchersForResource)) {
                            smallestList = null;
                        } else if (CollectionUtils.isEmpty(inheritedZoneMatchers)) {
                            smallestList.retainAll(zoneMatchersForResource);
                        } else if (CollectionUtils.isEmpty(zoneMatchersForResource)) {
                            smallestList.retainAll(inheritedZoneMatchers);
                        } else {
                            Set<RangerZoneResourceMatcher> smaller, bigger;
                            if (zoneMatchersForResource.size() < inheritedZoneMatchers.size()) {
                                smaller = zoneMatchersForResource;
                                bigger = inheritedZoneMatchers;
                            } else {
                                smaller = inheritedZoneMatchers;
                                bigger = zoneMatchersForResource;
                            }
                            Set<RangerZoneResourceMatcher> tmp = new HashSet<>();
                            if (smallestList.size() < smaller.size()) {
                                smallestList.stream().filter(smaller::contains).forEach(tmp::add);
                                smallestList.stream().filter(bigger::contains).forEach(tmp::add);
                            } else {
                                smaller.stream().filter(smallestList::contains).forEach(tmp::add);
                                if (smallestList.size() < bigger.size()) {
                                    smallestList.stream().filter(bigger::contains).forEach(tmp::add);
                                } else {
                                    bigger.stream().filter(smallestList::contains).forEach(tmp::add);
                                }
                            }
                            smallestList = tmp;
                        }
                    } else {
                        if (CollectionUtils.isEmpty(inheritedZoneMatchers) || CollectionUtils.isEmpty(zoneMatchersForResource)) {
                            Set<RangerZoneResourceMatcher> tmp = CollectionUtils.isEmpty(inheritedZoneMatchers) ? zoneMatchersForResource : inheritedZoneMatchers;
                            smallestList = resourceKeys.size() == 1 || CollectionUtils.isEmpty(tmp) ? tmp : new HashSet<>(tmp);
                        } else {
                            smallestList = new HashSet<>(zoneMatchersForResource);
                            smallestList.addAll(inheritedZoneMatchers);
                        }
                    }
                }

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Resource:[" + resource +"], matched-zones:[" + smallestList +"]");
                }

                if (CollectionUtils.isEmpty(smallestList) || smallestList.size() == 1) {
                    continue;
                }

                final Set<RangerZoneResourceMatcher> intersection = smallestList;

                RangerAccessResourceImpl accessResource = new RangerAccessResourceImpl();

                accessResource.setServiceDef(serviceDef);

                for (Map.Entry<String, List<String>> entry : resource.entrySet()) {
                    accessResource.setValue(entry.getKey(), entry.getValue());
                }

                Set<String> matchedZoneNames = new HashSet<>();

                for (RangerZoneResourceMatcher zoneMatcher : intersection) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Trying to match resource:[" + accessResource +"] using zoneMatcher:[" + zoneMatcher + "]");
                    }
                    // These are potential matches. Try to really match them
                    if (zoneMatcher.getPolicyResourceMatcher().isMatch(accessResource, RangerPolicyResourceMatcher.MatchScope.ANY, null)) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Matched resource:[" + accessResource +"] using zoneMatcher:[" + zoneMatcher + "]");
                        }
                        // Actual match happened
                        matchedZoneNames.add(zoneMatcher.getSecurityZoneName());
                    } else {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Did not match resource:[" + accessResource +"] using zoneMatcher:[" + zoneMatcher + "]");
                        }
                    }
                }
                LOG.info("The following zone-names matched resource:[" + resource + "]: " + matchedZoneNames);

                if (matchedZoneNames.size() > 1) {
                    ValidationErrorCode error = ValidationErrorCode.SECURITY_ZONE_VALIDATION_ERR_ZONE_RESOURCE_CONFLICT;

                    failures.add(new ValidationFailureDetailsBuilder().becauseOf(error.getMessage(matchedZoneNames, resource)).errorCode(error.getErrorCode()).build());
                    ret = false;
                    break;
                }
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("<== RangerPolicyValidator.validateZoneServiceInAllZones(%s, %s, %s, %s) : %s", zones, serviceName, serviceDef, failures, ret));
        }
        return ret;
    }

    private boolean validateSecurityZoneService(String serviceName, RangerSecurityZone.RangerSecurityZoneService securityZoneService, List<ValidationFailureDetails> failures) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("==> RangerPolicyValidator.validateSecurityZoneService(%s, %s, %s)", serviceName, securityZoneService, failures));
        }

        boolean ret = true;

        // Verify service with serviceName exists - get the service-type
        RangerService service = getService(serviceName);

        if (service == null) {
            ValidationErrorCode error = ValidationErrorCode.SECURITY_ZONE_VALIDATION_ERR_INVALID_SERVICE_NAME;

            failures.add(new ValidationFailureDetailsBuilder().field("security zone resource service-name").becauseOf(error.getMessage(serviceName)).errorCode(error.getErrorCode()).build());
            ret = false;
        } else {
            RangerServiceDef serviceDef = getServiceDef(service.getType());

            if (serviceDef == null) {
                ValidationErrorCode error = ValidationErrorCode.SECURITY_ZONE_VALIDATION_ERR_INVALID_SERVICE_TYPE;
                failures.add(new ValidationFailureDetailsBuilder().field("security zone resource service-type").becauseOf(error.getMessage(service.getType())).errorCode(error.getErrorCode()).build());
                ret = false;
            } else {
                String serviceType = serviceDef.getName();

                if (StringUtils.equals(serviceType, EmbeddedServiceDefsUtil.EMBEDDED_SERVICEDEF_TAG_NAME)) {
                    if (CollectionUtils.isNotEmpty(securityZoneService.getResources())) {
                        ValidationErrorCode error = ValidationErrorCode.SECURITY_ZONE_VALIDATION_ERR_UNEXPECTED_RESOURCES;
                        failures.add(new ValidationFailureDetailsBuilder().field("security zone resources").becauseOf(error.getMessage(serviceName)).errorCode(error.getErrorCode()).build());
                        ret = false;
                    }
                } else {
                    if (CollectionUtils.isEmpty(securityZoneService.getResources())) {
                        ValidationErrorCode error = ValidationErrorCode.SECURITY_ZONE_VALIDATION_ERR_MISSING_RESOURCES;
                        failures.add(new ValidationFailureDetailsBuilder().field("security zone resources").isMissing().becauseOf(error.getMessage(serviceName)).errorCode(error.getErrorCode()).build());
                        ret = false;
                    } else {
                        // For each resource-spec, verify that it forms valid hierarchy for some policy-type
                        for (Map<String, List<String>> resource : securityZoneService.getResources()) {
                            Set<String> resourceDefNames = resource.keySet();
                            RangerServiceDefHelper serviceDefHelper = new RangerServiceDefHelper(serviceDef);
                            boolean isValidHierarchy = false;

                            for (int policyType : RangerPolicy.POLICY_TYPES) {
                                Set<List<RangerServiceDef.RangerResourceDef>> resourceHierarchies = serviceDefHelper.getResourceHierarchies(policyType, resourceDefNames);

                                if (LOG.isDebugEnabled()) {
                                    LOG.debug("Size of resourceHierarchies for resourceDefNames:[" + resourceDefNames + ", policyType=" + policyType + "] = " + resourceHierarchies.size());
                                }

                                for (List<RangerServiceDef.RangerResourceDef> resourceHierarchy : resourceHierarchies) {

                                    if (RangerDefaultPolicyResourceMatcher.isHierarchyValidForResources(resourceHierarchy, resource)) {
                                        isValidHierarchy = true;
                                        break;
                                    } else {
                                        LOG.info("gaps found in resource, skipping hierarchy:[" + resourceHierarchies + "]");
                                    }
                                }
                            }

                            if (!isValidHierarchy) {
                                ValidationErrorCode error = ValidationErrorCode.SECURITY_ZONE_VALIDATION_ERR_INVALID_RESOURCE_HIERARCHY;

                                failures.add(new ValidationFailureDetailsBuilder().field("security zone resource hierarchy").becauseOf(error.getMessage(serviceName, resourceDefNames)).errorCode(error.getErrorCode()).build());
                                ret = false;
                            }

                        /*
                         * Ignore this check. It should be possible to have all wildcard resource in a zone if zone-admin so desires
                         *
                        boolean isValidResourceSpec = isAnyNonWildcardResource(resource, failures);

                        if (!isValidResourceSpec) {
                            ValidationErrorCode error = ValidationErrorCode.SECURITY_ZONE_VALIDATION_ERR_ALL_WILDCARD_RESOURCE_VALUES;

                            failures.add(new ValidationFailureDetailsBuilder().field("security zone resource values").becauseOf(error.getMessage(serviceName)).errorCode(error.getErrorCode()).build());
                            ret = false;
                            LOG.warn("RangerPolicyValidator.validateSecurityZoneService() : All wildcard resource-values specified for service :[" + serviceName + "]");
                        }
                        */

                        }
                    }
                }
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("<== RangerPolicyValidator.validateSecurityZoneService(%s, %s, %s) : %s", serviceName, securityZoneService, failures, ret));
        }

        return ret;
    }

    /*
    private boolean isAnyNonWildcardResource(Map<String, List<String>> resource, List<ValidationFailureDetails> failures) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("==> RangerPolicyValidator.isAnyNonWildcardResource(%s, %s)", resource, failures));
        }

        boolean ret = false;

        for (Map.Entry<String, List<String>> resourceDefValue : resource.entrySet()) {
            boolean      wildCardResourceFound = false;
            List<String> resourceValues        = resourceDefValue.getValue();

            for (String resourceValue : resourceValues) {
                if (StringUtils.equals(resourceValue, RangerDefaultResourceMatcher.WILDCARD_ASTERISK)) {
                    wildCardResourceFound = true;
                    break;
                }
            }

            if (!wildCardResourceFound) {
                ret = true;
                break;
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("<== RangerPolicyValidator.isAnyNonWildcardResource(%s, %s) : %s", resource, failures, ret));
        }
        return ret;
    }
    */
}
