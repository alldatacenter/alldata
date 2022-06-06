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
from unittest import TestCase
from resource_management.libraries.functions.namenode_ha_utils import \
  get_nameservices


class TestNamenodeHaUtils(TestCase):

  def test_get_nameservice(self):
    # our cluster is HAA

    # dfs.internal.nameservices in hdfs-site
    hdfs_site = {
      "dfs.internal.nameservices": "HAA",
      "dfs.nameservices": "HAA,HAB",
      "dfs.ha.namenodes.HAA": "nn1,nn2",
      "dfs.ha.namenodes.HAB": "nn1,nn2",
      "dfs.namenode.rpc-address.HAA.nn1": "hosta1:8020",
      "dfs.namenode.rpc-address.HAA.nn2": "hosta2:8020",
      "dfs.namenode.rpc-address.HAB.nn1": "hostb1:8020",
      "dfs.namenode.rpc-address.HAB.nn2": "hostb2:8020",
    }

    self.assertEqual(["HAA"], get_nameservices(hdfs_site))

    # dfs.internal.nameservices not in hdfs-site
    hdfs_site = {
      "dfs.nameservices": "HAA,HAB",
      "dfs.ha.namenodes.HAA": "nn1,nn2",
      "dfs.ha.namenodes.HAB": "nn1,nn2",
      "dfs.namenode.rpc-address.HAA.nn1": "hosta1:8020",
      "dfs.namenode.rpc-address.HAA.nn2": "hosta2:8020",
      "dfs.namenode.rpc-address.HAB.nn1": "hostb1:8020",
      "dfs.namenode.rpc-address.HAB.nn2": "hostb2:8020",
    }

    self.assertEqual(["HAA"], get_nameservices(hdfs_site))

    # Non HA
    hdfs_site = {}

    self.assertEqual([], get_nameservices(hdfs_site))

    # federated config dfs.internal.nameservices in hdfs-site
    hdfs_site = {
      "dfs.internal.nameservices": "ns1,ns2",
      "dfs.nameservices": "ns1,ns2,exns1,exns2"
    }

    self.assertEqual(["ns1","ns2"], get_nameservices(hdfs_site))
