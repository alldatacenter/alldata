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
from mock.mock import patch, MagicMock

from only_for_platform import get_platform, not_for_platform, os_distro_value, PLATFORM_WINDOWS

from ambari_commons.os_check import OSCheck

from resource_management import *
from resource_management.libraries.resources.template_config \
  import TemplateConfig

@patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
class TestTemplateConfigResource(TestCase):

  @patch("resource_management.libraries.providers.template_config.Template")
  @patch("resource_management.libraries.providers.template_config.File")
  def test_create_template_wo_tag(self, file_mock, template_mock):
    with Environment() as env:
      TemplateConfig("path",
                     action="create",
                     mode=0755,
                     owner="owner",
                     group="group",
                     extra_imports=["extra_imports"]
      )
      defined_arguments = env.resources['TemplateConfig']['path'].arguments
      expected_arguments = {'group': 'group', 'extra_imports': ['extra_imports'], 'action': ['create'], 'mode': 0755, 'owner': 'owner'}
      self.assertEqual(defined_arguments,expected_arguments)
      self.assertEqual(file_mock.call_args[0][0],'path')
      call_args = file_mock.call_args[1].copy()
      del call_args['content']
      self.assertEqual(call_args,{'owner': 'owner', 'group': 'group', 'mode': 0755})
      self.assertEqual(template_mock.call_args[0][0],'path.j2')
      self.assertEqual(template_mock.call_args[1],{'extra_imports': ['extra_imports']})


  @patch("resource_management.libraries.providers.template_config.Template")
  @patch("resource_management.core.providers.system.FileProvider")
  def test_create_template_with_tag(self, file_mock, template_mock):
    with Environment("/") as env:
      TemplateConfig("path",
                     action="create",
                     template_tag="template_tag"
      )
      self.assertEqual(template_mock.call_args[0][0],'path-template_tag.j2')