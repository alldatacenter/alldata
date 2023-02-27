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
from apache_atlas.model.instance import AtlasClassification
from apache_atlas.model.instance import AtlasEntityHeader
from apache_atlas.model.instance import AtlasObjectId
from apache_atlas.model.instance import AtlasStruct
from apache_atlas.model.misc import AtlasBase
from apache_atlas.utils import type_coerce
from apache_atlas.utils import type_coerce_dict
from apache_atlas.utils import type_coerce_list


class AtlasRelationship(AtlasStruct):
    def __init__(self, attrs=None):
        attrs = attrs or {}

        AtlasStruct.__init__(self, attrs)

        self.guid = attrs.get('guid')
        self.homeId = attrs.get('homeId')
        self.provenanceType = attrs.get('provenanceType')
        self.end1 = attrs.get('end1')
        self.end2 = attrs.get('end2')
        self.label = attrs.get('label')
        self.propagateTags = attrs.get('propagateTags')
        self.status = attrs.get('status')
        self.createdBy = attrs.get('createdBy')
        self.updatedBy = attrs.get('updatedBy')
        self.createTime = attrs.get('createTime')
        self.updateTime = attrs.get('updateTime')
        self.version = attrs.get('version')
        self.propagatedClassifications = attrs.get('propagatedClassifications')
        self.blockedPropagatedClassifications = attrs.get('blockedPropagatedClassifications')

    def type_coerce_attrs(self):
        super(AtlasRelationship, self).type_coerce_attrs()

        self.end1 = type_coerce(self.end1, AtlasObjectId)
        self.end2 = type_coerce(self.end2, AtlasObjectId)
        self.propagatedClassifications = type_coerce_list(self.propagatedClassifications, AtlasClassification)
        self.blockedPropagatedClassifications = type_coerce_list(
            self.blockedPropagatedClassifications, AtlasClassification)


class AtlasRelationshipWithExtInfo(AtlasBase):
    def __init__(self, attrs=None):
        attrs = attrs or {}

        AtlasBase.__init__(self, attrs)

        self.relationship = attrs.get('relationship')
        self.referredEntities = attrs.get('referredEntities')

    def type_coerce_attrs(self):
        super(AtlasBase, self).type_coerce_attrs()

        self.relationship = type_coerce(self.relationship, AtlasRelationship)
        self.referredEntities = type_coerce_dict(self.referredEntities, AtlasEntityHeader)
