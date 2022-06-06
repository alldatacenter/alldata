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
require('utils/configs/modification_handlers/modification_handler');

describe('App.ServiceConfigModificationHandler', function () {

  var handler = App.ServiceConfigModificationHandler.create();

  describe('#getConfig', function () {

    var allConfigs = [
        Em.Object.create({
          serviceName: 's',
          configs: [
            Em.Object.create({
              name: 'c1',
              filename: 'f1'
            })
          ]
        })
      ],
      cases = [
        {
          configName: 'c0',
          result: undefined,
          title: 'property isn\'t defined'
        },
        {
          configName: 'c1',
          result: Em.Object.create({
            name: 'c1',
            filename: 'f1'
          }),
          title: 'property is defined, filename isn\'t passed'
        },
        {
          configName: 'c1',
          configFilename: 'f1',
          result: Em.Object.create({
            name: 'c1',
            filename: 'f1'
          }),
          title: 'property is defined, filename is passed'
        },
        {
          configName: 'c1',
          configFilename: 'f2',
          result: undefined,
          title: 'property is defined, filenames don\'t match'
        }
      ];

    cases.forEach(function (item) {
      it(item.title, function () {
        expect(handler.getConfig(allConfigs, item.configName, item.configFilename, 's')).to.eql(item.result);
      });
    });

  });

});
