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
import logging

from apache_atlas.model.enums import TypeCategory
from apache_atlas.model.misc import AtlasBase
from apache_atlas.utils import non_null
from apache_atlas.utils import type_coerce
from apache_atlas.utils import type_coerce_dict_list
from apache_atlas.utils import type_coerce_list

LOG = logging.getLogger('apache_atlas')


class AtlasBaseTypeDef(AtlasBase):
    def __init__(self, attrs=None):
        attrs = attrs or {}

        AtlasBase.__init__(self, attrs)

        self.category = attrs.get('category')
        self.guid = attrs.get('guid')
        self.createdBy = attrs.get('createdBy')
        self.updatedBy = attrs.get('updatedBy')
        self.createTime = attrs.get('createTime')
        self.updateTime = attrs.get('updateTime')
        self.version = attrs.get('version')
        self.name = attrs.get('name')
        self.description = attrs.get('description')
        self.typeVersion = attrs.get('typeVersion')
        self.serviceType = attrs.get('serviceType')
        self.options = attrs.get('options')


class AtlasEnumDef(AtlasBaseTypeDef):
    def __init__(self, attrs=None):
        attrs = attrs or {}

        AtlasBaseTypeDef.__init__(self, attrs)

        self.elementDefs = attrs.get('elementDefs')
        self.defaultValue = attrs.get('defaultValue')

    def type_coerce_attrs(self):
        super(AtlasEnumDef, self).type_coerce_attrs()

        self.elementDefs = type_coerce_list(self.elementDefs, AtlasEnumElementDef)


class AtlasStructDef(AtlasBaseTypeDef):
    def __init__(self, attrs=None):
        attrs = attrs or {}

        AtlasBaseTypeDef.__init__(self, attrs)

        self.category = non_null(attrs.get('category'), TypeCategory.STRUCT.name)
        self.attributeDefs = attrs.get('attributeDefs')

    def type_coerce_attrs(self):
        super(AtlasStructDef, self).type_coerce_attrs()

        self.attributeDefs = type_coerce_list(self.attributeDefs, AtlasAttributeDef)


class AtlasClassificationDef(AtlasStructDef):
    def __init__(self, attrs=None):
        attrs = attrs or {}

        AtlasStructDef.__init__(self, attrs)

        self.category = TypeCategory.CLASSIFICATION.name
        self.superTypes = attrs.get('superTypes')
        self.entityTypes = attrs.get('entityTypes')
        self.subTypes = attrs.get('subTypes')


class AtlasEntityDef(AtlasStructDef):
    def __init__(self, attrs=None):
        attrs = attrs or {}

        AtlasStructDef.__init__(self, attrs)

        self.category = TypeCategory.ENTITY.name
        self.superTypes = attrs.get('superTypes')
        self.subTypes = attrs.get('subTypes')
        self.relationshipAttributeDefs = attrs.get('relationshipAttributeDefs')
        self.businessAttributeDefs = attrs.get('businessAttributeDefs')

    def type_coerce_attrs(self):
        super(AtlasEntityDef, self).type_coerce_attrs()

        self.relationshipAttributeDefs = type_coerce_list(self.relationshipAttributeDefs, AtlasRelationshipAttributeDef)
        self.businessAttributeDefs = type_coerce_dict_list(self.businessAttributeDefs, AtlasAttributeDef)


class AtlasRelationshipDef(AtlasStructDef):
    def __init__(self, attrs=None):
        attrs = attrs or {}

        AtlasStructDef.__init__(self, attrs)

        self.category = TypeCategory.RELATIONSHIP.name
        self.relationshipCategory = attrs.get('relationshipCategory')
        self.relationshipLabel = attrs.get('relationshipLabel')
        self.propagateTags = attrs.get('propagateTags')
        self.endDef1 = attrs.get('endDef1')
        self.endDef2 = attrs.get('endDef2')

    def type_coerce_attrs(self):
        super(AtlasRelationshipDef, self).type_coerce_attrs()

        self.endDef1 = type_coerce(self.endDef1, AtlasRelationshipEndDef)
        self.endDef2 = type_coerce(self.endDef2, AtlasRelationshipEndDef)


