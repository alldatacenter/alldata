#!/usr/bin/env python

'''
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
'''
import sys
from os import environ
from mock import patch
from mock import call
import unittest
import logging
import atlas_config as mc
import atlas_start as atlas
import platform

IS_WINDOWS = platform.system() == "Windows"
logger = logging.getLogger()


class TestMetadata(unittest.TestCase):
    # A map of mock values for the get_config calls
    mock_values = {
        'port': 21000,
        'host': "locahost",
        'atlas.graph.index.search.solr.zookeeper-url': "localhost:9838",
        'atlas.server.http.port': 21000,
        'atlas.server.https.port': 21443
    }

    # The getConfig mock has to be configured here to return the expected mock values
    def get_config_mock_side_effect(*args, **kwargs):
        print("get_config_mock_side_effect (" + args[2] + ")")
        return TestMetadata.mock_values.get(args[2])

    # The getConfig mock has to be configured here to return the expected mock values
    def get_default_config_mock_side_effect(*args, **kwargs):
        print("get_default_config_mock_side_effect (" + args[3] + ")")
        return TestMetadata.mock_values.get(args[3])

    @patch.object(mc, "runProcess")
    @patch.object(mc, "configure_hbase")
    @patch.object(mc, "getConfig")
    @patch.object(mc, "getConfigWithDefault")
    @patch.object(mc, "grep")
    @patch.object(mc, "exist_pid")
    @patch.object(mc, "writePid")
    @patch.object(mc, "executeEnvSh")
    @patch.object(mc, "atlasDir")
    @patch.object(mc, "expandWebApp")
    @patch("os.path.exists")
    @patch.object(mc, "java")
    @patch.object(mc, "is_hbase_local")
    @patch.object(mc, "is_solr_local")
    @patch.object(mc, "wait_for_startup")
    def test_main_embedded(self, wait_for_startup_mock, is_solr_local_mock, is_hbase_local_mock, java_mock, exists_mock, expandWebApp_mock,
                           atlasDir_mock, executeEnvSh_mock, writePid_mock, exist_pid_mock, grep_mock,
                           getConfigWithDefault_mock, getConfig_mock, configure_hbase_mock, runProcess_mock):
        sys.argv = []
        exists_mock.return_value = True
        expandWebApp_mock.return_value = "webapp"
        atlasDir_mock.return_value = "atlas_home"
        is_hbase_local_mock.return_value = True
        is_solr_local_mock.return_value = True
        wait_for_startup_mock.return_value = True

        exist_pid_mock(789)
        exist_pid_mock.assert_called_with(789)
        grep_mock.return_value = "hbase"
        getConfig_mock.side_effect = self.get_config_mock_side_effect
        getConfigWithDefault_mock.side_effect = self.get_default_config_mock_side_effect

        atlas.main()
        self.assertTrue(configure_hbase_mock.called)

        if IS_WINDOWS:
            calls = [call(['atlas_home\\hbase\\bin\\start-hbase.cmd', '--config', 'atlas_home\\hbase\\conf'],
                          'atlas_home\\logs', False, True),
                     call(['atlas_home\\solr\\bin\\solr.cmd', 'start', '-z', 'localhost:9838', '-p', '9838'],
                          'atlas_home\\logs', False, True),
                     call(['atlas_home\\solr\\bin\\solr.cmd', 'create', '-c', 'vertex_index', '-d',
                           'atlas_home\\conf\\solr', '-shards', '1',
                           '-replicationFactor', '1'], 'atlas_home\\logs', False, True),
                     call(['atlas_home\\solr\\bin\\solr.cmd', 'create', '-c', 'edge_index', '-d',
                           'atlas_home\\conf\\solr', '-shards', '1',
                           '-replicationFactor', '1'], 'atlas_home\\logs', False, True),
                     call(['atlas_home\\solr\\bin\\solr.cmd', 'create', '-c', 'fulltext_index', '-d',
                           'atlas_home\\conf\\solr', '-shards', '1',
                           '-replicationFactor', '1'], 'atlas_home\\logs', False, True)]

            runProcess_mock.assert_has_calls(calls)
        else:
            calls = [
                call(['atlas_home/hbase/bin/hbase-daemon.sh', '--config', 'atlas_home/hbase/conf', 'start', 'master'],
                     'atlas_home/logs', False, True),
                call(['atlas_home/solr/bin/solr', 'start', '-z', 'localhost:9838', '-p', '9838', '-s', 'atlas_home/data/solr'], 'atlas_home/logs',
                     False, True),
                call(['atlas_home/solr/bin/solr', 'create', '-c', 'vertex_index', '-d',
                      'atlas_home/conf/solr', '-shards', '1', '-replicationFactor',
                      '1'], 'atlas_home/logs', False, True),
                call(['atlas_home/solr/bin/solr', 'create', '-c', 'edge_index', '-d',
                      'atlas_home/conf/solr', '-shards', '1', '-replicationFactor',
                      '1'], 'atlas_home/logs', False, True),
                call(['atlas_home/solr/bin/solr', 'create', '-c', 'fulltext_index', '-d',
                      'atlas_home/conf/solr', '-shards', '1', '-replicationFactor',
                      '1'], 'atlas_home/logs', False, True)]

            runProcess_mock.assert_has_calls(calls)

        self.assertTrue(java_mock.called)
        if IS_WINDOWS:

            java_mock.assert_called_with(
                'org.apache.atlas.Atlas',
                ['-app', 'atlas_home\\server\\webapp\\atlas'],
                'atlas_home\\conf;atlas_home\\server\\webapp\\atlas\\WEB-INF\\classes;atlas_home\\server\\webapp\\atlas\\WEB-INF\\lib\\*;atlas_home\\libext\\*;atlas_home\\hbase\\conf',
                ['-Datlas.log.dir=atlas_home\\logs', '-Datlas.log.file=application.log', '-Datlas.home=atlas_home',
                 '-Datlas.conf=atlas_home\\conf', '-Xmx1024m',
                 '-Dlog4j.configuration=atlas-log4j.xml', '-Djava.net.preferIPv4Stack=true', '-server'],
                'atlas_home\\logs')

        else:
            java_mock.assert_called_with(
                'org.apache.atlas.Atlas',
                ['-app', 'atlas_home/server/webapp/atlas'],
                'atlas_home/conf:atlas_home/server/webapp/atlas/WEB-INF/classes:atlas_home/server/webapp/atlas/WEB-INF/lib/*:atlas_home/libext/*:atlas_home/hbase/conf',
                ['-Datlas.log.dir=atlas_home/logs', '-Datlas.log.file=application.log', '-Datlas.home=atlas_home',
                 '-Datlas.conf=atlas_home/conf', '-Xmx1024m',
                 '-Dlog4j.configuration=atlas-log4j.xml', '-Djava.net.preferIPv4Stack=true', '-server'],
                'atlas_home/logs')

        pass

    @patch.object(mc, "runProcess")
    @patch.object(mc, "configure_hbase")
    @patch.object(mc, "getConfig")
    @patch.object(mc, "getConfigWithDefault")
    @patch.object(mc, "grep")
    @patch.object(mc, "exist_pid")
    @patch.object(mc, "writePid")
    @patch.object(mc, "executeEnvSh")
    @patch.object(mc, "atlasDir")
    @patch.object(mc, "expandWebApp")
    @patch("os.path.exists")
    @patch.object(mc, "java")
    @patch.object(mc, "is_hbase_local")
    @patch.object(mc, "is_solr_local")
    @patch.object(mc, "wait_for_startup")
    def test_main_default(self, wait_for_startup_mock, is_solr_local_mock, is_hbase_local_mock, java_mock, exists_mock, expandWebApp_mock,
                          atlasDir_mock, executeEnvSh_mock, writePid_mock, exist_pid_mock, grep_mock,
                          getConfigWithDefault_mock, getConfig_mock, configure_hbase_mock, runProcess_mock):
        sys.argv = []
        exists_mock.return_value = True
        expandWebApp_mock.return_value = "webapp"
        atlasDir_mock.return_value = "atlas_home"
        is_hbase_local_mock.return_value = False
        is_solr_local_mock.return_value = False
        wait_for_startup_mock.return_value = True

        exist_pid_mock(789)
        exist_pid_mock.assert_called_with(789)
        grep_mock.return_value = "hbase"
        getConfig_mock.side_effect = self.get_config_mock_side_effect
        getConfigWithDefault_mock.side_effect = self.get_config_mock_side_effect

        atlas.main()
        self.assertFalse(configure_hbase_mock.called)

        if IS_WINDOWS:
            calls = [call(['atlas_home\\hbase\\bin\\start-hbase.cmd', '--config', 'atlas_home\\hbase\\conf'],
                          'atlas_home\\logs', False, True),
                     call(['atlas_home\\solr\\bin\\solr.cmd', 'start', '-z', 'localhost:9838', '-p', '9838'],
                          'atlas_home\\logs', False, True),
                     call(['atlas_home\\solr\\bin\\solr.cmd', 'create', '-c', 'vertex_index', '-d',
                           'atlas_home\\conf\\solr', '-shards', '1',
                           '-replicationFactor', '1'], 'atlas_home\\logs', False, True),
                     call(['atlas_home\\solr\\bin\\solr.cmd', 'create', '-c', 'edge_index', '-d',
                           'atlas_home\\conf\\solr', '-shards', '1',
                           '-replicationFactor', '1'], 'atlas_home\\logs', False, True),
                     call(['atlas_home\\solr\\bin\\solr.cmd', 'create', '-c', 'fulltext_index', '-d',
                           'atlas_home\\conf\\solr', '-shards', '1',
                           '-replicationFactor', '1'], 'atlas_home\\logs', False, True)]

            runProcess_mock.assert_not_called(calls)
        else:
            calls = [
                call(['atlas_home/hbase/bin/hbase-daemon.sh', '--config', 'atlas_home/hbase/conf', 'start', 'master'],
                     'atlas_home/logs', False, True),
                call(['atlas_home/solr/bin/solr', 'start', '-z', 'localhost:9838', '-p', '9838'], 'atlas_home/logs',
                     False, True),
                call(['atlas_home/solr/bin/solr', 'create', '-c', 'vertex_index', '-d',
                      'atlas_home/solr/server/solr/configsets/_default/conf', '-shards', '1', '-replicationFactor',
                      '1'], 'atlas_home/logs', False, True),
                call(['atlas_home/solr/bin/solr', 'create', '-c', 'edge_index', '-d',
                      'atlas_home/solr/server/solr/configsets/_default/conf', '-shards', '1', '-replicationFactor',
                      '1'], 'atlas_home/logs', False, True),
                call(['atlas_home/solr/bin/solr', 'create', '-c', 'fulltext_index', '-d',
                      'atlas_home/solr/server/solr/configsets/_default/conf', '-shards', '1', '-replicationFactor',
                      '1'], 'atlas_home/logs', False, True)]

            runProcess_mock.assert_not_called(calls)

        self.assertTrue(java_mock.called)
        if IS_WINDOWS:

            java_mock.assert_called_with(
                'org.apache.atlas.Atlas',
                ['-app', 'atlas_home\\server\\webapp\\atlas'],
                'atlas_home\\conf;atlas_home\\server\\webapp\\atlas\\WEB-INF\\classes;atlas_home\\server\\webapp\\atlas\\WEB-INF\\lib\\*;atlas_home\\libext\\*;atlas_home\\hbase\\conf',
                ['-Datlas.log.dir=atlas_home\\logs', '-Datlas.log.file=application.log', '-Datlas.home=atlas_home',
                 '-Datlas.conf=atlas_home\\conf', '-Xmx1024m',
                 '-Dlog4j.configuration=atlas-log4j.xml', '-Djava.net.preferIPv4Stack=true', '-server'],
                'atlas_home\\logs')

        else:
            java_mock.assert_called_with(
                'org.apache.atlas.Atlas',
                ['-app', 'atlas_home/server/webapp/atlas'],
                'atlas_home/conf:atlas_home/server/webapp/atlas/WEB-INF/classes:atlas_home/server/webapp/atlas/WEB-INF/lib/*:atlas_home/libext/*:atlas_home/hbase/conf',
                ['-Datlas.log.dir=atlas_home/logs', '-Datlas.log.file=application.log', '-Datlas.home=atlas_home',
                 '-Datlas.conf=atlas_home/conf', '-Xmx1024m',
                 '-Dlog4j.configuration=atlas-log4j.xml', '-Djava.net.preferIPv4Stack=true', '-server'],
                'atlas_home/logs')

        pass

    def test_jar_java_lookups_fail(self):
        java_home = environ.get("JAVA_HOME", None)
        if java_home != None:
            del environ['JAVA_HOME']
        orig_path = environ.get("PATH", None)
        environ['PATH'] = "/dev/null"

        self.assertRaises(EnvironmentError, mc.jar, "foo")
        self.assertRaises(EnvironmentError, mc.java, "empty", "empty", "empty", "empty")

        if java_home != None:
            environ['JAVA_HOME'] = java_home
        if orig_path != None:
            environ['PATH'] = orig_path

    @patch.object(mc, "runProcess")
    @patch.object(mc, "which", return_value="foo")
    def test_jar_java_lookups_succeed_from_path(self, which_mock, runProcess_mock):
        java_home = environ.get("JAVA_HOME", None)
        if java_home != None:
            del environ['JAVA_HOME']

        mc.jar("foo")
        mc.java("empty", "empty", "empty", "empty")

        if java_home != None:
            environ['JAVA_HOME'] = java_home


if __name__ == "__main__":
    logging.basicConfig(format='%(asctime)s %(message)s', level=logging.DEBUG)
    unittest.main()
