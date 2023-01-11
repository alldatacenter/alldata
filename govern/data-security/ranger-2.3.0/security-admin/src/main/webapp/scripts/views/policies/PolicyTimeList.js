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

        var Backbone		= require('backbone');
        var App		        = require('App');
        var XAEnums			= require('utils/XAEnums');
        var XAUtil			= require('utils/XAUtils');
        var XAGlobals		= require('utils/XAGlobals');
        var localization	= require('utils/XALangSupport');
        var localization    = require('utils/XALangSupport');
        var moment = require('moment');
        require('bootstrap-editable');
        require('daterangepicker');

        var PolicyTimeItem = Backbone.Marionette.ItemView.extend({
                _msvName : 'PolicyTimeItem',
                template : require('hbs!tmpl/policies/PolicyTimeItem_tmpl'),
                tagName : 'tr',
                templateHelpers : function(){
                },
                ui : {
                        'startTime' : '[data-js="startTime"]',
                        'endTime': '[data-js="endTime"]',
                        'timezone': '[data-js="timezone"]',
                },
                events : {
                        'click [data-action="delete"]'	: 'evDelete',
                        'change [data-js="startTimeInput"]'		: 'onInputStartTimeChange',
                        'change [data-js="endTimeInput"]'		: 'onInputEndTimeChange',
                        'change [data-js="timezone"]'		: 'onTimezoneChange',
                        'click .onCloseBtn' : 'onCloseBtn'

                },

                initialize : function(options) {
                        _.extend(this, _.pick(options,''));
                },

                onRender : function() {
                    var that = this;
                    if(!this.model.isEmpty()) {
                        if (!_.isEmpty(this.model.get('startTime'))) {
                            that.$el.find('[data-js="startTimeInput"]').html(this.model.get('startTime'));
                        }
                        if (!_.isEmpty(this.model.get('endTime'))) {
                            that.$el.find('[data-js="endTimeInput"]').html(this.model.get('endTime'))
                        }
                    }
                    var startTime = this.$el.find(this.ui.startTime).daterangepicker({
                        "singleDatePicker": true,
                        "timePicker": true,
                        "timePicker24Hour": true,
                        "showDropdowns": true,
                        "timePickerSeconds": true,
                        "locale" : {
                            'direction':'theme-date-picker',
                            'cancelLabel': 'Clear'
                        }
                    },function(start) {
                        that.$el.find('[data-js="startTimeInput"]').html(start.format('YYYY/MM/DD HH:mm:ss'));
                        that.model.set('startTime', start.format('YYYY/MM/DD HH:mm:ss'));
                    }),
                    endTime = this.$el.find(this.ui.endTime).daterangepicker({
                        "singleDatePicker": true,
                        "timePicker": true,
                        "timePicker24Hour": true,
                        "showDropdowns": true,
                        "timePickerSeconds": true,
                        "locale" : {
                            'direction':'theme-date-picker',
                            'cancelLabel': 'Clear'
                        }
                    },function(end) {
                        that.$el.find('[data-js="endTimeInput"]').html(end.format('YYYY/MM/DD HH:mm:ss'));
                        that.model.set('endTime', end.format('YYYY/MM/DD HH:mm:ss'));
                    });
                    this.$el.find(this.ui.timezone).select2({
                        data: XAGlobals.Timezones,
                        multiple: false,
                        closeOnSelect: true,
                        placeholder: 'Select Timezone',
                        maximumSelectionSize : 1,
                        allowClear: true,
                        width:'180px'
                    });
                },
                onCloseBtn : function(e){
                    if (e.currentTarget.dataset.value === "startTime") {
                        this.$el.find('[data-js="startTimeInput"]').html('');
                        this.model.unset('startTime');
                    } else {
                        this.$el.find('[data-js="endTimeInput"]').html('');
                        this.model.unset('endTime');
                    }
                },
                onTimezoneChange : function(e) {
                    if(!_.isEmpty($(e.currentTarget).val())  && !_.isUndefined($(e.currentTarget).val())){
                        this.model.set('timeZone', $(e.currentTarget).val());
                    }else{
                        this.model.unset('timeZone');
                    }
                },
                evDelete : function(){
                        var that = this;
                        this.collection.remove(this.model);
                        $('[data-js="policyTimeBtn"]').addClass('dirtyField');
                },
        });

        var PolicyTimeList =  Backbone.Marionette.CompositeView.extend({
                _msvName : 'PolicyTimeList',
                template : require('hbs!tmpl/policies/PolicyTimeList_tmpl'),
                templateHelpers :function(){
                        return {
                            'fieldLabel' : this.fieldLabel,
                            'errorMsg' : localization.tt('validationMessages.setTimeZoneErrorMsg'),
                        };
                },
                getItemView : function(item){
                        if(!item){
                                return;
                        }
                        return PolicyTimeItem;
                },
                itemViewContainer : ".js-formInput",
                itemViewOptions : function() {
                        return {
                                'collection' : this.collection,
                        };
                },
                events : {
                        'click [data-action="addTime"]' : 'addNew'
                },
                initialize : function(options) {
                        _.extend(this, _.pick(options, 'fieldLabel'));
                        if(this.collection.length == 0){
                                this.collection.add(new Backbone.Model());
                        }
                        this.bind("ok", this.onSave);
                },
                onRender : function(){
                    XAUtil.preventNavigation(localization.tt('dialogMsg.preventNavPolicyForm'),this.$el);
                },
                addNew : function(){
                        var that =this;
                        this.$('table').show();
                        this.collection.add(new Backbone.Model());
                },
                onSave: function (modal) {
                    var self = this;
                    if(! _.isUndefined(this.collection.models)){
                       var error = _.some(this.collection.models, function(m){
                            var startTime = new Date(m.get('startTime')), endTime = new Date(m.get('endTime'));
                            if(_.isEmpty(m.get('endTime')) && _.isEmpty(m.get('startTime')) && !_.isEmpty(m.get('timeZone'))){
                                modal.preventClose();
                                modal.$content.find('.errorMsg').removeClass('display-none');
                                return true
                            } else if(startTime.valueOf() > endTime.valueOf() ){
                                modal.preventClose();
                                modal.$content.find('.errorMsg').html('Start date can not be later in time than end date.').removeClass('display-none');
                                return true;
                            }
                        });
                       if(error){
                           return
                       }
                    }
                    this.model.set('validitySchedules', _.reject(this.collection.toJSON(), function(m){ return (_.isEmpty(m) || (_.isEmpty(m.startTime) && _.isEmpty(m.endTime)))}));
                }
        });
        return PolicyTimeList;
});
