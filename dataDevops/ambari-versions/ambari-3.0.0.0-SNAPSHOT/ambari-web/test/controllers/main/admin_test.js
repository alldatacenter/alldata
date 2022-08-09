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

require('models/service');
require('models/host_component');
require('controllers/main/admin');

describe('MainAdminController', function () {

  var controller = App.MainAdminController.create();

  describe('#isAccessAvailable()', function () {

    beforeEach(function () {
      Em.propertyDidChange(controller, 'isAccessAvailable');
    });

    it('Services do not match dependencies', function () {
      App.Service.find().clear();
      App.store.safeLoad(App.Service, {
        id: 'HDFS',
        service_name: 'HDFS'
      });
      expect(controller.get("isAccessAvailable")).to.be.false;
    });
    it('APP_TIMELINE_SERVER is absent', function () {
      App.Service.find().clear();
      App.HostComponent.find().clear();
      expect(controller.get("isAccessAvailable")).to.be.false;
    });
    it('Only one YARN service installed', function () {
      App.store.safeLoad(App.Service, {
        id: 'YARN',
        service_name: 'YARN'
      });
      expect(controller.get("isAccessAvailable")).to.be.false;
    });
    it('TEZ and YARN services installed', function () {
      App.store.safeLoad(App.Service, {
        id: 'TEZ',
        service_name: 'TEZ'
      });
      expect(controller.get("isAccessAvailable")).to.be.false;
    });
    it('TEZ and YARN services, APP_TIMELINE_SERVER component installed', function () {
      App.store.safeLoad(App.HostComponent, {
        id: 'APP_TIMELINE_SERVER_host1',
        component_name: 'APP_TIMELINE_SERVER'
      });
      expect(controller.get("isAccessAvailable")).to.be.true;
    });
  });
});
