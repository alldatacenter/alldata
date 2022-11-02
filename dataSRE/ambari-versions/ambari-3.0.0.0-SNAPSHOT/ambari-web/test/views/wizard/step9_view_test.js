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
require('views/wizard/step9_view');

var v;
function getView() {
  return App.WizardStep9View.create({
    onStatus: function () {},
    content: [],
    pageContent: function () {
      return this.get('content');
    }.property('content')
  });
}

describe('App.WizardStep9View', function () {
  beforeEach(function () {
    v = App.WizardStep9View.create({
      controller: App.WizardStep9Controller.create()
    });
  });
  var view = getView();
  var testCases = [
    {
      title: 'none hosts',
      content: [],
      result: {
        "all": 0,
        "inProgress": 0,
        "warning": 0,
        "success": 0,
        "failed": 0
      }
    },
    {
      title: 'all hosts inProgress',
      content: [
        Em.Object.create({
          name: 'host1',
          status: 'in_progress'
        }),
        Em.Object.create({
          name: 'host2',
          status: 'info'
        }),
        Em.Object.create({
          name: 'host3',
          status: 'pending'
        })
      ],
      result: {
        "all": 3,
        "inProgress": 3,
        "warning": 0,
        "success": 0,
        "failed": 0
      }
    },
    {
      title: 'all hosts warning',
      content: [
        Em.Object.create({
          name: 'host1',
          status: 'warning'
        }),
        Em.Object.create({
          name: 'host2',
          status: 'warning'
        }),
        Em.Object.create({
          name: 'host3',
          status: 'warning'
        })
      ],
      result: {
        "all": 3,
        "inProgress": 0,
        "warning": 3,
        "success": 0,
        "failed": 0
      }
    },
    {
      title: 'all hosts success',
      content: [
        Em.Object.create({
          name: 'host1',
          status: 'success'
        }),
        Em.Object.create({
          name: 'host2',
          status: 'success'
        }),
        Em.Object.create({
          name: 'host3',
          status: 'success'
        })
      ],
      result: {
        "all": 3,
        "inProgress": 0,
        "warning": 0,
        "success": 3,
        "failed": 0
      }
    },
    {
      title: 'all hosts failed',
      content: [
        Em.Object.create({
          name: 'host1',
          status: 'failed'
        }),
        Em.Object.create({
          name: 'host2',
          status: 'failed'
        }),
        Em.Object.create({
          name: 'host3',
          status: 'heartbeat_lost'
        })
      ],
      result: {
        "all": 3,
        "inProgress": 0,
        "warning": 0,
        "success": 0,
        "failed": 3
      }
    },
    {
      title: 'first host is failed, second is warning, third is success',
      content: [
        Em.Object.create({
          name: 'host1',
          status: 'failed'
        }),
        Em.Object.create({
          name: 'host2',
          status: 'success'
        }),
        Em.Object.create({
          name: 'host3',
          status: 'warning'
        })
      ],
      result: {
        "all": 3,
        "inProgress": 0,
        "warning": 1,
        "success": 1,
        "failed": 1
      }
    },
    {
      title: 'two hosts is inProgress, one is success',
      content: [
        Em.Object.create({
          name: 'host1',
          status: 'pending'
        }),
        Em.Object.create({
          name: 'host2',
          status: 'in_progress'
        }),
        Em.Object.create({
          name: 'host3',
          status: 'success'
        })
      ],
      result: {
        "all": 3,
        "inProgress": 2,
        "warning": 0,
        "success": 1,
        "failed": 0
      }
    }
  ];

  describe('#countCategoryHosts', function () {
    testCases.forEach(function (test) {
      describe(test.title, function () {
        var _v;
        beforeEach(function () {
          _v = getView();
          _v.set('content', test.content);
          _v.countCategoryHosts();
        });

        Object.keys(test.result).forEach(function (categoryName) {
          it('`' + categoryName + '`', function () {
            expect(_v.get('categories').findProperty('hostStatus', categoryName).get('hostsCount')).to.equal(test.result[categoryName])
          });
        });

      });
    }, this);
  });

  describe('#doFilter', function () {
    testCases.forEach(function (test) {
      describe(test.title, function () {
        view.get('categories').forEach(function (category) {
          it('. Selected category - ' + category.get('hostStatus'), function () {
            view.set('content', test.content);
            view.reopen({selectedCategory: category});
            view.doFilter();
            expect(view.get('filteredContent').length).to.equal(test.result[category.get('hostStatus')])
          });
        })
      });
    }, this);
  });

  describe('#isStepCompleted', function () {
    it('should be true if progress is 100', function () {
      v.set('controller.progress', '100');
      expect(v.get('isStepCompleted')).to.equal(true);
    });
    it('should be false if progress isn\'t 100', function () {
      v.set('controller.progress', '50');
      expect(v.get('isStepCompleted')).to.equal(false);
    });
  });

  describe('#content', function () {

    var hosts = [{}, {}, {}];

    beforeEach(function () {
      sinon.stub(v, 'hostStatusObserver', Em.K);
      v.set('controller.hosts', hosts);
    });

    afterEach(function () {
      v.hostStatusObserver.restore();
    });

    it('should be equal to controller.hosts', function () {
      expect(v.get('content')).to.eql(hosts);
    });
  });

  describe('#categoryObject', function () {
    it('label should contains value and hostsCount', function () {
      var value = 'v',
        hostsCount = 10,
        o = v.get('categoryObject').create({value: value, hostsCount: hostsCount});
      expect(o.get('label')).to.equal(value + ' (' + hostsCount + ')');
    });
    it('itemClass should depends on isActive', function () {
      var o = v.get('categoryObject').create();
      o.set('isActive', false);
      expect(o.get('itemClass')).to.equal('');
      o.set('isActive', true);
      expect(o.get('itemClass')).to.equal('active');
    });
  });

  describe('#isHostHeartbeatLost', function () {
    Em.A([
        {
          hostsWithHeartbeatLost: [],
          m: 'should be false if hostsWithHeartbeatLost is empty',
          e: false
        },
        {
          hostsWithHeartbeatLost: [
            {},
            {}
          ],
          m: 'should be true if hostsWithHeartbeatLost contains some values',
          e: true
        }
      ]).forEach(function (test) {
        it(test.m, function () {
          v.set('controller.hostsWithHeartbeatLost', test.hostsWithHeartbeatLost);
          expect(v.get('isHostHeartbeatLost')).to.equal(test.e);
        })
      });
  });

  describe('#barWidth', function () {
    it('should depends on controller.progress', function () {
      var w = '25';
      v.set('controller.progress', w);
      expect(v.get('barWidth')).to.equal('width: ' + w + '%;');
    });
  });

  describe('#progressMessage', function () {
    it('should depends on controller.progress', function () {
      var w = '25';
      v.set('controller.progress', w);
      expect(v.get('progressMessage').contains(w)).to.equal(true);
    });
  });

  describe('#showAllHosts', function () {
    it('should set active to category with all hosts', function () {
      v.get('categories').findProperty('hostStatus', 'inProgress').set('isActive', true);
      v.showAllHosts();
      var allCategory = v.get('categories').findProperty('hostStatus', 'all');
      expect(allCategory.get('isActive')).to.equal(true);
      expect(v.get('categories').without(allCategory).everyProperty('isActive', false)).to.equal(true);
    });
  });

  describe('#didInsertElement', function () {
    beforeEach(function () {
      sinon.stub(v, 'onStatus', Em.K);
      sinon.stub(v.get('controller'), 'navigateStep', Em.K);
    });
    afterEach(function () {
      v.onStatus.restore();
      v.get('controller').navigateStep.restore();
    });
    it('should call onStatus', function () {
      v.didInsertElement();
      expect(v.onStatus.calledOnce).to.equal(true);
    });
    it('should call navigateStep', function () {
      v.didInsertElement();
      expect(v.get('controller').navigateStep.calledOnce).to.equal(true);
    });
  });

  describe('#selectCategory', function () {
    it('should set isActive true to selected category', function () {
      var event = {context: Em.Object.create({hostStatus: 'inProgress'})},
        c = v.get('categories').findProperty('hostStatus', 'inProgress');
      c.set('isActive', false);
      v.selectCategory(event);
      expect(c.get('isActive')).to.equal(true);
    });
  });

  describe('#onStatus', function () {
    Em.A([
        {
          status: 'success',
          e: {
            barColor: 'progress-bar-success',
            resultMsg: Em.I18n.t('installer.step9.status.success'),
            resultMsgColor: 'alert-success'
          }
        },
        {
          status: 'info',
          e: {
            barColor: 'progress-bar-info',
            resultMsg: ''
          }
        },
        {
          status: 'warning',
          e: {
            barColor: 'progress-bar-warning',
            resultMsg: Em.I18n.t('installer.step9.status.warning'),
            resultMsgColor: 'alert-warning'
          }
        },
        {
          status: 'failed',
          e: {
            barColor: 'progress-bar-danger',
            resultMsgColor: 'alert-danger'
          }
        }
      ]).forEach(function (test) {
        describe(test.status, function () {

          beforeEach(function () {
            v.set('controller.status', test.status);
            v.onStatus();
          });

          Object.keys(test.e).forEach(function (k) {
            it(k, function () {
              expect(v.get(k)).to.equal(test.e[k]);
            });
          });
        });
      });
    Em.A([
        {
          hostsWithHeartbeatLost: [
            {},
            {}
          ],
          startCallFailed: false,
          m: 'heartbeat lost for 2 hosts',
          resultMsg: Em.I18n.t('installer.step9.status.hosts.heartbeat_lost').format(2)
        },
        {
          hostsWithHeartbeatLost: [],
          startCallFailed: true,
          m: 'heartbeat not lost, startCallFailed true',
          resultMsg: Em.I18n.t('installer.step9.status.start.services.failed')
        },
        {
          hostsWithHeartbeatLost: [],
          startCallFailed: false,
          m: 'heartbeat not lost, startCallFailed false',
          resultMsg: Em.I18n.t('installer.step9.status.failed')
        }
      ]).forEach(function (test) {
        it(test.m, function () {
          v.set('controller.hostsWithHeartbeatLost', test.hostsWithHeartbeatLost);
          v.set('controller.startCallFailed', test.startCallFailed);
          v.set('controller.status', 'failed');
          v.onStatus();
          expect(v.get('resultMsg')).to.equal(test.resultMsg);
        });
      });
  });

  describe('#hostWithInstallFailed', function () {
    it('popup property failedHosts should be equal to hostsWithHeartbeatLost', function () {
      var hostsWithHeartbeatLost = [
        {},
        {}
      ];
      v.set('controller.hostsWithHeartbeatLost', hostsWithHeartbeatLost);
      var body = v.hostWithInstallFailed().get('bodyClass').create();
      expect(body.get('failedHosts')).to.eql(hostsWithHeartbeatLost);
    });
  });

});

