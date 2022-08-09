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
var setups = require('test/init_model_test');
var testHelpers = require('test/helpers');

function getController() {
  return App.KerberosWizardStep2Controller.create({
    wizardController: Em.Object.create({
      deleteKerberosService: Em.K
    }),
    controllers: Em.Object.create(),
    content: Em.Object.create()
  });
}

describe('App.KerberosWizardStep2Controller', function() {
  var controller;

  beforeEach(function() {
    controller = getController();
  });

  App.TestAliases.testAsComputedOr(getController(), 'isBackBtnDisabled', ['testConnectionInProgress', 'App.router.nextBtnClickInProgress'], 'boolean');

  App.TestAliases.testAsComputedAlias(getController(), 'hostNames', 'App.allHostNames', 'array');

  App.TestAliases.testAsComputedAlias(getController(), 'isConfigsLoaded', 'wizardController.stackConfigsLoaded', 'boolean');

  describe('#createKerberosSiteObj', function() {

    beforeEach(function() {
      setups.setupStackVersion(this, 'HDP-2.3');
      sinon.stub(controller, 'tweakKdcTypeValue', Em.K);
      sinon.stub(controller, 'tweakManualKdcProperties', Em.K);
    });

    after(function() {
      setups.restoreStackVersion(this);
      controller.tweakKdcTypeValue.restore();
      controller.tweakManualKdcProperties.restore();
    });

    var _createProperty = function(name, value, displayType) {
      var preDefProp = App.config.get('preDefinedSiteProperties').findProperty('name', name);
      if (preDefProp) {
        return App.ServiceConfigProperty.create(
          $.extend(true, {}, preDefProp, {
            value: value, filename: 'some-site.xml',
            'displayType': displayType,
            isRequiredByAgent: preDefProp.isRequiredByAgent === undefined ? true : preDefProp.isRequiredByAgent
          }));
      }
      return App.ServiceConfigProperty.create({name: name, value: value, isRequiredByAgent: true, filename: 'some-site.xml'});
    };

    var tests = [
      {
        stepConfigs: [
          ['realm', ' SPACES ', 'host'],
          ['admin_server_host', ' space_left', 'host'],
          ['kdc_hosts', ' space_left_and_right ', 'host'],
          ['ldap_url', 'space_right ', 'host']
        ],
        e: {
          realm: 'SPACES',
          admin_server_host: 'space_left',
          kdc_hosts: 'space_left_and_right',
          ldap_url: 'space_right'
        }
      }
    ];

    tests.forEach(function(test) {
      describe('Should trim values for properties ' + Em.keys(test.e).join(','), function() {
        var result;

        beforeEach(function () {
          sinon.stub(App.StackService, 'find').returns([Em.Object.create({serviceName: 'KERBEROS'})]);
          controller.set('stepConfigs', [
            App.ServiceConfig.create({
              configs: test.stepConfigs.map(function(item) { return _createProperty(item[0], item[1], item[2]); })
            })
          ]);
          result = controller.createKerberosSiteObj('some-site', 'random-tag');
        });

        afterEach(function () {
          App.StackService.find.restore();
        });

        Em.keys(test.e).forEach(function(propertyName) {
          it(propertyName, function () {
            expect(result.properties[propertyName]).to.be.eql(test.e[propertyName]);
          });
        });
      });
    });
  });

  describe("#isSubmitDisabled", function () {
    var testCases = [
      {
        title: 'stepConfigs is empty',
        data: {
          stepConfigs: []
        },
        expected: true
      },
      {
        title: 'testConnectionInProgress is true',
        data: {
          stepConfigs: [{}],
          testConnectionInProgress: true
        },
        expected: true
      },
      {
        title: 'submitButtonClicked is true',
        data: {
          stepConfigs: [{}],
          testConnectionInProgress: false,
          submitButtonClicked: true
        },
        expected: true
      },
      {
        title: 'configs has errors',
        data: {
          stepConfigs: [{showConfig: true, errorCount: 1}],
          testConnectionInProgress: false,
          submitButtonClicked: false
        },
        expected: true
      },
      {
        title: 'miscModalVisible is true',
        data: {
          stepConfigs: [{showConfig: true, errorCount: 0}],
          testConnectionInProgress: false,
          submitButtonClicked: false,
          miscModalVisible: true
        },
        expected: true
      },
      {
        title: 'miscModalVisible is false',
        data: {
          stepConfigs: [{showConfig: true, errorCount: 0}],
          testConnectionInProgress: false,
          submitButtonClicked: false,
          miscModalVisible: false
        },
        expected: false
      }
    ];

    testCases.forEach(function(test) {
      it(test.title, function() {
        controller.setProperties(test.data);
        controller.propertyDidChange('isSubmitDisabled');
        expect(controller.get('isSubmitDisabled')).to.be.equal(test.expected);
      });
    });
  });

  describe("#clearStep()", function () {

    beforeEach(function() {
      controller.setProperties({
        configs: [{}],
        serviceConfigTags: [{}],
        servicesInstalled: true
      });
      controller.clearStep();
    });

    it("configs should be empty", function() {
      expect(controller.get('configs')).to.be.empty;
    });

    it("serviceConfigTags should be empty", function() {
      expect(controller.get('serviceConfigTags')).to.be.empty;
    });

    it("servicesInstalled should be false", function() {
      expect(controller.get('servicesInstalled')).to.be.false;
    });
  });

  describe("#loadStep()", function () {

    beforeEach(function() {
      this.mockStackService = sinon.stub(App.StackService, 'find').returns([{
        serviceName: 'KERBEROS'
      }]);
      sinon.stub(controller, 'clearStep');
      sinon.stub(App.config, 'setPreDefinedServiceConfigs');
      sinon.stub(App.config, 'mergeStoredValue');
      sinon.stub(controller, 'filterConfigs');
      sinon.stub(controller, 'getKerberosConfigs');
      sinon.stub(controller, 'initializeKDCStoreProperties');
      sinon.stub(controller, 'applyServicesConfigs');
      sinon.stub(controller, 'updateKDCStoreProperties');
      controller.reopen({
        isConfigsLoaded: true,
        stepConfigs: [Em.Object.create({serviceName: 'KERBEROS'})],
        content: Em.Object.create({
          serviceConfigProperties: [{}]
        }),
        wizardController: {
          skipClientInstall: true
        }
      });
    });

    afterEach(function() {
      this.mockStackService.restore();
      controller.clearStep.restore();
      App.config.setPreDefinedServiceConfigs.restore();
      App.config.mergeStoredValue.restore();
      controller.filterConfigs.restore();
      controller.getKerberosConfigs.restore();
      controller.initializeKDCStoreProperties.restore();
      controller.applyServicesConfigs.restore();
      controller.updateKDCStoreProperties.restore();
    });

    it("KERBEROS service absent", function() {
      this.mockStackService.returns([]);
      expect(controller.loadStep()).to.be.false;
    });

    it("configs not loaded", function() {
      controller.set('isConfigsLoaded', false);
      expect(controller.loadStep()).to.be.false;
    });

    it("clearStep should be called", function() {
      controller.loadStep();
      expect(controller.clearStep.calledOnce).to.be.true;
    });

    it("App.config.setPreDefinedServiceConfigs should be called", function() {
      controller.loadStep();
      expect(App.config.setPreDefinedServiceConfigs.calledOnce).to.be.true;
    });

    it("getKerberosConfigs should be called", function() {
      controller.set('content.serviceConfigProperties', null);
      controller.loadStep();
      expect(controller.getKerberosConfigs.calledOnce).to.be.true;
    });

    it("filterConfigs should be called", function() {
      controller.loadStep();
      expect(controller.filterConfigs.calledOnce).to.be.true;
    });

    it("initializeKDCStoreProperties should be called", function() {
      controller.set('wizardController.skipClientInstall', false);
      controller.loadStep();
      expect(controller.initializeKDCStoreProperties.calledOnce).to.be.true;
    });

    it("applyServicesConfigs should be called", function() {
      controller.loadStep();
      expect(controller.applyServicesConfigs.calledOnce).to.be.true;
    });

    it("updateKDCStoreProperties should be called", function() {
      controller.set('wizardController.skipClientInstall', false);
      controller.loadStep();
      expect(controller.updateKDCStoreProperties.calledOnce).to.be.true;
    });
  });

  describe("#getKerberosConfigs()", function () {

    beforeEach(function() {
      this.mock = sinon.stub(App.configsCollection, 'getAll');
      sinon.stub(App.config, 'getConfigTagFromFileName').returns('t1');
      sinon.stub(App.config, 'get').returns([
        Em.Object.create({
          serviceName: 'KERBEROS',
          configTypes: {
            't1': {}
          }
        })
      ]);
    });

    afterEach(function() {
      this.mock .restore();
      App.config.getConfigTagFromFileName.restore();
      App.config.get.restore();
    });

    it("fileName not specified (serviceName S1)", function() {
      this.mock.returns([
        {
          serviceName: 'S1'
        }
      ]);
      expect(controller.getKerberosConfigs()).to.be.empty;
    });

    it("fileName not specified (serviceName KERBEROS)", function() {
      this.mock.returns([
        {
          serviceName: 'KERBEROS'
        }
      ]);
      expect(controller.getKerberosConfigs()).to.be.eql([
        {
          serviceName: 'KERBEROS'
        }
      ]);
    });

    it("incorrect service", function() {
      this.mock.returns([
        {
          fileName: 'f1',
          serviceName: 'S1'
        }
      ]);
      expect(controller.getKerberosConfigs()).to.be.eql([
        {
          fileName: 'f1',
          serviceName: 'S1'
        }
      ]);
    });

    it("fileName and service correct", function() {
      this.mock.returns([
        {
          fileName: 'f1',
          serviceName: 'KERBEROS'
        }
      ]);
      expect(controller.getKerberosConfigs()).to.be.eql([
        {
          fileName: 'f1',
          serviceName: 'KERBEROS'
        }
      ]);
    });
  });

  describe("#filterConfigs()", function () {
    var configs = [
      Em.Object.create({
        serviceName: 'KERBEROS',
        isVisible: false
      }),
      Em.Object.create({
        serviceName: 'S1',
        isVisible: false
      })
    ];

    beforeEach(function() {
      controller.set('controllers', {
        kerberosWizardController: Em.Object.create({
          skipClientInstall: false,
          overrideVisibility: Em.K
        })
      });
      controller.set('content', Em.Object.create({
        kerberosOption: null
      }));
      sinon.stub(controller.get('controllers.kerberosWizardController'), 'overrideVisibility');
      sinon.stub(controller, 'setKDCTypeProperty');
      sinon.stub(controller, 'setConfigVisibility');
    });

    afterEach(function() {
      controller.setKDCTypeProperty.restore();
      controller.setConfigVisibility.restore();
      controller.get('controllers.kerberosWizardController').overrideVisibility.restore();
    });

    it("KERBEROS config should be visible", function() {
      controller.filterConfigs(configs);
      expect(configs.mapProperty('isVisible')).to.be.eql([true, false]);
    });

    it("setKDCTypeProperty should be called", function() {
      controller.filterConfigs(configs);
      expect(controller.setKDCTypeProperty.calledOnce).to.be.true;
    });

    it("setConfigVisibility should not be called", function() {
      controller.set('content.kerberosOption', Em.I18n.t('admin.kerberos.wizard.step1.option.manual'));
      controller.filterConfigs(configs);
      expect(controller.setConfigVisibility.called).to.be.false;
    });

    it("overrideVisibility should be called", function() {
      controller.set('content.kerberosOption', Em.I18n.t('admin.kerberos.wizard.step1.option.manual'));
      controller.set('controllers.kerberosWizardController.skipClientInstall', true);
      controller.filterConfigs(configs);
      expect(controller.get('controllers.kerberosWizardController').overrideVisibility.called).to.be.true;
    });

    it("overrideVisibility results are valid", function() {
      configs = [{
        name: 'manage_identities'
      }];
      controller.filterConfigs(configs);
      expect(configs[0].isVisible).to.be.false;
      expect(configs[0].value).to.be.equal('true');
    });

    it("setConfigVisibility should be called", function() {
      controller.filterConfigs(configs);
      expect(controller.setConfigVisibility.calledThrice).to.be.true;
    });
  });

  describe("#setConfigVisibility()", function () {

    it("ad type configs", function() {
      var configs = [{name: 'ldap_url'}];
      controller.setConfigVisibility('ad', configs, Em.I18n.t('admin.kerberos.wizard.step1.option.ad'));
      expect(configs[0].isVisible).to.be.true;
    });

    it("mit type configs", function() {
      var configs = [{name: 'kdc_create_attributes'}];
      controller.setConfigVisibility('mit', configs, Em.I18n.t('admin.kerberos.wizard.step1.option.kdc'));
      expect(configs[0].isVisible).to.be.true;
    });

    it("ipa type configs", function() {
      var configs = [{name: 'ipa_user_group'}];
      controller.setConfigVisibility('ipa', configs, Em.I18n.t('admin.kerberos.wizard.step1.option.ipa'));
      expect(configs[0].isVisible).to.be.true;
    });
  });

  describe("#submit()", function () {

    beforeEach(function() {
      sinon.stub(controller.get('wizardController'), 'deleteKerberosService').returns({
        always: Em.clb
      });
      sinon.stub(controller, 'configureKerberos');
      controller.reopen({
        isSubmitDisabled: false
      });
    });

    afterEach(function() {
      controller.get('wizardController').deleteKerberosService.restore();
      controller.configureKerberos.restore();
    });

    it("deleteKerberosService should not be called", function() {
      controller.set('isSubmitDisabled', true);
      expect(controller.submit()).to.be.false;
      expect(controller.get('wizardController').deleteKerberosService.called).to.be.false;
    });

    it("deleteKerberosService should be called", function() {
      controller.submit();
      expect(controller.get('wizardController').deleteKerberosService.called).to.be.true;
    });

    it("configureKerberos should be called", function() {
      controller.submit();
      expect(controller.configureKerberos.calledOnce).to.be.true;
    });
  });

  describe("#configureKerberos()", function () {
    var mock = Em.Object.create({
      createKerberosResources: Em.K
    });

    beforeEach(function() {
      sinon.stub(App.router, 'get').returns(mock);
      sinon.stub(controller, 'createConfigurations').returns({
        done: Em.clb
      });
      sinon.stub(controller, 'createKerberosAdminSession').returns({
        done: Em.clb
      });
      sinon.stub(App.router, 'send');
      sinon.stub(mock, 'createKerberosResources');
    });

    afterEach(function() {
      App.router.get.restore();
      controller.createConfigurations.restore();
      controller.createKerberosAdminSession.restore();
      App.router.send.restore();
      mock.createKerberosResources.restore();
    });

    it("createConfigurations should be called", function() {
      mock.set('skipClientInstall', true);
      controller.configureKerberos();
      expect(controller.createConfigurations.calledOnce).to.be.true;
    });

    it("createKerberosAdminSession should be called", function() {
      mock.set('skipClientInstall', true);
      controller.configureKerberos();
      expect(controller.createKerberosAdminSession.calledOnce).to.be.true;
    });

    it("App.router.send should be called", function() {
      mock.set('skipClientInstall', true);
      controller.configureKerberos();
      expect(App.router.send.calledWith('next')).to.be.true;
    });

    it("createKerberosResources should be called", function() {
      mock.set('skipClientInstall', false);
      controller.configureKerberos();
      expect(mock.createKerberosResources.calledOnce).to.be.true;
    });
  });

  describe("#createConfigurations()", function () {

    beforeEach(function() {
      sinon.stub(App.StackService, 'find').returns([
        Em.Object.create({
          serviceName: 'KERBEROS',
          configTypes: {
            t1: {},
            t3: {}
          },
          configTypesRendered: {
            t1: {},
            t2: {}
          }
        })
      ]);
      sinon.stub(controller, 'createKerberosSiteObj').returns({
        type: 't1'
      });
    });

    afterEach(function() {
      App.StackService.find.restore();
      controller.createKerberosSiteObj.restore();
    });

    it("App.ajax.send should be called", function() {
      controller.createConfigurations();
      var args = testHelpers.findAjaxRequest('name', 'common.across.services.configurations');
      expect(args[0]).to.be.eql({
        name: 'common.across.services.configurations',
        sender: controller,
        data: {
          data: '[' + JSON.stringify({
            Clusters: {
              desired_config: [
                {
                  type: 't1',
                  service_config_version_note: Em.I18n.t('admin.kerberos.wizard.configuration.note')
                }
              ]
            }
          }).toString() + ']'
        }
      });
    });
  });

  describe("#createKerberosSiteObj()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'tweakKdcTypeValue');
      sinon.stub(controller, 'tweakManualKdcProperties');
      sinon.stub(controller, 'tweakIpaKdcProperties');
      sinon.stub(App.config, 'trimProperty', function(arg) {
        return arg;
      });
      controller.set('stepConfigs', [Em.Object.create({
        configs: []
      })]);
    });

    afterEach(function() {
      controller.tweakKdcTypeValue.restore();
      controller.tweakManualKdcProperties.restore();
      controller.tweakIpaKdcProperties.restore();
      App.config.trimProperty.restore();
    });

    it("tweakKdcTypeValue should be called", function() {
      controller.createKerberosSiteObj();
      expect(controller.tweakKdcTypeValue.calledWith({})).to.be.true;
    });

    it("tweakManualKdcProperties should be called", function() {
      controller.createKerberosSiteObj();
      expect(controller.tweakManualKdcProperties.calledWith({})).to.be.true;
    });

    it("tweakIpaKdcProperties should be called", function() {
      controller.createKerberosSiteObj();
      expect(controller.tweakIpaKdcProperties.calledWith({})).to.be.true;
    });

    it("properties should be empty", function() {
      controller.set('stepConfigs', [Em.Object.create({
        configs: [{
          isRequiredByAgent: false,
          filename: 'site.xml'
        }]
      })]);
      expect(controller.createKerberosSiteObj('site')).to.be.eql({
        "type": 'site',
        "properties": {}
      });
    });

    it("properties should contain kdc_hosts", function() {
      controller.set('stepConfigs', [Em.Object.create({
        configs: [{
          name: 'kdc_hosts',
          value: 'v1',
          filename: 'site.xml'
        }]
      })]);
      expect(controller.createKerberosSiteObj('site')).to.be.eql({
        "type": 'site',
        "properties": {
          'kdc_hosts': {
            displayType: 'host',
            value: 'v1'
          }
        }
      });
    });

    it("properties should contain n1", function() {
      controller.set('stepConfigs', [Em.Object.create({
        configs: [{
          name: 'n1',
          value: 'v1',
          filename: 'site.xml'
        }]
      })]);
      expect(controller.createKerberosSiteObj('site')).to.be.eql({
        "type": 'site',
        "properties": {
          'n1': {
            name: 'n1',
            value: 'v1',
            filename: 'site.xml'
          }
        }
      });
    });
  });

  describe("#tweakKdcTypeValue()", function () {

    beforeEach(function() {
      sinon.stub(App.router, 'get').returns({
        'k1': 'p1'
      });
    });

    afterEach(function() {
      App.router.get.restore();
    });

    it("kdc_type should be p2", function() {
      var properties = {'kdc_type': 'p2'};
      controller.tweakKdcTypeValue(properties);
      expect(properties.kdc_type).to.be.equal('p2')
    });

    it("kdc_type should be k1", function() {
      var properties = {'kdc_type': 'p1'};
      controller.tweakKdcTypeValue(properties);
      expect(properties.kdc_type).to.be.equal('k1')
    });
  });

  describe("#tweakManualKdcProperties()", function () {

    it("properties shouldn't be changed", function() {
      var properties = {
        'kdc_type': 'p1'
      };
      controller.set('controllers.kerberosWizardController', Em.Object.create({
        skipClientInstall: false
      }));
      controller.tweakManualKdcProperties(properties);
      expect(properties).to.be.eql({
        'kdc_type': 'p1'
      });
    });

    it("kdc_type is none", function() {
      var properties = {
        'kdc_type': 'none',
        'manage_identities': 'true'
      };
      controller.set('controllers.kerberosWizardController', Em.Object.create({
        skipClientInstall: false
      }));
      controller.tweakManualKdcProperties(properties);
      expect(properties).to.be.eql({
        'kdc_type': 'none',
        'manage_identities': 'false'
      });
    });

    it("skipClientInstall is true", function() {
      var properties = {
        'kdc_type': 'p1',
        'manage_identities': 'true',
        'install_packages': 'true',
        'manage_krb5_conf': 'true'
      };
      controller.set('controllers.kerberosWizardController', Em.Object.create({
        skipClientInstall: true
      }));
      controller.tweakManualKdcProperties(properties);
      expect(properties).to.be.eql({
        'kdc_type': 'p1',
        'manage_identities': 'false',
        'install_packages': 'false',
        'manage_krb5_conf': 'false'
      });
    });
  });

  describe("#tweakIpaKdcProperties()", function () {

    beforeEach(function() {
      sinon.stub(App.router, 'get').returns({'ipa': 'p1'});
    });

    afterEach(function() {
      App.router.get.restore();
    });

    it("properties should be empty, kdc_type undefined", function() {
      var properties = {};
      controller.tweakIpaKdcProperties(properties);
      expect(properties).to.be.empty;
    });

    it("properties should not be empty", function() {
      var properties = {
        kdc_type: 'p1'
      };
      controller.set('content.kerberosOption', 'p2');
      controller.tweakIpaKdcProperties(properties);
      expect(properties).to.be.eql({
        kdc_type: 'p1'
      });
    });

    it("properties should set config values", function() {
      var properties = {
        'kdc_type': 'p1',
        'install_packages': 'true',
        'manage_krb5_conf': 'true'
      };
      controller.set('content.kerberosOption', 'p1');
      controller.tweakIpaKdcProperties(properties);
      expect(properties).to.be.eql({
        'kdc_type': 'p1',
        'install_packages': 'false',
        'manage_krb5_conf': 'false'
      });
    });
  });

  describe("#createKerberosAdminSession()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'createKDCCredentials');
    });

    afterEach(function() {
      controller.createKDCCredentials.restore();
    });

    it("createKDCCredentials should be called", function() {
      controller.set('wizardController.skipClientInstall', false);
      controller.createKerberosAdminSession([]);
      expect(controller.createKDCCredentials.calledWith([])).to.be.true;
    });

    it("createKDCCredentials should be called, with non-empty configs", function() {
      controller.set('stepConfigs', [Em.Object.create({configs: [{}]})]);
      controller.set('wizardController.skipClientInstall', false);
      controller.createKerberosAdminSession();
      expect(controller.createKDCCredentials.calledWith([{}])).to.be.true;
    });

    it("App.ajax.send should be called", function() {
      App.set('clusterName', 'c1');
      var configs = [
        {
          name: 'admin_principal',
          value: 'v1'
        },
        {
          name: 'admin_password',
          value: 'v2'
        }
      ];
      controller.set('wizardController.skipClientInstall', true);

      controller.createKerberosAdminSession(configs);
      var args = testHelpers.findAjaxRequest('name', 'common.cluster.update');
      expect(args[0]).to.be.eql({
        name: 'common.cluster.update',
        sender: controller,
        data: {
          clusterName: 'c1',
          data: [{
            session_attributes: {
              kerberos_admin: {principal: 'v1', password: 'v2'}
            }
          }]
        }
      });
    });
  });

  describe("#showConnectionInProgressPopup()", function () {

    beforeEach(function() {
      sinon.stub(App, 'showConfirmationPopup');
    });

    afterEach(function() {
      App.showConfirmationPopup.restore();
    });

    it("App.showConfirmationPopup should be called", function() {
      var primary = Em.K;
      controller.showConnectionInProgressPopup(primary);
      expect(App.showConfirmationPopup.calledWith(primary, Em.I18n.t('services.service.config.connection.exitPopup.msg'), null, null, Em.I18n.t('common.exitAnyway'))).to.be.true;
    });
  });


  describe("#setKDCTypeProperty()", function () {

    beforeEach(function() {
      sinon.stub(App.router, 'get').returns({
        'k1': 'p1'
      });
    });

    afterEach(function() {
      App.router.get.restore();
    });

    it("kdcTypeProperty should be set", function() {
      var configs = [{
        filename: 'kerberos-env.xml',
        name: 'kdc_type'
      }];
      controller.set('content.kerberosOption', 'p1');
      controller.setKDCTypeProperty(configs);
      expect(configs[0].value).to.be.equal('p1');
    });
  });
});
