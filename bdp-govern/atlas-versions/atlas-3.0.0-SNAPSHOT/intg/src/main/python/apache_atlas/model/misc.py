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
import json
import sys

from apache_atlas.utils import next_id
from apache_atlas.utils import non_null


class AtlasBase(dict):
    def __init__(self, attrs):
        pass

    def __getattr__(self, attr):
        return self.get(attr)

    def __setattr__(self, key, value):
        self.__setitem__(key, value)

    def __setitem__(self, key, value):
        super(AtlasBase, self).__setitem__(key, value)
        self.__dict__.update({key: value})

    def __delattr__(self, item):
        self.__delitem__(item)

    def __delitem__(self, key):
        super(AtlasBase, self).__delitem__(key)
        del self.__dict__[key]

    def __repr__(self):
        return json.dumps(self)

    def type_coerce_attrs(self):
        pass


class AtlasBaseModelObject(AtlasBase):
    def __init__(self, members):
        AtlasBase.__init__(self, members)

        self.guid = members.get('guid')

        if self.guid is None:
            self.guid = next_id()


class TimeBoundary(AtlasBase):
    def __init__(self, attrs=None):
        attrs = attrs or {}

        AtlasBase.__init__(self, attrs)

        self.startTime = attrs.get('startTime')
        self.endTime = attrs.get('endTime')
        self.timeZone = attrs.get('timeZone')


class Plist(AtlasBase):
    def __init__(self, attrs=None):
        attrs = attrs or {}

        AtlasBase.__init__(self, attrs)

        self.list = non_null(attrs.get('list'), [])
        self.startIndex = non_null(attrs.get('startIndex'), 0)
        self.pageSize = non_null(attrs.get('pageSize'), 0)
        self.totalCount = non_null(attrs.get('totalCount'), 0)
        self.sortBy = attrs.get('sortBy')
        self.sortType = attrs.get('sortType')


class SearchFilter(AtlasBase):
    def __init__(self, attrs=None):
        attrs = attrs or {}

        AtlasBase.__init__(self, attrs)

        self.startIndex = non_null(attrs.get('startIndex'), 0)
        self.maxsize = non_null(attrs.get('maxsize'), sys.maxsize)
        self.getCount = non_null(attrs.get('getCount'), True)
