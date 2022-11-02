#!/usr/bin/env/python
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
from apache_atlas.model.typedef import AtlasBusinessMetadataDef
from apache_atlas.model.typedef import AtlasClassificationDef
from apache_atlas.model.typedef import AtlasEntityDef
from apache_atlas.model.typedef import AtlasEnumDef
from apache_atlas.model.typedef import AtlasRelationshipDef
from apache_atlas.model.typedef import AtlasStructDef
from apache_atlas.model.typedef import AtlasTypesDef
from apache_atlas.utils import API
from apache_atlas.utils import BASE_URI
from apache_atlas.utils import HTTPMethod
from apache_atlas.utils import HTTPStatus


class TypeDefClient:
    TYPES_API = BASE_URI + "v2/types/"
    TYPEDEFS_API = TYPES_API + "typedefs/"
    TYPEDEF_BY_NAME = TYPES_API + "typedef/name"
    TYPEDEF_BY_GUID = TYPES_API + "typedef/guid"
    GET_BY_NAME_TEMPLATE = TYPES_API + "{path_type}/name/{name}"
    GET_BY_GUID_TEMPLATE = TYPES_API + "{path_type}/guid/{guid}"

    GET_TYPEDEF_BY_NAME = API(TYPEDEF_BY_NAME, HTTPMethod.GET, HTTPStatus.OK)
    GET_TYPEDEF_BY_GUID = API(TYPEDEF_BY_GUID, HTTPMethod.GET, HTTPStatus.OK)
    GET_ALL_TYPE_DEFS = API(TYPEDEFS_API, HTTPMethod.GET, HTTPStatus.OK)
    GET_ALL_TYPE_DEF_HEADERS = API(TYPEDEFS_API + "headers", HTTPMethod.GET, HTTPStatus.OK)
    UPDATE_TYPE_DEFS = API(TYPEDEFS_API, HTTPMethod.PUT, HTTPStatus.OK)
    CREATE_TYPE_DEFS = API(TYPEDEFS_API, HTTPMethod.POST, HTTPStatus.OK)
    DELETE_TYPE_DEFS = API(TYPEDEFS_API, HTTPMethod.DELETE, HTTPStatus.NO_CONTENT)
    DELETE_TYPE_DEF_BY_NAME = API(TYPEDEF_BY_NAME, HTTPMethod.DELETE, HTTPStatus.NO_CONTENT)

    def __init__(self, client):
        self.client = client

    def get_all_typedefs(self, search_filter):
        return self.client.call_api(TypeDefClient.GET_ALL_TYPE_DEFS, AtlasTypesDef, search_filter.params)

    def get_all_typedef_headers(self, search_filter):
        return self.client.call_api(TypeDefClient.GET_ALL_TYPE_DEF_HEADERS, list, search_filter.params)

    def type_with_guid_exists(self, guid):
        try:
            obj = self.client.call_api(TypeDefClient.GET_TYPEDEF_BY_GUID.format_path_with_params(guid), str)

            if obj is None:
                return False
        except Exception:
            return False

        return True

    def type_with_name_exists(self, name):
        try:
            obj = self.client.call_api(TypeDefClient.GET_TYPEDEF_BY_NAME.format_path_with_params(name), str)

            if obj is None:
                return False
        except Exception:
            return False

        return True

    def get_enumdef_by_name(self, name):
        return self.__get_typedef_by_name(name, AtlasEnumDef)

    def get_enumdef_by_guid(self, guid):
        return self.__get_typedef_by_guid(guid, AtlasEntityDef)

    def get_structdef_by_name(self, name):
        return self.__get_typedef_by_name(name, AtlasStructDef)

    def get_structdef_by_guid(self, guid):
        return self.__get_typedef_by_guid(guid, AtlasStructDef)

    def get_classificationdef_by_name(self, name):
        return self.__get_typedef_by_name(name, AtlasClassificationDef)

    def get_Classificationdef_by_guid(self, guid):
        return self.__get_typedef_by_guid(guid, AtlasClassificationDef)

    def get_entitydef_by_name(self, name):
        return self.__get_typedef_by_name(name, AtlasEntityDef)

    def get_entitydef_by_guid(self, guid):
        return self.__get_typedef_by_guid(guid, AtlasEntityDef)

    def get_relationshipdef_by_name(self, name):
        return self.__get_typedef_by_name(name, AtlasRelationshipDef)

    def get_relationshipdef_by_guid(self, guid):
        return self.__get_typedef_by_guid(guid, AtlasRelationshipDef)

    def get_businessmetadatadef_by_name(self, name):
        return self.__get_typedef_by_name(name, AtlasBusinessMetadataDef)

    def get_businessmetadatadef_by_guid(self, guid):
        return self.__get_typedef_by_guid(guid, AtlasBusinessMetadataDef)

    def create_atlas_typedefs(self, types_def):
        return self.client.call_api(TypeDefClient.CREATE_TYPE_DEFS, AtlasTypesDef, None, types_def)

    def update_atlas_typedefs(self, types_def):
        return self.client.call_api(TypeDefClient.UPDATE_TYPE_DEFS, AtlasTypesDef, None, types_def)

    def delete_atlas_typedefs(self, types_def):
        return self.client.call_api(TypeDefClient.DELETE_TYPE_DEFS, None, types_def)

    def delete_type_by_name(self, type_name):
        return self.client.call_api(TypeDefClient.DELETE_TYPE_DEF_BY_NAME.format_path_with_params(type_name))

    def __get_typedef_by_name(self, name, typedef_class):
        path_type = self.__get_path_for_type(typedef_class)
        api = API(TypeDefClient.GET_BY_NAME_TEMPLATE, HTTPMethod.GET, HTTPStatus.OK)

        return self.client.call_api(api.format_path({'path_type': path_type, 'name': name}), typedef_class)

    def __get_typedef_by_guid(self, guid, typedef_class):
        path_type = self.__get_path_for_type(typedef_class)
        api = API(TypeDefClient.GET_BY_GUID_TEMPLATE, HTTPMethod.GET, HTTPStatus.OK)

        return self.client.call_api(api.format_path({'path_type': path_type, 'guid': guid}), typedef_class)

    def __get_path_for_type(self, typedef_class):
        if issubclass(AtlasEnumDef, typedef_class):
            return "enumdef"
        if issubclass(AtlasEntityDef, typedef_class):
            return "entitydef"
        if issubclass(AtlasClassificationDef, typedef_class):
            return "classificationdef"
        if issubclass(AtlasStructDef, typedef_class):
            return "structdef"
        if issubclass(AtlasRelationshipDef, typedef_class):
            return "relationshipdef"
        if issubclass(AtlasBusinessMetadataDef, typedef_class):
            return "businessmetadatadef"

        return ""
