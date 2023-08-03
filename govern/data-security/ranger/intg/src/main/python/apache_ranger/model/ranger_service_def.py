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

from apache_ranger.model.ranger_base import RangerBase, RangerBaseModelObject
from apache_ranger.utils             import *


class RangerServiceDef(RangerBaseModelObject):
    def __init__(self, attrs=None):
        if attrs is None:
            attrs = {}

        RangerBaseModelObject.__init__(self, attrs)

        self.name             = attrs.get('name')
        self.displayName      = attrs.get('displayName')
        self.implClass        = attrs.get('implClass')
        self.label            = attrs.get('label')
        self.description      = attrs.get('description')
        self.rbKeyLabel       = attrs.get('rbKeyLabel')
        self.rbKeyDescription = attrs.get('rbKeyDescription')
        self.options          = attrs.get('options')
        self.configs          = attrs.get('configs')
        self.resources        = attrs.get('resources')
        self.accessTypes      = attrs.get('accessTypes')
        self.policyConditions = attrs.get('policyConditions')
        self.contextEnrichers = attrs.get('contextEnrichers')
        self.enums            = attrs.get('enums')
        self.dataMaskDef      = attrs.get('dataMaskDef')
        self.rowFilterDef     = attrs.get('rowFilterDef')

    def type_coerce_attrs(self):
        super(RangerServiceDef, self).type_coerce_attrs()

        self.configs          = type_coerce_list(self.configs, RangerResourceDef)
        self.resources        = type_coerce_list(self.resources, RangerResourceDef)
        self.accessTypes      = type_coerce_list(self.accessTypes, RangerAccessTypeDef)
        self.policyConditions = type_coerce_list(self.policyConditions, RangerPolicyConditionDef)
        self.contextEnrichers = type_coerce_list(self.contextEnrichers, RangerContextEnricherDef)
        self.enums            = type_coerce_list(self.enums, RangerEnumDef)
        self.dataMaskDef      = type_coerce(self.dataMaskDef, RangerDataMaskDef)
        self.rowFilterDef     = type_coerce(self.rowFilterDef, RangerRowFilterDef)


class RangerServiceConfigDef:
    def __init__(self, attrs=None):
        if attrs is None:
            attrs = {}

        RangerBase.__init__(self, attrs)

        self.itemId                 = attrs.get('itemId')
        self.name                   = attrs.get('name')
        self.type                   = attrs.get('type')
        self.subType                = attrs.get('subType')
        self.mandatory              = attrs.get('mandatory')
        self.defaultValue           = attrs.get('defaultValue')
        self.validationRegEx        = attrs.get('validationRegEx')
        self.validationMessage      = attrs.get('validationMessage')
        self.uiHint                 = attrs.get('uiHint')
        self.label                  = attrs.get('label')
        self.description            = attrs.get('description')
        self.rbKeyLabel             = attrs.get('rbKeyLabel')
        self.rbKeyDescription       = attrs.get('rbKeyDescription')
        self.rbKeyValidationMessage = attrs.get('rbKeyValidationMessage')


class RangerResourceDef(RangerBase):
    def __init__(self, attrs=None):
        if attrs is None:
            attrs = {}

        RangerBase.__init__(self, attrs)

        self.itemId                 = attrs.get('itemId')
        self.name                   = attrs.get('name')
        self.type                   = attrs.get('type')
        self.level                  = non_null(attrs.get('level'), 1)
        self.parent                 = attrs.get('parent')
        self.mandatory              = attrs.get('mandatory')
        self.lookupSupported        = attrs.get('lookupSupported')
        self.recursiveSupported     = attrs.get('recursiveSupported')
        self.excludesSupported      = attrs.get('excludesSupported')
        self.matcher                = attrs.get('matcher')
        self.matcherOptions         = attrs.get('matcherOptions')
        self.validationRegEx        = attrs.get('validationRegEx')
        self.validationMessage      = attrs.get('validationMessage')
        self.uiHint                 = attrs.get('uiHint')
        self.label                  = attrs.get('label')
        self.description            = attrs.get('description')
        self.rbKeyLabel             = attrs.get('rbKeyLabel')
        self.rbKeyDescription       = attrs.get('rbKeyDescription')
        self.rbKeyValidationMessage = attrs.get('rbKeyValidationMessage')
        self.accessTypeRestrictions = attrs.get('accessTypeRestrictions')
        self.isValidLeaf            = attrs.get('isValidLeaf')


