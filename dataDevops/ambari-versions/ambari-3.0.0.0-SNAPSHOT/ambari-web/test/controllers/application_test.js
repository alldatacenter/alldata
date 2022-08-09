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
require('models/cluster');
var testHelpers = require('test/helpers');

function getController() {
  return App.ApplicationController.create();
}

describe('App.ApplicationController', function () {

  var applicationController = getController();

  App.TestAliases.testAsComputedTruncate(getController(), 'clusterDisplayName', 'clusterName', 13, 10);

  App.TestAliases.testAsComputedAnd(getController(), 'isClusterDataLoaded', ['App.router.clusterController.isLoaded','App.router.loggedIn']);

  App.TestAliases.testAsComputedAnd(getController(), 'isExistingClusterDataLoaded', ['App.router.clusterInstallCompleted','isClusterDataLoaded']);

  App.TestAliases.testAsComputedAnd(getController(), 'enableLinks', ['isExistingClusterDataLoaded','!App.isOnlyViewUser']);

  describe('#showAboutPopup', function() {
    var dataToShowRes = {};
    beforeEach(function () {
      App.ModalPopup.show.restore();
      sinon.stub(App.ModalPopup, 'show', function(dataToShow){
        dataToShowRes = dataToShow;
      });
    });
    it ('Should send correct data to popup', function() {
      applicationController.showAboutPopup();
      dataToShowRes = JSON.parse(JSON.stringify(dataToShowRes));
      expect(dataToShowRes).to.eql({
        "header": "About",
        "secondary": false
      });
    });
  });

  describe('#startKeepAlivePoller', function() {
    it ('Should change run poller state', function() {
      applicationController.set('isPollerRunning', false);
      applicationController.startKeepAlivePoller();
      expect(applicationController.get('isPollerRunning')).to.be.true;
    });
  });

  describe('#goToAdminView', function() {
    var result;
    beforeEach(function () {
      sinon.stub(App.router, 'route', function(data) {
        result = data;
        return false;
      });
    });
    afterEach(function () {
      App.router.route.restore();
    });
    it ('Should call route once', function() {
      applicationController.goToAdminView();
      expect(result).to.be.equal('adminView');
    });
  });

  describe('#getStack', function() {

    it ('Should return send value', function() {
      var callback = {
        'callback': true
      };
      applicationController.getStack(callback);
      var args = testHelpers.findAjaxRequest('name', 'router.login.clusters');
      expect(args[0]).to.exists;
      expect(args[0].callback.callback).to.be.true;
    });
  });

});
