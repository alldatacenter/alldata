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
import getpass
import os
import re
import platform
import subprocess
import sys
import time
import errno
import socket
from re import split
from time import sleep

BIN = "bin"
LIB = "lib"
CONF = "conf"
LOG = "logs"
WEBAPP = "server" + os.sep + "webapp"
CONFIG_SETS_CONF = "server" + os.sep + "solr" + os.sep + "configsets" + os.sep + "_default" + os.sep + "conf"
DATA = "data"
ATLAS_CONF = "ATLAS_CONF"
ATLAS_LOG = "ATLAS_LOG_DIR"
ATLAS_PID = "ATLAS_PID_DIR"
ATLAS_WEBAPP = "ATLAS_EXPANDED_WEBAPP_DIR"
ATLAS_SERVER_OPTS = "ATLAS_SERVER_OPTS"
ATLAS_OPTS = "ATLAS_OPTS"
ATLAS_SERVER_HEAP = "ATLAS_SERVER_HEAP"
ATLAS_DATA = "ATLAS_DATA_DIR"
ATLAS_HOME = "ATLAS_HOME_DIR"
HBASE_CONF_DIR = "HBASE_CONF_DIR"
MANAGE_LOCAL_HBASE = "MANAGE_LOCAL_HBASE"
MANAGE_LOCAL_SOLR = "MANAGE_LOCAL_SOLR"
MANAGE_EMBEDDED_CASSANDRA = "MANAGE_EMBEDDED_CASSANDRA"
MANAGE_LOCAL_ELASTICSEARCH = "MANAGE_LOCAL_ELASTICSEARCH"
SOLR_BIN = "SOLR_BIN"
SOLR_CONF = "SOLR_CONF"
SOLR_HOME = "SOLR_HOME"
SOLR_PORT = "SOLR_PORT"
SOLR_DIR = "SOLR_DIR"
DEFAULT_SOLR_PORT = "9838"
SOLR_SHARDS = "SOLR_SHARDS"
DEFAULT_SOLR_SHARDS = "1"
SOLR_REPLICATION_FACTOR = "SOLR_REPLICATION_FACTOR"
DEFAULT_SOLR_REPLICATION_FACTOR = "1"

ENV_KEYS = ["JAVA_HOME", ATLAS_OPTS, ATLAS_SERVER_OPTS, ATLAS_SERVER_HEAP, ATLAS_LOG, ATLAS_PID, ATLAS_CONF,
            "ATLASCPPATH", ATLAS_DATA, ATLAS_HOME, ATLAS_WEBAPP, HBASE_CONF_DIR, SOLR_PORT, MANAGE_LOCAL_HBASE,
            MANAGE_LOCAL_SOLR, MANAGE_EMBEDDED_CASSANDRA, MANAGE_LOCAL_ELASTICSEARCH, SOLR_HOME, SOLR_DIR]
IS_WINDOWS = platform.system() == "Windows"
ON_POSIX = 'posix' in sys.builtin_module_names
CONF_FILE="atlas-application.properties"
STORAGE_BACKEND_CONF="atlas.graph.storage.backend"
HBASE_STORAGE_LOCAL_CONF_ENTRY="atlas.graph.storage.hostname\s*=\s*localhost"
SOLR_INDEX_CONF_ENTRY="atlas.graph.index.search.backend\s*=\s*solr"
SOLR_INDEX_MODE_CONF_ENTRY="atlas.graph.index.search.solr.mode"
SOLR_INDEX_LOCAL_STANDALONE_CONF_ENTRY="atlas.graph.index.search.solr.http-urls\s*=(http|https)://localhost"
SOLR_INDEX_LOCAL_CLOUD_CONF_ENTRY="atlas.graph.index.search.solr.zookeeper-url\s*=\s*localhost"
SOLR_INDEX_ZK_URL="atlas.graph.index.search.solr.zookeeper-url"
TOPICS_TO_CREATE="atlas.notification.topics"
ATLAS_HTTP_PORT="atlas.server.http.port"
ATLAS_HTTPS_PORT="atlas.server.https.port"
DEFAULT_ATLAS_HTTP_PORT="21000"
DEFAULT_ATLAS_HTTPS_PORT="21443"
ATLAS_ENABLE_TLS="atlas.enableTLS"
ATLAS_SERVER_BIND_ADDRESS="atlas.server.bind.address"
DEFAULT_ATLAS_SERVER_HOST="localhost"

