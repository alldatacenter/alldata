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

App.MainDashboardServiceZookeperView = App.MainDashboardServiceView.extend({
  templateName: require('templates/main/service/services/zookeeper'),
  serviceName: 'zookeeper',

  titleInfo: function () {
    var components = this.get('service.hostComponents').filterProperty('componentName', 'ZOOKEEPER_SERVER');
    var running = 0;
    components.forEach(function (item) {
      if (item.get('workStatus') === App.HostComponentStatus.started) {
        running++;
      }
    });

    return {
      pre: this.t('services.zookeeper.prefix').format(running),
      title: this.t('services.zookeeper.title').format(components.length),
      component: components.objectAt(0)
    };
  }.property('service')
});