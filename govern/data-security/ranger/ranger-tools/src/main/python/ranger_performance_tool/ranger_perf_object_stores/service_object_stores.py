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
from abc import ABC, abstractmethod

from ranger_performance_tool.ranger_perf_object_stores import random_generators


class ServiceStoreBase(ABC):

    @abstractmethod
    def generate_resources(self):
        pass


class HDFSServiceStore(ServiceStoreBase):

    def __init__(self, random_type="random"):
        self.random_generator = random_generators.get_random_generator(random_type)

    def generate_resources(self):
        paths = self.random_generator.generate_string_array()
        resources = {'path': {'values': paths, 'isExcludes': False, 'isRecursive': True}}
        return resources


class HBaseServiceStore(ServiceStoreBase):

    def __init__(self, random_type="random"):
        self.random_generator = random_generators.get_random_generator(random_type)

    def generate_resources(self):
        columns = self.random_generator.generate_string_array()
        tables = self.random_generator.generate_string_array()
        column_families = self.random_generator.generate_string_array()
        resources = {'column-family': {'values': column_families, 'isExcludes': False, 'isRecursive': False},
                     'column': {'values': columns, 'isExcludes': False, 'isRecursive': False},
                     'table': {'values': tables, 'isExcludes': False, 'isRecursive': False}}
        return resources


class HiveServiceStore(ServiceStoreBase):

    def __init__(self, random_type="random"):
        self.random_generator = random_generators.get_random_generator(random_type)

    def generate_resources(self):
        tables = self.random_generator.generate_string_array()
        databases = self.random_generator.generate_string_array()
        columns = self.random_generator.generate_string_array()
        resources = {'table': {'values': tables, 'isExcludes': False, 'isRecursive': False},
                     'database': {'values': databases, 'isExcludes': False, 'isRecursive': False},
                     'column': {'values': columns, 'isExcludes': False, 'isRecursive': False}}
        return resources


class YarnServiceStore(ServiceStoreBase):

    def __init__(self, random_type="random"):
        self.random_generator = random_generators.get_random_generator(random_type)

    def generate_resources(self):
        queues = self.random_generator.generate_string_array()
        resources = {'queue': {'values': queues, 'isExcludes': False, 'isRecursive': False}}
        return resources


class KnoxServiceStore(ServiceStoreBase):

    def __init__(self, random_type="random"):
        self.random_generator = random_generators.get_random_generator(random_type)

    def generate_resources(self):
        services = self.random_generator.generate_string_array()
        topologies = self.random_generator.generate_string_array()
        resources = {'topology': {'values': topologies, 'isExcludes': False, 'isRecursive': False},
                     'service': {'values': services, 'isExcludes': False, 'isRecursive': False}}
        return resources


class SolrServiceStore(ServiceStoreBase):

    def __init__(self, random_type="random"):
        self.random_generator = random_generators.get_random_generator(random_type)

    def generate_resources(self):
        collections = self.random_generator.generate_string_array()
        resources = {'collection': {'values': collections, 'isExcludes': False, 'isRecursive': False}}
        return resources


class KafkaServiceStore(ServiceStoreBase):

    def __init__(self, random_type="random"):
        self.random_generator = random_generators.get_random_generator(random_type)

    def generate_resources(self):
        consumer_groups = self.random_generator.generate_string_array()
        resources = {'consumergroup': {'values': consumer_groups, 'isExcludes': False, 'isRecursive': False}}
        return resources


class AtlasServiceStore(ServiceStoreBase):

    def __init__(self, random_type="random"):
        self.random_generator = random_generators.get_random_generator(random_type)

    def generate_resources(self):
        entity_types = self.random_generator.generate_string_array()
        entity_classifications = self.random_generator.generate_string_array()
        entity_business_metadatas = self.random_generator.generate_string_array()
        entities = self.random_generator.generate_string_array()
        resources = {'entity-type': {'values': entity_types, 'isExcludes': False, 'isRecursive': False},
                     'entity-classification': {'values': entity_classifications, 'isExcludes': False,
                                               'isRecursive': False},
                     'entity': {'values': entities, 'isExcludes': False, 'isRecursive': False},
                     'entity-business-metadata': {'values': entity_business_metadatas, 'isExcludes': False,
                                                  'isRecursive': False}}
        return resources


class OzoneServiceStore(ServiceStoreBase):

    def __init__(self, random_type="random"):
        self.random_generator = random_generators.get_random_generator(random_type)

    def generate_resources(self):
        buckets = self.random_generator.generate_string_array()
        keys = self.random_generator.generate_string_array()
        volume = self.random_generator.generate_string_array()
        resources = {'bucket': {'values': buckets, 'isExcludes': False, 'isRecursive': False},
                     'key': {'values': keys, 'isExcludes': False, 'isRecursive': False},
                     'volume': {'values': volume, 'isExcludes': False, 'isRecursive': False}}
        return resources


class AdlsServiceStore(ServiceStoreBase):

    def __init__(self, random_type="random"):
        self.random_generator = random_generators.get_random_generator(random_type)

    def generate_resources(self):
        containers = self.random_generator.generate_string_array()
        paths = self.random_generator.generate_string_array()
        storage_accounts = self.random_generator.generate_string_array()
        resources = {'container': {'values': containers, 'isExcludes': False, 'isRecursive': False},
                     'relativepath': {'values': paths, 'isExcludes': False, 'isRecursive': True},
                     'storageaccount': {'values': storage_accounts, 'isExcludes': False, 'isRecursive': False}}
        return resources


class S3ServiceStore(ServiceStoreBase):
    def __init__(self, random_type="random"):
        self.random_generator = random_generators.get_random_generator(random_type)

    def generate_resources(self):
        buckets = self.random_generator.generate_string_array()
        paths = self.random_generator.generate_string_array()
        resources = {'bucket': {'values': buckets, 'isExcludes': False, 'isRecursive': False},
                     'path': {'values': paths, 'isExcludes': False, 'isRecursive': False}}
        return resources


class KuduServiceStore(ServiceStoreBase):
    def __init__(self, random_type="random"):
        self.random_generator = random_generators.get_random_generator(random_type)

    def generate_resources(self):
        columns = self.random_generator.generate_string_array()
        databases = self.random_generator.generate_string_array()
        tables = self.random_generator.generate_string_array()
        resources = {'table': {'values': tables, 'isExcludes': False, 'isRecursive': False},
                     'database': {'values': databases, 'isExcludes': False, 'isRecursive': False},
                     'column': {'values': columns, 'isExcludes': False, 'isRecursive': False}}
        return resources
