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
require('views/common/configs/custom_category_views/notification_configs_view');
var view;

function getView() {
  return App.NotificationsConfigsView.create({
    $: function() {
      return {show: Em.K, hide: Em.K};
    },
    category: {
      name: 'name'
    },
    serviceConfigs: [],
    categoryConfigs: [],
    categoryConfigsAll: [],
    mainView: Em.View.create({
      filter: '',
      columns: []
    })
  });
}

describe('App.NotificationsConfigsView', function () {

  beforeEach(function () {
    view = getView();
  });

  App.TestAliases.testAsComputedFindBy(getView(), 'useAuthConfig', 'categoryConfigs', 'name', 'smtp_use_auth');

  describe('#didInsertElement', function () {

    beforeEach(function () {
      sinon.stub(view, 'updateCategoryConfigs', Em.K);
      sinon.stub(view, 'onTlsOrSslChanged');
    });

    afterEach(function () {
      view.updateCategoryConfigs.restore();
      view.onTlsOrSslChanged.restore();
    });

    it('should not do nothing if no configs', function () {

      view.set('categoryConfigsAll', []);
      view.didInsertElement();
      expect(view.updateCategoryConfigs.called).to.equal(false);

    });

    describe('should update category configs', function () {
      var configs = [
        Em.Object.create({
          name: "create_notification",
          value: 'yes',
          filename: "alert_notification"
        }),
        Em.Object.create({
          name: 'mail.smtp.starttls.enable',
          value: false,
          filename: "alert_notification"
        }),
        Em.Object.create({
          name: 'smtp_use_auth',
          value: 'true',
          filename: "alert_notification"
        })
      ];

      beforeEach(function () {
        view.set('serviceConfigs', configs);
        view.reopen({
          categoryConfigsAll: Em.computed.filterBy('serviceConfigs', 'filename', 'alert_notification')
        });
        view.didInsertElement();
      });

      it('createNotification = yes', function () {
        expect(view.get('createNotification')).to.equal('yes');
      });
      it('tlsOrSsl = ssl', function () {
        expect(view.get('tlsOrSsl')).to.equal('ssl');
      });
      it('updateCategoryConfigs is called once', function () {
        expect(view.updateCategoryConfigs.called).to.be.true;
      });

    });
  });

  describe("#onTlsOrSslChanged()", function () {

    var configs = [
      Em.Object.create({
        name: "mail.smtp.starttls.enable",
        value: 'yes'
      }),
      Em.Object.create({
        name: 'mail.smtp.startssl.enable',
        value: false
      })
    ];

    it("tls", function () {
      view.set('categoryConfigsAll', configs);
      view.set('tlsOrSsl', 'tls');
      view.onTlsOrSslChanged();
      expect(configs.findProperty('name', 'mail.smtp.starttls.enable').get('value')).to.be.true;
      expect(configs.findProperty('name', 'mail.smtp.startssl.enable').get('value')).to.be.false;
    });

    it("ssl", function () {
      view.set('categoryConfigsAll', configs);
      view.set('tlsOrSsl', 'ssl');
      view.onTlsOrSslChanged();
      expect(configs.findProperty('name', 'mail.smtp.starttls.enable').get('value')).to.be.false;
      expect(configs.findProperty('name', 'mail.smtp.startssl.enable').get('value')).to.be.true;
    });
  });

  describe("#onUseAuthConfigChange()", function () {

    beforeEach(function () {
      sinon.stub(view, 'updateConfig');
      view.set('categoryConfigs', [
        Em.Object.create({name: 'ambari.dispatch.credential.username'}),
        Em.Object.create({name: 'smtp_use_auth'})
      ]);
    });

    afterEach(function () {
      view.updateConfig.restore();
    });

    it("auth config is not editable", function () {
      view.get('categoryConfigs').findProperty('name', 'smtp_use_auth').setProperties({
        value: true,
        isEditable: false
      });
      view.onUseAuthConfigChange();
      expect(view.updateConfig.calledWith(
        Em.Object.create({name: 'ambari.dispatch.credential.username'}),
        false
      )).to.be.true;
    });

    it("auth config is editable", function () {
      view.get('categoryConfigs').findProperty('name', 'smtp_use_auth').setProperties({
        value: true,
        isEditable: true
      });
      view.onUseAuthConfigChange();
      expect(view.updateConfig.calledWith(
        Em.Object.create({name: 'ambari.dispatch.credential.username'}),
        true
      )).to.be.true;
    });
  });

  describe("#updateCategoryConfigs()", function () {

    beforeEach(function () {
      sinon.stub(view, 'updateConfig');
      sinon.stub(view, 'onUseAuthConfigChange');
      view.set('categoryConfigs', [
        Em.Object.create({name: 'ambari.dispatch.credential.username'})
      ]);
      view.set('categoryConfigsAll', [
        Em.Object.create({
          name: 'create_notification'
        })
      ]);
    });

    afterEach(function () {
      view.updateConfig.restore();
      view.onUseAuthConfigChange.restore();
    });

    it("createNotification is 'yes'", function () {
      view.set('createNotification', 'yes');
      view.updateCategoryConfigs();
      expect(view.onUseAuthConfigChange.called).to.be.true;
      expect(view.get('categoryConfigsAll').findProperty('name', 'create_notification').get('value')).to.equal('yes');
      expect(view.updateConfig.calledWith(
        Em.Object.create({name: 'ambari.dispatch.credential.username'}),
        true
      )).to.be.true;
    });

    it("createNotification is 'no'", function () {
      view.set('createNotification', 'no');
      view.updateCategoryConfigs();
      expect(view.onUseAuthConfigChange.called).to.be.true;
      expect(view.get('categoryConfigsAll').findProperty('name', 'create_notification').get('value')).to.equal('no');
      expect(view.updateConfig.calledWith(
        Em.Object.create({name: 'ambari.dispatch.credential.username'}),
        false
      )).to.be.true;
    });
  });

  describe("#updateConfig()", function () {

    var config;

    beforeEach(function () {
      config = Em.Object.create();
    });

    describe("flag is true", function () {

      beforeEach(function () {
        view.updateConfig(config, true);
      });
      it('isRequired is true', function () {
        expect(config.get('isRequired')).to.be.true;
      });
      it('isEditable is true', function () {
        expect(config.get('isEditable')).to.be.true;
      });
    });

    describe("flag is false", function () {
      beforeEach(function () {
        view.updateConfig(config, false);
      });
      it('isRequired is false', function () {
        expect(config.get('isRequired')).to.be.false;
      });
      it('isEditable is false', function () {
        expect(config.get('isEditable')).to.be.false;
      });
    });
  });
});
