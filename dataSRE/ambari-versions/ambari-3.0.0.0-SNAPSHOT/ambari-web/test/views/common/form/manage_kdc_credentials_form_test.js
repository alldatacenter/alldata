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
var credentialUtils = require('utils/credentials');

var view;

function getView() {
  return App.ManageCredentialsFormView.create({
    parentView: Em.Object.create({})
  });
}

describe('#App.ManageCredentialsFormView', function() {
  beforeEach(function() {
    view = getView();
  });

  afterEach(function() {
    view.destroy();
  });

  App.TestAliases.testAsComputedAlias(getView(), 'storePersisted', 'App.isCredentialStorePersistent', 'boolean');

  App.TestAliases.testAsComputedIfThenElse(getView(), 'formHeader', 'isRemovable', Em.I18n.t('admin.kerberos.credentials.form.header.stored'), Em.I18n.t('admin.kerberos.credentials.form.header.not.stored'));

  App.TestAliases.testAsComputedIfThenElse(getView(), 'hintMessage', 'storePersisted', Em.I18n.t('admin.kerberos.credentials.store.hint.supported'), Em.I18n.t('admin.kerberos.credentials.store.hint.not.supported'));

  describe('#prepareContent', function() {

    var credentials;

    beforeEach(function () {
      this.stub = sinon.stub(App, 'get');
      sinon.stub(credentialUtils, 'credentials', function(clusterName, callback) {
        callback(credentials);
      });
    });

    afterEach(function () {
      App.get.restore();
      credentialUtils.credentials.restore();
    });

    [
      {
        isStorePersistent: true,
        credentials: [
          {
            alias: 'kdc.admin.credential',
            type: 'persisted'
          }
        ],
        e: {
          isRemovable: true,
          isRemoveDisabled: false,
          storePersisted: true
        },
        m: 'persistent store is available, previous credentials were stored as persisted. Remove button should be visible and active.'
      },
      {
        isStorePersistent: true,
        credentials: [
          {
            alias: 'kdc.admin.credential',
            type: 'temporary'
          }
        ],
        e: {
          isRemovable: false,
          isRemoveDisabled: true,
          storePersisted: true
        },
        m: 'persistent store is available, previous credentials were stored as temporary. Remove button should be hidden and disabled.'
      }
    ].forEach(function(test) {
      it(test.m, function(done) {
        credentials = test.credentials;
        this.stub.withArgs('isCredentialStorePersistent').returns(test.e.storePersisted);
        view.prepareContent();
        Em.run.next(function() {
          assert.equal(view.get('isRemovable'), test.e.isRemovable, '#isRemovable property validation');
          assert.equal(view.get('isRemoveDisabled'), test.e.isRemoveDisabled, '#isRemoveDisabled property validation');
          assert.equal(view.get('storePersisted'), test.e.storePersisted, '#storePersisted property validation');
          done();
        });
      });
    });
  });

  describe('#isSubmitDisabled', function() {
    it('save button disabled by default', function() {
      expect(view.get('isSubmitDisabled')).to.be.true;
    });
    it('save button disabled when password is empty', function() {
      view.set('principal', 'some_principal');
      expect(view.get('isSubmitDisabled')).to.be.true;
    });
    it('save button disabled when principal is empty', function() {
      view.set('password', 'some_password');
      expect(view.get('isSubmitDisabled')).to.be.true;
    });
    it('save button should be enabled when principal and password are filled', function() {
      view.set('password', 'some_password');
      view.set('principal', 'principal');
      expect(view.get('isSubmitDisabled')).to.be.false;
    });
  });

  describe('fields validation', function() {
    var t = Em.I18n.t;

    it('should flow validation', function() {
      assert.isTrue(view.get('isSubmitDisabled'), 'submit disabled on initial state');
    });

    it('principal is not empty', function() {
      view.set('principal', ' a');
      expect(view.get('principalError')).to.equal(t('host.spacesValidation'));
      assert.isTrue(view.get('isPrincipalDirty'), 'principal name modified');
      assert.isTrue(view.get('isSubmitDisabled'), 'submit disabled because principal not valid');
    });

    it('principal is empty', function() {
      view.set('principal', ' a');
      view.set('principal', '');
      expect(view.get('principalError')).to.equal(t('admin.users.editError.requiredField'));
    });

    it('principal is not empty (2)', function() {
      view.set('principal', 'some_name');
      assert.isFalse(view.get('principalError'), 'principal name valid no message shown');
      assert.isTrue(view.get('isSubmitDisabled'), 'submit disabled because password field not modified');
    });

    it('password is updated', function() {
      view.set('password', '1');
      view.set('password', '');
      expect(view.get('passwordError')).to.equal(t('admin.users.editError.requiredField'));
      assert.isTrue(view.get('isPasswordDirty'), 'password modified');
      assert.isTrue(view.get('isSubmitDisabled'), 'submit disabled because password field is empty');
    });

    it('password is updated (2)', function() {
      view.set('password', 'some_pass');
      view.set('principal', 'some_name');
      assert.isFalse(view.get('passwordError'), 'password valid no message shown');
      assert.isFalse(view.get('isSubmitDisabled'), 'submit enabled all fields are valid');
    });

  });

  describe('#removeKDCCredentials', function() {

    var popup;

    beforeEach(function () {
      popup = view.removeKDCCredentials().popup;
      this.clock = sinon.useFakeTimers();
      sinon.stub(credentialUtils, 'removeCredentials', function() {
        var dfd = $.Deferred();
        setTimeout(function() {
          dfd.resolve();
        }, 500);
        return dfd.promise();
      });
    });

    afterEach(function () {
      popup.destroy();
      credentialUtils.removeCredentials.restore();
      this.clock.restore();
    });

    it('should show confirmation popup', function() {
      expect(popup).be.instanceof(App.ModalPopup);
    });

    it('on popup open', function() {
      assert.isFalse(view.get('actionStatus'), '#actionStatus before remove');
    });

    it('on Primary', function() {
      popup.onPrimary();
      assert.isTrue(view.get('isActionInProgress'), 'action in progress');
      assert.isTrue(view.get('isRemoveDisabled'), 'remove button disabled');
      assert.isTrue(view.get('isSubmitDisabled'), 'submit button disabled');
    });

    it('after 1s', function() {
      popup.onPrimary();
      this.clock.tick(1000);
      assert.isFalse(view.get('isActionInProgress'), 'action finished');
      assert.equal(Em.I18n.t('common.success'), view.get('actionStatus'), '#actionStatus after remove');
      assert.isTrue(view.get('parentView.isCredentialsRemoved'), 'parentView#isCredentialsRemoved property should be triggered when remove complete');
    });
  });

});
