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
require('views/main/service/reconfigure');

describe('App.StageLabelView', function () {
  var view;

  beforeEach(function() {
    view = App.StageLabelView.create({
      controller: Em.Object.create()
    });
  });

  describe('#didInsertElement', function() {
    beforeEach(function() {
      sinon.stub(view, 'onLink');
    });
    afterEach(function() {
      view.onLink.restore();
    });

    it('onLink should be called', function() {
      view.didInsertElement();
      expect(view.onLink.calledOnce).to.be.true;
    });
  });

  describe('#onLink', function() {
    it('removeLink should be null', function() {
      view.set('command', Em.Object.create({showLink: true}));
      view.onLink();
      expect(view.get('removeLink')).to.be.null;
    });
    it('removeLink should be "remove-link"', function() {
      view.set('command', Em.Object.create({showLink: false}));
      view.onLink();
      expect(view.get('removeLink')).to.be.equal('remove-link');
    });
  });

  describe('#click', function() {
    beforeEach(function() {
      sinon.stub(view, 'showHostPopup');
    });
    afterEach(function() {
      view.showHostPopup.restore();
    });

    it('showHostsPopup should be called', function() {
      view.set('command', Em.Object.create({showLink: true}));
      view.click();
      expect(view.showHostPopup.calledWith(Em.Object.create({showLink: true}))).to.be.true;
    });
  });

  describe('#showHostPopup', function() {
    var mock = Em.Object.create();
    beforeEach(function() {
      sinon.stub(App.router, 'get').returns({
        dataLoading: function() {
          return {
            done: Em.clb
          };
        }
      });
      sinon.stub(App.HostPopup, 'initPopup').returns(mock);
    });
    afterEach(function() {
      App.router.get.restore();
      App.HostPopup.initPopup.restore();
    });

    it('App.HostPopup.initPopup should be called', function() {
      view.showHostPopup(Em.Object.create({label: 'foo', requestId: 1}));
      expect(App.HostPopup.initPopup.calledWith('foo', view.get('controller'), false, 1)).to.be.true;
      expect(mock.get('isNotShowBgChecked')).to.be.true;
    });
  });
});

describe('App.StageInProgressView', function () {
  var view;

  beforeEach(function() {
    view = App.StageInProgressView.create({
      controller: Em.Object.create(),
      obj: Em.Object.create()
    });
  });

  describe('#isStageCompleted', function() {

    it('should return false when progress is 0 and step not completed', function() {
      view.set('obj.progress', 0);
      view.set('controller.isStepCompleted', false);
      expect(view.get('isStageCompleted')).to.be.false;
    });
    it('should return true when progress is 100 and step not completed', function() {
      view.set('obj.progress', 100);
      view.set('controller.isStepCompleted', false);
      expect(view.get('isStageCompleted')).to.be.true;
    });
    it('should return true when progress is 0 and step completed', function() {
      view.set('obj.progress', 0);
      view.set('controller.isStepCompleted', true);
      expect(view.get('isStageCompleted')).to.be.true;
    });
  });
});
