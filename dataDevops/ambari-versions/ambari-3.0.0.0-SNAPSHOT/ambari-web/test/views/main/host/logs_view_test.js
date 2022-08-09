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
var fileUtils = require('utils/file_utils');

describe('App.MainHostLogsView', function() {
  var view;

  beforeEach(function() {
    view = App.MainHostLogsView.create({
      host: Em.Object.create()
    });
  });

  describe("#hostLogs", function () {

    beforeEach(function() {
      sinon.stub(App.HostComponentLog, 'find').returns([Em.Object.create({
        hostName: 'host1',
        logFileNames: []
      })]);
    });

    afterEach(function() {
      App.HostComponentLog.find.restore();
    });

    it("should return host logs", function() {
      view.set('host.hostName', 'host1');
      view.propertyDidChange('hostLogs');
      expect(view.get('hostLogs')).to.be.eql([Em.Object.create({
        hostName: 'host1',
        logFileNames: []
      })]);
    });
  });

  describe("#content", function () {

    beforeEach(function() {
      sinon.stub(fileUtils, 'fileNameFromPath').returns('file1');
    });

    afterEach(function() {
      fileUtils.fileNameFromPath.restore();
    });

    it("should return content", function() {
      view.reopen({
        host: Em.Object.create({hostName: 'host1'}),
        hostLogs: [Em.Object.create({
          hostComponent: {
            service: {
              displayName: 's1',
              serviceName: 'S1'
            },
            componentName: 'C1',
            displayName: 'c1'
          },
          hostName: 'host1',
          name: 'l1',
          logFileNames: ['f1', 'f2']
        })]
      });
      view.propertyDidChange('content');
      expect(view.get('content')).to.be.eql([Em.Object.create({
        serviceName: 'S1',
        serviceDisplayName: 's1',
        componentName: 'C1',
        componentDisplayName: 'c1',
        hostName: 'host1',
        logComponentName: 'l1',
        fileNamesObject: [
          {
            fileName: 'file1',
            filePath: 'f1',
            linkTail: '/#/logs/serviceLogs;hosts=host1;components=l1;query=%5B%7B"id":0,"name":"path","label":"Path","value":"f1","isExclude":false%7D%5D'
          },
          {
            fileName: 'file1',
            filePath: 'f2',
            linkTail: '/#/logs/serviceLogs;hosts=host1;components=l1;query=%5B%7B"id":0,"name":"path","label":"Path","value":"f2","isExclude":false%7D%5D'
          }
        ],
        fileNamesFilterValue: 'f1,f2'
      })]);
    });
  });

  describe("#logFileRowView", function () {
    var logFileRowView;

    beforeEach(function() {
      logFileRowView = view.get('logFileRowView').create()
    });

    describe("#didInsertElement()", function () {

      beforeEach(function() {
        sinon.stub(App, 'tooltip');
      });

      afterEach(function() {
        App.tooltip.restore();
      });

      it("App.tooltip should be called", function() {
        logFileRowView.didInsertElement();
        expect(App.tooltip.calledOnce).to.be.true;
      });
    });

    describe("#willDestroyElement()", function () {
      var mock = {
        tooltip: Em.K
      };

      beforeEach(function() {
        sinon.stub(logFileRowView, '$').returns(mock);
        sinon.spy(mock, 'tooltip');
      });

      afterEach(function() {
        logFileRowView.$.restore();
        mock.tooltip.restore();
      });

      it("tooltip should be destroyed", function() {
        logFileRowView.willDestroyElement();
        expect(mock.tooltip.calledWith('destroy')).to.be.true;
      });
    });
  });

  describe("#openLogFile()", function () {

    beforeEach(function() {
      sinon.stub(App, 'showLogTailPopup');
    });

    afterEach(function() {
      App.showLogTailPopup.restore();
    });

    it("contexts is empty", function() {
      view.openLogFile({contexts: []});
      expect(App.showLogTailPopup.called).to.be.false;
    });

    it("App.showLogTailPopup should be called", function() {
      view.openLogFile({contexts: [Em.Object.create({
        logComponentName: 'lc1',
        hostName: 'host1'
      }), 'f1']});
      expect(App.showLogTailPopup.calledWith(Em.Object.create({
        logComponentName: 'lc1',
        hostName: 'host1',
        filePath: 'f1'
      }))).to.be.true;
    });
  });
});
