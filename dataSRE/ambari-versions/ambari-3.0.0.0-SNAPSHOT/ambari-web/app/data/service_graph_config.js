/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var App = require('app');

/**
 This determines the graphs to display on the service page under each service.

 This is based on the name of the object associated with it.

 The name of the object is of the format: 'App.ChartServiceMetrics<name>' where <name>
 is one of the items below.
 **/

module.exports = {
    'ambari_metrics': [
      'AMS_MasterAverageLoad',
      'AMS_RegionServerStoreFiles',
      'AMS_RegionServerRegions',
      'AMS_RegionServerRequests',
      'AMS_RegionServerBlockCacheHitPercent',
      'AMS_RegionServerCompactionQueueSize'
    ],

    'flume': [
      'Flume_ChannelSizeMMA',
      'Flume_ChannelSizeSum',
      'Flume_IncommingMMA',
      'Flume_IncommingSum',
      'Flume_OutgoingMMA',
      'Flume_OutgoingSum'
    ]
};