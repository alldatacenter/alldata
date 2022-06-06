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
require('views/main/admin/stack_upgrade/services_view');

describe('App.MainAdminStackServicesView', function () {
  var view = App.MainAdminStackServicesView.create();

  describe("#goToAddService()" , function() {
    var mock = Em.Object.create({
        checkAndStartKerberosWizard: Em.K,
        setDBProperty: sinon.spy()
      }),
      isAccessibleMock;
    beforeEach(function() {
      sinon.stub(App.get('router'), 'transitionTo', Em.K);
      sinon.stub(App.router, 'get').returns(mock);
      sinon.spy(mock, 'checkAndStartKerberosWizard');
      isAccessibleMock = sinon.stub(App, 'isAuthorized');
      App.set('supports.enableAddDeleteServices', true);
    });
    afterEach(function() {
      App.get('router').transitionTo.restore();
      App.router.get.restore();
      mock.checkAndStartKerberosWizard.restore();
      App.isAuthorized.restore();
    });
    it ("operations locked", function () {
      isAccessibleMock.returns(false);
      view.goToAddService();
      expect(App.router.get.called).to.be.false;
      expect(App.get('router').transitionTo.called).to.be.false;
    });
    it("routes to Add Service Wizard and set redirect path on wizard close", function() {
      isAccessibleMock.returns(true);
      view.goToAddService({context: "serviceName"});
      expect(App.router.get.calledWith('addServiceController')).to.be.true;
      expect(mock.setDBProperty.calledWith('onClosePath', 'main.admin.stackAndUpgrade.services')).to.be.true;
      expect(App.get('router').transitionTo.calledWith('main.serviceAdd')).to.be.true;
      expect(mock.get('serviceToInstall')).to.be.equal("serviceName");
    });
    it("routes to Security Wizard", function() {
      isAccessibleMock.returns(true);
      view.goToAddService({context: "KERBEROS"});
      expect(App.router.get.calledWith('kerberosWizardController')).to.be.true;
      expect(mock.setDBProperty.calledWith('onClosePath', 'main.admin.stackAndUpgrade.services')).to.be.true;
      expect(mock.checkAndStartKerberosWizard.calledOnce).to.be.true;
    });
  });

  describe('#isAddServiceAvailable', function () {

    var cases = [true, false],
      titleTemplate = 'should be {0}',
      isAccessibleMock;

    beforeEach(function() {
      isAccessibleMock = sinon.stub(App, 'isAuthorized');
    });

    afterEach(function() {
      App.isAuthorized.restore();
    });

    cases.forEach(function (item) {
      it(titleTemplate.format(item.toString()), function () {
        isAccessibleMock.returns(item);
        view.propertyDidChange('isAddServiceAvailable');
        expect(view.get('isAddServiceAvailable')).to.equal(item);
      });
    });

  });
});
