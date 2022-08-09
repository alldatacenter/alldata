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

describe('App.HBaseLinksView', function () {

  var view;

  beforeEach(function () {
    view = App.HBaseLinksView.create();
  });

  describe('#port', function () {
    it('should return 16010 port when compareCurrentVersion returns more than -1', function () {
      sinon.stub(App.StackService, 'find').returns({compareCurrentVersion: function () {return 1;}});
      expect(view.get('port')).to.be.equal('16010');
      App.StackService.find.restore();
    });

    it('should return 60010 port when compareCurrentVersion returns not more than -1', function () {
      sinon.stub(App.StackService, 'find').returns({compareCurrentVersion: function () {return -1;}});
      expect(view.get('port')).to.be.equal('60010');
      App.StackService.find.restore();
    });
  });

  describe('#hbaseMasterWebUrl', function () {
    it('should return empty string if no active master', function () {
      expect(view.get('hbaseMasterWebUrl')).to.be.equal('');
    });
    it('should return correct link if active master is provided', function () {
      view.set('port', '8080');
      view.set('activeMaster', {host: {publicHostName: 'test'}});
      expect(view.get('hbaseMasterWebUrl'));
    });
  });
});