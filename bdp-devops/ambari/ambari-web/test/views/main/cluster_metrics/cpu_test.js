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
require('views/main/admin');

describe('App.ChartClusterMetricsCPU', function () {
  var view;

  beforeEach(function () {
    view = App.ChartClusterMetricsCPU.create();
  });

  describe('#getData', function () {
    it('it should return empty array if there is no series template data', function () {
      expect(view.getData({})).to.be.eql([]);
    });

    it('it should properly map data', function () {
      expect(view.getData({
        metrics: {
          cpu: {
            test1: {val: 'test1'},
            test2: {val: 'test2'}
          }
        }
      })).to.be.eql([{name: 'test1', data: {val: 'test1'}}, {name: 'test2', data: {val: 'test2'}}]);
    });

    it('it should properly map data and put last element with Idle key', function () {
      expect(view.getData({
        metrics: {
          cpu: {
            test1: {val: 'test1'},
            Idle_test: {val: 'Idle_test'},
            test2: {val: 'test2'}
          }
        }
      })).to.be.eql([
        {name: 'test1', data: {val: 'test1'}},
        {name: 'test2', data: {val: 'test2'}},
        {name: 'Idle_test', data: {val: 'Idle_test'}},
      ]);
    });
  });
});