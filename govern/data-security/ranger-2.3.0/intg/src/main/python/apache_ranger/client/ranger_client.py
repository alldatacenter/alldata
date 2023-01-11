#!/usr/bin/env python

#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


import json
import logging
import os
from apache_ranger.exceptions                 import RangerServiceException
from apache_ranger.model.ranger_base          import RangerBase
from apache_ranger.model.ranger_policy        import RangerPolicy
from apache_ranger.model.ranger_role          import RangerRole
from apache_ranger.model.ranger_security_zone import RangerSecurityZone
from apache_ranger.model.ranger_service       import RangerService
from apache_ranger.model.ranger_service_def   import RangerServiceDef
from apache_ranger.model.ranger_service_tags  import RangerServiceTags
from apache_ranger.utils                      import *
from requests                                 import Session
from requests                                 import Response

LOG = logging.getLogger(__name__)


class RangerClient:
    def __init__(self, url, auth):
        self.client_http = RangerClientHttp(url, auth)

        logging.getLogger("requests").setLevel(logging.WARNING)


    # Service Definition APIs
    def create_service_def(self, serviceDef):
        resp = self.client_http.call_api(RangerClient.CREATE_SERVICEDEF, request_data=serviceDef)

        return type_coerce(resp, RangerServiceDef)

    def update_service_def_by_id(self, serviceDefId, serviceDef):
        resp = self.client_http.call_api(RangerClient.UPDATE_SERVICEDEF_BY_ID.format_path({ 'id': serviceDefId }), request_data=serviceDef)

        return type_coerce(resp, RangerServiceDef)

    def update_service_def(self, serviceDefName, serviceDef):
        resp = self.client_http.call_api(RangerClient.UPDATE_SERVICEDEF_BY_NAME.format_path({ 'name': serviceDefName }), request_data=serviceDef)

        return type_coerce(resp, RangerServiceDef)

    def delete_service_def_by_id(self, serviceDefId, params=None):
        self.client_http.call_api(RangerClient.DELETE_SERVICEDEF_BY_ID.format_path({ 'id': serviceDefId }), params)

    def delete_service_def(self, serviceDefName, params=None):
        self.client_http.call_api(RangerClient.DELETE_SERVICEDEF_BY_NAME.format_path({ 'name': serviceDefName }), params)

    def get_service_def_by_id(self, serviceDefId):
        resp = self.client_http.call_api(RangerClient.GET_SERVICEDEF_BY_ID.format_path({ 'id': serviceDefId }))

        return type_coerce(resp, RangerServiceDef)

    def get_service_def(self, serviceDefName):
        resp = self.client_http.call_api(RangerClient.GET_SERVICEDEF_BY_NAME.format_path({ 'name': serviceDefName }))

        return type_coerce(resp, RangerServiceDef)

    def find_service_defs(self, filter=None):
        resp = self.client_http.call_api(RangerClient.FIND_SERVICEDEFS, filter)

        return type_coerce_list(resp, RangerServiceDef)


    # Service APIs
    def create_service(self, service):
        resp = self.client_http.call_api(RangerClient.CREATE_SERVICE, request_data=service)

        return type_coerce(resp, RangerService)

    def get_service_by_id(self, serviceId):
        resp = self.client_http.call_api(RangerClient.GET_SERVICE_BY_ID.format_path({ 'id': serviceId }))

        return type_coerce(resp, RangerService)

    def get_service(self, serviceName):
        resp = self.client_http.call_api(RangerClient.GET_SERVICE_BY_NAME.format_path({ 'name': serviceName }))

        return type_coerce(resp, RangerService)

    def update_service_by_id(self, serviceId, service, params=None):
        resp = self.client_http.call_api(RangerClient.UPDATE_SERVICE_BY_ID.format_path({ 'id': serviceId }), params, service)

        return type_coerce(resp, RangerService)

    def update_service(self, serviceName, service, params=None):
        resp = self.client_http.call_api(RangerClient.UPDATE_SERVICE_BY_NAME.format_path({ 'name': serviceName }), params, service)

        return type_coerce(resp, RangerService)

    def delete_service_by_id(self, serviceId):
        self.client_http.call_api(RangerClient.DELETE_SERVICE_BY_ID.format_path({ 'id': serviceId }))

    def delete_service(self, serviceName):
        self.client_http.call_api(RangerClient.DELETE_SERVICE_BY_NAME.format_path({ 'name': serviceName }))

    def find_services(self, filter=None):
        resp = self.client_http.call_api(RangerClient.FIND_SERVICES, filter)

        return type_coerce_list(resp, RangerService)


    # Policy APIs
    def create_policy(self, policy, params=None):
        resp = self.client_http.call_api(RangerClient.CREATE_POLICY, params, policy)

        return type_coerce(resp, RangerPolicy)

    def get_policy_by_id(self, policyId):
        resp = self.client_http.call_api(RangerClient.GET_POLICY_BY_ID.format_path({ 'id': policyId }))

        return type_coerce(resp, RangerPolicy)

    def get_policy(self, serviceName, policyName):
        resp = self.client_http.call_api(RangerClient.GET_POLICY_BY_NAME.format_path({ 'serviceName': serviceName, 'policyName': policyName}))

        return type_coerce(resp, RangerPolicy)

    def get_policies_in_service(self, serviceName, params=None):
        resp = self.client_http.call_api(RangerClient.GET_POLICIES_IN_SERVICE.format_path({ 'serviceName': serviceName }), params)

        return type_coerce_list(resp, RangerPolicy)

    def update_policy_by_id(self, policyId, policy):
        resp = self.client_http.call_api(RangerClient.UPDATE_POLICY_BY_ID.format_path({ 'id': policyId }), request_data=policy)

        return type_coerce(resp, RangerPolicy)

    def update_policy(self, serviceName, policyName, policy):
        resp = self.client_http.call_api(RangerClient.UPDATE_POLICY_BY_NAME.format_path({ 'serviceName': serviceName, 'policyName': policyName}), request_data=policy)

        return type_coerce(resp, RangerPolicy)

    def apply_policy(self, policy, params=None):
        resp = self.client_http.call_api(RangerClient.APPLY_POLICY, params, policy)

        return type_coerce(resp, RangerPolicy)

    def delete_policy_by_id(self, policyId):
        self.client_http.call_api(RangerClient.DELETE_POLICY_BY_ID.format_path({ 'id': policyId }))

    def delete_policy(self, serviceName, policyName):
        self.client_http.call_api(RangerClient.DELETE_POLICY_BY_NAME, { 'servicename': serviceName, 'policyname': policyName })

    def find_policies(self, filter=None):
        resp = self.client_http.call_api(RangerClient.FIND_POLICIES, filter)

        return type_coerce_list(resp, RangerPolicy)


    # SecurityZone APIs
    def create_security_zone(self, securityZone):
        resp = self.client_http.call_api(RangerClient.CREATE_ZONE, request_data=securityZone)

        return type_coerce(resp, RangerSecurityZone)

    def update_security_zone_by_id(self, zoneId, securityZone):
        resp = self.client_http.call_api(RangerClient.UPDATE_ZONE_BY_ID.format_path({ 'id': zoneId }), request_data=securityZone)

        return type_coerce(resp, RangerSecurityZone)

    def update_security_zone(self, zoneName, securityZone):
        resp = self.client_http.call_api(RangerClient.UPDATE_ZONE_BY_NAME.format_path({ 'name': zoneName }), request_data=securityZone)

        return type_coerce(resp, RangerSecurityZone)

    def delete_security_zone_by_id(self, zoneId):
        self.client_http.call_api(RangerClient.DELETE_ZONE_BY_ID.format_path({ 'id': zoneId }))

    def delete_security_zone(self, zoneName):
        self.client_http.call_api(RangerClient.DELETE_ZONE_BY_NAME.format_path({ 'name': zoneName }))

    def get_security_zone_by_id(self, zoneId):
        resp = self.client_http.call_api(RangerClient.GET_ZONE_BY_ID.format_path({ 'id': zoneId }))

        return type_coerce(resp, RangerSecurityZone)

    def get_security_zone(self, zoneName):
        resp = self.client_http.call_api(RangerClient.GET_ZONE_BY_NAME.format_path({ 'name': zoneName }))

        return type_coerce(resp, RangerSecurityZone)

    def find_security_zones(self, filter=None):
        resp = self.client_http.call_api(RangerClient.FIND_ZONES, filter)

        return type_coerce_list(resp, RangerSecurityZone)


    # Role APIs
    def create_role(self, serviceName, role, params=None):
        if params is None:
            params = {}

        params['serviceName'] = serviceName

        resp = self.client_http.call_api(RangerClient.CREATE_ROLE, params, role)

        return type_coerce(resp, RangerRole)

    def update_role(self, roleId, role, params=None):
        resp = self.client_http.call_api(RangerClient.UPDATE_ROLE_BY_ID.format_path({ 'id': roleId }), params, role)

        return type_coerce(resp, RangerRole)

    def delete_role_by_id(self, roleId):
        self.client_http.call_api(RangerClient.DELETE_ROLE_BY_ID.format_path({ 'id': roleId }))

    def delete_role(self, roleName, execUser, serviceName):
        self.client_http.call_api(RangerClient.DELETE_ROLE_BY_NAME.format_path({ 'name': roleName }), { 'execUser': execUser, 'serviceName': serviceName })

    def get_role_by_id(self, roleId):
        resp = self.client_http.call_api(RangerClient.GET_ROLE_BY_ID.format_path({ 'id': roleId }))

        return type_coerce(resp, RangerRole)

    def get_role(self, roleName, execUser, serviceName):
        resp = self.client_http.call_api(RangerClient.GET_ROLE_BY_NAME.format_path({ 'name': roleName }), { 'execUser': execUser, 'serviceName': serviceName })

        return type_coerce(resp, RangerRole)

    def get_all_role_names(self, execUser, serviceName):
        resp = self.client_http.call_api(RangerClient.GET_ALL_ROLE_NAMES.format_path({ 'name': serviceName }), { 'execUser': execUser, 'serviceName': serviceName })

        return resp

    def get_user_roles(self, user, filters=None):
        ret = self.client_http.call_api(RangerClient.GET_USER_ROLES.format_path({ 'name': user }), filters)

        return list(ret) if ret is not None else None

    def find_roles(self, filter=None):
        resp = self.client_http.call_api(RangerClient.FIND_ROLES, filter)

        return type_coerce_list(resp, RangerRole)

    def grant_role(self, serviceName, request, params=None):
        resp = self.client_http.call_api(RangerClient.GRANT_ROLE.format_path({ 'name': serviceName }), params, request)

        return type_coerce(resp, RESTResponse)

    def revoke_role(self, serviceName, request, params=None):
        resp = self.client_http.call_api(RangerClient.REVOKE_ROLE.format_path({ 'name': serviceName }), params, request)

        return type_coerce(resp, RESTResponse)


    # Admin APIs
    def import_service_tags(self, serviceName, svcTags):
        self.client_http.call_api(RangerClient.IMPORT_SERVICE_TAGS.format_path({ 'serviceName': serviceName }), request_data=svcTags)

    def get_service_tags(self, serviceName):
        resp = self.client_http.call_api(RangerClient.GET_SERVICE_TAGS.format_path({ 'serviceName': serviceName }))

        return type_coerce(resp, RangerServiceTags)

    def delete_policy_deltas(self, days, reloadServicePoliciesCache):
        self.client_http.call_api(RangerClient.DELETE_POLICY_DELTAS, { 'days': days, 'reloadServicePoliciesCache': reloadServicePoliciesCache})





    # URIs
    URI_BASE                = "service/public/v2/api"

    URI_SERVICEDEF          = URI_BASE + "/servicedef"
    URI_SERVICEDEF_BY_ID    = URI_SERVICEDEF + "/{id}"
    URI_SERVICEDEF_BY_NAME  = URI_SERVICEDEF + "/name/{name}"

    URI_SERVICE             = URI_BASE + "/service"
    URI_SERVICE_BY_ID       = URI_SERVICE + "/{id}"
    URI_SERVICE_BY_NAME     = URI_SERVICE + "/name/{name}"
    URI_POLICIES_IN_SERVICE = URI_SERVICE + "/{serviceName}/policy"

    URI_POLICY              = URI_BASE + "/policy"
    URI_APPLY_POLICY        = URI_POLICY + "/apply"
    URI_POLICY_BY_ID        = URI_POLICY + "/{id}"
    URI_POLICY_BY_NAME      = URI_SERVICE + "/{serviceName}/policy/{policyName}"

    URI_ROLE                = URI_BASE + "/roles"
    URI_ROLE_NAMES          = URI_ROLE + "/names"
    URI_ROLE_BY_ID          = URI_ROLE + "/{id}"
    URI_ROLE_BY_NAME        = URI_ROLE + "/name/{name}"
    URI_USER_ROLES          = URI_ROLE + "/user/{name}"
    URI_GRANT_ROLE          = URI_ROLE + "/grant/{name}"
    URI_REVOKE_ROLE         = URI_ROLE + "/revoke/{name}"

    URI_ZONE                = URI_BASE + "/zones"
    URI_ZONE_BY_ID          = URI_ZONE + "/{id}"
    URI_ZONE_BY_NAME        = URI_ZONE + "/name/{name}"

    URI_SERVICE_TAGS        = URI_SERVICE + "/{serviceName}/tags"
    URI_PLUGIN_INFO         = URI_BASE + "/plugins/info"
    URI_POLICY_DELTAS       = URI_BASE + "/server/policydeltas"

    # APIs
    CREATE_SERVICEDEF         = API(URI_SERVICEDEF, HttpMethod.POST, HTTPStatus.OK)
    UPDATE_SERVICEDEF_BY_ID   = API(URI_SERVICEDEF_BY_ID, HttpMethod.PUT, HTTPStatus.OK)
    UPDATE_SERVICEDEF_BY_NAME = API(URI_SERVICEDEF_BY_NAME, HttpMethod.PUT, HTTPStatus.OK)
    DELETE_SERVICEDEF_BY_ID   = API(URI_SERVICEDEF_BY_ID, HttpMethod.DELETE, HTTPStatus.NO_CONTENT)
    DELETE_SERVICEDEF_BY_NAME = API(URI_SERVICEDEF_BY_NAME, HttpMethod.DELETE, HTTPStatus.NO_CONTENT)
    GET_SERVICEDEF_BY_ID      = API(URI_SERVICEDEF_BY_ID, HttpMethod.GET, HTTPStatus.OK)
    GET_SERVICEDEF_BY_NAME    = API(URI_SERVICEDEF_BY_NAME, HttpMethod.GET, HTTPStatus.OK)
    FIND_SERVICEDEFS          = API(URI_SERVICEDEF, HttpMethod.GET, HTTPStatus.OK)

    CREATE_SERVICE            = API(URI_SERVICE, HttpMethod.POST, HTTPStatus.OK)
    UPDATE_SERVICE_BY_ID      = API(URI_SERVICE_BY_ID, HttpMethod.PUT, HTTPStatus.OK)
    UPDATE_SERVICE_BY_NAME    = API(URI_SERVICE_BY_NAME, HttpMethod.PUT, HTTPStatus.OK)
    DELETE_SERVICE_BY_ID      = API(URI_SERVICE_BY_ID, HttpMethod.DELETE, HTTPStatus.NO_CONTENT)
    DELETE_SERVICE_BY_NAME    = API(URI_SERVICE_BY_NAME, HttpMethod.DELETE, HTTPStatus.NO_CONTENT)
    GET_SERVICE_BY_ID         = API(URI_SERVICE_BY_ID, HttpMethod.GET, HTTPStatus.OK)
    GET_SERVICE_BY_NAME       = API(URI_SERVICE_BY_NAME, HttpMethod.GET, HTTPStatus.OK)
    FIND_SERVICES             = API(URI_SERVICE, HttpMethod.GET, HTTPStatus.OK)

    CREATE_POLICY             = API(URI_POLICY, HttpMethod.POST, HTTPStatus.OK)
    UPDATE_POLICY_BY_ID       = API(URI_POLICY_BY_ID, HttpMethod.PUT, HTTPStatus.OK)
    UPDATE_POLICY_BY_NAME     = API(URI_POLICY_BY_NAME, HttpMethod.PUT, HTTPStatus.OK)
    APPLY_POLICY              = API(URI_APPLY_POLICY, HttpMethod.POST, HTTPStatus.OK)
    DELETE_POLICY_BY_ID       = API(URI_POLICY_BY_ID, HttpMethod.DELETE, HTTPStatus.NO_CONTENT)
    DELETE_POLICY_BY_NAME     = API(URI_POLICY, HttpMethod.DELETE, HTTPStatus.NO_CONTENT)
    GET_POLICY_BY_ID          = API(URI_POLICY_BY_ID, HttpMethod.GET, HTTPStatus.OK)
    GET_POLICY_BY_NAME        = API(URI_POLICY_BY_NAME, HttpMethod.GET, HTTPStatus.OK)
    GET_POLICIES_IN_SERVICE   = API(URI_POLICIES_IN_SERVICE, HttpMethod.GET, HTTPStatus.OK)
    FIND_POLICIES             = API(URI_POLICY, HttpMethod.GET, HTTPStatus.OK)

    CREATE_ZONE               = API(URI_ZONE, HttpMethod.POST, HTTPStatus.OK)
    UPDATE_ZONE_BY_ID         = API(URI_ZONE_BY_ID, HttpMethod.PUT, HTTPStatus.OK)
    UPDATE_ZONE_BY_NAME       = API(URI_ZONE_BY_NAME, HttpMethod.PUT, HTTPStatus.OK)
    DELETE_ZONE_BY_ID         = API(URI_ZONE_BY_ID, HttpMethod.DELETE, HTTPStatus.NO_CONTENT)
    DELETE_ZONE_BY_NAME       = API(URI_ZONE_BY_NAME, HttpMethod.DELETE, HTTPStatus.NO_CONTENT)
    GET_ZONE_BY_ID            = API(URI_ZONE_BY_ID, HttpMethod.GET, HTTPStatus.OK)
    GET_ZONE_BY_NAME          = API(URI_ZONE_BY_NAME, HttpMethod.GET, HTTPStatus.OK)
    FIND_ZONES                = API(URI_ZONE, HttpMethod.GET, HTTPStatus.OK)

    CREATE_ROLE               = API(URI_ROLE, HttpMethod.POST, HTTPStatus.OK)
    UPDATE_ROLE_BY_ID         = API(URI_ROLE_BY_ID, HttpMethod.PUT, HTTPStatus.OK)
    DELETE_ROLE_BY_ID         = API(URI_ROLE_BY_ID, HttpMethod.DELETE, HTTPStatus.NO_CONTENT)
    DELETE_ROLE_BY_NAME       = API(URI_ROLE_BY_NAME, HttpMethod.DELETE, HTTPStatus.NO_CONTENT)
    GET_ROLE_BY_ID            = API(URI_ROLE_BY_ID, HttpMethod.GET, HTTPStatus.OK)
    GET_ROLE_BY_NAME          = API(URI_ROLE_BY_NAME, HttpMethod.GET, HTTPStatus.OK)
    GET_ALL_ROLE_NAMES        = API(URI_ROLE_NAMES, HttpMethod.GET, HTTPStatus.OK)
    GET_USER_ROLES            = API(URI_USER_ROLES, HttpMethod.GET, HTTPStatus.OK)
    GRANT_ROLE                = API(URI_GRANT_ROLE, HttpMethod.PUT, HTTPStatus.OK)
    REVOKE_ROLE               = API(URI_REVOKE_ROLE, HttpMethod.PUT, HTTPStatus.OK)
    FIND_ROLES                = API(URI_ROLE, HttpMethod.GET, HTTPStatus.OK)

    IMPORT_SERVICE_TAGS       = API(URI_SERVICE_TAGS, HttpMethod.PUT, HTTPStatus.NO_CONTENT)
    GET_SERVICE_TAGS          = API(URI_SERVICE_TAGS, HttpMethod.GET, HTTPStatus.OK)
    GET_PLUGIN_INFO           = API(URI_PLUGIN_INFO, HttpMethod.GET, HTTPStatus.OK)
    DELETE_POLICY_DELTAS      = API(URI_POLICY_DELTAS, HttpMethod.DELETE, HTTPStatus.NO_CONTENT)


