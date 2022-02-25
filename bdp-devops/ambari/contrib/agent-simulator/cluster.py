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
import datetime
from config import Config
from docker import Docker
from vm import VM
import os
import time
from log import Log
from data import Data


class Cluster:
    """
    The Cluster instance holds a list of VMs,
    it has the methods to request cluster, generate information and run all Ambari-agent and Ambari-server
    """

    # The constants represent the state of the cluster
    # A newly requested cluster is in FREE state
    STATE_FREE = "FREE"
    # A cluster with running Ambari-server and Ambari-agents is in RUNNING state
    STATE_RUNNING = "RUNNING"
    # A cluster is merged into another cluster and running, is in MERGE state
    # the name of the extended cluster is directly following the state String in JSON
    STATE_MERGE = "MERGE"

    def __init__(self):
        self.cluster_name = ""
        self.state = ""
        self.create_time = ""
        # The list should only has one or zero VM, which holds the Ambari-server
        self.ambari_server_vm = []
        # The list of VMs, with Ambari-agents directly inside (not in Docker)
        self.service_server_vm_list = []
        # The list of VMs, each will hold multiple Docker containers with Ambari-agent inside
        self.ambari_agent_vm_list = []

    def _get_int_interval(self, int_list):
        """
        get the interval of the integer list
        example: input[4,5,6,1,2,3], output [(1,3),(4,6)]
        example: input[4,5,6,100,2,3], output [(2,3),(4,6),(100,100)]
        :param int_list: the list of integer
        :return: a tuple, each tuple has 2 integer, representing one interval
        """
        interval_list = []
        int_list.sort()

        begin = None
        end = None
        for integer in int_list:
            if begin is None:
                begin = integer
                end = integer
            else:
                if integer == end + 1:
                    end = integer
                else:
                    interval_list.append((begin, end))
                    begin = integer
                    end = integer

        if begin is not None:
            interval_list.append((begin, end))

        return interval_list

    def print_description(self):
        print "cluster name: ", self.cluster_name
        print "create time: ", self.create_time
        print "state: ", self.state
        print

        print "Ambari Server: "
        ambari_server_vm = self.get_ambari_server_vm()
        if ambari_server_vm is None:
            print "None"
        else:
            print ambari_server_vm.domain_name, " ", ambari_server_vm.external_ip, " ",\
                ambari_server_vm.weave_internal_ip
        print

        print "Service Server with Ambari Agent directly installed: "
        if len(self.service_server_vm_list) == 0:
            print "None"
        for vm in self.service_server_vm_list:
            print vm.weave_domain_name, " ", vm.external_ip, " ", vm.weave_internal_ip
        print

        print "Ambari Agent in Docker Container: "
        int_list = []
        for vm in self.ambari_agent_vm_list:
            for docker in vm.docker_list:
                int_list.append(int(docker.get_index()))
        interval_list = self._get_int_interval(int_list)
        for interval in interval_list:
            interval_str = ""
            if interval[0] == interval[1]:
                interval_str = str(interval(0))
            else:
                interval_str = "[{0}-{1}]".format(interval[0], interval[1])
            print Docker.get_pattern_presentation(self.cluster_name, interval_str)
        print

    def get_agent_vm(self, vm_ip):
        """
        get the VM instance which holds Docker containers from the cluster instance
        :param vm_ip: the external IP of the target VM
        :return: the VM instance with the specified iP
        """
        for vm in self.ambari_agent_vm_list:
            if vm.external_ip == vm_ip:
                return vm

    def get_ambari_server_vm(self):
        """
        get the VM instance which hold the Ambari-server
        :return: the VM instance hold the Ambari-server, or None if no Ambari-server in this cluster
        """
        for vm in self.ambari_server_vm:
            return vm
        return None

    def get_service_server_vm(self, vm_ip):
        """
        get the VM instance which directly hold the Ambari-agent
        :param vm_ip: the external IP of the target VM
        :return:
        """
        for vm in self.service_server_vm_list:
            if vm.external_ip == vm_ip:
                return vm

    def to_json(self):
        """
        create a map to hold the information of the Cluster instance
        :return: A map, which is JSON format object.
        """
        cluster = {}
        cluster["cluster_name"] = self.cluster_name
        cluster["create_time"] = self.create_time
        cluster["state"] = self.state

        cluster["ambari_server_vm"] = []
        for vm in self.ambari_server_vm:
            cluster["ambari_server_vm"].append(vm.to_json())

        cluster["service_server_vm_list"] = []
        for vm in self.service_server_vm_list:
            cluster["service_server_vm_list"].append(vm.to_json())

        cluster["ambari_agent_vm_list"] = []
        for vm in self.ambari_agent_vm_list:
            cluster["ambari_agent_vm_list"].append(vm.to_json())

        return cluster

    @staticmethod
    def load_from_json(cluster_name):
        """
        load the cluster information from json file
        :param cluster_name: the name of the cluster
        :return: a Cluster instance or None if no such cluster
        """
        data = Data()
        json_data = data.read_cluster_json(cluster_name)
        if json_data is None:
            return None

        ambari_server_vm = []
        service_server_vm_list = []
        ambari_agent_vm_list = []

        for vm_json in json_data["ambari_server_vm"]:
            ambari_server_vm.append(VM.load_from_json(vm_json))

        for vm_json in json_data["service_server_vm_list"]:
            service_server_vm_list.append(VM.load_from_json(vm_json))

        for vm_json in json_data["ambari_agent_vm_list"]:
            ambari_agent_vm_list.append(VM.load_from_json(vm_json))

        cluster = Cluster()
        cluster.cluster_name = cluster_name
        cluster.state = json_data["state"]
        cluster.create_time = json_data["create_time"]
        cluster.ambari_server_vm = ambari_server_vm
        cluster.service_server_vm_list = service_server_vm_list
        cluster.ambari_agent_vm_list = ambari_agent_vm_list
        return cluster

    def _extract_vm_fqdn_ip(self, gce_info_file_name):
        """
        exatract domain name and IP address of VMs from the output file of GCE
        :param gce_info_file_name: output file of "GCE info" command
        :return: A list of tuple, each tuple has domain name and IP of a VM
        """
        lines = []
        with open(gce_info_file_name) as f:
            lines = f.readlines()

        vm_list = []
        # the first line in the output file is title
        for line in lines[1:]:
            tokens = line.split()
            fqdn_ip = (tokens[0], tokens[1])
            vm_list.append(fqdn_ip)
        return vm_list

    def request_vm(self, name, vm_num, gce_vm_type, gce_vm_os, gce_extra_cmd):
        """
        Request VMs from GCE
        :param name: the name prefix of all requesting VMs
        :param vm_num: the number of VM
        :param gce_vm_type: the type of VM
        :param gce_vm_os: the OS of VM
        :param gce_extra_cmd: extra command for requesting the VMs
        :return: A list of tuple, each tuple has domain name and IP of a VM
        """
        gce_key = Config.ATTRIBUTES["gce_controller_key_file"]
        gce_login = "{0}@{1}".format(Config.ATTRIBUTES["gce_controller_user"], Config.ATTRIBUTES["gce_controller_ip"])
        gce_up_cmd = "gce up {0} {1} {2} {3} {4}".format(name, vm_num, gce_vm_type, gce_vm_os, gce_extra_cmd)
        subprocess.call(["ssh", "-o", "StrictHostKeyChecking=no", "-i", gce_key, gce_login, gce_up_cmd])

        Log.write("cluster launched, wait for cluster info ... ...")

        fqdn_ip_pairs = []
        # wait for long enough. the more VM, more time it takes.
        for retry in range(max(6, vm_num)):
            time.sleep(10)

            # request cluster info
            with open(Config.ATTRIBUTES["gce_info_output"], "w") as gce_info_output_file:
                gce_info_cmd = "gce info {0}".format(name)
                subprocess.call(["ssh", "-o", "StrictHostKeyChecking=no", "-i", gce_key, gce_login, gce_info_cmd],
                                stdout=gce_info_output_file)

            fqdn_ip_pairs = self._extract_vm_fqdn_ip(Config.ATTRIBUTES["gce_info_output"])

            if len(fqdn_ip_pairs) == vm_num:
                Log.write("Get info for all ", str(len(fqdn_ip_pairs)), " VMs successfully")
                break
            Log.write("Only get info for ", str(len(fqdn_ip_pairs)), " VMs, retry ... ...")
        return fqdn_ip_pairs

    def request_ambari_server_vm(self, name):
        """
        request a VM for holding Ambari-server
        :param name: the name prefix of all requesting VMs
        :return: A list of tuple, each tuple has domain name and IP of a VM
        """
        # only 1 ambari server
        vm_num = 1
        gce_vm_type = Config.ATTRIBUTES["ambari_server_vm_type"]
        gce_vm_os = Config.ATTRIBUTES["ambari_server_vm_os"]

        gce_extra_cmd = ""
        if "ambari_server_vm_extra" in Config.ATTRIBUTES:
            gce_extra_cmd = Config.ATTRIBUTES["ambari_server_vm_extra"]

        fqdn_ip_pairs = self.request_vm(name, vm_num, gce_vm_type, gce_vm_os, gce_extra_cmd)
        return fqdn_ip_pairs

    def reqeust_service_server_vm(self, vm_num, name):
        """
        Request VMs to directly hold Ambari-agent (not inside Docker)
        :param vm_num: the number of VM to request
        :param name: the name prefix of all requesting VMs
        :return: A list of tuple, each tuple has domain name and IP of a VM
        """
        gce_vm_type = Config.ATTRIBUTES["service_server_vm_type"]
        gce_vm_os = Config.ATTRIBUTES["service_server_vm_os"]

        gce_extra_cmd = ""
        if "service_server_vm_extra" in Config.ATTRIBUTES:
            gce_extra_cmd = Config.ATTRIBUTES["service_server_vm_extra"]

        fqdn_ip_pairs = self.request_vm(name, vm_num, gce_vm_type, gce_vm_os, gce_extra_cmd)
        return fqdn_ip_pairs

    def request_agent_vm(self, vm_num, name):
        """
        Request VMs to hold Docker containers, each with Ambari-agent inside
        :param vm_num: the number of VM to request
        :param name: the name prefix of all requesting VMs
        :return: A list of tuple, each tuple has domain name and IP of a VM
        """
        gce_vm_type = Config.ATTRIBUTES["ambari_agent_vm_type"]
        gce_vm_os = Config.ATTRIBUTES["ambari_agent_vm_os"]
        gce_extra_disk = ""
        if "ambari_agent_vm_extra_disk" in Config.ATTRIBUTES:
            gce_extra_disk = Config.ATTRIBUTES["ambari_agent_vm_extra_disk"]

        fqdn_ip_pairs = self.request_vm(name, vm_num, gce_vm_type, gce_vm_os, gce_extra_disk)
        return fqdn_ip_pairs

    def request_gce_cluster(self, ambari_agent_vm_num, docker_num,
                            service_server_num, with_ambari_server, cluster_name):
        """
        Request a cluster from GCE
        :param ambari_agent_vm_num: number of VMs to hold Docker containers
        :param docker_num: number of Docker containers inside each VM
        :param service_server_num: number of VMs which has Ambari-agent directly installed (not in Docker)
        :param with_ambari_server: True or False, whether to request a VM to hold Ambari-server
        :param cluster_name: the name of the cluster
        :return: None
        """
        ambari_server_fqdn_ip_pairs = []
        if with_ambari_server is True:
            ambari_server_fqdn_ip_pairs = self.request_ambari_server_vm(VM.get_ambari_server_vm_name(cluster_name))
        service_server_fqdn_ip_pairs = self.reqeust_service_server_vm(service_server_num,
                                                                      VM.get_service_server_vm_name(cluster_name))
        ambari_agent_fqdn_ip_pairs = self.request_agent_vm(ambari_agent_vm_num,
                                                           VM.get_ambari_agent_vm_name(cluster_name))

        # prepare all attributes of the cluster, write to a file
        self.generate_cluster_info(cluster_name, ambari_server_fqdn_ip_pairs, service_server_fqdn_ip_pairs,
                                   ambari_agent_fqdn_ip_pairs, docker_num)

    def generate_cluster_info(self, cluster_name, ambari_server_fqdn_ip_pairs, service_server_fqdn_ip_pairs,
                              ambari_agent_fqdn_ip_pairs, docker_num):
        """
        generate VM and docker info for this cluster
        set up parameter of the class instance as this info
        :param cluster_name: the name of the cluster
        :param ambari_server_fqdn_ip_pairs: the domain name and IP pairs for Ambari-server
        :param service_server_fqdn_ip_pairs: the domain name and IP pairs for VMs with Ambari-agent installed
        :param ambari_agent_fqdn_ip_pairs: the domain name and IP pairs for VM with Docker containers
        :param docker_num: the number of Dockers inside each VMs
        :return: None
        """
        weave_ip_base = Config.ATTRIBUTES["weave_ip_base"]
        weave_ip_mask = Config.ATTRIBUTES["weave_ip_mask"]
        current_ip = weave_ip_base

        for vm_domain_name, vm_ip in ambari_server_fqdn_ip_pairs:
            current_ip = self._increase_ip(current_ip, 1)
            weave_dns_ip = current_ip
            vm = VM(vm_ip, vm_domain_name, weave_dns_ip, weave_ip_mask)
            current_ip = self._increase_ip(current_ip, 1)
            vm.weave_internal_ip = current_ip
            self.ambari_server_vm.append(vm)

        for vm_domain_name, vm_ip in service_server_fqdn_ip_pairs:
            current_ip = self._increase_ip(current_ip, 1)
            weave_dns_ip = current_ip
            vm = VM(vm_ip, vm_domain_name, weave_dns_ip, weave_ip_mask)
            current_ip = self._increase_ip(current_ip, 1)
            vm.weave_internal_ip = current_ip
            self.service_server_vm_list.append(vm)

        vm_index = 0
        for vm_domain_name, vm_ip in ambari_agent_fqdn_ip_pairs:
            current_ip = self._increase_ip(current_ip, 1)
            weave_dns_ip = current_ip
            vm = VM(vm_ip, vm_domain_name, weave_dns_ip, weave_ip_mask)

            for docker_index in range(0, docker_num):
                current_ip = self._increase_ip(current_ip, 1)
                docker_ip_str = current_ip

                total_docker_index = vm_index * docker_num + docker_index
                docker_domain_name = Docker.get_weave_domain_name(cluster_name, total_docker_index)

                docker = Docker(docker_ip_str, str(weave_ip_mask), docker_domain_name)
                vm.add_docker(docker)

            vm_index += 1
            self.ambari_agent_vm_list.append(vm)

        self.cluster_name = cluster_name
        self.create_time = str(datetime.datetime.now())
        self.state = Cluster.STATE_FREE

        # update config file.
        # This step makes the user avoid reconfiguring the IP for next cluster creation
        Config.update("weave", "weave_ip_base", current_ip)

    def _increase_ip(self, base_ip_str, increase):
        """
        increase the IP address.
        example: 192.168.1.1, increased by 1: 192.168.1.2
        example: 192.168.1.254, increased by 2: 192.168.2.1
        :param base_ip_str: the IP to be increased
        :param increase: the amount of increase
        :return: the new IP address, in String
        """
        base_ip = base_ip_str.split(".")
        new_ip = [int(base_ip[0]), int(base_ip[1]), int(base_ip[2]), int(base_ip[3])]
        new_ip[3] = new_ip[3] + increase
        for index in reversed(range(0, 4)):
            if new_ip[index] > 255:
                new_ip[index - 1] += (new_ip[index] / 256)
                new_ip[index] %= 256
        return "{0}.{1}.{2}.{3}".format(new_ip[0], new_ip[1], new_ip[2], new_ip[3])

    def _scp_upload(self, vm_external_ip):
        """
        upload all the code in a VM
        :param vm_external_ip: the external IP of the VM
        :return: None
        """
        # upload necessary file to VM
        vm_directory = "{0}@{1}:{2}".format(Config.ATTRIBUTES["vm_user"], vm_external_ip,
                                            Config.ATTRIBUTES["vm_code_directory"])
        vm_key = Config.ATTRIBUTES["vm_key_file"]

        upload_return_code = 0
        with open(os.devnull, 'w') as shutup:
            upload_return_code = subprocess.call(["scp", "-o", "StrictHostKeyChecking=no", "-i",
                                                  vm_key, "-r", ".", vm_directory],
                                                 stdout=shutup, stderr=shutup)
        if upload_return_code == 0:
            Log.write("VM ", vm_external_ip, " file upload succeed")
        else:
            Log.write("VM ", vm_external_ip, " file upload fail")

    def _set_executable_permission(self, vm_external_ip):
        """
        Set all shell file to be executable
        :param vm_external_ip: the external IP of the VM
        :return: None
        """
        vm_ssh_login = "{0}@{1}".format(Config.ATTRIBUTES["vm_user"], vm_external_ip)
        vm_ssh_cd_cmd = "cd {0}".format(Config.ATTRIBUTES["vm_code_directory"])
        vm_ssh_chmod_cmd = "chmod a+x **/*.sh"
        vm_ssh_cmd = "{0};{1}".format(vm_ssh_cd_cmd, vm_ssh_chmod_cmd)
        vm_key = Config.ATTRIBUTES["vm_key_file"]

        with open(os.devnull, 'w') as shutup:
            subprocess.Popen(["ssh", "-o", "StrictHostKeyChecking=no", "-t", "-i", vm_key,
                              vm_ssh_login, vm_ssh_cmd],
                             stdout=shutup, stderr=shutup)

    def run_cluster(self, server_weave_ip, server_external_ip):
        """
        Run all Ambari-agents and Ambari-server in the cluster in parallel
        Wait until all processes finish
        :param server_weave_ip: the Weave IP of Ambari-server
        :param server_external_ip: the external IP of Ambari-server
        :return: None
        """
        process_list = {}
        process_list.update(self.run_ambari_server_asyn())
        process_list.update(self.run_service_server_asyn(server_weave_ip, server_external_ip))
        process_list.update(self.run_docker_on_cluster_asyn(server_weave_ip, server_external_ip))

        terminate_state_list = {}
        for hostname in process_list:
            terminate_state_list[hostname] = False

        Log.write("Wait for all VMs to finish configuration ... ...")

        # Wait for all configuration subprocesses
        while True:
            all_finished = True
            for hostname in process_list:
                output_file, output_file_path, process = process_list[hostname]
                if terminate_state_list[hostname] is False:
                    all_finished = False
                    returncode = process.poll()
                    if returncode is None:
                        continue
                    else:
                        Log.write("VM ", hostname, " configuration completed, return code: ", str(returncode),
                                  ", output file path: ", output_file_path)
                        terminate_state_list[hostname] = True
                        output_file.close()
                else:
                    pass
            if all_finished:
                break
            time.sleep(5)

        Log.write("All VM configuration completed.")

    def run_ambari_server_asyn(self):
        """
        Run configuration for Ambari-server in this cluster
        Set up Ambari-server and Weave network
        The method is NON-BLOCK
        :return: a map of tuple, the key of the map is the host name of the VM,
                the tuple has 3 elements: the file handler of the output of the VM,
                the file path of the output of the VM,
                and the process object of configuration for the VM
        """
        process_list = {}

        for vm in self.ambari_server_vm:
            vm_external_ip = vm.external_ip
            self._scp_upload(vm_external_ip)
            self._set_executable_permission(vm_external_ip)

            vm_output_file_path = vm.get_ssh_output_file_path()
            vm_output_file = open(vm_output_file_path, "w")

            # ssh install server
            vm_ssh_login = "{0}@{1}".format(Config.ATTRIBUTES["vm_user"], vm_external_ip)
            vm_ssh_cd_cmd = "cd {0}".format(Config.ATTRIBUTES["vm_code_directory"])
            vm_ssh_python_cmd = "python launcher_ambari_server.py {0}".format(self.cluster_name)
            vm_ssh_cmd = "{0};{1}".format(vm_ssh_cd_cmd, vm_ssh_python_cmd)
            vm_key = Config.ATTRIBUTES["vm_key_file"]
            Log.write(vm_ssh_python_cmd)

            process = subprocess.Popen(["ssh", "-o", "StrictHostKeyChecking=no", "-t", "-i", vm_key,
                                       vm_ssh_login, vm_ssh_cmd],
                                       stdout=vm_output_file, stderr=vm_output_file)
            process_list[vm.hostname] = (vm_output_file, vm_output_file_path, process)
            Log.write("Configuring VM ", vm.hostname, " ... ...")
        return process_list

    def run_service_server_asyn(self, server_weave_ip, server_external_ip):
        """
        Run configuration, set up Ambari-agent in this VM, and the Weave network
        :param server_weave_ip: the Weave IP of the Ambari-server
        :param server_external_ip: the external IP of the Ambari-server
        The method is NON-BLOCK
        :return: a map of tuple, the key of the map is the host name of the VM,
                the tuple has 3 elements: the file handler of the output of the VM,
                the file path of the output of the VM,
                and the process object of configuration for the VM
        """
        process_list = {}

        for vm in self.service_server_vm_list:
            vm_external_ip = vm.external_ip
            self._scp_upload(vm_external_ip)
            self._set_executable_permission(vm_external_ip)

            vm_output_file_path = vm.get_ssh_output_file_path()
            vm_output_file = open(vm_output_file_path, "w")

            # ssh install server
            vm_ssh_login = "{0}@{1}".format(Config.ATTRIBUTES["vm_user"], vm_external_ip)
            vm_ssh_cd_cmd = "cd {0}".format(Config.ATTRIBUTES["vm_code_directory"])
            vm_ssh_python_cmd = "python launcher_service_server.py {0} {1} {2} {3}".format(
                vm_external_ip, server_weave_ip, server_external_ip, self.cluster_name)
            vm_ssh_cmd = "{0};{1}".format(vm_ssh_cd_cmd, vm_ssh_python_cmd)
            vm_key = Config.ATTRIBUTES["vm_key_file"]
            Log.write(vm_ssh_python_cmd)

            process = subprocess.Popen(["ssh", "-o", "StrictHostKeyChecking=no", "-t", "-i", vm_key,
                                        vm_ssh_login, vm_ssh_cmd],
                                       stdout=vm_output_file, stderr=vm_output_file)

            process_list[vm.hostname] = (vm_output_file, vm_output_file_path, process)
            Log.write("Configuring VM ", vm.hostname, " ... ...")
        return process_list

    def run_docker_on_cluster_asyn(self, server_weave_ip, server_external_ip):
        """
        Run configuration, set up Docker and Weave network
        run all containers, each with Ambari-agent inside
        :param server_weave_ip: the Weave IP of the Ambari-server
        :param server_external_ip: the external IP of the Ambari-server
        The method is NON-BLOCK
        :return: a map of tuple, the key of the map is the host name of the VM,
                the tuple has 3 elements: the file handler of the output of the VM,
                the file path of the output of the VM,
                and the process object of configuration for the VM
        """
        process_list = {}

        for vm in self.ambari_agent_vm_list:
            vm_external_ip = vm.external_ip
            self._scp_upload(vm_external_ip)
            self._set_executable_permission(vm_external_ip)

            vm_output_file_path = vm.get_ssh_output_file_path()
            vm_output_file = open(vm_output_file_path, "w")

            vm_ssh_login = "{0}@{1}".format(Config.ATTRIBUTES["vm_user"], vm_external_ip)
            vm_ssh_cd_cmd = "cd {0}".format(Config.ATTRIBUTES["vm_code_directory"])
            vm_ssh_python_cmd = "python launcher_docker.py {0} {1} {2} {3}".format(
                vm_external_ip, server_weave_ip, server_external_ip, self.cluster_name)
            vm_ssh_cmd = "{0};{1}".format(vm_ssh_cd_cmd, vm_ssh_python_cmd)
            vm_key = Config.ATTRIBUTES["vm_key_file"]
            Log.write(vm_ssh_python_cmd)

            process = subprocess.Popen(["ssh", "-o", "StrictHostKeyChecking=no", "-t", "-i", vm_key,
                                        vm_ssh_login, vm_ssh_cmd],
                                       stdout=vm_output_file, stderr=vm_output_file)

            process_list[vm.hostname] = (vm_output_file, vm_output_file_path, process)
            Log.write("Configuring VM ", vm.hostname, " ... ...")

        return process_list
