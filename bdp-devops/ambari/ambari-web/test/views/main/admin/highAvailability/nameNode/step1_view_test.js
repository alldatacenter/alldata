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
require('views/main/admin/highAvailability/nameNode/step1_view');

describe('App.HighAvailabilityWizardStep1View', function () {
  var view = App.HighAvailabilityWizardStep1View.create({
    controller: Em.Object.create({
      content: {}
    })
  });

  describe("#didInsertElement()", function() {
    before(function(){
      sinon.stub(App, 'popover', Em.K);
    });
    after(function(){
      App.popover.restore();
    });
    it("init popover", function() {
      view.didInsertElement();
      expect(App.popover.calledOnce).to.be.true;
    });
  });

  describe("#showInputError", function() {
    it("isNameServiceIdValid is true, nameServiceId is empty", function() {
      view.set('controller.isNameServiceIdValid', true);
      view.set('controller.content.nameServiceId', "");
      view.propertyDidChange('showInputError');
      expect(view.get('showInputError')).to.be.false;
    });
    it("isNameServiceIdValid is false, nameServiceId is empty", function() {
      view.set('controller.isNameServiceIdValid', false);
      view.set('controller.content.nameServiceId', "");
      view.propertyDidChange('showInputError');
      expect(view.get('showInputError')).to.be.false;
    });
    it("isNameServiceIdValid is true, nameServiceId is valid", function() {
      view.set('controller.isNameServiceIdValid', true);
      view.set('controller.content.nameServiceId', "name");
      view.propertyDidChange('showInputError');
      expect(view.get('showInputError')).to.be.false;
    });
    it("isNameServiceIdValid is false, nameServiceId is valid", function() {
      view.set('controller.isNameServiceIdValid', false);
      view.set('controller.content.nameServiceId', "name");
      view.propertyDidChange('showInputError');
      expect(view.get('showInputError')).to.be.true;
    });
  });
});