class Message(RangerBase):
    def __init__(self, attrs=None):
        if attrs is None:
            attrs = {}

        RangerBase.__init__(self, attrs)

        self.name      = attrs.get('name')
        self.rbKey     = attrs.get('rbKey')
        self.message   = attrs.get('message')
        self.objectId  = attrs.get('objectId')
        self.fieldName = attrs.get('fieldName')


class RESTResponse(RangerBase):
    def __init__(self, attrs=None):
        if attrs is None:
            attrs = {}

        RangerBase.__init__(self, attrs)

        self.httpStatusCode = attrs.get('httpStatusCode')
        self.statusCode     = attrs.get('statusCode')
        self.msgDesc        = attrs.get('msgDesc')
        self.messageList    = non_null(attrs.get('messageList'), [])

    def type_coerce_attrs(self):
        super(RangerPolicy, self).type_coerce_attrs()

        self.messageList = type_coerce_dict(self.messageList, Message)


class RangerClientHttp:
    def __init__(self, url, auth):
        self.url          = url
        self.session      = Session()
        self.session.auth = auth


    def call_api(self, api, query_params=None, request_data=None):
        ret    = None
        params = { 'headers': { 'Accept': api.consumes, 'Content-type': api.produces } }

        if query_params:
            params['params'] = query_params

        if request_data:
            params['data'] = json.dumps(request_data)

        path = os.path.join(self.url, api.path)

        if LOG.isEnabledFor(logging.DEBUG):
            LOG.debug("------------------------------------------------------")
            LOG.debug("Call         : %s %s", api.method, path)
            LOG.debug("Content-type : %s", api.consumes)
            LOG.debug("Accept       : %s", api.produces)

        response = None

        if api.method == HttpMethod.GET:
            response = self.session.get(path, **params)
        elif api.method == HttpMethod.POST:
            response = self.session.post(path, **params)
        elif api.method == HttpMethod.PUT:
            response = self.session.put(path, **params)
        elif api.method == HttpMethod.DELETE:
            response = self.session.delete(path, **params)

        if LOG.isEnabledFor(logging.DEBUG):
            LOG.debug("HTTP Status: %s", response.status_code if response else "None")

        if response is None:
            ret = None
        elif response.status_code == api.expected_status:
            try:
                if response.status_code == HTTPStatus.NO_CONTENT or response.content is None:
                    ret = None
                else:
                    if LOG.isEnabledFor(logging.DEBUG):
                        LOG.debug("<== __call_api(%s, %s, %s), result=%s", vars(api), params, request_data, response)

                        LOG.debug(response.json())

                    ret = response.json()
            except Exception as e:
                print(e)

                LOG.exception("Exception occurred while parsing response with msg: %s", e)

                raise RangerServiceException(api, response)
        elif response.status_code == HTTPStatus.SERVICE_UNAVAILABLE:
            LOG.error("Ranger admin unavailable. HTTP Status: %s", HTTPStatus.SERVICE_UNAVAILABLE)

            ret = None
        elif response.status_code == HTTPStatus.NOT_FOUND:
            LOG.error("Not found. HTTP Status: %s", HTTPStatus.NOT_FOUND)

            ret = None
        else:
            raise RangerServiceException(api, response)

        return ret


class RangerClientPrivate:
    def __init__(self, url, auth):
        self.client_http = RangerClientHttp(url, auth)

        logging.getLogger("requests").setLevel(logging.WARNING)

    # URLs
    URI_DELETE_USER  = "service/xusers/secure/users/{name}"
    URI_DELETE_GROUP = "service/xusers/secure/groups/{name}"

    # APIs
    DELETE_USER  = API(URI_DELETE_USER, HttpMethod.DELETE, HTTPStatus.NO_CONTENT)
    DELETE_GROUP = API(URI_DELETE_GROUP, HttpMethod.DELETE, HTTPStatus.NO_CONTENT)

    def delete_user(self, userName, execUser, isForceDelete='true'):
        self.client_http.call_api(RangerClientPrivate.DELETE_USER.format_path({ 'name': userName }), { 'execUser': execUser, 'forceDelete': isForceDelete })

    def delete_group(self, groupName, execUser, isForceDelete='true'):
        self.client_http.call_api(RangerClientPrivate.DELETE_GROUP.format_path({ 'name': groupName }), { 'execUser': execUser, 'forceDelete': isForceDelete })
