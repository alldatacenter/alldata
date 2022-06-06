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

describe('App.UserSettingsController', function () {
  var controller;

  beforeEach(function () {
    controller = App.UserSettingsController.create();
  });

  afterEach(function () {
    controller.destroy();
  });

  describe('#userSettingsKeys', function () {
    it('should not be empty', function () {
      expect(Object.keys(controller.get('userSettingsKeys'))).to.have.length.gt(0);
    });
  });

  describe("#getUserPrefSuccessCallback()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'updateUserPrefWithDefaultValues');
    });

    afterEach(function() {
      controller.updateUserPrefWithDefaultValues.restore();
    });

    it("response is null, updateUserPrefWithDefaultValues should be called", function() {
      expect(controller.getUserPrefSuccessCallback(null, {url: ''})).to.be.null;
      expect(controller.updateUserPrefWithDefaultValues.calledWith(null, false)).to.be.true;
      expect(controller.get('currentPrefObject')).to.be.null;
    });

    it("response is correct, updateUserPrefWithDefaultValues should not be called", function() {
      expect(controller.getUserPrefSuccessCallback({}, {url: ''})).to.be.object;
      expect(controller.updateUserPrefWithDefaultValues.called).to.be.false;
      expect(controller.get('currentPrefObject')).to.be.object;
    });
  });

  describe("#dataLoading()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'getUserPref').returns({
        complete: Em.clb
      });
    });

    afterEach(function() {
      controller.getUserPref.restore();
    });

    it("should return promise with preferences", function() {
      controller.set('currentPrefObject', {data: {}});
      var promise = controller.dataLoading();
      promise.done(function(result) {
        expect(result).to.be.eql({data: {}});
      });
      expect(controller.get('currentPrefObject')).to.be.null;
    });
  });

  describe("#getUserPrefErrorCallback()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'updateUserPrefWithDefaultValues');
    });

    afterEach(function() {
      controller.updateUserPrefWithDefaultValues.restore();
    });

    it("updateUserPrefWithDefaultValues should be called", function() {
      controller.getUserPrefErrorCallback({status: 404});
      expect(controller.updateUserPrefWithDefaultValues.calledOnce).to.be.true;
    });

    it("updateUserPrefWithDefaultValues should not be called", function() {
      controller.getUserPrefErrorCallback({status: 200});
      expect(controller.updateUserPrefWithDefaultValues.called).to.be.false;
    });
  });

  describe("#getAllUserSettings()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'dataLoading').returns({
        done: function(callback) {
          callback({});
        }
      });
      sinon.stub(controller, 'postUserPref');
      controller.reopen({
        userSettingsKeys: {
          key1: {
            defaultValue: 'val'
          }
        }
      });
      controller.getAllUserSettings();
    });

    afterEach(function() {
      controller.dataLoading.restore();
      controller.postUserPref.restore();
    });

    it("postUserPref should be called", function() {
      expect(controller.postUserPref.calledWith('key1', 'val')).to.be.true;
    });

    it("userSettings should be set", function() {
      expect(controller.get('userSettings')).to.be.eql({key1: 'val'});
    });
  });

  describe('#updateUserPrefWithDefaultValues()', function() {

    beforeEach(function() {
      sinon.stub(controller, 'postUserPref');
      controller.reopen({
        userSettingsKeys: {
          key1: {
            name: 'n1',
            defaultValue: 'val'
          }
        }
      });
    });

    afterEach(function() {
      controller.postUserPref.restore();
    });

    it('postUserPref should be called', function() {
      controller.updateUserPrefWithDefaultValues({}, true);
      expect(controller.postUserPref.calledWith('key1', 'val')).to.be.true;
    });
  });

  describe('#showSettingsPopup()', function() {

    beforeEach(function() {
      this.mockAuthorized = sinon.stub(App, 'isAuthorized');
      sinon.stub(controller, 'dataLoading').returns({
        done: Em.clb
      });
      sinon.stub(controller, 'loadPrivileges').returns({
        complete: Em.clb
      });
      sinon.stub(controller, '_showSettingsPopup');
    });

    afterEach(function() {
      controller.dataLoading.restore();
      this.mockAuthorized.restore();
      controller.loadPrivileges.restore();
      controller._showSettingsPopup.restore();
    });

    it('dataLoading should not be called', function() {
      this.mockAuthorized.returns(false);
      controller.showSettingsPopup();
      expect(controller.dataLoading).to.not.be.called;
    });

    it('_showSettingsPopup should be called', function() {
      this.mockAuthorized.returns(true);
      controller.showSettingsPopup();
      expect(controller.dataLoading).to.be.calledOnce;
      expect(controller.loadPrivileges).to.be.calledOnce;
      expect(controller._showSettingsPopup).to.be.calledonce;
    });
  });

  describe('#loadPrivileges()', function() {

    beforeEach(function() {
      sinon.stub(App.db, 'getLoginName').returns('user');
    });

    afterEach(function() {
      App.db.getLoginName.restore();
    });

    it('App.ajax.send should be called', function() {
      controller.loadPrivileges();
      var args = testHelpers.findAjaxRequest('name', 'router.user.privileges');
      expect(args[0]).to.be.eql({
        name: 'router.user.privileges',
        sender: controller,
        data: {
          userName: 'user'
        },
        success: 'loadPrivilegesSuccessCallback'
      });
    });
  });

  describe('#postUserPref()', function() {

    it('short key', function() {
      controller.postUserPref('key1', 'val1');
      expect(controller.get('userSettings.key1')).to.be.equal('val1');
    });

    it('key with prefix', function() {
      controller.postUserPref('userSettingsKeys.key2', 'val2');
      expect(controller.get('userSettings.key2')).to.be.equal('val2');
    });
  });

  describe('#loadPrivilegesSuccessCallback()', function() {

    beforeEach(function() {
      this.mock = sinon.stub(controller, 'parsePrivileges');
    });

    afterEach(function() {
      this.mock.restore();
    });

    it('should set privilege info when data present', function() {
      this.mock.returns({
        clusters: {
          'c1': {}
        },
        views: {
          'v1': {
            privileges: {},
            version: 'ver1',
            view_name: 'view1'
          }
        }
      });
      controller.loadPrivilegesSuccessCallback({items: [{}]});
      expect(JSON.stringify(controller.get('privileges'))).to.be.equal(JSON.stringify({
        clusters: [
          {
            name: 'c1',
            privileges: {}
          }
        ],
        views: [
          {
            instance_name: 'v1',
            privileges: {},
            version: 'ver1',
            view_name: 'view1'
          }
        ]
      }));
      expect(controller.get('noClusterPriv')).to.be.false;
      expect(controller.get('noViewPriv')).to.be.false;
      expect(controller.get('hidePrivileges')).to.be.false;
    });

    it('should set privilege info when data is empty', function() {
      this.mock.returns({
        clusters: {},
        views: {}
      });
      controller.loadPrivilegesSuccessCallback({items: []});
      expect(controller.get('privileges')).to.be.null;
      expect(controller.get('noClusterPriv')).to.be.true;
      expect(controller.get('noViewPriv')).to.be.true;
      expect(controller.get('hidePrivileges')).to.be.true;
    });
  });

  describe('#parsePrivileges()', function() {

    it('should parse privileges from data', function() {
      var data = {
        items: [
          {
            PrivilegeInfo: {
              type: 'CLUSTER',
              cluster_name: 'c1',
              clusters: {},
              permission_label: 'perm1'
            }
          },
          {
            PrivilegeInfo: {
              type: 'VIEW',
              instance_name: 'c1',
              view_name: 'v1',
              version: 'ver1',
              views: {},
              permission_label: 'perm1'
            }
          }
        ]
      };
      expect(controller.parsePrivileges(data)).to.be.eql({
        "clusters": {
          "c1": [
            "perm1"
          ]
        },
        "views": {
          "c1": {
            "privileges": [
              "perm1"
            ],
            "version": "ver1",
            "view_name": "v1"
          }
        }
      })
    });
  });

  describe('#_showSettingsPopup()', function() {

    beforeEach(function() {
      sinon.stub(App, 'showAlertPopup');
    });

    afterEach(function() {
      App.showAlertPopup.restore();
    });

    it('App.ModalPopup.show should be called', function() {
      controller.set('userSettingsKeys', {show_bg: {name: 'n1'}});
      controller._showSettingsPopup({n1: {}});
      expect(App.ModalPopup.show).to.be.calledOnce;
    });

    it('App.showAlertPopup should be called', function() {
      controller.set('userSettingsKeys', {show_bg: {name: 'n1'}});
      controller._showSettingsPopup({});
      expect(App.showAlertPopup).to.be.calledOnce;
    });
  });

});