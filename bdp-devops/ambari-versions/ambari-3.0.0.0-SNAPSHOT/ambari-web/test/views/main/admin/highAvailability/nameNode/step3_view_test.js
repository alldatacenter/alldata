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
require('views/main/admin/highAvailability/nameNode/step3_view');

describe('App.HighAvailabilityWizardStep3View', function () {
  var view = App.HighAvailabilityWizardStep3View.create({
    controller: Em.Object.create({
      content: {},
      loadStep: Em.K
    })
  });

  describe("#didInsertElement()", function () {
    before(function () {
      sinon.spy(view.get('controller'), 'loadStep');
    });
    after(function () {
      view.get('controller').loadStep.restore();
    });
    it("call loadStep", function () {
      view.didInsertElement();
      expect(view.get('controller').loadStep.calledOnce).to.be.true;
    });
  });

  describe("#curNameNode", function() {
    it("curNameNode is get from `masterComponentHosts`", function() {
      view.set('controller.content.masterComponentHosts', [{
        component: 'NAMENODE',
        isInstalled: true,
        hostName: 'host1'
      }]);
      view.propertyDidChange('curNameNode');
      expect(view.get('curNameNode')).to.equal('host1');
    });
  });

  describe("#addNameNode", function() {
    it("addNameNode is get from `masterComponentHosts`", function() {
      view.set('controller.content.masterComponentHosts', [{
        component: 'NAMENODE',
        isInstalled: false,
        hostName: 'host1'
      }]);
      view.propertyDidChange('addNameNode');
      expect(view.get('addNameNode')).to.equal('host1');
    });
  });

  describe("#secondaryNameNode", function() {
    it("secondaryNameNode is get from `masterComponentHosts`", function() {
      view.set('controller.content.masterComponentHosts', [{
        component: 'SECONDARY_NAMENODE',
        hostName: 'host1'
      }]);
      view.propertyDidChange('secondaryNameNode');
      expect(view.get('secondaryNameNode')).to.equal('host1');
    });
  });

  describe("#journalNodes", function() {
    it("journalNodes is get from `masterComponentHosts`", function() {
      view.set('controller.content.masterComponentHosts', [{
        component: 'JOURNALNODE',
        hostName: 'host1'
      }]);
      view.propertyDidChange('journalNodes');
      expect(view.get('journalNodes')).to.eql([{
        component: 'JOURNALNODE',
        hostName: 'host1'
      }]);
    });
  });
});
