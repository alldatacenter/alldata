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

describe('PermissionSaver Service', function () {
  var PermissionSaver, $Cluster, $View;

  beforeEach(function () {
    module('ambariAdminConsole', angular.noop);
    inject(function (_PermissionSaver_, _Cluster_, _View_) {
      PermissionSaver = _PermissionSaver_;
      $Cluster = _Cluster_;
      $View = _View_;
    });
  });

  describe('#saveClusterPermissions', function () {

    var params = {
        clusterId: 'c0'
      },
      cases = [
        {
          permissions: {
            'CLUSTER.ADMINISTRATOR': {
              PermissionInfo: {
                permission_name: 'CLUSTER.ADMINISTRATOR'
              },
              ROLE: {},
              USER: ['u0', 'u1', 'g0'],
              GROUP: ['g0', 'g1', 'u0']
            },
            'CLUSTER.OPERATOR': {
              PermissionInfo: {
                permission_name: 'CLUSTER.OPERATOR'
              },
              ROLE: {},
              USER: ['g1'],
              GROUP: ['u1']
            }
          },
          updatePrivilegesCallCount: 1,
          generatedPermissions: [
            {
              PrivilegeInfo: {
                permission_name: 'CLUSTER.ADMINISTRATOR',
                principal_name: 'u0',
                principal_type: 'USER'
              }
            },
            {
              PrivilegeInfo: {
                permission_name: 'CLUSTER.ADMINISTRATOR',
                principal_name: 'u1',
                principal_type: 'USER'
              }
            },
            {
              PrivilegeInfo: {
                permission_name: 'CLUSTER.ADMINISTRATOR',
                principal_name: 'g0',
                principal_type: 'USER'
              }
            },
            {
              PrivilegeInfo: {
                permission_name: 'CLUSTER.ADMINISTRATOR',
                principal_name: 'g0',
                principal_type: 'GROUP'
              }
            },
            {
              PrivilegeInfo: {
                permission_name: 'CLUSTER.ADMINISTRATOR',
                principal_name: 'g1',
                principal_type: 'GROUP'
              }
            },
            {
              PrivilegeInfo: {
                permission_name: 'CLUSTER.ADMINISTRATOR',
                principal_name: 'u0',
                principal_type: 'GROUP'
              }
            },
            {
              PrivilegeInfo: {
                permission_name: 'CLUSTER.OPERATOR',
                principal_name: 'g1',
                principal_type: 'USER'
              }
            },
            {
              PrivilegeInfo: {
                permission_name: 'CLUSTER.OPERATOR',
                principal_name: 'u1',
                principal_type: 'GROUP'
              }
            }
          ],
          title: 'valid data'
        },
        {
          permissions: {
            'CLUSTER.ADMINISTRATOR': {
              PermissionInfo: {
                permission_name: 'CLUSTER.ADMINISTRATOR'
              },
              ROLE: {},
              USER: ['u0', 'u1'],
              GROUP: ['g0', 'g1']
            },
            'CLUSTER.OPERATOR': {
              PermissionInfo: {
                permission_name: 'CLUSTER.OPERATOR'
              },
              ROLE: {},
              USER: ['u0'],
              GROUP: ['g2']
            },
            'CLUSTER.USER': {
              PermissionInfo: {
                permission_name: 'CLUSTER.USER'
              },
              ROLE: {},
              USER: ['u2'],
              GROUP: ['g0']
            }
          },
          updatePrivilegesCallCount: 0,
          title: 'invalid data'
        }
      ];

    angular.forEach(cases, function (item) {

      describe(item.title, function () {

        beforeEach(function () {
          spyOn($Cluster, 'updatePrivileges').and.callFake(angular.noop);
          PermissionSaver.saveClusterPermissions(item.permissions, params);
        });

        it('updatePrivileges call', function () {
          expect($Cluster.updatePrivileges.calls.count()).toEqual(item.updatePrivilegesCallCount);
        });

        if (item.updatePrivilegesCallCount) {
          it('updatePrivileges arguments', function () {
            expect($Cluster.updatePrivileges).toHaveBeenCalledWith(params, item.generatedPermissions);
          });
        }

      });

    });

  });

  describe('#saveViewPermissions', function () {

    var params = {
        instance_name: 'i0',
        version: '1.0.0',
        view_name: 'v0'
      },
      permissions = {
        'VIEW.USER': {
          'PermissionInfo': {
            permission_name: 'VIEW.USER'
          },
          'ROLE': {
            'CLUSTER.ADMINISTRATOR': true,
            'CLUSTER.OPERATOR': false,
            'SERVICE.OPERATOR': false,
            'SERVICE.ADMINISTRATOR': false,
            'CLUSTER.USER': false
          },
          'USER': ['u0', 'u1', 'g0'],
          'GROUP': ['g0', 'g1', 'u0']
        }
      },
      generatedPermissions = [
        {
          PrivilegeInfo: {
            permission_name: 'VIEW.USER',
            principal_name: 'u0',
            principal_type: 'USER'
          }
        },
        {
          PrivilegeInfo: {
            permission_name: 'VIEW.USER',
            principal_name: 'u1',
            principal_type: 'USER'
          }
        },
        {
          PrivilegeInfo: {
            permission_name: 'VIEW.USER',
            principal_name: 'g0',
            principal_type: 'USER'
          }
        },
        {
          PrivilegeInfo: {
            permission_name: 'VIEW.USER',
            principal_name: 'g0',
            principal_type: 'GROUP'
          }
        },
        {
          PrivilegeInfo: {
            permission_name: 'VIEW.USER',
            principal_name: 'g1',
            principal_type: 'GROUP'
          }
        },
        {
          PrivilegeInfo: {
            permission_name: 'VIEW.USER',
            principal_name: 'u0',
            principal_type: 'GROUP'
          }
        },
        {
          PrivilegeInfo: {
            permission_name: 'VIEW.USER',
            principal_name: 'CLUSTER.ADMINISTRATOR',
            principal_type: 'ROLE'
          }
        }
      ];

    beforeEach(function () {
      spyOn($View, 'updatePrivileges').and.callFake(angular.noop);
      PermissionSaver.saveViewPermissions(permissions, params);
    });

      it('should update privileges', function () {
        expect($View.updatePrivileges.calls.count()).toEqual(1);
      });

      it('updatePrivileges arguments', function () {
        expect($View.updatePrivileges).toHaveBeenCalledWith(params, generatedPermissions);
      });

  });

});
