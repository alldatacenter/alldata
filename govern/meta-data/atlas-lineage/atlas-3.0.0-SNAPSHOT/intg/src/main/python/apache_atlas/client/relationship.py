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
from apache_atlas.model.relationship import AtlasRelationship
from apache_atlas.model.relationship import AtlasRelationshipWithExtInfo
from apache_atlas.utils import API
from apache_atlas.utils import BASE_URI
from apache_atlas.utils import HTTPMethod
from apache_atlas.utils import HTTPStatus


class RelationshipClient:
    RELATIONSHIPS_URI = BASE_URI + "v2/relationship/"
    BULK_HEADERS = "bulk/headers"
    BULK_SET_CLASSIFICATIONS = "bulk/setClassifications"

    GET_RELATIONSHIP_BY_GUID = API(RELATIONSHIPS_URI + "guid", HTTPMethod.GET, HTTPStatus.OK)
    CREATE_RELATIONSHIP = API(RELATIONSHIPS_URI, HTTPMethod.POST, HTTPStatus.OK)
    UPDATE_RELATIONSHIP = API(RELATIONSHIPS_URI, HTTPMethod.PUT, HTTPStatus.OK)
    DELETE_RELATIONSHIP_BY_GUID = API(RELATIONSHIPS_URI + "guid", HTTPMethod.DELETE, HTTPStatus.NO_CONTENT)

    def __init__(self, client):
        self.client = client

    def get_relationship_by_guid(self, guid):
        return self.client.call_api(RelationshipClient.GET_RELATIONSHIP_BY_GUID.format_path_with_params(guid),
                                    AtlasRelationshipWithExtInfo)

    def get_relationship_by_guid_with_ext_info(self, guid, extended_info):
        query_params = {"extendedInfo": extended_info}

        return self.client.call_api(RelationshipClient.GET_RELATIONSHIP_BY_GUID.format_path_with_params(guid),
                                    AtlasRelationshipWithExtInfo, query_params)

    def create_relationship(self, relationship):
        return self.client.call_api(RelationshipClient.CREATE_RELATIONSHIP, AtlasRelationship, None, relationship)

    def update_relationship(self, relationship):
        return self.client.call_api(RelationshipClient.UPDATE_RELATIONSHIP, AtlasRelationship, relationship)

    def delete_relationship_by_guid(self, guid):
        return self.client.call_api(RelationshipClient.DELETE_RELATIONSHIP_BY_GUID.format_path_with_params(guid))
