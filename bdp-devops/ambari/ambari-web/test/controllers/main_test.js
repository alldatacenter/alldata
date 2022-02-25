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

describe('App.MainController', function () {
  var mainController = App.MainController.create();

  App.TestAliases.testAsComputedAlias(mainController, 'isClusterDataLoaded', 'App.router.clusterController.isLoaded', 'boolean');

  App.TestAliases.testAsComputedAlias(mainController, 'clusterDataLoadedPercent', 'App.router.clusterController.clusterDataLoadedPercent', 'string');

  describe('#initialize', function() {
    var initialize = false;
    beforeEach(function () {
      sinon.stub(App.router, 'get').returns({
        loadClusterData: function() {
          initialize = true;
        },
        startSubscriptions: Em.K
      });
      sinon.stub(App.StompClient, 'connect').returns({
        done: function() {
          return {
            fail: Em.K
          }
        }
      });
    });
    afterEach(function () {
      App.router.get.restore();
      App.StompClient.connect.restore();
    });
    it ('Should return true', function() {
      mainController.initialize();
      expect(initialize).to.be.true;
    });
    it ('App.StompClient.connect should be called', function() {
      mainController.initialize();
      expect(App.StompClient.connect.calledOnce).to.be.true;
    });
  });

  describe('#dataLoading', function() {

    beforeEach(function () {
      this.stub = sinon.stub(App.router, 'get');
    });

    afterEach(function () {
      this.stub.restore();
    });

    it ('Should resolve promise', function() {
      this.stub.returns(true);
      var deffer = mainController.dataLoading();
      deffer.then(function(val){
        expect(val).to.be.undefined;
      });
    });
    it ('Should resolve promise (2)', function(done) {
      this.stub.returns(false);
      
      setTimeout(function() {
        mainController.set('isClusterDataLoaded', true);
      },150);

      var deffer = mainController.dataLoading();
      deffer.then(function(val){
        expect(val).to.be.undefined;
        done();
      });
    });
  });

  describe('#updateTitle', function() {
    beforeEach(function () {
      sinon.stub(App.router, 'get').withArgs('clusterController.clusterName').returns('c1')
        .withArgs('clusterInstallCompleted').returns(true)
        .withArgs('clusterController').returns({
          get: function() {
            return true;
          }
        });
    });
    afterEach(function () {
      App.router.get.restore();
    });
    it('Should update title', function() {
      $('body').append('<title id="title-id">text</title>');
      mainController.updateTitle();
      expect($('title').text()).to.be.equal('Ambari - c1');
      $('body').remove('#title-id');
    });
  });

  describe('#startPolling', function() {
    var mock,
        updateController = Em.Object.create({
          isWorking: false
        });
    beforeEach(function() {
      mock = sinon.stub(App.router, 'get');
      mock.withArgs('applicationController.isExistingClusterDataLoaded').returns(true);
      mock.withArgs('updateController').returns(updateController);
      mainController.startPolling();
    });
    afterEach(function() {
      App.router.get.restore();
    });

    it('updateController should be working', function() {
      expect(updateController.get('isWorking')).to.be.true;
    });

  });

});