DEBUG = False

def scriptDir():
    """
    get the script path
    """
    return os.path.dirname(os.path.realpath(__file__))

def atlasDir():
    home = os.path.dirname(scriptDir())
    return os.environ.get(ATLAS_HOME, home)

def libDir(dir) :
    return os.path.join(dir, LIB)

def confDir(dir):
    localconf = os.path.join(dir, CONF)
    return os.environ.get(ATLAS_CONF, localconf)

def hbaseBinDir(dir):
    return os.path.join(dir, "hbase", BIN)

def hbaseConfDir(dir):
    return os.environ.get(HBASE_CONF_DIR, os.path.join(dir, "hbase", CONF))

def zookeeperBinDir(dir):
    return os.environ.get(SOLR_BIN, os.path.join(dir, "zk", BIN))

def solrDir():
    return os.environ.get(SOLR_DIR, os.path.join(atlasDir(), "solr"))

def solrServerDir():
    return os.path.join(solrDir(), "server")

def solrBinDir(dir):
    return os.environ.get(SOLR_BIN, os.path.join(dir, "solr", BIN))

def elasticsearchBinDir(dir):
    return os.environ.get(SOLR_BIN, os.path.join(dir, "elasticsearch", BIN))

def solrHomeDir(dir):
    return os.environ.get(SOLR_HOME, os.path.join(dir, "data", "solr"))

def solrConfDir(dir):
    return os.environ.get(SOLR_CONF, os.path.join(dir, "conf", "solr"))

def solrPort():
    return os.environ.get(SOLR_PORT, DEFAULT_SOLR_PORT)

def solrShards():
    return os.environ.get(SOLR_SHARDS, DEFAULT_SOLR_SHARDS)

def solrReplicationFactor():
    return os.environ.get(SOLR_REPLICATION_FACTOR, DEFAULT_SOLR_REPLICATION_FACTOR)

def logDir(dir):
    localLog = os.path.join(dir, LOG)
    return os.environ.get(ATLAS_LOG, localLog)

def pidFile(dir):
    localPid = os.path.join(dir, LOG)
    return os.path.join(os.environ.get(ATLAS_PID, localPid), 'atlas.pid')

def dataDir(dir):
    data = os.path.join(dir, DATA)
    return os.environ.get(ATLAS_DATA, data)

def webAppDir(dir):
    webapp = os.path.join(dir, WEBAPP)
    return os.environ.get(ATLAS_WEBAPP, webapp)

def kafkaTopicSetupDir(homeDir):
    return os.path.join(homeDir, "hook", "kafka-topic-setup")

def expandWebApp(dir):
    webappDir = webAppDir(dir)
    webAppMetadataDir = os.path.join(webappDir, "atlas")
    d = os.sep
    if not os.path.exists(os.path.join(webAppMetadataDir, "WEB-INF")):
        try:
            os.makedirs(webAppMetadataDir)
        except OSError as e:
            if e.errno != errno.EEXIST:
                raise e
        atlasWarPath = os.path.join(atlasDir(), "server", "webapp", "atlas.war")
        if (isCygwin()):
            atlasWarPath = convertCygwinPath(atlasWarPath)
        os.chdir(webAppMetadataDir)
        jar(atlasWarPath)

def dirMustExist(dirname):
    if not os.path.exists(dirname):
        os.mkdir(dirname)
    return dirname

def executeEnvSh(confDir):
    envscript = '%s/atlas-env.sh' % confDir
    if not IS_WINDOWS and os.path.exists(envscript):
        envCmd = 'source %s && env' % envscript
        command = ['bash', '-c', envCmd]

        proc = subprocess.Popen(command, stdout = subprocess.PIPE)

        for line in proc.stdout:
            (key, _, value) = line.decode('utf8').strip().partition("=")
            if key in ENV_KEYS:
                os.environ[key] = value

        proc.communicate()

def java(classname, args, classpath, jvm_opts_list, logdir=None):
    java_home = os.environ.get("JAVA_HOME", None)
    if java_home:
        prg = os.path.join(java_home, "bin", "java")
    else:
        prg = which("java")

    if prg is None:
        raise EnvironmentError('The java binary could not be found in your path or JAVA_HOME')

    commandline = [prg]
    commandline.extend(jvm_opts_list)
    commandline.append("-classpath")
    commandline.append(classpath)
    commandline.append(classname)
    commandline.extend(args)
    return runProcess(commandline, logdir)

