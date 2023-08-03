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

package org.apache.ranger.plugin.errors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.Arrays;

public enum ValidationErrorCode {
    // SERVICE VALIDATION
    SERVICE_VALIDATION_ERR_UNSUPPORTED_ACTION(1001, "Internal error: unsupported action[{0}]; isValid(Long) is only supported for DELETE"),
    SERVICE_VALIDATION_ERR_MISSING_FIELD(1002, "Internal error: missing field[{0}]"),
    SERVICE_VALIDATION_ERR_NULL_SERVICE_OBJECT(1003, "Internal error: service object passed in was null"),
    SERVICE_VALIDATION_ERR_EMPTY_SERVICE_ID(1004, "Internal error: service id was null/empty/blank"),
    SERVICE_VALIDATION_ERR_INVALID_SERVICE_ID(1005, "No service found for id [{0}]"),
    SERVICE_VALIDATION_ERR_INVALID_SERVICE_NAME(1006, "Missing service name"),
    SERVICE_VALIDATION_ERR_SERVICE_NAME_CONFICT(1007, "Duplicate service name: name=[{0}]"),
    SERVICE_VALIDATION_ERR_SERVICE_DISPLAY_NAME_CONFICT(3051,"Display name [{0}] is already used by service [{1}]"),
    SERVICE_VALIDATION_ERR_SPECIAL_CHARACTERS_SERVICE_NAME(3031, "Invalid service name=[{0}]. It should not be longer than 256 characters and special characters are not allowed (except underscore and hyphen)"),
    SERVICE_VALIDATION_ERR_SPECIAL_CHARACTERS_SERVICE_DISPLAY_NAME(3050, "Invalid service display name [{0}]. It should not be longer than 256 characters, should not start with space, and should not include special characters (except underscore, hyphen and space)"),
    SERVICE_VALIDATION_ERR_ID_NAME_CONFLICT(1008, "Duplicate service name: name=[{0}], id=[{1}]"),
    SERVICE_VALIDATION_ERR_MISSING_SERVICE_DEF(1009, "Missing service def"),
    SERVICE_VALIDATION_ERR_INVALID_SERVICE_DEF(1010, "Service def not found: service-def-name=[{0}]"),
    SERVICE_VALIDATION_ERR_REQUIRED_PARM_MISSING(1011, "Missing required configuration parameter(s): missing parameters={0}"),

