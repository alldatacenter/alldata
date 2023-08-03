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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.plugin.errors.ValidationErrorCode;
import org.apache.ranger.plugin.model.RangerService;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.store.EmbeddedServiceDefsUtil;
import org.apache.ranger.plugin.store.ServiceStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

public class RangerServiceValidator extends RangerValidator {
	private static final Logger LOG = LoggerFactory.getLogger(RangerServiceValidator.class);

	private static final Pattern SERVICE_NAME_VALIDATION_REGEX         = Pattern.compile("^[a-zA-Z0-9_-][a-zA-Z0-9_-]{0,254}", Pattern.CASE_INSENSITIVE);
	private static final Pattern LEGACY_SERVICE_NAME_VALIDATION_REGEX  = Pattern.compile("^[a-zA-Z0-9_-][a-zA-Z0-9\\s_-]{0,254}", Pattern.CASE_INSENSITIVE);
	private static final Pattern SERVICE_DISPLAY_NAME_VALIDATION_REGEX = Pattern.compile("^[a-zA-Z0-9_-][a-zA-Z0-9\\s_-]{0,254}", Pattern.CASE_INSENSITIVE);

	public RangerServiceValidator(ServiceStore store) {
		super(store);
	}

	public void validate(RangerService service, Action action) throws Exception {
		if(LOG.isDebugEnabled()) {
			LOG.debug(String.format("==> RangerServiceValidator.validate(%s, %s)", service, action));
		}
		List<ValidationFailureDetails> failures = new ArrayList<>();
		boolean valid = isValid(service, action, failures);
		String message = "";
		try {
			if (!valid) {
				message = serializeFailures(failures);
				throw new Exception(message);
			}
		} finally {
			if(LOG.isDebugEnabled()) {
				LOG.debug(String.format("<== RangerServiceValidator.validate(%s, %s): %s, reason[%s]", service, action, valid, message));
			}
		}
	}

