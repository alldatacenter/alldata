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

from apache_ranger.model.ranger_policy import *
from ranger_performance_tool.ranger_perf_object_stores.service_object_stores import *
from ranger_performance_tool.ranger_perf_object_stores.random_generators import *


class RangerAPIObjectStore:
    """
    Primary class to connect object stores to the Ranger API

    Attributes
    ----------
    service_store : dict
        Mapping of service type to service store objects. Add new service names here if required or unsupported.
    policy_store: PolicyStore object to create objects associated with policy api's.

    Methods
    -------
    get_service_type_for_service_name(service_name)
        Returns the service type for the given service name
    get_service_store(service_type)
        Returns the service store for the given service type
    get_api(ranger_client, api_name)
        Returns the API object for the given API name
    get_api_param_dict(api_name)
        Returns the parameter dictionary for the given API name
    """
    service_store_def = {
        "hdfs": HDFSServiceStore,
        "hbase": HBaseServiceStore,
        "hive": HiveServiceStore,
        "yarn": YarnServiceStore,
        "knox": KnoxServiceStore,
        "solr": SolrServiceStore,
        "kafka": KafkaServiceStore,
        "atlas": AtlasServiceStore,
        "ozone": OzoneServiceStore,
        "adls": AdlsServiceStore,
        "s3": S3ServiceStore,
        "kudu": KuduServiceStore,
    }

    def __init__(self):
        """
        Constructor
        Modify if new object stores are added
        """
        self.policy_store = PolicyStore()
        self.service_store = {}

        from ranger_performance_tool import perf_globals
        enabled_services = perf_globals.CONFIG_READER.get_config_value("secondary", "enabled_services")
        service_type_mapping = perf_globals.CONFIG_READER.get_config_value("secondary", "service_type_mapping")
        for service_name in enabled_services:
            if service_name not in service_type_mapping:
                raise Exception(f"Unknown service name:{service_name}. "
                                f"Add it to service_type_mapping in secondary config file")
            service_type = service_type_mapping[service_name]
            random_type = perf_globals.CONFIG_READER.get_config_value("secondary", "services",
                                                                      service_type, "random_type")
            self.service_store[service_type] = RangerAPIObjectStore.service_store_def[service_type](random_type=random_type)

    def get_service_type_for_service_name(self, service_name):
        """
        Returns the service type for the given service name
        :parameter service_name: Service name for the service type to be returned
        :return: Service type for the given service name. See service_type_mapping dictionary for supported service names.
        """
        from ranger_performance_tool import perf_globals
        service_type_mapping = perf_globals.CONFIG_READER.get_config_value("secondary", "service_type_mapping")
        if service_name not in service_type_mapping.keys():
            raise Exception(f"Unknown service name:{service_name}."
                            f"Add it to service_type_mapping in secondary config file")
        return service_type_mapping[service_name]

    def get_service_store(self, service_type):
        """
        Returns the service store for the given service type
        :parameter service_type: Service type for the service store to be returned
        :return: Service store object for the given service type. See service_store dictionary for supported service types.
        """
        if service_type not in self.service_store:
            raise Exception("Service name not found in service store")
        return self.service_store[service_type]

    def get_api(self, ranger_client, api_name):
        """
        Returns the API object for the given API name
        :parameter ranger_client: Ranger client object
        :parameter api_name: Name of the method corresponding to intg python ranger client
        :return: API callable object
        """
        if api_name == "create_policy":
            return ranger_client.create_policy
        elif api_name == "delete_policy_by_id":
            return ranger_client.delete_policy_by_id
        elif api_name == "get_policy_by_id":
            return ranger_client.get_policy_by_id
        elif api_name == "update_policy_by_id":
            return ranger_client.update_policy_by_id
        else:
            raise Exception(f"Unknown API name: {api_name}")

    def get_api_param_dict(self, api_name):
        """
        Returns the parameter dictionary for the given API name
        :parameter api_name: Name of the method corresponding to intg python ranger client
        :return: Parameter dictionary to be used in conjunction with the API callable object from get_api()
        """
        if api_name == "create_policy":
            param_dict = {
                "policy": self.policy_store.generate_object()
            }
        elif api_name == "delete_policy_by_id":
            param_dict = {
                "policyId": self.policy_store.get_id()
            }
        elif api_name == "get_policy_by_id":
            param_dict = {
                "policyId": self.policy_store.get_id()
            }
        elif api_name == "update_policy_by_id":
            policyId = self.policy_store.get_id()
            policy = self.policy_store.get_policy_by_id(policyId)
            param_dict = {
                "policyId": policyId,
                "policy": self.policy_store.generate_object(id=policyId, service=policy.service,
                                                            serviceType=policy.serviceType)
            }
        else:
            raise Exception(f"Unknown API name: {api_name}")

        return param_dict


