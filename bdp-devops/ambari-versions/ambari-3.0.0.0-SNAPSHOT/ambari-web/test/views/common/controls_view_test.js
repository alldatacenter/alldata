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
require('views/common/controls_view');
var validator = require('utils/validator');
var testHelpers = require('test/helpers');

describe('App.ServiceConfigRadioButtons', function () {

  var view;

  beforeEach(function () {
    view = App.ServiceConfigRadioButtons.create();
  });

  describe('#handleDBConnectionProperty', function () {

    var cases = [
        {
          dbType: 'mysql',
          driver: 'mysql-connector-java.jar',
          dbName: 'MySQL',
          downloadUrl: 'https://dev.mysql.com/downloads/connector/j/',
          driverName: 'MySQL Connector/J JDBC Driver',
          serviceConfig: {
            name: 'hive_database',
            value: 'New MySQL Database',
            serviceName: 'HIVE'
          },
          controller: Em.Object.create({
            selectedService: {
              configs: [
                Em.Object.create({
                  name: 'javax.jdo.option.ConnectionURL',
                  displayName: 'Database URL'
                }),
                Em.Object.create({
                  name: 'hive_database',
                  displayName: 'Hive Database'
                })
              ]
            }
          }),
          currentStackVersion: 'HDP-2.2',
          rangerVersion: '0.4.0',
          propertyAppendTo1: 'javax.jdo.option.ConnectionURL',
          propertyAppendTo2: 'hive_database',
          isAdditionalView1Null: true,
          isAdditionalView2Null: false,
          title: 'Hive, embedded database'
        },
        {
          dbType: 'postgres',
          driver: 'postgresql.jar',
          dbName: 'PostgreSQL',
          downloadUrl: 'https://jdbc.postgresql.org/',
          driverName: 'PostgreSQL JDBC Driver',
          serviceConfig: {
            name: 'hive_database',
            value: 'Existing PostgreSQL Database',
            serviceName: 'HIVE'
          },
          controller: Em.Object.create({
            selectedService: {
              configs: [
                Em.Object.create({
                  name: 'javax.jdo.option.ConnectionURL',
                  displayName: 'Database URL'
                }),
                Em.Object.create({
                  name: 'hive_database',
                  displayName: 'Hive Database'
                })
              ]
            }
          }),
          currentStackVersion: 'HDP-2.2',
          rangerVersion: '0.4.0',
          propertyAppendTo1: 'javax.jdo.option.ConnectionURL',
          propertyAppendTo2: 'hive_database',
          isAdditionalView1Null: false,
          isAdditionalView2Null: false,
          title: 'Hive, external database'
        },
        {
          dbType: 'derby',
          driver: 'driver.jar',
          dbName: 'Derby',
          downloadUrl: 'http://',
          driverName: 'Derby JDBC Driver',
          serviceConfig: {
            name: 'oozie_database',
            value: 'New Derby Database',
            serviceName: 'OOZIE'
          },
          controller: Em.Object.create({
            selectedService: {
              configs: [
                Em.Object.create({
                  name: 'oozie.service.JPAService.jdbc.url',
                  displayName: 'Database URL'
                }),
                Em.Object.create({
                  name: 'oozie_database',
                  displayName: 'Oozie Database'
                })
              ]
            }
          }),
          currentStackVersion: 'HDP-2.2',
          rangerVersion: '0.4.0',
          propertyAppendTo1: 'oozie.service.JPAService.jdbc.url',
          propertyAppendTo2: 'oozie_database',
          isAdditionalView1Null: true,
          isAdditionalView2Null: true,
          title: 'Oozie, embedded database'
        },
        {
          dbType: 'oracle',
          driver: 'ojdbc6.jar',
          dbName: 'Oracle',
          downloadUrl: 'http://www.oracle.com/technetwork/database/features/jdbc/index-091264.html',
          driverName: 'Oracle JDBC Driver',
          serviceConfig: {
            name: 'oozie_database',
            value: 'Existing Oracle Database',
            serviceName: 'OOZIE'
          },
          controller: Em.Object.create({
            selectedService: {
              configs: [
                Em.Object.create({
                  name: 'oozie.service.JPAService.jdbc.url',
                  displayName: 'Database URL'
                }),
                Em.Object.create({
                  name: 'oozie_database',
                  displayName: 'Oozie Database'
                })
              ]
            }
          }),
          currentStackVersion: 'HDP-2.2',
          rangerVersion: '0.4.0',
          propertyAppendTo1: 'oozie.service.JPAService.jdbc.url',
          propertyAppendTo2: 'oozie_database',
          isAdditionalView1Null: false,
          isAdditionalView2Null: false,
          title: 'Oozie, external database'
        },
        {
          dbType: 'mysql',
          driver: 'mysql-connector-java.jar',
          dbName: 'MySQL',
          downloadUrl: 'https://dev.mysql.com/downloads/connector/j/',
          driverName: 'MySQL Connector/J JDBC Driver',
          serviceConfig: {
            name: 'DB_FLAVOR',
            value: 'MYSQL',
            serviceName: 'RANGER'
          },
          controller: Em.Object.create({
            selectedService: {
              configs: [
                Em.Object.create({
                  name: 'ranger.jpa.jdbc.url'
                }),
                Em.Object.create({
                  name: 'DB_FLAVOR'
                })
              ]
            }
          }),
          currentStackVersion: 'HDP-2.2',
          rangerVersion: '0.4.0',
          propertyAppendTo1: 'ranger.jpa.jdbc.url',
          propertyAppendTo2: 'DB_FLAVOR',
          isAdditionalView1Null: true,
          isAdditionalView2Null: true,
          title: 'Ranger, HDP 2.2, external database'
        }
      ];
    var rangerVersion = '';

    beforeEach(function () {
      sinon.stub(view, 'sendRequestRorDependentConfigs', Em.K);
      sinon.stub(Em.run, 'next', function (arg1, arg2) {
        if (typeof arg1 === 'function') {
          arg1();
        } else if (typeof arg1 === 'object' && typeof arg2 === 'function') {
          arg2();
        }
      });
      this.stub = sinon.stub(App, 'get');
      this.stub.withArgs('currentStackName').returns('HDP');
      sinon.stub(App.StackService, 'find', function() {
        return [Em.Object.create({
          serviceName: 'RANGER',
          serviceVersion: rangerVersion || ''
        })];
      });
    });

    afterEach(function () {
      Em.run.next.restore();
      App.get.restore();
      App.StackService.find.restore();
      view.sendRequestRorDependentConfigs.restore();
    });

    cases.forEach(function (item) {
      describe(item.title, function () {

        var additionalView1, additionalView2;
        beforeEach(function () {
          this.stub.withArgs('currentStackVersion').returns(item.currentStackVersion);
          rangerVersion = item.rangerVersion;
          view.reopen({controller: item.controller});
          view.setProperties({
            categoryConfigsAll: item.controller.get('selectedService.configs'),
            serviceConfig: item.serviceConfig
          });

          additionalView1 = view.get('categoryConfigsAll').findProperty('name', item.propertyAppendTo1).get('additionalView');
          additionalView2 = view.get('categoryConfigsAll').findProperty('name', item.propertyAppendTo2).get('additionalView');
        });

        it('additionalView1 is ' + (item.isAdditionalView1Null ? '' : 'not') + ' null', function () {
          expect(Em.isNone(additionalView1)).to.equal(item.isAdditionalView1Null);
        });

        it('additionalView2 is ' + (item.isAdditionalView2Null ? '' : 'not') + ' null', function () {
          expect(Em.isNone(additionalView2)).to.equal(item.isAdditionalView2Null);
        });

        if (!item.isAdditionalView2Null) {
          it('additionalView2.message is valid', function () {
            var message = Em.I18n.t('services.service.config.database.msg.jdbcSetup.detailed').format(item.dbName, item.dbType, item.driver, item.downloadUrl, item.driverName);
            expect(additionalView2.create().get('message')).to.equal(message);
          });
        }

      });
    });

  });

  describe('#options', function () {

    var options = [
        {
          displayName: 'MySQL'
        },
        {
          displayName: 'New PostgreSQL Database'
        },
        {
          displayName: 'existing postgres db'
        },
        {
          displayName: 'sqla database: existing'
        },
        {
          displayName: 'SQL Anywhere database (New)'
        },
        {
          displayName: 'displayName'
        }
      ],
      classNames = ['mysql', 'new-postgres', 'postgres', 'sqla', 'new-sqla', undefined];

    beforeEach(function () {
      view.reopen({
        serviceConfig: Em.Object.create({
          options: options
        })
      });
    });

    it('should set class names for options', function () {
      expect(view.get('options').mapProperty('displayName')).to.eql(options.mapProperty('displayName'));
      expect(view.get('options').mapProperty('className')).to.eql(classNames);
    });

  });

  describe('#name', function () {

    var cases = [
      {
        serviceConfig: {
          radioName: 'n0',
          isOriginalSCP: true,
          isComparison: false
        },
        name: 'n0',
        title: 'original value'
      },
      {
        serviceConfig: {
          radioName: 'n1',
          isOriginalSCP: false,
          isComparison: true,
          compareConfigs: []
        },
        controller: {
          selectedVersion: 1
        },
        name: 'n1-v1',
        title: 'comparison view, original value'
      },
      {
        serviceConfig: {
          radioName: 'n2',
          isOriginalSCP: false,
          isComparison: true,
          compareConfigs: null
        },
        version: 2,
        name: 'n2-v2',
        title: 'comparison view, value to be compared with'
      },
      {
        serviceConfig: {
          radioName: 'n3',
          isOriginalSCP: false,
          isComparison: false,
          group: {
            name: 'g'
          }
        },
        name: 'n3-g',
        title: 'override value'
      }
    ];

    beforeEach(function () {
      view.reopen({
        serviceConfig: Em.Object.create()
      });
    });

    cases.forEach(function (item) {
      it(item.title, function () {
        if (item.controller) {
          view.reopen({
            controller: item.controller
          });
        }
        view.set('version', item.version);
        view.get('serviceConfig').setProperties(item.serviceConfig);
        expect(view.get('name')).to.equal(item.name);
      });
    });

  });

  describe('#dontUseHandleDbConnection', function () {
    var rangerService = Em.Object.create({
      serviceName: 'RANGER'
    });
    beforeEach(function () {
      sinon.stub(App.StackService, 'find', function () {
        return [rangerService];
      });
    });

    afterEach(function () {
      App.StackService.find.restore();
    });

    var cases = [
      {
        title: 'Should return properties for old version of Ranger',
        version: '0.1',
        result: ['DB_FLAVOR', 'authentication_method']
      },
      {
        title: 'Should return properties for old version of Ranger',
        version: '0.4.0',
        result: ['DB_FLAVOR', 'authentication_method']
      },
      {
        title: 'Should return properties for old version of Ranger',
        version: '0.4.9',
        result: ['DB_FLAVOR', 'authentication_method']
      },
      {
        title: 'Should return properties for new version of Ranger',
        version: '0.5.0',
        result: ['ranger.authentication.method']
      },
      {
        title: 'Should return properties for new version of Ranger',
        version: '1.0.0',
        result: ['ranger.authentication.method']
      },
      {
        title: 'Should return properties for new version of Ranger',
        version: '0.5.0.1',
        result: ['ranger.authentication.method']
      }
    ];

    cases.forEach(function (test) {
      it(test.title, function () {
        rangerService.set('serviceVersion', test.version);
        expect(view.get('dontUseHandleDbConnection')).to.eql(test.result);
      });
    });
  });

});

