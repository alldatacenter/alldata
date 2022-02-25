#!/usr/bin/env python

#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


# Signal handling is OS-specific because there is no SIGKILL on Windows.

import os

if os.name == "nt":
  # Attempting to import SIGKILL on Windows would cause script to fail.
  from signal import SIGTERM
else:
  from signal import SIGTERM, SIGKILL

import sys
import traceback
import time
import atlas_config as mc

def main():

    atlas_home = mc.atlasDir()
    confdir = mc.dirMustExist(mc.confDir(atlas_home))
    mc.executeEnvSh(confdir)
    mc.dirMustExist(mc.logDir(atlas_home))

    atlas_pid_file = mc.pidFile(atlas_home)

    try:
        pf = open(atlas_pid_file, 'r')
        pid = int(pf.read().strip())
        pf.close()
    except:
        pid = None
    if not pid:
        sys.stderr.write("No process ID file found. Server not running?\n")
        return

    if not mc.exist_pid(pid):
       sys.stderr.write("Server no longer running with pid %s\nImproper shutdown?\npid file deleted.\n" %pid)
       os.remove(atlas_pid_file)
       return

    os.kill(pid, SIGTERM)

    mc.wait_for_shutdown(pid, "stopping atlas", 30)
    if not mc.exist_pid(pid):
        print("Apache Atlas Server stopped!!!\n")

    # assuming kill worked since process check on windows is more involved...
    if os.path.exists(atlas_pid_file):
        os.remove(atlas_pid_file)

    # stop solr
    if mc.is_solr_local(confdir):
        mc.run_solr(mc.solrBinDir(atlas_home), "stop", None, mc.solrPort(), None, True, mc.solrHomeDir(atlas_home))

    if mc.is_zookeeper_local(confdir):
        mc.run_zookeeper(mc.zookeeperBinDir(atlas_home), "stop")

    # stop elasticsearch
    if mc.is_elasticsearch_local():
        logdir = os.path.join(atlas_home, 'logs')
        elastic_pid_file = os.path.join(logdir, 'elasticsearch.pid')
        try:
            pf = open(elastic_pid_file, 'r')
            pid = int(pf.read().strip())
            pf.close()
        except:
            pid = None

        if not pid:
            sys.stderr.write("No process ID file found. Elasticsearch not running?\n")
            return

        if not mc.exist_pid(pid):
           sys.stderr.write("Elasticsearch no longer running with pid %s\nImproper shutdown?\npid file deleted.\n" %pid)
           os.remove(elastic_pid_file)
           return

        os.kill(pid, SIGTERM)

        mc.wait_for_shutdown(pid, "stopping elasticsearch", 30)
        if not mc.exist_pid(pid):
            print("Elasticsearch stopped!!!\n")

        # assuming kill worked since process check on windows is more involved...
        if os.path.exists(elastic_pid_file):
            os.remove(elastic_pid_file)

    # stop hbase
    if mc.is_hbase_local(confdir):
        mc.run_hbase_action(mc.hbaseBinDir(atlas_home), "stop", None, None, True)

    if mc.exist_pid(pid):
        #after 30 seconds kill it
        time.sleep(30)
        try:

            if os.name == "nt":
              # If running on Windows then timeout termination uses SIGTERM instead of SIGKILL.
              sys.stderr.write("did not stop gracefully after 30 seconds: killing process using SIGTERM\n")
              os.kill(pid, SIGTERM)
            else:
              sys.stderr.write("did not stop gracefully after 30 seconds: killing process using SIGKILL\n")
              os.kill(pid, SIGKILL)

        except:
            pass

if __name__ == '__main__':
    try:
        returncode = main()
    except Exception as e:
        print("Exception: %s " % str(e))
        print(traceback.format_exc())
        returncode = -1

    sys.exit(returncode)