    // SERVICE-DEF VALIDATION
    SERVICE_DEF_VALIDATION_ERR_UNSUPPORTED_ACTION(2001, "Internal error: unsupported action[{0}]; isValid(Long) is only supported for DELETE"),
    SERVICE_DEF_VALIDATION_ERR_MISSING_FIELD(2002, "Internal error: missing field[{0}]"),
    SERVICE_DEF_VALIDATION_ERR_NULL_SERVICE_DEF_OBJECT(2003, "Internal error: service def object passed in was null"),
    SERVICE_DEF_VALIDATION_ERR_EMPTY_SERVICE_DEF_ID(2004, "Internal error: service def id was null/empty/blank"),
    SERVICE_DEF_VALIDATION_ERR_INVALID_SERVICE_DEF_ID(2005, "No service def found for id [{0}]"),
    SERVICE_DEF_VALIDATION_ERR_INVALID_SERVICE_DEF_NAME(2006, "Service def name[{0}] was null/empty/blank"),
    SERVICE_DEF_VALIDATION_ERR_INVALID_SERVICE_DEF_DISPLAY_NAME(2024, "Service def display name[{0}] was null/empty/blank"),
    SERVICE_DEF_VALIDATION_ERR_SERVICE_DEF_NAME_CONFICT(2007, "service def with the name[{0}] already exists"),
    SERVICE_DEF_VALIDATION_ERR_SERVICE_DEF__DISPLAY_NAME_CONFICT(2025, "Display name [{0}] is already used by service def [{1}]"),
    SERVICE_DEF_VALIDATION_ERR_ID_NAME_CONFLICT(2008, "id/name conflict: another service def already exists with name[{0}], its id is [{1}]"),
    SERVICE_DEF_VALIDATION_ERR_IMPLIED_GRANT_UNKNOWN_ACCESS_TYPE(2009, "implied grant[{0}] contains an unknown access types[{1}]"),
    SERVICE_DEF_VALIDATION_ERR_IMPLIED_GRANT_IMPLIES_ITSELF(2010, "implied grants list [{0}] for access type[{1}] contains itself"),
    SERVICE_DEF_VALIDATION_ERR_POLICY_CONDITION_NULL_EVALUATOR(2011, "evaluator on policy condition definition[{0}] was null/empty!"),
    SERVICE_DEF_VALIDATION_ERR_CONFIG_DEF_UNKNOWN_ENUM(2012, "subtype[{0}] of service def config[{1}] was not among defined enums[{2}]"),
    SERVICE_DEF_VALIDATION_ERR_CONFIG_DEF_UNKNOWN_ENUM_VALUE(2013, "default value[{0}] of service def config[{1}] was not among the valid values[{2}] of enums[{3}]"),
    SERVICE_DEF_VALIDATION_ERR_CONFIG_DEF_MISSING_TYPE(2014, "type of service def config[{0}] was null/empty"),
    SERVICE_DEF_VALIDATION_ERR_CONFIG_DEF_INVALID_TYPE(2015, "type[{0}] of service def config[{1}] is not among valid types: {2}"),
    SERVICE_DEF_VALIDATION_ERR_RESOURCE_GRAPH_INVALID(2016, "Resource graph implied by various resources, e.g. parent value is invalid.  Valid graph must forest (union of disjoint trees)."),
    SERVICE_DEF_VALIDATION_ERR_ENUM_DEF_NULL_OBJECT(2017, "Internal error: An enum def in enums collection is null"),
    SERVICE_DEF_VALIDATION_ERR_ENUM_DEF_NO_VALUES(2018, "enum [{0}] does not have any elements"),
    SERVICE_DEF_VALIDATION_ERR_ENUM_DEF_INVALID_DEFAULT_INDEX(2019, "default index[{0}] for enum [{1}] is invalid"),
    SERVICE_DEF_VALIDATION_ERR_ENUM_DEF_NULL_ENUM_ELEMENT(2020, "An enum element in enum element collection of enum [{0}] is null"),
    SERVICE_DEF_VALIDATION_ERR_INVALID_SERVICE_RESOURCE_LEVELS(2021, "Resource-def levels are not in increasing order in an hierarchy"),
	SERVICE_DEF_VALIDATION_ERR_NOT_LOWERCASE_NAME(2022, "{0}:[{1}] Invalid resource name. Resource name should consist of only lowercase, hyphen or underscore characters"),
    SERVICE_DEF_VALIDATION_ERR_INVALID_MANADORY_VALUE_FOR_SERVICE_RESOURCE(2023, "{0} cannot be mandatory because {1}(parent) is not mandatory"),

