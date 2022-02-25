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

describe('ClusterInformationCtrl', function() {
  beforeEach(module('ambariAdminConsole'));

  var ctrl, $scope, Cluster, deferred, ConfirmationModal;

  beforeEach(inject(function($controller, $rootScope, _Cluster_, _$q_, _ConfirmationModal_, _$httpBackend_){
    // The injector unwraps the underscores (_) from around the parameter names when matching
    _$httpBackend_.expectGET('views/clusters/clusterInformation.html').respond(200);
    Cluster = _Cluster_;
    ConfirmationModal = _ConfirmationModal_;
    deferred = _$q_.defer();
    $scope = $rootScope.$new();
    $scope.$apply();
    ctrl = $controller('ClusterInformationCtrl', {
      $scope: $scope
    });

    spyOn(Cluster, 'getBlueprint').and.returnValue(deferred.promise);
    spyOn(Cluster, 'editName').and.returnValue(deferred.promise);
    spyOn(ConfirmationModal, 'show').and.returnValue(deferred.promise);
  }));

  describe('#getBlueprint', function() {
    it('Cluster.getBlueprint should be called', function() {
      $scope.cluster = {
        Clusters: {
          cluster_name: 'c1'
        }
      };
      $scope.getBlueprint();
      expect(Cluster.getBlueprint).toHaveBeenCalled();
    });
  });

  describe('#toggleSaveButton', function() {
    beforeEach(function() {
      $scope.cluster = {
        Clusters: {
          cluster_name: 'c1'
        }
      };
    });

    it('isClusterNameEdited should be true', function() {
      $scope.edit = {
        clusterName: 'c2'
      };
      $scope.toggleSaveButton();
      expect($scope.isClusterNameEdited).toBeTruthy();
    });

    it('isClusterNameEdited should be false', function() {
      $scope.edit = {
        clusterName: 'c1'
      };
      $scope.toggleSaveButton();
      expect($scope.isClusterNameEdited).toBeFalsy();
    });
  });

  describe('#confirmClusterNameChange', function() {
    it('ConfirmationModal.show should be called', function() {
      $scope.edit = {
        clusterName: 'c1'
      };
      $scope.confirmClusterNameChange();
      expect(ConfirmationModal.show).toHaveBeenCalled();
    });
  });

  describe('#saveClusterName', function() {
    it('Cluster.editName should be called', function() {
      $scope.edit = {
        clusterName: 'c1'
      };
      $scope.cluster = {
        Clusters: {
          cluster_name: 'c2'
        }
      };
      $scope.saveClusterName();
      expect(Cluster.editName).toHaveBeenCalledWith('c2', 'c1');
    });
  });
});
