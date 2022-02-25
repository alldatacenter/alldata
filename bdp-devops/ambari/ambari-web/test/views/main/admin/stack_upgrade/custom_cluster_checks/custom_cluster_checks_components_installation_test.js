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

describe('App.ComponentsInstallationFailedView', function () {
  var view;
  beforeEach(function () {
    view = App.ComponentsInstallationFailedView.create({});
  });

  describe('#reinstallComponent', function () {
    it('should call installComponent method of mainHostDetailsController with proper params', function () {
      var controller = App.router.get('mainHostDetailsController');
      var service = Em.Object.create({serviceName: 'HIVE'});
      var component = Em.Object.create({componentName: 'HIVE_SERVER_INTERACTIVE'});
      sinon.stub(controller, 'installComponent');
      sinon.stub(App.Service, 'find').returns(service);
      sinon.stub(App.StackServiceComponent, 'find').returns(component);
      view.reinstallComponent({context: {host_name: 'test', service_name: 'HIVE', componentName: 'HIVE_SERVER_INTERACTIVE'}});
      expect(controller.installComponent.calledOnce).to.be.true;
      expect(controller.installComponent.calledWith({context: component})).to.be.true;
      controller.installComponent.restore();
      App.Service.find.restore();
      App.StackServiceComponent.find.restore();
    });
  });
});