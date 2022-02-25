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
require('views/main/admin/highAvailability/progress_view');

describe('App.HighAvailabilityProgressPageView', function () {

  var view = App.HighAvailabilityProgressPageView.create({
    controller: Em.Object.create({
      loadStep: Em.K
    })
  });


  describe("#didInsertElement()", function () {
    beforeEach(function () {
      sinon.spy(view.get('controller'), 'loadStep');
    });
    afterEach(function () {
      view.get('controller').loadStep.restore();
    });
    it("loadStep is called once", function () {
      view.didInsertElement();
      expect(view.get('controller').loadStep.calledOnce).to.be.true;
    });
  });

  describe("#headerTitle", function () {
    beforeEach(function () {
      this.mock = sinon.stub(App.router, 'get');
    });
    afterEach(function () {
      this.mock.restore();
    });
    it("currentStep is 1", function () {
      this.mock.returns(1);
      view.propertyDidChange('headerTitle');
      expect(view.get('headerTitle')).to.equal(Em.I18n.t('admin.highAvailability.wizard.rollback.header.title'));
    });
    it("currentStep is 2", function () {
      this.mock.returns(2);
      view.propertyDidChange('headerTitle');
      expect(view.get('headerTitle')).to.equal(Em.I18n.t('admin.highAvailability.wizard.step2.header'));
    });
  });

  describe("#noticeInProgress", function () {
    beforeEach(function () {
      this.mock = sinon.stub(App.router, 'get');
    });
    afterEach(function () {
      this.mock.restore();
    });
    it("currentStep is 1", function () {
      this.mock.returns(1);
      view.propertyDidChange('noticeInProgress');
      expect(view.get('noticeInProgress')).to.equal(Em.I18n.t('admin.highAvailability.rollback.notice.inProgress'));
    });
    it("currentStep is 2", function () {
      this.mock.returns(2);
      view.propertyDidChange('noticeInProgress');
      expect(view.get('noticeInProgress')).to.equal(Em.I18n.t('admin.highAvailability.wizard.progressPage.notice.inProgress'));
    });
  });

  describe("#onStatusChange()", function() {
    it("COMPLETED status", function() {
      view.reopen({
        noticeCompleted: 'noticeCompleted'
      });
      view.set('controller.status', 'COMPLETED');
      expect(view.get('notice')).to.equal('noticeCompleted');
      expect(view.get('noticeClass')).to.equal('alert alert-success');
    });
    it("FAILED status", function() {
      view.reopen({
        noticeFailed: 'noticeFailed'
      });
      view.set('controller.status', 'FAILED');
      expect(view.get('notice')).to.equal('noticeFailed');
      expect(view.get('noticeClass')).to.equal('alert alert-danger');
    });
    it("IN_PROGRESS status", function() {
      view.reopen({
        noticeInProgress: 'noticeInProgress'
      });
      view.set('controller.status', 'IN_PROGRESS');
      expect(view.get('notice')).to.equal('noticeInProgress');
      expect(view.get('noticeClass')).to.equal('alert alert-info');
    });
  });

  describe("#taskView", function() {
    var taskView = view.get('taskView').create({
      content: Em.Object.create()
    });

    describe("#didInsertElement()", function () {
      beforeEach(function () {
        sinon.spy(taskView, 'onStatus');
      });
      afterEach(function () {
        taskView.onStatus.restore();
      });
      it("onStatus is called once", function () {
        taskView.didInsertElement();
        expect(taskView.onStatus.calledOnce).to.be.true;
      });
    });

    describe("#barWidth", function () {
      it("currentStep is 1", function () {
        taskView.set('content.progress', 1);
        taskView.propertyDidChange('barWidth');
        expect(taskView.get('barWidth')).to.equal('width: 1%;');
      });
    });

    describe("#onStatus", function() {
      it("IN_PROGRESS status", function() {
        taskView.set('content.status', 'IN_PROGRESS');
        taskView.set('content.requestIds', []);
        taskView.onStatus();
        expect(taskView.get('linkClass')).to.equal('active-text');
        expect(taskView.get('icon')).to.equal('glyphicon glyphicon-cog');
        expect(taskView.get('iconColor')).to.equal('text-info');
      });
      it("FAILED status", function() {
        taskView.set('content.status', 'FAILED');
        taskView.set('content.requestIds', [{}]);
        taskView.onStatus();
        expect(taskView.get('linkClass')).to.equal('active-link');
        expect(taskView.get('icon')).to.equal('glyphicon glyphicon-exclamation-sign');
        expect(taskView.get('iconColor')).to.equal('text-danger');
      });
      it("COMPLETED status", function() {
        taskView.set('content.status', 'COMPLETED');
        taskView.set('content.requestIds', []);
        taskView.onStatus();
        expect(taskView.get('linkClass')).to.equal('active-text');
        expect(taskView.get('icon')).to.equal('glyphicon glyphicon-ok');
        expect(taskView.get('iconColor')).to.equal('text-success');
      });
      it("else status", function() {
        taskView.set('content.status', '');
        taskView.set('content.requestIds', []);
        taskView.onStatus();
        expect(taskView.get('linkClass')).to.equal('not-active-link');
        expect(taskView.get('icon')).to.equal('glyphicon glyphicon-cog');
        expect(taskView.get('iconColor')).to.be.empty;
      });
    });
  });
});
