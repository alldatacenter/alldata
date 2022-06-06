#!/usr/bin/env python
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

import os
import re

from resource_management.core.resources.system import File, Execute
from resource_management.core.shell import checked_call
from resource_management.core.source import DownloadSource
from resource_management.core.utils import PasswordString

credential_util_cmd = 'org.apache.ambari.server.credentialapi.CredentialUtil'
credential_util_jar = 'CredentialUtil.jar'

def removeloglines(lines):
    regex = re.compile(r'^(([0-1][0-9])|([2][0-3])):([0-5][0-9])(:[0-5][0-9])[,]\d{1,3}')
    cleanlines = [x for x in lines if not regex.match(x)]
    return(cleanlines)

def downloadjar(cs_lib_path, jdk_location):
    # Try to download CredentialUtil.jar from ambari-server resources
    credential_util_dir = cs_lib_path.split('*')[0].split(":")[-1] # Remove the trailing '*' and get the last directory if an entire path is passed
    credential_util_path = os.path.join(credential_util_dir, credential_util_jar)
    credential_util_url =  jdk_location + '/' + credential_util_jar
    File(credential_util_path,
         content = DownloadSource(credential_util_url),
         mode = 0755,
         )

def get_password_from_credential_store(alias, provider_path, cs_lib_path, java_home, jdk_location):
    downloadjar(cs_lib_path, jdk_location)

    # Execute a get command on the CredentialUtil CLI to get the password for the specified alias
    java_bin = '{java_home}/bin/java'.format(java_home=java_home)
    cmd = (java_bin, '-cp', cs_lib_path, credential_util_cmd, 'get', alias, '-provider', provider_path)
    cmd_result, std_out_msg  = checked_call(cmd, quiet=True)
    std_out_lines = std_out_msg.split('\n')
    return(std_out_lines[-1]) # Get the last line of the output, to skip warnings if any.


def list_aliases_from_credential_store(provider_path, cs_lib_path, java_home, jdk_location):
    downloadjar(cs_lib_path, jdk_location)

    # Execute a get command on the CredentialUtil CLI to list all the aliases
    java_bin = '{java_home}/bin/java'.format(java_home=java_home)
    cmd = (java_bin, '-cp', cs_lib_path, credential_util_cmd, 'list', '-provider', provider_path)
    cmd_result, std_out_msg  = checked_call(cmd, quiet=True)
    std_out_lines = std_out_msg.split('\n')
    return(removeloglines(std_out_lines)[1:]) # Get the last line of the output, to skip warnings if any.


def delete_alias_from_credential_store(alias, provider_path, cs_lib_path, java_home, jdk_location):
    downloadjar(cs_lib_path, jdk_location)

    #Execute the creation and overwrite password
    java_bin = '{java_home}/bin/java'.format(java_home=java_home)
    cmd = (java_bin, '-cp', cs_lib_path, credential_util_cmd, 'delete', alias, '-provider', provider_path, '-f')
    Execute(cmd)


def create_password_in_credential_store(alias, provider_path, cs_lib_path, java_home, jdk_location, password):
    downloadjar(cs_lib_path, jdk_location)

    #Execute the creation and overwrite password
    java_bin = '{java_home}/bin/java'.format(java_home=java_home)
    cmd = (java_bin, '-cp', cs_lib_path, credential_util_cmd, 'create', alias, '-value', PasswordString(password) ,'-provider', provider_path, '-f')
    Execute(cmd)
