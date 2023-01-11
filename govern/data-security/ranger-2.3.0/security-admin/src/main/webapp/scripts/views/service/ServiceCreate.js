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
 * Repository/Service create view
 */

define(function(require){
    'use strict';

	var Backbone		= require('backbone');
	var App				= require('App');
	var XAUtil			= require('utils/XAUtils');
	var XAEnums			= require('utils/XAEnums');
	var XALinks 		= require('modules/XALinks');
	var localization	= require('utils/XALangSupport');
	var bootbox 		= require('bootbox');

	var ServiceForm		= require('views/service/ServiceForm');
	var RangerServiceDef	= require('models/RangerServiceDef');
	var ServiceCreateTmpl = require('hbs!tmpl/service/ServiceCreate_tmpl');

	var ServiceCreate = Backbone.Marionette.Layout.extend(
	/** @lends ServiceCreate */
	{
		_viewName : 'ServiceCreate',

		template: ServiceCreateTmpl,

		templateHelpers : function(){
			return { editService : this.editService};
		},

		breadCrumbs :function(){
			var name  = this.rangerServiceDefModel.get('name') != XAEnums.ServiceType.SERVICE_TAG.label ? 'ServiceManager' : 'TagBasedServiceManager';
			if(this.model.isNew()){
				return [XALinks.get(name), XALinks.get('ServiceCreate')];
			} else {
				return [XALinks.get(name), XALinks.get('ServiceEdit')];
			}
		},

		/** Layout sub regions */
		regions: {
			'rForm' :'div[data-id="r_form"]'
		},

		/** ui selector cache */
		ui: {
			'btnSave'	: '[data-id="save"]',
			'btnCancel' : '[data-id="cancel"]',
			'btnDelete' : '[data-id="delete"]',
			'btnTestConn' : '[data-id="testConn"]'
		},

		/** ui events hash */
		events: function() {
			var events = {};
			events['click ' + this.ui.btnSave]		= 'onSave';
			events['click ' + this.ui.btnCancel]	= 'onCancel';
			events['click ' + this.ui.btnDelete]	= 'onDelete';
			events['click ' + this.ui.btnTestConn]	= 'onTestConnection';
			return events;
		},

		/**
		 * intialize a new ServiceCreate Layout 
		 * @constructs
		 */
		initialize: function(options) {
			console.log("initialized a ServiceCreate Layout");
			_.extend(this, _.pick(options, 'serviceTypeId'));
			this.initializeServiceDef();
			this.form = new ServiceForm({
				model :	this.model,
				rangerServiceDefModel : this.rangerServiceDefModel,
				template : require('hbs!tmpl/service/ServiceForm_tmpl')
			});
			this.editService = this.model.has('id') ? true : false;
			this.bindEvents();
		},
		initializeServiceDef : function(){
		    this.rangerServiceDefModel	= new RangerServiceDef({ id : this.serviceTypeId});
			this.rangerServiceDefModel.fetch({
			   cache : false,
			   async : false
			});
		},
		setupModel : function(){
		},

		/** all events binding here */
		bindEvents : function(){
			/*this.listenTo(this.model, "change:foo", this.modelChanged, this);*/
			/*this.listenTo(communicator.vent,'someView:someEvent', this.someEventHandler, this)'*/
		},

		/** on render callback */
		onRender: function() {
			if(!this.editService){
				this.ui.btnDelete.hide();
				this.ui.btnSave.html('Add');
			}
			this.rForm.show(this.form);
			this.rForm.$el.dirtyFields();
			XAUtil.preventNavigation(localization.tt('dialogMsg.preventNavRepositoryForm'),this.rForm.$el);
			this.initializePlugins();
		},

		/** all post render plugin initialization */
		initializePlugins: function(){
		},

		onSave: function(){
			var errors = this.form.commit({validate : false});
			if(! _.isEmpty(errors)){
				return;
			}
			if (!this.form.formValidation()) {
				return;
			}
			this.saveService();
		},
		saveService : function(){
			var that = this;
			this.form.beforeSave();
			XAUtil.blockUI();
			this.model.save({},{
				wait: true,
				success: function (response) {
					XAUtil.blockUI('unblock');
					XAUtil.allowNavigation();
					var msg = that.editService ? 'Service updated successfully' :'Service created successfully';
					XAUtil.notifySuccess('Success', msg);
					if(localStorage.getItem('setOldUI') == "false" || localStorage.getItem('setOldUI') == null) {
						App.rSideBar.currentView.render();
						if(that.editService) {
							that.gotoServicePolicyListingPage();
						} else{
							that.gotoResourceOrTagNewTab(response);
						}
					} else {
						that.gotoResourceOrTagTab()
					}
				},
				error: function (model, response, options) {
					XAUtil.blockUI('unblock');
					var msg = that.editService ? 'Error updating Service.': 'Error creating Service.';
					if (response && response.responseJSON && response.responseJSON.msgDesc) {
						XAUtil.showErrorMsg(response.responseJSON.msgDesc);
					} else {
						XAUtil.notifyError('Error', msg);
					}
				}
			});
		},
		onDelete :function(){
			var that = this;
			XAUtil.confirmPopup({
				msg :'Are you sure want to delete ?',
				callback : function(){
					XAUtil.blockUI();
					if (!_.isUndefined($('.latestResponse')) && $('.latestResponse').length > 0) {
						$('.latestResponse').html('<b>Last Response Time : </b>' + Globalize.format(new Date(),  "MM/dd/yyyy hh:mm:ss tt"));
					}
					that.model.destroy({
						success: function(model, response) {
							XAUtil.blockUI('unblock');
							XAUtil.allowNavigation();
							XAUtil.notifySuccess('Success', 'Service delete successfully');
							if(localStorage.getItem('setOldUI') == "false" || localStorage.getItem('setOldUI') == null) {
								App.rSideBar.currentView.render();
							}
							that.gotoResourceOrTagTab()
						},
						error: function (model, response, options) {
							XAUtil.blockUI('unblock');
							if ( response && response.responseJSON && response.responseJSON.msgDesc){
									XAUtil.notifyError('Error', response.responseJSON.msgDesc);
							} else {
								XAUtil.notifyError('Error', 'Error occured while deleting service!');
							}
						}
					});

				}
			});
		},
		onTestConnection : function(){
			var errors = this.form.commit({validate : false});
			if(! _.isEmpty(errors)){
				return;
			}
			if (!this.form.formValidation()) {
				return;
			}
			this.form.beforeSave();
			this.model.testConfig(this.model,{
					//wait: true,
					success: function (msResponse, options) {
						if(msResponse.statusCode){
							if(!_.isUndefined(msResponse) && !_.isUndefined(msResponse.msgDesc)){ 
								var popupBtnOpts;
                               if(!_.isEmpty(msResponse.msgDesc)){
                            	   if(_.isArray(msResponse.messageList) && !_.isUndefined(msResponse.messageList[0].message)
                            			   && !_.isEmpty(msResponse.messageList[0].message)){
	                            		   popupBtnOpts = [{
	                            			   label: "Show More..",
	                            			   callback:function(e){
	                            				   console.log(e)
	                            				   if($(e.currentTarget).text() == 'Show More..'){
									   var respMsg = _.escape( msResponse.messageList[0].message );
										   var div = '<div class="showMore connection-error-font"><br>'+respMsg.split('\n').join('<br>')+'</div>'
                        							   $(e.delegateTarget).find('.modal-body').append(div)
                        							   $(e.currentTarget).html('Show Less..')
	                            				   } else {
	                            					   $(e.delegateTarget).find('.showMore').remove();
	                            					   $(e.currentTarget).html('Show More..')
	                            				   }
	                            				   return false;
	                            			   }
	                            		   }, {
	                            			   label: "OK",
	                            			   callback:function(){}
	                            		   }];
                            	   } else { 
                            		   		popupBtnOpts = [{label: "OK",
                            		   			callback:function(){}
                            		   		}];
                            	   }
                                   var msgHtml = '<b>Connection Failed.</b></br>'+msResponse.msgDesc;
                                    bootbox.dialog({
                                        message : msgHtml,
                                        buttons: popupBtnOpts
                                    });
								} else {
										bootbox.alert("Connection Failed.");
								}
							} else {
								bootbox.alert("Connection Failed.");
							}
						} else {
							bootbox.alert("Connected Successfully.");
						}
					},
					error: function (msResponse, options) {
                                                if(msResponse.status === 419){
                                                        XAUtil.defaultErrorHandler(options , msResponse);
                                                }
						bootbox.alert("Connection Failed.");
					}
				});
		},
		gotoResourceOrTagTab : function(){
			if(XAEnums.ServiceType.SERVICE_TAG.label == this.rangerServiceDefModel.get('name')){
				App.appRouter.navigate("#!/policymanager/tag",{trigger: true});
				return;
			}
			App.appRouter.navigate("#!/policymanager/resource",{trigger: true});
		},
		gotoResourceOrTagNewTab : function (response) {
			var Url = '#!/service/'+response.id+'/policies/0';
			App.appRouter.navigate(Url,{trigger: true});
		},

		gotoServicePolicyListingPage : function () {
			var Url = '#!/service/'+this.model.id+'/policies/0';
			App.appRouter.navigate(Url,{trigger: true});
		},
		onCancel : function(){
			XAUtil.allowNavigation();
			if(localStorage.getItem('setOldUI') == "false" || localStorage.getItem('setOldUI') == null) {
				if(this.editService) {
					this.gotoServicePolicyListingPage();
				} else{
					this.gotoResourceOrTagTab();
				}
			} else {
				this.gotoResourceOrTagTab();
			}
		},
		/** on close */
		onClose: function(){
            XAUtil.removeUnwantedDomElement();
            XAUtil.allowNavigation();
		}
	});

	return ServiceCreate; 
});
