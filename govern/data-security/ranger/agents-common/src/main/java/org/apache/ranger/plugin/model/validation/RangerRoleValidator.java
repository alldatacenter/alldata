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

import org.apache.commons.lang.StringUtils;
import org.apache.ranger.plugin.errors.ValidationErrorCode;
import org.apache.ranger.plugin.model.RangerRole;
import org.apache.ranger.plugin.store.RoleStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class RangerRoleValidator extends RangerValidator {
	private static final Logger LOG = LoggerFactory.getLogger(RangerRoleValidator.class);

	public RangerRoleValidator(RoleStore store) {
		super(store);
	}

	public void validate(RangerRole rangeRole, Action action) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug(String.format("==> RangerRoleValidator.validate(%s, %s)", rangeRole, action));
		}

		List<ValidationFailureDetails> failures = new ArrayList<>();
		boolean valid = isValid(rangeRole, action, failures);
		String message = "";
		try {
			if (!valid) {
				message = serializeFailures(failures);
				throw new Exception(message);
			}
		} finally {
			if (LOG.isDebugEnabled()) {
				LOG.debug(String.format("<== RangerRoleValidator.validate(%s, %s): %s, reason[%s]", rangeRole, action, valid, message));
			}
		}
	}

	@Override
	boolean isValid(Long id, Action action, List<ValidationFailureDetails> failures) {
		if(LOG.isDebugEnabled()) {
			LOG.debug(String.format("==> RangerRoleValidator.isValid(%s, %s, %s)", id, action, failures));
		}

		boolean valid = true;
		if (action != Action.DELETE) {
			ValidationErrorCode error = ValidationErrorCode.ROLE_VALIDATION_ERR_UNSUPPORTED_ACTION;
			failures.add(new ValidationFailureDetailsBuilder()
					.isAnInternalError()
					.becauseOf(error.getMessage())
					.errorCode(error.getErrorCode())
					.build());
			valid = false;
		} else if (id == null) {
			ValidationErrorCode error = ValidationErrorCode.ROLE_VALIDATION_ERR_MISSING_FIELD;
			failures.add(new ValidationFailureDetailsBuilder()
					.becauseOf("Role id was null/missing")
					.field("id")
					.isMissing()
					.errorCode(error.getErrorCode())
					.becauseOf(error.getMessage(id))
					.build());
			valid = false;
		} else if (!roleExists(id)) {
			ValidationErrorCode error = ValidationErrorCode.ROLE_VALIDATION_ERR_INVALID_ROLE_ID;
			failures.add(new ValidationFailureDetailsBuilder()
					.becauseOf("Role with id[{0}] does not exist")
					.field("id")
					.isMissing()
					.errorCode(error.getErrorCode())
					.becauseOf(error.getMessage(id))
					.build());
			valid = false;
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug(String.format("<== RangerRoleValidator.isValid(%s, %s, %s): %s", id, action, failures, valid));
		}
		return valid;
	}


	@Override
	boolean isValid(String name, Action action, List<ValidationFailureDetails> failures) {
		if(LOG.isDebugEnabled()) {
			LOG.debug(String.format("==> RangerRoleValidator.isValid(%s, %s, %s)", name, action, failures));
		}

		boolean valid = true;
		if (action != Action.DELETE) {
			ValidationErrorCode error = ValidationErrorCode.ROLE_VALIDATION_ERR_UNSUPPORTED_ACTION;
			failures.add(new ValidationFailureDetailsBuilder()
					.isAnInternalError()
					.becauseOf(error.getMessage())
					.errorCode(error.getErrorCode())
					.build());
			valid = false;
		} else if (name == null) {
			ValidationErrorCode error = ValidationErrorCode.ROLE_VALIDATION_ERR_MISSING_FIELD;
			failures.add(new ValidationFailureDetailsBuilder()
					.becauseOf("Role name was null/missing")
					.field("id")
					.isMissing()
					.errorCode(error.getErrorCode())
					.becauseOf(error.getMessage(name))
					.build());
			valid = false;
		} else if (!roleExists(name)) {
			ValidationErrorCode error = ValidationErrorCode.ROLE_VALIDATION_ERR_INVALID_ROLE_NAME;
			failures.add(new ValidationFailureDetailsBuilder()
					.becauseOf("Role with name[{0}] does not exist")
					.field("name")
					.isMissing()
					.errorCode(error.getErrorCode())
					.becauseOf(error.getMessage(name))
					.build());
			valid = false;
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug(String.format("<== RangerRoleValidator.isValid(%s, %s, %s): %s", name, action, failures, valid));
		}
		return valid;
	}

	boolean isValid(RangerRole rangerRole, Action action, List<ValidationFailureDetails> failures) {
		if (LOG.isDebugEnabled()) {
			LOG.debug(String.format("==> RangerRoleValidator.isValid(%s, %s, %s)", rangerRole, action, failures));
		}

		boolean valid = true;
		if (rangerRole == null) {
			ValidationErrorCode error = ValidationErrorCode.ROLE_VALIDATION_ERR_NULL_RANGER_ROLE_OBJECT;
			failures.add(new ValidationFailureDetailsBuilder()
					.isAnInternalError()
					.isMissing()
					.becauseOf(error.getMessage())
					.errorCode(error.getErrorCode())
					.build());
			valid = false;
		} else {
			String roleName = rangerRole.getName();
			if (StringUtils.isEmpty(roleName)) {
				ValidationErrorCode error = ValidationErrorCode.ROLE_VALIDATION_ERR_NULL_RANGER_ROLE_NAME;
				failures.add(new ValidationFailureDetailsBuilder()
						.field("name")
						.isMissing()
						.becauseOf(error.getMessage())
						.errorCode(error.getErrorCode())
						.build());
				valid = false;
			}

			Long id = rangerRole.getId();
			RangerRole existingRangerRole = null;
			if (null != id) {
				existingRangerRole = getRangerRole(id);
			}

			if (action == Action.CREATE) {
				if (existingRangerRole != null) {
					String existingRoleName = existingRangerRole.getName();
					if (roleName.equals(existingRoleName)) {
						ValidationErrorCode error = ValidationErrorCode.ROLE_VALIDATION_ERR_ROLE_NAME_CONFLICT;
						failures.add(new ValidationFailureDetailsBuilder()
								.field("name")
								.isSemanticallyIncorrect()
								.becauseOf(error.getMessage(existingRoleName))
								.errorCode(error.getErrorCode())
								.build());
						valid = false;
					}
				}
			} else  if (action == Action.UPDATE) { // id is ignored for CREATE
					if (id == null) {
						ValidationErrorCode error = ValidationErrorCode.ROLE_VALIDATION_ERR_MISSING_FIELD;
						failures.add(new ValidationFailureDetailsBuilder()
								.field("id")
								.isMissing()
								.becauseOf(error.getMessage(id))
								.errorCode(error.getErrorCode())
								.build());
						valid = false;
					}
					if (existingRangerRole == null) {
						ValidationErrorCode error = ValidationErrorCode.ROLE_VALIDATION_ERR_INVALID_ROLE_ID;
						failures.add(new ValidationFailureDetailsBuilder()
								.field("id")
								.isSemanticallyIncorrect()
								.becauseOf(error.getMessage(id))
								.errorCode(error.getErrorCode())
								.build());
						valid = false;
					}
				}
			}

			if (LOG.isDebugEnabled()) {
				LOG.debug(String.format("<== RangerRoleValidator.isValid(%s, %s, %s): %s", rangerRole, action, failures, valid));
			}

		return valid;
	}
}