class RangerAccessTypeDef(RangerBase):
    def __init__(self, attrs=None):
        if attrs is None:
            attrs = {}

        RangerBase.__init__(self, attrs)

        self.itemId        = attrs.get('itemId')
        self.name          = attrs.get('name')
        self.label         = attrs.get('label')
        self.rbKeyLabel    = attrs.get('rbKeyLabel')
        self.impliedGrants = attrs.get('impliedGrants')


class RangerPolicyConditionDef(RangerBase):
    def __init__(self, attrs=None):
        if attrs is None:
            attrs = {}

        RangerBase.__init__(self, attrs)

        self.itemId                 = attrs.get('itemId')
        self.name                   = attrs.get('name')
        self.evaluator              = attrs.get('evaluator')
        self.evaluatorOptions       = attrs.get('evaluatorOptions')
        self.validationRegEx        = attrs.get('validationRegEx')
        self.validationMessage      = attrs.get('validationMessage')
        self.uiHint                 = attrs.get('uiHint')
        self.label                  = attrs.get('label')
        self.description            = attrs.get('description')
        self.rbKeyLabel             = attrs.get('rbKeyLabel')
        self.rbKeyDescription       = attrs.get('rbKeyDescription')
        self.rbKeyValidationMessage = attrs.get('rbKeyValidationMessage')


class RangerContextEnricherDef(RangerBase):
    def __init__(self, attrs=None):
        if attrs is None:
            attrs = {}

        RangerBase.__init__(self, attrs)

        self.itemId         = attrs.get('itemId')
        self.name           = attrs.get('name')
        self.enricher       = attrs.get('enricher')
        self.enricherOptions = attrs.get('enricherOptions')


class RangerEnumDef(RangerBase):
    def __init__(self, attrs=None):
        if attrs is None:
            attrs = {}

        RangerBase.__init__(self, attrs)

        self.itemId       = attrs.get('itemId')
        self.name         = attrs.get('name')
        self.elements     = attrs.get('elements')
        self.defaultIndex = attrs.get('defaultIndex')

    def type_coerce_attrs(self):
        super(RangerEnumDef, self).type_coerce_attrs()

        self.elements = type_coerce_list(self.resources, RangerEnumElementDef)


class RangerDataMaskDef(RangerBase):
    def __init__(self, attrs=None):
        if attrs is None:
            attrs = {}

        RangerBase.__init__(self, attrs)

        self.maskTypes   = attrs.get('maskTypes')
        self.accessTypes = attrs.get('accessTypes')
        self.resources   = attrs.get('resources')

    def type_coerce_attrs(self):
        super(RangerDataMaskDef, self).type_coerce_attrs()

        self.maskTypes   = type_coerce_list(self.maskTypes, RangerDataMaskTypeDef)
        self.accessTypes = type_coerce_list(self.accessTypes, RangerAccessTypeDef)
        self.resources   = type_coerce_list(self.resources, RangerResourceDef)


class RangerRowFilterDef(RangerBase):
    def __init__(self, attrs=None):
        if attrs is None:
            attrs = {}

        RangerBase.__init__(self, attrs)

        self.accessTypes = attrs.get('accessTypes')
        self.resources   = attrs.get('resources')

    def type_coerce_attrs(self):
        super(RangerRowFilterDef, self).type_coerce_attrs()

        self.accessTypes = type_coerce_list(self.accessTypes, RangerAccessTypeDef)
        self.resources   = type_coerce_list(self.resources, RangerResourceDef)


class RangerEnumElementDef(RangerBase):
    def __init__(self, attrs=None):
        if attrs is None:
            attrs = {}

        RangerBase.__init__(self, attrs)

        self.itemId     = attrs.get('itemId')
        self.name       = attrs.get('name')
        self.label      = attrs.get('label')
        self.rbKeyLabel = attrs.get('rbKeyLabel')


class RangerDataMaskTypeDef(RangerBase):
    def __init__(self, attrs=None):
        if attrs is None:
            attrs = {}

        RangerBase.__init__(self, attrs)

        self.itemId           = attrs.get('itemId')
        self.name             = attrs.get('name')
        self.label            = attrs.get('label')
        self.description      = attrs.get('description')
        self.transformer      = attrs.get('transformer')
        self.dataMaskOptions  = attrs.get('dataMaskOptions')
        self.rbKeyLabel       = attrs.get('rbKeyLabel')
        self.rbKeyDescription = attrs.get('rbKeyDescription')
