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

describe('App.PrevUpgradeNotCompletedView', function () {
  var view;
  beforeEach(function () {
    view = App.PrevUpgradeNotCompletedView.create({});
  });

  describe('#resumeUpgrade', function () {
    it('should call resumeUpgrade and openUpgradeDialog methods', function () {
      var controller = App.router.get('mainAdminStackAndUpgradeController');
      sinon.stub(controller, 'resumeUpgrade');
      sinon.stub(controller, 'openUpgradeDialog');
      view.resumeUpgrade();
      expect(controller.resumeUpgrade.calledOnce).to.be.equal(true);
      expect(controller.openUpgradeDialog.calledOnce).to.be.equal(true);
      controller.resumeUpgrade.restore();
      controller.openUpgradeDialog.restore();
    });
  });

  describe('#abortUpgrade', function () {
    it('should call abortUpgrade method', function () {
      var controller = App.router.get('mainAdminStackAndUpgradeController');
      sinon.stub(controller, 'abortUpgrade').returns($.Deferred().resolve().promise());
      sinon.stub(controller, 'openUpgradeDialog');
      view.abortUpgrade();
      expect(controller.abortUpgrade.calledOnce).to.be.equal(true);
      expect(controller.openUpgradeDialog.calledOnce).to.be.equal(true);
      controller.abortUpgrade.restore();
      controller.openUpgradeDialog.restore();
    });
  });

  describe('#finalizeUpgrade', function () {
    it('should call updateFinalize method', function () {
      var controller = App.router.get('mainAdminStackAndUpgradeController');
      sinon.stub(controller, 'updateFinalize').returns($.Deferred().resolve().promise());
      sinon.stub(controller, 'openUpgradeDialog');
      view.finalizeUpgrade();
      expect(controller.updateFinalize.calledOnce).to.be.equal(true);
      expect(controller.openUpgradeDialog.calledOnce).to.be.equal(true);
      controller.updateFinalize.restore();
      controller.openUpgradeDialog.restore();
    });
  });
});