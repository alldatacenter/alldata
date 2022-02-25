#!/usr/bin/env python

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

from unittest import TestCase, main
from resource_management.libraries.functions.substitute_vars import substitute_vars

import StringIO, sys

class TestSubstituteVars(TestCase):
  def setUp(self):
    # disable stdout
    out = StringIO.StringIO()
    sys.stdout = out

  def test_substitute_vars(self):
    raw_config = {
      'val.intvar' : '42',
      'pass.intvar' : '${val.intvar}',
      'fail.unknown' : 'a${unknown}b',
      'fail.empty' : '${}',
      'fail.space' : '${ my.int}',
      'val.0' : 'will_fail',
      'fail.digit' : '${val.0}',
      'val.file' : 'hello',
      'val.suffix' : '.txt',
      'pass.seq.depth' : '${val.file}${val.suffix}${pass.intvar}',
      'fail.seq.depth.a' : '${val.file}${unknown}${pass.intvar}',
      'fail.seq.depth.b' : '${val.file}${fail.seq.depth.a}${pass.intvar}',
      'val.name' : 'val.intvar',
      'pass.name.as.param' : '${${val.name}}',
      'fail.inf.loop' : '${fail.inf.loop}'
    }
    expected_config = {
      'val.intvar' : '42',
      'pass.intvar' : '42',
      'fail.unknown' : 'a${unknown}b',
      'fail.empty' : '${}',
      'fail.space' : '${ my.int}',
      'val.0' : 'will_fail',
      'fail.digit' : '${val.0}',
      'val.file' : 'hello',
      'val.suffix' : '.txt',
      'pass.seq.depth' : 'hello.txt42',
      'fail.seq.depth.a' : 'hello${unknown}${pass.intvar}',
      'fail.seq.depth.b' : 'hellohello${unknown}${pass.intvar}${pass.intvar}',
      'val.name' : 'val.intvar',
      'pass.name.as.param' : '42',
      'fail.inf.loop' : '${fail.inf.loop}'
    }

    for key in raw_config.keys():
      actual_value = substitute_vars(raw_config[key], raw_config)
      expected_value = expected_config[key]

      self.assertEqual(actual_value, expected_value)

  def tearDown(self):
    # enable stdout
    sys.stdout = sys.__stdout__
