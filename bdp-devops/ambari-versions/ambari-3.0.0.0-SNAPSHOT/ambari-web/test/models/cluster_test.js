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
require('models/cluster');

describe('App.Cluster', function () {

  var cluster;

  beforeEach(function () {
    cluster = App.Cluster.createRecord();
  });

  describe('#isKerberosEnabled', function () {

    [
      {
        securityType: 'KERBEROS',
        isKerberosEnabled: true,
        title: 'Kerberos enabled'
      },
      {
        securityType: 'NONE',
        isKerberosEnabled: false,
        title: 'Kerberos disabled'
      }
    ].forEach(function (item) {

      it(item.title, function () {
        cluster.set('securityType', item.securityType);
        expect(cluster.get('isKerberosEnabled')).to.equal(item.isKerberosEnabled);
      });

    });

    describe('#isCredentialStorePersistent', function () {

      [
        {
          propertyValue: 'false',
          isCredentialStorePersistent: false,
          title: 'no persistent credential store'
        },
        {
          propertyValue: true,
          isCredentialStorePersistent: false,
          title: 'malformed value'
        },
        {
          propertyValue: 'true',
          isCredentialStorePersistent: true,
          title: 'persistent credential store'
        }
      ].forEach(function (item) {

        it(item.title, function () {
          cluster.set('credentialStoreProperties', {
            'storage.persistent': item.propertyValue
          });
          expect(cluster.get('isCredentialStorePersistent')).to.equal(item.isCredentialStorePersistent);
        });

      });

    });

  });

});