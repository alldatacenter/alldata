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
import utils

from apache_atlas.utils           import type_coerce
from apache_atlas.model.misc      import SearchFilter
from apache_atlas.model.typedef   import AtlasTypesDef


LOG = logging.getLogger('sample-example')


class TypeDefExample:
    SAMPLE_APP_TYPES = [
        utils.DATABASE_TYPE,
        utils.TABLE_TYPE,
        utils.COLUMN_TYPE,
        utils.PROCESS_TYPE,
        utils.PII_TAG,
        utils.FINANCE_TAG,
        utils.METRIC_CLASSIFICATION
    ]

    def __init__(self, client):
        self.typesDef = None
        self.client   = client

    def create_type_def(self):
        try:
            if not self.typesDef:
                with open('request_json/typedef_create.json') as f:
                    typedef       = type_coerce(json.load(f), AtlasTypesDef)
                    self.typesDef = self.__create(typedef)
        except Exception as e:
            LOG.exception("Error in creating typeDef", exc_info=e)

    def print_typedefs(self):
        for type_name in TypeDefExample.SAMPLE_APP_TYPES:
            filter_params = {"name": type_name}
            search        = SearchFilter(filter_params)
            response      = self.client.typedef.get_all_typedefs(search)

            if response:
                LOG.info("Created type: [%s]", type_name)

    def remove_typedefs(self):
        if not self.typesDef:
            LOG.info("There is no typeDef to delete.")
        else:
            for type_name in TypeDefExample.SAMPLE_APP_TYPES:
                self.client.typedef.delete_type_by_name(type_name)

            self.typesDef = None

        LOG.info("Deleted typeDef successfully!")

    def __create(self, type_def):
        types_to_create = AtlasTypesDef()

        types_to_create.enumDefs             = []
        types_to_create.structDefs           = []
        types_to_create.classificationDefs   = []
        types_to_create.entityDefs           = []
        types_to_create.relationshipDefs     = []
        types_to_create.businessMetadataDefs = []

        for enum_def in type_def.enumDefs:
            if self.client.typedef.type_with_name_exists(enum_def.name):
                LOG.info("Type with name %s already exists. Skipping.", enum_def.name)
            else:
                types_to_create.enumDefs.append(enum_def)

        for struct_def in type_def.structDefs:
            if self.client.typedef.type_with_name_exists(struct_def.name):
                LOG.info("Type with name %s already exists. Skipping.", struct_def.name)
            else:
                types_to_create.structDefs.append(struct_def)

        for classification_def in type_def.classificationDefs:
            if self.client.typedef.type_with_name_exists(classification_def.name):
                LOG.info("Type with name %s already exists. Skipping.", classification_def.name)
            else:
                types_to_create.classificationDefs.append(classification_def)

        for entity_def in type_def.entityDefs:
            if self.client.typedef.type_with_name_exists(entity_def.name):
                LOG.info("Type with name %s already exists. Skipping.", entity_def.name)
            else:
                types_to_create.entityDefs.append(entity_def)

        for relationship_def in type_def.relationshipDefs:
            if self.client.typedef.type_with_name_exists(relationship_def.name):
                LOG.info("Type with name %s already exists. Skipping.", relationship_def.name)
            else:
                types_to_create.relationshipDefs.append(relationship_def)

        for business_metadata_def in type_def.businessMetadataDefs:
            if self.client.typedef.type_with_name_exists(business_metadata_def.name):
                LOG.info("Type with name %s already exists. Skipping.", business_metadata_def.name)
            else:
                types_to_create.businessMetadataDefs.append(business_metadata_def)

        return self.client.typedef.create_atlas_typedefs(types_to_create)
