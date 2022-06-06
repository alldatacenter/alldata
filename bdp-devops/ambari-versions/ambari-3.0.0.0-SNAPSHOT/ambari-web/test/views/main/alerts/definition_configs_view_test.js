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

var alertDefinitionConfigsView, alertConfigRadioButtonView;

describe('App.AlertDefinitionConfigsView', function () {

  beforeEach(function () {
    alertDefinitionConfigsView = App.AlertDefinitionConfigsView.create({
      controller: Em.Object.create({
        renderConfigs: Em.K
      })
    });
  });

  describe("#init()", function() {

    beforeEach(function() {
      sinon.spy(alertDefinitionConfigsView.get('controller'), 'renderConfigs');
      alertDefinitionConfigsView.set('canEdit', true);
      alertDefinitionConfigsView.set('isWizard', true);
      alertDefinitionConfigsView.set('alertDefinitionType', 't1');
      alertDefinitionConfigsView.set('content', {});
      alertDefinitionConfigsView.init();
    });

    afterEach(function() {
      alertDefinitionConfigsView.get('controller').renderConfigs.restore();
    });

    it("should set canEdit to controller", function() {
      expect(alertDefinitionConfigsView.get('controller.canEdit')).to.be.true;
    });

    it("should set isWizard to controller", function() {
      expect(alertDefinitionConfigsView.get('controller.isWizard')).to.be.true;
    });

    it("should set alertDefinitionType to controller", function() {
      expect(alertDefinitionConfigsView.get('controller.alertDefinitionType')).to.equal('t1');
    });

    it("should set content to controller", function() {
      expect(alertDefinitionConfigsView.get('controller.content')).to.eql({});
    });

    it("should call renderConfigs of controller", function() {
      expect(alertDefinitionConfigsView.get('controller').renderConfigs.calledOnce).to.be.true;
    });
  });
});

describe('App.AlertConfigRadioButtonView', function () {

  beforeEach(function () {
    alertConfigRadioButtonView = App.AlertConfigRadioButtonView.create({
      parentView: Em.Object.create({
        controller: Em.Object.create({
          changeType: Em.K,
          configs: [
            Em.Object.create({
              group: 'g1',
              value: true
            })
          ]
        })
      }),
      property: {
        name: 'p1'
      }
    });
  });

  describe("#change", function() {
    var config = Em.Object.create({
      group: 'g1',
      value: true
    });

    beforeEach(function() {
      sinon.stub(alertConfigRadioButtonView.get('parentView.controller'), 'changeType');
      alertConfigRadioButtonView.reopen({
        name: 'g1'
      });
      alertConfigRadioButtonView.set('parentView.controller.configs', [config]);
      alertConfigRadioButtonView.change();
    });

    afterEach(function() {
      alertConfigRadioButtonView.get('parentView.controller').changeType.restore();
    });

    it("should set property.value", function() {
      expect(alertConfigRadioButtonView.get('property.value')).to.be.true;
    });

    it("should set config value", function() {
      expect(config.get('value')).to.be.false;
    });

    it("should call changeType()", function() {
      expect(alertConfigRadioButtonView.get('parentView.controller').changeType.calledWith('p1')).to.be.true;
    });
  });

});