def jar(path):
    java_home = os.environ.get("JAVA_HOME", None)
    if java_home:
        prg = os.path.join(java_home, "bin", "jar")
    else:
        prg = which("jar")

    if prg is None:
        raise EnvironmentError('The jar binary could not be found in your path or JAVA_HOME')

    commandline = [prg]
    commandline.append("-xf")
    commandline.append(path)
    process = runProcess(commandline)
    process.wait()

def is_exe(fpath):
    return os.path.isfile(fpath) and os.access(fpath, os.X_OK)

def which(program):

    fpath, fname = os.path.split(program)
    if fpath:
        if is_exe(program):
            return program
    else:
        for path in os.environ["PATH"].split(os.pathsep):
            path = path.strip('"')
            exe_file = os.path.join(path, program)
            if is_exe(exe_file):
                return exe_file

    return None

def runProcess(commandline, logdir=None, shell=False, wait=False):
    """
    Run a process
    :param commandline: command line
    :return:the return code
    """
    global finished
    debug ("Executing : %s" % str(commandline))
    timestr = time.strftime("atlas.%Y%m%d-%H%M%S")
    stdoutFile = None
    stderrFile = None
    if logdir:
        stdoutFile = open(os.path.join(logdir, timestr + ".out"), "w")
        stderrFile = open(os.path.join(logdir,timestr + ".err"), "w")

    p = subprocess.Popen(commandline, stdout=stdoutFile, stderr=stderrFile, shell=shell)

    if wait:
        p.communicate()

    return p

def print_output(name, src, toStdErr):
    """
    Relay the output stream to stdout line by line
    :param name:
    :param src: source stream
    :param toStdErr: flag set if stderr is to be the dest
    :return:
    """

    global needPassword
    debug ("starting printer for %s" % name )
    line = ""
    while not finished:
        (line, done) = read(src, line)
        if done:
            out(toStdErr, line + "\n")
            flush(toStdErr)
            if line.find("Enter password for") >= 0:
                needPassword = True
            line = ""
    out(toStdErr, line)
    # closedown: read remainder of stream
    c = src.read(1)
    while c!="" :
        c = c.decode('utf-8')
        out(toStdErr, c)
        if c == "\n":
            flush(toStdErr)
        c = src.read(1)
    flush(toStdErr)
    src.close()

def read_input(name, exe):
    """
    Read input from stdin and send to process
    :param name:
    :param process: process to send input to
    :return:
    """
    global needPassword
    debug ("starting reader for %s" % name )
    while not finished:
        if needPassword:
            needPassword = False
            if sys.stdin.isatty():
                cred = getpass.getpass()
            else:
                cred = sys.stdin.readline().rstrip()
            exe.stdin.write(cred + "\n")

def debug(text):
    if DEBUG: print('[DEBUG] ' + text)


def error(text):
    print('[ERROR] ' + text)
    sys.stdout.flush()

def info(text):
    print(text)
    sys.stdout.flush()


def out(toStdErr, text) :
    """
    Write to one of the system output channels.
    This action does not add newlines. If you want that: write them yourself
    :param toStdErr: flag set if stderr is to be the dest
    :param text: text to write.
    :return:
    """
    if toStdErr:
        sys.stderr.write(text)
    else:
        sys.stdout.write(text)

def flush(toStdErr) :
    """
    Flush the output stream
    :param toStdErr: flag set if stderr is to be the dest
    :return:
    """
    if toStdErr:
        sys.stderr.flush()
    else:
        sys.stdout.flush()

def read(pipe, line):
    """
    read a char, append to the listing if there is a char that is not \n
    :param pipe: pipe to read from
    :param line: line being built up
    :return: (the potentially updated line, flag indicating newline reached)
    """

    c = pipe.read(1)
    if c != "":
        o = c.decode('utf-8')
        if o != '\n':
            line += o
            return line, False
        else:
            return line, True
    else:
        return line, False

def writePid(atlas_pid_file, process):
    f = open(atlas_pid_file, 'w')
    f.write(str(process.pid))
    f.close()

