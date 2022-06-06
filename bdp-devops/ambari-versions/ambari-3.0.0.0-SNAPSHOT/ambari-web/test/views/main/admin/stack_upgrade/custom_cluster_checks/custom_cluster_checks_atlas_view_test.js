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

describe('App.AtlasInstalledCheckView', function () {
  var view;
  beforeEach(function () {
    view = App.AtlasInstalledCheckView.create({});
  });

  describe('#atlasRemoved', function () {
    it('should return true if no atlas available', function () {
      view.set('services', [Em.Object.create({serviceName: 'HIVE'})]);
      expect(view.get('atlasRemoved')).to.be.true;
    });

    it('should return false if atlas available', function () {
      view.set('services', [Em.Object.create({serviceName: 'ATLAS'})]);
      expect(view.get('atlasRemoved')).to.be.false;
    });
  });

  describe('#removeAtlas', function () {
    it('should call deleteService method', function() {
      var mockMainServiceItemController = Em.Object.create({deleteService: function () {}, content: {}});
      sinon.stub(mockMainServiceItemController, 'deleteService');
      sinon.stub(App.router, 'get').returns(mockMainServiceItemController);
      view.removeAtlas();
      expect(mockMainServiceItemController.deleteService.calledOnce).to.be.true;
      expect(mockMainServiceItemController.deleteService.calledWith('ATLAS')).to.be.true;
      mockMainServiceItemController.deleteService.restore();
      App.router.get.restore();
    });
  });
});