describe('App.ServiceConfigRadioButton', function () {

  var view;

  beforeEach(function () {
    view = App.ServiceConfigRadioButton.create({
      parentView: Em.Object.create({
        serviceConfig: Em.Object.create()
      }),
      controller: Em.Object.create({
        wizardController: Em.Object.create({
          name: null
        })
      })
    })
  });

  describe('#disabled', function () {

    var cases = [
      {
        wizardControllerName: 'addServiceController',
        value: 'New MySQL Database',
        title: 'Add Service Wizard, new database',
        disabled: false
      },
      {
        wizardControllerName: 'installerController',
        value: 'New MySQL Database',
        title: 'Install Wizard, new database',
        disabled: false
      },
      {
        wizardControllerName: 'addServiceController',
        value: 'Existing MySQL Database',
        title: 'Add Service Wizard, existing database',
        disabled: false
      },
      {
        wizardControllerName: 'installerController',
        value: 'Existing MySQL Database',
        title: 'Install Wizard, existing database',
        disabled: false
      },
      {
        wizardControllerName: null,
        value: 'New MySQL Database',
        title: 'No installer, new database',
        disabled: true
      },
      {
        wizardControllerName: null,
        value: 'Existing MySQL Database',
        title: 'No installer, existing database',
        disabled: false
      }
    ];

    cases.forEach(function (item) {
      it(item.title, function () {
        view.setProperties({
          'value': item.value,
          'controller.wizardController.name': item.wizardControllerName,
          'parentView.serviceConfig.isEditable': true
        });
        expect(view.get('disabled')).to.equal(item.disabled);
      });
    });

    it('parent view is disabled', function () {
      view.set('parentView.serviceConfig.isEditable', false);
      expect(view.get('disabled')).to.be.true;
    });

  });

  describe('#onChecked', function () {

    var cases = [
      {
        clicked: true,
        value: 'v1',
        sendRequestRorDependentConfigsCallCount: 1,
        updateForeignKeysCallCount: 1,
        title: 'invoked with click'
      },
      {
        clicked: false,
        value: 'v0',
        sendRequestRorDependentConfigsCallCount: 0,
        updateForeignKeysCallCount: 0,
        title: 'not invoked with click'
      }
    ];

    cases.forEach(function (item) {

      describe(item.title, function () {

        beforeEach(function () {
          sinon.stub(Em.run, 'next', function (context, callback) {
            callback.call(context);
          });
          sinon.stub(view, 'sendRequestRorDependentConfigs', Em.K);
          sinon.stub(view, 'updateForeignKeys', Em.K);
          sinon.stub(view, 'updateCheck', Em.K);
          view.setProperties({
            'clicked': item.clicked,
            'parentView.serviceConfig.value': 'v0',
            'value': 'v1'
          });
          view.propertyDidChange('checked');
        });

        afterEach(function () {
          Em.run.next.restore();
          view.sendRequestRorDependentConfigs.restore();
          view.updateForeignKeys.restore();
          view.updateCheck.restore();
        });

        it('property value', function () {
          expect(view.get('parentView.serviceConfig.value')).to.equal(item.value);
        });

        it('dependent configs request', function () {
          expect(view.sendRequestRorDependentConfigs.callCount).to.equal(item.sendRequestRorDependentConfigsCallCount);
        });

        if (item.sendRequestRorDependentConfigsCallCount) {
          it('config object for dependent configs request', function () {
            expect(view.sendRequestRorDependentConfigs.firstCall.args).to.eql([
              Em.Object.create({
                value: item.value
              })
            ]);
          });
        }

        it('clicked flag reset', function () {
          expect(view.get('clicked')).to.be.false;
        });

        it('update foreign keys', function () {
          expect(view.updateForeignKeys.callCount).to.equal(item.updateForeignKeysCallCount);
        });

      });

    });

  });

});

