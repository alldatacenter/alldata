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
	var XAEnums 		= require('utils/XAEnums');
	var XALinks 		= require('modules/XALinks');
	var XAUtil			= require('utils/XAUtils');
	var localization	= require('utils/XALangSupport');
	
	var RangerServiceList	= require('collections/RangerServiceList');
	var KmsKeyList			= require('collections/VXKmsKeyList');
	var KmsKey				= require('models/VXKmsKey');
	var XATableLayout		= require('views/common/XATableLayout');
	var KmsTablelayoutTmpl 	= require('hbs!tmpl/kms/KmsTableLayout_tmpl');
    var SessionMgr          = require('mgrs/SessionMgr');
    var App    = require('App');

	var KmsTableLayout = Backbone.Marionette.Layout.extend(
	/** @lends KmsTableLayout */
	{
		_viewName : 'KmsTableLayout',
		
    	template: KmsTablelayoutTmpl,
    	templateHelpers : function(){
	    return {
	        isKeyadmin : SessionMgr.isKeyAdmin() ? true :false,
	        setOldUi : localStorage.getItem('setOldUI') == "true" ? true : false,
	    }
    	},
    	breadCrumbs :[XALinks.get('KmsManage')],
		/** Layout sub regions */
    	regions: {
    		'rTableList' :'div[data-id="r_tableList"]',
    	},

    	/** ui selector cache */
    	ui: {
    		tab 		: '.nav-tabs',
    		addNewKey	: '[data-id="addNewKey"]',
    		deleteKeyBtn	: '[data-name="deleteKey"]',
    		visualSearch: '.visual_search',
    		selectServiceName	: '[data-js="serviceName"]',
    		rolloverBtn	: '[data-name="rolloverKey"]',
    	},

		/** ui events hash */
		events: function() {
			var events = {};
			events['click '+this.ui.tab+' li a']  = 'onTabChange';
			events['click '+this.ui.deleteKeyBtn]  = 'onDelete';
			events['click '+this.ui.rolloverBtn]  = 'onRollover';
			
			return events;
		},

    	/**
		* intialize a new KmsTableLayout Layout 
		* @constructs
		*/
		initialize: function(options) {
			console.log("initialized a KmsTableLayout Layout");
            _.extend(this, _.pick(options, 'tab','kmsServiceName','kmsManagePage'));
            this.urlQueryParams = XAUtil.urlQueryParams();
			this.showKeyList = true;
			this.isKnownKmsServicePage =  this.kmsManagePage == 'new' ? false : true;
			this.initializeKMSServices();
			this.bindEvents();
			this.defaultsCollstate = this.collection.state
		},

		/** all events binding here */
		bindEvents : function(){
			var that = this;
			/*this.listenTo(this.model, "change:foo", this.modelChanged, this);*/
			/*this.listenTo(communicator.vent,'someView:someEvent', this.someEventHandler, this)'*/
		},
		initializeKMSServices : function(){
			this.rangerKmsServList = new RangerServiceList();
			this.rangerKmsServList.queryParams['serviceType'] =  'kms';
			this.rangerKmsServList.fetch({
				cache : false,
				async : false
			});
		},
		getKeysForKmsService : function() {
			var that = this;
			this.collection.queryParams['provider'] = this.kmsServiceName;
			this.collection.fetch({
				cache : false,
				reset :true,
				error : function(collection,resp){
					var errorMsg = 'Error getting key list!!';
					if(!_.isUndefined(resp) && !_.isUndefined(resp.responseJSON) && !_.isUndefined(resp.responseJSON.msgDesc)){
						errorMsg = resp.responseJSON.msgDesc;
					}
					XAUtil.notifyError('Error', errorMsg);
					collection.state = that.defaultsCollstate;
					collection.reset();
				}
			});
		},
		/** on render callback */
		onRender: function() {
			if(localStorage.getItem('setOldUI') == "false" || localStorage.getItem('setOldUI') == null) {
				this.ui.selectServiceName = App.rSideBar.currentView.ui.selectServiceName;
			}
			this.initializePlugins();
			if(_.isUndefined(this.tab)){
				this.renderKeyTab();
			}
			if(this.isKnownKmsServicePage){
				this.ui.selectServiceName.val(this.kmsServiceName);
				this.ui.addNewKey.attr('disabled',false);
				this.ui.addNewKey.attr('href','#!/kms/keys/'+ this.kmsServiceName +'/create')
				
			}else{
				this.ui.addNewKey.attr('disabled',true);
			}
			this.setupKmsServiceAutoComplete();
			this.addVisualSearch();
			//Showing pagination even if No Key found
			if(!this.isKnownKmsServicePage){
				this.collection.reset();
			}
			if(this.isKnownKmsServicePage){
                                this.getKeysForKmsService();
                        }
		},
		onTabChange : function(e){
			var that = this;
			this.showKeyList = $(e.currentTarget).attr('href') == '#keys' ? true : false;
			if(this.showKeyList){				
				this.renderKeyTab();
			}
		},
		renderKeyTab : function(){
			var that = this;
			this.renderKeyListTable();
		},
		renderKeyListTable : function(){
			var that = this;
			this.rTableList.show(new XATableLayout({
				columns: this.getColumns(),
				collection: this.collection,
				includeFilter : false,
				gridOpts : {
					row: Backgrid.Row.extend({}),
					emptyText : 'No Key found!'
				}
			}));	
		},

		getColumns : function(){
			var that = this;
			var cols = {
				name : {
					label	: localization.tt("lbl.keyName"),
					cell :'string',
					editable:false,
					sortable:false,
				},
				cipher : {
					label	: localization.tt("lbl.cipher"),
					cell : 'string',
					editable:false,
					sortable:false,
				},
				versions : {
					label	: localization.tt("lbl.version"),
					cell : 'string',
					editable:false,
					sortable:false,
				},
				attributes : {
					label: localization.tt("lbl.attributes"),
					cell : 'html',
					editable:false,
					sortable:false,
					formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
						fromRaw: function (rawValue) {
							var html = '';
							_.each(rawValue, function(val, key) {
								html += key+' <i class="fa-fw fa fa-long-arrow-right fa-fw fa fa-3"></i>  '+val+'<br/>';
							});
							return html;
						}	
					})	
				},
				length : {
					label	: localization.tt("lbl.length"),
					cell : 'string',
					editable:false,
					sortable:false,
				},
				created : {
					label	: localization.tt("lbl.createdDate"),
					cell : 'string',
					click : false,
					drag : false,
					editable:false,
					sortable:false,
					formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
						fromRaw: function (rawValue, model) {
							if(!_.isUndefined(rawValue))
								return Globalize.format(new Date(rawValue),  "MM/dd/yyyy hh:mm:ss tt");
						}
					})
				},
				operation : {
						cell :  "html",
						label : localization.tt("lbl.action"),
						formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
							fromRaw: function (rawValue,model) {
								return '<a href="javascript:void(0);" data-name ="rolloverKey" data-id="'+model.get('name')+'" class="btn btn-sm" title="Rollover"><i class="fa-fw fa fa-edit"></i></a>\
										<a href="javascript:void(0);" data-name ="deleteKey" data-id="'+model.get('name')+'"  class="btn btn-sm btn-danger" title="Delete"><i class="fa-fw fa fa-trash"></i></a>';
								//You can use rawValue to custom your html, you can change this value using the name parameter.
							}
						}),
						editable: false,
						sortable : false
				}
				
			};
                        if(!SessionMgr.isKeyAdmin()){
                            delete cols.operation;
                        }
			return this.collection.constructor.getTableCols(cols, this.collection);
		},
		
		addVisualSearch : function(){
			var coll,placeholder;
			var searchOpt = [], serverAttrName = [];
			if(this.showKeyList){
				placeholder = localization.tt('h.searchForKeys');	
				coll = this.collection;
				searchOpt = ['Key Name'];
                                serverAttrName  = [	{text : "Key Name", label :"name", urlLabel : "keyName"}];
			}
			var query = (!_.isUndefined(coll.VSQuery)) ? coll.VSQuery : '';
                        if(!_.isUndefined(this.urlQueryParams)) {
                                var urlQueryParams = XAUtil.changeUrlToSearchQuery(this.urlQueryParams);
                                _.map(urlQueryParams, function(val , key) {
                    if (_.some(serverAttrName, function(m){return m.urlLabel == key})) {
                        query += '"'+XAUtil.filterKeyForVSQuery(serverAttrName, key)+'":"'+val+'"';
                    }
                                });
                        }
			var pluginAttr = {
				      placeholder :placeholder,
				      container : this.ui.visualSearch,
				      query     : query,
				      callbacks :  { 
				    	  valueMatches :function(facet, searchTerm, callback) {
								switch (facet) {
								}     
							}
				      }
				};
			XAUtil.addVisualSearch(searchOpt,serverAttrName, coll,pluginAttr);
		},
		/** all post render plugin initialization */
		initializePlugins: function(){
		},
		setupKmsServiceAutoComplete : function(serviceName){
			var that = this, arr = [];
			
			this.ui.selectServiceName.select2({
				maximumSelectionSize : 1,
				closeOnSelect : true,
                                allowClear: true,
				width :'220px',
				placeholder : 'Please select KMS service',
				initSelection : function (element, callback) {
                                        callback({ id : element.val(), text : _.escape( element.val() )});
				},
				ajax: { 
					url: "service/plugins/services",
					dataType: 'json',
					data: function (term, page) {
						return { name : term, 'serviceType' : 'kms' };
					},
					results: function (data, page) { 
						var results = [],selectedVals = [];
						if(data.resultSize != "0"){
                                                        results = data.services.map(function(m, i){	return {id : m.name, text: _.escape( m.name )};	});
							return { results : results };
						}
						return { results : results };
                                        },
                                        transport: function (options) {
                                                $.ajax(options).fail(function(respones) {
                                                        XAUtil.defaultErrorHandler('error',respones);
                                                        this.success({
                                                                resultSize : 0
                                                        });
                                                });
					}
				},	
				formatResult : function(result){
					return result.text;
				},
				formatSelection : function(result){
					return result.text;
				},
				formatNoMatches: function(result){
					return 'No service found.';
				}
			})
			//.on('select2-focus', XAUtil.select2Focus)
			.on('change',function(e) {
				that.kmsServiceName = (e.currentTarget.value)
				that.ui.addNewKey.attr('disabled',false);
				that.ui.addNewKey.attr('href','#!/kms/keys/'+that.kmsServiceName+'/create')
				that.getKeysForKmsService();
			});
			
		},
		onDelete :function(e){
			var that = this;
			
			var obj = this.collection.get($(e.currentTarget).data('id'));
			var model = new KmsKey(obj.attributes);
			model.collection = this.collection;
			var url = model.urlRoot +"/"+model.get('name') +"?provider="+ this.kmsServiceName;
			
			XAUtil.confirmPopup({
				msg :'Are you sure want to delete ?',
				callback : function(){
					XAUtil.blockUI();
					model.destroy({
						'url' : url,
						'success': function(model, response) {
							XAUtil.blockUI('unblock');
							that.collection.remove(model.get('id'));
							XAUtil.notifySuccess('Success', localization.tt('msg.keyDeleteMsg'));
							that.renderKeyTab();
							that.collection.fetch();
						},						
						'error' : function(model,resp){
							var errorMsg = 'Error deleting key!';
							XAUtil.blockUI('unblock');
							if(!_.isUndefined(resp) && !_.isUndefined(resp.responseJSON) && !_.isUndefined(resp.responseJSON.msgDesc)){
								errorMsg = resp.responseJSON.msgDesc;
							}
							XAUtil.notifyError('Error', errorMsg);
						}
					});
				}
			});
		},
		onRollover :function(e){
			var that = this;
			var obj = this.collection.get($(e.currentTarget).data('id'));
			var model = new KmsKey({ 'name' : obj.attributes.name });
			model.collection = this.collection;
			var url = model.urlRoot + "?provider=" + this.kmsServiceName;
			XAUtil.confirmPopup({
				msg :'Are you sure want to rollover ?',
				callback : function(){
					XAUtil.blockUI();
					
					model.save({},{
						 'type' : 'PUT',
                         'url' : url,
						'success': function(model, response) {
							XAUtil.blockUI('unblock');
							that.collection.remove(model.get('id'));
							XAUtil.notifySuccess('Success', localization.tt('msg.rolloverSuccessfully'));
							that.renderKeyTab();
							that.collection.fetch();
						},
						'error' : function(model,resp){
                            var errorMsg = 'Error rollovering key!';
                            XAUtil.blockUI('unblock');
                            if(!_.isUndefined(resp) && !_.isUndefined(resp.responseJSON) && !_.isUndefined(resp.responseJSON.msgDesc)){
                            	errorMsg = resp.responseJSON.msgDesc;
                            }
                            XAUtil.notifyError('Error', errorMsg);
						}
					});
				}
			});
		},
		/** on close */
		onClose: function(){
			XAUtil.allowNavigation();
                        XAUtil.removeUnwantedDomElement();
		}

	});

	return KmsTableLayout; 
});
