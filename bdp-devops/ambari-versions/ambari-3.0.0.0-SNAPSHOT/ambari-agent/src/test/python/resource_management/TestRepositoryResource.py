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

import os, sys
import tempfile
from unittest import TestCase
from mock.mock import patch, MagicMock
from only_for_platform import get_platform, not_for_platform, PLATFORM_WINDOWS

from ambari_commons.os_check import OSCheck

from resource_management.core.environment import Environment
from resource_management.core.source import InlineTemplate
from resource_management.core.system import System
from resource_management.libraries.resources.repository import Repository

if get_platform() != PLATFORM_WINDOWS:
  from resource_management.libraries.providers import repository

class DummyTemplate(object):

  def __init__(self, name, extra_imports=[], **kwargs):
    self._template = InlineTemplate(DummyTemplate._inline_text, extra_imports, **kwargs)
    self.context = self._template.context
    self.name = name

  def get_content(self):
    self.content = self._template.get_content()
    return self.content

  @classmethod
  def create(cls, text):
    cls._inline_text = text
    return cls

DEBIAN_DEFAUTL_TEMPLATE = "{{package_type}} {{base_url}} {{components}}\n"
RHEL_SUSE_DEFAULT_TEMPLATE ="""[{{repo_id}}]
name={{repo_id}}
{% if mirror_list %}mirrorlist={{mirror_list}}{% else %}baseurl={{base_url}}{% endif %}

path=/
enabled=1
gpgcheck=0
"""


