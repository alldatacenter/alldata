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

class RangerKey(RangerBase):
    def __init__(self, attrs=None):
        if attrs is None:
            attrs = {}

        RangerBase.__init__(self, attrs)

        self.name        = attrs.get('name')
        self.cipher      = attrs.get('cipher')
        self.material    = attrs.get('material')
        self.length      = attrs.get("length")
        self.description = attrs.get("description")
        self.attributes  = attrs.get("attributes")

class RangerKeyVersion(RangerBase):
    def __init__(self, attrs=None):
        if attrs is None:
            attrs = {}

        RangerBase.__init__(self, attrs)

        self.name        = attrs.get('name')
        self.versionName = attrs.get('versionName')
        self.material    = attrs.get('material')

class RangerKeyMetadata(RangerBase):
    def __init__(self, attrs=None):
      if attrs is None:
          attrs = {}

      RangerBase.__init__(self, attrs)

      self.cipher      = attrs.get('cipher')
      self.bitLength   = attrs.get('bitLength')
      self.description = attrs.get('description')
      self.attributes  = attrs.get('attributes')
      self.created     = attrs.get('created')
      self.versions    = attrs.get('versions')

class RangerEncryptedKeyVersion(RangerBase):
    def __init__(self, attrs=None):
      if attrs is None:
          attrs = {}

      RangerBase.__init__(self, attrs)

      self.versionName         = attrs.get('versionName')
      self.iv                  = attrs.get('iv')
      self.encryptedKeyVersion = attrs.get('encryptedKeyVersion')

    def type_coerce_attrs(self):
        super(RangerEncryptedKeyVersion, self).type_coerce_attrs()

        self.encryptedKeyVersion = type_coerce(self.encryptedKeyVersion, RangerKeyVersion)
