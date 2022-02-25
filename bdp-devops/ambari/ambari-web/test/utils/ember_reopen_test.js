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

require('utils/ember_reopen');

describe('Ember functionality extension', function () {

  describe('#Em.View', function () {

    var view,
      cases = [
        {
          result: {
            p0: 'v3',
            p1: 'v4',
            p2: 'v5'
          },
          title: 'active view'
        },
        {
          result: {
            p0: 'v0',
            p1: 'v1',
            p2: 'v2'
          },
          propertyToSet: 'isDestroyed',
          title: 'destroyed view'
        },
        {
          result: {
            p0: 'v0',
            p1: 'v1',
            p2: 'v2'
          },
          propertyToSet: 'isDestroying',
          title: 'view being destroyed'
        }
      ];

    beforeEach(function () {
      view = Em.View.create({
        isDestroyed: false,
        isDestroying: false,
        p0: 'v0',
        p1: 'v1',
        p2: 'v2'
      });
    });

    cases.forEach(function (item) {
      it(item.title, function () {
        if (item.propertyToSet) {
          view.set(item.propertyToSet, true);
        }
        view.set('p0', 'v3');
        view.setProperties({
          p1: 'v4',
          p2: 'v5'
        });
        expect(view.getProperties(['p0', 'p1', 'p2'])).to.eql(item.result);
      });
    });

  });

  describe('#Em.Route', function() {
    describe('#serializeQueryParams', function() {
      var route,
          cases = [
        {
          m: 'No query params',
          params: undefined,
          e: {
            result: {query: ''},
            serializedQuery: {}
          }
        },
        {
          m: 'Query params ?param1=value1&param2=value2',
          params: { query: '?param1=value1&param2=value2'},
          e: {
            result: {query: '?param1=value1&param2=value2'},
            serializedQuery: {param1: 'value1', param2: 'value2'}
          }
        },
        {
          m: 'Query params with encodedComponent ?param1=value1%30&param2=value2',
          params: { query: '?param1=value1%30&param2=value2'},
          e: {
            result: {query: '?param1=value1%30&param2=value2'},
            serializedQuery: {param1: 'value10', param2: 'value2'}
          }
        }
      ];

      beforeEach(function() {
        route = Ember.Route.create({
          route: 'demo:query',
          serialize: function(router, params) {
            return this.serializeQueryParams(router, params, 'testController');
          }
        });
      });

      afterEach(function() {
        route.destroy();
        route = null;
      });

      cases.forEach(function(test) {
        it(test.m, function() {
          var ctrl = Em.Object.create({});
          var router = Em.Object.create({
            testController: ctrl
          });
          var ret = route.serialize(router, test.params);
          expect(ret).to.be.eql(test.e.result);
          expect(ctrl.get('serializedQuery')).to.be.eql(test.e.serializedQuery);
        });
      });
    });
  });

  describe('#expand_properties', function () {

    var body;
    var tests = [
      {
        keys: ['a', 'b'],
        expandedKeys: ['a', 'b']
      },
      {
        keys: ['a.b', 'b'],
        expandedKeys: ['a.b', 'b']
      },
      {
        keys: ['a', 'a.b'],
        expandedKeys: ['a', 'a.b']
      },
      {
        keys: ['a.b', 'a.b'],
        expandedKeys: ['a.b']
      },
      {
        keys: ['a.b', 'b.c'],
        expandedKeys: ['a.b', 'b.c']
      },
      {
        keys: ['a.{b,c}'],
        expandedKeys: ['a.b', 'a.c']
      },
      {
        keys: ['{a,b}.c'],
        expandedKeys: ['a.c', 'b.c']
      },
      {
        keys: ['a.{b,c,d}'],
        expandedKeys: ['a.b', 'a.c', 'a.d']
      },
      {
        keys: ['a.@each.{b,c}'],
        expandedKeys: ['a.@each.b', 'a.@each.c']
      },
      {
        keys: ['a.{b.[],c}'],
        expandedKeys: ['a.b.[]', 'a.c']
      },
      {
        keys: ['a.{b,c.[]}'],
        expandedKeys: ['a.b', 'a.c.[]']
      },
      {
        keys: ['a.{b,c.[],b}'],
        expandedKeys: ['a.b', 'a.c.[]']
      },
      {
        keys: ['{a,b}.{c,d}'],
        expandedKeys: ['a.d', 'b.d', 'a.c', 'b.c']
      }
    ];

    beforeEach(function () {
      body = function () {};
    });

    describe('#computed properties', function() {

      describe('#Ember.computed', function () {
        tests.forEach(function(test) {
          it(JSON.stringify(test.keys), function () {
            var args = test.keys.slice(0);
            args.push(body);
            var cp = Ember.computed.apply(null, args);
            expect(cp._dependentKeys).to.be.eql(test.expandedKeys);
          });
        });
      });

      describe('#function(){}.property', function () {
        tests.forEach(function(test) {
          it(JSON.stringify(test.keys), function () {
            var cp = body.property.apply(null, test.keys);
            expect(cp._dependentKeys).to.be.eql(test.expandedKeys);
          });
        });
      });

    });

    describe('#observers', function () {

      describe('#Ember.observer', function () {
        tests.forEach(function(test) {
          it(JSON.stringify(test.keys), function () {
            var args = [body].concat(test.keys);
            var obs = Ember.observer.apply(null, args);
            expect(obs.__ember_observes__).to.be.eql(test.expandedKeys);
          });
        });
      });

      describe('#function(){}.observes', function () {
        tests.forEach(function(test) {
          it(JSON.stringify(test.keys), function () {
            var obs = body.observes.apply(body, test.keys);
            expect(obs.__ember_observes__).to.be.eql(test.expandedKeys);
          });
        });
      });

    });

  });

});