describe('App.CheckDBConnectionView', function () {

  describe('#masterHostName', function () {

    var cases = [
        {
          serviceName: 'OOZIE',
          value: 'h0'
        },
        {
          serviceName: 'KERBEROS',
          value: 'h1'
        },
        {
          serviceName: 'HIVE',
          value: 'h2'
        },
        {
          serviceName: 'RANGER',
          value: 'h3'
        }
      ],
      categoryConfigsAll = [
        Em.Object.create({
          name: 'oozie_server_hosts',
          value: 'h0'
        }),
        Em.Object.create({
          name: 'kdc_hosts',
          value: 'h1'
        }),
        Em.Object.create({
          name: 'hive_metastore_hosts',
          value: 'h2'
        }),
        Em.Object.create({
          name: 'ranger_server_hosts',
          value: 'h3'
        })
      ];

    cases.forEach(function (item) {
      it(item.serviceName, function () {
        var view = App.CheckDBConnectionView.create({
          parentView: {
            service: {
              serviceName: item.serviceName
            },
            categoryConfigsAll: categoryConfigsAll
          }
        });
        expect(view.get('masterHostName')).to.equal(item.value);
      });
    });
  });

  describe('#setResponseStatus', function () {

    var view,
      cases = [
        {
          isSuccess: 'success',
          logsPopupBefore: null,
          logsPopup: null,
          responseCaption: Em.I18n.t('services.service.config.database.connection.success'),
          isConnectionSuccess: true,
          title: 'success, no popup displayed'
        },
        {
          isSuccess: 'success',
          logsPopupBefore: {},
          logsPopup: {
            header: Em.I18n.t('services.service.config.connection.logsPopup.header').format('MySQL', Em.I18n.t('common.success'))
          },
          responseCaption: Em.I18n.t('services.service.config.database.connection.success'),
          isConnectionSuccess: true,
          title: 'success, popup is displayed'
        },
        {
          isSuccess: 'error',
          logsPopupBefore: {},
          logsPopup: {
            header: Em.I18n.t('services.service.config.connection.logsPopup.header').format('MySQL', Em.I18n.t('common.error'))
          },
          responseCaption: Em.I18n.t('services.service.config.database.connection.failed'),
          isConnectionSuccess: false,
          title: 'error, popup is displayed'
        }
      ];

    beforeEach(function () {
      view = App.CheckDBConnectionView.create({
        databaseName: 'MySQL'
      });
      sinon.stub(view, 'setConnectingStatus', Em.K);
    });

    afterEach(function () {
      view.setConnectingStatus.restore();
    });

    cases.forEach(function (item) {

      describe(item.title, function () {

        beforeEach(function () {
          view.set('logsPopup', item.logsPopupBefore);
          view.setResponseStatus(item.isSuccess);
        });

        it('isRequestResolved is true', function () {
          expect(view.get('isRequestResolved')).to.be.true;
        });

        it('setConnectingStatus is called with valid arguments', function () {
          expect(view.setConnectingStatus.calledOnce).to.be.true;
          expect(view.setConnectingStatus.calledWith(false)).to.be.true;
        });

        it('responseCaption is valid', function () {
          expect(view.get('responseCaption')).to.equal(item.responseCaption);
        });

        it('isConnectionSuccess is valid', function () {
          expect(view.get('isConnectionSuccess')).to.equal(item.isConnectionSuccess);
        });

        it('logsPopup is valid', function () {
          expect(view.get('logsPopup')).to.eql(item.logsPopup);
        });

      });
    });

  });

  describe('#showLogsPopup', function () {

    var view;

    beforeEach(function () {
      view = App.CheckDBConnectionView.create({
        databaseName: 'MySQL'
      });
      sinon.spy(App, 'showAlertPopup');
    });

    afterEach(function () {
      App.showAlertPopup.restore();
    });

    it('successful connection', function () {
      view.set('isConnectionSuccess', true);
      view.showLogsPopup();
      expect(App.showAlertPopup.callCount).to.equal(0);
    });

    describe('failed connection without output data, popup dismissed with Close button', function () {

      beforeEach(function () {
        view.set('isConnectionSuccess', false);
        view.set('isRequestResolved', true);
        view.set('responseFromServer', 'fail');
        view.showLogsPopup();
      });

      it('showAlertPopup is called once', function () {
        expect(App.showAlertPopup.callCount).to.equal(1);
      });
      it('logsPopup.header is valid', function () {
        expect(view.get('logsPopup.header')).to.equal(Em.I18n.t('services.service.config.connection.logsPopup.header').format('MySQL', Em.I18n.t('common.error')));
      });
      it('logsPopup.body is valid', function () {
        expect(view.get('logsPopup.body')).to.equal('fail');
      });
      it('logsPopup is null after close', function () {
        view.get('logsPopup').onClose();
        expect(view.get('logsPopup')).to.be.null;
      });
    });

    describe('check in progress with output data, popup dismissed with OK button', function () {
      var response = {
        stderr: 'stderr',
        stdout: 'stdout',
        structuredOut: 'structuredOut'
      };
      beforeEach(function () {
        view.set('isConnectionSuccess', false);
        view.set('isRequestResolved', false);
        view.set('responseFromServer', response);
        view.showLogsPopup();
      });

      it('showAlertPopup is called once', function () {
        expect(App.showAlertPopup.callCount).to.equal(1);
      });
      it('logsPopup.header is valid', function () {
        expect(view.get('logsPopup.header')).to.equal(Em.I18n.t('services.service.config.connection.logsPopup.header').format('MySQL', Em.I18n.t('common.testing')));
      });
      it('logsPopup.bodyClass is valid', function () {
        expect(view.get('logsPopup.bodyClass').create().get('openedTask')).to.eql(response);
      });
      it('logsPopup is null after primary click', function () {
        view.get('logsPopup').onPrimary();
        expect(view.get('logsPopup')).to.be.null;
      });
    });

  });

  describe("#createCustomAction()", function() {
    var view;
    beforeEach(function () {
      view = App.CheckDBConnectionView.create({
        databaseName: 'MySQL',
        getConnectionProperty: Em.K,
        masterHostName: 'host1'
      });
      this.mock = sinon.stub(App.Service, 'find');
    });
    afterEach(function () {
      this.mock.restore();
    });

    it("service not installed", function() {
      this.mock.returns(Em.Object.create({isLoaded: false}));
      view.createCustomAction();
      var args = testHelpers.findAjaxRequest('name', 'custom_action.create');
      expect(args[0]).exists;
    });
    it("service is installed", function() {
      this.mock.returns(Em.Object.create({isLoaded: true}));
      view.createCustomAction();
      var args = testHelpers.findAjaxRequest('name', 'cluster.custom_action.create');
      expect(args[0]).exists;
    });
  });

  describe('#requriedProperties', function() {
    var cases;
    beforeEach(function() {
      this.stackServiceStub = sinon.stub(App.StackService, 'find');
    });
    afterEach(function() {
      this.stackServiceStub.restore();
    });

    cases = [
      {
        stackServices: [
          {name: 'OOZIE', version: '1.0.0'}
        ],
        parentViewServiceName: 'OOZIE',
        e: ['oozie.db.schema.name', 'oozie.service.JPAService.jdbc.username', 'oozie.service.JPAService.jdbc.password', 'oozie.service.JPAService.jdbc.driver', 'oozie.service.JPAService.jdbc.url'],
        m: 'should return Oozie specific properties'
      },
      {
        stackServices: [
          {name: 'HIVE', version: '1.0.0'}
        ],
        parentViewServiceName: 'HIVE',
        e: ['ambari.hive.db.schema.name', 'javax.jdo.option.ConnectionUserName', 'javax.jdo.option.ConnectionPassword', 'javax.jdo.option.ConnectionDriverName', 'javax.jdo.option.ConnectionURL'],
        m: 'should return Hive specific properties'
      },
      {
        stackServices: [
          {name: 'KERBEROS', version: '1.0.0'}
        ],
        parentViewServiceName: 'KERBEROS',
        e: ['kdc_hosts'],
        m: 'should return specific Kerberos specific properties'
      },
      {
        stackServices: [
          {name: 'RANGER', version: '0.4.9'}
        ],
        parentViewServiceName: 'RANGER',
        e: ['db_user', 'db_password', 'db_name', 'ranger_jdbc_connection_url', 'ranger_jdbc_driver'],
        m: 'should return specific properties for Ranger when its version < 0.5'
      },
      {
        stackServices: [
          {name: 'RANGER', version: '1.0.0'}
        ],
        parentViewServiceName: 'RANGER',
        e: ['db_user', 'db_password', 'db_name', 'ranger.jpa.jdbc.url', 'ranger.jpa.jdbc.driver'],
        m: 'should return specific properties for Ranger when its version > 0.5'
      }
    ];

    cases.forEach(function(test) {
      it(test.m, function() {
        this.stackServiceStub.returns(test.stackServices.map(function(service) {
          return Em.Object.create({
            serviceName: service.name,
            serviceVersion: service.version,
            compareCurrentVersion: App.StackService.proto().compareCurrentVersion
          });
        }));
        var view = App.CheckDBConnectionView.create({
          parentView: {
            service: {
              serviceName: test.parentViewServiceName
            }
          }
        });
        expect(view.get('requiredProperties')).to.be.eql(test.e);
      });
    });
  });
});

