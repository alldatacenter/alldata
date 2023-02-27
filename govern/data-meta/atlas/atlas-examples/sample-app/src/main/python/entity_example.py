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

import json
import logging

from apache_atlas.model.enums    import EntityOperation
from apache_atlas.model.instance import AtlasEntityWithExtInfo, AtlasRelatedObjectId
from apache_atlas.utils          import type_coerce


LOG = logging.getLogger('entity-example')


class EntityExample:
    DATABASE_NAME                = "employee_db_entity"
    TABLE_NAME                   = "employee_table_entity"
    PROCESS_NAME                 = "employee_process_entity"
    METADATA_NAMESPACE_SUFFIX    = "@cl1"
    MANAGED_TABLE                = "Managed"
    ATTR_NAME                    = "name"
    ATTR_DESCRIPTION             = "description"
    ATTR_QUALIFIED_NAME          = "qualifiedName"
    REFERENCEABLE_ATTRIBUTE_NAME = ATTR_QUALIFIED_NAME
    ATTR_TIME_ID_COLUMN          = "time_id"
    ATTR_CUSTOMER_ID_COLUMN      = "customer_id"
    ATTR_COMPANY_ID_COLUMN       = "company_id"

    def __init__(self, client):
        self.client              = client
        self.entity_db           = None
        self.entity_table_us     = None
        self.entity_table_canada = None
        self.entity_process      = None

    def create_entities(self):
        self.__create_db()
        self.__create_us_table()
        self.__create_canada_table()
        self.__create_process()

    def get_table_entity(self):
        if self.entity_table_us:
            return self.entity_table_us

        return None

    def get_entity_by_guid(self, guid):
        entity = self.client.entity.get_entity_by_guid(guid)

        LOG.info("Entity(guid=%s): typeName=%s, attr.name=%s", guid, entity.entity.typeName, entity.entity.attributes['name'])

    def remove_entities(self):
        entity_list = [ self.entity_process.guid, self.entity_table_us.guid, self.entity_table_canada.guid, self.entity_db.guid ]

        self.client.entity.delete_entities_by_guids(entity_list)

        response = self.client.entity.purge_entities_by_guids(entity_list)

        if response is not None:
            LOG.info("Purged entities")
        else:
            LOG.info("Purge failed!")

    def __create_db(self):
        if not self.entity_db:
            with open('request_json/entity_create_db.json') as f:
                entity = type_coerce(json.load(f), AtlasEntityWithExtInfo)

                self.entity_db = self.__create_db_helper(entity)

        if self.entity_db:
            LOG.info("Created database entity: guid=%s, attr.name=%s", self.entity_db.guid, self.entity_db.attributes['name'])
        else:
            LOG.info("Failed to create database entity")

    def __create_us_table(self):
        if not self.entity_table_us:
            with open('request_json/entity_create_table_us.json') as f:
                entity = type_coerce(json.load(f), AtlasEntityWithExtInfo)

                self.entity_table_us = self.__create_table_helper(entity)

            if self.entity_table_us:
                LOG.info("Created US table entity: guid=%s, attr.name=%s", self.entity_table_us.guid, self.entity_table_us.attributes['name'])
            else:
                LOG.info("Failed to create US table entity")

    def __create_canada_table(self):
        if not self.entity_table_canada:
            with open('request_json/entity_create_table_canada.json') as f:
                entity = type_coerce(json.load(f), AtlasEntityWithExtInfo)

                self.entity_table_canada = self.__create_table_helper(entity)

            if self.entity_table_canada:
                LOG.info("Created Canada table entity: guid=%s, attr.name=%s", self.entity_table_canada.guid, self.entity_table_canada.attributes['name'])
            else:
                LOG.info("Failed to create Canada table entity")

    def __create_process(self):
        if not self.entity_process:
            with open('request_json/entity_create_process.json') as f:
                entity = type_coerce(json.load(f), AtlasEntityWithExtInfo)

                self.entity_process = self.__create_process_helper(entity)

        if self.entity_process:
            LOG.info("Created process entity: guid=%s, attr.name=%s", self.entity_process.guid, self.entity_process.attributes['name'])
        else:
            LOG.info("Failed to createa process entity")

    def __create_db_helper(self, entity):
        self.__create_entity(entity)

        return entity.entity

    def __create_table_helper(self, entity):
        table = entity.entity

        if self.entity_db:
            dbId = AtlasRelatedObjectId({ 'guid': self.entity_db.guid })

            LOG.info("setting: table(%s).db=%s", table.guid, dbId)

            table.relationshipAttributes['db'] = dbId

        self.__create_entity(entity)

        return table

    def __create_process_helper(self, entity):
        process = entity.entity

        process.relationshipAttributes = {}

        if self.entity_table_us:
            process.relationshipAttributes['inputs'] = [ AtlasRelatedObjectId({ 'guid': self.entity_table_us.guid }) ]

        if self.entity_table_canada:
            process.relationshipAttributes['outputs'] = [ AtlasRelatedObjectId({'guid': self.entity_table_canada.guid }) ]

        return self.__create_entity(entity)

    def __create_entity(self, entity):
        try:
            response = self.client.entity.create_entity(entity)

            guid = None

            if response and response.mutatedEntities:
                if EntityOperation.CREATE.name in response.mutatedEntities:
                    header_list = response.mutatedEntities[EntityOperation.CREATE.name]
                elif EntityOperation.UPDATE.name in response.mutatedEntities:
                    header_list = response.mutatedEntities[EntityOperation.UPDATE.name]

                if header_list and len(header_list) > 0:
                    guid = header_list[0].guid
            elif response and response.guidAssignments:
                if entity.entity is not None and entity.entity.guid is not None:
                    in_guid = entity.entity.guid
                else:
                    in_guid = None

                if in_guid and response.guidAssignments[in_guid]:
                    guid = response.guidAssignments[in_guid]

            if guid:
                entity.entity.guid = guid
        except Exception as e:
            LOG.exception("failed to create entity %s. error=%s", entity, e)

        return entity.entity if entity and entity.entity else None
