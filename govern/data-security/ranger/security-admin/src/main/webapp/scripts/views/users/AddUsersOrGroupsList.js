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

 /*
 *
 */
define(function(require) {
    'use strict';

    var Backbone        = require('backbone');
    var App             = require('App');
    var XAEnums         = require('utils/XAEnums');
    var XAUtil          = require('utils/XAUtils');
    var localization    = require('utils/XALangSupport');
    var VXGroup         = require('models/VXGroup');
    var VXGroupList         = require('collections/VXGroupList');
    var VXUserList          = require('collections/VXUserList');
    require('bootstrap-editable');

    var UsersAndGroupsItem = Backbone.Marionette.ItemView.extend({
        _msvName : 'FormInputItem',

        template : require('hbs!tmpl/users/AddUserOrGroupsItem_tmpl'),

        tagName : 'tr',

        templateHelpers : function(){
            var usersGroupsName = this.model.get('name');
            return {
                usersGroupsName : usersGroupsName,
            };
        },

        ui : {
            'isRoleAdmin' : 'input[data-js="isRoleAdmin"]',
        },

        events : {
            'click [data-action="delete"]'  : 'evDelete',
            'click [data-js="isRoleAdmin"]' : 'evClickTD',
        },

        initialize : function(options) {
            _.extend(this, _.pick(options, 'collection', 'fieldName'));
        },

        onRender : function() {
            var that = this;
            this.ui.isRoleAdmin.attr('checked', this.model.get('isAdmin'));
        },

        evClickTD : function(e){
            var $el = $(e.currentTarget);
            this.model.set('isAdmin', $el.is(':checked'));
        },

        evDelete : function(){
            var that = this;
            this.collection.remove(this.model);
        },
    });

    var AddUserOrGroupsList =  Backbone.Marionette.CompositeView.extend({

        _msvName : 'AddUserOrGroupsList',

        template : require('hbs!tmpl/users/AddUserOrGroupsList_tmpl'),

        templateHelpers :function(){
            var headerName = this.fieldName === 'users' ? "User Name" : this.fieldName === 'groups' ? "Group Name" : "Role Name",
            btnLable = this.fieldName === 'users' ? 'Add Users' : this.fieldName === 'groups' ? "Add Group" : "Add Role";
            return {
                headerName : headerName,
                btnLable : btnLable
            };
        },

        getItemView : function(item){
            if(!item){
                return;
            }
            return UsersAndGroupsItem;
        },
        itemViewContainer : ".js-formInput",

        itemViewOptions : function() {
            return {
                'collection' : this.collection,
                'fieldName'  : this.fieldName,
            };
        },

        ui : {
            'selectUsersOrGroups' : '[data-js="selectUsersOrGroups"]',
            'addUserGroupRoleBtn' : '[data-action="addUserGroup"]'
        },

        events : {
            'click [data-action="addUserGroup"]' : 'addNew'
        },

        /** all events binding here */
        bindEvents : function() {
        },

        initialize : function(options) {
            _.extend(this, _.pick(options, 'collection', 'fieldName', 'model'));
        },

        onRender : function() {
            if(this.collection.length === 0){
                this.$el.find('[data-id="userGroupsTable"]').html('<tr data-name="'+this.fieldName+'"><td colspan="3"> No '
                    +this.fieldName+' found</td></tr>');
            }
            this.ui.selectUsersOrGroups.select2(XAUtil.getUsersGroupsList(this.fieldName, this, '300px'));
            this.ui.selectUsersOrGroups.attr("data-name", this.fieldName+"Select");
            this.ui.addUserGroupRoleBtn.attr("data-name", this.fieldName+"AddBtn")
        },

        addNew : function() {
            var that =this;
            this.$('table').show();
            if(_.isEmpty(this.ui.selectUsersOrGroups.val())) {
                XAUtil.alertPopup ({
                    msg: 'Please select '+this.fieldName
                });
                return
            } else {
                this.$el.find('[data-name='+this.fieldName+']').hide();
                _.each(this.ui.selectUsersOrGroups.val().split(','), function(name){
                    that.collection.add(new Backbone.Model({'name' : name , 'isAdmin' : false}));
                });
                this.ui.selectUsersOrGroups.select2('data',[]);
            }
        },
    });

    return AddUserOrGroupsList;

});
