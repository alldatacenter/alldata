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

describe('App.LogTailView', function() {
  var view;
  beforeEach(function () {
    view = App.LogTailView.create({});
  });

  describe('#didInsertElement', function () {
    beforeEach(function () {
      sinon.stub(view, 'infiniteScrollInit');
      sinon.stub(view, 'fetchRows').returns({then: function () {}});
      sinon.stub(view, 'subscribeResize');
    });

    afterEach(function () {
      view.infiniteScrollInit.restore();
      view.fetchRows.restore();
      view.subscribeResize.restore();
    });

    it('should call infiniteScrollInit', function () {
      view.didInsertElement();
      expect(view.infiniteScrollInit.calledOnce).to.be.true;
    });

    it('should call fetchRows', function () {
      view.didInsertElement();
      expect(view.fetchRows.calledOnce).to.be.true;
    });

    it('should call subscribeResize', function () {
      view.didInsertElement();
      expect(view.subscribeResize.calledOnce).to.be.true;
    });
  });

  describe('#_infiniteScrollHandler', function () {
    beforeEach(function () {
      sinon.stub(view, 'fetchRows').returns({then: function () {}});
    });

    afterEach(function () {
      view.fetchRows.restore();
    });

    it('should do nothing if scrollTop is not equal to 0', function () {
      view._infiniteScrollHandler({target: {}});
      expect(view.fetchRows.called).to.be.false;
    });

    it('should do nothing if scrollTop is not equal to 0 noOldLogs is true and oldLogsIsFetching is false', function () {
      sinon.stub(window, '$').returns({
        scrollTop: function () {
          return 0;
        },
        get: function () {
          return {
            clientHeight: 100
          }
        },
        prop: function () {
          return 15;
        },
        trigger: function () {
        }
      });
      view.set('noOldLogs', true);
      view.set('oldLogsIsFetching', false);
      view._infiniteScrollHandler({target: {}});
      expect(view.fetchRows.called).to.be.false;
      window.$.restore();
    });

    it('should call fetchRows if scrollTop is not equal to 0 noOldLogs is false and oldLogsIsFetching is false', function () {
      sinon.stub(window, '$').returns({
        scrollTop: function () {
          return 0;
        },
        get: function () {
          return {
            clientHeight: 100
          }
        },
        prop: function () {
          return 15;
        },
        trigger: function () {
        }
      });
      view.set('noOldLogs', false);
      view.set('oldLogsIsFetching', false);
      view._infiniteScrollHandler({target: {}});
      expect(view.fetchRows.called).to.be.true;
      window.$.restore();
    });
  });

  describe('#willDestroyElement', function () {
    it('should call stopLogPolling unsubscribeResize and clear log rows', function () {
      sinon.stub(view, 'stopLogPolling');
      sinon.stub(view, 'unsubscribeResize');
      var logRows = view.get('logRows');
      sinon.stub(logRows, 'clear');
      view.willDestroyElement();
      expect(view.stopLogPolling.called).to.be.true;
      expect(view.unsubscribeResize.called).to.be.true;
      expect(logRows.clear.called).to.be.true;
      view.stopLogPolling.restore();
      view.unsubscribeResize.restore();
      logRows.clear.restore();
    });
  });

  describe('#unsubscribeResize', function () {
    var $ = {off: function () {}};
    beforeEach(function () {
      sinon.stub($, 'off');
      sinon.stub(window, '$').returns($);
    });

    afterEach(function () {
      $.off.restore();
      window.$.restore();
    });

    it('should do nothing if autoResize is false', function () {
      view.set('autoResize', false);
      view.unsubscribeResize();
      expect($.off.called).to.be.false;
    });

    it('should call off if autoResize is true', function () {
      view.set('autoResize', true);
      view.unsubscribeResize();
      expect($.off.called).to.be.true;
    });
  });

  describe('#subscribeResize', function () {
    var $ = {on: function () {}};
    beforeEach(function () {
      sinon.stub($, 'on');
      sinon.stub(view, 'resizeHandler')
      sinon.stub(window, '$').returns($);
    });

    afterEach(function () {
      $.on.restore();
      view.resizeHandler.restore();
      window.$.restore();
    });

    it('should do nothing if autoResize is false', function () {
      view.set('autoResize', false);
      view.subscribeResize();
      expect($.on.called).to.be.false;
      expect(view.resizeHandler.called).to.be.false;
    });

    it('should call on and resizeHandler if autoResize is true', function () {
      view.set('autoResize', true);
      view.subscribeResize();
      expect($.on.called).to.be.true;
      expect(view.resizeHandler.called).to.be.true;
    });
  });

  describe('#fetchRowsSuccess', function () {
    beforeEach(function () {
      sinon.stub(view, 'infiniteScrollSetDataAvailable');
    });

    afterEach(function () {
      view.infiniteScrollSetDataAvailable.restore();
    });

    it('should call infiniteScrollSetDataAvailable and return empty array if logList is not available', function () {
      var result = view.fetchRowsSuccess({});
      expect(result).to.be.eql([]);
      expect(view.infiniteScrollSetDataAvailable.called).to.be.true;
    });

    it('should call infiniteScrollSetDataAvailable and return empty array if logList is empty array', function () {
      var result = view.fetchRowsSuccess({logList: []});
      expect(result).to.be.eql([]);
      expect(view.infiniteScrollSetDataAvailable.called).to.be.true;
    });

  });

  describe('#saveLastTimestamp', function () {
    it('should set 0 if no logworks or they are empty', function () {
      view.saveLastTimestamp([]);
      expect(view.get('lastLogTime')).to.be.equal(0);
    });

    it('should set first logtime to lastLogTime', function () {
      view.saveLastTimestamp([Em.Object.create({logtime: 1}), Em.Object.create({logtime: 2})]);
      expect(view.get('lastLogTime')).to.be.equal(1);
    });
  });

  describe('#currentPage', function () {
    it('should return start index and page size', function () {
      view.set('selectedTailCount', 20);
      expect(view.currentPage()).to.be.eql({
        startIndex: 0,
        pageSize: 20
      });
    });
  });

  describe('#nextPage', function() {
    it('should increase startIndex on selectedTaiCount and return start index and page size', function() {
      view.set('selectedTailCount', 20);
      view.set('startIndex', 1);
      expect(view.nextPage()).to.be.eql({
        startIndex: 21,
        pageSize: 20
      });
    });

    it('should and return start index not less than 0 and page size', function() {
      view.set('selectedTailCount', 20);
      view.set('startIndex', -30);
      expect(view.nextPage()).to.be.eql({
        startIndex: 0,
        pageSize: 20
      });
    });
  });

  describe('#oldestLogs', function () {
    it('should return log rows length as start index and current page size', function () {
      view.set('logRows', [{}, {}]);
      view.set('selectedTailCount', 20);
      expect(view.oldestLogs()).to.be.eql({
        startIndex: 2,
        pageSize: 20
      });
    });
  });

  describe('#startLogPolling', function () {
    beforeEach(function () {
      view.set('pollLogTimeoutId', null);
    });

    it('should do nothing if no pollLogs', function () {
      view.set('pollLogs', null);
      expect(view.get('pollLogTimeoutId')).to.be.equal(null);
    });

    it('should do nothing if state is destroyed', function () {
      view.set('pollLogs', [{}]);
      view.set('state', 'destroyed');
      expect(view.get('pollLogTimeoutId')).to.be.equal(null);
    });

    it('should start polling if pollLogs are available and state is not destroyed', function () {
      view.set('pollLogs', [{}]);
      view.set('state', '');
      expect(view.get('pollLogTimeoutId')).to.be.truthy;
    });
  });

  describe('#stopLogPolling', function () {
    beforeEach(function () {
      sinon.stub(window, 'clearTimeout');
    });

    afterEach(function () {
      window.clearTimeout.restore();
    });

    it('should do nothing if no pollLogs', function () {
      view.set('pollLogs', null);
      view.stopLogPolling();
      expect(window.clearTimeout.called).to.be.false;
    });

    it('should do nothing if pollLogTimeoutId is null', function () {
      view.set('pollLogs', [{}]);
      view.set('pollLogTimeoutId', null);
      view.stopLogPolling();
      expect(window.clearTimeout.called).to.be.false;
    });

    it('should clear timeout if pollLogTimeoutId and pollLogs', function () {
      view.set('pollLogs', [{}]);
      view.set('pollLogTimeoutId', 1);
      view.stopLogPolling();
      expect(window.clearTimeout.called).to.be.true;
      expect(window.clearTimeout.calledWith(1)).to.be.true;
    });
  });
});