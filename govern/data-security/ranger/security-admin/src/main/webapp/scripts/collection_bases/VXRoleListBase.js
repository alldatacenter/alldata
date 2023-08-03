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

    var XABaseCollection    = require('collections/XABaseCollection');
    var XAGlobals           = require('utils/XAGlobals');
    var VXRole             = require('models/VXRole');

    var VXRoleListBase = XABaseCollection.extend(
    /** @lends VXGroupListBase.prototype */
    {
        url: XAGlobals.baseURL + 'roles/roles',

        model : VXRole,

        /**
         * VXGroupListBase initialize method
         * @augments XABaseCollection
         * @constructs
         */
        initialize : function() {
            this.modelName = 'roles';
            this.modelAttrName = 'roles';
            var multiSelect = new Backbone.Picky.MultiSelect(this);
            _.extend(this, multiSelect);
            this.bindErrorEvents();
            this._changes = { };
            this.on('change', this._onChange);
        },

        _onChange : function(m){
            this._changes[m.id] = m;
        },

        changed_models: function() {
            return _.chain(this._changes).values();
        },

        // /*************************
        //  * Non - CRUD operations
        //  *************************/

        // getGroupsForUser : function(userId, options){
        //     var url = XAGlobals.baseURL + 'xusers/' + userId + '/groups';

        //     options = _.extend({
        //         //data : JSON.stringify(postData),
        //         contentType : 'application/json',
        //         dataType : 'json'
        //     }, options);

        //     return this.constructor.nonCrudOperation.call(this, url, 'GET', options);
        // },

        // setRolesVisibility : function(postData , options){
        //     var url = XAGlobals.baseURL  + 'xusers/secure/roles/visibility';

        //     options = _.extend({
        //         data : JSON.stringify(postData),
        //         contentType : 'application/json',
        //         dataType : 'json'
        //     }, options);

        //     return this.constructor.nonCrudOperation.call(this, url, 'PUT', options);
        // },

    },{
        // static class members
        /**
        * Table Cols to be passed to Backgrid
        * UI has to use this as base and extend this.
        *
        */

        tableCols : {}

    });

    return VXRoleListBase;
});