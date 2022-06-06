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

import os.path
import json
from config import Config


class Data:
    def __init__(self):
        self.data_filename = Config.ATTRIBUTES["cluster_info_file"]

    def _load_data(self):
        """
        load all data from JSON file
        :return: a map, which is a JSON format object
        """
        json_data = {"clusters": []}
        if os.path.isfile(self.data_filename):
            with open(self.data_filename) as f:
                json_data = json.load(f)
        return json_data

    def _save_data(self, json_data):
        """
        save the JSON object into a file
        :param json_data: a map, which is a JSON format object
        :return: None
        """
        with open(self.data_filename, "w") as f:
            json.dump(json_data, f, indent=4, separators=(',', ': '))

    def add_new_cluster(self, cluster):
        """
        add a new cluster into the JSON file
        :param cluster: the cluster instance
        :return: None
        """
        json_data = self._load_data()
        new_cluster_json = cluster.to_json()
        json_data["clusters"].insert(0, new_cluster_json)
        self._save_data(json_data)

    def set_cluster_state(self, cluster_name, state):
        """
        set the state of a cluster into JSON file
        :param cluster_name: the name of the cluster
        :param state: the name of the state
        :return: None
        """
        json_data = self._load_data()
        for cluster in json_data["clusters"]:
            if cluster["cluster_name"] == cluster_name:
                cluster["state"] = state
                break
        self._save_data(json_data)

    def read_cluster_json(self, cluster_name):
        """
        get the JSON object for the cluster
        :param cluster_name: the name of cluster
        :return: a map which is a JSON object or None if the cluster is not found
        """
        json_data = self._load_data()
        for cluster_json in json_data["clusters"]:
            if cluster_json["cluster_name"] == cluster_name:
                return cluster_json
        return None

    def print_cluster_summary_list(self):
        """
        get a brief description of all the cluster from the JSON file
        :return: a list of tuple. The elements of the tuple are:
                 cluster_name, state, agent_number,
                 service_server_num, ambari_server_num, create_time
        """
        print "(cluster_name, state, agent_number, service_server_num, ambari_server_num, create_time)"

        json_data = self._load_data()
        for cluster in json_data["clusters"]:
            cluster_name = cluster["cluster_name"]
            state = cluster["state"]
            create_time = cluster["create_time"]
            agent_number = 0
            for agent_vm in cluster["ambari_agent_vm_list"]:
                agent_number += len(agent_vm["docker_list"])
            service_server_num = len(cluster["service_server_vm_list"])
            ambari_server_num = len(cluster["ambari_server_vm"])
            print cluster_name, state, agent_number, service_server_num, ambari_server_num, create_time