	boolean isValid(Long id, Action action, List<ValidationFailureDetails> failures) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerServiceValidator.isValid(" + id + ")");
		}

		boolean valid = true;
		if (action != Action.DELETE) {
			ValidationErrorCode error = ValidationErrorCode.SERVICE_VALIDATION_ERR_UNSUPPORTED_ACTION;
			failures.add(new ValidationFailureDetailsBuilder()
					.isAnInternalError()
					.errorCode(error.getErrorCode())
					.becauseOf(error.getMessage(action))
					.build());
			valid = false;
		} else if (id == null) {
			ValidationErrorCode error = ValidationErrorCode.SERVICE_VALIDATION_ERR_MISSING_FIELD;
			failures.add(new ValidationFailureDetailsBuilder()
					.field("id")
					.isMissing()
					.errorCode(error.getErrorCode())
					.becauseOf(error.getMessage(id))
					.build());
			valid = false;
		} else if (getService(id) == null) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("No service found for id[" + id + "]! ok!");
			}
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerServiceValidator.isValid(" + id + "): " + valid);
		}
		return valid;
	}

	boolean isValid(RangerService service, Action action, List<ValidationFailureDetails> failures) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerServiceValidator.isValid(" + service + ")");
		}
		if (!(action == Action.CREATE || action == Action.UPDATE)) {
			throw new IllegalArgumentException("isValid(RangerService, ...) is only supported for CREATE/UPDATE");
		}
		boolean valid = true;
		if (service == null) {
			ValidationErrorCode error = ValidationErrorCode.SERVICE_VALIDATION_ERR_NULL_SERVICE_OBJECT;
			failures.add(new ValidationFailureDetailsBuilder()
					.field("service")
					.isMissing()
					.errorCode(error.getErrorCode())
					.becauseOf(error.getMessage())
					.build());
			valid = false;
		} else {
			Long id = service.getId();
			if (action == Action.UPDATE) { // id is ignored for CREATE
				if (id == null) {
					ValidationErrorCode error = ValidationErrorCode.SERVICE_VALIDATION_ERR_EMPTY_SERVICE_ID;
					failures.add(new ValidationFailureDetailsBuilder()
							.field("id")
							.isMissing()
							.errorCode(error.getErrorCode())
							.becauseOf(error.getMessage())
							.build());
					valid = false;
				} else if (getService(id) == null) {
					ValidationErrorCode error = ValidationErrorCode.SERVICE_VALIDATION_ERR_INVALID_SERVICE_ID;
					failures.add(new ValidationFailureDetailsBuilder()
							.field("id")
							.isSemanticallyIncorrect()
							.errorCode(error.getErrorCode())
							.becauseOf(error.getMessage(id))
							.build());
					valid = false;
				}
			}
			String name = service.getName();
			boolean nameSpecified = StringUtils.isNotBlank(name);
			RangerServiceDef serviceDef = null;
			if (!nameSpecified) {
				ValidationErrorCode error = ValidationErrorCode.SERVICE_VALIDATION_ERR_INVALID_SERVICE_NAME;
				failures.add(new ValidationFailureDetailsBuilder()
						.field("name")
						.isMissing()
						.errorCode(error.getErrorCode())
						.becauseOf(error.getMessage(name))
						.build());
				valid = false;
			} else {
				Pattern serviceNameRegex = SERVICE_NAME_VALIDATION_REGEX;
				if (action == Action.UPDATE) {
					RangerService rangerService = getService(service.getId());
					if (rangerService != null && StringUtils.isNotBlank(rangerService.getName()) && rangerService.getName().contains(" ")) {
						//RANGER-2808 Support for space in services created with space in earlier version
						serviceNameRegex = LEGACY_SERVICE_NAME_VALIDATION_REGEX;
					}
				}

				if(!isValidString(serviceNameRegex, name)){
					ValidationErrorCode error = ValidationErrorCode.SERVICE_VALIDATION_ERR_SPECIAL_CHARACTERS_SERVICE_NAME;
					failures.add(new ValidationFailureDetailsBuilder()
							.field("name")
							.isSemanticallyIncorrect()
							.errorCode(error.getErrorCode())
							.becauseOf(error.getMessage(name))
							.build());
					valid = false;
				}else{
					RangerService otherService = getService(name);
					if (otherService != null && action == Action.CREATE) {
						ValidationErrorCode error = ValidationErrorCode.SERVICE_VALIDATION_ERR_SERVICE_NAME_CONFICT;
						failures.add(new ValidationFailureDetailsBuilder()
								.field("name")
								.isSemanticallyIncorrect()
								.errorCode(error.getErrorCode())
								.becauseOf(error.getMessage(name))
								.build());
						valid = false;
					} else if (otherService != null && otherService.getId() !=null && !otherService.getId().equals(id)) {
						ValidationErrorCode error = ValidationErrorCode.SERVICE_VALIDATION_ERR_ID_NAME_CONFLICT;
						failures.add(new ValidationFailureDetailsBuilder()
								.field("id/name")
								.isSemanticallyIncorrect()
								.errorCode(error.getErrorCode())
								.becauseOf(error.getMessage(name, otherService.getId()))
								.build());
						valid = false;
					}
				}
			}
			// Display name
			String displayName = service.getDisplayName();
			if(!isValidString(SERVICE_DISPLAY_NAME_VALIDATION_REGEX, displayName)){
				ValidationErrorCode error = ValidationErrorCode.SERVICE_VALIDATION_ERR_SPECIAL_CHARACTERS_SERVICE_DISPLAY_NAME;
				failures.add(new ValidationFailureDetailsBuilder()
						.field("displayName")
						.isSemanticallyIncorrect()
						.errorCode(error.getErrorCode())
						.becauseOf(error.getMessage(displayName))
						.build());
				valid = false;
			}else{
				RangerService otherService = getServiceByDisplayName(displayName);
				if (otherService != null && action == Action.CREATE) {
					ValidationErrorCode error = ValidationErrorCode.SERVICE_VALIDATION_ERR_SERVICE_DISPLAY_NAME_CONFICT;
					failures.add(new ValidationFailureDetailsBuilder()
							.field("displayName")
							.isSemanticallyIncorrect()
							.errorCode(error.getErrorCode())
							.becauseOf(error.getMessage(displayName, otherService.getName()))
							.build());
					valid = false;
				} else if (otherService != null && otherService.getId() !=null && !otherService.getId().equals(id)) {
					ValidationErrorCode error = ValidationErrorCode.SERVICE_VALIDATION_ERR_SERVICE_DISPLAY_NAME_CONFICT;
					failures.add(new ValidationFailureDetailsBuilder()
							.field("id/displayName")
							.isSemanticallyIncorrect()
							.errorCode(error.getErrorCode())
							.becauseOf(error.getMessage(displayName, otherService.getName()))
							.build());
					valid = false;
				}
			}
			String type = service.getType();
			boolean typeSpecified = StringUtils.isNotBlank(type);
			if (!typeSpecified) {
				ValidationErrorCode error = ValidationErrorCode.SERVICE_VALIDATION_ERR_MISSING_SERVICE_DEF;
				failures.add(new ValidationFailureDetailsBuilder()
						.field("type")
						.isMissing()
						.errorCode(error.getErrorCode())
						.becauseOf(error.getMessage(type))
						.build());
				valid = false;
			} else {
				serviceDef = getServiceDef(type);
				if (serviceDef == null) {
					ValidationErrorCode error = ValidationErrorCode.SERVICE_VALIDATION_ERR_INVALID_SERVICE_DEF;
					failures.add(new ValidationFailureDetailsBuilder()
							.field("type")
							.isSemanticallyIncorrect()
							.errorCode(error.getErrorCode())
							.becauseOf(error.getMessage(type))
						.build());
					valid = false;
				}
			}
			if (nameSpecified && serviceDef != null) {
				// check if required parameters were specified
				Set<String> reqiredParameters = getRequiredParameters(serviceDef);
				Set<String> inputParameters = getServiceConfigParameters(service);
				Set<String> missingParameters = Sets.difference(reqiredParameters, inputParameters);
				if (!missingParameters.isEmpty()) {
					ValidationErrorCode error = ValidationErrorCode.SERVICE_VALIDATION_ERR_REQUIRED_PARM_MISSING;
					failures.add(new ValidationFailureDetailsBuilder()
							.field("configuration")
							.subField(missingParameters.iterator().next()) // we return any one parameter!
							.isMissing()
							.errorCode(error.getErrorCode())
							.becauseOf(error.getMessage(missingParameters))
							.build());
					valid = false;
				}
			}
			String tagServiceName = service.getTagService();

			if (StringUtils.isNotBlank(tagServiceName) && StringUtils.equals(type, EmbeddedServiceDefsUtil.EMBEDDED_SERVICEDEF_TAG_NAME)) {
				failures.add(new ValidationFailureDetailsBuilder()
						.field("tag_service")
						.isSemanticallyIncorrect()
						.becauseOf("tag service cannot be part of any other service")
						.build());
				valid = false;
			}

			boolean needToEnsureServiceType = false;
			if (action == Action.UPDATE) {
				RangerService otherService = getService(name);
				String otherTagServiceName = otherService == null ? null : otherService.getTagService();

				if (StringUtils.isNotBlank(tagServiceName)) {
					if (!StringUtils.equals(tagServiceName, otherTagServiceName)) {
						needToEnsureServiceType = true;
					}
				}
			} else {    // action == Action.CREATE
				if (StringUtils.isNotBlank(tagServiceName)) {
					needToEnsureServiceType = true;
				}
			}
			if (needToEnsureServiceType) {
				RangerService maybeTagService = getService(tagServiceName);
				if (maybeTagService == null || !StringUtils.equals(maybeTagService.getType(), EmbeddedServiceDefsUtil.EMBEDDED_SERVICEDEF_TAG_NAME)) {
					failures.add(new ValidationFailureDetailsBuilder()
							.field("tag_service")
							.isSemanticallyIncorrect()
							.becauseOf("tag service name does not refer to existing tag service:" + tagServiceName)
							.build());
					valid = false;
				}
			}
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerServiceValidator.isValid(" + service + "): " + valid);
		}
		return valid;
	}

	public boolean isValidString(final Pattern pattern, final String name) {
		return pattern != null && StringUtils.isNotBlank(name) && pattern.matcher(name).matches();
	}
}
