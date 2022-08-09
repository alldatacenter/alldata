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
var misc = require('utils/misc');
var view;

describe('App.MainHostServiceMenuView', function () {

  beforeEach(function () {
    view = App.MainHostServiceMenuView.create({
      host: Em.Object.create(),
      controller: Em.Object.create({
        connectOutlet: Em.K
      })
    });
  });

  describe("#content", function() {

    beforeEach(function() {
      sinon.stub(App, 'get').returns([]);
      sinon.stub(App.StackService, 'find').returns([]);
      sinon.stub(misc, 'sortByOrder', function(stackServices, services) {
        return services;
      });
    });
    afterEach(function() {
      App.StackService.find.restore();
      misc.sortByOrder.restore();
      App.get.restore();
    });

    it("no hostComponents", function() {
      view.set('host', Em.Object.create({
        hostComponents: null
      }));
      view.propertyDidChange('content');
      expect(view.get('content')).to.be.empty;
    });

    it("hostComponents without service", function() {
      view.set('host', Em.Object.create({
        hostComponents: [
          Em.Object.create({
            service: null
          })
        ]
      }));
      view.propertyDidChange('content');
      expect(view.get('content')).to.be.empty;
    });

    it("hostComponents with service", function() {
      view.set('host', Em.Object.create({
        hostComponents: [
          Em.Object.create({
            service: Em.Object.create({
              serviceName: 'S1'
            })
          })
        ]
      }));
      view.propertyDidChange('content');
      expect(view.get('content').mapProperty('serviceName')).to.eql(['S1']);
    });

    it("hostComponents with the same services", function() {
      view.set('host', Em.Object.create({
        hostComponents: [
          Em.Object.create({
            service: Em.Object.create({
              serviceName: 'S1'
            })
          }),
          Em.Object.create({
            service: Em.Object.create({
              serviceName: 'S1'
            })
          })
        ]
      }));
      view.propertyDidChange('content');
      expect(view.get('content').mapProperty('serviceName')).to.eql(['S1']);
    });
  });



  describe("#showHostService()", function() {

    beforeEach(function() {
      sinon.stub(view.get('controller'), 'connectOutlet');
    });
    afterEach(function() {
      view.get('controller').connectOutlet.restore()
    });

    it("service is absent", function() {
      view.showHostService({contexts: []});
      expect(view.get('controller').connectOutlet.called).to.be.false;
    });

    it("service is present", function() {
      view.showHostService({contexts: [{serviceName: 'S1'}]});
      expect(view.get('controller').connectOutlet.calledWith('service_config_outlet', 'mainHostServiceConfigs', {serviceName: 'S1', host: Em.Object.create()})).to.be.true;
    });
  });

  describe("#didInsertElement()", function() {

    beforeEach(function() {
      sinon.stub(view, 'showHostService');
    });
    afterEach(function() {
      view.showHostService.restore();
    });

    it("showHostService should be called", function() {
      view.didInsertElement();
      expect(view.showHostService.calledWith({contexts: [undefined]})).to.be.true;
    });
  });

});