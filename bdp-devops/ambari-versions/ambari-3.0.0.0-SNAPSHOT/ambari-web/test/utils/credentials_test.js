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

var credentials = require('utils/credentials');
var testHelpers = require('test/helpers');

describe('credentials utils', function () {

  var storeTypeStatusMock = function (clusterName, key) {
    var result = {};
    result[key] = clusterName;
    return result;
  };

  describe('#createCredentials', function () {

    it('should send AJAX request', function () {
      credentials.createCredentials('c', 'a', {});
      expect(testHelpers.findAjaxRequest('name', 'credentials.create')).to.eql([
        {
          sender: credentials,
          name: 'credentials.create',
          data: {
            clusterName: 'c',
            resource: {},
            alias: 'a'
          },
          error: 'createCredentialsErrorCallback'
        }
      ]);
    });

  });

  describe('#credentialsSuccessCallback', function () {

    var params = {
        callback: Em.K
      },
      cases = [
        {
          items: [],
          callbackArgument: [],
          title: 'no data returned'
        },
        {
          items: [{}, {}],
          callbackArgument: [undefined, undefined],
          title: 'empty data returned'
        },
        {
          items: [
            {
              Credential: {
                id: 0
              }
            },
            {
              Credential: {
                id: 1
              }
            }
          ],
          callbackArgument: [
            {
              id: 0
            },
            {
              id: 1
            }
          ],
          title: 'valid data returned'
        }
      ];

    beforeEach(function () {
      sinon.spy(params, 'callback');
    });

    afterEach(function () {
      params.callback.restore();
    });

    cases.forEach(function (item) {

      it(item.title, function () {
        credentials.credentialsSuccessCallback({
          items: item.items
        }, null, params);
        expect(params.callback.firstCall.args).to.eql([item.callbackArgument]);
      });

    });

  });

  describe('#createOrUpdateCredentials', function () {

    var mock = {
        dfd: {
          getCredential: null,
          updateCredentials: null,
          createCredentials: null
        },
        callback: Em.K,
        getCredential: function () {
          return mock.dfd.getCredential.promise();
        },
        updateCredentials: function () {
          return mock.dfd.updateCredentials.promise();
        },
        createCredentials: function () {
          return mock.dfd.createCredentials.promise();
        }
      },
      cases = [
        {
          getCredentialResolve: true,
          credentialsCallback: 'updateCredentials',
          isCredentialsCallbackResolve: true,
          status: 'success',
          result: {
            status: 200
          },
          callbackArgs: [
            true,
            {
              status: 200
            }
          ],
          title: 'successful credentials update'
        },
        {
          getCredentialResolve: true,
          credentialsCallback: 'updateCredentials',
          isCredentialsCallbackResolve: false,
          status: 'error',
          result: {
            status: 404
          },
          callbackArgs: [
            false,
            {
              status: 404
            }
          ],
          title: 'failed credentials update'
        },
        {
          getCredentialResolve: false,
          credentialsCallback: 'createCredentials',
          isCredentialsCallbackResolve: true,
          status: 'success',
          result: {
            status: 201
          },
          callbackArgs: [
            true,
            {
              status: 201
            }
          ],
          title: 'successful credentials creation'
        },
        {
          getCredentialResolve: false,
          credentialsCallback: 'createCredentials',
          isCredentialsCallbackResolve: false,
          status: 'error',
          result: {
            status: 500
          },
          callbackArgs: [
            false,
            {
              status: 500
            }
          ],
          title: 'failed credentials creation'
        }
      ];

    beforeEach(function () {
      sinon.stub(credentials, 'getCredential', mock.getCredential);
      sinon.stub(credentials, 'updateCredentials', mock.updateCredentials);
      sinon.stub(credentials, 'createCredentials', mock.createCredentials);
      sinon.spy(mock, 'callback');
      mock.dfd.getCredential = $.Deferred();
      mock.dfd.updateCredentials = $.Deferred();
      mock.dfd.createCredentials = $.Deferred();
    });

    afterEach(function () {
      credentials.getCredential.restore();
      credentials.updateCredentials.restore();
      credentials.createCredentials.restore();
      mock.callback.restore();
    });

    cases.forEach(function (item) {

      var getCredentialMethod = item.getCredentialResolve ? 'resolve' : 'reject',
        credentialsCallbackMethod = item.isCredentialsCallbackResolve ? 'resolve' : 'reject';

      it(item.title, function () {
        mock.dfd.getCredential[getCredentialMethod]();
        mock.dfd[item.credentialsCallback][credentialsCallbackMethod](null, item.status, item.result);
        credentials.createOrUpdateCredentials().done(mock.callback);
        expect(mock.callback.firstCall.args).to.eql(item.callbackArgs);
      });

    });

  });

  describe('#getCredential', function () {

    it('should send AJAX request', function () {
      credentials.getCredential('c', 'a', Em.K);
      expect(testHelpers.findAjaxRequest('name', 'credentials.get')).to.eql([
        {
          sender: credentials,
          name: 'credentials.get',
          data: {
            clusterName: 'c',
            alias: 'a',
            callback: Em.K
          },
          success: 'getCredentialSuccessCallback',
          error: 'getCredentialErrorCallback'
        }
      ]);
    });

  });

  describe('#getCredentialSuccessCallback', function () {

    var params = {
        callback: Em.K
      },
      cases = [
        {
          data: null,
          callback: undefined,
          callbackCallCount: 0,
          title: 'no callback passed'
        },
        {
          data: null,
          callback: null,
          callbackCallCount: 0,
          title: 'invalid callback passed'
        },
        {
          data: null,
          callbackCallCount: 1,
          callbackArgument: null,
          title: 'no data passed'
        },
        {
          data: {},
          callbackCallCount: 1,
          callbackArgument: null,
          title: 'no credential info passed'
        },
        {
          data: {
            Credential: 'c'
          },
          callbackCallCount: 1,
          callbackArgument: 'c',
          title: 'credential info passed'
        }
      ];

    cases.forEach(function (item) {

      describe(item.title, function () {

        beforeEach(function () {
          sinon.spy(params, 'callback');
          credentials.getCredentialSuccessCallback(item.data, null, item.hasOwnProperty('callback') ? {
            callback: item.callback
          } : params);
        });

        afterEach(function () {
          params.callback.restore();
        });

        it('callback call count', function () {
          expect(params.callback.callCount).to.equal(item.callbackCallCount);
        });

        if (item.callbackCallCount) {
          it('callback argument', function () {
            expect(params.callback.firstCall.args).to.eql([item.callbackArgument]);
          });
        }

      });

    });

  });

  describe('#updateCredentials', function () {

    it('should send AJAX request', function () {
      credentials.updateCredentials('c', 'a', {});
      expect(testHelpers.findAjaxRequest('name', 'credentials.update')).to.eql([
        {
          sender: credentials,
          name: 'credentials.update',
          data: {
            clusterName: 'c',
            alias: 'a',
            resource: {}
          }
        }
      ]);
    });

  });

  describe('#credentials', function () {

    it('should send AJAX request', function () {
      credentials.credentials('c', Em.K);
      expect(testHelpers.findAjaxRequest('name', 'credentials.list')).to.eql([
        {
          sender: credentials,
          name: 'credentials.list',
          data: {
            clusterName: 'c',
            callback: Em.K
          },
          success: 'credentialsSuccessCallback'
        }
      ]);
    });

  });

  describe('#removeCredentials', function () {

    it('should send AJAX request', function () {
      credentials.removeCredentials('c', 'a');
      expect(testHelpers.findAjaxRequest('name', 'credentials.delete')).to.eql([
        {
          sender: credentials,
          name: 'credentials.delete',
          data: {
            clusterName: 'c',
            alias: 'a'
          }
        }
      ]);
    });

  });

  describe('#storageInfo', function () {

    it('should send AJAX request', function () {
      credentials.storageInfo('c', Em.K);
      expect(testHelpers.findAjaxRequest('name', 'credentials.store.info')).to.eql([
        {
          sender: credentials,
          name: 'credentials.store.info',
          data: {
            clusterName: 'c',
            callback: Em.K
          },
          success: 'storageInfoSuccessCallback'
        }
      ]);
    });

  });

  describe('#storageInfoSuccessCallback', function () {

    var params = {
        callback: Em.K
      },
      cases = [
        {
          callbackArgument: null,
          title: 'no clusters'
        },
        {
          clusters: null,
          callbackArgument: null,
          title: 'invalid clusters info'
        },
        {
          clusters: {},
          callbackArgument: {
            persistent: false,
            temporary: false
          },
          title: 'empty clusters info'
        },
        {
          clusters: {
            credential_store_properties: {
              'storage.persistent': true,
              'storage.temporary': true
            }
          },
          callbackArgument: {
            persistent: false,
            temporary: false
          },
          title: 'invalid storage properties format'
        },
        {
          clusters: {
            credential_store_properties: {}
          },
          callbackArgument: {
            persistent: false,
            temporary: false
          },
          title: 'no storage properties'
        },
        {
          clusters: {
            credential_store_properties: {
              'storage.persistent': 'true',
              'storage.temporary': 'false'
            }
          },
          callbackArgument: {
            persistent: true,
            temporary: false
          },
          title: 'valid storage properties format - persistent storage'
        },
        {
          clusters: {
            credential_store_properties: {
              'storage.persistent': 'false',
              'storage.temporary': 'true'
            }
          },
          callbackArgument: {
            persistent: false,
            temporary: true
          },
          title: 'valid storage properties format - temporary storage'
        },
        {
          clusters: {
            credential_store_properties: {
              'storage.persistent': 'true',
              'storage.temporary': 'true'
            }
          },
          callbackArgument: {
            persistent: true,
            temporary: true
          },
          title: 'valid storage properties format - both types'
        }
      ];

    cases.forEach(function (item) {

      describe(item.title, function () {

        beforeEach(function () {
          sinon.spy(params, 'callback');
          credentials.storageInfoSuccessCallback({
            Clusters: item.clusters
          }, null, params);
        });

        afterEach(function () {
          params.callback.restore();
        });

        it('callback execution', function () {
          expect(params.callback.calledOnce).to.be.true;
        });

        it('callback argument', function () {
          expect(params.callback.firstCall.args).to.eql([item.callbackArgument]);
        });

      });

    });

  });

  describe('#isStorePersisted', function () {

    beforeEach(function () {
      sinon.stub(credentials, 'storeTypeStatus', storeTypeStatusMock);
    });

    afterEach(function () {
      credentials.storeTypeStatus.restore();
    });

    it('should return storeTypeStatus result', function () {
      expect(credentials.isStorePersisted('c')).to.eql({
        persistent: 'c'
      });
    });

  });


  describe('#isStoreTemporary', function () {

    beforeEach(function () {
      sinon.stub(credentials, 'storeTypeStatus', storeTypeStatusMock);
    });

    afterEach(function () {
      credentials.storeTypeStatus.restore();
    });

    it('should return storeTypeStatus result', function () {
      expect(credentials.isStoreTemporary('c')).to.eql({
        temporary: 'c'
      });
    });

  });

  describe('#storeTypeStatus', function () {

    var mock = {
        successCallback: Em.K,
        errorCallback: Em.K
      },
      data = {
        clusterName: 'c'
      },
      error = {
        status: 404
      },
      cases = [
        {
          isSuccess: true,
          callbackArgument: data,
          title: 'success'
        },
        {
          isSuccess: false,
          callbackArgument: error,
          title: 'fail'
        }
      ];

    cases.forEach(function (item) {

      describe(item.title, function () {

        var callbackName = item.isSuccess ? 'successCallback' : 'errorCallback';

        beforeEach(function () {
          sinon.spy(mock, 'successCallback');
          sinon.spy(mock, 'errorCallback');
          sinon.stub(credentials, 'storageInfo', function (clusterName, callback) {
            var dfd = $.Deferred();
            if (item.isSuccess) {
              callback({
                temporary: data
              });
            } else {
              dfd.reject(error);
            }
            return dfd.promise();
          });
          credentials.storeTypeStatus(null, 'temporary').then(mock.successCallback, mock.errorCallback);
        });

        afterEach(function () {
          mock.successCallback.restore();
          mock.errorCallback.restore();
          credentials.storageInfo.restore();
        });

        it('success callback', function () {
          expect(mock.successCallback.called).to.equal(item.isSuccess);
        });

        it('error callback', function () {
          expect(mock.errorCallback.called).to.not.equal(item.isSuccess);
        });

        it('callback called once', function () {
          expect(mock[callbackName].calledOnce).to.be.true;
        });

        it('callback arguments', function () {
          expect(mock[callbackName].firstCall.args).to.eql([item.callbackArgument]);
        });

      });

    });

  });

  describe('#createCredentialResource', function () {

    it('should return object with arguments', function () {
      expect(credentials.createCredentialResource('p', 'c', 't')).to.eql({
        principal: 'p',
        key: 'c',
        type: 't'
      });
    });

  });

  describe('#isKDCCredentialsPersisted', function () {

    var cases = [
      {
        credentials: [],
        isKDCCredentialsPersisted: false,
        title: 'empty array passed'
      },
      {
        credentials: [{}, {}],
        isKDCCredentialsPersisted: false,
        title: 'no aliases passed'
      },
      {
        credentials: [
          {
            alias: 'a0'
          },
          {
            alias: 'a1'
          }
        ],
        isKDCCredentialsPersisted: false,
        title: 'no KDC admin credentials passed'
      },
      {
        credentials: [
          {
            alias: 'kdc.admin.credential'
          },
          {
            alias: 'a2'
          }
        ],
        isKDCCredentialsPersisted: false,
        title: 'no KDC admin credentials type passed'
      },
      {
        credentials: [
          {
            alias: 'kdc.admin.credential',
            type: 'temporary'
          },
          {
            alias: 'a3'
          }
        ],
        isKDCCredentialsPersisted: false,
        title: 'temporary storage'
      },
      {
        credentials: [
          {
            alias: 'kdc.admin.credential',
            type: 'persisted'
          },
          {
            alias: 'kdc.admin.credential'
          },
          {
            alias: 'a4'
          }
        ],
        isKDCCredentialsPersisted: true,
        title: 'persistent storage'
      }
    ];

    cases.forEach(function (item) {

      it(item.title, function () {
        expect(credentials.isKDCCredentialsPersisted(item.credentials)).to.equal(item.isKDCCredentialsPersisted);
      });

    });

  });

});