def exist_pid(pid):
    if  ON_POSIX:
        #check if process id exist in the current process table
        #See man 2 kill - Linux man page for info about the kill(pid,0) system function
        try:
            os.kill(pid, 0)
        except OSError as e :
            return e.errno == errno.EPERM
        else:
            return True

    elif IS_WINDOWS:
        #The os.kill approach does not work on Windows with python 2.7
        #the output from tasklist command is searched for the process id
        pidStr = str(pid)
        command='tasklist /fi  "pid eq %s"' % pidStr
        sub_process=subprocess.Popen(command, stdout = subprocess.PIPE, shell=False)
        sub_process.communicate()
        output = subprocess.check_output(command)
        output=split(" *",output)
        for line in output:
            if pidStr in line:
                return True
        return False
    #os other than nt or posix - not supported - need to delete the file to restart server if pid no longer exist
    return True

def wait_for_shutdown(pid, msg, wait):
    count = 0
    sys.stdout.write(msg)
    while exist_pid(pid):
        sys.stdout.write('.')
        sys.stdout.flush()
        sleep(1)
        if count > wait:
            break
        count = count + 1

    sys.stdout.write('\n')

def is_berkelydb(confdir):
    confFile = os.path.join(confdir, CONF_FILE)
    storageBackEnd = getConfig(confFile, STORAGE_BACKEND_CONF)
    if storageBackEnd is not None:
        storageBackEnd = storageBackEnd.strip()
    return storageBackEnd is not None and storageBackEnd == 'berkeleyje'

def is_hbase(confdir):
    confFile = os.path.join(confdir, CONF_FILE)
    storageBackEnd = getConfig(confFile, STORAGE_BACKEND_CONF)
    if storageBackEnd is not None:
        storageBackEnd = storageBackEnd.strip()
    return storageBackEnd is None or storageBackEnd == '' or storageBackEnd == 'hbase' or storageBackEnd == 'hbase2'

def is_hbase_local(confdir):
    if os.environ.get(MANAGE_LOCAL_HBASE, "False").lower() == 'false':
        return False

    confFile = os.path.join(confdir, CONF_FILE)
    return is_hbase(confdir) and grep(confFile, HBASE_STORAGE_LOCAL_CONF_ENTRY) is not None

def run_hbase_action(dir, action, hbase_conf_dir = None, logdir = None, wait=True):
    if IS_WINDOWS:
        if action == 'start':
            hbaseScript = 'start-hbase.cmd'
        else:
            hbaseScript = 'stop-hbase.cmd'
        if hbase_conf_dir is not None:
            cmd = [os.path.join(dir, hbaseScript), '--config', hbase_conf_dir]
        else:
            cmd = [os.path.join(dir, hbaseScript)]
    else:
        hbaseScript = 'hbase-daemon.sh'
        if hbase_conf_dir is not None:
            cmd = [os.path.join(dir, hbaseScript), '--config', hbase_conf_dir, action, 'master']
        else:
            cmd = [os.path.join(dir, hbaseScript), action, 'master']


    return runProcess(cmd, logdir, False, wait)

def is_solr(confdir):
    confdir = os.path.join(confdir, CONF_FILE)
    return grep(confdir, SOLR_INDEX_CONF_ENTRY) is not None

def is_cassandra_local(configdir):
    if os.environ.get(MANAGE_EMBEDDED_CASSANDRA, "False").lower() == 'false':
        return False

    return True

def is_solr_local(confdir):
    if os.environ.get(MANAGE_LOCAL_SOLR, "False").lower() == 'false':
        return False

    confdir = os.path.join(confdir, CONF_FILE)
    return is_solr_local_cloud_mode(confdir) or is_solr_local_standalone_mode(confdir)

def is_solr_local_cloud_mode(confdir):
    return grep(confdir, SOLR_INDEX_CONF_ENTRY) is not None and getConfig(confdir, SOLR_INDEX_MODE_CONF_ENTRY) == 'cloud' and grep(confdir, SOLR_INDEX_LOCAL_CLOUD_CONF_ENTRY) is not None


def is_solr_local_standalone_mode(confdir):
    return grep(confdir, SOLR_INDEX_CONF_ENTRY) is not None and getConfig(confdir, SOLR_INDEX_MODE_CONF_ENTRY) == 'http' and grep(confdir, SOLR_INDEX_LOCAL_STANDALONE_CONF_ENTRY) is not None

def is_elasticsearch_local():
    if os.environ.get(MANAGE_LOCAL_ELASTICSEARCH, "False").lower() == 'false':
        return False

    return True

