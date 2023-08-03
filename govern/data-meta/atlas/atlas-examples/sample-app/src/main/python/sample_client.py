#!/usr/bin/env/python

#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import logging
import getpass

from apache_atlas.client.base_client import AtlasClient
from typedef_example                 import TypeDefExample
from entity_example                  import EntityExample
from lineage_example                 import LineageExample
from glossary_example                import GlossaryExample
from discovery_example               import DiscoveryExample
from utils                           import METRIC_CLASSIFICATION, NAME


LOG = logging.getLogger('sample-example')


class SampleApp:
    def __init__(self):
        self.created_entity = None

    def main(self):
        # Python3
        global input
        try: input = raw_input
        except NameError: pass

        url      = input('Enter Atlas URL: ')
        username = input('Enter username: ')
        password = getpass.getpass('Enter password: ')

        client = AtlasClient(url, (username, password))

        self.__typedef_example(client)

        self.__entity_example(client)

        self.__lineage_example(client)

        self.__discovery_example(client)

        self.__glossary_example(client)

        self.__entity_cleanup()


    def __typedef_example(self, client):
        LOG.info("\n---------- Creating Sample Types -----------")

        typedefExample = TypeDefExample(client)

        typedefExample.create_type_def()

    def __entity_example(self, client):
        LOG.info("\n---------- Creating Sample Entities -----------")

        self.entityExample = EntityExample(client)

        self.entityExample.create_entities()

        self.created_entity = self.entityExample.get_table_entity()

        if self.created_entity and self.created_entity.guid:
            self.entityExample.get_entity_by_guid(self.created_entity.guid)

    def __lineage_example(self, client):
        LOG.info("\n---------- Lineage example -----------")

        lineage = LineageExample(client)

        if self.created_entity:
            lineage.lineage(self.created_entity.guid)
        else:
            LOG.info("Create entity first to get lineage info")

    def __discovery_example(self, client):
        LOG.info("\n---------- Search example -----------")

        discovery = DiscoveryExample(client)

        discovery.dsl_search()

        if not self.created_entity:
            LOG.info("Create entity first to get search info")
            return

        discovery.quick_search(self.created_entity.typeName)

        discovery.basic_search(self.created_entity.typeName, METRIC_CLASSIFICATION, self.created_entity.attributes[NAME])

    def __glossary_example(self, client):
        LOG.info("\n---------- Glossary Example -----------")

        glossary     = GlossaryExample(client)
        glossary_obj = glossary.create_glossary()

        if not glossary_obj:
            LOG.info("Create glossary first")
            return

        glossary.create_glossary_term()
        glossary.get_glossary_detail()
        glossary.create_glossary_category()
        glossary.delete_glossary()

    def __entity_cleanup(self):
        LOG.info("\n---------- Deleting Entities -----------")

        self.entityExample.remove_entities()


if __name__ == "__main__":
    SampleApp().main()