    // POLICY VALIDATION
    POLICY_VALIDATION_ERR_UNSUPPORTED_ACTION(3001, "Internal error: method signature isValid(Long) is only supported for DELETE"),
    POLICY_VALIDATION_ERR_MISSING_FIELD(3002, "Internal error: missing field[{0}]"),
    POLICY_VALIDATION_ERR_NULL_POLICY_OBJECT(3003, "Internal error: policy object passed in was null"),
    POLICY_VALIDATION_ERR_INVALID_POLICY_ID(3004, "No policy found for id[{0}]"),
    POLICY_VALIDATION_ERR_POLICY_NAME_MULTIPLE_POLICIES_WITH_SAME_NAME(3005, "Internal error: multiple policies found with the name[{0}]"),
    POLICY_VALIDATION_ERR_POLICY_NAME_CONFLICT(3006, "Another policy already exists for this name: policy-id=[{0}], service=[{1}]"),
    POLICY_VALIDATION_ERR_INVALID_SERVICE_NAME(3007, "no service found with name[{0}]"),
    POLICY_VALIDATION_ERR_MISSING_POLICY_ITEMS(3008, "at least one policy item must be specified if audit isn't enabled"),
    POLICY_VALIDATION_ERR_MISSING_SERVICE_DEF(3009, "Internal error: Service def[{0}] of policy's service[{1}] does not exist!"),
    POLICY_VALIDATION_ERR_DUPLICATE_POLICY_RESOURCE(3010, "Another policy already exists for matching resource: policy-name=[{0}], service=[{1}]"),
    POLICY_VALIDATION_ERR_INVALID_RESOURCE_NO_COMPATIBLE_HIERARCHY(3011, "Invalid resources specified. {0} policy can specify values for one of the following resource sets: {1}"),
    POLICY_VALIDATION_ERR_INVALID_RESOURCE_MISSING_MANDATORY(3012, "Invalid resources specified. {0} policy must specify values for one of the following resource sets: {1}"),
    POLICY_VALIDATION_ERR_NULL_RESOURCE_DEF(3013, "Internal error: a resource-def on resource def collection of service-def[{0}] was null"),
    POLICY_VALIDATION_ERR_MISSING_RESOURCE_DEF_NAME(3014, "Internal error: name of a resource-def on resource def collection of service-def[{0}] was null"),
    POLICY_VALIDATION_ERR_EXCLUDES_NOT_SUPPORTED(3015, "Excludes option not supported: resource-name=[{0}]"),
    POLICY_VALIDATION_ERR_EXCLUDES_REQUIRES_ADMIN(3016, "Insufficient permissions to create excludes policy"),
    POLICY_VALIDATION_ERR_RECURSIVE_NOT_SUPPORTED(3017, "Recursive option not supported: resource-name=[{0}]."),
    POLICY_VALIDATION_ERR_INVALID_RESOURCE_VALUE_REGEX(3018, "Invalid resource specified. A value of [{0}] is not valid for resource [{1}]"),
    POLICY_VALIDATION_ERR_NULL_POLICY_ITEM(3019, "policy items object was null"),
    POLICY_VALIDATION_ERR_MISSING_USER_AND_GROUPS(3020, "All of users,  user-groups and roles collections on the policy item were null/empty"),
    POLICY_VALIDATION_ERR_NULL_POLICY_ITEM_ACCESS(3021, "policy items access object was null"),
    POLICY_VALIDATION_ERR_POLICY_ITEM_ACCESS_TYPE_INVALID(3022, "Invalid access type: access type=[{0}], valid access types=[{1}]"),
    POLICY_VALIDATION_ERR_POLICY_ITEM_ACCESS_TYPE_DENY(3023, "Currently deny access types are not supported. Access type is set to deny."),
    POLICY_VALIDATION_ERR_INVALID_RESOURCE_NO_COMPATIBLE_HIERARCHY_SINGLE(3024, "Invalid resources specified. {0} policy can specify values for the following resources: {1}"),
    POLICY_VALIDATION_ERR_INVALID_RESOURCE_MISSING_MANDATORY_SINGLE(3025, "Invalid resources specified. {0} policy must specify values for the following resources: {1}"),
    POLICY_VALIDATION_ERR_MISSING_RESOURCE_LIST(3026, "Resource list was empty or contains null. At least one resource must be specified"),
    POLICY_VALIDATION_ERR_POLICY_UPDATE_MOVE_SERVICE_NOT_ALLOWED(3027, "attempt to move policy id={0} from service={1} to service={2} is not allowed"),
    POLICY_VALIDATION_ERR_POLICY_TYPE_CHANGE_NOT_ALLOWED(3028, "attempt to change type of policy id={0} from type={1} to type={2} is not allowed"),
    POLICY_VALIDATION_ERR_POLICY_INVALID_VALIDITY_SCHEDULE(3029, "Invalid validity schedule specification"),
    POLICY_VALIDATION_ERR_POLICY_INVALID_PRIORITY(3030, "Invalid priority value"),
    POLICY_VALIDATION_ERR_UPDATE_ZONE_NAME_NOT_ALLOWED(3032, "Update of Zone name from={0} to={1} in policy is not supported"),
    POLICY_VALIDATION_ERR_NONEXISTANT_ZONE_NAME(3033, "Non-existent Zone name={0} in policy create"),
    POLICY_VALIDATION_ERR_SERVICE_NOT_ASSOCIATED_TO_ZONE(3048, "Service name = {0} is not associated to Zone name = {1}"),
    POLICY_VALIDATION_ERR_UNSUPPORTED_POLICY_ITEM_TYPE(3049, "Deny or deny-exceptions are not supported if policy has isDenyAllElse flag set to true"),
    POLICY_VALIDATION_ERR_INVALID_SERVICE_TYPE(4009," Invalid service type [{0}] provided for service [{1}]"),

