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
# Python Imports
import subprocess
import os
import re
import time
import shutil
from datetime import datetime
import json

# Ambari Commons & Resource Management imports
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions import format
from resource_management.libraries.functions.check_process_status import check_process_status
from resource_management.core.source import InlineTemplate
from resource_management.core.resources.system import Execute

# Imports needed for Rolling/Express Upgrade
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions.copy_tarball import copy_to_hdfs

from resource_management.core import shell
from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger
from ambari_commons import OSCheck, OSConst
from ambari_commons.os_family_impl import OsFamilyImpl

from resource_management.core.exceptions import ComponentIsNotRunning
from resource_management.libraries.functions.decorator import retry
from resource_management.libraries.functions.security_commons import build_expectations, \
  cached_kinit_executor, get_params_from_filesystem, validate_security_config_properties, \
  FILE_TYPE_XML

# Local Imports
from setup_ranger_hive import setup_ranger_hive
from hive_service_interactive import hive_service_interactive
from hive_interactive import hive_interactive
from hive_server import HiveServerDefault
from setup_ranger_hive_interactive import setup_ranger_hive_interactive

import traceback

class HiveServerInteractive(Script):
  pass


@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class HiveServerInteractiveDefault(HiveServerInteractive):

    def get_component_name(self):
      return "hive-server2-hive2"

    def install(self, env):
      import params
      self.install_packages(env)

    def configure(self, env):
      import params
      env.set_params(params)
      hive_interactive(name='hiveserver2')

    def pre_upgrade_restart(self, env, upgrade_type=None):
      Logger.info("Executing Hive Server Interactive Stack Upgrade pre-restart")
      import params
      env.set_params(params)

      if params.version and check_stack_feature(StackFeature.ROLLING_UPGRADE, params.version):
        stack_select.select("hive-server2-hive2", params.version)

        # Copy hive.tar.gz and tez.tar.gz used by Hive Interactive to HDFS
        resource_created = copy_to_hdfs(
          "hive2",
          params.user_group,
          params.hdfs_user,
          host_sys_prepped=params.host_sys_prepped)

        resource_created = copy_to_hdfs(
          "tez_hive2",
          params.user_group,
          params.hdfs_user,
          host_sys_prepped=params.host_sys_prepped) or resource_created

        if resource_created:
          params.HdfsResource(None, action="execute")

    def start(self, env, upgrade_type=None):
      import params
      env.set_params(params)
      self.configure(env)

      if params.security_enabled:
        # Do the security setup, internally calls do_kinit()
        self.setup_security()

      # TODO : We need have conditional [re]start of LLAP once "status check command" for LLAP is ready.
      # Check status and based on that decide on [re]starting.

      # Start LLAP before Hive Server Interactive start.
      status = self._llap_start(env)
      if not status:
        raise Fail("Skipping START of Hive Server Interactive since LLAP app couldn't be STARTED.")

      # TODO : test the workability of Ranger and Hive2 during upgrade
      setup_ranger_hive_interactive(upgrade_type=upgrade_type)
      hive_service_interactive('hiveserver2', action='start', upgrade_type=upgrade_type)


    def stop(self, env, upgrade_type=None):
      import params
      env.set_params(params)

      if params.security_enabled:
        self.do_kinit()

      # Stop Hive Interactive Server first
      hive_service_interactive('hiveserver2', action='stop')

      self._llap_stop(env)

    def status(self, env):
      import status_params
      env.set_params(status_params)

      # We are not doing 'llap' status check done here as part of status check for 'HSI', as 'llap' status
      # check is a heavy weight operation.

      pid_file = format("{hive_pid_dir}/{hive_interactive_pid}")
      # Recursively check all existing gmetad pid files
      check_process_status(pid_file)

    def security_status(self, env):
      import status_params
      env.set_params(status_params)

      if status_params.security_enabled:
        props_value_check = {"hive.server2.authentication": "KERBEROS",
                             "hive.metastore.sasl.enabled": "true",
                             "hive.security.authorization.enabled": "true"}
        props_empty_check = ["hive.server2.authentication.kerberos.keytab",
                             "hive.server2.authentication.kerberos.principal",
                             "hive.server2.authentication.spnego.principal",
                             "hive.server2.authentication.spnego.keytab"]

        props_read_check = ["hive.server2.authentication.kerberos.keytab",
                            "hive.server2.authentication.spnego.keytab"]
        hive_site_props = build_expectations('hive-site', props_value_check, props_empty_check,
                                             props_read_check)

        hive_expectations ={}
        hive_expectations.update(hive_site_props)

        security_params = get_params_from_filesystem(status_params.hive_server_interactive_conf_dir,
                                                     {'hive-site.xml': FILE_TYPE_XML})
        result_issues = validate_security_config_properties(security_params, hive_expectations)
        if not result_issues: # If all validations passed successfully
          try:
            # Double check the dict before calling execute
            if 'hive-site' not in security_params \
              or 'hive.server2.authentication.kerberos.keytab' not in security_params['hive-site'] \
              or 'hive.server2.authentication.kerberos.principal' not in security_params['hive-site'] \
              or 'hive.server2.authentication.spnego.keytab' not in security_params['hive-site'] \
              or 'hive.server2.authentication.spnego.principal' not in security_params['hive-site']:
              self.put_structured_out({"securityState": "UNSECURED"})
              self.put_structured_out({"securityIssuesFound": "Keytab file or principal are not set property."})
              return

            cached_kinit_executor(status_params.kinit_path_local,
                                  status_params.hive_user,
                                  security_params['hive-site']['hive.server2.authentication.kerberos.keytab'],
                                  security_params['hive-site']['hive.server2.authentication.kerberos.principal'],
                                  status_params.hostname,
                                  status_params.tmp_dir)
            cached_kinit_executor(status_params.kinit_path_local,
                                  status_params.hive_user,
                                  security_params['hive-site']['hive.server2.authentication.spnego.keytab'],
                                  security_params['hive-site']['hive.server2.authentication.spnego.principal'],
                                  status_params.hostname,
                                  status_params.tmp_dir)
            self.put_structured_out({"securityState": "SECURED_KERBEROS"})
          except Exception as e:
            self.put_structured_out({"securityState": "ERROR"})
            self.put_structured_out({"securityStateErrorInfo": str(e)})
        else:
          issues = []
          for cf in result_issues:
            issues.append("Configuration file %s did not pass the validation. Reason: %s" % (cf, result_issues[cf]))
          self.put_structured_out({"securityIssuesFound": ". ".join(issues)})
          self.put_structured_out({"securityState": "UNSECURED"})
      else:
        self.put_structured_out({"securityState": "UNSECURED"})

    def restart_llap(self, env):
      """
      Custom command to Restart LLAP
      """
      Logger.info("Custom Command to retart LLAP")
      import params
      env.set_params(params)

      if params.security_enabled:
        self.do_kinit()

      self._llap_stop(env)
      self._llap_start(env)

    def _llap_stop(self, env):
      import params
      Logger.info("Stopping LLAP")
      SLIDER_APP_NAME = "llap0"

      stop_cmd = ["slider", "stop", SLIDER_APP_NAME]

      code, output, error = shell.call(stop_cmd, user=params.hive_user, stderr=subprocess.PIPE, logoutput=True)
      if code == 0:
        Logger.info(format("Stopped {SLIDER_APP_NAME} application on Slider successfully"))
      elif code == 69 and output is not None and "Unknown application instance" in output:
        Logger.info(format("Application {SLIDER_APP_NAME} was already stopped on Slider"))
      else:
        raise Fail(format("Could not stop application {SLIDER_APP_NAME} on Slider. {error}\n{output}"))

      # Will exit with code 4 if need to run with "--force" to delete directories and registries.
      Execute(('slider', 'destroy', SLIDER_APP_NAME, "--force"),
              user=params.hive_user,
              timeout=30,
              ignore_failures=True,
      )

    """
    Controls the start of LLAP.
    """
    def _llap_start(self, env, cleanup=False):
      import params
      env.set_params(params)
      Logger.info("Starting LLAP")
      LLAP_PACKAGE_CREATION_PATH = Script.get_tmp_dir()
      LLAP_APP_NAME = 'llap0'

      unique_name = "llap-slider%s" % datetime.utcnow().strftime('%Y-%m-%d_%H-%M-%S')

      cmd = format("{stack_root}/current/hive-server2-hive2/bin/hive --service llap --instances {params.num_llap_nodes}"
                   " --slider-am-container-mb {params.slider_am_container_mb} --size {params.llap_daemon_container_size}m "
                   " --cache {params.hive_llap_io_mem_size}m --xmx {params.llap_heap_size}m --loglevel {params.llap_log_level}"
                   " --output {LLAP_PACKAGE_CREATION_PATH}/{unique_name}")
      if params.security_enabled:
        llap_keytab_splits = params.hive_llap_keytab_file.split("/")
        Logger.debug("llap_keytab_splits : {0}".format(llap_keytab_splits))
        cmd += format(" --slider-keytab-dir .slider/keytabs/{params.hive_user}/ --slider-keytab "
                      "{llap_keytab_splits[4]} --slider-principal {params.hive_llap_principal}")

      # Append args.
      llap_java_args = InlineTemplate(params.llap_app_java_opts).get_content()
      cmd += format(" --args \" {llap_java_args}\"")

      run_file_path = None
      try:
        Logger.info(format("Command: {cmd}"))
        code, output, error = shell.checked_call(cmd, user=params.hive_user, stderr=subprocess.PIPE, logoutput=True)

        if code != 0 or output is None:
          raise Fail("Command failed with either non-zero return code or no output.")

        # E.g., output:
        # Prepared llap-slider-05Apr2016/run.sh for running LLAP on Slider
        exp = r"Prepared (.*?run.sh) for running LLAP"
        run_file_path = None
        out_splits = output.split("\n")
        for line in out_splits:
          line = line.strip()
          m = re.match(exp, line, re.I)
          if m and len(m.groups()) == 1:
            run_file_name = m.group(1)
            run_file_path = os.path.join(params.hive_user_home_dir, run_file_name)
            break
        if not run_file_path:
          raise Fail("Did not find run.sh file in output: " + str(output))

        Logger.info(format("Run file path: {run_file_path}"))
        Execute(run_file_path, user=params.hive_user)
        Logger.info("Submitted LLAP app name : {0}".format(LLAP_APP_NAME))

        # We need to check the status of LLAP app to figure out it got
        # launched properly and is in running state. Then go ahead with Hive Interactive Server start.
        status = self.check_llap_app_status(LLAP_APP_NAME, params.num_retries_for_checking_llap_status)
        if status:
          Logger.info("LLAP app '{0}' deployed successfully.".format(LLAP_APP_NAME))
          return True
        else:
          Logger.error("LLAP app '{0}' deployment unsuccessful.".format(LLAP_APP_NAME))
          return False
      except:
        # Attempt to clean up the packaged application, or potentially rename it with a .bak
        if run_file_path is not None and cleanup:
          try:
            parent_dir = os.path.dirname(run_file_path)
            if os.path.isdir(parent_dir):
              shutil.rmtree(parent_dir)
          except Exception, e:
            Logger.error("Could not cleanup LLAP app package. Error: " + str(e))

        # throw the original exception
        raise

    """
    Does kinit and copies keytab for Hive/LLAP to HDFS.
    """
    def setup_security(self):
      import params

      self.do_kinit()

      # Copy params.hive_llap_keytab_file to hdfs://<host>:<port>/user/<hive_user>/.slider/keytabs/<hive_user> , required by LLAP
      slider_keytab_install_cmd = format("slider install-keytab --keytab {params.hive_llap_keytab_file} --folder {params.hive_user} --overwrite")
      Execute(slider_keytab_install_cmd, user=params.hive_user)

    def do_kinit(self):
      import params

      hive_interactive_kinit_cmd = format("{kinit_path_local} -kt {params.hive_server2_keytab} {params.hive_principal}; ")
      Execute(hive_interactive_kinit_cmd, user=params.hive_user)

      llap_kinit_cmd = format("{kinit_path_local} -kt {params.hive_llap_keytab_file} {params.hive_llap_principal}; ")
      Execute(llap_kinit_cmd, user=params.hive_user)

    """
    Get llap app status data.
    """
    def _get_llap_app_status_info(self, app_name):
      import status_params
      LLAP_APP_STATUS_CMD_TIMEOUT = 0

      llap_status_cmd = format("{stack_root}/current/hive-server2-hive2/bin/hive --service llapstatus --name {app_name} --findAppTimeout {LLAP_APP_STATUS_CMD_TIMEOUT}")
      code, output, error = shell.checked_call(llap_status_cmd, user=status_params.hive_user, stderr=subprocess.PIPE,
                                               logoutput=False)
      Logger.info("Received 'llapstatus' command 'output' : {0}".format(output))
      return self._make_valid_json(output)


    """
    Remove extra lines from 'llapstatus' status output (eg: because of MOTD logging) so as to have a valid JSON data to be passed in
    to JSON converter.
    """
    def _make_valid_json(self, output):
      '''

      Note: It is assumed right now that extra lines will be only at the start and not at the end.

      Sample expected JSON to be passed for 'loads' is either of the form :

      Case 'A':
      {
          "amInfo" : {
          "appName" : "llap0",
          "appType" : "org-apache-slider",
          "appId" : "APP1",
          "containerId" : "container_1466036628595_0010_01_000001",
          "hostname" : "hostName",
          "amWebUrl" : "http://hostName:port/"
        },
        "state" : "LAUNCHING",
        ....
        "desiredInstances" : 1,
        "liveInstances" : 0,
        ....
        ....
      }

      or

      Case 'B':
      {
        "state" : "APP_NOT_FOUND"
      }

      '''
      splits = output.split("\n")

      len_splits = len(splits)
      if (len_splits < 3):
        raise Fail ("Malformed JSON data received from 'llapstatus' command. Exiting ....")

      marker_idx = None # To detect where from to start reading for JSON data
      for idx, split in enumerate(splits):
        curr_elem = split.strip()
        if idx+2 > len_splits:
          raise Fail("Iterated over the received 'llapstatus' comamnd. Couldn't validate the received output for JSON parsing.")
        next_elem = (splits[(idx + 1)]).strip()
        if curr_elem == "{":
          if next_elem == "\"amInfo\" : {" and (splits[len_splits-1]).strip() == '}':
            # For Case 'A'
            marker_idx = idx
            break;
          elif idx+3 == len_splits and next_elem.startswith('"state" : ') and (splits[idx + 2]).strip() == '}':
              # For Case 'B'
              marker_idx = idx
              break;

      Logger.info("Marker index for start of JSON data for 'llapsrtatus' comamnd : {0}".format(marker_idx))

      # Remove extra logging from possible JSON output
      if marker_idx is None:
        raise Fail("Couldn't validate the received output for JSON parsing.")
      else:
        if marker_idx != 0:
          del splits[0:marker_idx]
          Logger.info("Removed lines: '1-{0}' from the received 'llapstatus' output to make it valid for JSON parsing.".format(marker_idx))

      scanned_output = '\n'.join(splits)
      llap_app_info = json.loads(scanned_output)
      return llap_app_info


    """
    Checks llap app status. The states can be : 'COMPLETE', 'APP_NOT_FOUND', 'RUNNING_PARTIAL', 'RUNNING_ALL' & 'LAUNCHING'.

    if app is in 'APP_NOT_FOUND', 'RUNNING_PARTIAL' and 'LAUNCHING' state:
       we wait for 'num_times_to_wait' to have app in (1). 'RUNNING_ALL' or (2). 'RUNNING_PARTIAL'
       state with 80% or more 'desiredInstances' running and Return True
    else :
       Return False

    Parameters: llap_app_name : deployed llap app name.
                num_retries :   Number of retries to check the LLAP app status.
    """
    def check_llap_app_status(self, llap_app_name, num_retries):
      # counters based on various states.
      curr_time = time.time()

      if num_retries <= 0:
        num_retries = 2
      if num_retries > 20:
        num_retries = 20
      @retry(times=num_retries, sleep_time=2, err_class=Fail)
      def do_retries():
        live_instances = 0
        desired_instances = 0

        percent_desired_instances_to_be_up = 80 # Used in 'RUNNING_PARTIAL' state.
        llap_app_info = self._get_llap_app_status_info(llap_app_name)
        if llap_app_info is None or 'state' not in llap_app_info:
          Logger.error("Malformed JSON data received for LLAP app. Exiting ....")
          return False

        if llap_app_info['state'].upper() == 'RUNNING_ALL':
          Logger.info(
            "LLAP app '{0}' in '{1}' state.".format(llap_app_name, llap_app_info['state']))
          return True
        elif llap_app_info['state'].upper() == 'RUNNING_PARTIAL':
          # Check how many instances were up.
          if 'liveInstances' in llap_app_info and 'desiredInstances' in llap_app_info:
            live_instances = llap_app_info['liveInstances']
            desired_instances = llap_app_info['desiredInstances']
          else:
            Logger.info(
              "LLAP app '{0}' is in '{1}' state, but 'instances' information not available in JSON received. " \
              "Exiting ....".format(llap_app_name, llap_app_info['state']))
            Logger.info(llap_app_info)
            return False
          if desired_instances == 0:
            Logger.info("LLAP app '{0}' desired instance are set to 0. Exiting ....".format(llap_app_name))
            return False

          percentInstancesUp = 0
          if live_instances > 0:
            percentInstancesUp = float(live_instances) / desired_instances * 100
          if percentInstancesUp >= percent_desired_instances_to_be_up:
            Logger.info("LLAP app '{0}' in '{1}' state. Live Instances : '{2}'  >= {3}% of Desired Instances : " \
                        "'{4}'.".format(llap_app_name, llap_app_info['state'],
                                       llap_app_info['liveInstances'],
                                       percent_desired_instances_to_be_up,
                                       llap_app_info['desiredInstances']))
            return True
          else:
            Logger.info("LLAP app '{0}' in '{1}' state. Live Instances : '{2}'. Desired Instances : " \
                        "'{3}' after {4} secs.".format(llap_app_name, llap_app_info['state'],
                                                       llap_app_info['liveInstances'],
                                                       llap_app_info['desiredInstances'],
                                                       time.time() - curr_time))
            raise Fail("App state is RUNNING_PARTIAL. Live Instances : '{0}', Desired Instance : '{1}'".format(llap_app_info['liveInstances'],
                                                                                                           llap_app_info['desiredInstances']))
        elif llap_app_info['state'].upper() in ['APP_NOT_FOUND', 'LAUNCHING', 'COMPLETE']:
          status_str = format("LLAP app '{0}' current state is {1}.".format(llap_app_name, llap_app_info['state']))
          Logger.info(status_str)
          raise Fail(status_str)
        else:  # Covers any unknown that we get.
          Logger.info(
            "LLAP app '{0}' current state is '{1}'. Expected : 'RUNNING'.".format(llap_app_name, llap_app_info['state']))
          return False

      try:
        status = do_retries()
        return status
      except Exception, e:
        Logger.info("LLAP app '{0}' did not come up after a wait of {1} seconds.".format(llap_app_name,
                                                                                          time.time() - curr_time))
        traceback.print_exc()
        return False

    def get_log_folder(self):
      import params
      return params.hive_log_dir

    def get_user(self):
      import params
      return params.hive_user

@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class HiveServerInteractiveWindows(HiveServerInteractive):

  def status(self, env):
    pass

if __name__ == "__main__":
  HiveServerInteractive().execute()