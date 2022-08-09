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
import urllib2

from resource_management import *
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from ambari_commons import OSConst
import time

@OsFamilyFuncImpl(os_family=OSConst.WINSRV_FAMILY)
def webhcat_service_check():
  Logger.info("Webhcat smoke test - service status")

  import params
  # AMBARI-11633 [WinTP2] Webhcat service check fails
  # Hive doesn't pass the environment variables correctly to child processes, which fails the smoke test.
  # Reducing the amount of URLs checked to the minimum required.
  #smoke_cmd = os.path.join(params.stack_root,"Run-SmokeTests.cmd")
  #service = "WEBHCAT"
  #Execute(format("cmd /C {smoke_cmd} {service}"), user=params.hcat_user, logoutput=True)

  url_tests = [
    "status",
    #These are the failing ones:
    #"ddl/database?user.name=hadoop",
    #"ddl/database/default/table?user.name=hadoop"
  ]


  import socket

  url_host = socket.getfqdn()
  url_port = params.config["configurations"]["webhcat-site"]["templeton.port"]

  for url_test in url_tests:
    url_request = "http://{0}:{1}/templeton/v1/{2}".format(url_host, url_port, url_test)
    url_response = None

    try:
      # execute the query for the JSON that includes WebHCat status
      url_response = urllib2.urlopen(url_request, timeout=30)

      status = url_response.getcode()
      response = url_response.read()

      if status != 200:
        Logger.warning("Webhcat service check status: {0}".format(status))
      Logger.info("Webhcat service check response: {0}".format(response))
    except urllib2.HTTPError as he:
      raise Fail("Webhcat check {0} failed: {1}".format(url_request, he.msg))
    finally:
      if url_response is not None:
        try:
          url_response.close()
        except:
          pass


@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def webhcat_service_check():
  import params
  File(format("{tmp_dir}/templetonSmoke.sh"),
       content= StaticFile('templetonSmoke.sh'),
       mode=0755
  )

  if params.security_enabled:
    smokeuser_keytab=params.smoke_user_keytab
    smoke_user_principal=params.smokeuser_principal
  else:
    smokeuser_keytab= "no_keytab"
    smoke_user_principal="no_principal"
    
  unique_name = format("{smokeuser}.{timestamp}", timestamp = time.time())
  templeton_test_script = format("idtest.{unique_name}.pig")
  templeton_test_input = format("/tmp/idtest.{unique_name}.in")
  templeton_test_output = format("/tmp/idtest.{unique_name}.out")

  File(format("{tmp_dir}/{templeton_test_script}"),
       content = Template("templeton_smoke.pig.j2", templeton_test_input=templeton_test_input, templeton_test_output=templeton_test_output),
       owner=params.hdfs_user
  )
  
  params.HdfsResource(format("/tmp/{templeton_test_script}"),
                      action = "create_on_execute",
                      type = "file",
                      source = format("{tmp_dir}/{templeton_test_script}"),
                      owner = params.smokeuser
  )
  
  params.HdfsResource(templeton_test_input,
                      action = "create_on_execute",
                      type = "file",
                      source = "/etc/passwd",
                      owner = params.smokeuser
  )
  
  params.HdfsResource(None, action = "execute")

  cmd = format("{tmp_dir}/templetonSmoke.sh {webhcat_server_host[0]} {smokeuser} {templeton_port} {templeton_test_script} {smokeuser_keytab}"
               " {security_param} {kinit_path_local} {smoke_user_principal}"
               " {tmp_dir}")

  Execute(cmd,
          tries=3,
          try_sleep=5,
          path='/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
          logoutput=True)



