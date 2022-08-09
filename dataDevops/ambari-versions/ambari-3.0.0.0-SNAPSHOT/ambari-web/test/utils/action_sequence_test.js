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
require('utils/action_sequence');

describe('App.actionSequence', function () {

  var actionSequence;

  beforeEach(function () {
    actionSequence = App.actionSequence.create();
  });

  describe('#setSequence', function () {

    var cases = [
        {
          sequenceIn: [{}, {}],
          sequenceOut: [{}, {}],
          title: 'array passed'
        },
        {
          sequenceIn: {
            '0': {},
            '1': {},
            'length': 2
          },
          sequenceOut: [{}],
          title: 'array-like object passed'
        },
        {
          sequenceIn: 0,
          sequenceOut: [{}],
          title: 'primitive passed'
        }
      ],
      result;

    cases.forEach(function (item) {

      describe(item.title, function () {

        beforeEach(function () {
          actionSequence.set('sequence', [{}]);
          result = actionSequence.setSequence(item.sequenceIn);
        });

        it('should return context', function () {
          expect(result).to.eql(actionSequence);
        });

        it('sequence property', function () {
          expect(actionSequence.get('sequence')).to.eql(item.sequenceOut);
        });

      });

    });

  });

  describe('#start', function () {

    beforeEach(function () {
      actionSequence.setProperties({
        actionCounter: 0,
        sequence: [{}]
      });
      sinon.stub(actionSequence, 'runNextAction', Em.K);
      actionSequence.start();
    });

    afterEach(function () {
      actionSequence.runNextAction.restore();
    });

    it('should set the counter', function () {
      expect(actionSequence.get('actionCounter')).to.equal(1);
    });

    it('should start the sequence', function () {
      expect(actionSequence.runNextAction.calledOnce).to.be.true;
    });

    it('should call runNextAction with correct arguments', function () {
      expect(actionSequence.runNextAction.calledWith(0, null)).to.be.true;
    });

  });

  describe('#onFinish', function () {

    var cases = [
        {
          callbackIn: Em.isNone,
          callbackOut: Em.isNone,
          title: 'function passed'
        },
        {
          callbackIn: 'function () {}',
          callbackOut: Em.clb,
          title: 'array-like object passed'
        },
        {
          callbackIn: 'function () {}',
          callbackOut: Em.clb,
          title: 'primitive passed'
        }
      ],
      result;

    cases.forEach(function (item) {

      describe(item.title, function () {

        beforeEach(function () {
          actionSequence.set('finishedCallback', Em.clb);
          result = actionSequence.onFinish(item.callbackIn);
        });

        it('should return context', function () {
          expect(result).to.eql(actionSequence);
        });

        it('finishedCallback property', function () {
          expect(actionSequence.get('finishedCallback')).to.eql(item.callbackOut);
        });

      });

    });

  });

  describe('#runNextAction', function () {

    var actions = {
        callback: Em.K,
        sync: function (prevResponse) {
          actions.callback(prevResponse);
          return prevResponse;
        },
        async: function (prevResponse) {
          actions.callback(prevResponse);
          return {
            done: function (callback) {
              return callback.call(this, prevResponse);
            }
          };
        }
      },
      prevResponse = {},
      cases = [
        {
          index: 0,
          actionCounter: 0,
          sequence: [
            {
              callback: actions.sync,
              type: 'sync'
            }
          ],
          actionCallCount: 0,
          title: 'no iterations left (case 1)'
        },
        {
          index: 3,
          actionCounter: 3,
          sequence: [
            {
              callback: actions.sync,
              type: 'sync'
            },
            {
              callback: actions.sync,
              type: 'sync'
            },
            {
              callback: actions.sync,
              type: 'sync'
            }
          ],
          actionCallCount: 0,
          title: 'no iterations left (case 2)'
        },
        {
          index: 1,
          actionCounter: 3,
          sequence: [
            {
              callback: actions.sync,
              type: 'sync'
            },
            {
              callback: actions.sync,
              type: 'sync'
            },
            {
              callback: actions.sync,
              type: 'sync'
            }
          ],
          actionCallCount: 2,
          title: 'starting from the middle'
        },
        {
          index: 0,
          actionCounter: 2,
          sequence: [
            {
              callback: actions.sync,
              type: 'sync'
            },
            {
              callback: actions.sync,
              type: 'sync'
            },
            {
              callback: actions.sync,
              type: 'sync'
            }
          ],
          actionCallCount: 2,
          title: 'ending at the middle'
        },
        {
          index: 0,
          actionCounter: 3,
          sequence: [
            {
              callback: actions.sync,
              type: 'sync'
            },
            {
              callback: actions.sync,
              type: 'sync'
            },
            {
              callback: actions.sync,
              type: 'sync'
            }
          ],
          actionCallCount: 3,
          title: 'all iterations'
        },
        {
          index: 0,
          actionCounter: 3,
          sequence: [
            {
              callback: actions.sync,
              type: 'sync'
            },
            {
              callback: actions.async,
              type: 'async'
            },
            {
              callback: actions.sync,
              type: 'sync'
            }
          ],
          actionCallCount: 3,
          title: 'asynchronous action'
        }
      ];

    cases.forEach(function (item) {

      describe(item.title, function () {

        beforeEach(function () {
          sinon.spy(actions, 'callback');
          sinon.stub(actionSequence, 'finishedCallback', Em.K);
          actionSequence.setProperties({
            context: actionSequence,
            actionCounter: item.actionCounter,
            sequence: item.sequence
          });
          actionSequence.runNextAction(item.index, prevResponse);
        });

        afterEach(function () {
          actions.callback.restore();
          actionSequence.finishedCallback.restore();
        });

        it('number of calls', function () {
          expect(actions.callback.callCount).to.equal(item.actionCallCount);
        });

        if (item.actionCallCount) {
          it('argument passed to callback', function () {
            expect(actions.callback.alwaysCalledWith(prevResponse)).to.be.true;
          });
        }

        it('finish callback', function () {
          expect(actionSequence.finishedCallback.calledOnce).to.be.true;
        });

      });
    });

  });

});
