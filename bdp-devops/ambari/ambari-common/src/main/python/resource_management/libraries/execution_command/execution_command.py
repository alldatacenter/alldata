#!/usr/bin/env python
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

__all__ = ["ExecutionCommand"]

from resource_management.libraries.execution_command import module_configs
from resource_management.libraries.execution_command import stack_settings
from resource_management.libraries.execution_command import cluster_settings

class ExecutionCommand(object):
    """
    The class has two private objects: _execution_command maps to a command.json and
    _module_config maps to configurations block and configuratonAttributes within this
     command.json. This class provides a set of APIs to retrieve all command related info.
     The caller should not access the command.json to get the configuration info directly.
    """

    def __init__(self, command):
        """
        _execution_command is a wrapper object of a dict object maps to command.json
        _module_config is an wrapper object of configuration and configurationAttributes dict
        objects maps to configuration and configurationAttributes blocks within command.json
        :param command: json string or a python dict object
        """
        self.__execution_command = command
        self.__module_configs = module_configs.ModuleConfigs(self.__get_value("configurations"), self.__get_value("configurationAttributes"))
        # TODO : 'stack_settings' and 'cluster_settings' may be segregated later out of 'cluster-env'
        # as done in 'branch-feature-AMBARI-14714'.
        self.__stack_settings = stack_settings.StackSettings(self.__get_value("configurations/cluster-env"))
        self.__cluster_settings = cluster_settings.ClusterSettings(self.__get_value("configurations/cluster-env"))

    def __get_value(self, key, default_value=None):
        """
        A private method to query value with the full path of key, if key does not exist, return default_value
        :param key: query key string
        :param default_value: if key does not exist, return this value
        :return: the value maps to query key
        """
        sub_keys = filter(None, key.split('/'))
        value = self.__execution_command
        try:
            for sub_key in sub_keys:
                if not sub_key in value:
                    return default_value
                value = value[sub_key]
                if value == None:
                    value = default_value
            return value
        except:
            return default_value

    def get_value(self, query_string, default_value=None):
        """
        Generic query API to retrieve config attribute from execution_command directly
        :param query_string: full query key string
        :param default_value: if key does not exist, return default value
        :return: config attribute
        """
        return self.__get_value(query_string, default_value)

    """
    Global variables section
    """

    def get_module_configs(self):
        return self.__module_configs

    def get_stack_settings(self):
        return self.__stack_settings

    def get_cluster_settings(self):
        return self.__cluster_settings

    def get_module_name(self):
        """
        Retrieve service type from command.json, eg. 'ZOOKEEPER', 'HDFS'
        :return: service type
        """
        return self.__get_value("serviceName")

    def get_component_type(self):
        """
        Retrieve host role from command.json, i.e "role": "ZOOKEEPER_SERVER"
        :return:
        """
        return self.__get_value("role")

    def get_component_instance_name(self):
        """
        Retrieve service name from command.json, eg. 'zk1'
        :return: service name
        """
        module_name = self.get_module_name()
        if module_name and '_CLIENTS' in module_name: # FIXME temporary hack
            return 'default'
        return self.__get_value("serviceName") # multi-service, but not multi-component per service

    def get_cluster_name(self):
        """
        Retrieve cluster name from command.json
        :return: cluster name
        """
        return self.__get_value("clusterName")

    def get_repository_file(self):
        """
        Retrieve respository dict about mpack info from command.json
        :return: repository file name
        """
        return self.__get_value("repositoryFile")

    def get_local_components(self):
        """
        Retrieve a list of service components from command.json. i.e localComponents": ["ZOOKEEPER_CLIENT"]
        :return: list of components
        """
        return self.__get_value("localComponents", [])

    def get_role_command(self):
        """
        Retrieve execution command
        :return: String, i.e "ACTIONEXECUTE", "INSTALL", "START" etc
        """
        return self.__get_value("roleCommand")

    """
    Ambari variables section
    """

    def get_jdk_location(self):
        """
        Retrieve URL of jdk from command.json. i.e "jdk_location": "http://c7302.ambari.apache.org:8080/resources/"
        :return: jdk url string
        """
        return self.__get_value("ambariLevelParams/jdk_location")

    def get_jdk_name(self):
        """
        Retrieve jdk name from command.json. i.e "jdk_name": "jdk-8u112-linux-x64.tar.gz"
        :return: jdk name string
        """
        return self.__get_value("ambariLevelParams/jdk_name")

    def get_java_home(self):
        """
        Retrieve java home from command.json. i.e "java_home": "/usr/jdk64/jdk1.8.0_112"
        :return: java home string
        """
        return self.__get_value("ambariLevelParams/java_home")

    def get_java_version(self):
        """
        Retrieve java version from command.json, i.e "java_version": "8", note "8" will convert to 8
        :return: an integer represents java version
        """
        from resource_management.libraries.functions.expect import expect_v2
        return expect_v2("ambariLevelParams/java_version", int)

    def get_jce_name(self):
        """
        Retrieve jce name from command.json, i.e "jce_name": "jce_policy-8.zip"
        :return: jce name string
        """
        return self.__get_value("ambariLevelParams/jce_name")

    def get_db_driver_file_name(self):
        """
        Retrieve database driver file name, i.e "db_driver_filename": "mysql-connector-java.jar"
        :return: DB driver name string
        """
        return self.__get_value('ambariLevelParams/db_driver_filename')

    def get_db_name(self):
        """
        Retrieve database name, i.e Ambari server's db name is "db_name": "ambari"
        :return: DB name
        """
        return self.__get_value('ambariLevelParams/db_name')

    def get_oracle_jdbc_url(self):
        """
        Retrieve oracle database jdbc driver url,
        i.e "oracle_jdbc_url": "http://c7302.ambari.apache.org:8080/resources//ojdbc6.jar"
        :return: oracle jdbc url
        """
        return self.__get_value('ambariLevelParams/oracle_jdbc_url')

    def get_mysql_jdbc_url(self):
        """
        Retrieve mysql database jdbc driver url,
        i.e "mysql_jdbc_url": "http://c7302.ambari.apache.org:8080/resources//mysql-connector-java.jar"
        :return: mysql jdbc url
        """
        return self.__get_value('ambariLevelParams/mysql_jdbc_url')

    def get_agent_stack_retry_count(self):
        """
        Retrieve retry count for stack deployment on agent,
        i.e "agent_stack_retry_count": "5"
        :return: retry count for stack deployment on agent
        """
        from resource_management.libraries.functions.expect import expect_v2
        return expect_v2('ambariLevelParams/agent_stack_retry_count', int, 5)

    def check_agent_stack_want_retry_on_unavailability(self):
        """
        Check if retry is needed when agent is not reachable
        :return: True or False
        """
        return self.__get_value('ambariLevelParams/agent_stack_retry_on_unavailability')

    def get_ambari_server_host(self):
        """
        Retrieve ambari server host url from command.json, i.e "ambari_server_host": "c7302.ambari.apache.org"
        :return: ambari server url
        """
        return self.__get_value("ambariLevelParams/ambari_server_host")

    def get_ambari_server_port(self):
        """
        Retrieve ambar server port number from command.json, i.e "ambari_server_port": "8080"
        :return: amabari server port number
        """
        return self.__get_value("ambariLevelParams/ambari_server_port")

    def is_ambari_server_use_ssl(self):
        """
        Check if ssl is needed to connect to ambari server
        :return: True or False
        """
        return self.__get_value("ambariLevelParams/ambari_server_use_ssl", False)

    def is_host_system_prepared(self):
        """
        Check a global flag enabling or disabling the sysprep feature
        :return: True or False
        """
        return self.__get_value("ambariLevelParams/host_sys_prepped", False)

    def is_gpl_license_accepted(self):
        """
        Check ambari server accepts gpl license or not
        :return: True or False
        """
        return self.__get_value("ambariLevelParams/gpl_license_accepted", False)


    """
    Cluster related variables section
    TODO: deprecated, but some scripts still use them, need to remove them gradually
    """

    def get_mpack_name(self):
        """
        Retrieve mpack name from command.json, i.e "stack_name": "HDPCORE"
        :return: mpack name string
        """
        return self.__get_value("clusterLevelParams/stack_name")

    def get_mpack_version(self):
        """
        Retrieve mpack version from command.json, i.e "stack_version": "1.0.0-b224"
        :return: mpack version string
        """
        return self.__get_value("clusterLevelParams/stack_version")

    def get_user_groups(self):
        """
        Retrieve ambari server user groups, i.e "user_groups": "{\"zookeeper\":[\"hadoop\"],\"ambari-qa\":[\"hadoop\"]}"
        :return: a user group dict object
        """
        return self.__get_value("clusterLevelParams/user_groups")

    def get_group_list(self):
        """
        Retrieve a list of user groups from command.json, i.e "group_list": "[\"hadoop\"]"
        :return: a list of groups
        """
        group_list = self.__get_value("clusterLevelParams/group_list")
        if not group_list:
            group_list = "[]"
        return group_list

    def get_user_list(self):
        """
        Retrieve a list of users from command.json, i.e "user_list": "[\"zookeeper\",\"ambari-qa\"]"
        :return: a list of users
        """
        user_list = self.__get_value("clusterLevelParams/user_list")
        if not user_list:
            user_list = "[]"
        return user_list


    """
    Agent related variable section
    """

    def get_host_name(self):
        """
        Retrieve host name on which ambari agent is running
        :return: host name
        """
        return self.__get_value("agentLevelParams/hostname")

    def get_agent_cache_dir(self):
        """
        The root directory in which ambari agent stores cache or log etc,
        i.e "agentCacheDir": "/var/lib/ambari-agent/cache"
        :return: the cache directory path
        """
        return self.__get_value('agentLevelParams/agentCacheDir')

    def check_agent_config_execute_in_parallel(self):
        """
        Check if config commands can be executed in parallel in ambari agent
        :return: True or False
        """
        return int(self.__get_value("agentLevelParams/agentConfigParams/agent/parallel_execution", 0))

    def check_agent_proxy_settings(self):
        """
        Check if system proxy is set or not on agent
        :return: True by default
        """
        return self.__get_value("agentLevelParams/agentConfigParams/agent/use_system_proxy_settings", True)


    """
    Host related variables section
    """
    #### TODO : Doesn't exist as of now. Evaluate if we would be having these in hostLevelParams.
    '''
    def get_repo_info(self):
        return self.__get_value('hostLevelParams/repoInfo')

    def get_service_repo_info(self):
        return self.__get_value('hostLevelParams/service_repo_info')
    '''

    """
    Component related variables section
    """

    def check_unlimited_key_jce_required(self):
        """
        Check unlimited jce key is required or not
        :return: True or False
        """
        return self.__get_value('componentLevelParams/unlimited_key_jce_required', False)

    """
    Command related variables section
    """

    def get_new_mpack_version_for_upgrade(self):
        """
        New Cluster Stack Version that is defined during the RESTART of a Rolling Upgrade
        :return:
        """
        return self.__get_value("commandParams/version")

    def check_command_retry_enabled(self):
        """
        Check need to retry command or not
        :return: True or False
        """
        return self.__get_value('commandParams/command_retry_enabled', False)

    # TODO : Doesnt exist as of now.  Evaluate if we would be having these
    '''
    def check_upgrade_direction(self):
        return self.__get_value('commandParams/upgrade_direction')
    def get_upgrade_type(self):
        return self.__get_value('commandParams/upgrade_type', '')
    def is_rolling_restart_in_upgrade(self):
        return self.__get_value('commandParams/rolling_restart', False)
    def is_update_files_only(self):
        return self.__get_value('commandParams/update_files_only', False)
    def get_deploy_phase(self):
        return self.__get_value('commandParams/phase')
    '''

    def get_dfs_type(self):
        return self.__get_value('clusterLevelParams/dfs_type')

    def get_module_package_folder(self):
        return self.__get_value('commandParams/service_package_folder')

    # TODO : Doesnt exist as of now. Evaluate if we need them.
    '''
    def get_ambari_java_home(self):
        return self.__get_value('commandParams/ambari_java_home')
    def get_ambari_java_name(self):
        return self.__get_value('commandParams/ambari_java_name')
    def get_ambari_jce_name(self):
        return self.__get_value('commandParams/ambari_jce_name')
    def get_ambari_jdk_name(self):
        return self.__get_value('commandParams/ambari_jdk_name')
    def need_refresh_topology(self):
        return self.__get_value('commandParams/refresh_topology', False)
    def check_only_update_files(self):
        return self.__get_value('commandParams/update_files_only', False)
    def get_desired_namenode_role(self):
        """
        The desired role is only available during a Non-Rolling Upgrade in HA.
        The server calculates which of the two NameNodes will be the active,
        and the other the standby since they are started using different commands.
        :return: the node's role (active or standby)
        """
        return self.__get_value('commandParams/desired_namenode_role')
    def get_node(self, type):
        key = "commandParams/" + type + "node"
        return self.__get_value(key)
    
    """
    Role related variables section
    """
    def is_upgrade_suspended(self):
        return self.__get_value('roleParams/upgrade_suspended', False)
    '''


    """
    Cluster Host Info
    """

    def get_component_hosts(self, component_name):
        key = "clusterHostInfo/" + component_name + "_hosts"
        if component_name == "oozie_server":
            key = "clusterHostInfo/" + component_name
        return self.__get_value(key, [])

    def get_component_host(self, component_name):
        key = "clusterHostInfo/" + component_name + "_host"
        return self.__get_value(key, [])

    def get_all_hosts(self):
        return self.__get_value('clusterHostInfo/all_hosts', [])

    def get_all_racks(self):
        return self.__get_value('clusterHostInfo/all_racks', [])

    def get_all_ipv4_ips(self):
        return self.__get_value('clusterHostInfo/all_ipv4_ips', [])