def is_zookeeper_local(confdir):
    return is_berkelydb(confdir) or is_cassandra_local(confdir)

def get_solr_zk_url(confdir):
    confdir = os.path.join(confdir, CONF_FILE)
    return getConfig(confdir, SOLR_INDEX_ZK_URL)

def get_topics_to_create(confdir):
    confdir = os.path.join(confdir, CONF_FILE)
    topic_list = getConfig(confdir, TOPICS_TO_CREATE)
    if topic_list is not None:
        topics = topic_list.split(",")
    else:
        topics = [getConfigWithDefault("atlas.notification.hook.topic.name", "ATLAS_HOOK"), getConfigWithDefault("atlas.notification.entities.topic.name", "ATLAS_ENTITIES")]
    return topics

def get_atlas_url_port(confdir):
    port = None
    if '-port' in sys.argv:
        port = sys.argv[sys.argv.index('-port')+1]

    if port is None:
        confdir = os.path.join(confdir, CONF_FILE)
        enable_tls = getConfig(confdir, ATLAS_ENABLE_TLS)
        if enable_tls is not None and enable_tls.lower() == 'true':
            port = getConfigWithDefault(confdir, ATLAS_HTTPS_PORT, DEFAULT_ATLAS_HTTPS_PORT)
        else:
            port = getConfigWithDefault(confdir, ATLAS_HTTP_PORT, DEFAULT_ATLAS_HTTP_PORT)

    print("Starting Atlas server on port: %s" % port)
    return port

def get_atlas_url_host(confdir):
    confdir = os.path.join(confdir, CONF_FILE)
    host = getConfigWithDefault(confdir, ATLAS_SERVER_BIND_ADDRESS, DEFAULT_ATLAS_SERVER_HOST)
    if (host == '0.0.0.0'):
        host = DEFAULT_ATLAS_SERVER_HOST
    print("\nStarting Atlas server on host: %s" % host)
    return host

def wait_for_startup(confdir, wait):
    count = 0
    host = get_atlas_url_host(confdir)
    port = get_atlas_url_port(confdir)
    while True:
        try:
            s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            s.settimeout(1)
            s.connect((host, int(port)))
            s.close()
            break
        except Exception as e:
            # Wait for 1 sec before next ping
            sys.stdout.write('.')
            sys.stdout.flush()
            sleep(1)

        if count > wait:
            s.close()
            break

        count = count + 1

    sys.stdout.write('\n')

def run_zookeeper(dir, action, logdir = None, wait=True):
    zookeeperScript = "zkServer.sh"

    if IS_WINDOWS:
        zookeeperScript = "zkServer.cmd"

    cmd = [os.path.join(dir, zookeeperScript), action, os.path.join(dir, '../../conf/zookeeper/zoo.cfg')]

    return runProcess(cmd, logdir, False, wait)

def start_elasticsearch(dir, logdir = None, wait=True):

    elasticsearchScript = "elasticsearch"

    if IS_WINDOWS:
        elasticsearchScript = "elasticsearch.bat"

    cmd = [os.path.join(dir, elasticsearchScript), '-d', '-p', os.path.join(logdir, 'elasticsearch.pid')]

    processVal = runProcess(cmd, logdir, False, wait)
    sleep(6)
    return processVal

def run_solr(dir, action, zk_url = None, port = None, logdir = None, wait=True, homedir = None):

    solrScript = "solr"

    if IS_WINDOWS:
        solrScript = "solr.cmd"

    if zk_url is None:
        if port is None:
            cmd = [os.path.join(dir, solrScript), action]
        else:
            cmd = [os.path.join(dir, solrScript), action, '-p', str(port)]
    else:
        if port is None:
            cmd = [os.path.join(dir, solrScript), action, '-z', zk_url]
        else:
            cmd = [os.path.join(dir, solrScript), action, '-z', zk_url, '-p', port]

    if homedir is not None:
        if not os.path.exists(homedir) :
            os.makedirs(homedir)
        #Copy solr.xml from installation directory to the solr home directory
        srcSolrXmlPath = os.path.join(solrServerDir(), "solr", "solr.xml")
        destSolrXmlPath = os.path.join(homedir, "solr.xml")
        if not os.path.exists(destSolrXmlPath) :
            print("solr.xml doesn't exist in " + homedir + ", copying from " + srcSolrXmlPath)
            copyCmd = ["cp", srcSolrXmlPath, homedir]
            runProcess(copyCmd, logdir, False, True)
        cmd.append('-s')
        cmd.append(homedir)

    return runProcess(cmd, logdir, False, wait)

