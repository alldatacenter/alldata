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

var sort = require('views/common/sort_view');
require('utils/misc');
require('utils/string_utils');

describe('#wrapperView', function () {

  describe('#getSortFunc', function () {

    describe('number', function () {

      var property = Em.Object.create({type: 'number', name: 'lastTriggered'});

      Em.A([
          {
            a: Em.Object.create({lastTriggered: 1}),
            b: Em.Object.create({lastTriggered: 0}),
            order: true,
            e: 1
          },
          {
            a: Em.Object.create({lastTriggered: 1}),
            b: Em.Object.create({lastTriggered: 0}),
            order: false,
            e: -1
          },
          {
            a: Em.Object.create({lastTriggered: null}),
            b: Em.Object.create({lastTriggered: 1}),
            order: true,
            e: -Infinity
          },
          {
            a: Em.Object.create({lastTriggered: null}),
            b: Em.Object.create({lastTriggered: 1}),
            order: false,
            e: Infinity
          }
        ]).forEach(function (test, i) {
          it('test #' + (i + 1), function () {
            var func = sort.wrapperView.create().getSortFunc(property, test.order);
            expect(func(test.a, test.b)).to.equal(test.e);
          });
      });

    });

    describe('default', function () {

      var property = Em.Object.create({type: 'string', name: 'serviceName'});

      Em.A([
          {
            a: Em.Object.create({serviceName: 's1'}),
            b: Em.Object.create({serviceName: 's2'}),
            order: true,
            e: 1
          },
          {
            a: Em.Object.create({serviceName: 's1'}),
            b: Em.Object.create({serviceName: 's2'}),
            order: false,
            e: -1
          },
          {
            a: Em.Object.create({serviceName: 's1'}),
            b: Em.Object.create({serviceName: 's1'}),
            order: true,
            e: 0
          },
          {
            a: Em.Object.create({serviceName: null}),
            b: Em.Object.create({serviceName: 's2'}),
            order: true,
            e: 1
          },
          {
            a: Em.Object.create({serviceName: null}),
            b: Em.Object.create({serviceName: 's2'}),
            order: false,
            e: -1
          },
          {
            a: Em.Object.create({serviceName: 's1'}),
            b: Em.Object.create({serviceName: 'S2'}),
            order: true,
            e: 1
          },
          {
            a: Em.Object.create({serviceName: 's1'}),
            b: Em.Object.create({serviceName: 'S2'}),
            order: false,
            e: -1
          }
        ]).forEach(function (test, i) {
          it('test #' + (i + 1), function () {
            var func = sort.wrapperView.create().getSortFunc(property, test.order);
            expect(func(test.a, test.b)).to.equal(test.e);
          });
      });

      it('test non-string values', function () {
        property = Em.Object.create({type: 'string', name: 'enabled'});
        var func = sort.wrapperView.create().getSortFunc(property, true),
        a = Em.Object.create({enabled: false}),
        b = Em.Object.create({enabled: true});
        expect(func(a, b)).to.equal(1);
      });

    });

  });

  describe('#fieldView', function () {

    var fieldView, wrapperView;

    describe('#click', function () {

      beforeEach(function () {
        fieldView = sort.fieldView.create({
          controller: Em.Object.create({
            sortingColumn: null
          })
        });
        wrapperView = sort.wrapperView.create({
          childViews: [fieldView]
        });
        fieldView.reopen({'parentView': wrapperView});

        sinon.stub(wrapperView, 'sort', Em.K);
        sinon.stub(wrapperView, 'removeSortingObserver', Em.K);
        sinon.stub(wrapperView, 'addSortingObserver', Em.K);
      });

      afterEach(function () {
        wrapperView.sort.restore();
        wrapperView.removeSortingObserver.restore();
        wrapperView.addSortingObserver.restore();
      });

      it('should call sort function of wrapperView', function () {
        fieldView.click();
        expect(wrapperView.sort.calledOnce).to.be.true;
      });

      it('should call removeSortingObserver function of wrapperView if sortingColumn is absent in controller', function () {
        fieldView.reopen({
          controller: Em.Object.create({
            sortingColumn: {name: 'test'}
          })
        });
        fieldView.click();
        expect(wrapperView.removeSortingObserver.calledOnce).to.be.true;
      });

      it('should not call removeSortingObserver function of wrapperView if sortingColumn exists in controller', function () {
        fieldView.click();
        expect(wrapperView.removeSortingObserver.calledOnce).to.be.false;
      });

      it('should call addSortingObserver function of wrapperView', function () {
        fieldView.click();
        expect(wrapperView.addSortingObserver.calledOnce).to.be.true;
      });

    })
  });

  describe('#getSortedContent', function() {
    var wrapperView;
    var content = [
      Em.Object.create({
        id: 1
      }),
      Em.Object.create({
        id: 2
      })
    ];

    beforeEach(function() {
      wrapperView = sort.wrapperView.create({
        childViews: [],
        isSorting: false
      });
      sinon.stub(wrapperView, 'sort', function(arg1, arg2, arg3, arg4) {
        return arg4.reverse();
      });
    });
    afterEach(function() {
      wrapperView.sort.restore();
    });

    it('should return content without sorting', function() {
      expect(wrapperView.getSortedContent(content)).to.be.eql(content);
      expect(wrapperView.sort.called).to.be.false;
    });

    it('should return content with sorting', function() {
      wrapperView.set('childViews', [
        Em.Object.create({
          status: 'sorting_desc'
        })
      ]);
      expect(wrapperView.getSortedContent(content)).to.be.eql(content.reverse());
      expect(wrapperView.sort.calledWith(
        Em.Object.create({
          status: 'sorting_desc'
        }),
        true,
        true,
        content
      )).to.be.true;
    });
  });

});