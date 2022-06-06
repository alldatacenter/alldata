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

Ambari Agent
"""


from resource_management import *
import urlparse
from ambari_commons.inet_utils import download_file
import os

class MsiProvider(Provider):
  MSI_INSTALL_COMMAND = "cmd /C start /wait msiexec /qn /i {msi_file_path} /lv {log_file_path}{list_args_str}{dict_args_str}"

  def action_install(self):
    name = self.resource.msi_name
    msi_file_path = name
    dict_args = self.resource.dict_args
    list_args = self.resource.list_args
    working_dir = os.path.abspath(Script.get_config()["agentLevelParams"]["agentCacheDir"])
    http_source = self.resource.http_source

    # name can be a path to file in local file system
    msi_filename = os.path.split(name)[1]
    log_file_path = os.path.join(working_dir, msi_filename) + ".log"
    marker_file = os.path.join(working_dir, msi_filename) + ".installed"

    # build string from passed arguments to Msi resource
    dict_args_str = ' ALLUSERS="1"'
    for k, v in dict_args.iteritems():
      dict_args_str += " " + str(k)+"="+str(v)
    list_args_str = ''
    for a in list_args:
      list_args_str += " /" + str(a)

    # if http source present we download msi and then execute it
    if http_source:
      download_url = urlparse.urljoin(http_source, name)
      msi_file_path = os.path.join(working_dir, msi_filename)
      download_file(download_url, msi_file_path)
    if not os.path.exists(marker_file):
      Execute(MsiProvider.MSI_INSTALL_COMMAND.format(msi_file_path=msi_file_path,
                                                     log_file_path=log_file_path,
                                                     dict_args_str=dict_args_str,
                                                     list_args_str=list_args_str).rstrip())
      # writing marker file to not install new msi later
      open(marker_file,"w").close()

  def action_uninstall(self):
    pass

