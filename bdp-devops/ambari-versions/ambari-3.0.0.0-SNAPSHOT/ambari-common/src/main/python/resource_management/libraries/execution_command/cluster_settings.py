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

__all__ = ["ClusterSettings"]

class ClusterSettings(object):
    """
    This class maps to "configurations->cluster-env" in command.json which includes cluster setting information of a cluster
    """

    def __init__(self, clusterSettings):
        self.__cluster_settings = clusterSettings

    def __get_value(self, key):
        """
        Get corresponding value from the key
        :param key:
        :return: value if key exist else None
        """
        return self.__cluster_settings.get(key)

    def is_cluster_security_enabled(self):
        """
        Check cluster security enabled or not
        :return: True or False
        """
        security_enabled = self.__get_value("security_enabled")
        return security_enabled and security_enabled.lower() == "true"

    def get_recovery_max_count(self):
        """
        Retrieve cluster recovery count
        :return: String, need to convert to int
        """
        return int(self.__get_value("recovery_max_count"))

    def check_recovery_enabled(self):
        """
        Check if the cluster can be enabled or not
        :return: True or False
        """
        recovery_enabled =  self.__get_value("recovery_enabled")
        return recovery_enabled and recovery_enabled.lower() == "true"

    def get_recovery_type(self):
        """
        Retrieve cluster recovery type
        :return: recovery type, i.e "AUTO_START"
        """
        return self.__get_value("recovery_type")

    def get_kerberos_domain(self):
        """
        Retrieve kerberos domain
        :return: String as kerberos domain
        """
        return self.__get_value("kerberos_domain")

    def get_smokeuser(self):
        """
        Retrieve smokeuser
        :return: smkeuser string
        """
        return self.__get_value("smokeuser")

    def get_user_group(self):
        """
        Retrieve cluster usergroup
        :return: usergroup string
        """
        return self.__get_value("user_group")

    def get_repo_suse_rhel_template(self):
        """
        Retrieve template of suse and rhel repo
        :return: template string
        """
        return self.__get_value("repo_suse_rhel_template")

    def get_repo_ubuntu_template(self):
        """
        Retrieve template of ubuntu repo
        :return: template string
        """
        return self.__get_value("repo_ubuntu_template")

    def check_override_uid(self):
        """
        Check if override_uid is true or false
        :return: True or False
        """
        override_uid =  self.__get_value("override_uid")
        return override_uid and override_uid.lower() == "true"

    def check_sysprep_skip_copy_fast_jar_hdfs(self):
        """
        Check sysprep_skip_copy_fast_jar_hdfs is true or false
        :return: True or False
        """
        skip = self.__get_value("sysprep_skip_copy_fast_jar_hdfs")
        return skip and skip.lower() == "true"

    def check_sysprep_skip_lzo_package_operations(self):
        """
        Check sysprep_skip_lzo_package_operations is true or false
        :return: True or False
        """
        skip = self.__get_value("sysprep_skip_lzo_package_operations")
        return skip and skip.lower() == "true"

    def check_sysprep_skip_setup_jce(self):
        """
        Check sysprep_skip_setup_jce is true or false
        :return: True or False
        """
        skip = self.__get_value("sysprep_skip_setup_jce")
        return skip and skip.lower() == "true"

    def check_sysprep_skip_create_users_and_groups(self):
        """
        Check sysprep_skip_copy_create_users_and_groups is true or false
        :return: True or False
        """
        skip = self.__get_value("sysprep_skip_create_users_and_groups")
        return skip and skip.lower() == "true"

    def check_ignore_groupsusers_create(self):
        """
        Check ignore_groupsuers_create is true or false
        :return: True or False
        """
        ignored = self.__get_value("ignore_groupsusers_create")
        return ignored and ignored.lower() == "true"

    def check_fetch_nonlocal_groups(self):
        """
        Check fetch_nonlocal_group is true or false
        :return: True or False
        """
        fetch_nonlocal_group = self.__get_value("fetch_nonlocal_groups")
        return fetch_nonlocal_group and fetch_nonlocal_group.lower() == "true"