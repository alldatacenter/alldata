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

function getController() {
  return App.KerberosWizardStep1Controller.create({});
}

describe('App.KerberosWizardStep1Controller', function() {
  var controller;

  beforeEach(function() {
    controller = getController();
  });
  
  describe("#selectedOption", function () {
  	 
    it("test", function() {
      var options=controller.get('options');
      controller.propertyDidChange('selectedOption');
      var option = options.findProperty('value', controller.get('selectedItem'));
      expect(option.preConditions.everyProperty('checked', false)).to.be.true;
    });
    
  });

  describe("#loadStep()", function () {

    beforeEach(function() {
      controller.set('options', []);
    });

    it("on load selected item should not change", function() {
      controller.set('selectedItem',Em.I18n.t('admin.kerberos.wizard.step3.option.kdc'));	
      controller.loadStep();
      expect(controller.get('selectedItem')).to.be.equal(Em.I18n.t('admin.kerberos.wizard.step3.option.kdc'));
    });
  });

  describe("#submit()", function () {

    beforeEach(function() {
      sinon.stub(App.router, 'send');
    });

    afterEach(function() {
      App.router.send.restore();
    });

    it("App.router.send should be called", function() {
      controller.reopen({
        'isSubmitDisabled': false
      });
      controller.submit();
      expect(App.router.send.calledOnce).to.be.true;
    });

    it("App.router.send should not be called", function() {
      controller.reopen({
        'isSubmitDisabled': true
      });
      controller.submit();
      expect(App.router.send.called).to.be.false;
    });
  });

});
