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

describe('App.loadTimer', function () {

  window.performance = {
    now: function() {
      return 1;
    }
  };

  beforeEach(function() {
    App.set('supports.showPageLoadTime', true);
  });

  afterEach(function () {
    App.set('supports.showPageLoadTime', false);
    App.loadTimer.set('timeStampCache', {});
  });

  describe("#start()", function() {
    it("time should be cached", function() {
      App.loadTimer.start('test');
      expect(App.loadTimer.get('timeStampCache.test')).to.be.an('number');
    });
  });

  describe("#finish()", function() {
    it("timeStampCache is empty", function() {
      App.loadTimer.start('test');
      expect(App.loadTimer.finish('test')).to.be.not.empty;
      expect(App.loadTimer.get('timeStampCache')).to.be.empty;
    });
  });
});
