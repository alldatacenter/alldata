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


class RoleMember(RangerBase):
    def __init__(self, attrs=None):
        if attrs is None:
            attrs = {}

        RangerBase.__init__(self, attrs)

        self.name    = attrs.get('name')
        self.isAdmin = non_null(attrs.get('isAdmin'), False)


class RangerRole(RangerBaseModelObject):
    def __init__(self, attrs=None):
        if attrs is None:
            attrs = {}

        RangerBaseModelObject.__init__(self, attrs)

        self.name          = attrs.get('name')
        self.description   = attrs.get('description')
        self.options       = attrs.get('options')
        self.users         = attrs.get('users')
        self.groups        = attrs.get('groups')
        self.roles         = attrs.get('roles')
        self.createdByUser = attrs.get('createdByUser')