class AtlasBusinessMetadataDef(AtlasStructDef):
    def __init__(self, attrs=None):
        attrs = attrs or {}

        AtlasStructDef.__init__(self, attrs)

        self.category = TypeCategory.BUSINESS_METADATA.name


class AtlasAttributeDef(AtlasBase):
    def __init__(self, attrs=None):
        attrs = attrs or {}

        AtlasBase.__init__(self, attrs)

        self.name = attrs.get('name')
        self.typeName = attrs.get('typeName')
        self.isOptional = attrs.get('isOptional')
        self.cardinality = attrs.get('cardinality')
        self.valuesMinCount = attrs.get('valuesMinCount')
        self.valuesMaxCount = attrs.get('valuesMaxCount')
        self.isUnique = attrs.get('isUnique')
        self.isIndexable = attrs.get('isIndexable')
        self.includeInNotification = attrs.get('includeInNotification')
        self.defaultValue = attrs.get('defaultValue')
        self.description = attrs.get('description')
        self.searchWeight = non_null(attrs.get('searchWeight'), -1)
        self.indexType = attrs.get('indexType')
        self.constraints = attrs.get('constraints')
        self.options = attrs.get('options')
        self.displayName = attrs.get('displayName')

    def type_coerce_attrs(self):
        super(AtlasAttributeDef, self).type_coerce_attrs()

        self.constraints = type_coerce_list(self.constraints, AtlasConstraintDef)


class AtlasConstraintDef(AtlasBase):
    def __init__(self, attrs=None):
        attrs = attrs or {}

        AtlasBase.__init__(self, attrs)

        self.type = attrs.get('type')
        self.params = attrs.get('params')


class AtlasEnumElementDef(AtlasBase):
    def __init__(self, attrs=None):
        attrs = attrs or {}

        AtlasBase.__init__(self, attrs)

        self.value = attrs.get('value')
        self.description = attrs.get('description')
        self.ordinal = attrs.get('ordinal')


class AtlasRelationshipAttributeDef(AtlasAttributeDef):
    def __init__(self, attrs=None):
        attrs = attrs or {}

        AtlasAttributeDef.__init__(self, attrs)

        self.relationshipTypeName = attrs.get('relationshipTypeName')
        self.isLegacyAttribute = attrs.get('isLegacyAttribute')


class AtlasRelationshipEndDef(AtlasBase):
    def __init__(self, attrs=None):
        attrs = attrs or {}

        AtlasBase.__init__(self, attrs)

        self.type = attrs.get('type')
        self.name = attrs.get('name')
        self.isContainer = attrs.get('isContainer')
        self.cardinality = attrs.get('cardinality')
        self.isLegacyAttribute = attrs.get('isLegacyAttribute')
        self.description = attrs.get('description')


class AtlasTypesDef(AtlasBase):
    def __init__(self, attrs=None):
        attrs = attrs or {}

        AtlasBase.__init__(self, attrs)

        self.enumDefs = attrs.get('enumDefs')
        self.structDefs = attrs.get('structDefs')
        self.classificationDefs = attrs.get('classificationDefs')
        self.entityDefs = attrs.get('entityDefs')
        self.relationshipDefs = attrs.get('relationshipDefs')
        self.businessMetadataDefs = attrs.get('businessMetadataDefs')

    def type_coerce_attrs(self):
        super(AtlasTypesDef, self).type_coerce_attrs()

        self.enumDefs = type_coerce_list(self.enumDefs, AtlasEnumDef)
        self.structDefs = type_coerce_list(self.structDefs, AtlasStructDef)
        self.classificationDefs = type_coerce_list(self.classificationDefs, AtlasClassificationDef)
        self.entityDefs = type_coerce_list(self.entityDefs, AtlasEntityDef)
        self.relationshipDefs = type_coerce_list(self.relationshipDefs, AtlasRelationshipDef)
        self.businessMetadataDefs = type_coerce_list(self.businessMetadataDefs, AtlasBusinessMetadataDef)
