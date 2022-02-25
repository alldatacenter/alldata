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
.directive('editableList', ['$q', '$document', '$location', function($q, $document, $location) {
  return {
    restrict: 'E',
    templateUrl: 'views/directives/editableList.html',
    scope: {
      itemsSource: '=',
      resourceType: '@',
      editable: '='
    },
    link: function($scope, $elem, $attr, $ctrl) {
      var $editBox = $elem.find('[contenteditable]');

      var readInput = function() {
        $scope.$apply(function() {
          $scope.input = $editBox.text();
        });
      };

      var isIE = function () {
        var ua = window.navigator.userAgent;
        var msie = ua.indexOf("MSIE ");

        // If Internet Explorer, return version number
        if (msie > 0)
          return !!parseInt(ua.substring(msie + 5, ua.indexOf(".", msie)));

        // If Internet Explorer 11 handling differently because UserAgent string updated by Microsoft
        else if (!!navigator.userAgent.match(/Trident\/7\./))
          return true;
        else
        //If another browser just returning  0
          return false
      };

      $scope.$watch(function() {
        return $scope.input;
      }, function(newValue) {
        if(newValue === ''){
          $scope.clearInput();
        }
      });

      $scope.clearInput = function() {
        $editBox.html('').blur();
      };

      $scope.focusOnInput = function() {
        setTimeout(function() {
          var elem = $editBox[0];
          var selection = window.getSelection(),
              range = document.createRange();
          elem.innerHTML = '\u00a0';
          range.selectNodeContents(elem);

          if(!isIE())
            selection.removeAllRanges();

          selection.addRange(range);
          document.execCommand('delete', false, null);
        }, 0);
      };

      if(isIE()) {
        $editBox.keypress(function(e) {
          $scope.$apply(function() {
            $scope.input = $editBox.text() + e.char;
          })
        });
      }else{
        $editBox.on('input', readInput);
      }

      $editBox.on('keydown', function(e) {
        switch(e.which){
          case 27: // ESC
            $editBox.html('').blur();
            readInput();
            break;
          case 13: // Enter
            $scope.$apply(function() {
              if ($scope.addItem()) {
                $scope.focusOnInput();
              }
            });
            return false;
            break;
          case 40: // Down arrow
            $scope.downArrowHandler();
            break;
          case 38: // Up arrow
            $scope.upArrowHandler();
            break;
        }
      });

      $elem.find('.editable-list-container').on('reset', function(event) {
        $scope.editMode = false;
        $scope.items = angular.copy($scope.itemsSource);
        $scope.input = '';
        event.stopPropagation();
      });
    },
    controller: ['$scope', '$injector', '$modal', function($scope, $injector, $modal) {
      var $resource = $injector.get($scope.resourceType);

      $scope.identity = angular.identity; // Sorting function

      $scope.items = angular.copy($scope.itemsSource);
      $scope.editMode = false;
      $scope.input = '';
      $scope.typeahead = [];
      $scope.selectedTypeahed = 0;
      $scope.resources = [];
      $scope.invalidInput = false;

      preloadResources();

      // Watch source of items
      $scope.$watch(function() {
        return $scope.itemsSource;
      }, function(newValue) {
        $scope.items = angular.copy($scope.itemsSource);
      }, true);

      // When input has changed - load typeahead items
      $scope.$watch(function() {
        return $scope.input;
      }, function(newValue) {
        $scope.invalidInput = false;
        if(newValue){
          var newValue = newValue.split(',').filter(function(i){ 
            i = i.replace('&nbsp;', ''); // Sanitize from spaces
            return !!i.trim();
          }).map(function(i) { return i.trim(); });
          if( newValue.length > 1){
            var validInput = true;
            // If someone paste coma separated string, then just add all items to list
            angular.forEach(newValue, function(item) {
              if (validInput) {
                validInput = $scope.addItem(item);
              }
            });
            if (validInput) {
              $scope.clearInput();
              $scope.focusOnInput();
            }
          } else {
            var items = [];
            angular.forEach($scope.resources, function (name) {
              if (name.indexOf(newValue) !== -1 && $scope.items.indexOf(name) === -1) {
                items.push(name);
              }
            });
            $scope.typeahead = items.slice(0, 5);
            $scope.selectedTypeahed = 0;
          }
        } else {
          $scope.typeahead = [];
          $scope.selectedTypeahed = 0;
          $scope.focusOnInput();
        }
      });

      function preloadResources() {
        $resource.listByName('').then(function(data) {
          if (data && data.data.items) {
            $scope.resources = data.data.items.map(function(item) {
              if ($scope.resourceType === 'User') {
                return item.Users.user_name;
              } else if ($scope.resourceType === 'Group') {
                return item.Groups.group_name;
              }
            });
          }
        });
      }

      $scope.enableEditMode = function(event) {
        if( $scope.editable && !$scope.editMode){
          //only one editable-list could be in edit mode at once
          $('.cluster-manage-access-pane div.edit-mode').trigger('reset');
          $scope.editMode = true;
          $scope.focusOnInput();
        }
        event.stopPropagation();
      };

      $scope.cancel = function(event) {
        $scope.editMode = false;
        $scope.items = angular.copy($scope.itemsSource);
        $scope.input = '';
        event.stopPropagation();
      };
      $scope.save = function(event) {
        var validInput = true;
        if( $scope.input ){
          validInput = $scope.addItem($scope.input);
        }
        if (validInput) {
          $scope.itemsSource = $scope.items;
          $scope.editMode = false;
          $scope.input = '';
        }
        if(event){
          event.stopPropagation();
        }
      };


      $scope.downArrowHandler = function() {
        $scope.$apply(function() {
          $scope.selectedTypeahed = ($scope.selectedTypeahed+1) % $scope.typeahead.length;
        });
      };
      $scope.upArrowHandler = function() {
        $scope.$apply(function() {
          $scope.selectedTypeahed -= 1;
          $scope.selectedTypeahed = $scope.selectedTypeahed < 0 ? $scope.typeahead.length-1 : $scope.selectedTypeahed;
        });
      };

      $scope.addItem = function(item) {
        item = item ? item : $scope.typeahead.length ? $scope.typeahead[$scope.selectedTypeahed] : $scope.input;
        
        if (item && $scope.items.indexOf(item) === -1){
          if ($scope.resources.indexOf(item) !== -1) {
            $scope.items.push(item);
            $scope.input = '';
          } else {
            $scope.invalidInput = true;
            return false;
          }
        }
        return true;
      };

      $scope.removeFromItems = function(item) {
        $scope.items.splice( $scope.items.indexOf(item), 1);
      };

      $scope.$on('$locationChangeStart', function(event, targetUrl) {
        targetUrl = targetUrl.split('#').pop();
        if( $scope.input ){
          $scope.addItem($scope.input);
        }
        if( $scope.editMode && !angular.equals($scope.items, $scope.itemsSource)){
          var modalInstance = $modal.open({
            template: '<div class="modal-header"><h3 class="modal-title">{{"common.warning" | translate}}</h3></div><div class="modal-body">{{"common.alerts.unsavedChanges" | translate}}</div><div class="modal-footer"><div class="btn btn-default" ng-click="cancel()">{{"common.controls.cancel" | translate}}</div><div class="btn btn-warning" ng-click="discard()">{{"common.controls.discard" | translate}}</div><div class="btn btn-primary" ng-click="save()">{{"common.controls.save" | translate}}</div></div>',
            controller: ['$scope', '$modalInstance', function($scope, $modalInstance) {
              $scope.save = function() {
                $modalInstance.close('save');
              };
              $scope.discard = function() {
                $modalInstance.close('discard');
              };
              $scope.cancel = function() {
                $modalInstance.close('cancel');
              };
            }]
          });
          modalInstance.result.then(function(action) {
            switch(action){
              case 'save':
                $scope.save();
                break;
              case 'discard':
                $scope.editMode = false;
                $location.path(targetUrl);
                break;
            }
          });
          event.preventDefault();
        }
      });
    }]
  };
}]);

