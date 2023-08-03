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

	var Backbone		= require('backbone');
	var Communicator	= require('communicator');
	var XAUtil			= require('utils/XAUtils');
	var XAEnums			= require('utils/XAEnums');
	var localization	= require('utils/XALangSupport');
	var VXGroup			= require('models/VXGroup');
	var AddGroup_tmpl 	= require('hbs!tmpl/common/AddGroup_tmpl');
	
	require('bootstrap-editable');
	var AddGroup = Backbone.Marionette.ItemView.extend(
	/** @lends AddGroup */
	{
		_viewName : 'AddGroup',
		
    	template: AddGroup_tmpl,
        templateHelpers :function() {
        	return {
        		groupId : this.model.get('groupIdList'),
        		isModelNew	: this.model.isNew()
        	};
        },
    	/** ui selector cache */
    	ui: {
    		'addGroup' :'#addGroup',
    		'errorMsg' : '[data-error="groupIdList"]'
    	},

		/** ui events hash */
		events: function() {
			var events = {};
			//events['change ' + this.ui.input]  = 'onInputChange';
			return events;
		},

    	/**
		* intialize a new AddGroup ItemView 
		* @constructs
		*/
		initialize: function(options) {
			console.log("initialized a AddGroup ItemView");
			_.extend(this, _.pick(options));
			this.bindEvents();
		},

		/** all events binding here */
		bindEvents : function(){
			/*this.listenTo(this.model, "change:foo", this.modelChanged, this);*/
			/*this.listenTo(communicator.vent,'someView:someEvent', this.someEventHandler, this)'*/
		},

		/** on render callback */
		onRender: function() {
			var that = this , arr =[];
			this.initializePlugins();
			$.fn.editable.defaults.mode = 'popover';
			
			if(!_.isUndefined(this.model.get('userSource')) && this.model.get('userSource') == XAEnums.UserSource.XA_USER.value){
				this.$el.find('#tags-edit-1').hide();
				var labelStr1 = this.$el.find('label').html().replace('*','');
				this.$el.find('label').html(labelStr1);
			}
			this.$('.tags').editable({
			    placement: 'right',
			    emptytext : 'Please select',
				select2 :this.getSelect2Options(),
			    display: function(values,srcDate) {
			    	if(_.isNull(values) ){
			    		$(this).html('');
			    		return;
			    	}
			    	that.checkDirtyFieldForGroup(values);
			    	var valArr = [];
					if(!_.isUndefined($('.multi-group-select'))
							&& $('.multi-group-select').length > 0){
						values = $('.multi-group-select').select2('data');
			    	} else {
			    		var groupNameList = that.model.get('groupNameList');
			    		values = _.map(that.model.get('groupIdList'),function(id,i){ return {'id': id, 'text': _.escape(groupNameList[i]) };});
			    	}
			    	
			    	valArr = _.map(values,function(val,i){ 
						return "<span class='badge badge-dark'>" + val.text + "</span>"  
			    	},that);

			    	that.groupArr = values;
			    	that.firstTimeEditGroup = true;
		    		$(this).html(valArr.join(" "));
			    },
			    success: function(response, newValue) {
			    	that.firstTimeEditGroup = false;
			    }
			});

			this.$('[id^="tags-edit-"]').click(function(e) {
			    e.stopPropagation();
			    e.preventDefault();
			    $('#' + $(this).data('editable') ).editable('toggle');
			});

		},
		/** all post render plugin initialization */
		initializePlugins: function(){
		},
		checkDuplicatesInArray :function(array) {
		    var valuesSoFar = {};
		    for (var i = 0; i < array.length; ++i) {
		        var value = array[i];
		        if (Object.prototype.hasOwnProperty.call(valuesSoFar, value)) { return true; }
		        valuesSoFar[value] = true;
		    }
		    return false;
		},
		getSelect2Options :function(){
			var that = this,groupCnt = 0;
    		var tags = _.map(that.model.get('groupIdList'),function(id,i){ return {'id': id, 'text': _.escape(that.model.get('groupNameList')[i]) };});
			return{
				containerCssClass : 'multi-group-select',
				closeOnSelect : true,
				placeholder : 'Select Group',
			//	maximumSelectionSize : 1,
				width :'220px',
				tokenSeparators: [",", " "],
				tags : tags,
//				multiple: true,
				initSelection : function (element, callback) {
					var data = [];
					if(!_.isUndefined(that.groupArr) && that.firstTimeEditGroup){
						data = that.groupArr;
					} else {
						data = element.select2('data');
					}
					
					callback(data);
				},
				ajax: { 
					url: "service/xusers/groups",
					dataType: 'json',
					data: function (term, page) {
						return {name : term};
					},
					results: function (data, page) { 
						var results = [],selectedVals = [];
						groupCnt = data.resultSize
						
						if(!_.isEmpty(that.$('.tags').data('editable').input.$input.val())) {
							selectedVals = that.$('.tags').data('editable').input.$input.val().split(',');
						}
						if(data.resultSize != "0"){
							results = data.vXGroups.map(function(m, i){	return {id : (m.id).toString(), text: _.escape(m.name) };	});
							if(!_.isEmpty(selectedVals)) {
								results = XAUtil.filterResultByIds(results, selectedVals);
							}
							groupCnt = results.length;
							return {results : results};
						}
						
						return {results : results};
					}
				},	
				formatResult : function(result){
					return result.text;
				},
				formatSelection : function(result){
					return result.text;
				},
				formatNoMatches: function(result){
					return groupCnt > 0 ? 'Please enter one more character.' : 'No group found.';  
				}
			};
		},
		checkDirtyFieldForGroup : function(changeValues){
			var groupIdList = [];
			if(!_.isArray(changeValues)) changeValues = [changeValues];

			changeValues = _.map(changeValues, function(val){ return parseInt(val); });
			if(!_.isUndefined(this.model.get('groupIdList'))){
				groupIdList = this.model.get('groupIdList'); 
			}
			XAUtil.checkDirtyField(groupIdList, changeValues, this.$el);
		},
		
		/** on close */
		onClose: function(){
		}

	});

	return AddGroup;
});
