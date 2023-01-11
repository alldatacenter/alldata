/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


define(function(require){
    'use strict';

    var Backbone      = require('backbone');
    var XAEnums       = require('utils/XAEnums');
    var App           = require('App');
    var XAUtils       = require('utils/XAUtils');
    var localization  = require('utils/XALangSupport');
    var XALinks       = require('modules/XALinks');
    var XABackgrid    = require('views/common/XABackgrid');
    var XATableLayout = require('views/common/XATableLayout');
    var VXRole        = require('models/VXRole');
    var VXRoleListBase      = require('collection_bases/VXRoleListBase');
    var usersAndGroupsItemList = require('views/users/AddUsersOrGroupsList');

    require('backbone-forms');
    require('backbone-forms.list');
    require('backbone-forms.templates');
    require('backbone-forms.XAOverrides');
    var RoleForm = Backbone.Form.extend(
    /** @lends GroupForm */
    {
        _viewName : 'RoleForm',

        /**
        * intialize a new RoleForm Form View
        * @constructs
        */

        ui : {
            'usersItemList'  : '[data-name="userTableLayout"]',
            'groupsItemList'  : '[data-name="groupTableLayout"]',
            'rolesItemList'  : '[data-name="roleTableLayout"]',
        },

        initialize: function(options) {
            console.log("initialized a RoleForm Form View");
            Backbone.Form.prototype.initialize.call(this, options);

            this.bindEvents();
        },

        /** all events binding here */
        bindEvents : function() {
        },

        /** fields for the form
        */
        fields: ['name', 'description'],

        schema :{},

        /** on render callback */
        render: function(options) {
            Backbone.Form.prototype.render.call(this, options);
            this.addUserTbl();
            this.addGroupTbl();
            this.addRoleTbl();
        },

        /** all custom field rendering */
        renderCustomFields: function(collection, $filed, fieldName, model) {
            this.$el.find($filed).html(new usersAndGroupsItemList({
                collection : collection,
                fieldName  : fieldName,
                model      : model,
            }).render().el);
        },

        addUserTbl : function() {
            var that = this, models = this.model.isNew() ? [] : this.model.get('users');
            this.usersColl = new Backbone.Collection(models);
            this.renderCustomFields(this.usersColl, this.ui.usersItemList, 'users', this.model);
        },

        addGroupTbl : function() {
            var that = this, models = this.model.isNew() ? [] : this.model.get('groups');
            this.groupsColl = new Backbone.Collection(models);
            this.renderCustomFields(this.groupsColl, this.ui.groupsItemList, 'groups', this.model);
        },

        addRoleTbl : function() {
            var that = this, models = this.model.isNew() ? [] : this.model.get('roles');
            this.rolesColl = new Backbone.Collection(models);
            this.renderCustomFields(this.rolesColl, this.ui.rolesItemList, 'roles', this.model);
        },

        beforeSave : function() {
            var that = this;
            if(!_.isEmpty(this.$el.find('[data-name="usersSelect"]').select2('data')) ||
                !_.isEmpty(this.$el.find('[data-name="groupsSelect"]').select2('data')) ||
                !_.isEmpty(this.$el.find('[data-name="rolesSelect"]').select2('data'))) {
                if(!_.isEmpty(this.$el.find('[data-name="usersSelect"]').select2('data'))) {
                    XAUtils.scrollToRolesField(this.$el.find('[data-name="usersAddBtn"]'));
                } else if (!_.isEmpty(this.$el.find('[data-name="groupsSelect"]').select2('data'))) {
                    XAUtils.scrollToRolesField(this.$el.find('[data-name="groupsAddBtn"]'));
                } else {
                    XAUtils.scrollToRolesField(this.$el.find('[data-name="rolesAddBtn"]'));
                }
                XAUtils.alertPopup({
                    msg :localization.tt('msg.addSelectedUserGroupRoles'),
                });
                return false;
            }
            this.usersColl.remove(that.usersColl.models.filter(function(model){
                return _.isUndefined(model.get('name'))
            }));
            this.groupsColl.remove(that.groupsColl.models.filter(function(model){
                return _.isUndefined(model.get('name'))
            }));
            this.rolesColl.remove(that.rolesColl.models.filter(function(model){
                return _.isUndefined(model.get('name'))
            }))
            return true;
        },

    });

    return RoleForm;
});