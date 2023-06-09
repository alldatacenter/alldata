/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.drill.exec.store.druid;

public interface DruidTestConstants {
  String TEST_DATASOURCE_WIKIPEDIA = "wikipedia";
  String TEST_STRING_EQUALS_FILTER_QUERY_TEMPLATE1 = "select * from druid.`%s` as ds where ds.user = 'Dansker'";
  String TEST_STRING_TWO_AND_EQUALS_FILTER_QUERY_TEMPLATE1 = "select * from druid.`%s` as ds where ds.user = 'Dansker' AND ds.comment like 'Bitte'";
  String TEST_STRING_TWO_OR_EQUALS_FILTER_QUERY_TEMPLATE1 = "select * from druid.`%s` as ds where ds.user = 'Dansker' OR ds.page = '1904'";
  String TEST_QUERY_PROJECT_PUSH_DOWN_TEMPLATE_1 = "SELECT ds.`comment` FROM druid.`%s` as ds";
  String TEST_QUERY_COUNT_QUERY_TEMPLATE = "SELECT count(*) as mycount FROM druid.`%s` as ds";
}
