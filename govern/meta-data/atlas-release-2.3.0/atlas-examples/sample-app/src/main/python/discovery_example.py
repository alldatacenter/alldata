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

from utils import TABLE_TYPE

LOG = logging.getLogger('discovery-example')


class DiscoveryExample:
    DSL_QUERIES = {"from DataSet", "from Process"}

    def __init__(self, client):
        self.typesDef = None
        self.client   = client

    def dsl_search(self):
        for dsl_query in DiscoveryExample.DSL_QUERIES:
            try:
                result = self.client.discovery.dsl_search_with_params(dsl_query, 10, 0)

                if result:
                    entities_result = result.entities

                    if entities_result:
                        LOG.info("query: '%s' retrieved: %s rows", dsl_query, len(entities_result))

            except Exception as e:
                LOG.exception("query: %s failed in dsl search", dsl_query)

    def quick_search(self, search_string):
        try:
            result = self.client.discovery.quick_search(search_string, TABLE_TYPE, False, 2, 0)

            if result:
                LOG.info("Quick-search result: %s", result.searchResults)

        except Exception as e:
            LOG.exception("query: '%s' failed in quick search", search_string)

    def basic_search(self, type_name, classification, query):
        try:
            result = self.client.discovery.basic_search(type_name, classification, query, False, None, 'ASCENDING', 2, 0)

            if result:
                LOG.info("Basic-search result: %s", result)

        except Exception as e:
            LOG.exception("query: '%s' failed in basic search", query)
