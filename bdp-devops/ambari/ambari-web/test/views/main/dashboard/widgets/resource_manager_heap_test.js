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

describe('App.ResourceManagerHeapPieChartView', function () {

  var view;

  beforeEach(function () {
    view = App.ResourceManagerHeapPieChartView.create();
  });

  describe('#didInsertElement', function () {
    it('should call calc method', function () {
      sinon.stub(view, 'calc');
      view.didInsertElement();
      expect(view.calc.calledOnce).to.be.true;
      view.calc.restore();
    });
  });

  describe('#getUsed', function () {
    it('should return zero if no modelValueUsed', function () {
      expect(view.getUsed()).to.be.equal(0);
    });

    it('should return correct value if modelValueUsed', function () {
      view.set('model.jvmMemoryHeapUsed', 1024 * 1024 * 2);
      expect(view.getUsed()).to.be.equal(2);
    });
  });

  describe('#getMax', function () {
    it('should return zero if no modelValueMax', function () {
      expect(view.getMax()).to.be.equal(0);
    });

    it('should return correct value if modelValueMax', function () {
      view.set('model.jvmMemoryHeapMax', 1024 * 1024 * 3);
      expect(view.getMax()).to.be.equal(3);
    });
  });
});