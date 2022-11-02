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

const App = require('app');

App.NameNodeWidgetMixin = Em.Mixin.create({

  groupId: 'nn',

  subGroupId: 'default',

  isAllItemsSubGroup: false,

  componentGroup: Em.computed.findByKey('model.masterComponentGroups', 'name', 'subGroupId'),

  clusterId: Em.computed.alias('componentGroup.clusterId'),

  hostName: function () {
    const allHostNames = this.get('componentGroup.hosts') || [],
      hostComponents = App.HostComponent.find().filter(component => {
        return component.get('componentName') === 'NAMENODE' && allHostNames.contains(component.get('hostName'));
      }),
      resultingComponent = hostComponents.findProperty('haStatus', 'active') || hostComponents.get('firstObject');
    return resultingComponent && resultingComponent.get('hostName');
  }.property('clusterId')

});