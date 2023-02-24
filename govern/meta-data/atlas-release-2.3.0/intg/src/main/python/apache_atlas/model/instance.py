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
from apache_atlas.model.enums import EntityStatus
from apache_atlas.model.glossary import AtlasTermAssignmentHeader
from apache_atlas.model.misc import AtlasBase, next_id, Plist, TimeBoundary
from apache_atlas.utils import non_null, type_coerce, type_coerce_dict, type_coerce_dict_list, type_coerce_list


class AtlasStruct(AtlasBase):
    def __init__(self, attrs=None):
        attrs = attrs or {}

        AtlasBase.__init__(self, attrs)

        self.typeName = attrs.get('typeName')
        self.attributes = attrs.get('attributes')

    def get_attribute(self, name):
        return self.attributes[name] if self.attributes is not None and name in self.attributes else None

    def set_attribute(self, name, value):
        if self.attributes is None:
            self.attributes = {}

        self.attributes[name] = value

    def remove_attribute(self, name):
        if name and self.attributes is not None and name in self.attributes:
            del self.attributes[name]


class AtlasEntity(AtlasStruct):
    def __init__(self, attrs=None):
        attrs = attrs or {}

        AtlasStruct.__init__(self, attrs)

        self.guid = attrs.get('guid')
        self.homeId = attrs.get('homeId')
        self.relationshipAttributes = attrs.get('relationshipAttributes')
        self.classifications = attrs.get('classifications')
        self.meanings = attrs.get('meanings')
        self.customAttributes = attrs.get('customAttributes')
        self.businessAttributes = attrs.get('businessAttributes')
        self.labels = attrs.get('labels')
        self.status = attrs.get('status')
        self.isIncomplete = attrs.get('isIncomplete')
        self.provenanceType = attrs.get('provenanceType')
        self.proxy = attrs.get('proxy')
        self.version = attrs.get('version')
        self.createdBy = attrs.get('createdBy')
        self.updatedBy = attrs.get('updatedBy')
        self.createTime = attrs.get('createTime')
        self.updateTime = attrs.get('updateTime')

        if self.guid is None:
            self.guid = next_id()

    def type_coerce_attrs(self):
        super(AtlasEntity, self).type_coerce_attrs()

        self.classifications = type_coerce_list(self.classifications, AtlasClassification)
        self.meanings = type_coerce_list(self.meanings, AtlasTermAssignmentHeader)

    def get_relationship_attribute(self, name):
        return self.relationshipAttributes[
            name] if self.relationshipAttributes is not None and name in self.relationshipAttributes else None

    def set_relationship_attribute(self, name, value):
        if self.relationshipAttributes is None:
            self.relationshipAttributes = {}

        self.relationshipAttributes[name] = value

    def remove_relationship_attribute(self, name):
        if name and self.relationshipAttributes is not None and name in self.relationshipAttributes:
            del self.relationshipAttributes[name]


class AtlasEntityExtInfo(AtlasBase):
    def __init__(self, attrs=None):
        attrs = attrs or {}

        AtlasBase.__init__(self, attrs)

        self.referredEntities = attrs.get('referredEntities')

    def type_coerce_attrs(self):
        super(AtlasEntityExtInfo, self).type_coerce_attrs()

        self.referredEntities = type_coerce_dict(self.referredEntities, AtlasEntity)

    def get_referenced_entity(self, guid):
        return self.referredEntities[
            guid] if self.referredEntities is not None and guid in self.referredEntities else None

    def add_referenced_entity(self, entity):
        if self.referredEntities is None:
            self.referredEntities = {}

        self.referredEntities[entity.guid] = entity


class AtlasEntityWithExtInfo(AtlasEntityExtInfo):
    def __init__(self, attrs=None):
        attrs = attrs or {}

        AtlasEntityExtInfo.__init__(self, attrs)

        self.entity = attrs.get('entity')

    def type_coerce_attrs(self):
        super(AtlasEntityWithExtInfo, self).type_coerce_attrs()

        self.entity = type_coerce(self.entity, AtlasEntity)


class AtlasEntitiesWithExtInfo(AtlasEntityExtInfo):
    def __init__(self, attrs=None):
        attrs = attrs or {}

        AtlasEntityExtInfo.__init__(self, attrs)

        self.entities = attrs.get('entities')

    def type_coerce_attrs(self):
        super(AtlasEntitiesWithExtInfo, self).type_coerce_attrs()

        self.entities = type_coerce_list(self.entities, AtlasEntity)

    def add_entity(self, entity):
        if self.entities is None:
            self.entities = []

        self.entities.append(entity)


class AtlasEntityHeader(AtlasStruct):
    def __init__(self, attrs=None):
        attrs = attrs or {}

        AtlasStruct.__init__(self, attrs)

        self.guid = attrs.get('guid')
        self.status = non_null(attrs.get('status'), EntityStatus.ACTIVE.name)
        self.displayText = attrs.get('displayText')
        self.classificationNames = attrs.get('classificationNames')
        self.classifications = attrs.get('classifications')
        self.meaningNames = attrs.get('meaningNames')
        self.meanings = attrs.get('.meanings')
        self.isIncomplete = non_null(attrs.get('isIncomplete'), False)
        self.labels = attrs.get('labels')

        if self.guid is None:
            self.guid = next_id()

    def type_coerce_attrs(self):
        super(AtlasEntityHeader, self).type_coerce_attrs()

        self.classifications = type_coerce_list(self.classifications, AtlasClassification)
        self.meanings = type_coerce_list(self.meanings, AtlasTermAssignmentHeader)


