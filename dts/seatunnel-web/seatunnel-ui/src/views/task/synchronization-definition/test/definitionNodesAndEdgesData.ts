/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

export default {
  edges: [
    {
      inputPluginId: '1',
      targetPluginId: '2'
    }
  ],
  plugins: [
    {
      config: '',
      connectorType: '',
      dataSourceId: '8600164027328',
      name: 'test-1',
      pluginId: '1',
      sceneMode: 'SINGLE_TABLE',
      selectTableFields: {
        all: false,
        tableFields: []
      },
      tableOptions: {
        databases: [],
        tables: []
      },
      type: 'source'
    },
    {
      config: '',
      connectorType: '',
      dataSourceId: '8600164027328',
      name: 'test-2',
      pluginId: '2',
      sceneMode: 'SINGLE_TABLE',
      selectTableFields: {
        all: false,
        tableFields: []
      },
      tableOptions: {
        databases: [],
        tables: []
      },
      type: 'sink'
    }
  ]
}
