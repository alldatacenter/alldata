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
# Unless required by applicabwle law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
from apache_atlas.model.discovery import SearchParameters
from apache_atlas.model.misc import AtlasBaseModelObject
from apache_atlas.utils import type_coerce


class AtlasUserSavedSearch(AtlasBaseModelObject):
    def __init__(self, attrs=None):
        attrs = attrs or {}

        AtlasBaseModelObject.__init__(self, attrs)

        self.ownerName = attrs.get('ownerName')
        self.name = attrs.get('name')
        self.searchType = attrs.get('searchType')
        self.searchParameters = attrs.get('searchParameters')
        self.uiParameters = attrs.get('uiParameters')

    def type_coerce_attrs(self):
        super(AtlasUserSavedSearch, self).type_coerce_attrs()

        self.searchParameters = type_coerce(self.searchParameters, SearchParameters)
