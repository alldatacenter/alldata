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


from apache_ranger.model.ranger_base import *
from apache_ranger.utils             import *


class RangerPolicy(RangerBaseModelObject):
    POLICY_TYPE_ACCESS    = 0
    POLICY_TYPE_DATAMASK  = 1
    POLICY_TYPE_ROWFILTER = 2

    def __init__(self, attrs=None):
        if attrs is None:
            attrs = {}

        RangerBaseModelObject.__init__(self, attrs)

        self.service              = attrs.get('service')
        self.name                 = attrs.get('name')
        self.policyType           = attrs.get('policyType')
        self.policyPriority       = attrs.get('policyPriority')
        self.description          = attrs.get('description')
        self.resourceSignature    = attrs.get('resourceSignature')
        self.isAuditEnabled       = attrs.get('isAuditEnabled')
        self.resources            = attrs.get('resources')
        self.additionalResources  = attrs.get('additionalResources')
        self.policyItems          = attrs.get('policyItems')
        self.denyPolicyItems      = attrs.get('denyPolicyItems')
        self.allowExceptions      = attrs.get('allowExceptions')
        self.denyExceptions       = attrs.get('denyExceptions')
        self.dataMaskPolicyItems  = attrs.get('dataMaskPolicyItems')
        self.rowFilterPolicyItems = attrs.get('rowFilterPolicyItems')
        self.serviceType          = attrs.get('serviceType')
        self.options              = attrs.get('options')
        self.validitySchedules    = attrs.get('validitySchedules')
        self.policyLabels         = attrs.get('policyLabels')
        self.zoneName             = attrs.get('zoneName')
        self.conditions           = attrs.get('conditions')
        self.isDenyAllElse        = non_null(attrs.get('isDenyAllElse'), False)

    def type_coerce_attrs(self):
        super(RangerPolicy, self).type_coerce_attrs()

        self.resources            = type_coerce_dict(self.resources, RangerPolicyResource)
        self.additionalResources  = type_coerce_list(self.additionalResources, dict)
        self.policyItems          = type_coerce_list(self.policyItems, RangerPolicyItem)
        self.denyPolicyItems      = type_coerce_list(self.denyPolicyItems, RangerPolicyItem)
        self.allowExceptions      = type_coerce_list(self.allowExceptions, RangerPolicyItem)
        self.denyExceptions       = type_coerce_list(self.denyExceptions, RangerPolicyItem)
        self.dataMaskPolicyItems  = type_coerce_list(self.dataMaskPolicyItems, RangerDataMaskPolicyItem)
        self.rowFilterPolicyItems = type_coerce_list(self.rowFilterPolicyItems, RangerRowFilterPolicyItem)
        self.validitySchedules    = type_coerce_list(self.validitySchedules, RangerValiditySchedule)

        if isinstance(self.additionalResources, list):
            additionalResources = []

            for entry in self.additionalResources:
                additionalResources.append(type_coerce_dict(entry, RangerPolicyResource))

            self.additionalResources = additionalResources
        else:
            self.additionalResources = None

    def add_resource(self, resource):
        if resource is not None:
            if self.resources is None:
                self.resources = resource
            else:
                if self.additionalResources is None:
                    self.additionalResources = []

                self.additionalResources.append(resource)

class RangerPolicyResource(RangerBase):
    def __init__(self, attrs=None):
        if attrs is None:
            attrs = {}

        RangerBase.__init__(self, attrs)

        self.values      = attrs.get('values')
        self.isExcludes  = non_null(attrs.get('isExcludes'), False)
        self.isRecursive = non_null(attrs.get('isRecursive'), False)


class RangerPolicyItemCondition(RangerBase):
    def __init__(self, attrs=None):
        if attrs is None:
            attrs = {}

        RangerBase.__init__(self, attrs)

        self.type   = attrs.get('type')
        self.values = attrs.get('values')


class RangerPolicyItem(RangerBase):
    def __init__(self, attrs=None):
        if attrs is None:
            attrs = {}

        RangerBase.__init__(self, attrs)

        self.accesses      = attrs.get('accesses')
        self.users         = attrs.get('users')
        self.groups        = attrs.get('groups')
        self.roles         = attrs.get('roles')
        self.conditions    = attrs.get('conditions')
        self.delegateAdmin = non_null(attrs.get('delegateAdmin'), False)

    def type_coerce_attrs(self):
        super(RangerPolicyItem, self).type_coerce_attrs()

        self.accesses = type_coerce_list(self.accesses, RangerPolicyItemAccess)


class RangerDataMaskPolicyItem(RangerPolicyItem):
    def __init__(self, attrs=None):
        if attrs is None:
            attrs = {}

        RangerPolicyItem.__init__(self, attrs)

        self.dataMaskInfo = attrs.get('dataMaskInfo')

    def type_coerce_attrs(self):
        super(RangerDataMaskPolicyItem, self).type_coerce_attrs()

        self.dataMaskInfo = type_coerce(self.dataMaskInfo, RangerPolicyItemDataMaskInfo)


class RangerRowFilterPolicyItem(RangerPolicyItem):
    def __init__(self, attrs=None):
        if attrs is None:
            attrs = {}

        RangerPolicyItem.__init__(self, attrs)

        self.rowFilterInfo = attrs.get('rowFilterInfo')

    def type_coerce_attrs(self):
        super(RangerRowFilterPolicyItem, self).type_coerce_attrs()

        self.rowFilterInfo = type_coerce(self.rowFilterInfo, RangerPolicyItemRowFilterInfo)


class RangerValiditySchedule(RangerBase):
    def __init__(self, attrs=None):
        if attrs is None:
            attrs = {}

        RangerBase.__init__(self, attrs)

        self.startTime = attrs.get('startTime')
        self.endTime   = attrs.get('endTime')
        self.timeZone  = attrs.get('timeZone')


class RangerPolicyItemAccess(RangerBase):
    def __init__(self, attrs=None):
        if attrs is None:
            attrs = {}

        RangerBase.__init__(self, attrs)

        self.type      = attrs.get('type')
        self.isAllowed = non_null(attrs.get('isAllowed'), True)


class RangerPolicyItemDataMaskInfo(RangerBase):
    def __init__(self, attrs=None):
        if attrs is None:
            attrs = {}

        RangerBase.__init__(self, attrs)

        self.dataMaskType  = attrs.get('dataMaskType')
        self.conditionExpr = attrs.get('conditionExpr')
        self.valueExpr     = attrs.get('valueExpr')


class RangerPolicyItemRowFilterInfo(RangerBase):
    def __init__(self, attrs=None):
        if attrs is None:
            attrs = {}

        RangerBase.__init__(self, attrs)

        self.filterExpr = attrs.get('filterExpr')
