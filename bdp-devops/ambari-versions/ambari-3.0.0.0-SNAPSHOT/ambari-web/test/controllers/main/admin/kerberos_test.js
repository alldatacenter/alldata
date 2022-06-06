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

describe('App.MainAdminKerberosController', function() {

  var controller = App.MainAdminKerberosController.create({});

  App.TestAliases.testAsComputedEqual(controller, 'isManualKerberos', 'kdc_type', 'none');

  App.TestAliases.testAsComputedSomeBy(controller, 'isPropertiesChanged', 'stepConfigs', 'isPropertiesChanged', true);

  App.TestAliases.testAsComputedOr(controller, 'isSaveButtonDisabled', ['isSubmitDisabled', '!isPropertiesChanged']);

  describe('#prepareConfigProperties', function() {
    beforeEach(function() {
      sinon.stub(App.Service, 'find').returns([
        Em.Object.create({ serviceName: 'KERBEROS'}),
        Em.Object.create({ serviceName: 'HDFS' })
      ]);
      this.result = controller.prepareConfigProperties([
        Em.Object.create({ name: 'prop1', isEditable: true, serviceName: 'SERVICE1'}),
        Em.Object.create({ name: 'prop2', isEditable: true, serviceName: 'KERBEROS'}),
        Em.Object.create({ name: 'prop3', isEditable: true, serviceName: 'HDFS'}),
        Em.Object.create({ name: 'prop4', isEditable: true, serviceName: 'Cluster'}),
        Em.Object.create({ name: 'prop5', isEditable: true, serviceName: 'SERVICE1'})
      ]);
    });

    afterEach(function() {
      App.Service.find.restore();
    });

    ['prop1', 'prop5'].forEach(function(item) {
      it('property `{0}` should be absent'.format(item), function() {
        expect(this.result.findProperty('name', item)).to.be.undefined;
      });
    });

    ['prop2', 'prop3', 'prop4'].forEach(function(item) {
      it('property `{0}` should be present and not editable'.format(item), function() {
        var prop = this.result.findProperty('name', item);
        expect(prop).to.be.ok;
        expect(prop.get('isEditable')).to.be.false;
      });
    });

    describe('should take displayType from predefinedSiteProperties', function () {

      beforeEach(function () {
        sinon.stub(App.configsCollection, 'getAll').returns([
          {
            name: 'hadoop.security.auth_to_local',
            displayType: 'multiLine'
          }
        ]);
      });

      afterEach(function () {
        App.configsCollection.getAll.restore();
      });

      it('displayType is valid', function () {
        expect(controller.prepareConfigProperties([
          Em.Object.create({
            name: 'hadoop.security.auth_to_local',
            serviceName: 'HDFS'
          })
        ])[0].get('displayType')).to.equal('multiLine');
      });

    });
  });

  describe("#runSecurityCheckSuccess()", function () {
    beforeEach(function () {
      sinon.stub(App, 'showClusterCheckPopup', Em.K);
      sinon.stub(controller, 'startKerberosWizard', Em.K);
    });
    afterEach(function () {
      App.showClusterCheckPopup.restore();
      controller.startKerberosWizard.restore();
    });
    it("shows popup", function () {
      var check = { items: [{
        UpgradeChecks: {
          "check": "Work-preserving RM/NM restart is enabled in YARN configs",
          "status": "FAIL",
          "reason": "FAIL",
          "failed_on": [],
          "check_type": "SERVICE"
        }
      }]};
      controller.runSecurityCheckSuccess(check,null,{label: "name"});
      expect(controller.startKerberosWizard.called).to.be.false;
      expect(App.showClusterCheckPopup.called).to.be.true;
    });
    it("runs startKerberosWizard", function () {
      var check = { items: [{
        UpgradeChecks: {
          "check": "Work-preserving RM/NM restart is enabled in YARN configs",
          "status": "PASS",
          "reason": "OK",
          "failed_on": [],
          "check_type": "SERVICE"
        }
      }]};
      controller.runSecurityCheckSuccess(check,null,{label: "name"});
      expect(controller.startKerberosWizard.called).to.be.true;
      expect(App.showClusterCheckPopup.called).to.be.false;
    });
  });

  describe('#regenerateKeytabs()', function () {

    beforeEach(function () {
      sinon.spy(controller, 'restartServicesAfterRegenerate');
      sinon.spy(controller, 'restartAllServices');
    });
    afterEach(function () {
      controller.restartServicesAfterRegenerate.restore();
      controller.restartAllServices.restore();
    });

    it('both confirmation popups should be displayed', function () {
      var popup = controller.regenerateKeytabs();
      expect(App.ModalPopup.show.calledOnce).to.be.true;
      popup.onPrimary();
      expect(controller.restartServicesAfterRegenerate.calledOnce).to.be.true;
      expect(App.ModalPopup.show.calledTwice).to.be.true;
    });

    it('user checked regeneration only for missing host/components', function () {
      var popup = controller.regenerateKeytabs();
      popup.set('regenerateKeytabsOnlyForMissing', true);

      var popup2 = popup.onPrimary();
      popup2.set('restartComponents', true)
      popup2.onPrimary();
      var args = testHelpers.findAjaxRequest('name', 'admin.kerberos_security.regenerate_keytabs');
      expect(args[0].data.type).to.be.equal('missing');
    });

    it('user didn\'t check regeneration only for missing host/components', function () {
      var popup = controller.regenerateKeytabs();
      popup.set('regenerateKeytabsOnlyForMissing', false);

      var popup2 = popup.onPrimary();
      popup2.set('restartComponents', true)
      popup2.onPrimary();

      var args = testHelpers.findAjaxRequest('name', 'admin.kerberos_security.regenerate_keytabs');
      expect(args[0].data.type).to.be.equal('all');
    });

    it('user checked restart services automatically', function () {
      var popup = controller.regenerateKeytabs();
      popup.set('regenerateKeytabsOnlyForMissing', true);

      var popup2 = popup.onPrimary();
      popup2.set('restartComponents', true)
      popup2.onPrimary();

      var args = testHelpers.findAjaxRequest('name', 'admin.kerberos_security.regenerate_keytabs');
      expect(args[0].data.withAutoRestart).to.be.true;
    });

    it('user didn\'t check restart services automatically', function () {
      var popup = controller.regenerateKeytabs();
      popup.set('regenerateKeytabsOnlyForMissing', true);

      var popup2 = popup.onPrimary();
      popup2.set('restartComponents', false)
      popup2.onPrimary();

      var args = testHelpers.findAjaxRequest('name', 'admin.kerberos_security.regenerate_keytabs');
      expect(args[0].data.withAutoRestart).to.be.false;
    });
  });

  describe('#getKDCSessionState()', function () {

    var mock = {callback: Em.K};

    beforeEach(function () {
      sinon.spy(mock, 'callback');
      sinon.stub(controller, 'getSecurityType', function (c) {
        c();
      });
    });

    afterEach(function () {
      mock.callback.restore();
      controller.getSecurityType.restore();
      Em.tryInvoke(App.get, 'restore');
    });

    [
      {
        m: 'Skip request, as securityEnabled and isKerberosEnabled are false',
        securityEnabled: false,
        isKerberosEnabled: false,
        kdc_type: 'not_none',
        result: false
      },
      {
        m: 'Skip request, as isManualKerberos is true',
        securityEnabled: true,
        isKerberosEnabled: true,
        kdc_type: 'none',
        result: false
      },
      {
        m: 'Make request',
        securityEnabled: true,
        isKerberosEnabled: true,
        kdc_type: 'not_none',
        result: true
      }
    ].forEach(function (test) {
          describe(test.m, function () {

            beforeEach(function () {
              sinon.stub(App, 'get').returns(test.isKerberosEnabled);
              controller.set('securityEnabled', test.securityEnabled);
              controller.set('kdc_type', test.kdc_type);
              controller.getKDCSessionState(mock.callback);
              this.args = testHelpers.findAjaxRequest('name', 'kerberos.session.state');
            });


            if (test.result) {
              it('callback is not called', function () {
                expect(mock.callback.calledOnce).to.be.false;
              });
              it('1 request is sent', function () {
                expect(this.args).to.exists;
              });
            }
            else {
              it('callback is called once', function () {
                expect(mock.callback.calledOnce).to.be.true;
              });
              it('no request is sent', function () {
                expect(this.args).to.not.exists;
              });
            }
          });
        });
  });

  describe('#getSecurityType()', function () {

    var mock = {callback: Em.K};

    beforeEach(function () {
      sinon.spy(mock, 'callback');
    });

    afterEach(function () {
      mock.callback.restore();
      Em.tryInvoke(App.get, 'restore');
    });

    [
      {
        m: 'Skip request, as securityEnabled and isKerberosEnabled are false',
        securityEnabled: false,
        isKerberosEnabled: false,
        kdc_type: '',
        result: false
      },
      {
        m: 'Skip request, as kdc_type exists',
        securityEnabled: true,
        isKerberosEnabled: true,
        kdc_type: 'none',
        result: false
      },
      {
        m: 'Make request',
        securityEnabled: true,
        isKerberosEnabled: true,
        kdc_type: '',
        result: true
      }
    ].forEach(function (test) {
          describe(test.m, function () {

            beforeEach(function () {
              sinon.stub(App, 'get').returns(test.isKerberosEnabled);
              controller.set('securityEnabled', test.securityEnabled);
              controller.set('kdc_type', test.kdc_type);
              controller.getSecurityType(mock.callback);
              this.args = testHelpers.findAjaxRequest('name', 'admin.security.cluster_configs.kerberos');
            });

            if (test.result) {
              it('callback os not called', function () {
                expect(mock.callback.calledOnce).to.be.false;
              });
              it('1 request is sent', function () {
                expect(this.args).to.exists;
              });
            } else {
              it('callback is called once', function () {
                expect(mock.callback.calledOnce).to.be.true;
              });
              it('no request is sent', function () {
                expect(this.args).to.not.exists;
              });
            }
          });
        });
  });

  describe('#getSecurityTypeSuccess', function() {
    [
      {
        data: { },
        e: 'none'
      },
      {
        data: {
          items: []
        },
        e: 'none'
      },
      {
        data: {
          items: [
            {
              configurations: []
            }
          ]
        },
        e: 'none'
      },
      {
        data: {
          items: [
            {
              configurations: [
                {
                  type: 'krb-conf',
                  properties: {
                    'kdc_type': 'mit'
                  }
                }
              ]
            }
          ]
        },
        e: 'none'
      },
      {
        data: {
          items: [
            {
              configurations: [
                {
                  type: 'kerberos-env',
                  properties: {
                    'kdc_type': 'mit'
                  }
                }
              ]
            }
          ]
        },
        e: 'mit'
      },
      {
        data: {
          items: [
            {
              configurations: [
                {
                  type: 'kerberos-env',
                  properties: {
                    'kdc_type': 'none'
                  }
                }
              ]
            }
          ]
        },
        e: 'none'
      }
    ].forEach(function(test) {
      it('json is ' + JSON.stringify(test.data) + ' kdc type should be ' + test.e, function() {
        controller.set('isManualKerberos', undefined);
        controller.getSecurityTypeSuccess(test.data, {}, {});
        expect(controller.get('kdc_type')).to.eql(test.e);
      });
    });
  });
});
