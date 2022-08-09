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
require('views/main/admin/stack_upgrade/failed_hosts_modal_view');

describe('App.FailedHostsPopupBodyView', function () {
  var view = App.FailedHostsPopupBodyView.create({
    parentView: Em.Object.create({
      content: {}
    })
  });


  describe("#subHeader", function() {
    it("subHeader is formatted with hosts count", function() {
      view.set('parentView.content', {
        hosts: ['host1', 'host2', 'host3']
      });
      view.propertyDidChange('subHeader');
      expect(view.get('subHeader')).to.equal(Em.I18n.t('admin.stackUpgrade.failedHosts.subHeader').format(3));
    });
  });

  describe("#hosts", function() {
    beforeEach(function(){
      sinon.stub(App.format, 'role', function(name){
        return name;
      })
    });
    afterEach(function(){
      App.format.role.restore();
    });

    it("hosts are mapped from parentView.content", function() {
      view.set('parentView.content', {
        hosts: ['host1', 'long.host.50.chars.commmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmm'],
        host_detail: {
          "host1": [
            {
              component: 'DATANODE',
              service: 'HDFS'
            },
            {
              component: 'HBASE_REGIONSERVER',
              service: 'HBASE'
            }
          ],
          "long.host.50.chars.commmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmm": [
            {
              service: 'FLUME',
              component: 'FLUME_AGENT'
            }
          ]
        }
      });
      view.propertyDidChange('hosts');
      expect(view.get('hosts')).to.eql([
        Em.Object.create({
          hostName: 'host1',
          displayName: 'host1',
          collapseId: 'collapse0',
          collapseHref: '#collapse0',
          hostComponents: [
            Em.Object.create({
              componentName: 'DATANODE',
              serviceName: 'HDFS'
            }),
            Em.Object.create({
              componentName: 'HBASE_REGIONSERVER',
              serviceName: 'HBASE'
            })
          ]
        }),
        Em.Object.create({
          hostName: 'long.host.50.chars.commmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmm',
          displayName: 'long.host.50.chars.commmmmmmmmmmmmmmmmmmmmmmmmmmmm...',
          collapseId: 'collapse1',
          collapseHref: '#collapse1',
          hostComponents: [
            Em.Object.create({
              componentName: 'FLUME_AGENT',
              serviceName: 'FLUME'
            })
          ]
        })
      ]);
    });
  });
});
