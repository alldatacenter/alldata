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

from apache_ranger.model.ranger_base import RangerBaseModelObject


class RangerService(RangerBaseModelObject):
    def __init__(self, attrs=None):
        if attrs is None:
            attrs = {}

        RangerBaseModelObject.__init__(self, attrs)

        self.type             = attrs.get('type')
        self.name             = attrs.get('name')
        self.displayName      = attrs.get('displayName')
        self.description      = attrs.get('description')
        self.tagService       = attrs.get('tagService')
        self.configs          = attrs.get('configs')
        self.policyVersion    = attrs.get('policyVersion')
        self.policyUpdateTime = attrs.get('policyUpdateTime')
        self.tagVersion       = attrs.get('tagVersion')
        self.tagUpdateTime    = attrs.get('tagUpdateTime')


class RangerServiceHeaderInfo(RangerBaseModelObject):
    def __init__(self, attrs=None):
        if attrs is None:
            attrs = {}

        RangerBaseModelObject.__init__(self, attrs)

        self.name         = attrs.get('name')
        self.isTagService = attrs.get('isTagService')