var hv;
describe('App.HostStatusView', function () {
  beforeEach(function () {
    hv = App.HostStatusView.create();
  });
  var tests = [
    {
      p: 'isFailed',
      tests: [
        {
          obj: {
            status: 'failed',
            progress: 100
          },
          e: true
        },
        {
          obj: {
            status: 'failed',
            progress: 99
          },
          e: false
        },
        {
          obj: {
            status: 'success',
            progress: 100
          },
          e: false
        },
        {
          obj: {
            status: 'success',
            progress: 99
          },
          e: false
        }
      ]
    },
    {
      p: 'isSuccess',
      tests: [
        {
          obj: {
            status: 'success',
            progress: 100
          },
          e: true
        },
        {
          obj: {
            status: 'success',
            progress: 99
          },
          e: false
        },
        {
          obj: {
            status: 'failed',
            progress: 100
          },
          e: false
        },
        {
          obj: {
            status: 'failed',
            progress: 99
          },
          e: false
        }
      ]
    },
    {
      p: 'isWarning',
      tests: [
        {
          obj: {
            status: 'warning',
            progress: 100
          },
          e: true
        },
        {
          obj: {
            status: 'warning',
            progress: 99
          },
          e: false
        },
        {
          obj: {
            status: 'failed',
            progress: 100
          },
          e: false
        },
        {
          obj: {
            status: 'failed',
            progress: 99
          },
          e: false
        }
      ]
    }
  ];
  tests.forEach(function (test) {
    describe(test.p, function () {
      test.tests.forEach(function (t) {
        var hostStatusView = App.HostStatusView.create();
        it('obj.progress = ' + t.obj.progress + '; obj.status = ' + t.obj.status, function () {
          hostStatusView.set('obj', t.obj);
          expect(hostStatusView.get(test.p)).to.equal(t.e);
        });
      });
    });
  });

  describe('#barWidth', function () {
    it('should depends of obj.progress', function () {
      hv.set('obj', {progress: '25'});
      expect(hv.get('barWidth')).to.equal('width: 25%;');
    });
  });

  describe('#didInsertElement', function () {

    beforeEach(function () {
      sinon.stub(hv, 'onStatus', Em.K);
    });

    afterEach(function () {
      hv.onStatus.restore();
    });

    it('should call onStatus', function () {
      hv.didInsertElement();
      expect(hv.onStatus.calledOnce).to.equal(true);
    });
  });

  describe('#onStatus', function () {
    Em.A([
        {
          obj: {
            status: 'info'
          },
          e: {
            barColor: 'progress-bar-info'
          }
        },
        {
          obj: {
            status: 'warning'
          },
          e: {
            barColor: 'progress-bar-warning'
          }
        },
        {
          obj: {
            status: 'warning',
            progress: '100'
          },
          e: {
            barColor: 'progress-bar-warning',
            'obj.message': Em.I18n.t('installer.step9.host.status.warning')
          }
        },
        {
          obj: {
            status: 'failed'
          },
          e: {
            barColor: 'progress-bar-danger'
          }
        },
        {
          obj: {
            status: 'failed',
            progress: '100'
          },
          e: {
            barColor: 'progress-bar-danger',
            'obj.message': Em.I18n.t('installer.step9.host.status.failed')
          }
        },
        {
          obj: {
            status: 'heartbeat_lost'
          },
          e: {
            barColor: 'progress-bar-danger'
          }
        },
        {
          obj: {
            status: 'heartbeat_lost',
            progress: '100'
          },
          e: {
            barColor: 'progress-bar-danger',
            'obj.message': Em.I18n.t('installer.step9.host.heartbeat_lost')
          }
        }
      ]).forEach(function (test) {
        describe(JSON.stringify(test.obj), function () {

          beforeEach(function () {
            hv.set('obj', test.obj);
            hv.onStatus();
          });

          Object.keys(test.e).forEach(function (k) {
            it(k, function () {
              expect(hv.get(k)).to.equal(test.e[k]);
            });
          });
        });
      });
    Em.A([
        {
          obj: {
            status: 'success',
            progress: '100'
          },
          progress: '35',
          e: true
        },
        {
          obj: {
            status: 'success',
            progress: '100'
          },
          progress: '34',
          e: false
        },
        {
          obj: {
            status: 'success',
            progress: '99'
          },
          progress: '35',
          e: false
        },
        {
          obj: {
            status: 'failed',
            progress: '100'
          },
          progress: '35',
          e: false
        }
      ]).forEach(function (test) {
        describe(JSON.stringify(test.obj) + ' ' + test.progress, function() {
          beforeEach(function () {
            hv.setProperties({
              barColor: '',
              obj: test.obj
            });
            hv.set('obj.message', '');
            hv.set('controller', {progress: test.progress});
            hv.onStatus();
          });

          if (test.e) {
            it('completed successful', function () {
              expect(hv.get('obj.message')).to.be.equal(Em.I18n.t('installer.step9.host.status.success'));
              expect(hv.get('barColor')).to.be.equal('progress-bar-success');
            });
          }
          else {
            it('completed not successful', function () {
              expect(hv.get('obj.message')).to.be.not.equal(Em.I18n.t('installer.step9.host.status.success'));
              expect(hv.get('barColor')).to.be.not.equal('progress-bar-success');
            });
          }
        });
      });
  });

  describe('#hostLogPopup', function() {

    describe('#onClose', function() {

      beforeEach(function() {
        hv.set('controller', {currentOpenTaskId: 123});
        hv.set('obj', Em.Object.create());
        this.p = hv.hostLogPopup();
        sinon.spy(this.p, 'hide');
      });

      afterEach(function () {
        this.p.hide.restore();
      });

      it('popup should clear currentOpenTaskId', function() {
        this.p.onClose();
        expect(hv.get('controller.currentOpenTaskId')).to.equal(0);
      });

      it('onClose popup should hide popup', function() {
        this.p.onClose();
        expect(this.p.hide.calledOnce).to.equal(true);
      });

    });
  });

});
