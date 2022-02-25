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
require('views/wizard/step10_view');
var view;
describe('App.WizardStep10View', function() {
  beforeEach(function() {
    view = App.WizardStep10View.create({
      controller: App.WizardStep10Controller.create({
        isAddServiceWizard: false
      })
    });
  });
  describe('#didInsertElement()', function() {

    beforeEach(function () {
      sinon.stub(view.get('controller'), 'loadStep', Em.K);
    });

    afterEach(function () {
      view.get('controller').loadStep.restore();
    });

    it('should call loadStep', function() {
      view.didInsertElement();
      expect(view.get('controller').loadStep.calledOnce).to.equal(true);
    });
  });

  describe("#serviceRestartText", function() {
    it("text is empty", function() {
      view.set('controller.isAddServiceWizard', false);
      expect(view.get('serviceRestartText')).to.be.empty;
    });
    it("text is complete", function() {
      view.set('controller.isAddServiceWizard', true);
      expect(view.get('serviceRestartText')).to.equal(Em.I18n.t('common.important.strong') + Em.I18n.t('installer.step10.staleServicesRestartRequired'));
    });
  });
});
