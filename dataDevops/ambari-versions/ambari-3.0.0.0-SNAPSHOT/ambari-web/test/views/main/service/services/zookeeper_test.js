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
require('/views/main/service/services/zookeeper');

describe('App.MainDashboardServiceZookeperView', function () {
  var view;

  beforeEach(function () {
    view = App.MainDashboardServiceZookeperView.create();
  });

  describe("#titleInfo", function () {

    it("should return ZOOKEEPER info", function () {
      view.set('service', Em.Object.create({
        hostComponents: [
          Em.Object.create({
            componentName: 'ZOOKEEPER_SERVER',
            workStatus: App.HostComponentStatus.stopped
          }),
          Em.Object.create({
            componentName: 'ZOOKEEPER_SERVER',
            workStatus: App.HostComponentStatus.started
          })
        ]
      }));
      view.propertyDidChange('titleInfo');
      expect(view.get('titleInfo')).to.be.eql({
        pre: view.t('services.zookeeper.prefix').format(1),
        title: view.t('services.zookeeper.title').format(2),
        component: Em.Object.create({
          componentName: 'ZOOKEEPER_SERVER',
          workStatus: App.HostComponentStatus.stopped
        })
      });
    });
  });
});