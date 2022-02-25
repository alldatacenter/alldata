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
'use strict';

angular.module('ambariAdminConsole')
.factory('ConfirmationModal', ['$modal', '$q', '$translate', function($modal, $q, $translate) {

  var $t = $translate.instant;

	return {
		show: function(header, body, confirmText, cancelText, options) {
			var deferred = $q.defer();
      options = options || {};

			var modalInstance = $modal.open({
				templateUrl: 'views/modals/ConfirmationModal.html',
				controller: ['$scope', '$modalInstance', function($scope, $modalInstance) {
					$scope.header = header;
          $scope.isTempalte = !!body.url;
					$scope.body = body;
          $scope.innerScope = body.scope;
          $scope.confirmText = confirmText || $t('common.controls.ok');
          $scope.cancelText = cancelText || $t('common.controls.cancel');
          $scope.primaryClass = options.primaryClass || 'btn-primary',
					$scope.showCancelButton = !options.hideCancelButton;

					$scope.ok = function() {
						$modalInstance.close();
						deferred.resolve();
					};
					$scope.cancel = function() {
						$modalInstance.dismiss();
						deferred.reject();
					};
				}]
			});

      modalInstance.result.then(function() {
        // Gets triggered on close
      }, function() {
        // Gets triggered on dismiss
        deferred.reject();
      });

			return deferred.promise;
		}
	};
}]);