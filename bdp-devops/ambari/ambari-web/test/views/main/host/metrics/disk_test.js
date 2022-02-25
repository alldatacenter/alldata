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
require('views/main/host/metrics/disk');

describe('App.ChartHostMetricsDisk', function () {

  var view;

  beforeEach(function () {
    view = App.ChartHostMetricsDisk.create();
  });

  describe('#getData', function () {

    var cases = [
      {
        data: {},
        result: {},
        title: 'no metrics data'
      },
      {
        data: {
          metrics: null
        },
        result: {
          metrics: null
        },
        title: 'malformed metrics data'
      },
      {
        data: {
          metrics: {}
        },
        result: {
          metrics: {}
        },
        title: 'no metrics data'
      },
      {
        data: {
          metrics: {
            part_max_used: 0,
            disk: {}
          }
        },
        result: {
          metrics: {
            part_max_used: 0,
            disk: {
              part_max_used: 0
            }
          }
        },
        title: 'part_max_used = 0'
      },
      {
        data: {
          metrics: {
            part_max_used: 1,
            disk: {}
          }
        },
        result: {
          metrics: {
            part_max_used: 1,
            disk: {
              part_max_used: 1
            }
          }
        },
        title: 'part_max_used != 0'
      },
      {
        data: {
          metrics: {
            part_max_used: 1
          }
        },
        result: {
          metrics: {
            part_max_used: 1
          }
        },
        title: 'no metrics.disk data'
      },
      {
        data: {
          metrics: {
            part_max_used: 1,
            disk: null
          }
        },
        result: {
          metrics: {
            part_max_used: 1,
            disk: null
          }
        },
        title: 'malformed metrics.disk data'
      }
    ];

    cases.forEach(function (item) {
      it(item.title, function () {
        view.getData(item.data);
        expect(item.data).to.eql(item.result);
      });
    });

  });

});
