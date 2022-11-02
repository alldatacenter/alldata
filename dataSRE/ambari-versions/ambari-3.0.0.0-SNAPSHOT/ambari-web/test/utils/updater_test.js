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
require('utils/updater');
describe('utils/updater', function () {
  beforeEach(function () {
    this.clock = sinon.useFakeTimers();
    sinon.stub(App.router, "get").returns('test');
    App.updater.run.restore();
    sinon.spy(App.updater, 'run');
    App.updater.immediateRun.restore();
    sinon.spy(App.updater, 'immediateRun');
  });

  var tests = {
    t1: {
      obj: Em.Object.create({
        method: sinon.spy(),
        isWorking: true
      }),
      m: 'method called once with default interval in 15 000 ms'
    },
    t2: {
      obj: Em.Object.create({
        method: function () {
        }
      }),
      m: 'should return false if key name is invalid or absent'
    },
    t3: {
      obj: Em.Object.create({
        method2: sinon.spy(),
        isWorking: true
      }),
      m: 'method should be called immediately'
    },
    t4: {
      obj: Em.Object.create({
        method3: sinon.spy(),
        isWorking: true
      }),
      m: 'method call should be ignored if `isWorking` set to false'
    },
    t5: {
      obj: Em.Object.create({
        method4: sinon.spy(),
        isWorking: true
      }),
      m: 'method call should be ignored if urlPattern is not matching router location'
    }
  };

  it(tests.t1.m, function () {
    App.updater.run(tests.t1.obj, 'method', 'isWorking');
    this.clock.tick(3600000 * 1.5);
    expect(tests.t1.obj.method.called).to.be.ok;
  });

  it(tests.t2.m, function () {
    var methodCall = App.updater.run(tests.t2.obj, 'method', 'isWorking');
    expect(methodCall).to.be.false;
  });

  it(tests.t3.m, function () {
    App.updater.run(tests.t3.obj, 'method2', 'isWorking');
    App.updater.immediateRun('method2');
    expect(tests.t3.obj.method2.called).to.be.ok;
  });

  it(tests.t4.m, function () {
    App.updater.run(tests.t4.obj, 'method3', 'isWorking');
    this.clock.tick(10000);
    tests.t4.obj.set('isWorking', false);
    this.clock.tick(5000);
    expect(tests.t4.obj.method3.called).to.be.false;
  });

  it(tests.t5.m, function () {
    App.updater.run(tests.t5.obj, 'method4', 'isWorking', 15000, 'pattern');
    this.clock.tick(15000);
    expect(tests.t5.obj.method4.called).to.be.false;
  });

  afterEach(function () {
    this.clock.restore();
    App.router.get.restore();
  });
});