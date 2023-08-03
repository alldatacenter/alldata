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
from apache_ranger.exceptions           import RangerServiceException
from apache_ranger.client.ranger_client import RangerClientHttp
from apache_ranger.model.ranger_kms     import RangerKey
from apache_ranger.model.ranger_kms     import RangerKeyVersion
from apache_ranger.model.ranger_kms     import RangerKeyMetadata
from apache_ranger.model.ranger_kms     import RangerEncryptedKeyVersion
from apache_ranger.utils                import *

LOG = logging.getLogger(__name__)

#
# Python client for KMS REST APIs
# More details in https://hadoop.apache.org/docs/current/hadoop-kms/index.html#KMS_HTTP_REST_API
#
class RangerKMSClient:
    def __init__(self, url, auth):
        self.client_http = RangerClientHttp(url, auth)

        logging.getLogger("requests").setLevel(logging.WARNING)


    def create_key(self, key):
        resp = self.client_http.call_api(RangerKMSClient.CREATE_KEY, request_data=key)

        return type_coerce(resp, RangerKeyVersion)

    def rollover_key(self, key_name, material=None):
        resp = self.client_http.call_api(RangerKMSClient.ROLLOVER_KEY.format_path({ 'name': key_name }), request_data={ 'material': material})

        return type_coerce(resp, RangerKeyVersion)

    def invalidate_cache_for_key(self, key_name):
        self.client_http.call_api(RangerKMSClient.INVALIDATE_CACHE_FOR_KEY.format_path({ 'name': key_name }))

    def delete_key(self, key_name):
        self.client_http.call_api(RangerKMSClient.DELETE_KEY.format_path({ 'name': key_name }))

    def get_key_metadata(self, key_name):
        resp = self.client_http.call_api(RangerKMSClient.GET_KEY_METADATA.format_path({ 'name': key_name }))

        return type_coerce(resp, RangerKeyMetadata)

    def get_current_key(self, key_name):
        resp = self.client_http.call_api(RangerKMSClient.GET_CURRENT_KEY.format_path({ 'name': key_name }))

        return type_coerce(resp, RangerKeyVersion)

    def generate_encrypted_key(self, key_name, num_keys):
        resp = self.client_http.call_api(RangerKMSClient.GENERATE_ENCRYPTED_KEY.format_path({'name': key_name}), query_params={'eek_op': 'generate', 'num_keys': num_keys})

        return type_coerce_list(resp, RangerEncryptedKeyVersion)

    def decrypt_encrypted_key(self, key_name, version_name, iv, material):
        resp = self.client_http.call_api(RangerKMSClient.DECRYPT_ENCRYPTED_KEY.format_path({'version_name': version_name}), request_data={'name': key_name, 'iv': iv, 'material': material}, query_params={'eek_op': 'decrypt'})

        return type_coerce(resp, RangerKeyVersion)

    def reencrypt_encrypted_key(self, key_name, version_name, iv, material):
        resp = self.client_http.call_api(RangerKMSClient.REENCRYPT_ENCRYPTED_KEY.format_path({'version_name': version_name}), request_data={'name': key_name, 'iv': iv, 'material': material}, query_params={'eek_op': 'reencrypt'})

        return type_coerce(resp, RangerEncryptedKeyVersion)

    def batch_reencrypt_encrypted_keys(self, key_name, encrypted_key_versions):
        resp = self.client_http.call_api(RangerKMSClient.BATCH_REENCRYPT_ENCRYPTED_KEYS.format_path({'name': key_name}), request_data=encrypted_key_versions)

        return type_coerce_list(resp, RangerEncryptedKeyVersion)

    def get_key_version(self, version_name):
        resp = self.client_http.call_api(RangerKMSClient.GET_KEY_VERSION.format_path({ 'version_name': version_name }))

        return type_coerce(resp, RangerKeyVersion)

    def get_key_versions(self, key_name):
        resp = self.client_http.call_api(RangerKMSClient.GET_KEY_VERSIONS.format_path({ 'name': key_name}))

        return type_coerce_list(resp, RangerKeyVersion)

    def get_key_names(self):
        resp = self.client_http.call_api(RangerKMSClient.GET_KEYS_NAMES)

        return resp

    def get_keys_metadata(self, key_names):
        resp = self.client_http.call_api(RangerKMSClient.GET_KEYS_METADATA, query_params={'key': key_names})

        return type_coerce_list(resp, RangerKeyMetadata)

    # Ranger KMS
    def get_key(self, key_name):
        resp = self.client_http.call_api(RangerKMSClient.GET_KEY.format_path({ 'name': key_name }))

        return type_coerce(resp, RangerKeyMetadata)

    # Ranger KMS
    def kms_status(self):
        resp = self.client_http.call_api(RangerKMSClient.KMS_STATUS)

        return resp

    # URIs
    URI_KEYS                    = "kms/v1/keys"
    URI_KEY_BY_NAME             = "kms/v1/key/{name}"
    URI_KEY_INVALIDATE_CACHE    = URI_KEY_BY_NAME + "/_invalidatecache"
    URI_KEY_METADATA            = URI_KEY_BY_NAME + "/_metadata"
    URI_CURRENT_KEY             = URI_KEY_BY_NAME + "/_currentversion"
    URI_KEY_EEK                 = URI_KEY_BY_NAME + "/_eek"
    URI_BATCH_REENCRYPT_KEYS    = URI_KEY_BY_NAME + "/_reencryptbatch"
    URI_KEY_VERSIONS            = URI_KEY_BY_NAME + "/_versions"
    URI_KEY_VERSION_BY_NAME     = "kms/v1/keyversion/{version_name}"
    URI_KEY_VERSION_BY_NAME_EEK = URI_KEY_VERSION_BY_NAME + "/_eek"
    URI_KEYS_NAMES              = URI_KEYS + "/names"
    URI_KEYS_METADATA           = URI_KEYS + "/metadata"

    # Ranger KMS
    URI_KMS_STATUS = "kms/api/status"


    # APIs
    CREATE_KEY                     = API(URI_KEYS, HttpMethod.POST, HTTPStatus.CREATED)
    ROLLOVER_KEY                   = API(URI_KEY_BY_NAME, HttpMethod.POST, HTTPStatus.OK)
    INVALIDATE_CACHE_FOR_KEY       = API(URI_KEY_INVALIDATE_CACHE, HttpMethod.POST, HTTPStatus.OK)
    DELETE_KEY                     = API(URI_KEY_BY_NAME, HttpMethod.DELETE, HTTPStatus.OK)
    GET_KEY_METADATA               = API(URI_KEY_METADATA, HttpMethod.GET, HTTPStatus.OK)
    GET_CURRENT_KEY                = API(URI_CURRENT_KEY, HttpMethod.GET, HTTPStatus.OK)
    GENERATE_ENCRYPTED_KEY         = API(URI_KEY_EEK, HttpMethod.GET, HTTPStatus.OK)
    DECRYPT_ENCRYPTED_KEY          = API(URI_KEY_VERSION_BY_NAME_EEK, HttpMethod.POST, HTTPStatus.OK)
    REENCRYPT_ENCRYPTED_KEY        = API(URI_KEY_VERSION_BY_NAME_EEK, HttpMethod.POST, HTTPStatus.OK)
    BATCH_REENCRYPT_ENCRYPTED_KEYS = API(URI_BATCH_REENCRYPT_KEYS, HttpMethod.POST, HTTPStatus.OK)
    GET_KEY_VERSION                = API(URI_KEY_VERSION_BY_NAME, HttpMethod.GET, HTTPStatus.OK)
    GET_KEY_VERSIONS               = API(URI_KEY_VERSIONS, HttpMethod.GET, HTTPStatus.OK)
    GET_KEYS_NAMES                 = API(URI_KEYS_NAMES, HttpMethod.GET, HTTPStatus.OK)
    GET_KEYS_METADATA              = API(URI_KEYS_METADATA, HttpMethod.GET, HTTPStatus.OK)

    # Ranger KMS
    GET_KEY    = API(URI_KEY_BY_NAME, HttpMethod.GET, HTTPStatus.OK)
    KMS_STATUS = API(URI_KMS_STATUS, HttpMethod.GET, HTTPStatus.OK)
