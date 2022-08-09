'''
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
'''

from unittest import TestCase

from resource_management.libraries.functions.data_structure_utils import get_from_dict
from resource_management.libraries.functions.data_structure_utils import KeyNotFound


class TestDictUtils(TestCase):

  def test_get_nested(self):
    dict_ = {1: {2: {3: 'data'}}}
    empty_dict = {}

    self.assertEquals('data', get_from_dict(dict_, (1, 2, 3)))
    self.assertEquals('data', get_from_dict(dict_, [1, 2, 3]))

    self.assertEquals({3: 'data'}, get_from_dict(dict_, (1, 2)))

    self.assertEquals({2: {3: 'data'}}, get_from_dict(dict_, 1))

    self.assertEquals(KeyNotFound, get_from_dict(dict_, (1, 2, 0)))
    self.assertEquals(KeyNotFound, get_from_dict(dict_, [1, 2, 0]))
    self.assertEquals(KeyNotFound, get_from_dict(dict_, (1, 0, 3)))
    self.assertEquals(KeyNotFound, get_from_dict(dict_, (1, 2, 3, 4)))
    self.assertEquals(KeyNotFound, get_from_dict(dict_, (0, 2)))

    self.assertEquals('default', get_from_dict(dict_, (0, 2, 3), default_value='default'))
    self.assertEquals('default', get_from_dict(empty_dict, (0, 2, 3), default_value='default'))

    self.assertEquals(KeyNotFound, get_from_dict(empty_dict, [1]))