class RemoteStore:
    """
    Calls RangerClient api's to support other object stores. For example see usage in PolicyStore.
    """
    def __init__(self):
        from ranger_performance_tool import perf_globals
        self.ranger_client = perf_globals.RANGER_CLIENT
        self.policy_list = None

    def get_policy_list(self):
        if self.policy_list is None:
            from ranger_performance_tool import perf_globals
            self.policy_list = {service: self.ranger_client.get_policies_in_service(service)
                                 for service in perf_globals.CONFIG_READER.get_config_value("secondary",
                                                                                            "enabled_services")}
        return self.policy_list


class PolicyStore:
    """
    Support object creation for API's related to policies

    Attributes:
        random_generator: Type of random generator based on random_type defines in secondary config file
        remote_store: Remote store object that fetches required policy objects from RangerClient

    Methods:
        generate_object: Generates a policy object with random values base on random_generator type
        get_id: Returns the id of the policy object by looking up the policy object in the remote store
        get_policy_by_id: Returns the policy object by looking up the policy object in the remote store
    """
    def __init__(self):
        from ranger_performance_tool import perf_globals
        self.random_generator = random_generators.get_random_generator(
            perf_globals.CONFIG_READER.get_config_value("secondary", "policy_store", "random_type"))
        self.remote_store = RemoteStore()

    def get_policy_by_id(self, id):
        """
        Returns the policy object by looking up the policy object in the remote store
        :param id: int , id of the policy object
        :return: policy: dict , policy object
        """
        for service, policy_list in self.remote_store.get_policy_list().items():
            for policy in policy_list:
                if policy.id == id:
                    return policy

    def get_id(self):
        """
        Returns the id of the policy object by looking up the policy object in the remote store
        :return: id: int , id of the policy object
        """
        from ranger_performance_tool import perf_globals
        enabled_services = perf_globals.CONFIG_READER.get_config_value("secondary", "enabled_services")
        service = random.choice(enabled_services)
        policy_list = self.remote_store.get_policy_list()[service]
        return random.choice(policy_list).id

    def generate_object(self, **kwargs):
        """
        Generates a policy object with random values base on random_generator type
        :param kwargs: dict , optional parameters to be used in generating the policy object. Overrides the default generated values.
        :return: policy_object: dict , policy object
        """
        from ranger_performance_tool import perf_globals
        service_type_mapping = perf_globals.CONFIG_READER.get_config_value("secondary", "service_type_mapping")
        if "id" not in kwargs:
            kwargs["id"] = self.random_generator.generate_int()
        if "name" not in kwargs:
            kwargs["name"] = self.random_generator.generate_string()
        if "service" not in kwargs:
            enabled_service = random.choice(perf_globals.CONFIG_READER.get_config_value("secondary", "enabled_services"))
            service_type = service_type_mapping[enabled_service]
            kwargs["service"] = enabled_service
            kwargs["serviceType"] = service_type
        policy_object = RangerPolicy()
        for key, value in kwargs.items():
            if key not in dir(policy_object):
                raise Exception("Invalid key: " + key)
            policy_object[key] = value
        service_type = policy_object.serviceType
        service_store = perf_globals.OBJECT_STORE.service_store[service_type]
        policy_object.resources = service_store.generate_resources()
        return policy_object
