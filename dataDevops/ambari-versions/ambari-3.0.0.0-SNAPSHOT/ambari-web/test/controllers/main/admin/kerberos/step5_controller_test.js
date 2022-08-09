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
var stringUtils = require('utils/string_utils');
var fileUtils = require('utils/file_utils');

describe('App.KerberosWizardStep5Controller', function() {
  var c;
  beforeEach(function () {
    c = App.KerberosWizardStep5Controller.create({});
  });

  describe('#prepareCSVData', function () {

    it('should split data', function () {
      var data = [
        'a,b,c',
        'd,e',
        '1,2,3,4'
      ];

      var result = c.prepareCSVData(data);
      expect(result).to.be.eql([['a', 'b', 'c'], ['d', 'e'], ['1', '2', '3', '4']]);
    });
  });

  describe("#submit()", function () {

    beforeEach(function() {
      sinon.stub(App.router, 'send');
    });

    afterEach(function() {
      App.router.send.restore();
    });

    it("App.router.send should be called", function() {
      c.submit();
      expect(App.router.send.calledWith('next')).to.be.true;
    });
  });

  describe("#getCSVData()", function () {

    it("App.ajax.send should be called", function() {
      c.getCSVData(true);
      var args = testHelpers.findAjaxRequest('name', 'admin.kerberos.cluster.csv');
      expect(args[0]).to.be.eql({
        name: 'admin.kerberos.cluster.csv',
        sender: c,
        data: {
          'skipDownload': true
        },
        success: 'getCSVDataSuccessCallback',
        error: 'getCSVDataSuccessCallback'
      });
    });
  });

  describe("#getCSVDataSuccessCallback()", function () {

    beforeEach(function() {
      sinon.stub(fileUtils, 'downloadTextFile');
      sinon.stub(stringUtils, 'arrayToCSV').returns('arrayToCSV');
      sinon.stub(c, 'prepareCSVData').returns('csvData');
      c.getCSVDataSuccessCallback("a\nb", {}, {skipDownload: false});
    });

    afterEach(function() {
      fileUtils.downloadTextFile.restore();
      stringUtils.arrayToCSV.restore();
      c.prepareCSVData.restore();
    });

    it("csvData should be set", function() {
      expect(c.get('csvData')).to.be.equal('csvData');
    });

    it("fileUtils.downloadTextFile should be called", function() {
      expect(fileUtils.downloadTextFile.calledWith('arrayToCSV', 'csv', 'kerberos.csv')).to.be.true;
    });
  });

  describe("#unkerberizeCluster()", function () {

    it("App.ajax.send should be called", function() {
      c.unkerberizeCluster();
      var args = testHelpers.findAjaxRequest('name', 'admin.unkerberize.cluster');
      expect(args[0]).to.be.eql({
        name: 'admin.unkerberize.cluster',
        sender: c,
        success: 'goToNextStep',
        error: 'goToNextStep'
      });
    });
  });

  describe("#goToNextStep()", function () {

    beforeEach(function() {
      sinon.stub(c, 'clearStage');
      sinon.stub(App.router, 'transitionTo');
      c.goToNextStep();
    });

    afterEach(function() {
      c.clearStage.restore();
      App.router.transitionTo.restore();
    });

    it("clearStage should be called", function() {
      expect(c.clearStage.calledOnce).to.be.true;
    });

    it("App.router.transitionTo should be called", function() {
      expect(App.router.transitionTo.calledWith('step5')).to.be.true;
    });
  });

  describe("#confirmProperties", function () {

    beforeEach(function() {
      this.mock = sinon.stub(App.router, 'get');
    });

    afterEach(function() {
      this.mock.restore();
    });

    it("should return properties", function() {
      this.mock.returns(Em.Object.create({
        content: {
          kerberosOption: Em.I18n.t('admin.kerberos.wizard.step1.option.kdc'),
          serviceConfigProperties: [{name: 'kdc_type'}]
        }
      }));
      c.propertyDidChange('confirmProperties');
      expect(c.get('confirmProperties')).to.be.eql([
        {
          name: 'kdc_type',
          label: Em.I18n.t('admin.kerberos.wizard.step5.kdc_type.label')
        }
      ]);
    });

    it("should return empty properties", function() {
      this.mock.returns(Em.Object.create({
        content: {
          kerberosOption: null,
          serviceConfigProperties: [{name: 'kdc_type'}]
        }
      }));
      c.propertyDidChange('confirmProperties');
      expect(c.get('confirmProperties')).to.be.empty;
    });
  });
});