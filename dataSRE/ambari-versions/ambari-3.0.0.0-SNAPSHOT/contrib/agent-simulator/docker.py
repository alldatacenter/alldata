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

from config import Config


class Docker:
    """
    Docker represents a Docker container, each with its IP and domain name
    """
    def __init__(self, ip, mask, weave_domain_name):
        self.ip = ip
        self.mask = mask
        self.weave_domain_name = weave_domain_name

    def to_json(self):
        """
        create a map to hold the information of the Docker instance
        :return: A map, which is JSON format object.
        """
        docker_json = {}
        docker_json["weave_ip"] = "{0}/{1}".format(self.ip, self.mask)
        docker_json["weave_domain_name"] = self.weave_domain_name
        return docker_json

    @staticmethod
    def load_from_json(json_data):
        """
        load the docker information from a JSON object
        :param json_data: a map, which is a JSON object
        :return: a Docker object
        """
        ip = json_data["weave_ip"].split("/")[0]
        mask = json_data["weave_ip"].split("/")[1]
        weave_domain_name = json_data["weave_domain_name"]
        return Docker(ip, mask, weave_domain_name)

    def __str__(self):
        return str(self.ip) + "/" + str(self.mask) + " " + self.weave_domain_name

    @staticmethod
    def get_weave_domain_name(cluster_name, index):
        """
        given the index and the name of cluster, generate the  Weave domain name for the docker
        :param cluster_name: the name of the cluster
        :param index: a number
        :return: Weave domain name of the docker container
        """
        return "{0}-{1}-{2}.{3}".format(Config.ATTRIBUTES["container_hostname_fix"],
                                        index, cluster_name, "weave.local")

    @staticmethod
    def get_pattern_presentation(cluster_name, range_str):
        return Docker.get_weave_domain_name(cluster_name, range_str)

    def get_index(self):
        """
        extract the index of the docker within the cluster
        :return: the index
        """
        return self.weave_domain_name.split("-")[1]

    def get_container_name(self):
        """
        :return: the name of the container
        """
        return self.get_hostname()

    def get_hostname(self):
        return self.weave_domain_name.split(".")[0]
