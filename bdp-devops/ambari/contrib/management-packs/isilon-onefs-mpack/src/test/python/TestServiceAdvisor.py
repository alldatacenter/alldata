"""
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
"""

from unittest import TestCase
from service_advisor import Uri, CoreSite, HdfsSite

configs = {
  'configurations': {
    'core-site': {
      'properties': {
        'fs.defaultFS': 'hdfs://localhost:8020'
      }
    },
    'hdfs-site': {
      'properties': {
        'dfs.namenode.http-address'  : 'scisilon.fqdn:8082',
        'dfs.namenode.https-address' : 'scisilon.fqdn:8080',
      }
    },
    'onefs': {
      'properties': {
        'onefs_host': 'scisilon.fqdn'
      }
    }
  }
}

class TestUri(TestCase):
  def test_fix_host(self):
    onefs_host = Uri.onefs(configs)
    default_fs = Uri.default_fs(configs)
    fixed = default_fs.fix_host(onefs_host)
    self.assertEquals('hdfs://scisilon.fqdn:8020', fixed)

  def test_skip_replacing_to_empty_host(self):
    default_fs = Uri.default_fs(configs)
    self.assertEquals('hdfs://localhost:8020', default_fs.fix_host(Uri("")))

  def test_skip_fixing_invalid_host(self):
    default_fs = Uri("hdfs://:8080")
    self.assertEquals('hdfs://:8080', default_fs.fix_host(Uri("host")))

  def test_core_site_validation_error_on_host_mismatch(self):
    core_site = CoreSite(configs)
    erros = core_site.validate()
    self.assertEquals(len(erros), 1)
    self.assertEquals(erros[0]['config-name'], 'fs.defaultFS')

  def test_hdfs_site_no_validation_error(self):
    hdfs_site = HdfsSite(configs)
    erros = hdfs_site.validate()
    self.assertEquals(len(erros), 0)