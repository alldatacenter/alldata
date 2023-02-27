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
from apache_atlas.model.misc import AtlasBase


class AtlasAdminMetrics(AtlasBase):
    def __init__(self, attrs=None):
        attrs = attrs or {}

        AtlasBase.__init__(self, attrs)

        _data = attrs.get('data', {})

        self.general = _data.get('general', {})
        self.tag = _data.get('tag', {})
        self.entity = _data.get('entity', {})
        self.system = _data.get('system', {})