class TestRepositoryResource(TestCase):
    @patch.object(OSCheck, "is_suse_family")
    @patch.object(OSCheck, "is_ubuntu_family")
    @patch.object(OSCheck, "is_redhat_family")
    @patch("resource_management.libraries.providers.repository.File")
    @patch("filecmp.cmp", new=MagicMock(return_value=False))
    @patch("os.path.isfile", new=MagicMock(return_value=True))
    @patch.object(System, "os_family", new='redhat')
    def test_create_repo_redhat(self, file_mock,
                                is_redhat_family, is_ubuntu_family, is_suse_family):
        is_redhat_family.return_value = True
        is_ubuntu_family.return_value = False
        is_suse_family.return_value = False
        with Environment('/') as env:
          with patch.object(repository, "__file__", new='/ambari/test/repo/dummy/path/file'):
            Repository('hadoop',
                       base_url='http://download.base_url.org/rpm/',
                       mirror_list='https://mirrors.base_url.org/?repo=Repository&arch=$basearch',
                       repo_file_name='Repository',
                       repo_template=RHEL_SUSE_DEFAULT_TEMPLATE)
                       
            Repository(None, action="create")

            self.assertTrue('hadoop' in env.resources['Repository'])
            defined_arguments = env.resources['Repository']['hadoop'].arguments
            expected_arguments = {'repo_template': RHEL_SUSE_DEFAULT_TEMPLATE,
                                  'base_url': 'http://download.base_url.org/rpm/',
                                  'mirror_list': 'https://mirrors.base_url.org/?repo=Repository&arch=$basearch',
                                  'repo_file_name': 'Repository'}
            expected_template_arguments = {'base_url': 'http://download.base_url.org/rpm/',
                                  'mirror_list': 'https://mirrors.base_url.org/?repo=Repository&arch=$basearch',
                                  'repo_file_name': 'Repository'}

            self.assertEqual(defined_arguments, expected_arguments)
            self.assertEqual(file_mock.call_args[0][0], '/etc/yum.repos.d/Repository.repo')


    @patch.object(OSCheck, "is_suse_family")
    @patch.object(OSCheck, "is_ubuntu_family")
    @patch.object(OSCheck, "is_redhat_family")
    @patch.object(System, "os_family", new='suse')
    @patch("resource_management.libraries.providers.repository.checked_call")
    @patch("os.path.isfile", new=MagicMock(return_value=True))
    @patch("filecmp.cmp", new=MagicMock(return_value=False))
    @patch("resource_management.libraries.providers.repository.File")
    def test_create_repo_suse(self, file_mock, checked_call,
                              is_redhat_family, is_ubuntu_family, is_suse_family):
        is_redhat_family.return_value = False
        is_ubuntu_family.return_value = False
        is_suse_family.return_value = True
        with Environment('/') as env:
          with patch.object(repository, "__file__", new='/ambari/test/repo/dummy/path/file'):
            Repository('hadoop',
                       base_url='http://download.base_url.org/rpm/',
                       mirror_list='https://mirrors.base_url.org/?repo=Repository&arch=$basearch',
                       repo_template = RHEL_SUSE_DEFAULT_TEMPLATE,
                       repo_file_name='Repository')
                       
            Repository(None, action="create")

            self.assertTrue('hadoop' in env.resources['Repository'])
            defined_arguments = env.resources['Repository']['hadoop'].arguments
            expected_arguments = {'repo_template': RHEL_SUSE_DEFAULT_TEMPLATE,
                                  'mirror_list': 'https://mirrors.base_url.org/?repo=Repository&arch=$basearch',
                                  'base_url': 'http://download.base_url.org/rpm/',
                                  'repo_file_name': 'Repository'}
            expected_template_arguments = {'mirror_list': 'https://mirrors.base_url.org/?repo=Repository&arch=$basearch',
                                  'base_url': 'http://download.base_url.org/rpm/',
                                  'repo_file_name': 'Repository'}

            self.assertEqual(defined_arguments, expected_arguments)
            self.assertEqual(file_mock.call_args[0][0], '/etc/zypp/repos.d/Repository.repo')


    @patch.object(OSCheck, "is_suse_family")
    @patch.object(OSCheck, "is_ubuntu_family")
    @patch.object(OSCheck, "is_redhat_family")
    @patch.object(System, "os_family", new='suse')
    @patch("resource_management.libraries.providers.repository.File")
    @patch("resource_management.libraries.providers.repository.checked_call")
    @patch("resource_management.core.sudo.read_file")
    @patch("os.path.isfile", new=MagicMock(return_value=True))
    @patch("filecmp.cmp")
    def test_recreate_repo_suse(self, filecmp_mock, read_file_mock, checked_call_mock, file_mock,
                              is_redhat_family, is_ubuntu_family, is_suse_family):
        filecmp_mock.return_value = False
        is_redhat_family.return_value = False
        is_ubuntu_family.return_value = False
        is_suse_family.return_value = True
        read_file_mock.return_value = "Dummy repo file contents"
        checked_call_mock.return_value = 0, "Flushing zypper cache"
        with Environment('/') as env:
          with patch.object(repository, "__file__", new='/ambari/test/repo/dummy/path/file'):
            # Check that zypper cache is flushed
            Repository('hadoop',
                       base_url='http://download.base_url.org/rpm/',
                       mirror_list='https://mirrors.base_url.org/?repo=Repository&arch=$basearch',
                       repo_template = RHEL_SUSE_DEFAULT_TEMPLATE,
                       repo_file_name='Repository')
            
            Repository(None, action="create")
            
            self.assertTrue(checked_call_mock.called)

            expected_repo_file_content = "[hadoop]\nname=hadoop\nmirrorlist=https://mirrors.base_url.org/?repo=Repository&arch=$basearch\n\npath=/\nenabled=1\ngpgcheck=0"
            template = file_mock.call_args_list[0][1]['content']
            self.assertEqual(expected_repo_file_content, template)

            # Check that if content is equal, zypper cache is not flushed
            checked_call_mock.reset_mock()
            filecmp_mock.return_value = True

            Repository('hadoop',
                       base_url='http://download.base_url.org/rpm/',
                       mirror_list='https://mirrors.base_url.org/?repo=Repository&arch=$basearch',
                       repo_template = RHEL_SUSE_DEFAULT_TEMPLATE,
                       repo_file_name='Repository')
            Repository(None, action="create")

            self.assertFalse(checked_call_mock.called)

            expected_repo_file_content = "[hadoop]\nname=hadoop\nmirrorlist=https://mirrors.base_url.org/?repo=Repository&arch=$basearch\n\npath=/\nenabled=1\ngpgcheck=0"
            template = file_mock.call_args_list[0][1]['content']
            self.assertEqual(expected_repo_file_content, template)


    @patch.object(OSCheck, "is_suse_family")
    @patch.object(OSCheck, "is_ubuntu_family")
    @patch.object(OSCheck, "is_redhat_family")
    @patch("resource_management.libraries.providers.repository.call")
    @patch.object(tempfile, "NamedTemporaryFile")
    @patch("resource_management.libraries.providers.repository.Execute")
    @patch("resource_management.libraries.providers.repository.File")
    @patch("os.path.isfile", new=MagicMock(return_value=True))
    @patch("filecmp.cmp", new=MagicMock(return_value=False))
    @patch.object(System, "os_release_name", new='precise')
    @patch.object(System, "os_family", new='ubuntu')
    @patch("ambari_commons.os_utils.current_user", new=MagicMock(return_value='ambari-agent'))
    def test_create_repo_ubuntu_repo_exists(self, file_mock, execute_mock,
                                            tempfile_mock, call_mock, is_redhat_family, is_ubuntu_family, is_suse_family):
      is_redhat_family.return_value = False
      is_ubuntu_family.return_value = True
      is_suse_family.return_value = False
      tempfile_mock.return_value = MagicMock(spec=file)
      tempfile_mock.return_value.__enter__.return_value.name = "/tmp/1.txt"
      call_mock.return_value = 0, "The following signatures couldn't be verified because the public key is not available: NO_PUBKEY 123ABCD"
      
      with Environment('/') as env:
        with patch.object(repository, "__file__", new='/ambari/test/repo/dummy/path/file'):
          Repository('HDP',
                     base_url='http://download.base_url.org/rpm/',
                     repo_file_name='HDP',
                     repo_template = DEBIAN_DEFAUTL_TEMPLATE,
                     components = ['a','b','c']
          )
          Repository(None, action="create")

      call_content = file_mock.call_args_list[0]
      template_name = call_content[0][0]
      template_content = call_content[1]['content']

      self.assertEquals(template_name, '/tmp/1.txt')
      self.assertEquals(template_content, 'deb http://download.base_url.org/rpm/ a b c')

      copy_item0 = str(file_mock.call_args_list[1])
      copy_item1 = str(file_mock.call_args_list[2])
      self.assertEqual(copy_item0, "call('/tmp/1.txt', owner='ambari-agent', content=StaticFile('/etc/apt/sources.list.d/HDP.list'))")
      self.assertEqual(copy_item1, "call('/etc/apt/sources.list.d/HDP.list', content=StaticFile('/tmp/1.txt'))")
      #'apt-get update -qq -o Dir::Etc::sourcelist="sources.list.d/HDP.list" -o APT::Get::List-Cleanup="0"')
      execute_command_item = execute_mock.call_args_list[0][0][0]

      self.assertEqual(call_mock.call_args_list[0][0][0], ['apt-get', 'update', '-qq', '-o', 'Dir::Etc::sourcelist=sources.list.d/HDP.list', '-o', 'Dir::Etc::sourceparts=-', '-o', 'APT::Get::List-Cleanup=0'])
      self.assertEqual(execute_command_item, ('apt-key', 'adv', '--recv-keys', '--keyserver', 'keyserver.ubuntu.com', '123ABCD'))

    @patch("resource_management.libraries.providers.repository.call")
    @patch.object(tempfile, "NamedTemporaryFile")
    @patch("resource_management.libraries.providers.repository.Execute")
    @patch("resource_management.libraries.providers.repository.File")
    @patch("os.path.isfile", new=MagicMock(return_value=True))
    @patch("filecmp.cmp", new=MagicMock(return_value=False))
    @patch.object(System, "os_release_name", new='precise')
    @patch.object(System, "os_family", new='ubuntu')
    @patch("ambari_commons.os_utils.current_user", new=MagicMock(return_value='ambari-agent'))
    def test_create_repo_ubuntu_gpg_key_wrong_output(self, file_mock, execute_mock,
                                            tempfile_mock, call_mock):
      """
      Checks that GPG key is extracted from output without \r sign
      """
      tempfile_mock.return_value = MagicMock(spec=file)
      tempfile_mock.return_value.__enter__.return_value.name = "/tmp/1.txt"
      call_mock.return_value = 0, "The following signatures couldn't be verified because the public key is not available: NO_PUBKEY 123ABCD\r\n"

      with Environment('/') as env:
        with patch.object(repository, "__file__", new='/ambari/test/repo/dummy/path/file'):
          Repository('HDP',
                     base_url='http://download.base_url.org/rpm/',
                     repo_file_name='HDP',
                     repo_template = DEBIAN_DEFAUTL_TEMPLATE,
                     components = ['a','b','c']
          )
          Repository(None, action="create")

      call_content = file_mock.call_args_list[0]
      template_name = call_content[0][0]
      template_content = call_content[1]['content']

      self.assertEquals(template_name, '/tmp/1.txt')
      self.assertEquals(template_content, 'deb http://download.base_url.org/rpm/ a b c')

      copy_item0 = str(file_mock.call_args_list[1])
      copy_item1 = str(file_mock.call_args_list[2])
      self.assertEqual(copy_item0, "call('/tmp/1.txt', owner='ambari-agent', content=StaticFile('/etc/apt/sources.list.d/HDP.list'))")
      self.assertEqual(copy_item1, "call('/etc/apt/sources.list.d/HDP.list', content=StaticFile('/tmp/1.txt'))")
      execute_command_item = execute_mock.call_args_list[0][0][0]

      self.assertEqual(call_mock.call_args_list[0][0][0], ['apt-get', 'update', '-qq', '-o', 'Dir::Etc::sourcelist=sources.list.d/HDP.list', '-o', 'Dir::Etc::sourceparts=-', '-o', 'APT::Get::List-Cleanup=0'])
      self.assertEqual(execute_command_item, ('apt-key', 'adv', '--recv-keys', '--keyserver', 'keyserver.ubuntu.com', '123ABCD'))

    @patch.object(tempfile, "NamedTemporaryFile")
    @patch("resource_management.libraries.providers.repository.Execute")
    @patch("resource_management.libraries.providers.repository.File")
    @patch("os.path.isfile", new=MagicMock(return_value=True))
    @patch("filecmp.cmp", new=MagicMock(return_value=True))
    @patch.object(System, "os_release_name", new='precise')        
    @patch.object(System, "os_family", new='ubuntu')
    def test_create_repo_ubuntu_doesnt_repo_exist(self, file_mock, execute_mock, tempfile_mock):
      tempfile_mock.return_value = MagicMock(spec=file)
      tempfile_mock.return_value.__enter__.return_value.name = "/tmp/1.txt"
      
      with Environment('/') as env:
        with patch.object(repository, "__file__", new='/ambari/test/repo/dummy/path/file'):
          Repository('HDP',
                     base_url='http://download.base_url.org/rpm/',
                     repo_file_name='HDP',
                     repo_template = DEBIAN_DEFAUTL_TEMPLATE,
                     components = ['a','b','c']
          )
          Repository(None, action="create")

      call_content = file_mock.call_args_list[0]
      template_name = call_content[0][0]
      template_content = call_content[1]['content']
      
      self.assertEquals(template_name, '/tmp/1.txt')
      self.assertEquals(template_content, 'deb http://download.base_url.org/rpm/ a b c')
      
      self.assertEqual(file_mock.call_count, 2)
      self.assertEqual(execute_mock.call_count, 0)
      
    
    @patch("os.path.isfile", new=MagicMock(return_value=True))
    @patch.object(System, "os_family", new='ubuntu')
    @patch("resource_management.libraries.providers.repository.Execute")
    @patch("resource_management.libraries.providers.repository.File")
    def test_remove_repo_ubuntu_repo_exist(self, file_mock, execute_mock):
      with Environment('/') as env:
          Repository('HDP',
                     action = "remove",
                     repo_file_name='HDP'
          )
          
      self.assertEqual(str(file_mock.call_args), "call('/etc/apt/sources.list.d/HDP.list', action='delete')")
      self.assertEqual(execute_mock.call_args[0][0], ['apt-get', 'update', '-qq', '-o', 'Dir::Etc::sourcelist=sources.list.d/HDP.list', '-o', 'Dir::Etc::sourceparts=-', '-o', 'APT::Get::List-Cleanup=0'])

    @patch("os.path.isfile", new=MagicMock(return_value=False))
    @patch.object(System, "os_family", new='ubuntu')
    @patch("resource_management.libraries.providers.repository.Execute")
    @patch("resource_management.libraries.providers.repository.File")
    def test_remove_repo_ubuntu_repo_doenst_exist(self, file_mock, execute_mock):
      with Environment('/') as env:
          Repository('HDP',
                     action = "remove",
                     repo_file_name='HDP'
          )
          
      self.assertEqual(file_mock.call_count, 0)
      self.assertEqual(execute_mock.call_count, 0)

    @patch.object(OSCheck, "is_suse_family")
    @patch.object(OSCheck, "is_ubuntu_family")
    @patch.object(OSCheck, "is_redhat_family")
    @patch.object(System, "os_family", new='redhat')
    @patch("resource_management.libraries.providers.repository.File")
    def test_remove_repo_redhat(self, file_mock,
                              is_redhat_family, is_ubuntu_family, is_suse_family):
        is_redhat_family.return_value = True
        is_ubuntu_family.return_value = False
        is_suse_family.return_value = False
        with Environment('/') as env:
            Repository('hadoop',
                       action='remove',
                       base_url='http://download.base_url.org/rpm/',
                       mirror_list='https://mirrors.base_url.org/?repo=Repository&arch=$basearch',
                       repo_file_name='Repository')        

            self.assertTrue('hadoop' in env.resources['Repository'])
            defined_arguments = env.resources['Repository']['hadoop'].arguments
            expected_arguments = {'action': ['remove'],
                                  'base_url': 'http://download.base_url.org/rpm/',
                                  'mirror_list': 'https://mirrors.base_url.org/?repo=Repository&arch=$basearch',
                                  'repo_file_name': 'Repository'}
            self.assertEqual(defined_arguments, expected_arguments)


    @patch.object(OSCheck, "is_suse_family")
    @patch.object(OSCheck, "is_ubuntu_family")
    @patch.object(OSCheck, "is_redhat_family")
    @patch.object(System, "os_family", new='suse')
    @patch("resource_management.libraries.providers.repository.File")
    def test_remove_repo_suse(self, file_mock,
                              is_redhat_family, is_ubuntu_family, is_suse_family):
        is_redhat_family.return_value = False
        is_ubuntu_family.return_value = False
        is_suse_family.return_value = True
        with Environment('/') as env:
            Repository('hadoop',
                       action='remove',
                       base_url='http://download.base_url.org/rpm/',
                       mirror_list='https://mirrors.base_url.org/?repo=Repository&arch=$basearch',
                       repo_file_name='Repository')

            self.assertTrue('hadoop' in env.resources['Repository'])
            defined_arguments = env.resources['Repository']['hadoop'].arguments
            expected_arguments = {'action': ['remove'],
                                  'base_url': 'http://download.base_url.org/rpm/',
                                  'mirror_list': 'https://mirrors.base_url.org/?repo=Repository&arch=$basearch',
                                  'repo_file_name': 'Repository'}
            self.assertEqual(defined_arguments, expected_arguments)
            self.assertEqual(file_mock.call_args[1]['action'], 'delete')
            self.assertEqual(file_mock.call_args[0][0], '/etc/zypp/repos.d/Repository.repo')
