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

require('views/login');

var view,
  pass;

describe('App.LoginView', function () {

  before(function () {
    sinon.stub(App, 'get', function(k) {
      if (k === 'router') {
        return {
          login: Em.K
        };
      }
      return Em.get(App, k);
    });
  });

  beforeEach(function () {
    view = App.LoginView.create();
    pass = view.passTextField.create({
      controller: App.LoginController.create()
    });
  });

  after(function () {
    App.get.restore();
  });

  describe('#passTextField', function () {
    it('should change error message', function () {
      pass.insertNewline();
      expect(pass.get('controller.errorMessage')).to.be.empty;
    });
  });

});
