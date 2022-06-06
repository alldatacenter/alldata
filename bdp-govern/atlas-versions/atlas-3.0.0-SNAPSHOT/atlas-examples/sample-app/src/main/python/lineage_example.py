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

from apache_atlas.model.enums import LineageDirection

LOG = logging.getLogger('lineage-example')


class LineageExample:
    def __init__(self, client):
        self.client = client

    def lineage(self, guid):
        direction    = LineageDirection.BOTH.name
        lineage_info = self.client.lineage.get_lineage_info(guid, direction, 0)

        if not lineage_info:
            LOG.info("Not able to find lineage info")
            return

        relations       = lineage_info.relations
        guid_entity_map = lineage_info.guidEntityMap

        for relation in relations:
            from_entity = guid_entity_map[relation.fromEntityId]
            to_entity   = guid_entity_map[relation.toEntityId]

            LOG.info("%s (%s) -> %s (%s)", from_entity.displayText, from_entity.typeName, to_entity.displayText, to_entity.typeName)