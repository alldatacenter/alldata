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
var preInstallChecksController;

describe('App.PreInstallChecksController', function () {

  beforeEach(function () {
    preInstallChecksController = App.PreInstallChecksController.create();
  });

  describe('#loadStep', function () {

    beforeEach(function () {
      preInstallChecksController.set('preInstallChecksWhereRun', true);
    });

    it('should set `preInstallChecksWhereRun` to false', function () {
      preInstallChecksController.loadStep();
      expect(preInstallChecksController.get('preInstallChecksWhereRun')).to.be.false;
    });

  });

  describe('#runPreInstallChecks', function () {

    it('should set `preInstallChecksWhereRun` to true', function () {
      preInstallChecksController.runPreInstallChecks();
      expect(preInstallChecksController.get('preInstallChecksWhereRun')).to.be.true;
    });

  });

  describe('#notRunChecksWarnPopup', function () {

    it('should throw error', function () {
      expect(function () {
        preInstallChecksController.notRunChecksWarnPopup()
      }).to.throw('`afterChecksCallback` should be a function');
    });

    describe('popup', function () {

      var popup;

      beforeEach(function () {
        popup = preInstallChecksController.notRunChecksWarnPopup(Em.K);
        sinon.spy(preInstallChecksController, 'runPreInstallChecks');
      });

      afterEach(function () {
        preInstallChecksController.runPreInstallChecks.restore();
      });

      it('#onPrimary', function (done) {
        preInstallChecksController.notRunChecksWarnPopup(done).onPrimary();
      });

      it('#onSecondary', function () {
        popup.onSecondary();
        expect(preInstallChecksController.runPreInstallChecks.calledOnce).to.be.true;
      });

    });

  });

});