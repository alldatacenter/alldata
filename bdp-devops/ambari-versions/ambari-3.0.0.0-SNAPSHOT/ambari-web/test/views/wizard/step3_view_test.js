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
require('messages');
require('views/wizard/step3_view');
var v;
function getView() {
  return App.WizardStep3View.create({
    monitorStatuses: function () {
    },
    content: [
      Em.Object.create({
        name: 'host1',
        bootStatus: 'PENDING',
        isChecked: false
      }),
      Em.Object.create({
        name: 'host2',
        bootStatus: 'PENDING',
        isChecked: true
      }),
      Em.Object.create({
        name: 'host3',
        bootStatus: 'PENDING',
        isChecked: true
      }),
      Em.Object.create({
        name: 'host4',
        bootStatus: 'RUNNING',
        isChecked: true
      })
    ],
    pageContent: function () {
      return this.get('content');
    }.property('content')
  });
}

describe('App.WizardStep3View', function () {

  var view = getView();

  describe('#watchSelection', function () {
    it('2 of 4 not running hosts selected', function () {
      view.watchSelection();
      expect(view.get('noNotRunningHostsSelected')).to.equal(false);
      expect(view.get('selectedNotRunningHostsCount')).to.equal(2);
    });
    it('all not running hosts selected', function () {
      view.selectAll();
      view.watchSelection();
      expect(view.get('noNotRunningHostsSelected')).to.equal(false);
      expect(view.get('selectedNotRunningHostsCount')).to.equal(3);
    });
    it('none hosts selected', function () {
      view.unSelectAll();
      view.watchSelection();
      expect(view.get('noNotRunningHostsSelected')).to.equal(true);
      expect(view.get('selectedNotRunningHostsCount')).to.equal(0);
    });
  });

  describe('#selectAll', function () {
    it('select all hosts', function () {
      view.selectAll();
      expect(view.get('content').everyProperty('isChecked', true)).to.equal(true);
    });
  });

  describe('#unSelectAll', function () {
    it('unselect all hosts', function () {
      view.unSelectAll();
      expect(view.get('content').everyProperty('isChecked', false)).to.equal(true);
    });
  });

  var testCases = Em.A([
    {
      title: 'none hosts',
      content: [],
      result: {
        "ALL": 0,
        "RUNNING": 0,
        "REGISTERING": 0,
        "REGISTERED": 0,
        "FAILED": 0
      }
    },
    {
      title: 'all hosts RUNNING',
      content: [
        Em.Object.create({
          name: 'host1',
          bootStatus: 'RUNNING'
        }),
        Em.Object.create({
          name: 'host2',
          bootStatus: 'RUNNING'
        }),
        Em.Object.create({
          name: 'host3',
          bootStatus: 'RUNNING'
        })
      ],
      result: {
        "ALL": 3,
        "RUNNING": 3,
        "REGISTERING": 0,
        "REGISTERED": 0,
        "FAILED": 0
      }
    },
    {
      title: 'all hosts REGISTERING',
      content: [
        Em.Object.create({
          name: 'host1',
          bootStatus: 'REGISTERING'
        }),
        Em.Object.create({
          name: 'host2',
          bootStatus: 'REGISTERING'
        }),
        Em.Object.create({
          name: 'host3',
          bootStatus: 'REGISTERING'
        })
      ],
      result: {
        "ALL": 3,
        "RUNNING": 0,
        "REGISTERING": 3,
        "REGISTERED": 0,
        "FAILED": 0
      }
    },
    {
      title: 'all hosts REGISTERED',
      content: [
        Em.Object.create({
          name: 'host1',
          bootStatus: 'REGISTERED'
        }),
        Em.Object.create({
          name: 'host2',
          bootStatus: 'REGISTERED'
        }),
        Em.Object.create({
          name: 'host3',
          bootStatus: 'REGISTERED'
        })
      ],
      result: {
        "ALL": 3,
        "RUNNING": 0,
        "REGISTERING": 0,
        "REGISTERED": 3,
        "FAILED": 0
      }
    },
    {
      title: 'all hosts FAILED',
      content: [
        Em.Object.create({
          name: 'host1',
          bootStatus: 'FAILED'
        }),
        Em.Object.create({
          name: 'host2',
          bootStatus: 'FAILED'
        }),
        Em.Object.create({
          name: 'host3',
          bootStatus: 'FAILED'
        })
      ],
      result: {
        "ALL": 3,
        "RUNNING": 0,
        "REGISTERING": 0,
        "REGISTERED": 0,
        "FAILED": 3
      }
    },
    {
      title: 'first host is FAILED, second is RUNNING, third is REGISTERED',
      content: [
        Em.Object.create({
          name: 'host1',
          bootStatus: 'FAILED'
        }),
        Em.Object.create({
          name: 'host2',
          bootStatus: 'RUNNING'
        }),
        Em.Object.create({
          name: 'host3',
          bootStatus: 'REGISTERED'
        })
      ],
      result: {
        "ALL": 3,
        "RUNNING": 1,
        "REGISTERING": 0,
        "REGISTERED": 1,
        "FAILED": 1
      }
    },
    {
      title: 'two hosts is REGISTERING, one is REGISTERED',
      content: [
        Em.Object.create({
          name: 'host1',
          bootStatus: 'REGISTERING'
        }),
        Em.Object.create({
          name: 'host2',
          bootStatus: 'REGISTERING'
        }),
        Em.Object.create({
          name: 'host3',
          bootStatus: 'REGISTERED'
        })
      ],
      result: {
        "ALL": 3,
        "RUNNING": 0,
        "REGISTERING": 2,
        "REGISTERED": 1,
        "FAILED": 0
      }
    }
  ]);

  describe('#countCategoryHosts', function () {
    var _view;
    testCases.forEach(function (test) {
      describe(test.title, function () {

        beforeEach(function () {
          _view = getView();
          _view.set('content', test.content);
          _view.countCategoryHosts();
        });

        Object.keys(test.result).forEach(function (categoryName) {
          it('`' + categoryName + '`', function () {
            expect(_view.get('categories').findProperty('hostsBootStatus', categoryName).get('hostsCount')).to.be.equal(test.result[categoryName])
          });
        });

      });
    }, this);
  });

  describe('#doFilter', function () {
    testCases.forEach(function (test) {
      describe(test.title, function () {
        view.get('categories').forEach(function (category) {
          it('. Selected category - ' + category.get('hostsBootStatus'), function () {
            view.set('content', test.content);
            view.reopen({
              selectedCategory: category
            });
            view.doFilter();
            expect(view.get('filteredContent').length).to.equal(test.result[category.get('hostsBootStatus')])
          });
        });
      });
    }, this);
  });

  describe('#monitorStatuses', function() {
    var tests = Em.A([
      {
        controller: Em.Object.create({bootHosts: Em.A([])}),
        m: 'Empty hosts list',
        e: {status: 'alert-warning', linkText: ''}
      },
      {
        controller: Em.Object.create({bootHosts: Em.A([{}]), isWarningsLoaded: false}),
        m: 'isWarningsLoaded false',
        e: {status: 'alert-info', linkText: ''}
      },
      {
        controller: Em.Object.create({bootHosts: Em.A([{}]), isWarningsLoaded: true, isHostHaveWarnings: true}),
        m: 'isWarningsLoaded true, isHostHaveWarnings true',
        e: {status: 'alert-warning', linkText: Em.I18n.t('installer.step3.warnings.linkText')}
      },
      {
        controller: Em.Object.create({bootHosts: Em.A([{}]), isWarningsLoaded: true, repoCategoryWarnings: ['']}),
        m: 'isWarningsLoaded true, repoCategoryWarnings not empty',
        e: {status: 'alert-warning', linkText: Em.I18n.t('installer.step3.warnings.linkText')}
      },
      {
        controller: Em.Object.create({bootHosts: Em.A([{}]), isWarningsLoaded: true, diskCategoryWarnings: ['']}),
        m: 'isWarningsLoaded true, diskCategoryWarnings not empty',
        e: {status: 'alert-warning', linkText: Em.I18n.t('installer.step3.warnings.linkText')}
      },
      {
        controller: Em.Object.create({bootHosts: Em.A([{}]), isWarningsLoaded: true, diskCategoryWarnings: [], repoCategoryWarnings: []}),
        m: 'isWarningsLoaded true, diskCategoryWarnings is empty, repoCategoryWarnings is empty',
        e: {status: 'alert-success', linkText: Em.I18n.t('installer.step3.noWarnings.linkText')}
      },
      {
        controller: Em.Object.create({bootHosts: Em.A([{bootStatus: 'FAILED'}]), isWarningsLoaded: true, diskCategoryWarnings: [], repoCategoryWarnings: []}),
        m: 'isWarningsLoaded true, diskCategoryWarnings is empty, repoCategoryWarnings is empty, all failed',
        e: {status: 'alert-warning', linkText: ''}
      }
    ]);

    tests.forEach(function(test) {
      it(test.m, function() {
        v = App.WizardStep3View.create({
          controller: test.controller
        });
        v.monitorStatuses();
        expect(v.get('status')).to.equal(test.e.status);
        expect(v.get('linkText')).to.equal(test.e.linkText);
      });
    });
  });

  describe('#retrySelectedHosts', function() {
    it('should set active category "All"', function() {
      view.set('controller', Em.Object.create({retrySelectedHosts: Em.K, registeredHosts: []}));
      view.retrySelectedHosts();
      expect(view.get('categories').findProperty('hostsBootStatus', 'ALL').get('isActive')).to.equal(true);
    });
  });

  describe('#selectCategory', function() {
    var tests = Em.A(['ALL','RUNNING','REGISTERING','REGISTERED','FAILED']);
    tests.forEach(function(test) {
      it('should set active category "' + test + '"', function() {
        view.set('controller', Em.Object.create({retrySelectedHosts: Em.K, registeredHosts: []}));
        view.selectCategory({context:Em.Object.create({hostsBootStatus:test})});
        expect(view.get('categories').findProperty('hostsBootStatus', test).get('isActive')).to.equal(true);
      });
    });
  });

  describe('#countCategoryHosts', function() {
    it('should set host count for each category', function() {
      view.set('content', Em.A([
        Em.Object.create({bootStatus: 'RUNNING'}),
        Em.Object.create({bootStatus: 'REGISTERING'}),
        Em.Object.create({bootStatus: 'REGISTERED'}),
        Em.Object.create({bootStatus: 'FAILED'})
      ]));
      view.countCategoryHosts();
      expect(view.get('categories').mapProperty('hostsCount')).to.eql([4,1,1,1,1]);
    });
  });

  describe('#hostBootStatusObserver', function() {

    beforeEach(function () {
      sinon.spy(Em.run, 'once');
      view.hostBootStatusObserver();
    });

    afterEach(function () {
      Em.run.once.restore();
    });

    it('should call "Em.run.once" three times', function() {
      expect(Em.run.once.firstCall.args[1]).to.equal('countCategoryHosts');
      expect(Em.run.once.secondCall.args[1]).to.equal('filter');
      expect(Em.run.once.thirdCall.args[1]).to.equal('monitorStatuses');
    });
  });

  describe('#watchSelection', function() {
    describe('should set "pageChecked"', function() {
      var tests = Em.A([
        {pageContent: Em.A([]),m:'false if empty "pageContent"', e: false},
        {pageContent: Em.A([{isChecked: false}]),m:'false if not-empty "pageContent" and not all "isChecked" true', e: false},
        {pageContent: Em.A([{isChecked: true}]),m:'true if not-empty "pageContent" and all "isChecked" true', e: false}
      ]);
      tests.forEach(function(test) {
        it(test.m, function() {
          view.set('pageContent', test.pageContent);
          view.watchSelection();
          expect(view.get('pageChecked')).to.equal(test.e);
        });
      });
    });
    describe('should set "noNotRunningHostsSelected" and "selectedNotRunningHostsCount"', function() {
      var tests = Em.A([
        {pageContent: Em.A([]),content:Em.A([]),m:' - "true", "0" if content is empty',e:{selectedNotRunningHostsCount: 0, noNotRunningHostsSelected: true}},
        {pageContent: Em.A([]),content:Em.A([Em.Object.create({isChecked: false})]),m:' - "true", "0" if no one isChecked',e:{selectedNotRunningHostsCount: 0, noNotRunningHostsSelected: true}},
        {pageContent: Em.A([]),content:Em.A([Em.Object.create({isChecked: true}),Em.Object.create({isChecked: false})]),m:' - "false", "1" if one isChecked',e:{selectedNotRunningHostsCount: 1, noNotRunningHostsSelected: false}}
      ]);
      tests.forEach(function(test) {
        it(test.m, function() {
          view.set('pageContent', test.pageContent);
          view.set('content', test.content);
          view.watchSelection();
          expect(view.get('noNotRunningHostsSelected')).to.equal(test.e.noNotRunningHostsSelected);
          expect(view.get('selectedNotRunningHostsCount')).to.equal(test.e.selectedNotRunningHostsCount);
        });
      });
    });
  });

  describe('#watchSelectionOnce', function() {

    beforeEach(function () {
      sinon.spy(Em.run, 'once');
      view.watchSelectionOnce();
    });

    afterEach(function () {
      Em.run.once.restore();
    });

    it('should call "Em.run.once" one time', function() {
      expect(Em.run.once.calledOnce).to.equal(true);
      expect(Em.run.once.firstCall.args[1]).to.equal('watchSelection');
    });
  });

  describe('#selectedCategory', function() {
    it('should equal category with isActive = true', function() {
      view.get('categories').findProperty('hostsBootStatus', 'FAILED').set('isActive', true);
      expect(view.get('selectedCategory.hostsBootStatus')).to.equal('FAILED');
    });
  });

  describe('#onPageChecked', function() {
    var tests = Em.A([
      {
        selectionInProgress: true,
        pageContent: [Em.Object.create({isChecked: true}), Em.Object.create({isChecked: false})],
        pageChecked: true,
        m: 'shouldn\'t do nothing if selectionInProgress is true',
        e: [true, false]
      },
      {
        selectionInProgress: false,
        pageContent: [Em.Object.create({isChecked: true}), Em.Object.create({isChecked: false})],
        pageChecked: true,
        m: 'should set each isChecked to pageChecked value',
        e: [true, true]
      }
    ]);
    tests.forEach(function(test) {
      it(test.m, function() {
        v = App.WizardStep3View.create({
          'pageContent': test.pageContent,
          'pageChecked': test.pageChecked,
          'selectionInProgress': test.selectionInProgress
        });
        v.onPageChecked();
        expect(v.get('pageContent').mapProperty('isChecked')).to.eql(test.e);
      });
    });
  });

  describe('#didInsertElement', function() {
    beforeEach(function() {
      v = App.WizardStep3View.create({
        controller: Em.Object.create({
          loadStep: Em.K
        })
      });
      sinon.spy(v.get('controller'), 'loadStep');
      sinon.stub(v, '$').returns({
        on: Em.K
      });
    });
    afterEach(function() {
      v.get('controller').loadStep.restore();
      v.$.restore();
    });
    it('should call loadStep', function() {
      v.didInsertElement();
      expect(v.get('controller').loadStep.calledOnce).to.equal(true);
    });
  });

  describe('#categoryObject', function() {
    var o;
    beforeEach(function() {
      v = App.WizardStep3View.create();
      o = v.get('categoryObject').create();
    });

    describe('#label', function() {
      it('should use value and hostCount', function() {
        o.setProperties({
          value: 'abc',
          hostsCount: 3
        });
        expect(o.get('label')).to.equal('abc (3)');
      });
    });

    describe('#itemClass', function() {
      it('should depends on isActive', function() {
        o.set('isActive', true);
        expect(o.get('itemClass')).to.equal('active');
        o.set('isActive', false);
        expect(o.get('itemClass')).to.equal('');
      });
    });

  });

});

var wView;
describe('App.WizardHostView', function() {

  beforeEach(function() {
    wView = App.WizardHostView.create({
      hostInfo: {},
      controller: Em.Object.create({
        removeHost: Em.K,
        retryHost: Em.K
      })
    });
    sinon.spy(wView.get('controller'), 'retryHost');
    sinon.spy(wView.get('controller'), 'removeHost');
  });

  afterEach(function() {
    wView.get('controller').retryHost.restore();
    wView.get('controller').removeHost.restore();
  });

  describe('#retry', function() {
    it('should call controller.retryHost', function() {
      wView.retry();
      expect(wView.get('controller').retryHost.calledWith({})).to.equal(true);
      expect(wView.get('controller').retryHost.calledOnce).to.equal(true);
    });
  });

  describe('#remove', function() {
    it('should call controller.removeHost', function() {
      wView.remove();
      expect(wView.get('controller').removeHost.calledWith({})).to.equal(true);
      expect(wView.get('controller').removeHost.calledOnce).to.equal(true);
    });
  });

});
