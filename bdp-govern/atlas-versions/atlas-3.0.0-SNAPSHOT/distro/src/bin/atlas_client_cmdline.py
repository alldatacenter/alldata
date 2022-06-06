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
import atlas_config as mc

ATLAS_COMMAND_OPTS="-Datlas.home=%s"
ATLAS_LOG_OPTS="-Datlas.log.dir=%s -Datlas.log.file=%s"
DEFAULT_JVM_OPTS="-Xmx1024m -Dlog4j.configuration=atlas-log4j.xml"

def setup_conf_dir():
    atlas_home = mc.atlasDir()
    return mc.dirMustExist(mc.confDir(atlas_home))

def get_atlas_classpath(confdir):
    atlas_home = mc.atlasDir()
    web_app_dir = mc.webAppDir(atlas_home)
    mc.expandWebApp(atlas_home)
    p = os.pathsep
    atlas_classpath = confdir + p \
                      + os.path.join(web_app_dir, "atlas", "WEB-INF", "classes") + p \
                      + os.path.join(web_app_dir, "atlas", "WEB-INF", "lib", "*") + p \
                      + os.path.join(atlas_home, "libext", "*")
    if mc.isCygwin():
        atlas_classpath = mc.convertCygwinPath(atlas_classpath, True)
    return atlas_classpath

def get_atlas_hook_classpath(confdir):
    atlas_home = mc.atlasDir()
    kafka_topic_setup_dir = mc.kafkaTopicSetupDir(atlas_home)
    p = os.pathsep
    atlas_hook_classpath = confdir + p \
                            + os.path.join(kafka_topic_setup_dir, "*")
    if mc.isCygwin():
        atlas_hook_classpath = mc.convertCygwinPath(atlas_hook_classpath, True)
    return atlas_hook_classpath

def setup_jvm_opts_list(confdir, log_name):
    atlas_home = mc.atlasDir()
    mc.executeEnvSh(confdir)
    logdir = mc.dirMustExist(mc.logDir(atlas_home))
    if mc.isCygwin():
        # Pathnames that are passed to JVM must be converted to Windows format.
        jvm_atlas_home = mc.convertCygwinPath(atlas_home)
        jvm_logdir = mc.convertCygwinPath(logdir)
    else:
        jvm_atlas_home = atlas_home
        jvm_logdir = logdir

    # create sys property for conf dirs
    jvm_opts_list = (ATLAS_LOG_OPTS % (jvm_logdir, log_name)).split()
    cmd_opts = (ATLAS_COMMAND_OPTS % jvm_atlas_home)
    jvm_opts_list.extend(cmd_opts.split())
    atlas_jvm_opts = os.environ.get(mc.ATLAS_OPTS, DEFAULT_JVM_OPTS)
    jvm_opts_list.extend(atlas_jvm_opts.split())
    return jvm_opts_list
