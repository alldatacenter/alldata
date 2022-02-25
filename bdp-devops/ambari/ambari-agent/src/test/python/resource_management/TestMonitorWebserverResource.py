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
from resource_management import *
from resource_management.libraries.providers.monitor_webserver\
  import MonitorWebserverProvider
from resource_management.libraries.resources.monitor_webserver\
  import MonitorWebserver
from ambari_commons.os_check import OSCheck


class TestMonitorWebserverResource(TestCase):
  @patch.object(OSCheck, "is_suse_family")
  @patch.object(OSCheck, "is_ubuntu_family")
  @patch.object(OSCheck, "is_redhat_family")
  @patch.object(System, "os_family", new='redhat')
  def test_setup_redhat(self, is_redhat_family, is_ubuntu_family, is_suse_family):
    is_redhat_family.return_value = True
    is_ubuntu_family.return_value = False
    is_suse_family.return_value = False
    with Environment(test_mode=True) as env:
      MonitorWebserverProvider(MonitorWebserver("start")).action_start()
    defined_resources = env.resource_list
    expected_resources = '[MonitorWebserver[\'start\'], Execute[\'grep -E \'KeepAlive (On|Off)\' ' \
                         '/etc/httpd/conf/httpd.conf && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E sed -i ' \
                         '\'s/KeepAlive Off/KeepAlive On/\' /etc/httpd/conf/httpd.conf || echo \'KeepAlive On\' ' \
                         '| ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E tee --append /etc/httpd/conf/httpd.conf > /dev/null\']' \
                         ', Execute[(\'/etc/init.d/httpd\', \'start\')]]'
    self.assertEqual(str(defined_resources), expected_resources)

  @patch.object(OSCheck, "is_suse_family")
  @patch.object(OSCheck, "is_ubuntu_family")
  @patch.object(OSCheck, "is_redhat_family")
  @patch.object(System, "os_family", new='suse')
  def test_setup_suse(self, is_redhat_family, is_ubuntu_family, is_suse_family):
    is_redhat_family.return_value = False
    is_ubuntu_family.return_value = False
    is_suse_family.return_value = True
    
    with Environment(test_mode=True) as env:
      MonitorWebserverProvider(MonitorWebserver("start")).action_start()

    defined_resources = env.resource_list
    expected_resources = '[MonitorWebserver[\'start\'], Execute[\'grep -E \'KeepAlive (On|Off)\' ' \
                         '/etc/apache2/httpd.conf && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E sed -i ' \
                         '\'s/KeepAlive Off/KeepAlive On/\' /etc/apache2/httpd.conf || echo \'KeepAlive On\' ' \
                         '| ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E tee --append /etc/apache2/httpd.conf > /dev/null\'],' \
                         ' Execute[(\'/etc/init.d/apache2\', \'start\')]]'
    self.assertEqual(str(defined_resources), expected_resources)

  @patch.object(OSCheck, "is_suse_family")
  @patch.object(OSCheck, "is_ubuntu_family")
  @patch.object(OSCheck, "is_redhat_family")
  @patch.object(System, "os_family", new='redhat')
  def test_stop_redhat(self, is_redhat_family, is_ubuntu_family, is_suse_family):
    is_redhat_family.return_value = True
    is_ubuntu_family.return_value = False
    is_suse_family.return_value = False
    with Environment(test_mode=True) as env:
      MonitorWebserverProvider(MonitorWebserver("stop")).action_stop()
    defined_resources = env.resource_list
    expected_resources = '[MonitorWebserver[\'stop\'], Execute[(\'/etc/init.d/httpd\', \'stop\')]]'
    self.assertEqual(str(defined_resources), expected_resources)

  @patch.object(OSCheck, "is_suse_family")
  @patch.object(OSCheck, "is_ubuntu_family")
  @patch.object(OSCheck, "is_redhat_family")
  @patch.object(System, "os_family", new='suse')
  def test_stop_suse(self, is_redhat_family, is_ubuntu_family, is_suse_family):
    is_redhat_family.return_value = False
    is_ubuntu_family.return_value = False
    is_suse_family.return_value = True

    with Environment(test_mode=True) as env:
      MonitorWebserverProvider(MonitorWebserver("stop")).action_stop()
    defined_resources = env.resource_list
    expected_resources = '[MonitorWebserver[\'stop\'], Execute[(\'/etc/init.d/apache2\', \'stop\')]]'
    self.assertEqual(str(defined_resources), expected_resources)
