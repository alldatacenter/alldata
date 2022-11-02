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
require('views/main/admin/stack_upgrade/upgrade_task_view');

describe('App.upgradeTaskView', function () {
  var view = App.upgradeTaskView.create({
    content: Em.Object.create(),
    controller: Em.Object.create({
      getUpgradeTask: sinon.stub().returns({
        complete: Em.clb
      })
    }),
    taskDetailsProperties: ['prop1']
  });
  view.removeObserver('content.isExpanded', view, 'doPolling');
  view.removeObserver('outsideView', view, 'doPolling');

  describe("#logTabId", function() {
    it("depends on `elementId`", function() {
      view.reopen({
        elementId: 'elementId'
      });
      expect(view.get('logTabId')).to.equal('elementId-log-tab');
    });
  });

  describe("#errorTabId", function() {
    it("depends on `elementId`", function() {
      view.reopen({
        elementId: 'elementId'
      });
      expect(view.get('errorTabId')).to.equal('elementId-error-tab');
    });
  });

  describe("#logTabIdLink", function() {
    it("depends on `logTabId`", function() {
      view.reopen({
        logTabId: 'elementId-log-tab'
      });
      expect(view.get('logTabIdLink')).to.equal('#elementId-log-tab');
    });
  });

  describe("#errorTabIdLInk", function() {
    it("depends on `errorTabId`", function() {
      view.reopen({
        errorTabId: 'elementId-error-tab'
      });
      expect(view.get('errorTabIdLInk')).to.equal('#elementId-error-tab');
    });
  });

  describe("#copyErrLog()", function () {
    before(function () {
      sinon.stub(view, 'toggleProperty', Em.K);
    });
    after(function () {
      view.toggleProperty.restore();
    });
    it("`errorLogOpened` is toggled", function () {
      view.copyErrLog();
      expect(view.toggleProperty.calledWith('errorLogOpened')).to.be.true;
    });
  });

  describe("#copyOutLog()", function () {
    before(function () {
      sinon.stub(view, 'toggleProperty', Em.K);
    });
    after(function () {
      view.toggleProperty.restore();
    });
    it("outputLogOpened is toggled", function () {
      view.copyOutLog();
      expect(view.toggleProperty.calledWith('outputLogOpened')).to.be.true;
    });
  });

  describe("#openErrorLog()", function () {
    before(function () {
      sinon.stub(view, 'openLogWindow', Em.K);
    });
    after(function () {
      view.openLogWindow.restore();
    });
    it("stderr is open with openLogWindow", function () {
      view.set('content.stderr', 'stderr');
      view.openErrorLog();
      expect(view.openLogWindow.calledWith('stderr')).to.be.true;
    });
  });

  describe("#openOutLog()", function () {
    before(function () {
      sinon.stub(view, 'openLogWindow', Em.K);
    });
    after(function () {
      view.openLogWindow.restore();
    });
    it("stdout is open with openLogWindow", function () {
      view.set('content.stdout', 'stdout');
      view.openOutLog();
      expect(view.openLogWindow.calledWith('stdout')).to.be.true;
    });
  });

  describe("#openLogWindow()", function () {
    var mockAppendChild = {
        appendChild: Em.K
      },
      mockWindow = {
        document: {
          write: Em.K,
          close: Em.K,
          createElement: function () {
            return mockAppendChild;
          },
          createTextNode: Em.K,
          body: mockAppendChild
        }
      };
    beforeEach(function () {
      sinon.stub(window, 'open').returns(mockWindow);
      sinon.spy(mockWindow.document, 'write');
      sinon.spy(mockWindow.document, 'close');
      sinon.spy(mockWindow.document, 'createElement');
      sinon.spy(mockWindow.document, 'createTextNode');
      sinon.spy(mockAppendChild, 'appendChild');
      view.openLogWindow('log');
    });
    afterEach(function () {
      window.open.restore();
      mockWindow.document.write.restore();
      mockWindow.document.close.restore();
      mockWindow.document.createElement.restore();
      mockWindow.document.createTextNode.restore();
      mockAppendChild.appendChild.restore();
    });
    it("window.open is called once", function () {
      expect(window.open.calledOnce).to.be.true;
    });
    it("pre-element is created", function () {
      expect(mockWindow.document.createElement.calledWith('pre')).to.be.true;
    });
    it("log-node is created", function () {
      expect(mockWindow.document.createTextNode.calledWith('log')).to.be.true;
    });
    it("two nodes are appended", function () {
      expect(mockAppendChild.appendChild.calledTwice).to.be.true;
    });
    it("document is closed", function () {
      expect(mockWindow.document.close.calledOnce).to.be.true;
    });
  });

  describe('#toggleExpanded', function() {
    beforeEach(function() {
      sinon.stub(view, 'doPolling');
    });
    afterEach(function() {
      view.doPolling.restore();
    });

    it('doPolling should be called', function() {
      view.set('isExpanded', false);
      view.toggleExpanded();
      expect(view.get('isExpanded')).to.be.true;
      expect(view.doPolling.calledOnce).to.be.true;
    });
  });

  describe('#doPolling', function() {

    it('getUpgradeTask should be called', function() {
      view.set('isExpanded', true);
      view.doPolling();
      expect(view.get('controller').getUpgradeTask.calledOnce).to.be.true;
      expect(view.get('content.isContentLoaded')).to.be.true;
    });

    it('getUpgradeTask should be called when outside view', function() {
      view.set('outsideView', Em.Object.create({
        isDetailsOpened: true
      }));
      view.doPolling();
      expect(view.get('controller').getUpgradeTask.called).to.be.true;
      expect(view.get('content.isContentLoaded')).to.be.true;
    });
  });

});