class AtlasClassification(AtlasStruct):
    def __init__(self, attrs=None):
        attrs = attrs or {}

        AtlasStruct.__init__(self, attrs)

        self.entityGuid = attrs.get('entityGuid')
        self.entityStatus = non_null(attrs.get('entityStatus'), EntityStatus.ACTIVE.name)
        self.propagate = attrs.get('propagate')
        self.validityPeriods = attrs.get('validityPeriods')
        self.removePropagationsOnEntityDelete = attrs.get('removePropagationsOnEntityDelete')

    def type_coerce_attrs(self):
        super(AtlasClassification, self).type_coerce_attrs()

        self.validityPeriods = type_coerce_list(self.validityPeriods, TimeBoundary)


class AtlasObjectId(AtlasBase):
    def __init__(self, attrs=None):
        attrs = attrs or {}

        AtlasBase.__init__(self, attrs)

        self.guid = attrs.get('guid')
        self.typeName = attrs.get('typeName')
        self.uniqueAttributes = attrs.get('uniqueAttributes')


class AtlasRelatedObjectId(AtlasObjectId):
    def __init__(self, attrs=None):
        attrs = attrs or {}

        AtlasObjectId.__init__(self, attrs)

        self.entityStatus = attrs.get('entityStatus')
        self.displayText = attrs.get('displayText')
        self.relationshipType = attrs.get('relationshipType')
        self.relationshipGuid = attrs.get('relationshipGuid')
        self.relationshipStatus = attrs.get('relationshipStatus')
        self.relationshipAttributes = attrs.get('relationshipAttributes')

    def type_coerce_attrs(self):
        super(AtlasRelatedObjectId, self).type_coerce_attrs()

        self.relationshipAttributes = type_coerce(self.relationshipAttributes, AtlasStruct)


class AtlasClassifications(Plist):
    def __init__(self, attrs=None):
        attrs = attrs or {}

        Plist.__init__(self, attrs)

    def type_coerce_attrs(self):
        super(AtlasClassifications, self).type_coerce_attrs()

        Plist.list = type_coerce_list(Plist.list, AtlasClassification)


class AtlasEntityHeaders(AtlasBase):
    def __init__(self, attrs=None):
        attrs = attrs or {}

        AtlasBase.__init__(self, attrs)

        self.guidHeaderMap = attrs.get('guidHeaderMap')

    def type_coerce_attrs(self):
        super(AtlasEntityHeaders, self).type_coerce_attrs()

        self.guidHeaderMap = type_coerce_dict(self.guidHeaderMap, AtlasEntityHeader)


class EntityMutationResponse(AtlasBase):
    def __init__(self, attrs=None):
        attrs = attrs or {}

        AtlasBase.__init__(self, attrs)

        self.mutatedEntities = attrs.get('mutatedEntities')
        self.guidAssignments = attrs.get('guidAssignments')

    def type_coerce_attrs(self):
        super(EntityMutationResponse, self).type_coerce_attrs()

        self.mutatedEntities = type_coerce_dict_list(self.mutatedEntities, AtlasEntityHeader)

    def get_assigned_guid(self, guid):
        return self.guidAssignments.get(guid) if self.guidAssignments else None


class EntityMutations(AtlasBase):
    def __init__(self, attrs=None):
        attrs = attrs or {}

        AtlasBase.__init__(self, attrs)

        self.entity_mutations = attrs.get('entity_mutations')

    def type_coerce_attrs(self):
        super(EntityMutations, self).type_coerce_attrs()

        self.entity_mutations = type_coerce_list(self.entity_mutations, EntityMutation)


class EntityMutation(AtlasBase):
    def __init__(self, attrs=None):
        attrs = attrs or {}

        AtlasBase.__init__(self, attrs)

        self.op = attrs.get('op')
        self.entity = attrs.get('entity')

    def type_coerce_attrs(self):
        super(EntityMutation, self).type_coerce_attrs()

        self.entity = type_coerce(self.entity, AtlasEntity)


class AtlasCheckStateRequest(AtlasBase):
    def __init__(self, attrs=None):
        attrs = attrs or {}

        AtlasBase.__init__(self, attrs)

        self.entityGuids = attrs.get('entityGuids')
        self.entityTypes = attrs.get('entityTypes')
        self.fixIssues = attrs.get('fixIssues')


class AtlasCheckStateResult(AtlasBase):
    def __init__(self, attrs=None):
        attrs = attrs or {}

        AtlasBase.__init__(self, attrs)

        self.entitiesScanned = attrs.get('entitiesScanned')
        self.entitiesOk = attrs.get('entitiesOk')
        self.entitiesFixed = attrs.get('entitiesFixed')
        self.entitiesPartiallyFixed = attrs.get('entitiesPartiallyFixed')
        self.entitiesNotFixed = attrs.get('entitiesNotFixed')
        self.state = attrs.get('state')
        self.entities = attrs.get('entities')

    def type_coerce_attrs(self):
        super(AtlasCheckStateResult, self).type_coerce_attrs()

        self.entities = type_coerce(self.entities, AtlasEntityState)


class AtlasEntityState(AtlasBase):
    def __init__(self, attrs=None):
        attrs = attrs or {}

        AtlasBase.__init__(self, attrs)

        self.guid = attrs.get('guid')
        self.typeName = attrs.get('typeName')
        self.name = attrs.get('name')
        self.status = attrs.get('status')
        self.state = attrs.get('state')
        self.issues = attrs.get('issues')
