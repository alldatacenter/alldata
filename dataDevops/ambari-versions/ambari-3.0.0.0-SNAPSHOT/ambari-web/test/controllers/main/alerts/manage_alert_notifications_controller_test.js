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
var controller;
var helpers = require('test/helpers');
require('templates/main/alerts/alert_instance/status');


function getController() {
  return App.ManageAlertNotificationsController.create({});
}

function getInputFields() {
  return Em.Object.create({
    name: {
      value: ''
    },
    groups: {
      value: []
    },
    global: {
      value: false
    },
    allGroups: {
      value: false
    },
    method: {
      value: ''
    },
    email: {
      value: ''
    },
    severityFilter: {
      value: []
    },
    description: {
      value: ''
    },
    SMTPServer: {
      value: ''
    },
    SMTPPort: {
      value: ''
    },
    SMTPUseAuthentication: {
      value: ''
    },
    SMTPUsername: {
      value: ''
    },
    SMTPPassword: {
      value: ''
    },
    retypeSMTPPassword: {
      value: ''
    },
    SMTPSTARTTLS: {
      value: ''
    },
    emailFrom: {
      value: ''
    },
    version: {
      value: ''
    },
    OIDs: {
      value: ''
    },
    community: {
      value: ''
    },
    host: {
      value: ''
    },
    port: {
      value: ''
    },
    scriptDispatchProperty:{
      value: ''
    },
    scriptFileName:{
      value: ''
    }
  });
}

var createEditPopupView = getController().showCreateEditPopup();

