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

import json

from apache_ranger.utils import *


class RangerBase(dict):
    def __init__(self, attrs):
        pass

    def __getattr__(self, attr):
        return self.get(attr)

    def __setattr__(self, key, value):
        self.__setitem__(key, value)

    def __setitem__(self, key, value):
        super(RangerBase, self).__setitem__(key, value)
        self.__dict__.update({key: value})

    def __delattr__(self, item):
        self.__delitem__(item)

    def __delitem__(self, key):
        super(RangerBase, self).__delitem__(key)
        del self.__dict__[key]

    def __repr__(self):
        return json.dumps(self)

    def type_coerce_attrs(self):
        pass


class RangerBaseModelObject(RangerBase):
    def __init__(self, attrs=None):
        if attrs is None:
            attrs = {}

        RangerBase.__init__(self, attrs)

        self.id         = attrs.get('id')
        self.guid       = attrs.get('guid')
        self.isEnabled  = non_null(attrs.get('isEnabled'), True)
        self.createdBy  = attrs.get('createdBy')
        self.updatedBy  = attrs.get('updatedBy')
        self.createTime = attrs.get('createTime')
        self.updateTime = attrs.get('updateTime')
        self.version    = attrs.get('version')
