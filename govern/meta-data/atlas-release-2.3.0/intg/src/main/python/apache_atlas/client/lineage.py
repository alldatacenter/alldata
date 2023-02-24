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
from apache_atlas.model.lineage import AtlasLineageInfo
from apache_atlas.utils import API
from apache_atlas.utils import attributes_to_params
from apache_atlas.utils import BASE_URI
from apache_atlas.utils import HTTPMethod
from apache_atlas.utils import HTTPStatus


class LineageClient:
    LINEAGE_URI = BASE_URI + "v2/lineage"
    LINEAGE_INFO = API(LINEAGE_URI, HTTPMethod.GET, HTTPStatus.OK)
    GET_LINEAGE_BY_ATTRIBUTES = API(LINEAGE_URI + "/uniqueAttribute/type/", HTTPMethod.GET, HTTPStatus.OK)

    def __init__(self, client):
        self.client = client

    def get_lineage_info(self, guid, direction, depth):
        query_params = {"direction": direction, "depth": depth}

        return self.client.call_api(LineageClient.LINEAGE_INFO.format_path_with_params(guid), AtlasLineageInfo,
                                    query_params)

    def get_lineage_info_attr(self, type_name, attributes, direction, depth):
        query_params = attributes_to_params(attributes, {"direction": direction, "depth": depth})

        return self.client.call_api(LineageClient.GET_LINEAGE_BY_ATTRIBUTES.format_path_with_params(type_name),
                                    AtlasLineageInfo, query_params)
