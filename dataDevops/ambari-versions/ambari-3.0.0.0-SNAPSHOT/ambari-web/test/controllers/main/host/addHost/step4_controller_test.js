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
var testHelpers = require('test/helpers');

describe('App.AddHostStep4Controller', function() {
  var controller;

  before(function() {
    controller = App.AddHostStep4Controller.create();
  });

  describe("#loadConfigGroups()", function () {

    it("App.ajax.send should be called", function() {
      controller.loadConfigGroups();
      var args = testHelpers.filterAjaxRequests('name', 'config_groups.all_fields');
      expect(args[0][0]).to.eql({
        name: 'config_groups.all_fields',
        sender: controller,
        success: 'successLoadingConfigGroup',
        error: 'errorLoadingConfigGroup'
      });
    });
  });

  describe('#successLoadingConfigGroup()', function() {
    before(function() {
      controller.successLoadingConfigGroup({items: [{}]});
    });
    it('should set config groups on succeeded request', function() {
      expect(App.router.get('addHostController.content.configGroups')).to.eql([{}]);
    });
    it('should set `isConfigGroupLoaded` to true', function() {
      expect(controller.get('isConfigGroupLoaded')).to.true;
    });
  });
  
  describe('#errorLoadingConfigGroup()', function() {
    before(function() {
      controller.errorLoadingConfigGroup();
    });
    it('should set config groups on failed request', function() {
      expect(App.router.get('addHostController.content.configGroups')).to.eql([]);
    });
    it('should set `isConfigGroupLoaded` to true', function() {
      expect(controller.get('isConfigGroupLoaded')).to.true;
    });
  });
});
