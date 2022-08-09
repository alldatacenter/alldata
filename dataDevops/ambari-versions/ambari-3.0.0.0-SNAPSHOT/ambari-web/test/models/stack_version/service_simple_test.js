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
require('models/stack_version/service_simple');

describe('App.ServiceSimple', function () {

  var serviceSimple;

  beforeEach(function () {
    serviceSimple = App.ServiceSimple.createRecord();
  });

  describe('#isHidden', function () {

    var cases = [
      {
        name: 'KERBEROS',
        doNotShowAndInstall: true,
        isHidden: true,
        title: 'Kerberos isn\'t displayed in wizard'
      },
      {
        name: 'ZOOKEEPER',
        doNotShowAndInstall: false,
        isHidden: false,
        title: 'ZooKeeper is displayed in wizard'
      }
    ];

    cases.forEach(function (item) {

      it(item.title, function () {
        serviceSimple.reopen({
          doNotShowAndInstall: item.doNotShowAndInstall
        });
        serviceSimple.set('name', item.name);
        expect(serviceSimple.get('isHidden')).to.equal(item.isHidden);
      });

    });

  });

  describe('#doNotShowAndInstall', function () {

    var cases = [
      {
        name: 'KERBEROS',
        isGangliaSupported: false,
        doNotShowAndInstall: true,
        title: 'Kerberos can\'t be installed from wizard'
      },
      {
        name: 'GANGLIA',
        isGangliaSupported: false,
        doNotShowAndInstall: true,
        title: 'Ganglia can\'t be installed from wizard by default'
      },
      {
        name: 'GANGLIA',
        isGangliaSupported: true,
        doNotShowAndInstall: false,
        title: 'Ganglia can be installed from wizard only if experimental feature is enabled'
      },
      {
        name: 'ZOOKEEPER',
        isGangliaSupported: false,
        doNotShowAndInstall: false,
        title: 'ZooKeeper can be installed from wizard'
      }
    ];

    cases.forEach(function (item) {

      describe(item.name, function () {

        beforeEach(function () {
          sinon.stub(App, 'get').withArgs('supports.installGanglia').returns(item.isGangliaSupported);
          serviceSimple.set('name', item.name);
        });

        afterEach(function () {
          App.get.restore();
        });

        it(item.title, function () {
          expect(serviceSimple.get('doNotShowAndInstall')).to.equal(item.doNotShowAndInstall);
        });

      });

    });

  });

});
