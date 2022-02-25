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
require('/views/main/service/info/summary/hdfs/slaves');

function getView(options) {
  return App.HDFSSlaveComponentsView.create(options || {});
}

describe('App.HDFSSlaveComponentsView', function () {
  var view;

  beforeEach(function () {
    view = getView({
      service: Em.Object.create()
    });
  });

  App.TestAliases.testAsComputedAlias(getView(), 'journalNodesTotal', 'service.journalNodes.length', 'number');

  describe('#journalNodesLive', function () {

    it('should return live journal nodes count', function () {
      view.set('service', Em.Object.create({
        journalNodes: [
          Em.Object.create({
            workStatus: 'STARTED'
          }),
          Em.Object.create()
        ]
      }));
      view.propertyDidChange('journalNodesLive');
      expect(view.get('journalNodesLive')).to.be.equal(1);
    });
  });

  describe('#isNfsInStack', function () {

    beforeEach(function () {
      this.mock = sinon.stub(App.StackServiceComponent, 'find');
    });
    afterEach(function () {
      this.mock.restore();
    });

    it('no NFS_GATEWAY component', function () {
      this.mock.returns([]);
      view.propertyDidChange('isNfsInStack');
      expect(view.get('isNfsInStack')).to.be.false;
    });

    it('NFS_GATEWAY component present', function () {
      this.mock.returns([
        {
          componentName: 'NFS_GATEWAY'
        }
      ]);
      view.propertyDidChange('isNfsInStack');
      expect(view.get('isNfsInStack')).to.be.true;
    });
  });

});
