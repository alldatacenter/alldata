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
import os
import sys
import traceback

import atlas_config as mc

ATLAS_LOG_OPTS="-Datlas.log.dir=%s -Datlas.log.file=%s.log"
ATLAS_COMMAND_OPTS="-Datlas.home=%s"
ATLAS_CONFIG_OPTS="-Datlas.conf=%s"
DEFAULT_JVM_HEAP_OPTS="-Xmx1024m"
DEFAULT_JVM_OPTS="-Dlog4j.configuration=atlas-log4j.xml -Djava.net.preferIPv4Stack=true -server"

def main():

    is_setup = (len(sys.argv)>1) and sys.argv[1] is not None and sys.argv[1] == '-setup'

    atlas_home = mc.atlasDir()
    confdir = mc.dirMustExist(mc.confDir(atlas_home))
    mc.executeEnvSh(confdir)
    logdir = mc.dirMustExist(mc.logDir(atlas_home))
    mc.dirMustExist(mc.dataDir(atlas_home))
    if mc.isCygwin():
        # Pathnames that are passed to JVM must be converted to Windows format.
        jvm_atlas_home = mc.convertCygwinPath(atlas_home)
        jvm_confdir = mc.convertCygwinPath(confdir)
        jvm_logdir = mc.convertCygwinPath(logdir)
    else:
        jvm_atlas_home = atlas_home
        jvm_confdir = confdir
        jvm_logdir = logdir

    #create sys property for conf dirs
    if not is_setup:
        jvm_opts_list = (ATLAS_LOG_OPTS % (jvm_logdir, "application")).split()
    else:
        jvm_opts_list = (ATLAS_LOG_OPTS % (jvm_logdir, "atlas_setup")).split()

    cmd_opts = (ATLAS_COMMAND_OPTS % jvm_atlas_home)
    jvm_opts_list.extend(cmd_opts.split())

    config_opts = (ATLAS_CONFIG_OPTS % jvm_confdir)
    jvm_opts_list.extend(config_opts.split())

    atlas_server_heap_opts = os.environ.get(mc.ATLAS_SERVER_HEAP, DEFAULT_JVM_HEAP_OPTS)
    jvm_opts_list.extend(atlas_server_heap_opts.split())

    atlas_server_jvm_opts = os.environ.get(mc.ATLAS_SERVER_OPTS)
    if atlas_server_jvm_opts:
        jvm_opts_list.extend(atlas_server_jvm_opts.split())

    atlas_jvm_opts = os.environ.get(mc.ATLAS_OPTS, DEFAULT_JVM_OPTS)
    jvm_opts_list.extend(atlas_jvm_opts.split())

    #expand web app dir
    web_app_dir = mc.webAppDir(atlas_home)
    mc.expandWebApp(atlas_home)

    p = os.pathsep
    atlas_classpath = confdir + p \
                       + os.path.join(web_app_dir, "atlas", "WEB-INF", "classes" ) + p \
                       + os.path.join(web_app_dir, "atlas", "WEB-INF", "lib", "*" )  + p \
                       + os.path.join(atlas_home, "libext", "*")

    is_hbase = mc.is_hbase(confdir)

    if is_hbase:
        #add hbase-site.xml to classpath
        hbase_conf_dir = mc.hbaseConfDir(atlas_home)

        if os.path.exists(hbase_conf_dir):
            atlas_classpath = atlas_classpath + p \
                            + hbase_conf_dir
        else:
            if mc.is_hbase(confdir):
                raise Exception("Could not find hbase-site.xml in %s. Please set env var HBASE_CONF_DIR to the hbase client conf dir", hbase_conf_dir)

    if mc.isCygwin():
        atlas_classpath = mc.convertCygwinPath(atlas_classpath, True)

    atlas_pid_file = mc.pidFile(atlas_home)

    if os.path.isfile(atlas_pid_file):
       #Check if process listed in atlas.pid file is still running
       pf = open(atlas_pid_file, 'r')
       pid = pf.read().strip()
       pf.close()
       if pid != "":
           if mc.exist_pid((int)(pid)):
               if is_setup:
                   print("Cannot run setup when server is running.")
               mc.server_already_running(pid)
           else:
               mc.server_pid_not_running(pid)

    if is_hbase and mc.is_hbase_local(confdir):
        print("\nConfigured for local HBase.")
        mc.configure_hbase(atlas_home)

        print("Starting local HBase...")
        mc.run_hbase_action(mc.hbaseBinDir(atlas_home), "start", hbase_conf_dir, logdir)

        print("Local HBase started!")

    #solr setup
    if mc.is_solr_local(confdir):
        print("\nConfigured for local Solr.")

        if mc.is_cassandra_local(confdir):
            print("Cassandra embedded configured.")
            mc.configure_cassandra(atlas_home)

        if mc.is_zookeeper_local(confdir):
            mc.configure_zookeeper(atlas_home)
            mc.run_zookeeper(mc.zookeeperBinDir(atlas_home), "start", logdir)
            print("zookeeper started.")

        print("Starting local Solr...")
        mc.run_solr(mc.solrBinDir(atlas_home), "start", mc.get_solr_zk_url(confdir), mc.solrPort(), logdir, True, mc.solrHomeDir(atlas_home))

        print("Local Solr started!")

        print("\nCreating Solr collections for Atlas using config: " + mc.solrConfDir(atlas_home))

        mc.create_solr_collection(mc.solrBinDir(atlas_home), mc.solrConfDir(atlas_home), "vertex_index", logdir)

        mc.create_solr_collection(mc.solrBinDir(atlas_home), mc.solrConfDir(atlas_home), "edge_index", logdir)

        mc.create_solr_collection(mc.solrBinDir(atlas_home), mc.solrConfDir(atlas_home), "fulltext_index", logdir)

    #elasticsearch setup
    if mc.is_elasticsearch_local():
        print("configured for local elasticsearch.")
        mc.start_elasticsearch(mc.elasticsearchBinDir(atlas_home), logdir)
        print("elasticsearch started.")

    web_app_path = os.path.join(web_app_dir, "atlas")
    if (mc.isCygwin()):
        web_app_path = mc.convertCygwinPath(web_app_path)
    if not is_setup:
        start_atlas_server(atlas_classpath, atlas_pid_file, jvm_logdir, jvm_opts_list, web_app_path)
        mc.wait_for_startup(confdir, 300)
        print("Apache Atlas Server started!!!\n")
    else:
        process = mc.java("org.apache.atlas.web.setup.AtlasSetup", [], atlas_classpath, jvm_opts_list, jvm_logdir)
        return process.wait()


def start_atlas_server(atlas_classpath, atlas_pid_file, jvm_logdir, jvm_opts_list, web_app_path):
    args = ["-app", web_app_path]
    args.extend(sys.argv[1:])
    process = mc.java("org.apache.atlas.Atlas", args, atlas_classpath, jvm_opts_list, jvm_logdir)
    mc.writePid(atlas_pid_file, process)

if __name__ == '__main__':
    try:
        returncode = main()
    except Exception as e:
        print("Exception: %s " % str(e))
        print(traceback.format_exc())
        returncode = -1

    sys.exit(returncode)
