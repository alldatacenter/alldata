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

describe('App.HostTableMenuView', function() {
  var view;

  beforeEach(function() {
    view = App.HostTableMenuView.create();
  });

  describe("#components", function () {

    beforeEach(function() {
      sinon.stub(App.Service, 'find').returns([Em.Object.create({serviceName: 'S1'})]);
      this.mockMenu = sinon.stub(view, 'getBulkMenuItemsPerServiceComponent');
    });

    afterEach(function() {
      App.Service.find.restore();
      this.mockMenu.restore();
    });

    it("should return components", function() {
      this.mockMenu.returns([
        {serviceName: 'S1'},
        {serviceName: 'S2'}
      ]);
      view.propertyDidChange('components');
      expect(view.get('components')).to.be.eql([
        {serviceName: 'S1'}
      ]);
    });
  });

  describe("#getBulkMenuItemsPerServiceComponent()", function () {

    beforeEach(function() {
      sinon.stub(App.StackServiceComponent, 'find').returns([
        Em.Object.create({
          serviceName: 'S1',
          componentName: 'C1',
          bulkCommandsMasterComponentName: 'm1',
          bulkCommandsDisplayName: 'c1',
          hasBulkCommandsDefinition: {}
        }),
        Em.Object.create({
          serviceName: 'S2',
          componentName: 'C2',
          bulkCommandsMasterComponentName: 'm2',
          bulkCommandsDisplayName: 'c2',
          hasBulkCommandsDefinition: null
        })
      ]);
    });

    afterEach(function() {
      App.StackServiceComponent.find.restore();
    });

    it("should return bulk menu items", function() {
      expect(view.getBulkMenuItemsPerServiceComponent()).to.be.eql([
        Em.Object.create({
          serviceName: 'S1',
          componentName: 'C1',
          masterComponentName: 'm1',
          componentNameFormatted: 'c1'
        })
      ]);
    });
  });

  describe("#slaveItemView", function () {
    var slaveItemView;

    beforeEach(function () {
      slaveItemView = view.get('slaveItemView').create();
    });


    describe("#commonOperationView", function () {
      var commonOperationView;

      beforeEach(function () {
        commonOperationView = slaveItemView.get('commonOperationView').create({
          controller: Em.Object.create({
            bulkOperationConfirm: Em.K
          })
        });
      });

      describe("#click()", function () {

        beforeEach(function () {
          sinon.spy(commonOperationView.get('controller'), 'bulkOperationConfirm');
        });

        afterEach(function () {
          commonOperationView.get('controller').bulkOperationConfirm.restore();
        });

        it("bulkOperationConfirm should be called", function () {
          commonOperationView.setProperties({
            content: {},
            selection: 'selection'
          });
          commonOperationView.click();
          expect(commonOperationView.get('controller').bulkOperationConfirm.calledWith(
            {}, 'selection'
          )).to.be.true;
        });
      });
    });

    describe("#advancedOperationView", function () {
      var advancedOperationView;

      beforeEach(function() {
        advancedOperationView = slaveItemView.get('advancedOperationView').create({
          content: Em.Object.create(),
          controller: Em.Object.create({
            bulkOperationConfirm: Em.K
          })
        });
      });

      describe("#service", function () {

        beforeEach(function() {
          sinon.stub(App.router, 'get').returns([
            {serviceName: 'S1'}
          ]);
        });

        afterEach(function() {
          App.router.get.restore();
        });

        it("should return service", function() {
          advancedOperationView.set('content.serviceName', 'S1');
          advancedOperationView.propertyDidChange('service');
          expect(advancedOperationView.get('service')).to.be.eql({serviceName: 'S1'});
        });
      });

      describe("#tooltipMsg", function () {

        it("disabled", function() {
          advancedOperationView.reopen({
            disabledElement: 'disabled'
          });
          advancedOperationView.set('content.componentName', 'C1');
          advancedOperationView.set('content.message', 'msg');
          expect(advancedOperationView.get('tooltipMsg')).to.be.equal(
            Em.I18n.t('hosts.decommission.tooltip.warning').format('msg', 'c1')
          );
        });

        it("not disabled", function() {
          advancedOperationView.reopen({
            disabledElement: ''
          });
          advancedOperationView.set('content.componentName', 'C1');
          expect(advancedOperationView.get('tooltipMsg')).to.be.empty;
        });
      });

      describe("#disabledElement", function () {

        it("workStatus=STARTED", function() {
          advancedOperationView.reopen({
            service: Em.Object.create({
              workStatus: 'STARTED'
            })
          });
          advancedOperationView.propertyDidChange('disabledElement');
          expect(advancedOperationView.get('disabledElement')).to.be.empty;
        });

        it("workStatus=INSTALLED", function() {
          advancedOperationView.reopen({
            service: Em.Object.create({
              workStatus: 'INSTALLED'
            })
          });
          advancedOperationView.propertyDidChange('disabledElement');
          expect(advancedOperationView.get('disabledElement')).to.be.equal('disabled');
        });
      });

      describe("#click()", function () {

        beforeEach(function () {
          sinon.spy(advancedOperationView.get('controller'), 'bulkOperationConfirm');
        });

        afterEach(function () {
          advancedOperationView.get('controller').bulkOperationConfirm.restore();
        });

        it("bulkOperationConfirm should be called", function () {
          advancedOperationView.setProperties({
            content: {},
            selection: 'selection'
          });
          advancedOperationView.reopen({
            disabledElement: ''
          });
          advancedOperationView.click();
          expect(advancedOperationView.get('controller').bulkOperationConfirm.calledWith(
            {}, 'selection'
          )).to.be.true;
        });

        it("bulkOperationConfirm should not be called", function () {
          advancedOperationView.setProperties({
            content: {},
            selection: 'selection'
          });
          advancedOperationView.reopen({
            disabledElement: 'disabled'
          });
          advancedOperationView.click();
          expect(advancedOperationView.get('controller').bulkOperationConfirm.called).to.be.false;
        });
      });

      describe("#didInsertElement()", function () {

        beforeEach(function() {
          sinon.stub(App, 'tooltip');
        });

        afterEach(function() {
          App.tooltip.restore();
        });

        it("App.tooltip should be called", function() {
          advancedOperationView.didInsertElement();
          expect(App.tooltip.calledOnce).to.be.true;
        });
      });
    });
  });

  describe("#hostItemView", function () {
    var hostItemView;

    beforeEach(function () {
      hostItemView = view.get('hostItemView').create();
    });

    describe("#operationView", function () {
      var operationView;

      beforeEach(function () {
        operationView = hostItemView.get('operationView').create({
          controller: Em.Object.create({
            bulkOperationConfirm: Em.K
          })
        });
      });

      describe("#click()", function () {

        beforeEach(function () {
          sinon.spy(operationView.get('controller'), 'bulkOperationConfirm');
        });

        afterEach(function () {
          operationView.get('controller').bulkOperationConfirm.restore();
        });

        it("bulkOperationConfirm should be called", function () {
          operationView.setProperties({
            content: {},
            selection: 'selection'
          });
          operationView.click();
          expect(operationView.get('controller').bulkOperationConfirm.calledWith(
            {}, 'selection'
          )).to.be.true;
        });
      });
    });
  });

});