describe('App.BaseUrlTextField', function () {

  var view = App.BaseUrlTextField.create({
    repository: Em.Object.create({
      baseUrl: 'val'
    }),
    parentView: Em.Object.create({
      uiValidation: Em.K
    })
  });

  describe('#valueWasChanged', function () {
    it('should be recalculated after value is changed', function () {
      view.setProperties({
        value: 'val',
        defaultValue: 'val'
      });
      expect(view.get('valueWasChanged')).to.be.false;
      view.set('value', 'newVal');
      expect(view.get('valueWasChanged')).to.be.true;
    });
  });

  describe('#restoreValue()', function () {
    it('should unset value', function () {
      view.setProperties({
        value: 'valNew',
        defaultValue: 'val'
      });
      view.restoreValue();
      expect(view.get('value')).to.equal('val');
    });
  });

  describe('#didInsertElement()', function () {
    it('should set defaultValue', function () {
      view.setProperties({
        value: 'valNew',
        defaultValue: 'val'
      });
      view.didInsertElement();
      expect(view.get('defaultValue')).to.equal('valNew');
    });
  });

  describe('#validate()', function () {
    beforeEach(function(){
      sinon.stub(view.get('parentView'), 'uiValidation', Em.K);
      sinon.stub(validator, 'isValidBaseUrl').returns(true);
    });
    afterEach(function(){
      view.get('parentView').uiValidation.restore();
      validator.isValidBaseUrl.restore();
    });
    it('skip validation', function () {
      view.set('repository', Em.Object.create({
        skipValidation: true
      }));
      expect(view.get('repository.hasError')).to.be.false;
    });
    it('apply validation', function () {
      view.set('repository', Em.Object.create({
          skipValidation: false
      }));
      expect(view.get('repository.hasError')).to.be.false;
      expect(validator.isValidBaseUrl.calledOnce).to.be.true;
    });
  });
});

describe('App.ServiceConfigComponentHostsView', function () {

  function getView (value, serviceConfig) {
    return App.ServiceConfigComponentHostsView.create({
      value: value,
      serviceConfig: serviceConfig
    });
  }

  App.TestAliases.testAsComputedFirstNotBlank(getView(), 'firstHost', ['value.firstObject', 'serviceConfig.value.firstObject']);

  App.TestAliases.testAsComputedIfThenElseByKeys(getView(), 'formatValue', 'hasOneHost', 'value.firstObject', 'value');

  App.TestAliases.testAsComputedEqual(getView(), 'hasOneHost', 'value.length', 1);

  App.TestAliases.testAsComputedGt(getView(), 'hasMultipleHosts', 'value.length', 1);

})