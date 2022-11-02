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

import os

if get_platform() != PLATFORM_WINDOWS:
  with patch.object(os, "geteuid", return_value=0):
    from resource_management.core import sudo
    reload(sudo)

from ambari_commons.os_check import OSCheck

from resource_management.core import Environment
from resource_management.core.system import System
from resource_management.core.source import StaticFile
from resource_management.core.source import DownloadSource
from resource_management.core.source import Template
from resource_management.core.source import InlineTemplate

from ambari_jinja2 import UndefinedError, TemplateNotFound
import urllib2


@patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
class TestContentSources(TestCase):

  @patch.object(os.path, "isfile")
  @patch.object(os.path, "join")
  def test_static_file_absolute_path(self, join_mock, is_file_mock):
    """
    Testing StaticFile source with absolute path
    """
    sudo.read_file = lambda path: 'content'
    is_file_mock.return_value = True

    with Environment("/base") as env:
      static_file = StaticFile("/absolute/path/file")
      content = static_file.get_content()

    self.assertEqual('content', content)
    self.assertEqual(is_file_mock.call_count, 1)
    self.assertEqual(join_mock.call_count, 0)


  @patch.object(os.path, "isfile")
  @patch.object(os.path, "join")
  def test_static_file_relative_path(self, join_mock, is_file_mock):
    """
    Testing StaticFile source with relative path
    """
    sudo.read_file = lambda path: 'content'
    is_file_mock.return_value = True

    with Environment("/base") as env:
      static_file = StaticFile("relative/path/file")
      content = static_file.get_content()

    self.assertEqual('content', content)
    self.assertEqual(is_file_mock.call_count, 1)
    self.assertEqual(join_mock.call_count, 1)
    join_mock.assert_called_with('/base', 'files', 'relative/path/file')

  @patch.object(urllib2, "build_opener")
  @patch.object(urllib2, "Request")
  @patch.object(os.path, "exists")
  def test_download_source_get_content_nocache(self, exists_mock, request_mock, opener_mock):
    """
    Testing DownloadSource.get_content without cache
    """
    exists_mock.return_value = True
    web_file_mock = MagicMock()
    web_file_mock.read.return_value = 'web_content'
    opener_mock.return_value.open = MagicMock(return_value=web_file_mock)

    with Environment("/base", tmp_dir='/var/tmp/downloads') as env:
      download_source = DownloadSource("http://download/source", redownload_files=True)
    content = download_source.get_content()

    self.assertEqual('web_content', content)
    self.assertEqual(opener_mock.call_count, 1)
    request_mock.assert_called_with('http://download/source')
    self.assertEqual(web_file_mock.read.call_count, 1)

  @patch("__builtin__.open")
  @patch.object(urllib2, "Request")
  @patch.object(urllib2, "build_opener")
  @patch.object(os, "makedirs")
  @patch.object(os.path, "exists")
  @patch("resource_management.core.sudo.create_file")
  def test_download_source_get_content_cache_new(self, create_mock, exists_mock, makedirs_mock, opener_mock, request_mock, open_mock):
    """
    Testing DownloadSource.get_content with cache on non-cached resource
    """
    exists_mock.side_effect = [True, False]
    web_file_mock = MagicMock()
    web_file_mock.read.return_value = 'web_content'
    opener_mock.return_value.open = MagicMock(return_value=web_file_mock)
    
    file_mock = MagicMock(name = 'file_mock')
    file_mock.__enter__.return_value = file_mock
    file_mock.read.return_value = 'content'
    open_mock.return_value = file_mock

    with Environment("/base", tmp_dir='/var/tmp/downloads') as env:
      download_source = DownloadSource("http://download/source", redownload_files=False)
    content = download_source.get_content()

    self.assertEqual('web_content', content)
    self.assertEqual(opener_mock.call_count, 1)
    request_mock.assert_called_with('http://download/source')
    self.assertEqual(web_file_mock.read.call_count, 1)

  @patch("__builtin__.open")
  @patch.object(os.path, "exists")
  def test_download_source_get_content_cache_existent(self, exists_mock, open_mock):
    """
    Testing DownloadSource.get_content with cache on cached resource
    """
    exists_mock.side_effect = [True, True]

    file_mock = MagicMock(name = 'file_mock')
    file_mock.__enter__.return_value = file_mock
    file_mock.read.return_value = 'cached_content'
    open_mock.return_value = file_mock


    with Environment("/base", tmp_dir='/var/tmp/downloads') as env:
      download_source = DownloadSource("http://download/source", redownload_files=False)
    content = download_source.get_content()

    self.assertEqual('cached_content', content)
    self.assertEqual(open_mock.call_count, 1)
    self.assertEqual(file_mock.read.call_count, 1)
    
  @patch("__builtin__.open")
  @patch.object(os.path, "getmtime")
  @patch.object(os.path, "exists")
  def test_template_loader(self, exists_mock, getmtime_mock, open_mock):
    """
    Testing template loader on existent file
    """
    exists_mock.return_value = True
    getmtime_mock.return_value = 10
    file_mock = MagicMock(name = 'file_mock')
    file_mock.__enter__.return_value = file_mock
    file_mock.read.return_value = 'template content'
    open_mock.return_value = file_mock

    with Environment("/base") as env:
      template = Template("test.j2")

    self.assertEqual(open_mock.call_count, 1)
    open_mock.assert_called_with('/base/templates/test.j2', 'rb')
    self.assertEqual(getmtime_mock.call_count, 1)
    getmtime_mock.assert_called_with('/base/templates/test.j2')

  @patch.object(os.path, "exists")
  def test_template_loader_fail(self, exists_mock):
    """
    Testing template loader on non-existent file
    """
    exists_mock.return_value = False

    try:
      with Environment("/base") as env:
        template = Template("test.j2")
      self.fail("Template should fail with nonexistent template file")
    except TemplateNotFound:
      pass



  @patch("__builtin__.open")
  @patch.object(os.path, "getmtime")
  @patch.object(os.path, "exists")
  def test_template_loader_absolute_path(self, exists_mock, getmtime_mock, open_mock):
    """
    Testing template loader with absolute file-path
    """
    exists_mock.return_value = True
    getmtime_mock.return_value = 10
    file_mock = MagicMock(name = 'file_mock')
    file_mock.__enter__.return_value = file_mock
    file_mock.read.return_value = 'template content'
    open_mock.return_value = file_mock

    with Environment("/base") as env:
      template = Template("/absolute/path/test.j2")

    self.assertEqual(open_mock.call_count, 1)
    open_mock.assert_called_with('/absolute/path/test.j2', 'rb')
    self.assertEqual(getmtime_mock.call_count, 1)
    getmtime_mock.assert_called_with('/absolute/path/test.j2')

  @patch("__builtin__.open")
  @patch.object(os.path, "getmtime")
  @patch.object(os.path, "exists")
  def test_template_loader_arguments(self, exists_mock, getmtime_mock, open_mock):
    """
    Testing template loader additional arguments in template and absolute file-path
    """
    exists_mock.return_value = True
    getmtime_mock.return_value = 10
    file_mock = MagicMock(name = 'file_mock')
    file_mock.__enter__.return_value = file_mock
    file_mock.read.return_value = '{{test_arg1}} template content'
    open_mock.return_value = file_mock

    with Environment("/base") as env:
      template = Template("/absolute/path/test.j2", [], test_arg1 = "test")
      content = template.get_content()
    self.assertEqual(open_mock.call_count, 1)

    self.assertEqual(u'test template content', content)
    open_mock.assert_called_with('/absolute/path/test.j2', 'rb')
    self.assertEqual(getmtime_mock.call_count, 1)
    getmtime_mock.assert_called_with('/absolute/path/test.j2')

  def test_inline_template(self):
    """
    Testing InlineTemplate
    """
    with Environment("/base") as env:
      template = InlineTemplate("{{test_arg1}} template content", [], test_arg1 = "test")
      content = template.get_content()

    self.assertEqual(u'test template content', content)

  def test_template_imports(self):
    """
    Testing Template additional imports
    """
    try:
      with Environment("/base") as env:
        template = InlineTemplate("{{test_arg1}} template content {{os.path.join(path[0],path[1])}}", [], test_arg1 = "test", path = ["/one","two"])
        content = template.get_content()
        self.fail("Template.get_content should fail when evaluating unknown import")
    except UndefinedError:
      pass
    with Environment("/base") as env:
      template = InlineTemplate("{{test_arg1}} template content {{os.path.join(path[0],path[1])}}", [os], test_arg1 = "test", path = ["/one","two"])
      content = template.get_content()
    self.assertEqual(u'test template content /one/two', content)
