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

import Ember from 'ember';
import OperationModal from '../mixins/operation-modal';

export default Ember.Component.extend(OperationModal, {
  fileSelectionService: Ember.inject.service('files-selection'),
  fileOperationService: Ember.inject.service('file-operation'),
  closeOnEscape: true,
  isUpdating: false,
  selected: Ember.computed('fileSelectionService.files', function() {
    return this.get('fileSelectionService.files').objectAt(0);
  }),
  permission: Ember.computed('selected.permission', function() {
    return this.get('selected.permission');
  }),
  setPermissionGuards: function() {
    var permission = this.get('permission');
    this.set('usrR', this.isSet(permission, 'user', "read"));
    this.set('usrW', this.isSet(permission, 'user', "write"));
    this.set('usrE', this.isSet(permission, 'user', "execute"));

    this.set('grpR', this.isSet(permission, 'group', "read"));
    this.set('grpW', this.isSet(permission, 'group', "write"));
    this.set('grpE', this.isSet(permission, 'group', "execute"));

    this.set('othR', this.isSet(permission, 'other', "read"));
    this.set('othW', this.isSet(permission, 'other', "write"));
    this.set('othE', this.isSet(permission, 'other', "execute"));
  },

  isSet: function(permission, userType, permissionType) {
    var checkValueAtLocation = function(index, value) {
      return permission[index] === value;
    };

    var checkValueForPermissionType = function(startIndex, permissionType) {
      switch(permissionType) {
        case 'read':
          return checkValueAtLocation(startIndex, 'r');
        case 'write':
          return checkValueAtLocation(startIndex + 1, 'w');
        case 'execute':
          return checkValueAtLocation(startIndex + 2, 'x');
      }
    };
    switch(userType) {
      case "user":
        return checkValueForPermissionType(1, permissionType);
      case "group":
        return checkValueForPermissionType(4, permissionType);
      case "other":
        return checkValueForPermissionType(7, permissionType);
    }
  },

  getPermissionFromGuards: function() {
    var oldPermission = this.get('permission');
    var replaceAt = function(index, value) {
      return oldPermission.substring(0, index) + value + oldPermission.substring(index + value.length);
    };
    oldPermission = this.get('usrR') ? replaceAt(1, 'r') : replaceAt(1, '-');
    oldPermission = this.get('usrW') ? replaceAt(2, 'w') : replaceAt(2, '-');
    oldPermission = this.get('usrE') ? replaceAt(3, 'x') : replaceAt(3, '-');
    oldPermission = this.get('grpR') ? replaceAt(4, 'r') : replaceAt(4, '-');
    oldPermission = this.get('grpW') ? replaceAt(5, 'w') : replaceAt(5, '-');
    oldPermission = this.get('grpE') ? replaceAt(6, 'x') : replaceAt(6, '-');
    oldPermission = this.get('othR') ? replaceAt(7, 'r') : replaceAt(7, '-');
    oldPermission = this.get('othW') ? replaceAt(8, 'w') : replaceAt(8, '-');
    oldPermission = this.get('othE') ? replaceAt(9, 'x') : replaceAt(9, '-');
    return oldPermission;
  },
  actions: {
    didOpenModal: function() {
      this.setPermissionGuards();
    },

    chmod: function() {
      var newPermission = this.getPermissionFromGuards();
      if(newPermission === this.get('permission')) {
        return false;
      }
      this.set('isUpdating', true);
      this.get('fileOperationService').chmod(this.get('selected').get('path'), newPermission).then((response) => {
        this.get('selected').set('permission', response.permission);
        this.set('isUpdating', false);
        this.send('close');
      }, (error) => {
        this.set('isUpdating', false);
        this.send('close');
      });
    },

    togglePermission: function(propertyName) {
      Ember.run.later(() => {
        this.set(propertyName, !this.get(propertyName));
      });
    }
  }
});
