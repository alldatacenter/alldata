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


from apache_ranger.model.ranger_base   import *
from apache_ranger.model.ranger_policy import RangerValiditySchedule
from apache_ranger.utils               import *


class RangerTag(RangerBaseModelObject):
    def __init__(self, attrs=None):
        if attrs is None:
            attrs = {}

        RangerBaseModelObject.__init__(self, attrs)

        self.type            = attrs.get('type')
        self.attributes      = attrs.get('attributes')
        self.options         = attrs.get('options')
        self.validityPeriods = attrs.get('validityPeriods')

    def type_coerce_attrs(self):
        super(RangerTag, self).type_coerce_attrs()

        self.validityPeriods = type_coerce_list(self.validitySchedules, RangerValiditySchedule)
