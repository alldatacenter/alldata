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


import subprocess
import shutil
from config import Config
import os
from docker import Docker
from docker_image.launcher_agent import replace_conf


class VM:
    """
    This class represents VM, including its network setting and the possible Docker instance list
    """
    def __init__(self, external_ip, domain_name, weave_dns_ip, weave_ip_mask):
        self.external_ip = external_ip
        self.domain_name = domain_name
        self.hostname = self._gce_get_hostname(domain_name)
        self.weave_domain_name = self._get_weave_domain_name(self.hostname)
        self.weave_dns_ip = weave_dns_ip
        self.weave_internal_ip = ""
        self.weave_ip_mask = weave_ip_mask
        self.docker_list = []

    def to_json(self):
        """
        create a map to hold the information of the VM instance
        :return: A map, which is JSON format object.
        """
        vm_json = {}
        vm_json["external_ip"] = self.external_ip
        vm_json["domain_name"] = self.domain_name
        vm_json["weave_dns_ip"] = self.weave_dns_ip
        vm_json["weave_internal_ip"] = self.weave_internal_ip
        vm_json["weave_domain_name"] = self.weave_domain_name
        vm_json["weave_ip_mask"] = self.weave_ip_mask
        vm_json["docker_list"] = []
        for docker in self.docker_list:
            vm_json["docker_list"].append(docker.to_json())
        return vm_json

    @staticmethod
    def load_from_json(json_data):
        """
        load the VM information from a JSON object
        :param json_data: a map, which is a JSON object
        :return: a VM object
        """
        external_ip = json_data["external_ip"]
        domain_name = json_data["domain_name"]
        weave_dns_ip = json_data["weave_dns_ip"]
        weave_internal_ip = json_data["weave_internal_ip"]
        weave_domain_name = json_data["weave_domain_name"]
        weave_ip_mask = json_data["weave_ip_mask"]
        docker_list = []
        for json_docker in json_data["docker_list"]:
            docker_list.append(Docker.load_from_json(json_docker))

        vm = VM(external_ip, domain_name, weave_dns_ip, weave_ip_mask)
        vm.docker_list = docker_list
        vm.weave_internal_ip = weave_internal_ip
        vm.weave_domain_name = weave_domain_name
        return vm

    def _get_weave_domain_name(self, hostname):
        """
        get the Weave domain name of the VM
        :param hostname: the hostname of the VM
        :return:the Weave domain name
        """
        return "{0}.weave.local".format(hostname)

    def _gce_get_hostname(self, domain_name):
        """
        The hostname of GCE VM is the first part of the internal domain name
        :param domain_name: the internal domain name of GCE VM
        :return: the hostname of GCE VM
        """
        return domain_name.split(".")[0]

    def get_ssh_output_file_path(self):
        """
        get the file name to hold the SSH output of the VM
        :return: a file name
        """
        vm_output_file_path = "{0}/vm-{1}-{2}".format(Config.ATTRIBUTES["output_folder"],
                                                      self.hostname, self.external_ip)
        return vm_output_file_path

    def add_docker(self, docker):
        """
        add a Docker instance to the VM instance
        :param docker: the docker instance
        :return: None
        """
        self.docker_list.append(docker)

    def _centos7_weave_install(self):
        """
        install Weave on this VM
        :return: None
        """
        subprocess.call("./Linux/CentOS7/weave_install.sh")

    def _set_weave_network(self, vm_external_ip_list, weave_dns_ip):
        """
        launch Weave, make this VM connect with other VM
        :param vm_external_ip_list: external IP list of all VMs
        :param weave_dns_ip: the IP of DNS in this VM
        :return: None
        """
        # add other VMs and the ambari-server to set up connections
        weave_launch_command = ["sudo", "weave", "launch"]
        weave_launch_command.extend(vm_external_ip_list)

        print weave_launch_command

        with open(os.devnull, 'w') as shutup:
            subprocess.call(weave_launch_command, stdout=shutup)

        # establish DNS server
        weave_dns_ip_with_mask = "{0}/{1}".format(weave_dns_ip, Config.ATTRIBUTES["weave_ip_mask"])
        weave_launch_dns_command = ["sudo", "weave", "launch-dns", weave_dns_ip_with_mask]
        subprocess.call(weave_launch_dns_command)

    def _centos7_docker_install(self):
        """
        install Docker on this VM
        :return: None
        """
        subprocess.call("./Linux/CentOS7/docker_install.sh")

    def _build_docker_image(self, image_name):
        """
        build docker image
        :param image_name: the name of the Docker image
        :return: None
        """
        # choose the right Dockerfile
        target_dockerfile_name = "docker_image/{0}".format(Config.ATTRIBUTES["dockerfile_name"])
        standard_dockerfile_name = "docker_image/Dockerfile"
        shutil.copyfile(target_dockerfile_name, standard_dockerfile_name)
        with open(os.devnull, 'w') as shutup:
            subprocess.call(["sudo", "docker", "build", "-t", image_name, "docker_image/"])
            # subprocess.call(["sudo", "docker", "build", "-q", "-t", image_name, "docker_image/"], stdout=shutup)
        os.remove(standard_dockerfile_name)

    def _pull_docker_image(self, image_name):
        with open(os.devnull, 'w') as shutup:
            subprocess.call(["sudo", "docker", "pull", image_name], stdout=shutup)

    def _launch_containers(self, docker_image, server_weave_ip):
        """
        launch Docker containers, issue the script to install,
        configure and launch Ambari-gent inside Docker.
        :param docker_image: the name of the Docker image
        :param server_weave_ip: Weave internal IP of Ambari-server
        :return: None
        """
        for docker in self.docker_list:
            docker_ip_with_mask = "{0}/{1}".format(docker.ip, docker.mask)
            cmd = "python /launcher_agent.py {0} {1}; /bin/bash".format(server_weave_ip, docker.ip)

            command = ["sudo", "weave", "run", docker_ip_with_mask, "-d", "-it",
                       "-h", docker.weave_domain_name,
                       "--name", docker.get_container_name(),
                       docker_image, "bash", "-c", cmd]
            print command
            subprocess.call(command)

    def _set_docker_partition(self, mount_point):
        """
        set docker container to use the disk storage of other partitions.
        :param mount_point: the mount point of the partition to be used
        :return: None
        """
        subprocess.call(["./Linux/CentOS7/set_docker_partition.sh", mount_point])

    def run_ambari_server(self):
        """
        set up Weave network, run Ambari-server in this VM
        :return: None
        """
        # set up network, run script inside the network directory
        os.chdir("network")
        subprocess.call(["./set_ambari_server_network.sh", self.weave_internal_ip,
                         self.weave_dns_ip, self.weave_ip_mask])
        os.chdir("..")

        # install ambari server and start service
        subprocess.call(["./server/ambari_server_install.sh"])

        # start service
        subprocess.call(["./server/ambari_server_start.sh"])

    def run_service_server(self, ambari_server_weave_ip, ambari_server_external_ip):
        """
        set up Weave network, run Ambari-agent in this VM
        :param ambari_server_weave_ip: the Weave IP of Ambari-server
        :param ambari_server_external_ip: the external IP of Ambari-server
        :return: None
        """
        # set up network, run script inside the network directory
        os.chdir("network")
        subprocess.call(["./set_host_network.sh", self.weave_internal_ip,
                         self.weave_dns_ip, self.weave_ip_mask, self.hostname,
                         self.weave_domain_name, ambari_server_external_ip])
        os.chdir("..")

        # install ambari agent and start service
        subprocess.call(["./docker_image/ambari_agent_install.sh"])
        replace_conf(ambari_server_weave_ip)

        # start service
        subprocess.call(["./docker_image/ambari_agent_start.sh"])

        # forward public IP to Weave IP for server UI access
        port_list = Config.ATTRIBUTES["server_port_list"].split(",")
        for port in port_list:
            subprocess.call(["./network/set_ui_port_forward.sh", self.external_ip, self.weave_internal_ip, port])


    def run_docker(self, server_weave_ip, vm_ip_list):
        """
        run all Docker containers with Ambari-agent inside
        :param server_weave_ip: Weave internal IP of Ambari-server
        :param vm_ip_list: external IP list of all other VMs to be connected
                each docker vm connect to each other and service VM (and ambari-server)
        :return: None
        """
        self._centos7_docker_install()

        if "use_partition" in Config.ATTRIBUTES:
            self._set_docker_partition(Config.ATTRIBUTES["use_partition"])

        self._centos7_weave_install()

        image_name = Config.ATTRIBUTES["docker_image_name"]
        if "pull_docker_hub" in Config.ATTRIBUTES and Config.ATTRIBUTES["pull_docker_hub"] == "yes":
            self._pull_docker_image(image_name)
        else:
            self._build_docker_image(image_name)

        self._set_weave_network(vm_ip_list, self.weave_dns_ip)
        self._launch_containers(Config.ATTRIBUTES["docker_image_name"], server_weave_ip)

    @staticmethod
    def get_ambari_agent_vm_name(cluster_name):
        return "{0}-agent-vm".format(cluster_name)

    @staticmethod
    def get_ambari_server_vm_name(cluster_name):
        return "{0}-ambari-server".format(cluster_name)

    @staticmethod
    def get_service_server_vm_name(cluster_name):
        return "{0}-service-server".format(cluster_name)
