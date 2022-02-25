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

var lazyLoading = require('utils/lazy_loading');

describe('lazy_loading', function () {

  describe('#run()', function () {
    var context = Em.Object.create({isLoaded: false});
    var options = {
      destination: [],
      source: [{'test':'test'}],
      context: context
    };
    it('load one item', function () {
      lazyLoading.run(options);
      expect(options.destination[0]).to.eql(options.source[0]);
      expect(context.get('isLoaded')).to.equal(true);
    });

    var testsInfo = [
      {
        title: 'load 11 item with initSize - 11',
        result: true,
        initSize: 11,
        destinationLength: 11,
        destination: [],
        source: [{i:1}, {i:2}, {i:3}, {i:4}, {i:5}, {i:6}, {i:7}, {i:8}, {i:9}, {i:10},{i:11}],
        context: Em.Object.create()
      },
      {
        title: 'load 11 item with initSize - 12',
        result: true,
        initSize: 12,
        destinationLength: 11,
        destination: [],
        source: [{i:1}, {i:2}, {i:3}, {i:4}, {i:5}, {i:6}, {i:7}, {i:8}, {i:9}, {i:10},{i:11}],
        context: Em.Object.create()
      },
      {//items will be completely loaded on next iteration of pushing chunk
        title: 'load 11 item with initSize - 10',
        result: false,
        initSize: 10,
        destinationLength: 10,
        destination: [],
        source: [{i:1}, {i:2}, {i:3}, {i:4}, {i:5}, {i:6}, {i:7}, {i:8}, {i:9}, {i:10},{i:11}],
        context: Em.Object.create({isLoaded: false})
      }
    ];
    testsInfo.forEach(function(test){
      it(test.title, function () {
        lazyLoading.run(test);
        expect(test.destinationLength).to.equal(test.destination.length);
        expect(test.context.get('isLoaded')).to.equal(test.result);
      });
    });
  });

  describe('#divideIntoChunks()', function () {
    var testsInfo = [
      {
        title: 'load 11 item with chunkSize - 3',
        chunkSize: 3,
        source: [{i:1}, {i:2}, {i:3}, {i:4}, {i:5}, {i:6}, {i:7}, {i:8}, {i:9}, {i:10},{i:11}],
        chunks: [[{i:1}, {i:2}, {i:3}], [{i:4}, {i:5}, {i:6}], [{i:7}, {i:8}, {i:9}], [{i:10},{i:11}]]
      },
      {
        title: 'load 11 item with chunkSize - 0',
        chunkSize: 0,
        source: [{i:1}, {i:2}, {i:3}, {i:4}, {i:5}, {i:6}, {i:7}, {i:8}, {i:9}, {i:10},{i:11}],
        chunks: [[{i:1}, {i:2}, {i:3}, {i:4}, {i:5}, {i:6}, {i:7}, {i:8}, {i:9}, {i:10},{i:11}]]
      },
      {
        title: 'load 11 item with chunkSize - 1',
        chunkSize: 1,
        source: [{i:1}, {i:2}, {i:3}, {i:4}, {i:5}, {i:6}, {i:7}, {i:8}, {i:9}, {i:10},{i:11}],
        chunks: [[{i:1}], [{i:2}], [{i:3}], [{i:4}], [{i:5}], [{i:6}], [{i:7}], [{i:8}], [{i:9}], [{i:10}], [{i:11}]]
      },
      {
        title: 'load 11 item with chunkSize - 11',
        chunkSize: 0,
        source: [{i:1}, {i:2}, {i:3}, {i:4}, {i:5}, {i:6}, {i:7}, {i:8}, {i:9}, {i:10},{i:11}],
        chunks: [[{i:1}, {i:2}, {i:3}, {i:4}, {i:5}, {i:6}, {i:7}, {i:8}, {i:9}, {i:10},{i:11}]]
      }
    ];
    testsInfo.forEach(function(test){
      it(test.title, function () {
        var chunks = lazyLoading.divideIntoChunks(test.source, test.chunkSize);
        expect(chunks).to.eql(test.chunks);
      });
    });
  });

  describe("#pushChunk()", function() {
    beforeEach(function () {
      this.clock = sinon.useFakeTimers();
      sinon.spy(lazyLoading, 'pushChunk');
    });
    afterEach(function () {
      this.clock.restore();
      lazyLoading.pushChunk.restore();
    });
    it("last chunk", function() {
      this.clock = sinon.useFakeTimers();

      var destination = [],
          chunks = [[1]],
          context = Em.Object.create(),
          instance = {
            context: context,
            timeoutRef: null,
            terminate: Em.K
          };
      lazyLoading.pushChunk(chunks, 0, 10, destination, instance);
      this.clock.tick(10);
      expect(destination[0]).to.equal(1);
      expect(context.get('isLoaded')).to.be.true;
      expect(lazyLoading.pushChunk.calledTwice).to.be.false;
    });
    it("two chunks", function() {
      this.clock = sinon.useFakeTimers();
      var destination = [],
        chunks = [[1], [2]],
        context = Em.Object.create(),
        instance = {
          context: context,
          timeoutRef: null,
          terminate: Em.K
        };
      lazyLoading.pushChunk(chunks, 0, 10, destination, instance);
      this.clock.tick(20);
      expect(destination.length).to.equal(2);
      expect(context.get('isLoaded')).to.be.true;
      expect(lazyLoading.pushChunk.calledTwice).to.be.true;
    });
    it("terminated chunks", function() {
      this.clock = sinon.useFakeTimers();
      var destination = [],
        chunks = [[1]],
        context = Em.Object.create({isLoaded: false}),
        instance = {
          context: context,
          timeoutRef: null,
          terminate: Em.K
        };
      lazyLoading.pushChunk(chunks, 0, 10, destination, instance);
      clearTimeout(instance.timeoutRef);
      this.clock.tick(10);
      expect(destination.length).to.empty;
      expect(context.get('isLoaded')).to.be.false;
      expect(lazyLoading.pushChunk.calledTwice).to.be.false;
    });
  });

  describe("#terminate()", function() {
    before(function () {
      sinon.spy(lazyLoading, 'pushChunk');
      this.clock = sinon.useFakeTimers();
    });
    after(function () {
      lazyLoading.pushChunk.restore();
      this.clock.restore();
    });
    it("loading terminated", function() {
      var context = Em.Object.create({isLoaded: false});
      var options = {
        destination: [],
        source: [1, 2],
        delay: 10,
        chunkSize: 1,
        initSize: 1,
        context: context
      };

      var ll = lazyLoading.run(options);
      lazyLoading.terminate(ll);
      this.clock.tick(10);
      expect(options.destination.length).to.equal(1);
      expect(context.get('isLoaded')).to.be.false;
      expect(lazyLoading.pushChunk.calledTwice).to.be.false;
    });
  });
});
