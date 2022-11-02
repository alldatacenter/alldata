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
require('utils/helper');
require('utils/string_utils');
require('views/wizard/step6_view');
var view;

function getView() {
  return App.WizardStep6View.create({
    controller: App.WizardStep6Controller.create(),
    $: function () {
      return {
        on: Em.K
      };
    }
  });
}

describe('App.WizardStep6View', function() {

  beforeEach(function() {
    view = getView();
  });

  App.TestAliases.testAsComputedAlias(getView(), 'filteredContent', 'content', 'array');

  describe('#content', function() {
    it('should be same to controller.hosts', function() {
      view.set('content', []);
      var d = [{}, {}];
      view.set('controller.hosts', d);
      expect(view.get('content')).to.eql(d);
    });
  });

  describe('#didInsertElement', function() {

    beforeEach(function() {
      sinon.stub(view.get('controller'), 'loadStep', Em.K);
      sinon.stub(App, 'tooltip', Em.K);
      sinon.stub(view, 'setLabel', Em.K);
    });

    afterEach(function() {
      view.get('controller').loadStep.restore();
      App.tooltip.restore();
      view.setLabel.restore();
    });

    it('should call loadStep', function() {
      view.didInsertElement();
      expect(view.get('controller').loadStep.calledOnce).to.equal(true);
    });

    it('should call setLabel if not controller.isMasters', function() {
      view.set('controller.isMasters', false);
      view.didInsertElement();
      expect(view.setLabel.calledOnce).to.equal(true);
    });

  });

  describe('#setLabel', function() {
    var tests = Em.A([
      {
        clients: [{display_name: 'c1'}],
        m: 'One client',
        e: 'c1'
      },
      {
        clients: [{display_name: 'c1'}, {display_name: 'c2'}],
        m: 'Two clients',
        e: 'c1 and c2.'
      },
      {
        clients: [{display_name: 'c1'}, {display_name: 'c2'}, {display_name: 'c3'}],
        m: 'Three clients',
        e: 'c1, c2 and c3.'
      },
      {
        clients: [{display_name: 'c1'}, {display_name: 'c2'}, {display_name: 'c3'}, {display_name: 'c4'}],
        m: 'Four clients',
        e: 'c1, c2, c3 and c4.'
      },
      {
        clients: [{display_name: 'c1'}, {display_name: 'c2'}, {display_name: 'c3'}, {display_name: 'c4'}, {display_name: 'c5'}],
        m: 'Five clients',
        e: 'c1, c2, c3, c4 and c5.'
      }
    ]);
    tests.forEach(function(test) {
      it(test.m, function() {
        view.set('controller.content', {clients: test.clients});
        view.setLabel();
        expect(view.get('label').endsWith(test.e)).to.equal(true);
      });
    });
  });

  describe("#checkboxClick()", function() {
    var e;
    beforeEach(function() {
      sinon.stub(view.get('controller'), 'checkCallback', Em.K);
      sinon.stub(view.get('controller'), 'callValidation', Em.K);
      e = {
        context: {
          checked: true,
          component: 'c1'
        }
      };
      view.checkboxClick(e);
    });
    afterEach(function() {
      view.get('controller').checkCallback.restore();
      view.get('controller').callValidation.restore();
    });

    it("checked is false", function() {
      expect(e.context.checked).to.be.false;
    });

    it("checkCallback is called with correct data", function() {
      expect(view.get('controller').checkCallback.calledWith('c1')).to.be.true;
    });

    it("callValidation is called once", function() {
      expect(view.get('controller').callValidation.calledOnce).to.be.true;
    });
  });

});

describe('App.WizardStep6HostView', function() {

  beforeEach(function() {
    view = App.WizardStep6HostView.create({
      controller: App.WizardStep6Controller.create()
    });
  });

  describe('#didInsertElement', function() {

    var data = [];

    beforeEach(function() {
      sinon.stub(App, 'popover', Em.K);
      sinon.stub(view.get('controller'), 'getMasterComponentsForHost', function() {return data;});
    });

    afterEach(function() {
      App.popover.restore();
      view.get('controller').getMasterComponentsForHost.restore();
    });

    it('should create popover if not controller.isMasters', function() {
      data = [{}, {}];
      view.set('controller.isMasters', false);
      view.didInsertElement();
      expect(App.popover.calledOnce).to.equal(true);
    });

    it('should create popover even if controller.getMasterComponentsForHost is an empty array', function() {
      data = [{}];
      view.set('controller.isMasters', true);
      view.didInsertElement();
      expect(App.popover.calledOnce).to.equal(true);
    });
  });

});
