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
require('views/main/charts/heatmap/heatmap_rack');

describe('App.MainChartsHeatmapRackView', function() {

  var view = App.MainChartsHeatmapRackView.create({
    templateName: ''
  });

  describe('#hostCssStyle', function () {
    var testCases = [
      {
        title: 'if hosts haven\'t been loaded yet then hostCssStyle should be have width 100%',
        rack: Em.Object.create({
          hosts: new Array(0),
          isLoaded: false
        }),
        result: "width:100%;float:left;"
      },
      {
        title: 'if hosts number is zero then hostCssStyle should be have width 10%',
        rack: Em.Object.create({
          hosts: new Array(0),
          isLoaded: true
        }),
        result: "width:10%;float:left;"
      },
      {
        title: 'if hosts number is one then hostCssStyle should be have width 99.5%',
        rack: Em.Object.create({
          hosts: new Array(1),
          isLoaded: true
        }),
        result: "width:99.5%;float:left;"
      },
      {
        title: 'if hosts number is ten then hostCssStyle should be have width 9.5%',
        rack: Em.Object.create({
          hosts: new Array(10),
          isLoaded: true
        }),
        result: "width:9.5%;float:left;"
      },
      {
        title: 'if hosts number is ten then hostCssStyle should be have width 10%',
        rack: Em.Object.create({
          hosts: new Array(11),
          isLoaded: true
        }),
        result: "width:10%;float:left;"
      }
    ];
    testCases.forEach(function (test) {
      it(test.title, function () {
        view.set('rack', test.rack);
        expect(view.get('hostCssStyle')).to.equal(test.result);
      });
    });
  });

});