    // SECURITY_ZONE Validations
    SECURITY_ZONE_VALIDATION_ERR_UNSUPPORTED_ACTION(3034, "Internal error: unsupported action[{0}]; isValid() is only supported for DELETE"),
    SECURITY_ZONE_VALIDATION_ERR_MISSING_FIELD(3035, "Internal error: missing field[{0}]"),
    SECURITY_ZONE_VALIDATION_ERR_ZONE_NAME_CONFLICT(3036, "Another security zone already exists for this name: zone-id=[{0}]]"),
    SECURITY_ZONE_VALIDATION_ERR_INVALID_ZONE_ID(3037, "No security zone found for [{0}]"),
    SECURITY_ZONE_VALIDATION_ERR_MISSING_USER_AND_GROUPS(3038, "both users and user-groups collections for the security zone were null/empty"),
    SECURITY_ZONE_VALIDATION_ERR_MISSING_RESOURCES(3039, "No resources specified for service [{0}]"),
    SECURITY_ZONE_VALIDATION_ERR_INVALID_SERVICE_NAME(3040, "Invalid service [{0}]"),
    SECURITY_ZONE_VALIDATION_ERR_INVALID_SERVICE_TYPE(3041, "Invalid service-type [{0}]"),
    SECURITY_ZONE_VALIDATION_ERR_INVALID_RESOURCE_HIERARCHY(3042, "Invalid resource hierarchy specified for service:[{0}], resource-hierarchy:[{1}]"),
    SECURITY_ZONE_VALIDATION_ERR_ALL_WILDCARD_RESOURCE_VALUES(3043, "All wildcard values specified for resources for service:[{0}]"),
    SECURITY_ZONE_VALIDATION_ERR_MISSING_SERVICES(3044, "No services specified for security-zone:[{0}]"),
    SECURITY_ZONE_VALIDATION_ERR_INTERNAL_ERROR(3045, "Internal Error:[{0}]"),
    SECURITY_ZONE_VALIDATION_ERR_ZONE_RESOURCE_CONFLICT(3046, "Multiple zones:[{0}] match resource:[{1}]"),
    SECURITY_ZONE_VALIDATION_ERR_UNEXPECTED_RESOURCES(3047, "Tag service [{0}], with non-empty resources, is associated with security zone"),

    //RANGER ROLE Validations
    ROLE_VALIDATION_ERR_NULL_RANGER_ROLE_OBJECT(4001, "Internal error: RangerRole object passed in was null"),
    ROLE_VALIDATION_ERR_MISSING_FIELD(4002, "Internal error: missing field[{0}]"),
    ROLE_VALIDATION_ERR_NULL_RANGER_ROLE_NAME(4003, "Internal error: RangerRole name passed in was null/empty"),
    ROLE_VALIDATION_ERR_MISSING_USER_OR_GROUPS_OR_ROLES(4004, "RangerRole should contain atleast one user or group or role"),
    ROLE_VALIDATION_ERR_ROLE_NAME_CONFLICT(4005, "Another RangerRole already exists for this name: Role =[{0}]]"),
    ROLE_VALIDATION_ERR_INVALID_ROLE_ID(4006, "No RangerRole found for id[{0}]"),
    ROLE_VALIDATION_ERR_INVALID_ROLE_NAME(4007, "No RangerRole found for name[{0}]"),
    ROLE_VALIDATION_ERR_UNSUPPORTED_ACTION(4008, "Internal error: method signature isValid(Long) is only supported for DELETE"),


    ;


    private static final Logger LOG = LoggerFactory.getLogger(ValidationErrorCode.class);

    final int _errorCode;
    final String _template;

    ValidationErrorCode(int errorCode, String template) {
        _errorCode = errorCode;
        _template = template;
    }

    public String getMessage(Object... items) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("<== ValidationErrorCode.getMessage(%s)", Arrays.toString(items)));
        }

        MessageFormat mf = new MessageFormat(_template);
        String result = mf.format(items);

        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("<== ValidationErrorCode.getMessage(%s): %s", Arrays.toString(items), result));
        }
        return result;
    }

    public int getErrorCode() {
        return _errorCode;
    }

    @Override
    public String toString() {
        return String.format("Code: %d, template: %s", _errorCode, _template);
    }
}