def create_solr_collection(dir, confdir, index, logdir = None, wait=True):
    solrScript = "solr"

    if IS_WINDOWS:
        solrScript = "solr.cmd"

    cmd = [os.path.join(dir, solrScript), 'create', '-c', index, '-d', confdir,  '-shards',  solrShards(),  '-replicationFactor', solrReplicationFactor()]

    return runProcess(cmd, logdir, False, wait)

def configure_hbase(dir):
    env_conf_dir = os.environ.get(HBASE_CONF_DIR)
    conf_dir = os.path.join(dir, "hbase", CONF)
    tmpl_dir = os.path.join(dir, CONF, "hbase")
    data_dir = dataDir(atlasDir())

    if env_conf_dir is None or env_conf_dir == conf_dir:
        hbase_conf_file = "hbase-site.xml"

        tmpl_file = os.path.join(tmpl_dir, hbase_conf_file + ".template")
        if IS_WINDOWS:
             url_prefix="file:///"
        else:
             url_prefix="file://"

        conf_file = os.path.join(conf_dir, hbase_conf_file)
        if os.path.exists(tmpl_file):

            debug ("Configuring " + tmpl_file + " to " + conf_file)
            f = open(tmpl_file,'r')
            template = f.read()
            f.close()

            config = template.replace("${hbase_home}", dir)
            config = config.replace("${atlas_data}", data_dir)
            config = config.replace("${url_prefix}", url_prefix)

            f = open(conf_file,'w')
            f.write(config)
            f.close()
            os.remove(tmpl_file)

def configure_zookeeper(dir):

    conf_dir = os.path.join(dir, CONF, "zookeeper")
    zk_conf_file = "zoo.cfg"
    tmpl_file = os.path.join(conf_dir, zk_conf_file + ".template")

    conf_file = os.path.join(conf_dir, zk_conf_file)
    if os.path.exists(tmpl_file):
        debug ("Configuring " + tmpl_file + " to " + conf_file)

        f = open(tmpl_file,'r')
        template = f.read()
        f.close()

        config = template.replace("${atlas_home}", dir)

        f = open(conf_file,'w')
        f.write(config)
        f.close()
        os.remove(tmpl_file)

def configure_cassandra(dir):

    conf_dir = os.path.join(dir, CONF)
    cassandra_conf_file = "cassandra.yml"
    tmpl_file = os.path.join(conf_dir, cassandra_conf_file + ".template")

    conf_file = os.path.join(conf_dir, cassandra_conf_file)
    if os.path.exists(tmpl_file):
        debug ("Configuring " + tmpl_file + " to " + conf_file)

        f = open(tmpl_file,'r')
        template = f.read()
        f.close()

        config = template.replace("${atlas_home}", dir)

        f = open(conf_file,'w')
        f.write(config)
        f.close()
        os.remove(tmpl_file)

def server_already_running(pid):
    print("Atlas server is already running under process %s" % pid)
    sys.exit()

def server_pid_not_running(pid):
    print("The Server is no longer running with pid %s" %pid)

def grep(file, value):
    for line in open(file).readlines():
        if re.match(value, line):
           return line
    return None

def getConfig(file, key):
    key = key + "\s*="
    for line in open(file).readlines():
        if re.match(key, line):
            return line.split('=')[1].strip()
    return None

def getConfigWithDefault(file, key, defaultValue):
    value = getConfig(file, key)
    if value is None:
        value = defaultValue
    return value

def isCygwin():
    return platform.system().startswith("CYGWIN")

# Convert the specified cygwin-style pathname to Windows format,
# using the cygpath utility.  By default, path is assumed
# to be a file system pathname.  If isClasspath is True,
# then path is treated as a Java classpath string.
def convertCygwinPath(path, isClasspath=False):
    if (isClasspath):
        cygpathArgs = ["cygpath", "-w", "-p", path]
    else:
        cygpathArgs = ["cygpath", "-w", path]
    windowsPath = subprocess.Popen(cygpathArgs, stdout=subprocess.PIPE).communicate()[0]
    windowsPath = windowsPath.strip()
    return windowsPath
