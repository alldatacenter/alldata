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

import base64
import getpass
import os
import stat
from ambari_commons.constants import AGENT_TMP_DIR
from collections import namedtuple
from resource_management.core import sudo
from resource_management.core.logger import Logger
from resource_management.core.resources.klist import Klist
from resource_management.core.resources.system import Directory, File
from resource_management.core.source import InlineTemplate
from tempfile import gettempdir
from .utils import get_property_value

KRB5_REALM_PROPERTIES = [
  'kdc',
  'admin_server',
  'default_domain',
  'master_kdc'
]


class MissingKeytabs(object):
  class Identity(namedtuple('Identity', ['principal', 'keytab_file_path'])):
    @staticmethod
    def from_kerberos_record(item, hostname):
      return MissingKeytabs.Identity(
        get_property_value(item, 'principal').replace("_HOST", hostname),
        get_property_value(item, 'keytab_file_path'))

    def __str__(self):
      return "Keytab: %s Principal: %s" % (self.keytab_file_path, self.principal)

  @classmethod
  def from_kerberos_records(self, kerberos_record, hostname):
    if kerberos_record is not None:
      with_missing_keytab = (each for each in kerberos_record \
                           if not self.keytab_exists(each) or not self.keytab_has_principal(each, hostname))
      return MissingKeytabs(
        set(MissingKeytabs.Identity.from_kerberos_record(each, hostname) for each in with_missing_keytab))
    else:
      return MissingKeytabs(None)

  @staticmethod
  def keytab_exists(kerberos_record):
    return sudo.path_exists(get_property_value(kerberos_record, 'keytab_file_path'))

  @staticmethod
  def keytab_has_principal(kerberos_record, hostname):
    principal = get_property_value(kerberos_record, 'principal').replace("_HOST", hostname)
    keytab = get_property_value(kerberos_record, 'keytab_file_path')
    klist = Klist.find_in_search_path()
    return principal in klist.list_principals(keytab)

  def __init__(self, items):
    self.items = items

  def as_dict(self):
    return [each._asdict() for each in self.items] if self.items is not None else []

  def __str__(self):
    return "Missing keytabs:\n%s" % ("\n".join(map(str, self.items))) if self.items and self.items is not None else 'No missing keytabs'


def write_krb5_conf(params):
  Directory(params.krb5_conf_dir,
            owner='root',
            create_parents=True,
            group='root',
            mode=0755
            )

  content = InlineTemplate(params.krb5_conf_template)

  File(params.krb5_conf_path,
       content=content,
       owner='root',
       group='root',
       mode=0644
       )


def clear_tmp_cache():
  tmp_dir = AGENT_TMP_DIR
  if tmp_dir is None:
    tmp_dir = gettempdir()
  curl_krb_cache_path = os.path.join(tmp_dir, "curl_krb_cache")
  Directory(curl_krb_cache_path, action="delete")


def write_keytab_file(params, output_hook=lambda principal, keytab_file_path: None):
  if params.kerberos_command_params is not None:
    for item in params.kerberos_command_params:
      keytab_content_base64 = get_property_value(item, 'keytab_content_base64')
      if (keytab_content_base64 is not None) and (len(keytab_content_base64) > 0):
        keytab_file_path = get_property_value(item, 'keytab_file_path')
        if (keytab_file_path is not None) and (len(keytab_file_path) > 0):
          head, tail = os.path.split(keytab_file_path)
          if head:
            Directory(head, create_parents=True, mode=0755, owner="root", group="root")

          owner = get_property_value(item, 'keytab_file_owner_name')
          if not owner:
            owner = getpass.getuser()
          owner_access = get_property_value(item, 'keytab_file_owner_access')
          group = get_property_value(item, 'keytab_file_group_name')
          group_access = get_property_value(item, 'keytab_file_group_access')
          mode = 0

          if owner_access == 'rw':
            mode |= stat.S_IREAD | stat.S_IWRITE
          else:
            mode |= stat.S_IREAD

          if group_access == 'rw':
            mode |= stat.S_IRGRP | stat.S_IWGRP
          elif group_access == 'r':
            mode |= stat.S_IRGRP

          keytab_content = base64.b64decode(keytab_content_base64)

          # to hide content in command output
          def make_lambda(data):
            return lambda: data

          File(keytab_file_path,
               content=make_lambda(keytab_content),
               mode=mode,
               owner=owner,
               group=group)

          principal = get_property_value(item, 'principal')

          output_hook(principal, keytab_file_path)


def delete_keytab_file(params, output_hook=lambda principal, keytab_file_path: None):
  if params.kerberos_command_params is not None:
    for item in params.kerberos_command_params:
      keytab_file_path = get_property_value(item, 'keytab_file_path')
      if (keytab_file_path is not None) and (len(keytab_file_path) > 0):
        # Delete the keytab file
        File(keytab_file_path, action="delete")

        principal = get_property_value(item, 'principal')
        output_hook(principal, keytab_file_path)


def find_missing_keytabs(params, output_hook=lambda missing_keytabs: None):
  missing_keytabs = MissingKeytabs.from_kerberos_records(params.kerberos_command_params, params.hostname)
  Logger.info(str(missing_keytabs))
  output_hook(missing_keytabs.as_dict())

# Encryption families from: http://web.mit.edu/KERBEROS/krb5-latest/doc/admin/conf_files/kdc_conf.html#encryption-types
ENCRYPTION_FAMILY_MAP = {
  'aes'       : ['aes256-cts-hmac-sha1-96', 'aes128-cts-hmac-sha1-96', 'aes256-cts-hmac-sha384-192', 'aes128-cts-hmac-sha256-128'],
  'rc4'       : ['rc4-hmac'],
  'camellia'  : ['camellia256-cts-cmac', 'camellia128-cts-cmac'],
  'des3'      : ['des3-cbc-sha1'],
  'des'       : ['des-cbc-crc', 'des-cbc-md5', 'des-cbc-md4']
}

def resolve_encryption_family_list(enc_types_list):
  result = []
  for each in enc_types_list:
    result.extend(ENCRYPTION_FAMILY_MAP[each] if each in ENCRYPTION_FAMILY_MAP else [each])
  return set(result)

def resolve_encryption_families(enc_types_str):
  return None if enc_types_str is None \
    else ' '.join(resolve_encryption_family_list(enc_types_str.split()))