describe('App.ManageAlertNotificationsController', function () {

  beforeEach(function () {
    controller = getController();
  });

  describe('#alertNotifications', function () {

    beforeEach(function () {
      sinon.stub(App.AlertNotification, 'find', function () {
        return [1, 2, 3];
      });
    });

    afterEach(function () {
      App.AlertNotification.find.restore();
    });

    it("should return all alert notifications if controller isLoaded", function () {

      controller.set('isLoaded', true);
      expect(controller.get('alertNotifications')).to.eql([1, 2, 3]);
    });

    it("should return [] if controller isLoaded=false", function () {

      controller.set('isLoaded', false);
      expect(controller.get('alertNotifications')).to.eql([]);
    });

  });

  describe('#loadAlertNotifications()', function () {

    it("should send ajax request and set isLoaded to false", function () {

      controller.set('isLoaded', true);
      controller.loadAlertNotifications();
      expect(controller.get('isLoaded')).to.be.false;
    });

  });

  describe('#getAlertNotificationsSuccessCallback()', function () {

    beforeEach(function () {
      sinon.spy(App.alertNotificationMapper, 'map');
    });

    afterEach(function () {
      App.alertNotificationMapper.map.restore();
    });

    it("should call mapper and set isLoaded to true", function () {

      controller.set('isLoaded', false);
      controller.getAlertNotificationsSuccessCallback('test');
      expect(controller.get('isLoaded')).to.be.true;
      expect(App.alertNotificationMapper.map.calledWith('test')).to.be.true;
    });

  });

  describe('#getAlertNotificationsErrorCallback()', function () {

    it("should set isLoaded to true", function () {

      controller.set('isLoaded', false);
      controller.getAlertNotificationsSuccessCallback('test');
      expect(controller.get('isLoaded')).to.be.true;
    });

  });

  describe('#addAlertNotification()', function () {

    var inputFields = Em.Object.create({
      a: {
        value: '',
        defaultValue: 'a'
      },
      b: {
        value: '',
        defaultValue: 'b'
      },
      c: {
        value: '',
        defaultValue: 'c'
      },
      severityFilter: {
        value: [],
        defaultValue: ['OK', 'WARNING', 'CRITICAL', 'UNKNOWN']
      },
      global: {
        value: false
      },
      allGroups: Em.Object.create({
        value: 'custom'
      })
    });

    beforeEach(function () {
      sinon.stub(controller, 'showCreateEditPopup');
      controller.set('inputFields', inputFields);
      controller.addAlertNotification();
    });

    afterEach(function () {
      controller.showCreateEditPopup.restore();
    });

    Object.keys(inputFields).forEach(function (key) {
      it(key, function () {
        expect(controller.get('inputFields.' + key + '.value')).to.be.eql(controller.get('inputFields.' + key + '.defaultValue'));
      });
    });

    it("should reset custom properties",function(){
       expect(controller.get('inputFields.customProperties')).to.be.eql(Em.A([]));
    });

    it("should call showCreateEditPopup", function () {
      expect(controller.showCreateEditPopup.calledOnce).to.be.true;
    });

  });

  describe('#editAlertNotification()', function () {

    beforeEach(function () {
      sinon.stub(controller, 'showCreateEditPopup', Em.K);
      sinon.stub(controller, 'fillEditCreateInputs', Em.K);
    });

    afterEach(function () {
      controller.showCreateEditPopup.restore();
      controller.fillEditCreateInputs.restore();
    });

    it("should call fillEditCreateInputs and showCreateEditPopup", function () {

      controller.editAlertNotification();

      expect(controller.fillEditCreateInputs.calledOnce).to.be.true;
      expect(controller.showCreateEditPopup.calledWith(true)).to.be.true;
    });

  });

  describe('#fillEditCreateInputs()', function () {

    it("should map properties from selectedAlertNotification to inputFields (ambari.dispatch.recipients ignored) - EMAIL", function () {

      controller.set('selectedAlertNotification', Em.Object.create({
        name: 'test_name',
        global: true,
        description: 'test_description',
        groups: ['test1', 'test2'],
        type: 'EMAIL',
        alertStates: ['OK', 'UNKNOWN'],
        properties: {
          'ambari.dispatch.recipients': [
            'test1@test.test',
            'test2@test.test'
          ],
          'customName': 'customValue',
          "mail.smtp.from" : "from",
          "ambari.dispatch.credential.username" : "user",
          "mail.smtp.host" : "s1",
          "mail.smtp.port" : "25",
          "mail.smtp.auth" : "true",
          "ambari.dispatch.credential.password" : "pass",
          "mail.smtp.starttls.enable" : "true"
        }
      }));

      controller.set('inputFields', Em.Object.create({
        name: {
          value: ''
        },
        groups: {
          value: []
        },
        global: {
          value: false
        },
        allGroups: {
          value: false
        },
        method: {
          value: ''
        },
        email: {
          value: ''
        },
        severityFilter: {
          value: []
        },
        description: {
          value: ''
        },
        SMTPServer: {
          value: ''
        },
        SMTPPort: {
          value: ''
        },
        SMTPUseAuthentication: {
          value: ''
        },
        SMTPUsername: {
          value: ''
        },
        SMTPPassword: {
          value: ''
        },
        retypeSMTPPassword: {
          value: ''
        },
        SMTPSTARTTLS: {
          value: ''
        },
        emailFrom: {
          value: ''
        },
        version: {
          value: ''
        },
        OIDs: {
          value: ''
        },
        community: {
          value: ''
        },
        host: {
          value: ''
        },
        port: {
          value: ''
        },
        scriptDispatchProperty:{
          value: ''
        },
        scriptFileName:{
          value: ''
        },
        customProperties: [
          {name: 'customName', value: 'customValue1', defaultValue: 'customValue1'},
          {name: 'customName2', value: 'customValue1', defaultValue: 'customValue1'}
        ]
      }));

      controller.fillEditCreateInputs();

      expect(JSON.stringify(controller.get('inputFields'))).to.equal(JSON.stringify({
        name: {
          value: 'test_name'
        },
        groups: {
          value: ['test1', 'test2']
        },
        global: {
          value: true,
          disabled: true
        },
        allGroups: {
          value: 'all'
        },
        method: {
          value: 'EMAIL'
        },
        email: {
          value: 'test1@test.test, test2@test.test'
        },
        severityFilter: {
          value: ['OK', 'UNKNOWN']
        },
        description: {
          value: 'test_description'
        },
        SMTPServer: {
          value: 's1'
        },
        SMTPPort: {
          value: '25'
        },
        SMTPUseAuthentication: {
          value: true
        },
        SMTPUsername: {
          value: 'user'
        },
        SMTPPassword: {
          value: 'pass'
        },
        retypeSMTPPassword: {
          value: 'pass'
        },
        SMTPSTARTTLS: {
          value: true
        },
        emailFrom: {
          value: 'from'
        },
        version: {},
        OIDs: {},
        community: {},
        host: {
          value: 'test1@test.test, test2@test.test'
        },
        port: {},
        scriptDispatchProperty:{
          value: ''
        },
        scriptFileName:{
          value: ''
        },
        customProperties: [
          {name: 'customName', value: 'customValue', defaultValue: 'customValue'}
        ]
      }));

    });

    it("should map properties from selectedAlertNotification to inputFields (ambari.dispatch.recipients ignored) - SNMP", function () {

      controller.set('selectedAlertNotification', Em.Object.create({
        name: 'test_SNMP_name',
        global: true,
        description: 'test_description',
        groups: ['test1', 'test2'],
        type: 'SNMP',
        alertStates: ['OK', 'UNKNOWN'],
        properties: {
          'ambari.dispatch.recipients': [
            'c6401.ambari.apache.org',
            'c6402.ambari.apache.org'
          ],
          'customName': 'customValue',
          'ambari.dispatch.snmp.version': 'SNMPv1',
          'ambari.dispatch.snmp.oids.trap': '1',
          'ambari.dispatch.snmp.community': 'snmp',
          'ambari.dispatch.snmp.port': 161

        }
      }));

      controller.set('inputFields', Em.Object.create({
        name: {
          value: ''
        },
        groups: {
          value: []
        },
        global: {
          value: false
        },
        allGroups: {
          value: false
        },
        method: {
          value: ''
        },
        email: {
          value: ''
        },
        severityFilter: {
          value: []
        },
        description: {
          value: ''
        },
        SMTPServer: {
          value: ''
        },
        SMTPPort: {
          value: ''
        },
        SMTPUseAuthentication: {
          value: ''
        },
        SMTPUsername: {
          value: ''
        },
        SMTPPassword: {
          value: ''
        },
        retypeSMTPPassword: {
          value: ''
        },
        SMTPSTARTTLS: {
          value: ''
        },
        emailFrom: {
          value: ''
        },
        version: {
          value: ''
        },
        OIDs: {
          value: ''
        },
        community: {
          value: ''
        },
        host: {
          value: ''
        },
        port: {
          value: ''
        },
        scriptDispatchProperty:{
          value: ''
        },
        scriptFileName:{
          value: ''
        },
        customProperties: [
          {name: 'customName', value: 'customValue1', defaultValue: 'customValue1'},
          {name: 'customName2', value: 'customValue1', defaultValue: 'customValue1'}
        ]
      }));

      controller.fillEditCreateInputs();

      expect(JSON.stringify(controller.get('inputFields'))).to.equal(JSON.stringify({
        name: {
          value: 'test_SNMP_name'
        },
        groups: {
          value: ['test1', 'test2']
        },
        global: {
          value: true,
          disabled: true
        },
        allGroups: {
          value: 'all'
        },
        method: {
          value: 'Custom SNMP'
        },
        email: {
          value: 'c6401.ambari.apache.org, c6402.ambari.apache.org'
        },
        severityFilter: {
          value: ['OK', 'UNKNOWN']
        },
        description: {
          value: 'test_description'
        },
        SMTPServer: {},
        SMTPPort: {},
        SMTPUseAuthentication: {
          value: true
        },
        SMTPUsername: {},
        SMTPPassword: {},
        retypeSMTPPassword: {},
        SMTPSTARTTLS: {
          value: true
        },
        emailFrom: {},
        version: {
          value:'SNMPv1'
        },
        OIDs: {
          value: '1'
        },
        community: {
          value: 'snmp'
        },
        host: {
          value: 'c6401.ambari.apache.org, c6402.ambari.apache.org'
        },
        port: {
          value: 161
        },
        scriptDispatchProperty:{
          value: ''
        },
        scriptFileName:{
          value: ''
        },
        customProperties: [
          {name: 'customName', value: 'customValue', defaultValue: 'customValue'}
        ]
      }));

    });

    it("should map properties from selectedAlertNotification to inputFields (ambari.dispatch.recipients ignored) - AMBARI_SNMP", function () {

      controller.set('selectedAlertNotification', Em.Object.create({
        name: 'AMBARI_SNMP_name',
        global: true,
        description: 'test_description',
        groups: ['test1', 'test2'],
        type: 'AMBARI_SNMP',
        alertStates: ['OK', 'UNKNOWN'],
        properties: {
          'ambari.dispatch.recipients': [
            'c6401.ambari.apache.org',
            'c6402.ambari.apache.org'
          ],
          'customName': 'customValue',
          'ambari.dispatch.snmp.version': 'SNMPv1',
          'ambari.dispatch.snmp.community': 'public',
          'ambari.dispatch.snmp.port': 161

        }
      }));

      controller.set('inputFields', Em.Object.create({
        name: {
          value: ''
        },
        groups: {
          value: []
        },
        global: {
          value: false
        },
        allGroups: {
          value: false
        },
        method: {
          value: ''
        },
        email: {
          value: ''
        },
        severityFilter: {
          value: []
        },
        description: {
          value: ''
        },
        SMTPServer: {
          value: ''
        },
        SMTPPort: {
          value: ''
        },
        SMTPUseAuthentication: {
          value: ''
        },
        SMTPUsername: {
          value: ''
        },
        SMTPPassword: {
          value: ''
        },
        retypeSMTPPassword: {
          value: ''
        },
        SMTPSTARTTLS: {
          value: ''
        },
        emailFrom: {
          value: ''
        },
        version: {
          value: ''
        },
        OIDs: {
          value: ''
        },
        community: {
          value: ''
        },
        host: {
          value: ''
        },
        port: {
          value: ''
        },
        scriptDispatchProperty:{
          value: ''
        },
        scriptFileName:{
          value: ''
        },
        customProperties: [
          {name: 'customName', value: 'customValue1', defaultValue: 'customValue1'},
          {name: 'customName2', value: 'customValue1', defaultValue: 'customValue1'}
        ]
      }));

      controller.fillEditCreateInputs();

      expect(JSON.stringify(controller.get('inputFields'))).to.equal(JSON.stringify({
        name: {
          value: 'AMBARI_SNMP_name'
        },
        groups: {
          value: ['test1', 'test2']
        },
        global: {
          value: true,
          disabled: true
        },
        allGroups: {
          value: 'all'
        },
        method: {
          value: 'SNMP'
        },
        email: {
          value: 'c6401.ambari.apache.org, c6402.ambari.apache.org'
        },
        severityFilter: {
          value: ['OK', 'UNKNOWN']
        },
        description: {
          value: 'test_description'
        },
        SMTPServer: {},
        SMTPPort: {},
        SMTPUseAuthentication: {
          value: true
        },
        SMTPUsername: {},
        SMTPPassword: {},
        retypeSMTPPassword: {},
        SMTPSTARTTLS: {
          value: true
        },
        emailFrom: {},
        version: {
          value:'SNMPv1'
        },
        OIDs: {},
        community: {
          value: 'public'
        },
        host: {
          value: 'c6401.ambari.apache.org, c6402.ambari.apache.org'
        },
        port: {
          value: 161
        },
        scriptDispatchProperty:{
          value: ''
        },
        scriptFileName:{
          value: ''
        },
        customProperties: [
          {name: 'customName', value: 'customValue', defaultValue: 'customValue'}
        ]
      }));

    });

    it("should map properties from selectedAlertNotification to inputFields - ALERT_SCRIPT", function () {

          controller.set('selectedAlertNotification', Em.Object.create({
            name: 'test_alert_script',
            global: true,
            description: 'test_description',
            groups: ['test1', 'test2'],
            type: 'ALERT_SCRIPT',
            alertStates: ['OK', 'UNKNOWN'],
            properties: {
              'ambari.dispatch-property.script': "com.mycompany.dispatch.syslog.script",
              'ambari.dispatch-property.script.filename': 'a.py',
              'customName': 'customValue'
            }
          }));

          controller.set('inputFields', Em.Object.create({
            name: {
              value: ''
            },
            groups: {
              value: []
            },
            global: {
              value: false
            },
            allGroups: {
              value: false
            },
            method: {
              value: ''
            },
            email: {
              value: ''
            },
            severityFilter: {
              value: []
            },
            description: {
              value: ''
            },
            SMTPServer: {
              value: ''
            },
            SMTPPort: {
              value: ''
            },
            SMTPUseAuthentication: {
              value: ''
            },
            SMTPUsername: {
              value: ''
            },
            SMTPPassword: {
              value: ''
            },
            retypeSMTPPassword: {
              value: ''
            },
            SMTPSTARTTLS: {
              value: ''
            },
            emailFrom: {
              value: ''
            },
            version: {
              value: ''
            },
            OIDs: {
              value: ''
            },
            community: {
              value: ''
            },
            host: {
              value: ''
            },
            port: {
              value: ''
            },
            scriptDispatchProperty: {
              value: ''
            },
            scriptFileName: {
              value: ''
            },
            customProperties: [
              {name: 'customName', value: 'customValue1', defaultValue: 'customValue1'},
              {name: 'customName2', value: 'customValue1', defaultValue: 'customValue1'}
            ]
          }));

          controller.fillEditCreateInputs();

          expect(JSON.stringify(controller.get('inputFields'))).to.equal(JSON.stringify({
            name: {
              value: 'test_alert_script'
            },
            groups: {
              value: ['test1', 'test2']
            },
            global: {
              value: true,
              disabled: true
            },
            allGroups: {
              value: 'all'
            },
            method: {
              value: 'Alert Script'
            },
            email: {
              value: ''
            },
            severityFilter: {
              value: ['OK', 'UNKNOWN']
            },
            description: {
              value: 'test_description'
            },
            SMTPServer: {},
            SMTPPort: {},
            SMTPUseAuthentication: {
              value: true
            },
            SMTPUsername: {},
            SMTPPassword: {},
            retypeSMTPPassword: {},
            SMTPSTARTTLS: {
              value: true
            },
            emailFrom: {},
            version: {},
            OIDs: {},
            community: {},
            host: {
              value: ''
            },
            port: {},
            scriptDispatchProperty: {
               value: 'com.mycompany.dispatch.syslog.script'
            },
            scriptFileName:{
               value: 'a.py'
            },
            customProperties: [
              {name: 'customName', value: 'customValue', defaultValue: 'customValue'}
            ]
          }));

        });
  });

  describe("#showCreateEditPopup()", function () {

    it("should open popup and set popup object to createEditPopup", function () {

      controller.showCreateEditPopup();
      expect(App.ModalPopup.show.calledOnce).to.be.true;

    });

    App.TestAliases.testAsComputedOr(getController().showCreateEditPopup(), 'disablePrimary', ['isSaving', 'hasErrors']);

    describe('#bodyClass', function () {
      function getBodyClass() {
        return createEditPopupView.get('bodyClass').create({
          controller: Em.Object.create({
            inputFields: {
              name: {},
              global: {},
              allGroups: {},
              SMTPUseAuthentication: {},
              SMTPUsername: {},
              SMTPPassword: {},
              retypeSMTPPassword: {},
              method: {}
            }
          }),
          groupSelect: Em.Object.create({
            selection: [],
            content: [{}, {}]
          }),
          parentView: Em.Object.create({
            hasErrors: false
          })
        });
      }

      var view;

      beforeEach(function () {
        view = getBodyClass();
      });

      App.TestAliases.testAsComputedOr(getBodyClass(), 'someErrorExists', ['nameError', 'emailToError', 'emailFromError', 'smtpPortError', 'hostError', 'portError', 'smtpUsernameError', 'smtpPasswordError', 'passwordError','scriptFileNameError']);

      describe('#selectAllGroups', function () {

        it('should check inputFields.allGroups.value', function () {

          view.set('controller.inputFields.allGroups.value', 'all');
          view.selectAllGroups();
          expect(view.get('groupSelect.selection')).to.eql([]);

          view.set('controller.inputFields.allGroups.value', 'custom');
          view.selectAllGroups();
          expect(view.get('groupSelect.selection')).to.eql([{}, {}]);

        });

      });

      describe('#clearAllGroups', function () {

        it('should check inputFields.allGroups.value', function () {

          view.set('controller.inputFields.allGroups.value', 'custom');
          view.selectAllGroups();

          view.set('controller.inputFields.allGroups.value', 'all');
          view.clearAllGroups();
          expect(view.get('groupSelect.selection')).to.eql([{}, {}]);

          view.set('controller.inputFields.allGroups.value', 'custom');
          view.clearAllGroups();
          expect(view.get('groupSelect.selection')).to.eql([]);

        });

      });

      describe('#nameValidation', function () {

        it('should check inputFields.name.value', function () {
          view.set('controller.inputFields.name.value', '');
          expect(view.get('controller.inputFields.name.errorMsg')).to.equal(Em.I18n.t('alerts.actions.manage_alert_notifications_popup.error.name.empty'));
          expect(view.get('parentView.hasErrors')).to.be.true;
        });

        it('should check inputFields.name.value (2)', function () {
          view.set('controller.inputFields.name.errorMsg', 'error');
          view.set('controller.inputFields.name.value', 'test');
          expect(view.get('controller.inputFields.name.errorMsg')).to.equal('');
        });

        it('should check inputFields.name.value (3)', function () {
          view.set('isEdit', true);
          view.set('controller.inputFields.name.value', '');
          expect(view.get('controller.inputFields.name.errorMsg')).to.equal(Em.I18n.t('alerts.actions.manage_alert_notifications_popup.error.name.empty'));
          expect(view.get('parentView.hasErrors')).to.be.true;
        });

        it('should check inputFields.name.value (4)', function () {
          view.set('isEdit', true);
          view.set('controller.inputFields.name.errorMsg', 'error');
          view.set('controller.inputFields.name.value', 'test');
          expect(view.get('controller.inputFields.name.errorMsg')).to.equal('');
        });
        
        it('should check inputFields.name.value (5)', function () {
         view.set('isEdit', true);
         view.set('controller.inputFields.name.errorMsg', 'error');
         view.set('controller.inputFields.name.value', 'test%');
         expect(view.get('controller.inputFields.name.errorMsg')).to.equal(Em.I18n.t('form.validator.alertNotificationName'));
       });

      });

      describe('#smtpUsernameValidation', function () {

        beforeEach(function () {
          view.set('controller.inputFields', getInputFields());
          view.set('controller.inputFields.emailFrom.value', '1@2.com');
          view.set('controller.inputFields.method.value', 'EMAIL');
        });

        it('should check inputFields.SMTPUsername.value', function () {

          view.set('parentView.hasErrors', false);
          view.set('controller.inputFields.SMTPUsername.errorMsg', null);
          view.set('controller.inputFields.SMTPUseAuthentication.value', true);
          view.set('controller.inputFields.SMTPUsername.value', '');
          view.set('controller.inputFields.SMTPPassword.value', 'pass');
          view.set('controller.inputFields.retypeSMTPPassword.value', 'pass');
          expect(view.get('controller.inputFields.SMTPUsername.errorMsg')).to.equal(Em.I18n.t('alerts.notifications.error.SMTPUsername'));
          expect(view.get('smtpUsernameError')).to.be.true;

        });

        it('should check inputFields.SMTPUsername.value (2)', function () {

          view.set('parentView.hasErrors', true);
          view.set('controller.inputFields.SMTPUsername.errorMsg', 'error');
          view.set('controller.inputFields.SMTPUseAuthentication.value', true);
          view.set('controller.inputFields.SMTPUsername.value', 'test');
          view.set('controller.inputFields.SMTPPassword.value', 'pass');
          view.set('controller.inputFields.retypeSMTPPassword.value', 'pass');
          expect(view.get('controller.inputFields.SMTPUsername.errorMsg')).to.equal(null);
          expect(view.get('smtpUsernameError')).to.be.false;

        });

        it('should check inputFields.SMTPUsername.value (3)', function () {

          view.set('parentView.hasErrors', true);
          view.set('controller.inputFields.SMTPUsername.errorMsg', 'error');
          view.set('controller.inputFields.SMTPUseAuthentication.value', false);
          view.set('controller.inputFields.SMTPUsername.value', '');
          view.set('controller.inputFields.SMTPPassword.value', '');
          view.set('controller.inputFields.retypeSMTPPassword.value', '');
          expect(view.get('controller.inputFields.SMTPUsername.errorMsg')).to.equal(null);
          expect(view.get('smtpUsernameError')).to.be.false;

        });

      });

      describe('#smtpPasswordValidation', function () {

        beforeEach(function () {
          view.set('controller.inputFields', getInputFields());
          view.set('controller.inputFields.emailFrom.value', '1@2.com');
          view.set('controller.inputFields.method.value', 'EMAIL');
        });

        it('should check inputFields.SMTPPassword.value', function () {

          view.set('parentView.hasErrors', false);
          view.set('controller.inputFields.SMTPPassword.errorMsg', null);
          view.set('controller.inputFields.SMTPUseAuthentication.value', true);
          view.set('controller.inputFields.SMTPUsername.value', 'user');
          view.set('controller.inputFields.SMTPPassword.value', '');
          view.set('controller.inputFields.retypeSMTPPassword.value', '');
          expect(view.get('controller.inputFields.SMTPPassword.errorMsg')).to.equal(Em.I18n.t('alerts.notifications.error.SMTPPassword'));
          expect(view.get('smtpPasswordError')).to.be.true;

        });

        it('should check inputFields.SMTPPassword.value (2)', function () {

          view.set('parentView.hasErrors', true);
          view.set('controller.inputFields.SMTPPassword.errorMsg', 'error');
          view.set('controller.inputFields.SMTPUseAuthentication.value', true);
          view.set('controller.inputFields.SMTPUsername.value', 'user');
          view.set('controller.inputFields.SMTPPassword.value', 'test');
          view.set('controller.inputFields.retypeSMTPPassword.value', 'test');
          expect(view.get('controller.inputFields.SMTPPassword.errorMsg')).to.equal(null);
          expect(view.get('smtpPasswordError')).to.be.false;

        });

        it('should check inputFields.SMTPPassword.value (3)', function () {

          view.set('parentView.hasErrors', true);
          view.set('controller.inputFields.SMTPPassword.errorMsg', 'error');
          view.set('controller.inputFields.SMTPUseAuthentication.value', false);
          view.set('controller.inputFields.SMTPUsername.value', '');
          view.set('controller.inputFields.SMTPPassword.value', '');
          view.set('controller.inputFields.retypeSMTPPassword.value', '');
          expect(view.get('controller.inputFields.SMTPPassword.errorMsg')).to.equal(null);
          expect(view.get('smtpPasswordError')).to.be.false;

        });

      });

      describe('#retypePasswordValidation', function () {

        it('should check inputFields.retypeSMTPPassword.value', function () {

          view.set('controller.inputFields.retypeSMTPPassword.errorMsg', null);
          view.set('controller.inputFields.SMTPPassword.value', 'pass');
          view.set('controller.inputFields.retypeSMTPPassword.value', 'pas');

          expect(view.get('controller.inputFields.retypeSMTPPassword.errorMsg')).to.equal(Em.I18n.t('alerts.notifications.error.retypePassword'));
          expect(view.get('parentView.hasErrors')).to.be.true;

        });

        it('should check inputFields.retypeSMTPPassword.value (2)', function () {

          view.set('parentView.hasErrors', true);
          view.set('controller.inputFields.retypeSMTPPassword.errorMsg', 'error');
          view.set('controller.inputFields.SMTPPassword.value', 'pass');
          view.set('controller.inputFields.retypeSMTPPassword.value', 'pass');

          expect(view.get('controller.inputFields.retypeSMTPPassword.errorMsg')).to.equal(null);
          expect(view.get('parentView.hasErrors')).to.be.false;

        });

      });

      describe('#methodObserver', function () {

        var cases = [
            {
              method: 'EMAIL',
              errors: ['portError', 'hostError'],
              validators: ['emailToValidation', 'emailFromValidation', 'smtpPortValidation', 'smtpUsernameValidation', 'smtpPasswordValidation', 'retypePasswordValidation']
            },
            {
              method: 'SNMP',
              errors: ['emailToError', 'emailFromError', 'smtpPortError', 'smtpUsernameError', 'smtpPasswordError', 'passwordError'],
              validators: ['portValidation', 'hostsValidation']
            },
            {
              method: 'ALERT_SCRIPT',
              errors: ['scriptFileNameError'],
              validators: ['scriptFileNameValidation']
            }
          ],
          validators = [];

        before(function () {
          cases.forEach(function (item) {
            validators.pushObjects(item.validators);
          });
        });

        beforeEach(function () {
          validators.forEach(function (item) {
            sinon.stub(view, item, Em.K);
          });
        });

        afterEach(function () {
          validators.forEach(function (item) {
            view.get(item).restore();
          });
        });

        cases.forEach(function (item) {
          describe(item.method, function () {

            beforeEach(function () {
              item.errors.forEach(function (errorName) {
                view.set(errorName, true);
              });
              view.set('controller.inputFields.method.value', item.method);
            });

            item.errors.forEach(function (errorName) {
              it(errorName + ' is false', function () {
                expect(view.get(errorName)).to.be.false;
              });

            });
            validators.forEach(function (validatorName) {
              var called = item.validators.contains(validatorName);
              it(validatorName + ' ' + (called ? '' : 'not') + ' called', function () {
                expect(view.get(validatorName).calledOnce).to.equal(called);
              });
            });
          });
        });

      });

      App.TestAliases.testAsComputedEqualProperties(getBodyClass(), 'allGroupsSelected', 'groupSelect.selection.length', 'groupSelect.content.length');

      App.TestAliases.testAsComputedEqualProperties(getBodyClass(), 'allSeveritySelected', 'severitySelect.selection.length', 'severitySelect.content.length');

    });

  });

  describe("#formatNotificationAPIObject()", function () {

    var inputFields = Em.Object.create({
      name: {
        value: 'test_name'
      },
      groups: {
        value: [{id: 1}, {id: 2}, {id: 3}]
      },
      allGroups: {
        value: 'custom'
      },
      global: {
        value: false
      },
      method: {
        value: 'EMAIL'
      },
      email: {
        value: 'test1@test.test, test2@test.test,test3@test.test , test4@test.test'
      },
      severityFilter: {
        value: ['OK', 'CRITICAL']
      },
      SMTPServer: {
        value: 's1'
      },
      SMTPPort: {
        value: '25'
      },
      SMTPUseAuthentication: {
        value: "true"
      },
      SMTPUsername: {
        value: 'user'
      },
      SMTPPassword: {
        value: 'pass'
      },
      SMTPSTARTTLS: {
        value: "true"
      },
      emailFrom: {
        value: 'from'
      },
      description: {
        value: 'test_description'
      },
      customProperties: [
        {name: 'n1', value: 'v1'},
        {name: 'n2', value: 'v2'}
      ]
    });

    it("should create object with properties from inputFields values", function () {

      controller.set('inputFields', inputFields);

      var result = controller.formatNotificationAPIObject();

      expect(JSON.stringify(result)).to.eql(JSON.stringify({
        AlertTarget: {
          name: 'test_name',
          description: 'test_description',
          global: false,
          notification_type: 'EMAIL',
          alert_states: ['OK', 'CRITICAL'],
          properties: {
            'ambari.dispatch.recipients': [
              'test1@test.test',
              'test2@test.test',
              'test3@test.test',
              'test4@test.test'
            ],
            "mail.smtp.host" : "s1",
            "mail.smtp.port" : "25",
            "mail.smtp.from" : "from",
            "mail.smtp.auth" : "true",
            "ambari.dispatch.credential.username" : "user",
            "ambari.dispatch.credential.password" : "pass",
            "mail.smtp.starttls.enable" : "true",
            'n1': 'v1',
            'n2': 'v2'
          },
          groups: [1,2,3]
        }
      }));
    });

    it('should ignore groups if global is true', function () {

      controller.set('inputFields', inputFields);
      controller.set('inputFields.allGroups.value', 'all');

      var result = controller.formatNotificationAPIObject();
      expect(Em.keys(result.AlertTarget)).to.not.contain('groups');

    });

  });

  describe('#createAlertNotification()', function () {

    it("should send ajax request", function () {

      controller.createAlertNotification();
      var args = helpers.findAjaxRequest('name', 'alerts.create_alert_notification');
      expect(args[0]).to.exists;
    });

  });

  describe('#createAlertNotificationSuccessCallback()', function () {

    beforeEach(function () {
      controller.set('createEditPopup', {
        hide: Em.K
      });
      sinon.stub(controller, 'loadAlertNotifications', Em.K);
      sinon.spy(controller.createEditPopup, 'hide');
    });

    afterEach(function () {
      controller.loadAlertNotifications.restore();
      controller.createEditPopup.hide.restore();
    });

    it("should call loadAlertNotifications and createEditPopup.hide", function () {

      controller.createAlertNotificationSuccessCallback();

      expect(controller.loadAlertNotifications.calledOnce).to.be.true;
      expect(controller.createEditPopup.hide.calledOnce).to.be.true;
    });

  });

  describe('#updateAlertNotification()', function () {

    it("should send ajax request", function () {

      controller.updateAlertNotification();
      var args = helpers.findAjaxRequest('name', 'alerts.update_alert_notification');
      expect(args[0]).to.exists;
    });

  });

  describe('#updateAlertNotificationSuccessCallback()', function () {

    beforeEach(function () {
      controller.set('createEditPopup', {
        hide: Em.K
      });
      sinon.stub(controller, 'loadAlertNotifications', Em.K);
      sinon.spy(controller.createEditPopup, 'hide');
    });

    afterEach(function () {
      controller.loadAlertNotifications.restore();
      controller.createEditPopup.hide.restore();
    });

    it("should call loadAlertNotifications and createEditPopup.hide", function () {

      controller.updateAlertNotificationSuccessCallback();

      expect(controller.loadAlertNotifications.calledOnce).to.be.true;
      expect(controller.createEditPopup.hide.calledOnce).to.be.true;
    });

  });

  describe('#deleteAlertNotification()', function () {

    beforeEach(function () {
      sinon.spy(App, 'showConfirmationPopup');
    });

    afterEach(function () {
      App.showConfirmationPopup.restore();
    });

    it("should show popup and send request on confirmation", function () {

      var popup = controller.deleteAlertNotification();

      expect(App.showConfirmationPopup.calledOnce).to.be.true;
      popup.onPrimary();
      var args = helpers.findAjaxRequest('name', 'alerts.delete_alert_notification');
      expect(args[0]).to.exists;
    });

  });

  describe('#deleteAlertNotificationSuccessCallback()', function () {
    var mockSelectedAlertNotification;

    beforeEach(function () {
      mockSelectedAlertNotification = {
        deleteRecord: Em.K
      };
      controller.set('selectedAlertNotification', mockSelectedAlertNotification);
      sinon.stub(controller, 'loadAlertNotifications', Em.K);
      sinon.spy(mockSelectedAlertNotification, 'deleteRecord');
      controller.deleteAlertNotificationSuccessCallback();
    });

    afterEach(function () {
      controller.loadAlertNotifications.restore();
      mockSelectedAlertNotification.deleteRecord.restore();
    });

    it("should call loadAlertNotifications", function () {
      expect(controller.loadAlertNotifications.calledOnce).to.be.true;
    });

    it("should call selectedAlertNotification.deleteRecord", function () {
      expect(mockSelectedAlertNotification.deleteRecord.calledOnce).to.be.true;
    });

    it("should set null to selectedAlertNotification", function () {
      expect(controller.get('selectedAlertNotification')).to.equal(null);
    });

  });

  describe('#duplicateAlertNotification()', function () {

    beforeEach(function () {
      sinon.stub(controller, 'fillEditCreateInputs', Em.K);
      sinon.stub(controller, 'showCreateEditPopup', Em.K);
    });

    afterEach(function () {
      controller.fillEditCreateInputs.restore();
      controller.showCreateEditPopup.restore();
    });

    it("should call fillEditCreateInputs and showCreateEditPopup", function () {

      controller.duplicateAlertNotification();

      expect(controller.fillEditCreateInputs.calledWith(true)).to.be.true;
      expect(controller.showCreateEditPopup.calledOnce).to.be.true;
    });

  });

  describe('#addCustomProperty', function () {

    beforeEach(function () {
      controller.set('inputFields.customProperties', []);
    });

    /*eslint-disable mocha-cleanup/asserts-limit */
    it('should add custom Property to customProperties', function () {
      controller.set('newCustomProperty', {name: 'n1', value: 'v1'});
      controller.addCustomProperty();
      helpers.nestedExpect([{name: 'n1', value: 'v1', defaultValue: 'v1'}], controller.get('inputFields.customProperties'));
    });
    /*eslint-enable mocha-cleanup/asserts-limit */

  });

  describe('#removeCustomPropertyHandler', function () {

    var c = {name: 'n2', value: 'v2', defaultValue: 'v2'};

    beforeEach(function () {
      controller.set('inputFields.customProperties', [
        {name: 'n1', value: 'v1', defaultValue: 'v1'},
        c,
        {name: 'n3', value: 'v3', defaultValue: 'v3'}
      ]);
    });

    /*eslint-disable mocha-cleanup/asserts-limit */
    it('should remove selected custom property', function () {
      controller.removeCustomPropertyHandler({context: c});
      helpers.nestedExpect(
        [
          {name: 'n1', value: 'v1', defaultValue: 'v1'},
          {name: 'n3', value: 'v3', defaultValue: 'v3'}
        ],
        controller.get('inputFields.customProperties')
      );
    });
    /*eslint-enable mocha-cleanup/asserts-limit */

  });

  describe('#addCustomPropertyHandler', function () {

    it('should clean up newCustomProperty on primary click', function () {

      controller.set('newCustomProperty', {name: 'n1', value: 'v1'});
      controller.addCustomPropertyHandler().onPrimary();
      expect(controller.get('newCustomProperty')).to.eql({name: '', value: ''});

    });

  });

  App.TestAliases.testAsComputedMapBy(getController(), 'customPropertyNames', 'inputFields.customProperties', 'name');

  App.TestAliases.testAsComputedExistsInByKey(getController(), 'isNewCustomPropertyExists', 'newCustomProperty.name', 'customPropertyNames', ['customA', 'customB']);

  App.TestAliases.testAsComputedExistsInByKey(getController(), 'isNewCustomPropertyIgnored', 'newCustomProperty.name', 'ignoredCustomProperties', ['customA', 'customB']);

});
